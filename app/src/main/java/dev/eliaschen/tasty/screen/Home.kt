package dev.eliaschen.tasty.screen

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
import dev.eliaschen.tasty.ui.theme.Orange
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Home(modifier: Modifier = Modifier, api: NetworkClient = hiltViewModel()) {
    val foodTypes = remember { mutableStateListOf<FoodType>() }
    val foods = remember { mutableStateListOf<Food>() }
    var selectedTypeId by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }

    suspend fun reloadFoodTypesAndFoods() {
        val latestFoodTypes = api.getFoodTypes()
        val resolvedTypeId = when {
            latestFoodTypes.isEmpty() -> 0
            latestFoodTypes.any { it.id == selectedTypeId } -> selectedTypeId
            else -> latestFoodTypes.first().id
        }
        val latestFoods = if (resolvedTypeId != 0) api.getFoods(resolvedTypeId) else emptyList()

        foodTypes.clear()
        foodTypes.addAll(latestFoodTypes)
        selectedTypeId = resolvedTypeId
        foods.apply {
            clear()
            addAll(latestFoods)
        }
    }

    suspend fun reloadSelectedFoods() {
        if (selectedTypeId == 0) {
            foods.clear()
            return
        }

        loading = true
        val newData = api.getFoods(selectedTypeId)
        loading = false
        foods.clear()
        foods.addAll(newData)
    }

    LaunchedEffect(Unit) {
        loading = true
        reloadFoodTypesAndFoods()
        loading = false

        api.observeOrderUpdates("foods") {
            reloadFoodTypesAndFoods()
        }
    }

    LaunchedEffect(selectedTypeId) {
        if (!loading) {
            reloadSelectedFoods()
        }
    }

    val selectedType = foodTypes.firstOrNull { it.id == selectedTypeId } ?: foodTypes.firstOrNull()

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(
            bottom = WindowInsets.navigationBars.asPaddingValues()
                .calculateBottomPadding() + 10.dp
        )
    ) {
        stickyHeader {
            HeroHeader(backgroundImage = "$apiHostUrl/${selectedType?.cover ?: "foods.jpg"}") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "歡迎回來 ${api.username ?: ""}",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row {
                        IconButton(onClick = { NavController.navigate(Screen.Account) }) {
                            Icon(
                                Icons.Outlined.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(25.dp)
                                    .scale(-1f, 1f),
                                tint = Color.White
                            )
                        }
                        Box {
                            val count = api.cart.sumOf { it.count }
                            IconButton(onClick = { NavController.navigate(Screen.CheckOut) }) {
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
                }
                if (foodTypes.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 15.dp),
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(foodTypes, key = { it.id }) {
                            FilterChip(
                                selected = selectedTypeId == it.id,
                                onClick = { selectedTypeId = it.id },
                                label = { Text(it.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ), border = null
                            )
                        }
                    }
                    Crossfade(selectedType?.description.orEmpty()) {
                        Text(
                            it,
                            modifier = Modifier.padding(horizontal = 15.dp),
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
        if (loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Orange)
                }
            }
        } else if (foods.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("找不到該項目的食品", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(foods, key = { it.id }) { food ->
                FoodCard(food = food, api = api)
            }
        }
    }
}

@Composable
fun FoodCard(
    food: Food,
    price: Float? = null,
    api: NetworkClient = hiltViewModel(),
    enableQuantityAdjust: Boolean = price == null,
) {
    var quality by remember { mutableStateOf(0) }

    LaunchedEffect(api.cart.size, api.cart.toList()) {
        quality = api.cart.firstOrNull { it.id == food.id }?.count ?: 0
    }

    fun adjustQuality(factor: Int) {
        quality = (quality + factor).coerceIn(0, 5)
        api.cart.removeIf { it.id == food.id }
        if (quality > 0) {
            api.cart.add(CartItem(food.id, quality))
        }
    }

    Card(
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .background(
                MaterialTheme.colorScheme.background,
                androidx.compose.foundation.shape.RoundedCornerShape(30f)
            )
            .height(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(bottomEnd = 30f, topEnd = 30f))
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
                    Text(food.remark, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            "$ ${food.price.formattedPrice()}",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (price !== null)
                            Text(
                                "x $quality",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                    }
                    Spacer(Modifier.weight(1f))
                    if (price != null) {
                        Text(
                            "$ ${price.formattedPrice()}",
                            color = Orange, fontStyle = FontStyle.Italic
                        )
                    }
                    if (enableQuantityAdjust)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Spacer(Modifier.weight(1f))
                            if (quality != 0) {
                                IconButton(onClick = { adjustQuality(-1) }) {
                                    Icon(
                                        painterResource(R.drawable.icon_minus),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(quality.toString())
                            }
                            IconButton(onClick = { adjustQuality(+1) }) {
                                Icon(
                                    painterResource(R.drawable.icon_add),
                                    contentDescription = null, modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                }
            }
            if (!food.stock) {
                Box(
                    modifier = Modifier
                        .pointerInput(Unit) {}
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
                )
                Text("已完售", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

fun Float.formattedPrice(): String =
    if (this % 1.0f == 0f) this.roundToInt().toString() else this.toString()