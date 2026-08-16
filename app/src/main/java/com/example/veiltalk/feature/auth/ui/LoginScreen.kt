package com.example.veiltalk.feature.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.veiltalk.feature.auth.data.dto.AuthResponseDto

private enum class LoginMode { PASSWORD, OTP }

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onSwitchToRegister: () -> Unit,
    onOtpRequested: (identifier: String) -> Unit,
    onAuthenticated: (AuthResponseDto) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var mode by remember { mutableStateOf(LoginMode.PASSWORD) }
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AuthEvent.OtpRequested -> onOtpRequested(event.identifier)
                is AuthEvent.Authenticated -> onAuthenticated(event.auth)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // لوگوی متنی در بالای صفحه
            AsyncImage(
                model = "file:///android_asset/logo-text-veil-talk.png",
                contentDescription = "VeilTalk",
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(80.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(32.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    // لوگوی آیکون هود
                    AsyncImage(
                        model = "file:///android_asset/logo-veil-talk.png",
                        contentDescription = null,
                        modifier = Modifier
                            .size(100.dp)
                            .align(Alignment.CenterHorizontally),
                        contentScale = ContentScale.Fit
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        "خوش آمدید",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    
                    Spacer(Modifier.height(24.dp))

                    TabRow(
                        selectedTabIndex = if (mode == LoginMode.PASSWORD) 0 else 1,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {}
                    ) {
                        Tab(
                            selected = mode == LoginMode.PASSWORD,
                            onClick = { mode = LoginMode.PASSWORD },
                            text = { Text("رمز عبور", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = mode == LoginMode.OTP,
                            onClick = { mode = LoginMode.OTP },
                            text = { Text("کد تایید", fontWeight = FontWeight.Bold) }
                        )
                    }

                    if (uiState.errorMessage != null) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            uiState.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    OutlinedTextField(
                        value = identifier,
                        onValueChange = { identifier = it },
                        label = { Text("ایمیل یا شماره موبایل") },
                        placeholder = { Text("+989120000000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (mode == LoginMode.PASSWORD) {
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("رمز عبور") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (mode == LoginMode.PASSWORD) {
                                viewModel.loginWithPassword(identifier.trim(), password)
                            } else {
                                viewModel.requestOtpForLogin(identifier.trim())
                            }
                        },
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                if (mode == LoginMode.PASSWORD) "ورود به حساب" else "ارسال کد تایید",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("حساب کاربری ندارید؟ ", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        TextButton(onClick = onSwitchToRegister) {
                            Text("ثبت‌نام سریع", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
