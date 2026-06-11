package dev.eliaschen.tasty.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.eliaschen.tasty.R
import dev.eliaschen.tasty.ui.theme.Orange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun Splash(
    modifier: Modifier = Modifier,
    onSplashFinished: () -> Unit = {}
) {
    val view = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeight = with(density) { view.screenHeightDp.dp.toPx() }
    val animateLogo = remember { Animatable(0f) }
    val animateIconShake = remember { Animatable(0f) }
    val animateTitle = remember { Animatable(0f) }
    val animateButton = remember { Animatable(1f) }
    val animateRingRaise = remember { Animatable(0f) }
    val animateExit = remember { Animatable(0f) }
    var splashFinish by remember { mutableStateOf(false) }
    var isExiting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        animateLogo.animateTo(1f, tween(2000))
        launch {
            animateTitle.animateTo(1f, tween(1000))
        }
        launch {
            animateButton.animateTo(0f, tween(1000))
        }
        delay(1000)
        splashFinish = true
    }

    LaunchedEffect(splashFinish, isExiting) {
        while (splashFinish && !isExiting) {
            launch {
                animateRingRaise.animateTo(1f, spring(Spring.DampingRatioMediumBouncy))
            }
            val duration = 20
            val repeatCount = 15

            repeat(repeatCount) {
                animateIconShake.animateTo(5f, tween(duration))
                animateIconShake.animateTo(-5f, tween(duration))
            }

            animateIconShake.animateTo(5f, tween(duration))
            animateIconShake.animateTo(0f, tween(duration))
            launch {
                animateRingRaise.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
            }
            delay(3000)
        }
    }

    fun onStartClick() {
        if (isExiting) return
        isExiting = true
        scope.launch {
            // Reverse animations: button, title, logo
            launch {
                animateButton.animateTo(1f, tween(500))
            }
            launch {
                animateTitle.animateTo(0f, tween(500))
            }
            launch {
                animateRingRaise.snapTo(0f)
            }
            delay(300)
            launch {
                animateLogo.animateTo(0f, tween(800))
            }
            launch {
                animateExit.animateTo(1f, tween(800))
            }
            delay(500)
            onSplashFinished()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.graphicsLayer {
                alpha = animateTitle.value
                translationY = animateTitle.value * (size.height / 2) + 50f
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Tasty", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Medium)
            Text("盡情享受你喜愛的美食", color = Color.White.copy(0.9f), fontSize = 20.sp)
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    translationY =
                        -(screenHeight / 2 + size.height) * (1f - animateLogo.value) + -animateTitle.value * size.height / 2
                    rotationZ = (1f - animateLogo.value) * 90f
                }
                .background(Color(0xFFFFD6AC).copy(0.9f), CircleShape)
                .padding(15.dp), contentAlignment = Alignment.Center
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.graphicsLayer {
                translationY = -size.height / 8
            }) {
                Image(
                    painterResource(R.drawable.ding),
                    contentDescription = null,
                    modifier = Modifier.graphicsLayer {
                        translationY = -animateRingRaise.value * 20f
                        rotationZ = animateIconShake.value
                        transformOrigin = TransformOrigin(0.5f, 0.2f)
                    })
                Image(
                    painterResource(R.drawable.plate),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .graphicsLayer {
                            translationY = -size.height
                        }

                )
            }
        }
        Button(
            onClick = { onStartClick() },
            modifier = Modifier
                .graphicsLayer {
                    translationY = animateButton.value * size.height
                }
                .fillMaxWidth()
                .padding(15.dp)
                .navigationBarsPadding()
                .align(Alignment.BottomCenter),
            colors = ButtonDefaults.buttonColors(containerColor = Orange),
            contentPadding = PaddingValues(vertical = 20.dp)
        ) {
            Text("開始體驗", fontSize = 15.sp)
        }
    }
}