package dev.eliaschen.tasty.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.eliaschen.tasty.R
import dev.eliaschen.tasty.component.HeroHeader
import dev.eliaschen.tasty.core.NavController
import dev.eliaschen.tasty.core.NetworkClient
import dev.eliaschen.tasty.core.OrderStatus
import dev.eliaschen.tasty.core.PlacedOrder
import dev.eliaschen.tasty.core.Screen
import dev.eliaschen.tasty.core.apiHostUrl
import dev.eliaschen.tasty.ui.theme.Orange
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Account(modifier: Modifier = Modifier, api: NetworkClient = hiltViewModel()) {
    val placedOrders = remember { mutableStateListOf<PlacedOrder>() }
    val foodNamesById = remember { mutableStateMapOf<Int, String>() }
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAddressSheet by remember { mutableStateOf(false) }
    var editableAddress by remember { mutableStateOf(api.address) }

    suspend fun reloadAccountOrders() {
        val orders = api.getPlacedOrders()
        val latestFoodNamesById = mutableMapOf<Int, String>()

        orders
            .flatMap { it.items }
            .map { it.id }
            .distinct()
            .forEach { foodId ->
                latestFoodNamesById[foodId] = api.getFoodById(foodId)?.name ?: "商品 #$foodId"
            }

        placedOrders.clear()
        placedOrders.addAll(orders.sortedByDescending { it.createdAt })
        foodNamesById.clear()
        foodNamesById.putAll(latestFoodNamesById)
    }

    LaunchedEffect(Unit) {
        loading = true
        reloadAccountOrders()
        loading = false
        api.observeOrderUpdates("orders") { _ ->
            reloadAccountOrders()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(
            bottom = WindowInsets.navigationBars.asPaddingValues()
                .calculateBottomPadding() + 10.dp
        )
    ) {
        stickyHeader {
            HeroHeader(backgroundImage = "$apiHostUrl/foods.jpg") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { NavController.navigate(Screen.Home) }) {
                            Icon(
                                painterResource(R.drawable.arrow_back),
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        Text(
                            "我的帳戶",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = {
                        editableAddress = api.address
                        showAddressSheet = true
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }

                val username = api.username ?: "Guest"
                val email = api.email ?: "-"
                val avatarChar = username.firstOrNull()?.uppercase() ?: "?"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            username,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 25.sp
                        )
                        Text(
                            email,
                            color = Color.White.copy(0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            avatarChar,
                            color = Orange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 30.sp,
                        )
                    }
                }
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .padding(horizontal = 15.dp, vertical = 10.dp)
                        .fillMaxWidth()
                        .alpha(0.9f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84343)),
                    shape = RoundedCornerShape(30f)
                ) {
                    Text("登出", color = Color.White, fontSize = 15.sp)
                }
            }
        }

        item {
            Text(
                "訂單紀錄",
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 4.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        if (loading) {
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth()
                ) {
                    CircularProgressIndicator(color = Orange)
                }
            }
        } else if (placedOrders.isEmpty()) {
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        "目前沒有訂單",
                        modifier = Modifier.padding(horizontal = 15.dp),
                        color = Color.Gray
                    )
                }
            }
        } else {
            items(placedOrders, key = { it.id }) { order ->
                SwipeToDeleteOrderHistoryCard(
                    order = order,
                    foodNameById = foodNamesById,
                    modifier = Modifier.animateItem(),
                    onDelete = { deletingOrder ->
                        deletingOrder.id.let { deletingOrderId ->
                            scope.launch {
                                val removeIndex =
                                    placedOrders.indexOfFirst { it.id == deletingOrderId }
                                if (removeIndex == -1) return@launch
                                val removedOrder = placedOrders.removeAt(removeIndex)
                                val deleted = api.deletePlacedOrder(deletingOrderId)
                                if (!deleted) {
                                    val restoreIndex = removeIndex.coerceAtMost(placedOrders.size)
                                    placedOrders.add(restoreIndex, removedOrder)
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("確認登出") },
            text = { Text("你確定要登出目前帳號嗎？") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    api.signOut()
                }) {
                    Text("確認", color = Color(0xFFD84343))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showAddressSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddressSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("設定預設地址", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                OutlinedTextField(
                    value = editableAddress,
                    onValueChange = { editableAddress = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("請輸入地址") },
                    singleLine = true,
                    shape = RoundedCornerShape(30f),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFF8F8F8),
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = Orange.copy(0.5f),
                        focusedBorderColor = Orange
                    )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            bottom = WindowInsets.navigationBars.asPaddingValues()
                                .calculateBottomPadding()
                        ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = { showAddressSheet = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            api.updateAddress(editableAddress.trim())
                            showAddressSheet = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Orange)
                    ) {
                        Text("儲存")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteOrderHistoryCard(
    order: PlacedOrder,
    foodNameById: Map<Int, String>,
    onDelete: (PlacedOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(SwipeToDismissBoxValue.Settled) {
        it * 0.8f
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        modifier = modifier.animateContentSize(),
        onDismiss = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete(order)
            }
        },
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFFD84343))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        }
    ) {
        OrderHistoryCard(order = order, foodNameById = foodNameById)
    }
}

@Composable
private fun OrderHistoryCard(order: PlacedOrder, foodNameById: Map<Int, String>) {
    val status = order.status ?: OrderStatus.Pending
    val isCompletedOrder = status == OrderStatus.Completed
    val formattedCreatedAt = formatCreatedAtForSystemPreference(order.createdAt)
    var detailsExpanded by rememberSaveable(order.id, order.createdAt, status) {
        mutableStateOf(!isCompletedOrder)
    }
    val detailsArrowRotation by animateFloatAsState(
        targetValue = if (detailsExpanded) 180f else 0f,
        label = "detailsArrowRotation"
    )
    var itemsExpanded by rememberSaveable(
        order.id,
        order.createdAt,
        "items"
    ) { mutableStateOf(false) }
    val itemsArrowRotation by animateFloatAsState(
        targetValue = if (itemsExpanded) 180f else 0f,
        label = "itemsArrowRotation"
    )
    Card(
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Orange.copy(alpha = 0.15f),
                shape = RoundedCornerShape(22.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCFA)),
        shape = RoundedCornerShape(22.dp),
//        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isCompletedOrder) {
                            Modifier.clickable { detailsExpanded = !detailsExpanded }
                        } else {
                            Modifier
                        }
                    )
                    .padding(15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "訂單 #${order.id ?: "-"}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF4E342E)
                    )
                    formattedCreatedAt?.let {
                        Text(
                            it,
                            color = Color.Gray,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(status.colorHex).copy(alpha = 0.16f))
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            status.displayName,
                            color = Color(status.colorHex),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (isCompletedOrder) {
                        Icon(
                            imageVector = Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .rotate(detailsArrowRotation)
                                .size(20.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(detailsExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .padding(horizontal = 15.dp)
                        .padding(bottom = 15.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OrderMetaBlock(
                            modifier = Modifier.weight(1f),
                            label = "總金額",
                            value = "$ ${order.totalPrice.formattedPrice()}",
                            valueColor = Orange
                        )
                        OrderMetaBlock(
                            modifier = Modifier.weight(1f),
                            label = "支付方式",
                            value = order.payment.displayName,
                            valueColor = Color(0xFF37474F)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFFF4EB))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "配送資訊",
                            color = Color(0xFF795548),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "地址：${order.address.ifBlank { "-" }}",
                            color = Color(0xFF5D4037),
                            fontSize = 13.sp
                        )
                        if (order.note.isNotBlank()) {
                            Text("備註：${order.note}", color = Color(0xFF6D4C41), fontSize = 13.sp)
                        }
                    }

                    if (order.items.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF6F0)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { itemsExpanded = !itemsExpanded }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "購買品項 (${order.items.size})",
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF424242),
                                        fontSize = 13.sp
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.ExpandMore,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier
                                            .rotate(itemsArrowRotation)
                                            .size(18.dp)
                                    )
                                }

                                AnimatedVisibility(itemsExpanded) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(5.dp),
                                        modifier = Modifier.padding(10.dp)
                                    ) {
                                        order.items.forEach { item ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(Color(0xFFF7F7F7))
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    foodNameById[item.id] ?: "商品 #${item.id}",
                                                    color = Color(0xFF455A64),
                                                    fontSize = 12.sp
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(100.dp))
                                                        .background(Orange.copy(alpha = 0.15f))
                                                        .padding(horizontal = 7.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        "x${item.count}",
                                                        color = Orange,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatCreatedAtForSystemPreference(createdAt: String?): String? {
    val value = createdAt?.trim().orEmpty()
    if (value.isBlank()) return null
    val instant = parseCreatedAtInstant(value) ?: return value
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date.from(instant))
}

private fun parseCreatedAtInstant(value: String): Instant? {
    return runCatching { Instant.parse(value) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
        ?: runCatching {
            LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toInstant()
        }.getOrNull()
        ?: listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss.SSS",
            "yyyy/MM/dd HH:mm:ss"
        ).firstNotNullOfOrNull { pattern ->
            runCatching {
                LocalDateTime.parse(value, DateTimeFormatter.ofPattern(pattern))
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
            }.getOrNull()
        }
}

@Composable
private fun OrderMetaBlock(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(30f))
            .background(Color(0xFFFFF1E5))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, color = Color.Gray, fontSize = 12.sp)
        Text(
            value,
            color = valueColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
