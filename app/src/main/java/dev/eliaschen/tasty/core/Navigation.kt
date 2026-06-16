package dev.eliaschen.tasty.core

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface NavAction {
    data class Navigate(val screen: Screen) : NavAction
    data object GoBack : NavAction
    data class Reset(val screen: Screen) : NavAction
}

@Singleton
class NavigationManager @Inject constructor() {
    private val _events = MutableSharedFlow<NavAction>(extraBufferCapacity = 1)
    val events: SharedFlow<NavAction> = _events.asSharedFlow()

    fun navigate(screen: Screen) {
        _events.tryEmit(NavAction.Navigate(screen))
    }

    fun goBack() {
        _events.tryEmit(NavAction.GoBack)
    }

    fun resetTo(screen: Screen) {
        _events.tryEmit(NavAction.Reset(screen))
    }
}

interface NavController {
    val currentScreen: Screen
    fun navigate(screen: Screen)
    fun goBack(): Boolean
    fun resetTo(screen: Screen)
}

val LocalNavController = staticCompositionLocalOf<NavController> {
    error("No NavController provided")
}

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
