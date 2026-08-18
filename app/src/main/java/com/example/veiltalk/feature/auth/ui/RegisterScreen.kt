package com.example.veiltalk.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.example.veiltalk.common.util.CountryCodes
import com.example.veiltalk.common.util.CountryInfo

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onSwitchToLogin: () -> Unit,
    onRegistered: (identifier: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(CountryCodes.countries.find { it.code == "+98" } ?: CountryCodes.countries[0]) }
    var showCountryPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is AuthEvent.OtpRequested) onRegistered(event.identifier)
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
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .align(Alignment.CenterHorizontally)
                        .background(Color(0xFF22C55E), shape = androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✨", fontSize = 28.sp)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "ثبت‌نام در پیام‌رسان",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
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

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("نام") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("نام‌خانوادگی") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("شماره موبایل (بدون صفر)") },
                    placeholder = { Text("9120000000") },
                    leadingIcon = {
                        TextButton(onClick = { showCountryPicker = true }) {
                            Text("${selectedCountry.flag} ${selectedCountry.code}")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (showCountryPicker) {
                    AlertDialog(
                        onDismissRequest = { showCountryPicker = false },
                        title = { Text("انتخاب کشور") },
                        text = {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                CountryCodes.countries.forEach { country ->
                                    TextButton(
                                        onClick = {
                                            selectedCountry = country
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

                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("ایمیل (اختیاری)") },
                    placeholder = { Text("you@example.com") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(4.dp))
                Text(
                    "* حداقل یکی از شماره موبایل یا ایمیل الزامی است.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

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

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        val fullPhoneNumber = if (phoneNumber.isNotBlank()) {
                            val cleanNumber = phoneNumber.trim().removePrefix("0")
                            "${selectedCountry.code}$cleanNumber"
                        } else null

                        viewModel.register(
                            firstName.trim(),
                            lastName.trim(),
                            email.trim().ifBlank { null },
                            fullPhoneNumber,
                            password
                        )
                    },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(if (uiState.isLoading) "در حال ثبت اطلاعات..." else "ثبت‌نام")
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("قبلاً ثبت‌نام کرده‌اید؟ ")
                    TextButton(onClick = onSwitchToLogin) {
                        Text("وارد شوید")
                    }
                }
            }
        }
    }
}