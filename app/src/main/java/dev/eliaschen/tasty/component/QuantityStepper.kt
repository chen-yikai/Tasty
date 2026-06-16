package dev.eliaschen.tasty.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.eliaschen.tasty.R

@Composable
fun QuantityStepper(
    quantity: Int,
    onAdjust: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        AnimatedVisibility(quantity != 0, enter = fadeIn(), exit = fadeOut()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onAdjust(-1) }) {
                    Icon(
                        painterResource(R.drawable.icon_minus),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
                AnimatedContent(quantity.toString(), transitionSpec = {
                    val factor = if (targetState > initialState) 1 else -1
                    (fadeIn() + slideInVertically { it * factor } togetherWith fadeOut() + slideOutVertically { -it * factor }).using(
                        SizeTransform(clip = false)
                    )
                }) {
                    Text(
                        it,
                        modifier = Modifier.width(24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        IconButton(onClick = { onAdjust(+1) }) {
            Icon(
                painterResource(R.drawable.icon_add),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
