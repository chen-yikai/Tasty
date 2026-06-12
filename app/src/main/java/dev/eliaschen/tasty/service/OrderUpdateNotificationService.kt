package dev.eliaschen.tasty.service

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import dev.eliaschen.tasty.core.OrderStatus
import dev.eliaschen.tasty.core.PlacedOrder
import dev.eliaschen.tasty.core.apiHostUrl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.hostWithPort
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.coroutines.channels.consumeEach

class OrderUpdateNotificationService : Service() {
    private val trackerAnimationJobs = mutableMapOf<Int, Job>()
    private val latestTrackerProgressByOrder = mutableMapOf<Int, Int>()
    private val latestStatusByOrder = mutableMapOf<Int, OrderStatus>()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var observeJob: Job? = null
    private var activeToken: String? = null
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }

    private val ktor = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(WebSockets) {
            pingInterval = 10000
        }
        defaultRequest {
            url(apiHostUrl)
            contentType(ContentType.Application.Json)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopObserving()
            stopSelf()
            return START_NOT_STICKY
        }

        val token = intent?.getStringExtra(EXTRA_TOKEN).orEmpty()
            .ifBlank { getStoredToken().orEmpty() }
        if (token.isBlank()) {
            stopObserving()
            stopSelf()
            return START_NOT_STICKY
        }

        if (observeJob?.isActive == true && activeToken == token) {
            return START_NOT_STICKY
        }

        activeToken = token
        startObserving(token)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopObserving()
        cancelAllTrackerAnimations()
        serviceScope.cancel()
        ktor.close()
        super.onDestroy()
    }

    private fun startObserving(token: String) {
        stopObserving()
        observeJob = serviceScope.launch {
            observeOrderUpdates(token)
        }
    }

    private fun stopObserving() {
        observeJob?.cancel()
        observeJob = null
        activeToken = null
    }

    private fun cancelAllTrackerAnimations() {
        trackerAnimationJobs.values.forEach { it.cancel() }
        trackerAnimationJobs.clear()
        latestTrackerProgressByOrder.clear()
        latestStatusByOrder.clear()
    }

    private suspend fun observeOrderUpdates(token: String) {
        val wsUrl = "wss://${Url(apiHostUrl).hostWithPort}/ws/orders"
        while (currentCoroutineContext().isActive) {
            runCatching {
                ktor.webSocket(urlString = wsUrl) {
                    incoming.consumeEach { frame ->
                        if (frame !is Frame.Text) return@consumeEach
                        handleOrderUpdate(token, frame.readText())
                    }
                }
            }
            delay(1000)
        }
    }

    private suspend fun handleOrderUpdate(token: String, message: String) {
        val update = parseOrderUpdateMessage(message) ?: return
        val orderId = update.order?.id ?: return

        if (update.type == "deleted") {
            trackerAnimationJobs.remove(orderId)?.cancel()
            latestTrackerProgressByOrder.remove(orderId)
            latestStatusByOrder.remove(orderId)
            notificationManager.cancel(orderId)
            return
        }

        val latestOrder = fetchOrderById(token, orderId) ?: return
        val currentStatus = latestOrder.status ?: return
        if (!hasNotificationPermission()) return
        animateTrackerToStatusTargets(orderId, currentStatus)
    }

    private suspend fun fetchOrderById(token: String, orderId: Int): PlacedOrder? {
        val orders = runCatching {
            ktor.get("/api/order") {
                header("Authorization", "Bearer $token")
            }.body<List<PlacedOrder>>()
        }.getOrElse { emptyList() }

        return orders.firstOrNull { it.id == orderId }
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun getStoredToken(): String? {
        return getSharedPreferences("app", MODE_PRIVATE).getString("token", null)
    }

    private fun animateTrackerToStatusTargets(orderId: Int, currentStatus: OrderStatus) {
        val targets = trackerTargetsForStatus(currentStatus)
        val finalTarget = targets.last()
        val sameStatus = latestStatusByOrder[orderId] == currentStatus
        if (sameStatus && trackerAnimationJobs[orderId]?.isActive == true) return
        if (sameStatus && latestTrackerProgressByOrder[orderId] == finalTarget) return

        val startProgress = latestTrackerProgressByOrder[orderId]
            ?: segmentRangeForStatus(currentStatus).first

        trackerAnimationJobs.remove(orderId)?.cancel()
        trackerAnimationJobs[orderId] = serviceScope.launch {
            latestStatusByOrder[orderId] = currentStatus
            var progress = startProgress.coerceIn(0, 400)
            targets.forEach { target ->
                progress = animateProgressTo(
                    orderId = orderId,
                    currentStatus = currentStatus,
                    startProgress = progress,
                    targetProgress = target
                )
                if (!isActive) return@launch
            }
        }.also { job ->
            job.invokeOnCompletion {
                if (trackerAnimationJobs[orderId] === job) {
                    trackerAnimationJobs.remove(orderId)
                }
            }
        }
    }

    private suspend fun animateProgressTo(
        orderId: Int,
        currentStatus: OrderStatus,
        startProgress: Int,
        targetProgress: Int
    ): Int {
        var progress = startProgress.coerceIn(0, 400)
        val target = targetProgress.coerceIn(0, 400)

        while (currentCoroutineContext().isActive) {
            if (!hasNotificationPermission()) break
            notificationManager.notify(
                orderId,
                buildOrderProgressNotification(
                    context = this@OrderUpdateNotificationService,
                    orderId = orderId,
                    currentStatus = currentStatus,
                    trackerProgress = progress
                )
            )

            latestStatusByOrder[orderId] = currentStatus
            latestTrackerProgressByOrder[orderId] = progress
            if (progress == target) break
            delay(TRACKER_STEP_DELAY_MILLIS)
            progress = if (progress < target) {
                (progress + TRACKER_STEP).coerceAtMost(target)
            } else {
                (progress - TRACKER_STEP).coerceAtLeast(target)
            }
        }
        return progress
    }

    private fun trackerTargetsForStatus(status: OrderStatus): List<Int> {
        return listOf(segmentRangeForStatus(status).last)
    }

    companion object {
        const val ACTION_START = "dev.eliaschen.tasty.service.OrderUpdateNotificationService.START"
        const val ACTION_STOP = "dev.eliaschen.tasty.service.OrderUpdateNotificationService.STOP"
        const val EXTRA_TOKEN = "token"
        private const val TRACKER_STEP = 1
        private const val TRACKER_STEP_DELAY_MILLIS = 16L
    }
}
