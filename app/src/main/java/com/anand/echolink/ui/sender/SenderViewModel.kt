package com.anand.echolink.ui.sender

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.anand.echolink.data.audio.PlaybackCaptureService

data class SenderUiState(
    val isBroadcasting: Boolean = false,
    val error: String? = null
)

class SenderViewModel(
    private val appCtx: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SenderUiState())
    val state: StateFlow<SenderUiState> = _state

    /** Call after MediaProjection dialog returns OK (resultCode, data). */
    fun startBroadcast(resultCode: Int, data: Intent) {
        runCatching {
            val svc = Intent(appCtx, PlaybackCaptureService::class.java).apply {
                action = PlaybackCaptureService.ACTION_START
                putExtra(PlaybackCaptureService.EXTRA_RESULT_CODE, resultCode)
                putExtra(PlaybackCaptureService.EXTRA_RESULT_DATA, data)
            }
            appCtx.startForegroundService(svc)
            _state.value = _state.value.copy(isBroadcasting = true, error = null)
        }.onFailure { e ->
            _state.value = _state.value.copy(error = e.message)
        }
    }

    fun stopBroadcast() {
        runCatching {
            appCtx.startService(
                Intent(appCtx, PlaybackCaptureService::class.java).apply {
                    action = PlaybackCaptureService.ACTION_STOP
                }
            )
        }
        _state.value = _state.value.copy(isBroadcasting = false)
    }

    override fun onCleared() {
        super.onCleared()
        stopBroadcast()
    }
}
