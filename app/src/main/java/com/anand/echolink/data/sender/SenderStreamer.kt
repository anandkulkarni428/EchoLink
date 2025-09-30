package com.anand.echolink.data.sender

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.anand.echolink.NetConfig.CHANNELS
import com.anand.echolink.NetConfig.SAMPLE_RATE
import com.anand.echolink.NetConfig.STREAM_MAGIC
import com.anand.echolink.NetConfig.UDP_PORT
import com.anand.echolink.data.net.NetProtocol
import com.anand.echolink.util.HostNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketException
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.coroutineContext
import kotlin.math.sqrt

class SenderStreamer(private val appContext: Context) {
    private val running = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // media
    private var projection: MediaProjection? = null
    private var recorder: AudioRecord? = null
    private var encoder: MediaCodec? = null

    // sockets
    private var audioSocket: DatagramSocket? = null
    private var ctrlSocket: DatagramSocket? = null

    // jobs
    private var encodeJob: Job? = null
    private var ctrlJob: Job? = null
    private var pingJob: Job? = null

    // listeners
    private val listeners = ListenerRegistry()

    /* ---------------- Public API for UI ---------------- */

    /** Immutable snapshot for UI (safe to call from main thread). */
    fun listenersSnapshot(): List<ListenerInfo> = listeners.all()
    fun listenerCount(): Int = listeners.size()

    /** Toggle per-device streaming (by hostString/IP). */
    fun setListenerEnabled(hostString: String, enabled: Boolean) {
        listeners.all().firstOrNull { it.addr.hostString == hostString }?.let {
            listeners.setEnabled(it.addr, enabled)
        }
        HostBus.updateFrom(listeners.all())

    }

    /* ---------------- Start / Stop ---------------- */

    /**
     * Start capture/encode/stream after MediaProjection permission has been granted.
     * Must be called from a foreground service context (Android 14+ requirement).
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startWithProjection(resultCode: Int, data: Intent) {
        if (running.getAndSet(true)) return

        // 1) Projection
        val mpm = appContext.getSystemService(MediaProjectionManager::class.java)
        projection = mpm.getMediaProjection(resultCode, data)

        // 2) Playback capture (system audio)
        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
            .build()

        val config = AudioPlaybackCaptureConfiguration.Builder(projection!!)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT
        )
        recorder = AudioRecord.Builder()
            .setAudioFormat(audioFormat)
            .setAudioPlaybackCaptureConfig(config)
            .setBufferSizeInBytes((minBuf * 3).coerceAtLeast(24_576))
            .build()

        // 3) AAC encoder
        val aacFormat = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, CHANNELS
        ).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 160_000)
            try { setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR) } catch (_: Throwable) {}
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }
        encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
            configure(aacFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            start()
        }

        // 4) Sockets (audio + control)
        audioSocket = DatagramSocket(UDP_PORT).apply {
            reuseAddress = true
            receiveBufferSize = 1_048_576
            soTimeout = 500
        }
        ctrlSocket = DatagramSocket(UDP_PORT + NetProtocol.CONTROL_OFFSET).apply {
            reuseAddress = true
            receiveBufferSize = 262_144
            soTimeout = 500
        }

        // 5) Start streams
        recorder?.startRecording()

        // Control loop (HELLO/GOODBYE/PONG)
        ctrlJob = scope.launch(Dispatchers.IO) { controlLoop() }

        // Periodic PING keep-alive (every 3s)
        pingJob = scope.launch(Dispatchers.IO) {
            val s = ctrlSocket ?: return@launch
            while (coroutineContext.isActive && running.get()) {
                val now = System.currentTimeMillis()
                val pkt = ByteBuffer.allocate(1 + 8)
                    .put(NetProtocol.OP_PING)
                    .putLong(now)
                    .array()
                for (l in listeners.all()) {
                    runCatching {
                        s.send(
                            DatagramPacket(
                                pkt, pkt.size,
                                InetSocketAddress(l.addr.address, UDP_PORT + NetProtocol.CONTROL_OFFSET)
                            )
                        )
                    }
                }
                delay(3_000)
            }
        }

        // Encode + fan-out to ENABLED listeners
        encodeJob = scope.launch(Dispatchers.Default) {
            val enc = encoder!!
            val rec = recorder!!
            val info = MediaCodec.BufferInfo()
            val pcmShorts = ShortArray(2048)
            val pcmBytes = ByteArray(pcmShorts.size * 2)
            val seq = AtomicInteger(0)

            while (coroutineContext.isActive && running.get()) {
                val read = rec.read(pcmShorts, 0, pcmShorts.size)
                if (read <= 0) continue

                // optional input level log
                if (seq.get() % 100 == 0) {
                    var s = 0.0; for (i in 0 until read) s += pcmShorts[i]*pcmShorts[i].toDouble()
                    Log.d("EchoLink/CAP", "rms=${"%.0f".format(sqrt(s/read))} read=$read")
                }

                // PCM → bytes
                for (i in 0 until read) {
                    val v = pcmShorts[i].toInt()
                    pcmBytes[i * 2] = (v and 0xFF).toByte()
                    pcmBytes[i * 2 + 1] = ((v ushr 8) and 0xFF).toByte()
                }

                // feed encoder
                val inIx = enc.dequeueInputBuffer(0)
                if (inIx >= 0) {
                    enc.getInputBuffer(inIx)!!.apply { clear(); put(pcmBytes, 0, read * 2) }
                    enc.queueInputBuffer(inIx, 0, read * 2, System.nanoTime() / 1000, 0)
                }

                // drain and send to enabled listeners (with ADTS header)
                var outIx = enc.dequeueOutputBuffer(info, 0)
                while (outIx >= 0 && coroutineContext.isActive && running.get()) {
                    val ob = enc.getOutputBuffer(outIx)!!
                    val aacRaw = ByteArray(info.size)
                    ob.get(aacRaw)

                    val adts = buildAdtsHeader(aacRaw.size)
                    val aac = ByteArray(adts.size + aacRaw.size)
                    System.arraycopy(adts, 0, aac, 0, adts.size)
                    System.arraycopy(aacRaw, 0, aac, adts.size, aacRaw.size)

                    val pkt = ByteBuffer.allocate(1 + 2 + 8 + aac.size)
                        .put(STREAM_MAGIC)
                        .putShort(seq.incrementAndGet().toShort())
                        .putLong(info.presentationTimeUs)
                        .put(aac)
                        .array()

                    val sock = audioSocket ?: break
                    for (l in listeners.enabledList()) {
                        try {
                            sock.send(DatagramPacket(pkt, pkt.size, l.addr))
                        } catch (e: IOException) {
                            Log.w("EchoLink/Send", "drop ${l.addr.hostString}: ${e.message}")
                        }
                    }

                    enc.releaseOutputBuffer(outIx, false)
                    outIx = enc.dequeueOutputBuffer(info, 0)
                }

                // prune stale (30s without PONG/HELLO)
                listeners.stale(30_000).forEach { stale ->
                    Log.d("EchoLink/CTRL", "prune stale ${stale.hostString}")
                    listeners.remove(stale)
                }
            }
        }
    }

    fun stop() {
        running.set(false)

        // Close sockets first (unblock receive), then cancel jobs
        runCatching { ctrlSocket?.close() }
        runCatching { audioSocket?.close() }
        pingJob?.cancel(); ctrlJob?.cancel(); encodeJob?.cancel()
        pingJob = null; ctrlJob = null; encodeJob = null

        // Release media
        runCatching { recorder?.stop() }; runCatching { recorder?.release() }; recorder = null
        runCatching { encoder?.stop() };  runCatching { encoder?.release() };  encoder = null
        runCatching { projection?.stop() }; projection = null
    }

    /* ---------------- Control loop ---------------- */

    /** Handles HELLO / GOODBYE / PONG from receivers on control port. */
    private suspend fun controlLoop() {
        val s = ctrlSocket ?: return
        val buf = ByteArray(1024)

        while (kotlin.coroutines.coroutineContext.isActive && running.get()) {
            try {
                // 1) Wait for a control packet (HELLO / GOODBYE / PONG)
                val dp = DatagramPacket(buf, buf.size)
                s.receive(dp) // times out every ~500 ms (so stop() can exit loop)

                // 2) Parse opcode
                val bb = ByteBuffer.wrap(dp.data, 0, dp.length)
                val op: Byte = bb.get()  // first byte = opcode
                val addr = InetSocketAddress(dp.address, UDP_PORT) // we always stream audio to UDP_PORT

                when (op) {
                    NetProtocol.OP_HELLO -> {
                        // Payload: [2 bytes nameLen][nameBytes...]
                        val nameLen = (bb.short.toInt() and 0xFFFF)
                        val name = if (nameLen in 1..bb.remaining()) {
                            val b = ByteArray(nameLen); bb.get(b); String(b, StandardCharsets.UTF_8)
                        } else ""

                        // Add/update this listener (also refreshes lastSeenMs)
                        listeners.upsert(addr, name.ifBlank { null })

                        // (Optional) Notify system + refresh UI
                        runCatching {
                            com.anand.echolink.util.HostNotifications.notifyJoin(
                                appContext,
                                if (name.isNotBlank()) name else addr.hostString,
                                addr.hostString
                            )
                        }
                        com.anand.echolink.data.sender.HostBus.updateFrom(listeners.all())

                        Log.d("EchoLink/CTRL", "HELLO ${addr.hostString} (${if (name.isNotBlank()) name else "unknown"})")
                    }

                    NetProtocol.OP_GOODBYE -> {
                        listeners.remove(addr)

                        // (Optional) Notify system + refresh UI
                        runCatching {
                            val display = listeners.all().firstOrNull { it.addr == addr }?.name ?: addr.hostString
                            com.anand.echolink.util.HostNotifications.notifyLeave(appContext, display, addr.hostString)
                        }
                        com.anand.echolink.data.sender.HostBus.updateFrom(listeners.all())

                        Log.d("EchoLink/CTRL", "GOODBYE ${addr.hostString}")
                    }

                    NetProtocol.OP_PONG -> {
                        // Payload: [8 bytes echoMillis]
                        val echo = bb.long
                        val rtt = System.currentTimeMillis() - echo

                        // Update RTT and refresh lastSeenMs (treat as keep-alive)
                        listeners.setRtt(addr, rtt)
                        listeners.upsert(addr, null)

                        // Push to UI
                        com.anand.echolink.data.sender.HostBus.updateFrom(listeners.all())

                        Log.d("EchoLink/CTRL", "PONG ${addr.hostString} rtt=${rtt}ms")
                    }

                    else -> {
                        // Unknown opcode — ignore safely
                        Log.d("EchoLink/CTRL", "Unknown op=${op.toInt()} from ${addr.hostString}")
                    }
                }
            } catch (_: SocketTimeoutException) {
                // Normal idle tick; loop continues
            } catch (e: SocketException) {
                // Usually occurs during shutdown (sockets closed in stop())
                if (running.get()) Log.w("EchoLink/CTRL", "socket: ${e.message}")
                break
            } catch (e: Throwable) {
                Log.w("EchoLink/CTRL", "error: ${e.message}")
            }
        }
    }

    /* ---------------- Helpers ---------------- */

    private fun buildAdtsHeader(aacLen: Int): ByteArray {
        val profile = 2 // AAC LC
        val freqIdx = 3 // 48 kHz
        val chanCfg = 2 // stereo
        val frameLen = aacLen + 7
        return byteArrayOf(
            0xFF.toByte(), 0xF1.toByte(),
            (((profile - 1) shl 6) or (freqIdx shl 2) or ((chanCfg shr 2) and 1)).toByte(),
            (((chanCfg and 3) shl 6) or ((frameLen shr 11) and 3)).toByte(),
            ((frameLen shr 3) and 0xFF).toByte(),
            (((frameLen and 7) shl 5) or 0x1F).toByte(),
            0xFC.toByte()
        )
    }
}
