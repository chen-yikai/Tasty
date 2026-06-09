package dev.eliaschen.tasty.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.eliaschen.tasty.R
import dev.eliaschen.tasty.core.NavController
import dev.eliaschen.tasty.core.Screen

@Composable
fun CheckoutConfirm(modifier: Modifier = Modifier) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 50.dp)
        ) {
            Image(painterResource(R.drawable.icon_finish), contentDescription = null)
            Text("訂單送出成功", fontWeight = FontWeight.Bold, fontSize = 25.sp)
            Text(
                "你可以在\"我的帳戶\"中即時追蹤這筆訂單的狀態",
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            TextButton(onClick = { NavController.navigate(Screen.Account) }) { Text("追蹤這筆訂單") }
        }
    }
}