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
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.hostWithPort
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.coroutines.coroutineContext

//const val apiHostUrl = "http://10.0.2.2:3000"
const val apiHostUrl = "https://tasty.skills.eliaschen.dev"

@HiltViewModel
class NetworkClient @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    var token by mutableStateOf<String?>(null)
    var username by mutableStateOf<String?>(null)
    var email by mutableStateOf<String?>(null)
    var address by mutableStateOf("")
    var cart = mutableStateListOf<CartItem>()
    var pendingOrder by mutableStateOf<Order?>(null)
    var agentMessages = mutableStateListOf<AgentMessage>()
    var isAgentBottomSheetVisible by mutableStateOf(false)

    private val sharedPreferences = context.getSharedPreferences("app", Context.MODE_PRIVATE)

    val ktor = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
        install(WebSockets) {
            pingInterval = 10000
        }
        defaultRequest {
            url(apiHostUrl)
            contentType(ContentType.Application.Json)
        }
    }

    init {
        token = sharedPreferences.getString("token", null)
        username = sharedPreferences.getString("username", null)
        email = sharedPreferences.getString("email", null)
        address = sharedPreferences.getString("address", "") ?: ""
    }

    fun writeAuthData() {
        if (token != null) {
            sharedPreferences.edit {
                putString("token", token)
                putString("username", username)
                putString("email", email)
                putString("address", address)
            }
        }
    }

    fun updateAddress(newAddress: String) {
        address = newAddress
        sharedPreferences.edit {
            putString("address", newAddress)
        }
    }

    suspend fun getFoodTypes(): List<FoodType> =
        runCatching { ktor.get("/api/types").body<List<FoodType>>() }.getOrElse { emptyList() }

    suspend fun getFoods(foodType: Int?): List<Food> =
        runCatching {
            ktor.get("/api/foods") { foodType?.let { header("type", foodType) } }.body<List<Food>>()
        }.getOrElse { emptyList() }

    suspend fun getFoodById(id: Int): Food? =
        runCatching { ktor.get("/api/foods/${id}").body<Food>() }.getOrNull()

    suspend fun signIn(email: String, password: String): String? {
        val body = mapOf(
            "email" to email,
            "password" to password
        )
        val res = runCatching {
            ktor.post("/api/sign-in") {
                setBody(body)
            }
        }.getOrElse {
            return "無法連線到伺服器，請確認網路後再試"
        }

        if (res.status.isSuccess()) {
            val resBody: JsonObject = res.body()
            token = resBody["token"]?.jsonPrimitive?.contentOrNull
            username = resBody["username"]?.jsonPrimitive?.contentOrNull
            this.email = resBody["email"]?.jsonPrimitive?.contentOrNull ?: email
            address = resBody["address"]?.jsonPrimitive?.contentOrNull ?: address
            writeAuthData()
            NavController.navigate(Screen.Home)
            return null
        } else {
            val error = runCatching {
                res.body<JsonObject>()["error"]?.jsonPrimitive?.contentOrNull
            }.getOrNull()
            return if (error !== null) error else "登入失敗"
        }
    }

    suspend fun signUp(username: String, email: String, password: String): String? {
        val body = mapOf(
            "username" to username,
            "email" to email,
            "password" to password
        )
        val res = runCatching {
            ktor.post("/api/sign-up") { setBody(body) }
        }.getOrElse {
            return "無法連線到伺服器，請確認網路後再試"
        }

        if (res.status.isSuccess()) {
            signIn(email, password)
            return null
        } else {
            val error = runCatching {
                res.body<JsonObject>()["error"]?.jsonPrimitive?.contentOrNull
            }.getOrNull()
            return if (error !== null) error else "註冊失敗"
        }
    }

    suspend fun placeOrder(order: Order) {
        saveAddressToApi(order.address)
        val res = runCatching {
            ktor.post("/api/order") {
                header("Authorization", "Bearer $token")
                setBody(order)
            }
        }.getOrElse {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "無法送出訂單，請確認網路後再試", Toast.LENGTH_SHORT).show()
            }
            return
        }

        if (!res.status.isSuccess()) {
            val error = runCatching {
                res.body<JsonObject>()["error"]?.jsonPrimitive?.contentOrNull
            }.getOrNull() ?: "送出訂單失敗"
            withContext(Dispatchers.Main) {
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
            return
        }

        Log.i("PlaceOrder", "order submitted")
        cart.clear()
        NavController.navigate(Screen.Account)
    }

    suspend fun saveAddressToApi(newAddress: String): Boolean {
        val normalizedAddress = newAddress.trim()
        updateAddress(normalizedAddress)

        val authToken = token
        if (normalizedAddress.isEmpty() || authToken.isNullOrBlank()) return false

        val endpoints = listOf("/api/user/address", "/api/account/address")
        endpoints.forEach { path ->
            val res = runCatching {
                ktor.put(path) {
                    header("Authorization", "Bearer $authToken")
                    setBody(UpdateAddressRequest(address = normalizedAddress))
                }
            }.getOrNull() ?: return@forEach

            if (res.status.isSuccess()) {
                return true
            }

            if (res.status.value != 404 && res.status.value != 405) {
                return false
            }
        }

        return false
    }

    suspend fun chatWithAgent(messages: List<AgentMessage>): List<AgentMessage>? {
        val res = try {
            ktor.post("/api/agent/chat") {
                setBody(AgentChatRequest(messages))
            }
        } catch (_: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "無法連線到 TT 助理，請稍後再試", Toast.LENGTH_SHORT).show()
            }
            return null
        }

        if (!res.status.isSuccess()) {
            val error = runCatching {
                res.body<JsonObject>()["error"]?.jsonPrimitive?.contentOrNull
            }.getOrNull() ?: "TT 助理暫時無法使用"
            withContext(Dispatchers.Main) {
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
            return null
        }

        return try {
            res.body<List<AgentMessage>>()
        } catch (_: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "TT 回應格式錯誤，請稍後再試", Toast.LENGTH_SHORT).show()
            }
            null
        }
    }

    suspend fun getPlacedOrders(): List<PlacedOrder> {
        val allOrders =
            runCatching {
                ktor.get("/api/order") {
                    token?.let { header("Authorization", "Bearer $it") }
                }.body<List<PlacedOrder>>()
            }.getOrElse { emptyList() }
        val signedInEmail = email?.trim()?.lowercase().orEmpty()
        if (signedInEmail.isEmpty()) return allOrders

        return allOrders.filter { order ->
            val ownerEmail = (order.userEmail ?: order.email)?.trim()?.lowercase().orEmpty()
            ownerEmail.isEmpty() || ownerEmail == signedInEmail
        }
    }

    suspend fun deletePlacedOrder(orderId: Int): Boolean {
        val res = runCatching {
            ktor.delete("/api/order/$orderId") {
                token?.let { header("Authorization", "Bearer $it") }
            }
        }.getOrElse {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "無法刪除訂單，請確認網路後再試", Toast.LENGTH_SHORT).show()
            }
            return false
        }

        if (!res.status.isSuccess()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "刪除訂單失敗", Toast.LENGTH_SHORT).show()
            }
            return false
        }

        return true
    }

    suspend fun observeOrderUpdates(path: String, onMessage: suspend (String) -> Unit) {
        val wsUrl = "wss://${Url(apiHostUrl).hostWithPort}/ws/$path"
        while (coroutineContext.isActive) {
            runCatching {
                ktor.webSocket(urlString = wsUrl) {
                    incoming.consumeEach { frame ->
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            onMessage(text)
                        }
                    }
                }
            }.onFailure { exception ->
                println("WebSocket error: ${exception.localizedMessage}")
            }
            delay(1000)
        }
    }

    fun signOut() {
        token = null
        username = null
        email = null
        address = ""
        cart.clear()
        agentMessages.clear()
        isAgentBottomSheetVisible = false
        sharedPreferences.edit { clear() }
        NavController.screenStack.clear()
        NavController.screenStack.add(Screen.Auth)
        NavController.navigate(Screen.Auth)
    }
}