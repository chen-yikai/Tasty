package dev.eliaschen.tasty.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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

    fun goBack(): Boolean {
        if (screenStack.size <= 1) return false
        screenStack.removeAt(screenStack.lastIndex)
        currentScreen = screenStack.last()
        return true
    }
}