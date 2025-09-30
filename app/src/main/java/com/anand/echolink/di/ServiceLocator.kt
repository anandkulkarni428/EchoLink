package com.anand.echolink.di

import android.content.Context
import com.anand.echolink.data.audio.PlaybackCaptureController
import com.anand.echolink.data.net.NsdController
import com.anand.echolink.data.playback.ReceiverPlayer
import com.anand.echolink.domain.StartBroadcast
import com.anand.echolink.domain.StopBroadcast
import com.anand.echolink.domain.StartListening
import com.anand.echolink.domain.StopListening

object ServiceLocator {
    fun startBroadcast(ctx: Context) = StartBroadcast(PlaybackCaptureController(ctx), NsdController(ctx))
    fun stopBroadcast(ctx: Context) = StopBroadcast(PlaybackCaptureController(ctx), NsdController(ctx))
    fun startListening(ctx: Context) = StartListening(NsdController(ctx), ReceiverPlayer(ctx))
    fun stopListening(ctx: Context) = StopListening(NsdController(ctx),ReceiverPlayer(ctx))
}
