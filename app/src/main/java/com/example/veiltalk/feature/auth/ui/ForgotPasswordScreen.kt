package com.example.veiltalk.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.veiltalk.common.util.CountryCodes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onOtpRequested: (identifier: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var identifier by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(CountryCodes.countries.find { it.code == "+98" } ?: CountryCodes.countries[0]) }
    var useCountryCode by remember { mutableStateOf(false) }
    var showCountryPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is AuthEvent.PasswordResetOtpRequested) {
                onOtpRequested(event.identifier)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("فراموشی رمز عبور") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "برای بازیابی رمز عبور، ایمیل یا شماره موبایل خود را وارد کنید تا کد تایید ارسال شود.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (uiState.errorMessage != null) {
                Text(
                    uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                )
                Spacer(Modifier.height(16.dp))
            }

            OutlinedTextField(
                value = identifier,
                onValueChange = { 
                    identifier = it
                    if (it.isNotEmpty() && it.first().isDigit()) useCountryCode = true
                    else if (it.contains("@")) useCountryCode = false
                },
                label = { Text("ایمیل یا شماره موبایل") },
                placeholder = { Text("example@mail.com یا 9120000000") },
                leadingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showCountryPicker = true }) {
                            Text(if (useCountryCode) "${selectedCountry.flag} ${selectedCountry.code}" else "📧")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = if (useCountryCode) KeyboardType.Phone else KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    val finalIdentifier = if (useCountryCode && identifier.isNotBlank()) {
                        val cleanNumber = identifier.trim().removePrefix("0")
                        "${selectedCountry.code}$cleanNumber"
                    } else {
                        identifier.trim()
                    }
                    viewModel.requestPasswordReset(finalIdentifier)
                },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("ارسال کد تایید", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showCountryPicker) {
        AlertDialog(
            onDismissRequest = { showCountryPicker = false },
            title = { Text("انتخاب پیش‌شماره") },
            text = {
                Column {
                    TextButton(onClick = { useCountryCode = false; showCountryPicker = false }) {
                        Text("📧 استفاده از ایمیل")
                    }
                    HorizontalDivider()
                    CountryCodes.countries.forEach { country ->
                        TextButton(onClick = {
                            selectedCountry = country
                            useCountryCode = true
                            showCountryPicker = false
                        }) {
                            Text("${country.flag} ${country.name} (${country.code})")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCountryPicker = false }) { Text("بستن") } }
        )
    }
}
