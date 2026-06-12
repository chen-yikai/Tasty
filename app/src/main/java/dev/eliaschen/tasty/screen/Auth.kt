package dev.eliaschen.tasty.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ModifierInfo
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.eliaschen.tasty.R
import dev.eliaschen.tasty.core.NetworkClient
import dev.eliaschen.tasty.ui.theme.Orange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun AuthScreen(modifier: Modifier = Modifier, api: NetworkClient = hiltViewModel()) {
    var showSplash by remember { mutableStateOf(true) }
    val animateBackground = remember { Animatable(-1f) }
    val animateSplash = remember { Animatable(0f) }
    val animateOpacity = remember { Animatable(0f) }
    val animateAuthSlide = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        launch {
            animateOpacity.animateTo(1f)
        }
        launch {
            animateSplash.animateTo(1f, tween(1500))
        }
        launch {
            animateBackground.animateTo(0f, tween(1500))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = animateOpacity.value
            }
    ) {
        Image(
            painter = painterResource(R.drawable.foods_horizontal),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize(),
            alignment = BiasAlignment(animateBackground.value, 0f)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(
                        alpha = (1f - animateSplash.value).coerceIn(
                            0.4f,
                            0.7f
                        )
                    )
                )
        )

        if (showSplash) {
            Splash(
                modifier = Modifier.fillMaxSize(),
                onSplashFinished = {
                    scope.launch {
                        launch {
                            animateBackground.animateTo(1f, tween(1000))
                        }
                        launch {
                            animateAuthSlide.animateTo(0f, tween(1000))
                        }
                    }
                    showSplash = false
                }
            )
        } else {
            AuthContent(
                api = api,
                slideOffset = animateAuthSlide.value
            )
        }
    }
}

@Composable
private fun AuthContent(
    modifier: Modifier = Modifier,
    api: NetworkClient,
    slideOffset: Float = 0f
) {
    val focusManager = LocalFocusManager.current
    var isSignUpMode by remember { mutableStateOf(false) }

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var usernameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var apiError by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = Orange.copy(alpha = 0.5f),
        focusedBorderColor = Orange,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedLabelColor = Orange,
        errorBorderColor = MaterialTheme.colorScheme.error,
        errorLabelColor = MaterialTheme.colorScheme.error
    )

    val shakeOffset = remember { Animatable(0f) }

    fun shakeSubmit() {
        scope.launch {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 300
                    -20f at 50
                    20f at 100
                    -15f at 150
                    15f at 200
                    -10f at 250
                }
            )
        }
    }


    val customShape = RoundedCornerShape(30f)
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()

    LaunchedEffect(isSignUpMode) {
        username = ""
        email = ""
        password = ""
        usernameError = null
        emailError = null
        passwordError = null
        apiError = null
        focusManager.clearFocus()
    }

    fun handleAuth() {
        apiError = null
        var isValid = true

        if (isSignUpMode && username.trim().isEmpty()) {
            usernameError = "使用者名稱不能為空"
            isValid = false
        } else {
            usernameError = null
        }

        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty()) {
            emailError = "電子郵件不能為空"
            isValid = false
        } else if (!trimmedEmail.matches(emailRegex)) {
            emailError = "電子郵件格式不正確"
            isValid = false
        } else {
            emailError = null
        }

        if (password.isEmpty()) {
            passwordError = "密碼不能為空"
            isValid = false
        } else if (password.length < 6) {
            passwordError = "密碼長度至少需要 6 個字元"
            isValid = false
        } else {
            passwordError = null
        }

        if (!isValid) {
            shakeSubmit()
            return
        }

        scope.launch {
            if (isSignUpMode) {
                api.signUp(username.trim(), trimmedEmail, password)?.let {
                    shakeSubmit()
                    apiError = it

                }
            } else {
                api.signIn(trimmedEmail, password)?.let {
                    shakeSubmit()
                    apiError = it
                }
            }
        }
    }

    val transitionFactor = if (isSignUpMode) -1 else 1
    val slideUpTransition =
        (slideInVertically() { transitionFactor * it } + fadeIn() togetherWith slideOutVertically() { transitionFactor * -it } + fadeOut())


    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .graphicsLayer {
                    translationX = slideOffset * size.width * 1.5f
                }
                .animateContentSize()
                .fillMaxWidth(0.9f)
                .background(MaterialTheme.colorScheme.surface, customShape)
                .padding(24.dp),
        ) {
            Crossfade(
                targetState = isSignUpMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) { isSignUp ->
                Column {
                    Text(
                        text = if (isSignUp) "成為會員" else "歡迎回來",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isSignUp) "只需花幾分鐘填寫下方資料即可成為會員" else "請輸入您的帳號密碼進行登入",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }

            AnimatedVisibility(isSignUpMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            apiError = null
                            if (usernameError != null) usernameError =
                                null
                        },
                        label = { Text("使用者名稱") },
                        placeholder = { Text("請輸入使用者名稱") },
                        shape = customShape,
                        colors = textFieldColors,
                        isError = usernameError != null,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    usernameError?.let { errorMsg ->
                        Text(
                            text = errorMsg,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            apiError = null
                            if (emailError != null) emailError = null
                        },
                        label = { Text("電子郵件") },
                        placeholder = { Text("example@mail.com") },
                        shape = customShape,
                        colors = textFieldColors,
                        isError = emailError != null,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    emailError?.let { errorMsg ->
                        Text(
                            text = errorMsg,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                        )
                    }
                }

                Column {
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            apiError = null
                            if (passwordError != null) passwordError = null
                        },
                        label = { Text("密碼") },
                        placeholder = { Text("請輸入密碼") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (passwordVisible) "隱藏密碼" else "顯示密碼",
                                    tint = Orange
                                )
                            }
                        },
                        shape = customShape,
                        colors = textFieldColors,
                        isError = passwordError != null,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    passwordError?.let { errorMsg ->
                        Text(
                            text = errorMsg,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                Button(
                    onClick = { handleAuth() },
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = shakeOffset.value
                        }
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = customShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Orange)
                ) {
                    AnimatedContent(isSignUpMode, transitionSpec = { slideUpTransition }) {
                        Text(
                            text = if (it) "註冊" else "登入",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                AnimatedContent(
                    apiError,
                    modifier = Modifier.padding(vertical = 5.dp),
                    transitionSpec = { slideUpTransition }) {
                    it?.let {
                        Text(
                            it,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Crossfade(isSignUpMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (it) "已經有帳號了？ " else "還沒有帳號嗎？ ",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (it) "點我登入" else "點我註冊",
                            color = Orange,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(30f))
                                .clickable { isSignUpMode = !isSignUpMode }
                                .padding(5.dp)
                        )
                    }
                }
            }
        }
    }
}