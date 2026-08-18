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
import com.example.veiltalk.common.util.CountryCodes
import com.example.veiltalk.common.util.CountryInfo
import com.example.veiltalk.feature.auth.data.dto.AuthResponseDto
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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
    var selectedCountry by remember { mutableStateOf(CountryCodes.countries.find { it.code == "+98" } ?: CountryCodes.countries[0]) }
    var showCountryPicker by remember { mutableStateOf(false) }
    var useCountryCode by remember { mutableStateOf(false) }

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
                        onValueChange = { 
                            identifier = it
                            // اگر ورودی با عدد شروع شود، احتمالاً شماره موبایل است
                            if (it.isNotEmpty() && it.first().isDigit()) {
                                useCountryCode = true
                            } else if (it.contains("@")) {
                                useCountryCode = false
                            }
                        },
                        label = { Text("ایمیل یا شماره موبایل") },
                        placeholder = { Text("example@mail.com یا 9120000000") },
                        leadingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { showCountryPicker = true }) {
                                    Text(if (useCountryCode) "${selectedCountry.flag} ${selectedCountry.code}" else "📧")
                                }
                                if (useCountryCode) {
                                    VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = if (useCountryCode) KeyboardType.Phone else KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (showCountryPicker) {
                        AlertDialog(
                            onDismissRequest = { showCountryPicker = false },
                            title = { Text("انتخاب پیش‌شماره یا ایمیل") },
                            text = {
                                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                    TextButton(
                                        onClick = {
                                            useCountryCode = false
                                            showCountryPicker = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("📧 استفاده از ایمیل", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Start)
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    CountryCodes.countries.forEach { country ->
                                        TextButton(
                                            onClick = {
                                                selectedCountry = country
                                                useCountryCode = true
                                                showCountryPicker = false
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.Start
                                            ) {
                                                Text(country.flag, modifier = Modifier.padding(end = 8.dp))
                                                Text(country.name, modifier = Modifier.weight(1f))
                                                Text(country.code, color = Color.Gray)
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showCountryPicker = false }) {
                                    Text("بستن")
                                }
                            }
                        )
                    }

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
                            val finalIdentifier = if (useCountryCode && identifier.isNotBlank()) {
                                val cleanNumber = identifier.trim().removePrefix("0")
                                "${selectedCountry.code}$cleanNumber"
                            } else {
                                identifier.trim()
                            }

                            if (mode == LoginMode.PASSWORD) {
                                viewModel.loginWithPassword(finalIdentifier, password)
                            } else {
                                viewModel.requestOtpForLogin(finalIdentifier)
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
