package dev.eliaschen.tasty.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import dev.eliaschen.tasty.MainActivity
import dev.eliaschen.tasty.R
import dev.eliaschen.tasty.core.OrderStatus
import dev.eliaschen.tasty.order_progress_channel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

private data class SegmentSpec(
    val status: OrderStatus,
    val weight: Int,
    val color: Int
)

private val segmentSpecs = listOf(
    SegmentSpec(OrderStatus.Pending, weight = 30, color = OrderStatus.Pending.colorHex.toInt()),
    SegmentSpec(
        OrderStatus.Preparing,
        weight = 100,
        color = OrderStatus.Preparing.colorHex.toInt()
    ),
    SegmentSpec(
        OrderStatus.Delivering,
        weight = 70,
        color = OrderStatus.Delivering.colorHex.toInt()
    ),
    SegmentSpec(OrderStatus.Completed, weight = 20, color = OrderStatus.Completed.colorHex.toInt())
)

private val segmentRangesByStatus = buildMap {
    var cursor = 0
    segmentSpecs.forEach { spec ->
        val start = cursor
        val end = cursor + spec.weight
        put(spec.status, start..end)
        cursor = end
    }
}
private val maxProgressValue = segmentSpecs.sumOf { it.weight }

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
fun createOrderProgressNotification(
    context: Context,
    currentStatus: OrderStatus,
    trackerProgress: Int = defaultTrackerProgressForStatus(currentStatus)
): Notification.ProgressStyle {
    val segments = segmentSpecs.map { spec ->
        Notification.ProgressStyle.Segment(spec.weight)
            .setColor(spec.color)
    }
    val trackerIconRes = if (currentStatus == OrderStatus.Completed) {
        R.drawable.progress_point_done
    } else {
        R.drawable.progress_point
    }

    return Notification.ProgressStyle()
        .setStyledByProgress(false)
        .setProgress(trackerProgress.coerceIn(0, maxProgressValue))
        .setProgressSegments(segments)
        .setProgressTrackerIcon(Icon.createWithResource(context, trackerIconRes))
}

fun segmentRangeForStatus(status: OrderStatus): IntRange {
    return segmentRangesByStatus[status] ?: 0..maxProgressValue
}

fun defaultTrackerProgressForStatus(status: OrderStatus): Int {
    val range = segmentRangeForStatus(status)
    return ((range.first + range.last) / 2).coerceIn(0, maxProgressValue)
}

@Serializable
data class OrderUpdateMessage(
    val type: String,
    val order: LiveOrderPayload? = null
)

@Serializable
data class LiveOrderPayload(
    val id: Int? = null,
    val status: OrderStatus? = null
)

private val orderUpdateJson = Json {
    ignoreUnknownKeys = true
}

fun parseOrderUpdateMessage(message: String): OrderUpdateMessage? {
    val jsonObject = runCatching {
        orderUpdateJson.decodeFromString<JsonObject>(message)
    }.getOrNull() ?: return null
    val type = jsonObject["type"]?.jsonPrimitive?.contentOrNull ?: return null
    val orderObject = jsonObject["order"]?.jsonObject ?: return null
    val id = orderObject["id"]?.jsonPrimitive?.longOrNull?.toInt()
    val status = when (orderObject["status"]?.jsonPrimitive?.contentOrNull?.lowercase()) {
        "pending" -> OrderStatus.Pending
        "preparing" -> OrderStatus.Preparing
        "delivering" -> OrderStatus.Delivering
        "completed" -> OrderStatus.Completed
        else -> null
    }
    return OrderUpdateMessage(
        type = type,
        order = LiveOrderPayload(
            id = id,
            status = status
        )
    )
}

fun buildOrderProgressNotification(
    context: Context,
    orderId: Int,
    currentStatus: OrderStatus,
    trackerProgress: Int = defaultTrackerProgressForStatus(currentStatus)
): Notification {
    val description = when (currentStatus) {
        OrderStatus.Pending -> "店家已收到您的訂單"
        OrderStatus.Preparing -> "店家正在準備您的餐點"
        OrderStatus.Delivering -> "餐點配送中，請稍候"
        OrderStatus.Completed -> "餐點已送達，祝您用餐愉快"
    }

    val intent = Intent(context, MainActivity::class.java).apply {
        action = MainActivity.ACTION_OPEN_ACCOUNT
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        orderId,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val builder = Notification.Builder(context, order_progress_channel)
        .setSmallIcon(R.drawable.tasty_favicon)
        .setContentTitle("訂單 #$orderId：${currentStatus.displayName}")
        .setContentText(description)
        .setContentIntent(pendingIntent)
        .setOnlyAlertOnce(true)
        .setOngoing(currentStatus != OrderStatus.Completed)
        .setAutoCancel(currentStatus == OrderStatus.Completed)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        builder.setStyle(
            createOrderProgressNotification(
                context = context,
                currentStatus = currentStatus,
                trackerProgress = trackerProgress
            )
        )
    }

    return builder.build()
}
