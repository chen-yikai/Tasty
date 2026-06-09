package dev.eliaschen.tasty.core

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

const val apiHostUrl = "https://tasty.skills.eliaschen.dev"

@HiltViewModel
class NetworkClient @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    var token by mutableStateOf<String?>(null)
    var username by mutableStateOf<String?>(null)
    var cart = mutableStateListOf<CartItem>()

    private val sharedPreferences = context.getSharedPreferences("app", Context.MODE_PRIVATE)

    val ktor = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
        defaultRequest {
            url(apiHostUrl)
            contentType(ContentType.Application.Json)
        }
    }

    init {
        token = sharedPreferences.getString("token", null)
        username = sharedPreferences.getString("username", null)
    }

    fun writeAuthData() {
        if (token != null) {
            sharedPreferences.edit {
                putString("token", token)
                putString("username", username)
            }
        }
    }

    suspend fun getFoodTypes(): List<FoodType> = ktor.get("/api/types").body()

    suspend fun getFoods(foodType: Int?): List<Food> =
        ktor.get("/api/foods") { foodType?.let { header("type", foodType) } }.body()

    suspend fun getFoodById(id: Int): Food = ktor.get("/api/foods/${id}").body()

    suspend fun signIn(email: String, password: String) {
        val body = mapOf(
            "email" to email,
            "password" to password
        )
        val res = ktor.post("/api/sign-in") {
            setBody(body)
        }

        if (res.status.isSuccess()) {
            val resBody: JsonObject = res.body()
            token = resBody["token"]?.jsonPrimitive?.contentOrNull
            username = resBody["username"]?.jsonPrimitive?.contentOrNull
            writeAuthData()
            NavController.navigate(Screen.Home)
        } else {
            showToast("登入失敗")
        }
    }

    suspend fun signUp(username: String, email: String, password: String) {
        val body = mapOf(
            "username" to username,
            "email" to email,
            "password" to password
        )
        val res = ktor.post("/api/sign-up") { setBody(body) }

        if (res.status.isSuccess()) {
            signIn(email, password)
        } else {
            showToast("註冊失敗")
        }
    }

    suspend fun placeOrder(order: Order) {
        val res = ktor.post("/api/order") {
            header("Authorization", "Bearer $token")
            setBody(order)
        }
        Log.i("PlaceOrder", res.body())
        cart.clear()
        NavController.navigate(Screen.CheckOutConfirm)
    }

    fun signOut() {
        sharedPreferences.edit { clear() }
        NavController.navigate(Screen.Auth)
    }

    private suspend fun showToast(message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}