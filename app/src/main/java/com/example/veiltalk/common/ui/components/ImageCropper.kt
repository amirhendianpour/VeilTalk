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

import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.toSize

@Composable
fun ImageCropperDialog(
    uri: Uri,
    onDismiss: () -> Unit,
    onCropped: (Uri) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(uri) {
        withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = false
                }
                bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (bitmap != null) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                var scale by remember { mutableStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }
                var canvasSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
                val cropBoxSize = with(density) { 300.dp.toPx() }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Image and Mask Layer
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { canvasSize = it.size.toSize() }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(0.5f, 5f)
                                    offset += pan
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val imgWidth = bitmap!!.width.toFloat()
                            val imgHeight = bitmap!!.height.toFloat()
                            
                            // Fit image to screen initially
                            val baseScale = Math.min(size.width / imgWidth, size.height / imgHeight)
                            val finalScale = baseScale * scale
                            
                            val drawWidth = imgWidth * finalScale
                            val drawHeight = imgHeight * finalScale
                            
                            val drawOffset = Offset(
                                (size.width - drawWidth) / 2 + offset.x,
                                (size.height - drawHeight) / 2 + offset.y
                            )

                            // 1. Draw Image
                            drawImage(
                                image = bitmap!!.asImageBitmap(),
                                dstOffset = IntOffset(drawOffset.x.toInt(), drawOffset.y.toInt()),
                                dstSize = IntSize(drawWidth.toInt(), drawHeight.toInt())
                            )

                            // 2. Draw Dim Mask with square hole
                            val holePath = Path().apply {
                                addRect(
                                    androidx.compose.ui.geometry.Rect(
                                        left = (size.width - cropBoxSize) / 2,
                                        top = (size.height - cropBoxSize) / 2,
                                        right = (size.width + cropBoxSize) / 2,
                                        bottom = (size.height + cropBoxSize) / 2
                                    )
                                )
                            }
                            
                            clipPath(holePath, clipOp = ClipOp.Difference) {
                                drawRect(Color.Black.copy(alpha = 0.7f))
                            }
                        }

                        // White Border for the crop area
                        Box(
                            modifier = Modifier
                                .size(300.dp)
                                .align(Alignment.Center)
                                .border(1.dp, Color.White.copy(alpha = 0.8f), RectangleShape)
                        )
                    }

                    // Bottom controls
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(bottom = 32.dp, top = 16.dp, start = 24.dp, end = 24.dp)
                    ) {
                        Text(
                            "برای تنظیم تصویر زوم کنید و آن را جابجا کنید",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 24.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("انصراف", color = Color.White)
                            }
                            
                            Button(
                                onClick = {
                                    val cropped = cropBitmap(bitmap!!, scale, offset, canvasSize, cropBoxSize)
                                    val file = File(context.cacheDir, "cropped_avatar_${System.currentTimeMillis()}.jpg")
                                    FileOutputStream(file).use { out ->
                                        cropped.compress(Bitmap.CompressFormat.JPEG, 95, out)
                                    }
                                    onCropped(Uri.fromFile(file))
                                },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                            ) {
                                Text("انتخاب عکس")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun cropBitmap(
    source: Bitmap,
    userScale: Float,
    pan: Offset,
    canvasSize: androidx.compose.ui.geometry.Size,
    cropBoxSize: Float
): Bitmap {
    val imgWidth = source.width.toFloat()
    val imgHeight = source.height.toFloat()

    // Same logic as in Canvas
    val baseScale = Math.min(canvasSize.width / imgWidth, canvasSize.height / imgHeight)
    val totalScale = baseScale * userScale

    val drawWidth = imgWidth * totalScale
    val drawHeight = imgHeight * totalScale

    val drawOffset = Offset(
        (canvasSize.width - drawWidth) / 2 + pan.x,
        (canvasSize.height - drawHeight) / 2 + pan.y
    )

    // Crop box coordinates in UI
    val cropLeft = (canvasSize.width - cropBoxSize) / 2
    val cropTop = (canvasSize.height - cropBoxSize) / 2

    // Map UI crop box to Bitmap coordinates
    val bitmapStartX = ((cropLeft - drawOffset.x) / totalScale).toInt().coerceIn(0, source.width - 1)
    val bitmapStartY = ((cropTop - drawOffset.y) / totalScale).toInt().coerceIn(0, source.height - 1)
    
    val bitmapCropSize = (cropBoxSize / totalScale).toInt().coerceAtLeast(10)
    
    // Ensure we don't exceed bitmap bounds
    val finalWidth = Math.min(bitmapCropSize, source.width - bitmapStartX).coerceAtLeast(1)
    val finalHeight = Math.min(bitmapCropSize, source.height - bitmapStartY).coerceAtLeast(1)

    return try {
        Bitmap.createBitmap(source, bitmapStartX, bitmapStartY, finalWidth, finalHeight)
    } catch (e: Exception) {
        e.printStackTrace()
        // Fallback to center crop if something goes wrong
        val size = Math.min(source.width, source.height)
        Bitmap.createBitmap(source, (source.width - size) / 2, (source.height - size) / 2, size, size)
    }
}
