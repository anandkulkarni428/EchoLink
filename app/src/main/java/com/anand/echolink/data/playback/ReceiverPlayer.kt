package com.anand.echolink.data.playback

import android.content.Context
import android.media.*
import android.os.Build
import android.util.Log
import com.anand.echolink.NetConfig.SAMPLE_RATE
import com.anand.echolink.NetConfig.STREAM_MAGIC
import com.anand.echolink.NetConfig.UDP_PORT
import kotlinx.coroutines.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext

/**
 * Robust, low-latency UDP AAC receiver & player.
 * - Binds to UDP_PORT and consumes packets: [STREAM_MAGIC][seq(2)][pts(8)][AAC(ADTS)]
 * - Tries ADTS first; if no PCM appears, switches to RAW AAC with ASC (48kHz/stereo)
 * - Small jitter buffer (3–4 frames) to reduce stalls while keeping latency low
 */
class ReceiverPlayer(private val appContext: Context) {

    companion object {
        private const val TAG = "EchoLink/RX"

        // Audio params
        private const val CHANNELS = 2
        private const val FRAME_SAMPLES = 1024        // AAC-LC frame size
        private const val BYTES_PER_SAMPLE = 2

        // Latency tuning
        private const val TARGET_FRAMES = 3           // try 3..4 (3 ≈ ~64ms, 4 ≈ ~85ms)
        private const val QUEUE_CAP = 64

        // ADTS header sniffing
        private const val ADTS_HDR_MIN = 7
        private const val ADTS_SYNC1: Byte = 0xFF.toByte()
    }

    // ---- State ----
    private val running = AtomicBoolean(false)
    private var sock: DatagramSocket? = null
    private var job: Job? = null

    // Decoder mode flag (Boolean value, NOT a lambda)
    @Volatile private var adtsMode: Boolean = true

    // Guard codec lifecycle to avoid IllegalStateException from races
    private val codecLock = Any()
    @Volatile private var decoder: MediaCodec? = null
    @Volatile private var track: AudioTrack? = null

    // Tiny jitter queue
    private val jitter = ArrayBlockingQueue<ByteArray>(QUEUE_CAP)

    /**
     * Start listening and playing.
     * Call once (e.g., when user taps "Join"). Safe to call again (no-op if already running).
     */
    suspend fun start(bindIp: String = "0.0.0.0", port: Int = UDP_PORT) {
        if (running.getAndSet(true)) return

        // 1) Socket
        val s = DatagramSocket(null).apply {
            reuseAddress = true
            soTimeout = 500
            bind(InetSocketAddress(bindIp, port))
            receiveBufferSize = 512 * 1024
        }
        sock = s
        Log.i(TAG, "Listening on $bindIp:$port")

        // 2) Configure decoder (ADTS first) and low-latency AudioTrack
        configureDecoderLocked(adts = true)
        configureTrackLocked()

        // 3) Loops: network producer + decode/play consumer
        job = CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val netJob = launch { networkLoop(s) }
            val playJob = launch { decodePlayLoop() }
            try {
                netJob.join()
                playJob.join()
            } finally {
                netJob.cancel()
                playJob.cancel()
            }
        }
    }

    /** Stop playback and release resources. Safe to call multiple times. */
    fun stop() {
        running.set(false)
        runCatching { sock?.close() }
        job?.cancel()
        job = null

        synchronized(codecLock) {
            runCatching { track?.stop() }; runCatching { track?.release() }; track = null
            runCatching { decoder?.stop() }; runCatching { decoder?.release() }; decoder = null
        }
        jitter.clear()
    }

    // ---------------- Internals ----------------

    private suspend fun networkLoop(s: DatagramSocket) {
        val buf = ByteArray(64 * 1024)
        var rxCount = 0L
        while (coroutineContext.isActive && running.get()) {
            try {
                val dp = DatagramPacket(buf, buf.size)
                s.receive(dp)
                val bb = ByteBuffer.wrap(dp.data, 0, dp.length)
                if (bb.remaining() < 1 + 2 + 8) continue
                val magic = bb.get()
                if (magic != STREAM_MAGIC) continue
                bb.short // seq (unused)
                bb.long  // pts (unused live)
                val payload = ByteArray(bb.remaining())
                bb.get(payload)

                // Keep queue small to cap latency
                if (!jitter.offer(payload)) {
                    jitter.poll()
                    jitter.offer(payload)
                }

                rxCount++
                if (rxCount % 120 == 0L) {
                    Log.d(TAG, "rx=$rxCount q=${jitter.size} mode=${if (adtsMode) "ADTS" else "RAW"}")
                }
            } catch (_: java.net.SocketTimeoutException) {
                // idle tick
            } catch (e: Throwable) {
                if (running.get()) Log.w(TAG, "net: ${e.message}")
            }
        }
    }

    private suspend fun decodePlayLoop() {
        var lastPcmAt = System.nanoTime()
        var pcmCount = 0L
        val pcmBuf = ByteArray(FRAME_SAMPLES * CHANNELS * BYTES_PER_SAMPLE * 2)

        // Prime a few frames to reach target latency
        while (jitter.size < TARGET_FRAMES && running.get()) delay(2)

        loop@ while (kotlin.coroutines.coroutineContext.isActive && running.get()) {
            val frame = jitter.poll() ?: run { delay(1); continue@loop }

            val dec = synchronized(codecLock) { decoder }
            val trk = synchronized(codecLock) { track }
            if (dec == null || trk == null) { delay(5); continue@loop }

            // --- Feed input safely ---
            val data = if (adtsMode) frame else stripAdtsIfPresent(frame)
            val queued: Boolean = try {
                val inIx = dec.dequeueInputBuffer(0)
                if (inIx >= 0) {
                    val inBuf = dec.getInputBuffer(inIx)
                    if (inBuf == null) {
                        false
                    } else {
                        inBuf.clear()
                        if (inBuf.capacity() < data.size) {
                            Log.w(TAG, "input cap=${inBuf.capacity()} need=${data.size}")
                            dec.queueInputBuffer(inIx, 0, 0, 0, 0) // keep codec ticking
                            false
                        } else {
                            inBuf.put(data)
                            dec.queueInputBuffer(inIx, 0, data.size, System.nanoTime() / 1000, 0)
                            true
                        }
                    }
                } else {
                    false
                }
            } catch (ise: IllegalStateException) {
                if (running.get()) Log.w(TAG, "deqIn ISE; reconfiguring…")
                reconfigureDecoderSafe(fallbackToRaw = true)
                delay(10)
                // We can’t 'continue' inside a try; so just report 'not queued' and loop continues.
                false
            }

            // --- Drain output safely ---
            var producedAnyPcm = false
            try {
                val info = MediaCodec.BufferInfo()
                var outIx = dec.dequeueOutputBuffer(info, 0)
                while (outIx >= 0) {
                    val out = dec.getOutputBuffer(outIx)
                    if (out != null && info.size > 0) {
                        val len = info.size.coerceAtMost(pcmBuf.size)
                        out.get(pcmBuf, 0, len)
                        trk.write(pcmBuf, 0, len, AudioTrack.WRITE_BLOCKING)
                        producedAnyPcm = true
                    }
                    dec.releaseOutputBuffer(outIx, false)
                    outIx = dec.dequeueOutputBuffer(info, 0)
                }
            } catch (ise: IllegalStateException) {
                if (running.get()) Log.w(TAG, "deqOut ISE; reconfiguring…")
                reconfigureDecoderSafe(fallbackToRaw = true)
                delay(10)
                continue@loop
            }

            if (producedAnyPcm) {
                pcmCount++
                lastPcmAt = System.nanoTime()
                if (pcmCount % 120 == 0L) Log.d(TAG, "pcm=$pcmCount")
            } else if (queued) {
                val sinceMs = (System.nanoTime() - lastPcmAt) / 1_000_000
                if (sinceMs > 500 && adtsMode) {
                    Log.w(TAG, "No PCM in ${sinceMs}ms with ADTS; switching to RAW")
                    reconfigureDecoderSafe(fallbackToRaw = true)
                }
            }
        }
    }

    // ---------------- Configure / Reconfigure ----------------

    private fun configureTrackLocked() {
        synchronized(codecLock) {
            runCatching { track?.stop() }; runCatching { track?.release() }
            val minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT
            )
            val targetPcmBytes = TARGET_FRAMES * FRAME_SAMPLES * CHANNELS * BYTES_PER_SAMPLE
            val playBuf = targetPcmBytes.coerceAtLeast(minBuf)

            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setFlags(AudioAttributes.FLAG_LOW_LATENCY)
                .build()
            val fmt = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build()

            track = if (Build.VERSION.SDK_INT >= 26) {
                AudioTrack.Builder()
                    .setAudioAttributes(attrs)
                    .setAudioFormat(fmt)
                    .setBufferSizeInBytes(playBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(attrs, fmt, playBuf, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE)
            }
            track?.setVolume(1f)
            track?.play()
        }
    }

    private fun configureDecoderLocked(adts: Boolean) {
        synchronized(codecLock) {
            runCatching { decoder?.stop() }; runCatching { decoder?.release() }
            decoder = null
            adtsMode = adts

            val fmt = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, CHANNELS)
            if (adts) {
                fmt.setInteger(MediaFormat.KEY_IS_ADTS, 1)
                Log.i(TAG, "Decoder configured: ADTS")
            } else {
                // AudioSpecificConfig for AAC LC @ 48kHz stereo: 0x12,0x10
                val asc = byteArrayOf(0x12, 0x10)
                fmt.setByteBuffer("csd-0", ByteBuffer.wrap(asc))
                Log.i(TAG, "Decoder configured: RAW (ASC=0x12,0x10)")
            }

            decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
                configure(fmt, null, null, 0)
                start()
            }
        }
    }

    /** Thread-safe reconfigure; when fallbackToRaw=true switches to RAW. */
    private fun reconfigureDecoderSafe(fallbackToRaw: Boolean) {
        if (!running.get()) return
        val targetAdts = if (fallbackToRaw) false else adtsMode
        configureDecoderLocked(adts = targetAdts)
        // Keep AudioTrack as-is
    }

    // ---------------- Helpers ----------------

    /** If ADTS header present, strip it; otherwise return input. */
    private fun stripAdtsIfPresent(frame: ByteArray): ByteArray {
        if (frame.size < ADTS_HDR_MIN) return frame
        if (frame[0] != ADTS_SYNC1) return frame
        val b1 = frame[1].toInt() and 0xF0
        if (b1 != 0xF0) return frame
        return frame.copyOfRange(ADTS_HDR_MIN, frame.size)
    }
}
