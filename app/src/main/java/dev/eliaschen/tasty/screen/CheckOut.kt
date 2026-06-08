package dev.eliaschen.tasty.screen

import androidx.compose.animation.VectorConverter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastRoundToInt
import dev.eliaschen.tasty.R
import dev.eliaschen.tasty.component.HeroHeader
import dev.eliaschen.tasty.core.Food
import dev.eliaschen.tasty.core.NavController
import dev.eliaschen.tasty.core.NetworkClient
import dev.eliaschen.tasty.core.Payment
import dev.eliaschen.tasty.core.Screen
import dev.eliaschen.tasty.core.apiHostUrl
import dev.eliaschen.tasty.core.getFoodById
import dev.eliaschen.tasty.ui.theme.Orange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckOut(modifier: Modifier = Modifier) {
    var address by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showPayment by remember { mutableStateOf(false) }
    var payment by remember { mutableStateOf(Payment.Cash) }
    val cartItems = remember { mutableStateListOf<Food>() }
    var totalPrice by remember { mutableStateOf(0f) }

    LaunchedEffect(NetworkClient.cart.toString()) {
        cartItems.clear()
        totalPrice = 0f
        NetworkClient.cart.forEach {
            cartItems.add(NetworkClient.ktorClient.getFoodById(it.id))
            totalPrice += it.count * cartItems.last().price
        }
    }

    Column {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            ), modifier = Modifier.weight(1f)
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
                            IconButton(onClick = { NavController.navigate(Screen.Home) }) {
                                Icon(
                                    painterResource(R.drawable.arrow_back),
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                            Text(
                                "購物清單",
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        IconButton(onClick = {}) {
                            Icon(
                                painterResource(R.drawable.clear),
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                address,
                                { address = it },
                                modifier = Modifier
                                    .weight(2f),
                                placeholder = { Text("住址") },
                                shape = RoundedCornerShape(30f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = Color.White.copy(
                                        0.5f
                                    ),
                                    focusedContainerColor = Color.White.copy(0.8f),
                                    unfocusedBorderColor = Orange.copy(0.5f),
                                    focusedBorderColor = Orange,
                                ),
                            )
                            ExposedDropdownMenuBox(
                                showPayment,
                                { showPayment = !showPayment },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    payment.displayName,
                                    { },
                                    placeholder = { Text("支付方式") },
                                    shape = RoundedCornerShape(30f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedContainerColor = Color.White.copy(
                                            0.5f
                                        ),
                                        focusedContainerColor = Color.White.copy(0.8f),
                                        unfocusedBorderColor = Orange.copy(0.5f),
                                        focusedBorderColor = Orange,
                                    ),
                                    readOnly = true, modifier = Modifier.menuAnchor(
                                        ExposedDropdownMenuAnchorType.PrimaryEditable
                                    )
                                )
                                DropdownMenu(showPayment, { showPayment = false }) {
                                    Payment.entries.forEach {
                                        DropdownMenuItem({ Text(it.displayName) }, {
                                            payment = it
                                            showPayment = false
                                        })
                                    }
                                }
                            }
                        }
                        OutlinedTextField(
                            note,
                            { note = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("註記") },
                            shape = RoundedCornerShape(30f),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.White.copy(
                                    0.5f
                                ),
                                focusedContainerColor = Color.White.copy(0.8f),
                                unfocusedBorderColor = Orange.copy(0.5f),
                                focusedBorderColor = Orange,
                            ),
                        )
                    }
                }
            }
            items(cartItems, { it.id }) { food ->
                val subTotal = food.price * NetworkClient.cart.first { it.id == food.id }.count
                FoodCard(food, subTotal)
            }
        }
        Row(
            modifier = Modifier
                .shadow(20.dp)
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .fillMaxWidth()
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("總金額", fontSize = 15.sp, color = Color.Gray)
                Text(
                    "$ ${totalPrice.formattedPrice()}",
                    color = Orange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 25.sp
                )
            }
            Button(
                onClick = {

                },
                colors = ButtonDefaults.buttonColors(containerColor = Orange),
                shape = RoundedCornerShape(30f)
            ) {
                Text("送出訂單")
            }
        }
    }
}