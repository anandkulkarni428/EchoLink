package com.anand.echolink

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// Screens
import com.anand.echolink.ui.welcome.RolePicker
import com.anand.echolink.ui.host.HostScreen
import com.anand.echolink.ui.receiver.ReceiverScreen
import com.anand.echolink.ui.legal.AttributionsScreen   // ⬅️ add this import

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun AppNav() {
    val nav = rememberNavController()
    MaterialTheme {
        Surface {
            NavHost(navController = nav, startDestination = "role") {

                composable("role") {
                    RolePicker(
                        onChooseSender = { nav.navigate("host") },
                        onChooseReceiver = { nav.navigate("join") },
                        onOpenAttributions = { nav.navigate("attributions") } // ⬅️ new
                    )
                }

                composable("host") {
                    HostScreen()
                }

                composable("join") {
                    ReceiverScreen()
                }

                // ⬇️ New Attributions route
                composable("attributions") {
                    AttributionsScreen(
                        onBack = { nav.popBackStack() }
                    )
                }
            }
        }
    }
}
