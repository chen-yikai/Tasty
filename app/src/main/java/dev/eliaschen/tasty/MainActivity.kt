package dev.eliaschen.tasty

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.edit
import dagger.hilt.android.AndroidEntryPoint
import dev.eliaschen.tasty.core.NavController
import dev.eliaschen.tasty.core.NetworkClient
import dev.eliaschen.tasty.core.Screen
import dev.eliaschen.tasty.screen.Auth
import dev.eliaschen.tasty.screen.CheckOut
import dev.eliaschen.tasty.screen.CheckoutConfirm
import dev.eliaschen.tasty.screen.Home
import dev.eliaschen.tasty.ui.theme.TastyTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TastyTheme {
                Crossfade(NavController.currentScreen) {
                    when (it) {
                        Screen.Home -> Home()
                        Screen.CheckOut -> CheckOut()
                        Screen.Auth -> Auth()
                        Screen.CheckOutConfirm -> CheckoutConfirm()
                        else -> {}
                    }
                }
            }
        }
    }
}
