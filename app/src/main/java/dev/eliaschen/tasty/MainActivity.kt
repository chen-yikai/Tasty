package dev.eliaschen.tasty

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.edit
import dagger.hilt.android.AndroidEntryPoint
import dev.eliaschen.tasty.core.NavController
import dev.eliaschen.tasty.core.NetworkClient
import dev.eliaschen.tasty.core.Screen
import dev.eliaschen.tasty.screen.Account
import dev.eliaschen.tasty.screen.Auth
import dev.eliaschen.tasty.screen.CheckOut
import dev.eliaschen.tasty.screen.CheckoutConfirm
import dev.eliaschen.tasty.screen.Home
import dev.eliaschen.tasty.ui.theme.TastyTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TastyTheme {
                val sharedPreferences = this.getSharedPreferences("app", Context.MODE_PRIVATE)
                var init by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    sharedPreferences.getString("token", null)?.let {
                        NavController.screenStack.removeLast()
                        NavController.navigate(Screen.Home)
                    }
                    init = true
                }

                BackHandler {
                    if (NavController.screenStack.size > 1) {
                        NavController.navigate(NavController.screenStack[NavController.screenStack.lastIndex - 1])
                        NavController.screenStack.removeLast()
                    } else {
                        finish()
                    }
                }

                if (init) {
                    AnimatedContent(
                        NavController.currentScreen,
                        transitionSpec = {
                            if (targetState.order == 0 || targetState.order == 1) {
                                return@AnimatedContent fadeIn() togetherWith fadeOut()
                            }
                            if (targetState.order < initialState.order) {
                                fadeIn(tween(500)) togetherWith slideOutHorizontally { it } + fadeOut()
                            } else {
                                slideInHorizontally { it } + fadeIn() togetherWith ExitTransition.KeepUntilTransitionsFinished
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Surface(color = Color.White) {
                            when (it) {
                                Screen.Home -> Home()
                                Screen.CheckOut -> CheckOut()
                                Screen.Auth -> Auth()
                                Screen.CheckOutConfirm -> CheckoutConfirm()
                                Screen.Account -> Account()
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}
