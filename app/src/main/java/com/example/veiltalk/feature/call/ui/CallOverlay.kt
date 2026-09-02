package com.example.veiltalk.feature.call.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.veiltalk.common.model.CallKind
import com.example.veiltalk.common.model.CallStatus
import com.example.veiltalk.common.ui.components.AvatarView
import com.example.veiltalk.ui.theme.*
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

    // Handle back button to minimize
    BackHandler(enabled = uiState.status != CallStatus.IDLE && !uiState.isMinimized) {
        viewModel.toggleMinimize()
    }

    if (uiState.isMinimized) {
        MinimizedCallView(
            uiState = uiState,
            duration = duration,
            onExpand = viewModel::toggleMinimize,
            onEnd = viewModel.callRepository::endCall,
            displayName = viewModel.remoteDisplayName()
        )
        return
    }

    // Pulsating animation for calling state
    val infiniteTransition = rememberInfiniteTransition(label = "pulsating")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (uiState.status == CallStatus.CALLING || uiState.status == CallStatus.RINGING) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // درخواست دسترسی میکروفون/دوربین برای پذیرفتن تماس
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) {
            viewModel.acceptCall()
        }
    }

    fun handleAccept() {
        val needed = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (isVideo) needed.add(Manifest.permission.CAMERA)
        val missing = needed.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            viewModel.acceptCall()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        VeilRedDark,
                        VeilBlack,
                        Color.Black
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {} // Consume touches to prevent leaking to underlying screens
            )
    ) {
        val remoteTrack by viewModel.remoteVideoTrack.collectAsState()
        val localTrack by viewModel.localVideoTrack.collectAsState()

        val primaryTrack = if (uiState.isLocalVideoPrimary) localTrack else remoteTrack
        val secondaryTrack = if (uiState.isLocalVideoPrimary) remoteTrack else localTrack

        val showPrimary = isVideo && uiState.status == CallStatus.CONNECTED && primaryTrack != null
        val showSecondary = isVideo && uiState.status == CallStatus.CONNECTED && secondaryTrack != null

        // تصویر اصلی (تمام صفحه)
        if (isVideo && showPrimary) {
            VideoRendererView(
                track = primaryTrack,
                eglContext = viewModel.callRepository.eglBaseContext,
                modifier = Modifier.fillMaxSize()
            )
        }

        // تصویر ثانویه (کوچک)
        if (isVideo && showSecondary && !(uiState.isLocalVideoPrimary && uiState.isCameraOff)) {
            val isSecondaryLocal = !uiState.isLocalVideoPrimary
            if (!(isSecondaryLocal && uiState.isCameraOff)) {
                VideoRendererView(
                    track = secondaryTrack,
                    eglContext = viewModel.callRepository.eglBaseContext,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 84.dp, end = 16.dp)
                        .size(width = 110.dp, height = 160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { viewModel.swapVideoViews() }
                )
            }
        }

        // دکمه کوچک کردن (Minimize)
        IconButton(
            onClick = viewModel::toggleMinimize,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 48.dp, start = 16.dp)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
        ) {
            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White)
        }

        // Top Info (Name, Status)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!showPrimary || uiState.isLocalVideoPrimary) {
                Box(modifier = Modifier.scale(scale)) {
                    AvatarView(
                        name = viewModel.remoteDisplayName().ifBlank { "?" },
                        size = 120.dp,
                        colorSeed = uiState.remoteUser
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
            
            Text(
                viewModel.remoteDisplayName(),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            
            Surface(
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = when (uiState.status) {
                        CallStatus.CALLING -> "در حال تماس..."
                        CallStatus.RINGING -> "تماس ورودی..."
                        CallStatus.CONNECTED -> formatDuration(duration)
                        else -> ""
                    },
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        // Bottom Controls
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp, start = 20.dp, end = 20.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = Color.White.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(vertical = 20.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.status == CallStatus.RINGING) {
                    CallButton(
                        icon = Icons.Default.Call, 
                        background = Color(0xFF22C55E),
                        tint = Color.White,
                        label = "پاسخ"
                    ) { handleAccept() }
                    
                    CallButton(
                        icon = Icons.Default.CallEnd, 
                        background = Color(0xFFEF4444),
                        tint = Color.White,
                        label = "رد تماس"
                    ) { viewModel.rejectCall() }
                } else {
                    CallButton(
                        icon = if (uiState.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        background = if (uiState.isSpeakerOn) Color.White.copy(alpha = 0.3f) else Color.Transparent,
                        label = "بلندگو"
                    ) { viewModel.toggleSpeaker() }

                    if (isVideo) {
                        CallButton(
                            icon = if (uiState.isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                            background = if (uiState.isCameraOff) Color.White.copy(alpha = 0.3f) else Color.Transparent,
                            label = "دوربین"
                        ) { viewModel.toggleCamera() }
                        
                        CallButton(
                            icon = Icons.Default.Cameraswitch,
                            background = Color.Transparent,
                            label = "چرخش"
                        ) { viewModel.flipCamera() }
                    }

                    CallButton(
                        icon = if (uiState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        background = if (uiState.isMuted) Color.White.copy(alpha = 0.3f) else Color.Transparent,
                        label = "بی‌صدا"
                    ) { viewModel.toggleMute() }

                    CallButton(
                        icon = Icons.Default.CallEnd,
                        background = Color(0xFFEF4444),
                        tint = Color.White,
                        label = "پایان"
                    ) { viewModel.endCall() }
                }
            }
        }
    }
}

@Composable
fun MinimizedCallView(
    uiState: com.example.veiltalk.feature.call.data.CallUiSnapshot,
    duration: Int,
    onExpand: () -> Unit,
    onEnd: () -> Unit,
    displayName: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 12.dp, end = 12.dp)
            .height(56.dp)
            .clickable { onExpand() },
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primary,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (uiState.callType == CallKind.VIDEO) Icons.Default.Videocam else Icons.Default.Call,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    if (uiState.status == CallStatus.CONNECTED) formatDuration(duration) else "در حال تماس...",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = onEnd) {
                Icon(Icons.Default.CallEnd, null, tint = Color.Red)
            }
        }
    }
}

@Composable
private fun CallButton(
    icon: ImageVector, 
    background: Color = Color.Transparent, 
    tint: Color = Color.White,
    label: String? = null,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(background, CircleShape)
                .clickableNoRipple(onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(30.dp))
        }
        if (label != null) {
            Text(
                label,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun VideoRendererView(
    track: org.webrtc.VideoTrack?,
    eglContext: org.webrtc.EglBase.Context,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val renderer = remember { SurfaceViewRenderer(context) }
    
    DisposableEffect(track) {
        renderer.init(eglContext, null)
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        track?.addSink(renderer)
        
        onDispose {
            track?.removeSink(renderer)
            renderer.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { renderer }
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
    val interactionSource = remember { MutableInteractionSource() }
    return this.then(
        Modifier.clickable(
            indication = null,
            interactionSource = interactionSource,
            onClick = onClick
        )
    )
}
