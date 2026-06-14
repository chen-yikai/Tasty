package dev.eliaschen.tasty

import android.Manifest
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import dagger.hilt.android.AndroidEntryPoint
import dev.eliaschen.tasty.component.OfflineOverlay
import dev.eliaschen.tasty.core.LocalNavController
import dev.eliaschen.tasty.core.NavAction
import dev.eliaschen.tasty.core.NavigationManager
import dev.eliaschen.tasty.core.NetworkClient
import dev.eliaschen.tasty.core.NetworkObserver
import dev.eliaschen.tasty.core.Screen
import dev.eliaschen.tasty.core.NavController as TastyNavController
import dev.eliaschen.tasty.screen.Account
import dev.eliaschen.tasty.screen.AuthScreen
import dev.eliaschen.tasty.screen.CartAgentBottomSheet
import dev.eliaschen.tasty.screen.CheckOut
import dev.eliaschen.tasty.screen.CheckoutConfirm
import dev.eliaschen.tasty.screen.Home
import dev.eliaschen.tasty.service.OrderUpdateNotificationService
import dev.eliaschen.tasty.ui.theme.TastyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sqrt

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var navigationManager: NavigationManager
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var shakeListener: SensorEventListener? = null
    private var onShakeDetected: (() -> Unit)? = null
    private var lastShakeAt = 0L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleNotificationIntent(intent)
        setupShakeDetector()
        setContent {
            TastyTheme {
                val sharedPreferences = this.getSharedPreferences("app", Context.MODE_PRIVATE)
                val api: NetworkClient = hiltViewModel()
                var init by rememberSaveable { mutableStateOf(false) }
                val networkObserver = remember { NetworkObserver(this) }
                val isOnline by networkObserver.isOnline.collectAsState(initial = true)

                val initialKey = remember {
                    if (sharedPreferences.getString(
                            "token",
                            null
                        ) != null
                    ) Screen.Home else Screen.Auth
                }
                val backStack = rememberNavBackStack(initialKey)
                val navController = remember(backStack) {
                    object : TastyNavController {
                        override val currentScreen: Screen get() = backStack.last() as Screen
                        override fun navigate(screen: Screen) {
                            backStack.add(screen)
                        }

                        override fun goBack(): Boolean {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.size - 1)
                                return true
                            }
                            return false
                        }

                        override fun resetTo(screen: Screen) {
                            backStack.clear()
                            backStack.add(screen)
                        }
                    }
                }

                LaunchedEffect(navigationManager) {
                    navigationManager.events.collect { action ->
                        when (action) {
                            is NavAction.Navigate -> navController.navigate(action.screen)
                            is NavAction.GoBack -> navController.goBack()
                            is NavAction.Reset -> navController.resetTo(action.screen)
                        }
                    }
                }

                val currentScreen = navController.currentScreen
                val canOpenAgentSheet =
                    currentScreen != Screen.Auth && currentScreen != Screen.CheckOutConfirm

                LaunchedEffect(Unit) {
                    init = true
                }

                LaunchedEffect(navController.currentScreen) {
                    if (navController.currentScreen == Screen.Home) requestNotificationPermissionIfNeeded()
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
                DisposableEffect(api, canOpenAgentSheet) {
                    onShakeDetected = {
                        if (canOpenAgentSheet) {
                            api.isAgentBottomSheetVisible = true
                        }
                    }
                    onDispose {
                        onShakeDetected = null
                    }
                }

                LaunchedEffect(canOpenAgentSheet) {
                    if (!canOpenAgentSheet && api.isAgentBottomSheetVisible) {
                        api.isAgentBottomSheetVisible = false
                    }
                }

                BackHandler {
                    if (!navController.goBack()) {
                        finish()
                    }
                }
                CompositionLocalProvider(LocalNavController provides navController) {

                    if (init) {
                        if (!isOnline) {
                            OfflineOverlay()
                        } else {
                            Surface(color = MaterialTheme.colorScheme.background) {
                                NavDisplay(
                                    backStack = backStack,
                                    onBack = { navController.goBack() },
                                    transitionSpec = {
                                        val initial =
                                            initialState.entries.lastOrNull()?.metadata?.get("screen") as? Screen
                                        val target =
                                            targetState.entries.lastOrNull()?.metadata?.get("screen") as? Screen
                                        if (target == null || initial == null || target.order == 0 || target.order == 1) {
                                            fadeIn() togetherWith fadeOut()
                                        } else if (target.order < initial.order) {
                                            fadeIn(tween(500)) togetherWith slideOutHorizontally { it } + fadeOut()
                                        } else {
                                            slideInHorizontally { it } + fadeIn() togetherWith fadeOut()
                                        }
                                    },
                                    popTransitionSpec = {
                                        val initial =
                                            initialState.entries.lastOrNull()?.metadata?.get("screen") as? Screen
                                        val target =
                                            targetState.entries.lastOrNull()?.metadata?.get("screen") as? Screen
                                        if (target == null || initial == null || target.order == 0 || target.order == 1) {
                                            fadeIn() togetherWith fadeOut()
                                        } else if (target.order < initial.order) {
                                            slideInHorizontally { -it / 4 } + fadeIn() togetherWith
                                                slideOutHorizontally { it } + fadeOut()
                                        } else {
                                            slideInHorizontally { it } + fadeIn() togetherWith fadeOut()
                                        }
                                    },
                                    predictivePopTransitionSpec = {
                                        val initial =
                                            initialState.entries.lastOrNull()?.metadata?.get("screen") as? Screen
                                        val target =
                                            targetState.entries.lastOrNull()?.metadata?.get("screen") as? Screen
                                        if (target == null || initial == null || target.order == 0 || target.order == 1) {
                                            fadeIn() togetherWith fadeOut()
                                        } else if (target.order < initial.order) {
                                            slideInHorizontally { -it / 4 } + fadeIn() togetherWith
                                                slideOutHorizontally { it } + fadeOut()
                                        } else {
                                            slideInHorizontally { it } + fadeIn() togetherWith fadeOut()
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                ) { key: NavKey ->
                                    val screen = key as Screen
                                    NavEntry(screen, metadata = mapOf("screen" to screen)) {
                                        Surface(color = MaterialTheme.colorScheme.background) {
                                            when (screen) {
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

                        if (api.isAgentBottomSheetVisible && canOpenAgentSheet) {
                            CartAgentBottomSheet(
                                api = api,
                                onDismiss = { api.isAgentBottomSheetVisible = false },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val listener = shakeListener ?: return
        val sensor = accelerometer ?: return
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onPause() {
        shakeListener?.let { sensorManager.unregisterListener(it) }
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun setupShakeDetector() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        shakeListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val values = event?.values ?: return
                if (values.size < 3) return

                val gX = values[0]
                val gY = values[1]
                val gZ = values[2]
                val gForce = sqrt(gX * gX + gY * gY + gZ * gZ) - SensorManager.GRAVITY_EARTH

                if (gForce < SHAKE_THRESHOLD_GRAVITY) return

                val now = System.currentTimeMillis()
                if (now - lastShakeAt < SHAKE_COOLDOWN_MS) return
                lastShakeAt = now
                onShakeDetected?.invoke()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent?.action == ACTION_OPEN_ACCOUNT) {
            CoroutineScope(Dispatchers.Main).launch {
                delay(1000)
                navigationManager.navigate(Screen.Account)
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) return
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2)
    }

    companion object {
        const val ACTION_OPEN_ACCOUNT = "dev.eliaschen.tasty.ACTION_OPEN_ACCOUNT"
        private const val SHAKE_THRESHOLD_GRAVITY = 3f
        private const val SHAKE_COOLDOWN_MS = 500L
    }
}
