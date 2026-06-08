package dev.eliaschen.tasty.screen

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastRoundToInt
import coil3.compose.AsyncImage
import dev.eliaschen.tasty.R
import dev.eliaschen.tasty.component.HeroHeader
import dev.eliaschen.tasty.core.CartItem
import dev.eliaschen.tasty.core.Food
import dev.eliaschen.tasty.core.FoodType
import dev.eliaschen.tasty.core.NavController
import dev.eliaschen.tasty.core.NetworkClient
import dev.eliaschen.tasty.core.Screen
import dev.eliaschen.tasty.core.apiHostUrl
import dev.eliaschen.tasty.core.getFoodTypes
import dev.eliaschen.tasty.core.getFoods
import dev.eliaschen.tasty.ui.theme.Orange
import kotlin.math.roundToInt

@Composable
fun Home(modifier: Modifier = Modifier) {
    val foodTypes = remember { mutableStateListOf<FoodType>() }
    val foods = remember { mutableStateListOf<Food>() }
    var selectedTypeId by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        foodTypes.apply {
            clear()
            addAll(NetworkClient.ktorClient.getFoodTypes())
            selectedTypeId = foodTypes.first().id
        }
    }

    LaunchedEffect(selectedTypeId) {
        foods.clear()
        foods.addAll(NetworkClient.ktorClient.getFoods(selectedTypeId))
    }

    if (foodTypes.isNotEmpty()) {
        val selectedType = foodTypes.first { it.id == selectedTypeId }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(
                bottom = WindowInsets.navigationBars.asPaddingValues()
                    .calculateBottomPadding() + 10.dp
            )
        ) {
            stickyHeader {
                HeroHeader(backgroundImage = "$apiHostUrl/${selectedType.cover}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "歡迎回來 Elias",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Box {
                            val count = NetworkClient.cart.sumOf { it.count }
                            IconButton(onClick = { if (count != 0) NavController.navigate(Screen.CheckOut) }) {
                                Icon(
                                    painterResource(R.drawable.icon_cart),
                                    contentDescription = null, tint = Color.White
                                )
                            }
                            if (count != 0) {
                                Badge(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .graphicsLayer {
                                            translationY = size.height / 2
                                        }) {
                                    Text(count.toString())
                                }
                            }
                        }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 15.dp),
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(foodTypes) {
                            FilterChip(
                                selected = selectedTypeId == it.id,
                                onClick = { selectedTypeId = it.id },
                                label = { Text(it.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.White,
                                    selectedContainerColor = Orange,
                                    selectedLabelColor = Color.White
                                ), border = null
                            )
                        }
                    }
                    Text(
                        selectedType.description,
                        modifier = Modifier.padding(horizontal = 15.dp),
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }
            }
            if (foods.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(500.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("找不到該項目的食品", color = Color.Gray)
                    }
                }
            }
            items(foods, { it.id }) { food ->
                FoodCard(food)
            }
        }
    }
}

@Composable
fun FoodCard(food: Food, price: Float? = null) {
    var quality by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        NetworkClient.cart.firstOrNull { it.id == food.id }?.let {
            quality = it.count
        }
    }

    fun adjustQuality(factor: Int) {
        quality = (quality + factor).coerceIn(0, 5)
        NetworkClient.cart.removeIf { it.id == food.id }
        NetworkClient.cart.add(CartItem(food.id, quality))
        if (quality == 0) {
            NetworkClient.cart.removeIf { it.id == food.id }
        }
    }

    Card(
        modifier = Modifier.padding(horizontal = 10.dp),
        colors = CardDefaults.cardColors(containerColor = Orange.copy(0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .fillMaxHeight()
            ) {
                AsyncImage(
                    "$apiHostUrl/${food.imageUrl}",
                    contentDescription = null,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                )
            }
            Column(
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    food.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Orange
                )
                Text(food.remark)
                Text("$ ${food.price.formattedPrice()}")
                if (price != null) {
                    Text(
                        "小記 $ ${price.formattedPrice()}",
                        color = Orange, fontStyle = FontStyle.Italic
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    if (quality != 0) {
                        if (price == null)
                            IconButton(onClick = { adjustQuality(-1) }) {
                                Icon(
                                    painterResource(R.drawable.icon_minus),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        Text("${if (price != null) "x " else ""}$quality")
                    }
                    if (price == null)
                        IconButton(onClick = { adjustQuality(+1) }) {
                            Icon(
                                painterResource(R.drawable.icon_add),
                                contentDescription = null, modifier = Modifier.size(20.dp)
                            )
                        }
                }
            }
        }
    }
}

fun Float.formattedPrice(): String =
    if (this % 1.0f == 0f) this.roundToInt().toString() else this.toString()