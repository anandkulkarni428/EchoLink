package com.anand.echolink.ui.host

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anand.echolink.data.audio.PlaybackCaptureService
import com.anand.echolink.data.sender.HostBus
import com.anand.echolink.data.sender.SenderStreamerToggler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HostViewModel(private val appContext: Context) : ViewModel() {
    val isHosting: StateFlow<Boolean> =
        HostBus.isHosting.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val listeners: StateFlow<List<HostListItem>> =
        HostBus.listeners
            .map { list -> list.map { HostListItem(it.name, it.hostString, it.rttMs, it.enabled) } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun startHosting(resultCode: Int, data: Intent) {
        val svc = Intent(appContext, PlaybackCaptureService::class.java).apply {
            action = PlaybackCaptureService.ACTION_START
            putExtra(PlaybackCaptureService.EXTRA_RESULT_CODE, resultCode)
            putExtra(PlaybackCaptureService.EXTRA_RESULT_DATA, data)
        }
        ContextCompat.startForegroundService(appContext, svc)
    }

    fun stopHosting() {
        val svc = Intent(appContext, PlaybackCaptureService::class.java).apply {
            action = PlaybackCaptureService.ACTION_STOP
        }
        appContext.startService(svc)
    }

    fun setEnabled(hostString: String, enabled: Boolean) {
        SenderStreamerToggler.set(hostString, enabled)
    }
}

data class HostListItem(
    val name: String,
    val hostString: String,
    val rttMs: Long?,
    val enabled: Boolean
)
