package com.example.ui.screens

import android.Manifest
import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.dev.DevConsoleDialog
import com.example.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ArLensScreen(viewModel: ArLensViewModel = viewModel()) {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.CAMERA)
    )

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    if (permissionsState.allPermissionsGranted) {
        ArLensCameraContent(viewModel)
    } else {
        Box(
            modifier = Modifier.fillMaxSize().background(CyberBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp).cyberGlass().padding(24.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("AR WORLD LENS", color = CyberTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Camera permission is required for real-time menu scanning and AR translation overlays.", color = CyberTextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { permissionsState.launchMultiplePermissionRequest() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CyberBackground)
                ) {
                    Text("ENABLE CAMERA")
                }
            }
        }
    }
}

@Composable
fun ArLensCameraContent(viewModel: ArLensViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val selectedMode by viewModel.selectedMode.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val lensResult by viewModel.lensResult.collectAsState()
    val savedMsg by viewModel.savedToMemoryMessage.collectAsState()

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var showDevConsole by remember { mutableStateOf(false) }

    if (showDevConsole) {
        DevConsoleDialog(onDismiss = { showDevConsole = false })
    }

    Box(modifier = Modifier.fillMaxSize().background(CyberBackground)) {
        // CameraX Live Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    imageCapture = capture

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            capture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Holographic Target HUD
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(280.dp)
                .align(Alignment.Center)
        ) {
            HolographicReticle(
                modifier = Modifier.fillMaxSize(),
                color = when (selectedMode) {
                    ArLensMode.MENU_SCANNER -> NeonCyan
                    ArLensMode.WORLD_LENS -> ElectricBlue
                    ArLensMode.CULTURE_HUD -> NeonPurple
                },
                bracketLength = 48f,
                strokeWidth = 3.5f
            )

            // Scanning indicator line or pulse
            if (isAnalyzing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                    color = NeonCyan,
                    trackColor = Color.Transparent
                )
            }
        }

        // Top Cyber Mode Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CyberBadge(text = "AR LENS 2.0", color = NeonCyan)
                IconButton(
                    onClick = { showDevConsole = true },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CyberCard.copy(alpha = 0.8f))
                        .border(1.dp, CyberBorder, CircleShape)
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = "Terminal", tint = NeonPurple, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AR Mode Selector Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ArLensMode.values()) { mode ->
                    val isSelected = selectedMode == mode
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else CyberCard.copy(alpha = 0.8f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) NeonCyan else CyberBorder
                        ),
                        modifier = Modifier.clickable { viewModel.setMode(mode) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = mode.label,
                                color = if (isSelected) NeonCyan else CyberTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Bottom HUD: AR Result Card & Capture Shutter
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // AR Holographic Result Card
            AnimatedVisibility(visible = lensResult != null) {
                lensResult?.let { res ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cyberGlass(
                                cornerRadius = 18.dp,
                                borderBrush = androidx.compose.ui.graphics.SolidColor(NeonCyan.copy(alpha = 0.6f)),
                                backgroundColor = CyberCard.copy(alpha = 0.95f)
                            )
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = res.title,
                                    color = NeonCyan,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                IconButton(onClick = { viewModel.clearResult() }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = CyberTextMuted, modifier = Modifier.size(16.dp))
                                }
                            }

                            Text(
                                text = res.translationOrInsight,
                                color = CyberTextPrimary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )

                            // Dietary tags if in Menu Scanner mode
                            if (res.dietaryTags.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    res.dietaryTags.forEach { tag ->
                                        CyberBadge(text = tag, color = CyberEmerald, trailingDot = false)
                                    }
                                }
                            }

                            // Allergen Warning
                            res.allergenWarning?.let { warning ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CyberRose.copy(alpha = 0.15f))
                                        .border(1.dp, CyberRose.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = CyberRose, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ALLERGEN ALERT: $warning",
                                        color = CyberRose,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Cultural note
                            res.culturalNote?.let { note ->
                                Text(
                                    text = "Cultural context: $note",
                                    color = NeonPurple,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Action: Save to Infinite Memory
                            Button(
                                onClick = { viewModel.saveResultToMemory() },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple, contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(savedMsg ?: "SAVE TO INFINITE MEMORY", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Big Shutter Button
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(NeonCyan, ElectricBlue)))
                    .border(2.dp, NeonCyan, CircleShape)
            ) {
                IconButton(
                    onClick = {
                        val capture = imageCapture ?: return@IconButton
                        capture.takePicture(
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val bitmap = image.toBitmap()
                                    image.close()
                                    viewModel.analyzeCapturedBitmap(bitmap)
                                }
                                override fun onError(exception: ImageCaptureException) {
                                    exception.printStackTrace()
                                }
                            }
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                    enabled = !isAnalyzing
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), color = CyberBackground, strokeWidth = 3.dp)
                    } else {
                        Icon(Icons.Default.Camera, contentDescription = "Scan Reticle", tint = CyberBackground, modifier = Modifier.size(34.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "AIM AT MENU OR SIGN TO SCAN",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}
