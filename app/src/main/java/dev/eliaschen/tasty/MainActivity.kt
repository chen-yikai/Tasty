package dev.eliaschen.tasty

import android.Manifest
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.eliaschen.tasty.component.OfflineOverlay
import dev.eliaschen.tasty.core.NavController
import dev.eliaschen.tasty.core.NetworkClient
import dev.eliaschen.tasty.core.NetworkObserver
import dev.eliaschen.tasty.core.Screen
import dev.eliaschen.tasty.screen.Account
import dev.eliaschen.tasty.screen.AuthScreen
import dev.eliaschen.tasty.screen.CheckOut
import dev.eliaschen.tasty.screen.CheckoutConfirm
import dev.eliaschen.tasty.screen.Home
import dev.eliaschen.tasty.service.OrderUpdateNotificationService
import dev.eliaschen.tasty.ui.theme.TastyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        handleNotificationIntent(intent)
        setContent {
            TastyTheme {
                val sharedPreferences = this.getSharedPreferences("app", Context.MODE_PRIVATE)
                val api: NetworkClient = hiltViewModel()
                var init by remember { mutableStateOf(false) }
                val networkObserver = remember { NetworkObserver(this) }
                val isOnline by networkObserver.isOnline.collectAsState(initial = true)

                LaunchedEffect(Unit) {
                    sharedPreferences.getString("token", null)?.let {
                        NavController.screenStack.removeLast()
                        NavController.navigate(Screen.Home)
                    }
                    init = true
                }

                LaunchedEffect(api.token) {
                    val serviceIntent =
                        Intent(this@MainActivity, OrderUpdateNotificationService::class.java)

                    val token = api.token
                    if (token.isNullOrBlank()) {
                        serviceIntent.action = OrderUpdateNotificationService.ACTION_STOP
                        stopService(serviceIntent)
                        return@LaunchedEffect
                    }

                    serviceIntent.action = OrderUpdateNotificationService.ACTION_START
                    serviceIntent.putExtra(OrderUpdateNotificationService.EXTRA_TOKEN, token)
                    startService(serviceIntent)
                }

                BackHandler {
                    if (!NavController.goBack()) {
                        finish()
                    }
                }

                if (init) {
                    if (!isOnline) {
                        OfflineOverlay()
                    } else {
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
                                    Screen.Auth -> AuthScreen()
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent?.action == ACTION_OPEN_ACCOUNT) {
            CoroutineScope(Dispatchers.Main).launch {
                delay(1000)
                NavController.navigate(Screen.Account)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) return
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2)
    }

    companion object {
        const val ACTION_OPEN_ACCOUNT = "dev.eliaschen.tasty.ACTION_OPEN_ACCOUNT"
    }
}
