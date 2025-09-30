package com.anand.echolink.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Simple app-wide bus for current audio RMS level in [0f..1f]. */
object AudioLevelBus {
    private val _level = MutableStateFlow(0f)
    val level: StateFlow<Float> = _level

    /** Clamp and publish. */
    fun publish(rms: Double, norm: Double = 3000.0) {
        val v = (rms / norm).toFloat().coerceIn(0f, 1f)
        _level.value = v
    }
}
