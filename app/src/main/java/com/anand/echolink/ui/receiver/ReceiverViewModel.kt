package com.anand.echolink.ui.receiver

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anand.echolink.core.AudioLevelBus
import com.anand.echolink.data.net.NsdController
import com.anand.echolink.data.playback.ReceiverPlayer
import com.anand.echolink.data.receiver.ReceiverControlClient
import com.anand.echolink.data.receiver.ReceiverKeepAlive
import com.anand.echolink.domain.StartListening
import com.anand.echolink.domain.StopListening
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReceiverUiState(
    val isListening: Boolean = false,
    val discovered: String = "",
    val error: String? = null
)

class ReceiverViewModel(
    private val appContext: Context,
    private val start: StartListening,
    private val stop: StopListening,
    private val ctl: ReceiverControlClient = ReceiverControlClient(),
    private val keepAlive: ReceiverKeepAlive = ReceiverKeepAlive()
) : ViewModel() {

    private val _state = MutableStateFlow(ReceiverUiState())
    val state: StateFlow<ReceiverUiState> = _state

    private var discoverJob: Job? = null
    private var currentHost: String? = null
    private var currentPort: Int = -1

    val audioLevel: StateFlow<Float> =
        AudioLevelBus.level
            .map { it.coerceIn(0f, 1f) }
            .runningFold(0f) { prev, curr -> prev * 0.7f + curr * 0.3f } // simple low-pass
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    /** Discover host, send HELLO on control port, start player + keep-alive. */
    fun listen() {
        if (_state.value.isListening) return
        _state.value = _state.value.copy(error = null)

        discoverJob?.cancel()
        discoverJob = viewModelScope.launch {
            runCatching {
                // Discover() must invoke with (host, port) of the AUDIO port
                start.discover { host: String, port: Int ->
                    currentHost = host
                    currentPort = port
                    _state.value = _state.value.copy(discovered = "$host:$port")

                    // Send HELLO (includes device name) on CONTROL PORT (UDP_PORT + 1)
                    viewModelScope.launch {
                        runCatching {
                            ctl.sendHello(appContext, host)
                            // small pause lets Host add us before audio arrives
                            delay(100)
                            start.startPlayer(port = port)
                            keepAlive.start(host)      // PONG replies + periodic HELLO
                            _state.value = _state.value.copy(isListening = true)
                        }.onFailure { e ->
                            _state.value = _state.value.copy(error = e.message ?: "Failed to start playback")
                        }
                    }
                }
            }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message ?: "Discovery failed")
            }
        }
    }

    /** Stop audio + keep-alive, send GOODBYE, clear state. */
    fun stopListening() {
        discoverJob?.cancel()
        keepAlive.stop()
        viewModelScope.launch {
            runCatching {
                currentHost?.let { ctl.sendGoodbye(it) }
                stop.stop() // your use-case should stop ReceiverPlayer + NSD if needed
            }.onFailure { e ->
                _state.value = _state.value.copy(error = e.message)
            }.onSuccess {
                _state.value = _state.value.copy(isListening = false)
            }
        }
    }
}

/* ---------- Factory helper for Compose screens ----------

Usage in your ReceiverScreen():
val vm: ReceiverViewModel = viewModel(factory = receiverVmFactory(LocalContext.current.applicationContext))

*/
fun receiverVmFactory(appCtx: Context) = object : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val nsd = NsdController(appCtx)
        val player = ReceiverPlayer(appCtx)
        val start = StartListening(nsd, player)
        val stop = StopListening(nsd, player)
        return ReceiverViewModel(appCtx, start, stop) as T
    }
}
