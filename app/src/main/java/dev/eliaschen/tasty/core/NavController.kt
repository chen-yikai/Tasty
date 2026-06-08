package dev.eliaschen.tasty.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.eliaschen.tasty.core.Screen.Home
import dev.eliaschen.tasty.screen.Auth

enum class Screen { Auth, Home, CheckOut }

object NavController {
    var currentScreen by mutableStateOf(Screen.Auth)

    fun navigate(screen: Screen) {
        currentScreen = screen
    }
}