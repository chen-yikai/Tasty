package dev.eliaschen.tasty.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.eliaschen.tasty.core.Screen.Home
import dev.eliaschen.tasty.screen.Auth

enum class NavigationTransition {

}

enum class Screen(val order: Int) {
    Auth(1), Home(2), CheckOut(3), CheckOutConfirm(4), Account(3), Splash(
        0
    )
}

object NavController {
    val screenStack = mutableStateListOf(Screen.Auth)
    var currentScreen by mutableStateOf(Screen.Auth)

    fun navigate(screen: Screen) {
        currentScreen = screen
        screenStack.add(screen)
    }
}