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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.veiltalk.feature.auth.data.dto.AuthResponseDto

@Composable
fun OtpVerifyScreen(
    identifier: String,
    viewModel: AuthViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onVerified: (AuthResponseDto) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var code by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is AuthEvent.Authenticated) onVerified(event.auth)
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
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFF3B82F6), shape = androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔐", fontSize = 28.sp)
                }
                Spacer(Modifier.height(12.dp))
                Text("تایید کد", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "کد ۶ رقمی ارسال‌شده به $identifier را وارد کنید",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                if (uiState.errorMessage != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        uiState.errorMessage!!,
                        color = Color(0xFFDC2626),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEE2E2), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    )
                }
                if (uiState.resendMessage != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        uiState.resendMessage!!,
                        color = Color(0xFF16A34A),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) code = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center
                    ),
                    placeholder = { Text("------", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { viewModel.verifyOtp(identifier, code) },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(if (uiState.isLoading) "در حال بررسی..." else "تایید")
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onBack) { Text("بازگشت") }
                    TextButton(onClick = { viewModel.resendOtp(identifier) }) {
                        Text("ارسال مجدد کد")
                    }
                }
            }
        }
    }
}