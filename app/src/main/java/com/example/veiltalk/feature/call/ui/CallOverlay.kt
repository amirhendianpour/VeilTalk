package com.example.veiltalk.feature.call.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.veiltalk.common.model.CallKind
import com.example.veiltalk.common.model.CallStatus
import com.example.veiltalk.common.ui.components.AvatarView
import kotlinx.coroutines.delay
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

@Composable
fun CallOverlay(viewModel: CallViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.status == CallStatus.IDLE) return

    val context = LocalContext.current
    val isVideo = uiState.callType == CallKind.VIDEO
    var duration by remember { mutableStateOf(0) }

    LaunchedEffect(uiState.status) {
        if (uiState.status == CallStatus.CONNECTED) {
            duration = 0
            while (true) {
                delay(1000)
                duration++
            }
        }
    }

    // درخواست دسترسی میکروفون/دوربین درست قبل از پذیرفتن یا شروع تماس متصل‌شده
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* نتیجه در جریان عادی UI رصد می‌شود */ }

    LaunchedEffect(uiState.status) {
        if (uiState.status == CallStatus.CALLING || uiState.status == CallStatus.CONNECTED) {
            val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
            if (isVideo) needed.add(Manifest.permission.CAMERA)
            val missing = needed.filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
            if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val remoteTrack by viewModel.remoteVideoTrack.collectAsState()
        val localTrack by viewModel.localVideoTrack.collectAsState()
        val showRemoteVideo = isVideo && uiState.status == CallStatus.CONNECTED && remoteTrack != null

        if (isVideo && showRemoteVideo) {
            VideoRendererView(
                track = remoteTrack,
                eglContext = viewModel.callRepository.eglBaseContext,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
            )
        }

        if (isVideo && localTrack != null && !uiState.isCameraOff) {
            VideoRendererView(
                track = localTrack,
                eglContext = viewModel.callRepository.eglBaseContext,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 120.dp)
                    .size(width = 110.dp, height = 160.dp)
            )
        }

        if (!showRemoteVideo) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AvatarView(
                    name = viewModel.remoteDisplayName().ifBlank { "?" },
                    size = 96.dp,
                    colorSeed = uiState.remoteUser
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    viewModel.remoteDisplayName(),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (uiState.status) {
                    CallStatus.CALLING -> if (isVideo) "در حال تماس تصویری..." else "در حال تماس..."
                    CallStatus.RINGING -> if (isVideo) "تماس تصویری ورودی..." else "تماس ورودی..."
                    CallStatus.CONNECTED -> formatDuration(duration)
                    else -> ""
                },
                color = Color(0xFFE5E7EB),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (uiState.status == CallStatus.RINGING) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    CallButton(icon = if (isVideo) "🎥" else "📞", background = Color(0xFF22C55E)) {
                        viewModel.acceptCall()
                    }
                    CallButton(icon = "☎️", background = Color(0xFFEF4444)) {
                        viewModel.rejectCall()
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    CallButton(
                        icon = if (uiState.isMuted) "🔇" else "🎤",
                        background = if (uiState.isMuted) Color(0xFF9CA3AF) else Color(0xFF374151)
                    ) { viewModel.toggleMute() }

                    if (isVideo) {
                        CallButton(
                            icon = if (uiState.isCameraOff) "📷" else "🎥",
                            background = if (uiState.isCameraOff) Color(0xFF9CA3AF) else Color(0xFF374151)
                        ) { viewModel.toggleCamera() }
                    }

                    CallButton(icon = "☎️", background = Color(0xFFEF4444)) {
                        viewModel.endCall()
                    }
                }
            }
        }
    }
}

@Composable
private fun CallButton(icon: String, background: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(background, CircleShape)
            .clickableNoRipple(onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(icon, fontSize = 22.sp)
    }
}

@Composable
private fun VideoRendererView(
    track: org.webrtc.VideoTrack?,
    eglContext: org.webrtc.EglBase.Context,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            SurfaceViewRenderer(ctx).apply {
                init(eglContext, null)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                setMirror(false)
            }
        },
        update = { renderer ->
            track?.addSink(renderer)
        }
    )
}

private fun formatDuration(seconds: Int): String {
    val m = (seconds / 60).toString().padStart(2, '0')
    val s = (seconds % 60).toString().padStart(2, '0')
    return "$m:$s"
}

// یک Modifier کوچک برای کلیک بدون افکت ریپل روی دکمه‌های دایره‌ای تماس
@Composable
private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    return this.then(
        Modifier.clickable(
            indication = null,
            interactionSource = interactionSource,
            onClick = onClick
        )
    )
}