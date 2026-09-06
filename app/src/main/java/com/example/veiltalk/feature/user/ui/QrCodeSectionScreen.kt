package com.example.veiltalk.feature.user.ui

import android.graphics.Bitmap
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.veiltalk.common.ui.components.AvatarView
import com.example.veiltalk.common.util.QrCodeHelper
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.CompoundBarcodeView

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrCodeSectionScreen(
    displayName: String,
    username: String,
    profilePicture: String?,
    onBack: () -> Unit,
    onScanned: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val qrBitmap = remember(username) { QrCodeHelper.generateQrCode("veiltalk://user/$username") }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("کد QR") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("کد من") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("اسکن کد") }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (selectedTab == 0) {
                    MyQrCodeTab(
                        displayName = displayName,
                        username = username,
                        profilePicture = profilePicture,
                        qrBitmap = qrBitmap,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString("@$username"))
                            android.widget.Toast.makeText(context, "نام کاربری کپی شد", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    ScannerTab(onScanned = onScanned)
                }
            }
        }
    }
}

@Composable
private fun MyQrCodeTab(
    displayName: String,
    username: String,
    profilePicture: String?,
    qrBitmap: Bitmap?,
    onCopy: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AvatarView(name = displayName, imageUrl = profilePicture, size = 80.dp, colorSeed = username)
                Spacer(Modifier.height(16.dp))
                Text(displayName, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("@$username", color = Color.Gray, fontSize = 14.sp)
                
                Spacer(Modifier.height(32.dp))
                
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                
                Text(
                    "کد QR شما خصوصی است. با اسکن کردن این کد، دیگران می‌توانند به شما پیام دهند.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, null)
                Spacer(Modifier.width(8.dp))
                Text("کپی آیدی")
            }
            OutlinedButton(onClick = { /* Share Logic */ }) {
                Icon(Icons.Default.Share, null)
                Spacer(Modifier.width(8.dp))
                Text("اشتراک‌گذاری")
            }
        }
    }
}

@Composable
private fun ScannerTab(onScanned: (String) -> Unit) {
    val context = LocalContext.current
    var scanResult by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                CompoundBarcodeView(ctx).apply {
                    val activity = ctx.findActivity()
                    if (activity != null) {
                        val capture = CaptureManager(activity, this)
                        capture.initializeFromIntent(activity.intent, null)
                        capture.decode()
                    }
                    this.decodeContinuous { result ->
                        val text = result.text
                        if (text.startsWith("veiltalk://user/")) {
                            val scannedUsername = text.removePrefix("veiltalk://user/")
                            onScanned(scannedUsername)
                        }
                    }
                    this.resume()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay UI for scanner
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            )
        }
        
        Text(
            "کد QR دوست خود را در کادر بالا قرار دهید",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            color = Color.White,
            fontSize = 13.sp
        )
    }
}
