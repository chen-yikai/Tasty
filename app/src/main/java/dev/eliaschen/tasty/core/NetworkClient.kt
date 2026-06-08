package dev.eliaschen.tasty.core

import androidx.compose.runtime.mutableStateListOf
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

//const val host = "http://10.0.2.2"
const val apiHostUrl = "https://tasty.skills.eliaschen.dev"

object NetworkClient {
    var cart = mutableStateListOf<CartItem>()

    val ktorClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }

        defaultRequest {
            url(apiHostUrl)
        }
    }
}