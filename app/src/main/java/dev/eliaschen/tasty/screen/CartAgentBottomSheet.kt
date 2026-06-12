package dev.eliaschen.tasty.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.eliaschen.tasty.R
import dev.eliaschen.tasty.core.AgentMessage
import dev.eliaschen.tasty.core.CartItem
import dev.eliaschen.tasty.core.NavController
import dev.eliaschen.tasty.core.NetworkClient
import dev.eliaschen.tasty.core.Screen
import dev.eliaschen.tasty.ui.theme.Orange
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartAgentBottomSheet(
    api: NetworkClient,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val messages = api.agentMessages
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var conversationVersion by remember { mutableStateOf(0) }
    val isCartScreen = NavController.currentScreen == Screen.CheckOut

    fun sendMessage() {
        val text = input.trim()
        if (text.isEmpty() || sending) return
        val requestVersion = conversationVersion

        input = ""
        messages.add(
            AgentMessage(
                author = "user",
                type = "text",
                content = text,
            ),
        )

        sending = true
        scope.launch {
            val response = api.chatWithAgent(messages.toList())
            if (requestVersion != conversationVersion) {
                sending = false
                return@launch
            }

            if (response.isNullOrEmpty()) {
                messages.add(
                    AgentMessage(
                        author = "agent",
                        type = "text",
                        content = "抱歉，我現在無法回覆，請稍後再試。",
                    ),
                )
            } else {
                messages.clear()
                messages.addAll(response)
            }
            sending = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = modifier
                .padding(horizontal = 15.dp, vertical = 10.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                if (!isCartScreen) {
                    IconButton(
                        onClick = {
                            api.isAgentBottomSheetVisible = false
                            NavController.navigate(Screen.CheckOut)
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.icon_cart),
                            contentDescription = "前往購物車",
                            tint = Color.Gray,
                        )
                    }
                }
                IconButton(
                    onClick = {
                        conversationVersion += 1
                        sending = false
                        messages.clear()
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.restart),
                        contentDescription = "清除對話",
                        tint = Color.Gray,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                if (messages.isEmpty()) {
                    Text(
                        text = "開始與TT對話",
                        color = Color.Gray,
                        fontSize = 16.sp,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(messages, key = { index, _ -> index }) { _, message ->
                            AgentMessageBubble(
                                message = message,
                                onAddSuggestionToCart = { suggestion, quantity ->
                                    addSuggestionToCart(api, suggestion, quantity)
                                },
                            )
                        }
                    }
                }
            }

            if (sending) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Orange,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("輸入你的問題") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                )

                Button(
                    onClick = { sendMessage() },
                    enabled = input.trim().isNotEmpty() && !sending,
                    colors = ButtonDefaults.buttonColors(containerColor = Orange),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (sending) "傳送中" else "送出")
                }
            }
        }
    }
}

private data class AgentFoodSuggestion(
    val id: Int,
    val name: String,
    val reason: String,
    val quantity: Int,
)

private fun addSuggestionToCart(
    api: NetworkClient,
    suggestion: AgentFoodSuggestion,
    quantity: Int,
) {
    val currentCount = api.cart.firstOrNull { it.id == suggestion.id }?.count ?: 0
    val nextCount = (currentCount + quantity.coerceIn(1, 5)).coerceIn(1, 5)
    api.cart.removeIf { it.id == suggestion.id }
    api.cart.add(CartItem(id = suggestion.id, count = nextCount))
}

private fun parseFoodSuggestions(content: String): List<AgentFoodSuggestion> {
    if (content.isBlank()) return emptyList()

    val element = runCatching { Json.parseToJsonElement(content) }.getOrNull() ?: return emptyList()
    val rawArray = when (element) {
        is JsonArray -> element
        is JsonObject -> {
            when (val nested = element["items"] ?: element["food_items"]) {
                is JsonArray -> nested
                else -> return emptyList()
            }
        }

        else -> return emptyList()
    }

    return rawArray.mapNotNull { item ->
        val obj = item as? JsonObject ?: return@mapNotNull null
        val id = obj["id"].asIntOrNull() ?: return@mapNotNull null
        val name =
            obj["name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: "商品 #$id"
        val reason = obj["reason"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val quantity = obj["quantity"].asIntOrNull()?.coerceAtLeast(1) ?: 1
        AgentFoodSuggestion(
            id = id,
            name = name,
            reason = reason,
            quantity = quantity,
        )
    }
}

private fun JsonElement?.asIntOrNull(): Int? {
    val primitive = this?.jsonPrimitive ?: return null
    return primitive.intOrNull ?: primitive.contentOrNull?.toIntOrNull()
}

@Composable
private fun AgentMessageBubble(
    message: AgentMessage,
    onAddSuggestionToCart: (AgentFoodSuggestion, Int) -> Unit,
) {
    val isUser = message.author == "user"
    val isSuggestion = !isUser && message.type == "food_items"
    val bubbleColor = if (isUser) Orange else Color(0xFFF4F4F4)
    val textColor = if (isUser) Color.White else Color(0xFF333333)
    val bubbleWidthModifier =
        if (isSuggestion) Modifier.fillMaxWidth() else Modifier.widthIn(max = 300.dp)
    val suggestions = remember(message.content) {
        if (isSuggestion) parseFoodSuggestions(message.content) else emptyList()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .then(bubbleWidthModifier)
                .clip(RoundedCornerShape(14.dp))
                .background(bubbleColor)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (isSuggestion) {
                Text(
                    text = "餐點推薦",
                    color = if (isUser) Color.White.copy(alpha = 0.8f) else Color.Gray,
                    fontSize = 11.sp,
                )
                if (suggestions.isEmpty()) {
                    Text(
                        text = message.content,
                        color = textColor,
                        fontSize = 14.sp,
                    )
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(
                            suggestions,
                            key = { index, suggestion -> "${suggestion.id}-$index" },
                        ) { index, suggestion ->
                            var selectedQuantity by remember(
                                message.content,
                                suggestion.id,
                                suggestion.quantity,
                                index,
                            ) {
                                mutableIntStateOf(suggestion.quantity.coerceIn(1, 5))
                            }
                            var isAddedNotice by remember(
                                message.content,
                                suggestion.id,
                                index,
                            ) {
                                mutableStateOf(false)
                            }
                            Column(
                                modifier = Modifier
                                    .widthIn(min = 190.dp, max = 220.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White)
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "${suggestion.name} x${selectedQuantity}",
                                    color = Color(0xFF333333),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                )
                                if (suggestion.reason.isNotBlank()) {
                                    Text(
                                        text = suggestion.reason,
                                        color = Color(0xFF616161),
                                        fontSize = 12.sp,
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text("數量", color = Color(0xFF616161), fontSize = 12.sp)
                                    Spacer(Modifier.weight(1f))
                                    IconButton(
                                        onClick = {
                                            selectedQuantity =
                                                (selectedQuantity - 1).coerceAtLeast(1)
                                            isAddedNotice = false
                                        },
                                        enabled = selectedQuantity > 1,
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.icon_minus),
                                            contentDescription = "減少建議數量",
                                            modifier = Modifier.size(18.dp),
                                            tint = if (selectedQuantity > 1) Color(0xFF616161) else Color.LightGray,
                                        )
                                    }
                                    Text(
                                        text = selectedQuantity.toString(),
                                        color = Color(0xFF333333),
                                        fontWeight = FontWeight.Bold,
                                    )
                                    IconButton(
                                        onClick = {
                                            selectedQuantity =
                                                (selectedQuantity + 1).coerceAtMost(5)
                                            isAddedNotice = false
                                        },
                                        enabled = selectedQuantity < 5,
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.icon_add),
                                            contentDescription = "增加建議數量",
                                            modifier = Modifier.size(18.dp),
                                            tint = if (selectedQuantity < 5) Color(0xFF616161) else Color.LightGray,
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        onAddSuggestionToCart(suggestion, selectedQuantity)
                                        isAddedNotice = true
                                    },
                                    enabled = !isAddedNotice,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isAddedNotice) Color(0xFF43A047) else Orange,
                                        disabledContainerColor = if (isAddedNotice) Color(0xFF43A047) else Color.Gray.copy(
                                            0.4f
                                        ),
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        if (isAddedNotice) "已加入購物車" else "加入購物車",
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = message.content,
                    color = textColor,
                    fontSize = 14.sp,
                )
            }
        }
    }
}
