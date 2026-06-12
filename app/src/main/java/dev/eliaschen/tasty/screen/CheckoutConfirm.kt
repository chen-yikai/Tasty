package dev.eliaschen.tasty.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.eliaschen.tasty.core.NavController
import dev.eliaschen.tasty.core.NetworkClient
import dev.eliaschen.tasty.core.Screen
import dev.eliaschen.tasty.ui.theme.Orange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class ConfirmCartItem(
    val id: Int,
    val name: String,
    val count: Int
)

@Composable
fun CheckoutConfirm(modifier: Modifier = Modifier, api: NetworkClient = hiltViewModel()) {
    val scope = rememberCoroutineScope()
    var progress by remember { mutableFloatStateOf(1f) }
    var showContent by remember { mutableStateOf(false) }
    var countdownActive by remember { mutableStateOf(true) }

    val cartItems = remember { mutableStateListOf<ConfirmCartItem>() }
    val pendingOrder = api.pendingOrder

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 250),
        label = "progress"
    )

    LaunchedEffect(Unit) {
        val loadedItems = api.pendingOrder?.items?.map { item ->
            val foodName = api.getFoodById(item.id)?.name ?: "商品 #${item.id}"
            ConfirmCartItem(item.id, foodName, item.count)
        }.orEmpty()

        cartItems.addAll(loadedItems)
        delay(100)
        showContent = true
    }

    LaunchedEffect(countdownActive) {
        if (!countdownActive) return@LaunchedEffect
        val totalDuration = 8000L
        val interval = 50L
        val steps = totalDuration / interval

        for (i in 0 until steps.toInt()) {
            if (!countdownActive) break
            delay(interval)
            progress = 1f - ((i + 1).toFloat() / steps)
        }

        if (countdownActive) {
            progress = 0f
            api.pendingOrder?.let { api.placeOrder(it) }
            api.pendingOrder = null
        }
    }

    fun skipCountdown() {
        countdownActive = false
        scope.launch {
            api.pendingOrder?.let { api.placeOrder(it) }
            api.pendingOrder = null
        }
    }

    fun cancelAndGoBack() {
        countdownActive = false
        if (!NavController.goBack()) NavController.navigate(Screen.CheckOut)
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(tween(1000)) +
                            slideInVertically(tween(1000)) { -it / 2 }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "再次確認您的訂單",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            color = Orange
                        )
                        Text(
                            text = "訂單將在倒數結束後送出",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = showContent && pendingOrder != null,
                    enter = fadeIn(tween(1000, delayMillis = 1000)) +
                            slideInVertically(tween(1000, delayMillis = 1000)) { -it }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 5.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "配送資訊",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "地址：${pendingOrder?.address?.ifBlank { "-" } ?: "-"}",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "支付方式：${pendingOrder?.payment?.displayName ?: "-"}",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                        if (pendingOrder?.note?.isNotBlank() == true) {
                            Text(
                                text = "備註：${pendingOrder.note}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            itemsIndexed(cartItems, key = { _, item -> item.id }) { index, item ->
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(
                        animationSpec = tween(
                            durationMillis = 500,
                            delayMillis = 2000 + (index * 150)
                        )
                    ) + slideInVertically(
                        animationSpec = tween(
                            durationMillis = 500,
                            delayMillis = 2000 + (index * 150)
                        )
                    ) { -it / 2 }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.name,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(Orange.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "x${item.count}",
                                color = Orange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .shadow(20.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
                .fillMaxWidth()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "正在確認訂單...",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { cancelAndGoBack() }) {
                        Text(
                            "取消",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    TextButton(onClick = { skipCountdown() }) {
                        Text("直接送出", color = Orange, fontWeight = FontWeight.Bold)
                    }
                }
            }

            LinearProgressIndicator(
                progress = { 1f - animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Orange,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
        }
    }
}