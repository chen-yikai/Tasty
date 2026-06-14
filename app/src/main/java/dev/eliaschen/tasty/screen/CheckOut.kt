package dev.eliaschen.tasty.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.eliaschen.tasty.R
import dev.eliaschen.tasty.component.HeroHeader
import dev.eliaschen.tasty.core.Food
import dev.eliaschen.tasty.core.LocalNavController
import dev.eliaschen.tasty.core.NetworkClient
import dev.eliaschen.tasty.core.Order
import dev.eliaschen.tasty.core.Payment
import dev.eliaschen.tasty.core.Screen
import dev.eliaschen.tasty.core.apiHostUrl
import dev.eliaschen.tasty.ui.theme.Orange

private const val BULK_DISCOUNT_THRESHOLD = 5
private const val BULK_DISCOUNT_RATE = 0.9f

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CheckOut(modifier: Modifier = Modifier, api: NetworkClient = hiltViewModel()) {
    val navController = LocalNavController.current
    var address by remember { mutableStateOf(api.address) }
    var note by remember { mutableStateOf("") }
    var showPayment by remember { mutableStateOf(false) }
    var payment by remember { mutableStateOf(Payment.Cash) }
    var showClearCartDialog by remember { mutableStateOf(false) }

    var addressError by remember { mutableStateOf(false) }

    val cartItems = remember { mutableStateListOf<Food>() }
    var totalPrice by remember { mutableStateOf(0f) }
    var originalTotalPrice by remember { mutableStateOf(0f) }

    LaunchedEffect(api.cart.size, api.cart.toList().hashCode()) {
        var calculatedTotal = 0f
        var calculatedOriginalTotal = 0f

        try {
            val newData = api.cart.map { item ->
                api.getFoodById(item.id)?.let { foodDetails ->
                    val itemOriginal = item.count * foodDetails.price
                    val itemFinal = if (item.count >= BULK_DISCOUNT_THRESHOLD)
                        itemOriginal * BULK_DISCOUNT_RATE else itemOriginal
                    calculatedOriginalTotal += itemOriginal
                    calculatedTotal += itemFinal
                    foodDetails
                }!!
            }.sortedBy { it.name }
            cartItems.clear()
            cartItems.addAll(newData)

            totalPrice = calculatedTotal
            originalTotalPrice = calculatedOriginalTotal
        } catch (e: Exception) {
            e.printStackTrace()

        }
    }

    LaunchedEffect(api.address) {
        if (api.address != address) {
            address = api.address
        }
    }

    fun handlePlaceOrder() {
        if (address.trim().isEmpty()) {
            addressError = true
            return
        }
        if (api.cart.isEmpty()) {
            return
        }

        val finalAddress = address.trim()
        api.updateAddress(finalAddress)
        api.pendingOrder = Order(
            address = finalAddress,
            note = note.trim(),
            payment = payment,
            totalPrice = totalPrice,
            items = api.cart.toList()
        )
        navController.navigate(Screen.CheckOutConfirm)
    }

    Column(modifier = modifier) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ),
            modifier = Modifier.weight(1f)
        ) {
            stickyHeader {
                HeroHeader(backgroundImage = "$apiHostUrl/delivery.jpg") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { navController.navigate(Screen.Home) }) {
                                Icon(
                                    painterResource(R.drawable.arrow_back),
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                            Text(
                                "檢視訂單",
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { api.isAgentBottomSheetVisible = true }) {
                                Icon(
                                    painterResource(R.drawable.wand_spark),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                            IconButton(onClick = {
                                if (api.cart.isNotEmpty()) showClearCartDialog = true
                            }) {
                                Icon(
                                    painterResource(R.drawable.clear),
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = address,
                                onValueChange = {
                                    address = it
                                    api.updateAddress(it)
                                    if (addressError && it.trim().isNotEmpty()) {
                                        addressError = false
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier.weight(2f),
                                placeholder = { Text(if (addressError) "住址不能為空" else "住址") },
                                shape = RoundedCornerShape(30f),
                                isError = addressError,
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(
                                        alpha = 0.72f
                                    ),
                                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(
                                        alpha = 0.92f
                                    ),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(
                                        alpha = 0.6f
                                    ),
                                    focusedBorderColor = Orange,
                                    errorContainerColor = MaterialTheme.colorScheme.surface.copy(
                                        alpha = 0.92f
                                    ),
                                    errorBorderColor = MaterialTheme.colorScheme.error,
                                    errorPlaceholderColor = MaterialTheme.colorScheme.error.copy(
                                        alpha = 0.7f
                                    )
                                ),
                            )
                            ExposedDropdownMenuBox(
                                expanded = showPayment,
                                onExpandedChange = { showPayment = !showPayment },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = payment.displayName,
                                    onValueChange = { },
                                    placeholder = { Text("支付方式") },
                                    shape = RoundedCornerShape(30f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(
                                            alpha = 0.72f
                                        ),
                                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(
                                            alpha = 0.92f
                                        ),
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(
                                            alpha = 0.6f
                                        ),
                                        focusedBorderColor = Orange,
                                    ),
                                    readOnly = true,
                                    modifier = Modifier.menuAnchor(
                                        ExposedDropdownMenuAnchorType.PrimaryEditable
                                    )
                                )
                                DropdownMenu(
                                    expanded = showPayment,
                                    onDismissRequest = { showPayment = false }) {
                                    Payment.entries.forEach { entry ->
                                        DropdownMenuItem(
                                            text = { Text(entry.displayName) },
                                            onClick = {
                                                payment = entry
                                                showPayment = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("備註") },
                            shape = RoundedCornerShape(30f),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(
                                    alpha = 0.72f
                                ),
                                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                focusedBorderColor = Orange,
                            ),
                        )
                    }
                }
            }

            item {
                if (cartItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "購物車目前沒有商品",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp
                        )
                    }
                }
            }
            items(cartItems, key = { it.id }) { food ->
                val cartItem = api.cart.firstOrNull { it.id == food.id }
                val count = cartItem?.count ?: 0
                val rawSubTotal = food.price * count
                val isDiscounted = count >= BULK_DISCOUNT_THRESHOLD
                val subTotal = if (isDiscounted) rawSubTotal * BULK_DISCOUNT_RATE else rawSubTotal
                SwipeToDeleteCheckoutItemCard(
                    food = food,
                    subTotal = subTotal,
                    originalSubTotal = if (isDiscounted) rawSubTotal else null,
                    api = api,
                    onDelete = { targetFood ->
                        api.cart.removeIf { it.id == targetFood.id }
                    }, modifier = Modifier
                        .animateItem()
                )
            }
        }

        Row(
            modifier = Modifier
                .shadow(20.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .fillMaxWidth()
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("總金額", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (originalTotalPrice > totalPrice) {
                    Text(
                        "$ ${originalTotalPrice.formattedPrice()}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough,
                        fontSize = 13.sp
                    )
                }
                Text(
                    "$ ${totalPrice.formattedPrice()}",
                    color = Orange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 25.sp
                )
            }
            Button(
                onClick = {
                    handlePlaceOrder()
                },
                enabled = api.cart.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Orange,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(30f)
            ) {
                Text("送出訂單")
            }
        }
    }

    if (showClearCartDialog) {
        AlertDialog(
            onDismissRequest = { showClearCartDialog = false },
            title = { Text("清空購物車") },
            text = { Text("確定要清空目前購物車嗎？") },
            confirmButton = {
                TextButton(onClick = {
                    showClearCartDialog = false
                    api.cart.clear()
                    navController.navigate(Screen.Home)
                }) {
                    Text("確認", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCartDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteCheckoutItemCard(
    food: Food,
    subTotal: Float,
    originalSubTotal: Float? = null,
    api: NetworkClient,
    onDelete: (Food) -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> distance * 0.8f }
    )

    SwipeToDismissBox(
        state = dismissState, modifier = modifier.animateContentSize(),
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        onDismiss = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete(food)
            }
        },
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp)
                    .clip(RoundedCornerShape(30f))
                    .background(MaterialTheme.colorScheme.error)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onError
                )
            }
        }
    ) {
        FoodCard(
            food = food,
            price = subTotal,
            originalPrice = originalSubTotal,
            api = api,
            enableQuantityAdjust = true,
        )
    }
}
