package dev.eliaschen.tasty.screen

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.eliaschen.tasty.R
import dev.eliaschen.tasty.ui.theme.Orange

@Composable
fun Auth(modifier: Modifier = Modifier) {
    var isSignUpMode by remember { mutableStateOf(false) }

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = Orange.copy(alpha = 0.5f),
        focusedBorderColor = Orange,
        unfocusedLabelColor = Color.Gray,
        focusedLabelColor = Orange
    )

    val customShape = RoundedCornerShape(30f)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.foods),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(Color.White, customShape)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column {
                Text(
                    text = if (isSignUpMode) "成為會員" else "歡迎回來",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isSignUpMode) "只需花幾分鐘填寫下方資料即可成為會員" else "請輸入您的帳號密碼進行登入",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            if (isSignUpMode) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("使用者名稱") },
                    placeholder = { Text("請輸入使用者名稱") },
                    shape = customShape,
                    colors = textFieldColors,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("電子郵件") },
                placeholder = { Text("example@mail.com") },
                shape = customShape,
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密碼") },
                placeholder = { Text("請輸入密碼") },
                visualTransformation = PasswordVisualTransformation(),
                shape = customShape,
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = customShape,
                colors = ButtonDefaults.buttonColors(containerColor = Orange)
            ) {
                Text(
                    text = if (isSignUpMode) "註冊" else "登入",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSignUpMode) "已經有帳號了？ " else "還沒有帳號嗎？ ",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Text(
                    text = if (isSignUpMode) "點我登入" else "點我註冊",
                    color = Orange,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        isSignUpMode = !isSignUpMode
                    }
                )
            }
        }
    }
}