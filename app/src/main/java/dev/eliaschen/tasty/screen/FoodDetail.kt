package dev.eliaschen.tasty.screen

import android.view.MotionEvent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import dev.eliaschen.tasty.R
import dev.eliaschen.tasty.component.QuantityStepper
import dev.eliaschen.tasty.core.CartItem
import dev.eliaschen.tasty.core.Food
import dev.eliaschen.tasty.core.LocalNavController
import dev.eliaschen.tasty.core.LocalSharedTransitionScope
import dev.eliaschen.tasty.core.NetworkClient
import dev.eliaschen.tasty.core.apiHostUrl
import dev.eliaschen.tasty.ui.theme.Orange

@Composable
fun FoodDetail(
    foodId: Int,
    modifier: Modifier = Modifier,
    api: NetworkClient = hiltViewModel(),
) {
    val navController = LocalNavController.current
    val sharedTransitionScope = LocalSharedTransitionScope.current
    var food by remember { mutableStateOf<Food?>(null) }
    var loading by remember { mutableStateOf(true) }
    var quality by remember { mutableStateOf(0) }

    LaunchedEffect(foodId) {
        loading = true
        food = api.getFoodById(foodId)
        loading = false
    }

    LaunchedEffect(api.cart.size, api.cart.toList(), foodId) {
        quality = api.cart.firstOrNull { it.id == foodId }?.count ?: 0
    }

    fun adjustQuality(factor: Int) {
        quality = (quality + factor).coerceIn(0, 15)
        api.cart.removeIf { it.id == foodId }
        if (quality > 0) {
            api.cart.add(CartItem(foodId, quality))
        }
    }

    val resolvedFood = food

    Box(modifier = modifier.fillMaxSize()) {
        if (loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Orange)
            }
        } else if (resolvedFood == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "找不到該食品",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        bottom = WindowInsets.navigationBars.asPaddingValues()
                            .calculateBottomPadding() + 20.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(bottomStart = 50f, bottomEnd = 50f))
                ) {
                    AsyncImage(
                        model = "$apiHostUrl/${resolvedFood.imageUrl}",
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .sharedFoodImage(
                                sharedTransitionScope,
                                "food-image-$foodId",
                                RoundedCornerShape(bottomStart = 50f, bottomEnd = 50f)
                            )
                            .fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.15f))
                    )
                    IconButton(
                        onClick = { navController.goBack() },
                        modifier = Modifier
                            .renderInSharedOverlay(sharedTransitionScope)
                            .statusBarsPadding().padding(start = 15.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.35f))
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        resolvedFood.name,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Orange,
                        modifier = Modifier.sharedFoodTitle(
                            sharedTransitionScope,
                            "food-title-$foodId"
                        )
                    )
                    Text(
                        resolvedFood.remark,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                    Text(
                        "$ ${resolvedFood.price.formattedPrice()}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(30f))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(horizontal = 15.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "數量",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                    QuantityStepper(
                        quantity = quality,
                        onAdjust = ::adjustQuality,
                        modifier = Modifier.sharedFoodTitle(
                            sharedTransitionScope,
                            "food-quality-${food!!.id}"
                        )
                    )
                }
            }
        }
    }
}
