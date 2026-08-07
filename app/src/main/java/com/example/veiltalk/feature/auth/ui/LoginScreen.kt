package com.example.veiltalk.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
            .background(Color(0xFFF3F4F6)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .align(Alignment.CenterHorizontally)
                        .background(Color(0xFF3B82F6), shape = androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💬", fontSize = 28.sp)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "ورود به پیام‌رسان",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = { mode = LoginMode.PASSWORD },
                        modifier = Modifier.weight(1f)
                    ) { Text("ورود با رمز عبور") }
                    TextButton(
                        onClick = { mode = LoginMode.OTP },
                        modifier = Modifier.weight(1f)
                    ) { Text("ورود با کد یکبار مصرف") }
                }

                if (uiState.errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        uiState.errorMessage!!,
                        color = Color(0xFFDC2626),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEE2E2), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text("ایمیل یا شماره موبایل") },
                    placeholder = { Text("+989120000000 یا you@example.com") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (mode == LoginMode.PASSWORD) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("رمز عبور") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Spacer(Modifier.height(20.dp))

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
                        .height(48.dp)
                ) {
                    Text(
                        if (uiState.isLoading) "در حال ارتباط با سرور..."
                        else if (mode == LoginMode.PASSWORD) "ورود" else "ارسال کد تایید"
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("حساب کاربری ندارید؟ ")
                    TextButton(onClick = onSwitchToRegister) {
                        Text("ثبت‌نام کنید")
                    }
                }
            }
        }
    }
}