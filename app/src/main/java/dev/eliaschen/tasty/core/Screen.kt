package dev.eliaschen.tasty.core

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen : NavKey {
    val order: Int

    @Serializable data object Auth : Screen { override val order = 1 }
    @Serializable data object Home : Screen { override val order = 2 }
    @Serializable data object CheckOut : Screen { override val order = 3 }
    @Serializable data object CheckOutConfirm : Screen { override val order = 4 }
    @Serializable data object Account : Screen { override val order = 3 }
}
