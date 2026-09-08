package com.example.veiltalk.common.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun ImageCropperDialog(
    uri: Uri,
    onDismiss: () -> Unit,
    onCropped: (Uri) -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(uri) {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                bitmap = BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (bitmap != null) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                var scale by remember { mutableStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                        Text("تنظیم تصویر", color = Color.White)
                        IconButton(onClick = {
                            val cropped = cropBitmap(bitmap!!, scale, offset)
                            val file = File(context.cacheDir, "cropped_avatar.jpg")
                            FileOutputStream(file).use { out ->
                                cropped.compress(Bitmap.CompressFormat.JPEG, 90, out)
                            }
                            onCropped(Uri.fromFile(file))
                        }) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Cropping Area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale *= zoom
                                    offset += pan
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(300.dp)) {
                            val canvasSize = size
                            val imgWidth = bitmap!!.width.toFloat()
                            val imgHeight = bitmap!!.height.toFloat()
                            
                            val baseScale = Math.min(canvasSize.width / imgWidth, canvasSize.height / imgHeight)
                            
                            val drawWidth = imgWidth * baseScale * scale
                            val drawHeight = imgHeight * baseScale * scale
                            
                            val drawOffset = Offset(
                                (canvasSize.width - drawWidth) / 2 + offset.x,
                                (canvasSize.height - drawHeight) / 2 + offset.y
                            )

                            drawImage(
                                image = bitmap!!.asImageBitmap(),
                                dstOffset = IntOffset(drawOffset.x.toInt(), drawOffset.y.toInt()),
                                dstSize = IntSize(drawWidth.toInt(), drawHeight.toInt())
                            )
                        }
                        
                        // Border window
                        Box(
                            modifier = Modifier
                                .size(300.dp)
                                .border(2.dp, Color.White, RectangleShape)
                        )
                    }
                    
                    Text(
                        "با دو انگشت زوم کنید و تصویر را جابجا کنید",
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

private fun cropBitmap(source: Bitmap, scale: Float, offset: Offset): Bitmap {
    val size = Math.min(source.width, source.height)
    return Bitmap.createBitmap(source, (source.width - size) / 2, (source.height - size) / 2, size, size)
}
