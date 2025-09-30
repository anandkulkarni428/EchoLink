package com.anand.echolink

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anand.echolink.ui.EchoLinkTheme
import com.anand.echolink.ui.receiver.ReceiverScreen
import com.anand.echolink.ui.sender.SenderScreen
import com.anand.echolink.ui.shared.RequireAudioPermission
import com.anand.echolink.ui.welcome.RolePicker

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { EchoLinkTheme { AppNav() } }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun App() {
    val nav = rememberNavController()
    val ctx = LocalContext.current

    val darkTheme = isSystemInDarkTheme()
    val colors = if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)

    MaterialTheme(colorScheme = colors) {
        NavHost(navController = nav, startDestination = "role") {
            composable("role") {
                RolePicker(
                    onChooseSender = { nav.navigate("sender") },
                    onChooseReceiver = { nav.navigate("receiver") }
                )
            }
            composable("sender") { RequireAudioPermission { SenderScreen() } }
            composable("receiver") { ReceiverScreen() }
        }
    }
}
