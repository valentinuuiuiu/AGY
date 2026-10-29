package com.example.ui.screens

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
fun GeminiLiveScreen(viewModel: GeminiLiveViewModel = viewModel()) {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    if (permissionsState.allPermissionsGranted) {
        JarvisLiveCameraContent(viewModel)
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(24.dp)
                    .cyberGlass()
                    .padding(24.dp)
            ) {
                Icon(
                    Icons.Default.Videocam,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "JARVIS LIVE COPILOT",
                    style = MaterialTheme.typography.titleLarge,
                    color = CyberTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Ultra-low latency bidirectional audio and camera vision require Camera and Microphone permissions.",
                    color = CyberTextSecondary,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { permissionsState.launchMultiplePermissionRequest() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CyberBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("AUTHORIZE NEURAL SENSORS", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun JarvisLiveCameraContent(viewModel: GeminiLiveViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val status by viewModel.status.collectAsState()
    val transcript by viewModel.transcript.collectAsState()
    val lastToolAction by viewModel.lastToolAction.collectAsState()
    val framesSent by viewModel.framesSentCount.collectAsState()
    val amplitude by viewModel.audioAmplitude.collectAsState()

    var useFrontCamera by remember { mutableStateOf(false) }
    var showDevConsole by remember { mutableStateOf(false) }

    if (showDevConsole) {
        DevConsoleDialog(onDismiss = { showDevConsole = false })
    }

    Box(modifier = Modifier.fillMaxSize().background(CyberBackground)) {
        // Camera Preview Feed
        key(useFrontCamera) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                                    viewModel.processImageProxy(imageProxy)
                                }
                            }

                        try {
                            cameraProvider.unbindAll()
                            val selector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                selector,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Dark Vignette Overlays for futuristic contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to CyberBackground.copy(alpha = 0.85f),
                        0.25f to Color.Transparent,
                        0.75f to Color.Transparent,
                        1.0f to CyberBackground.copy(alpha = 0.95f)
                    )
                )
        )

        // Holographic Target Reticle in Center
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.Center)
        ) {
            HolographicReticle(
                modifier = Modifier.fillMaxSize(),
                color = if (status == "Connected") NeonCyan else NeonPurple,
                bracketLength = 36f,
                strokeWidth = 3f
            )

            // Radar pulse inside reticle when connected
            if (status == "Connected") {
                JarvisRadarPulse(
                    modifier = Modifier
                        .size(160.dp)
                        .align(Alignment.Center),
                    tint = NeonCyan
                )
            }
        }

        // Top Cyber HUD Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 42.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Status Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            when {
                                status == "Connected" -> CyberEmerald.copy(alpha = 0.25f)
                                status.startsWith("Error") -> CyberRose.copy(alpha = 0.25f)
                                status == "Connecting..." -> CyberAmber.copy(alpha = 0.25f)
                                else -> CyberCard.copy(alpha = 0.85f)
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                status == "Connected" -> CyberEmerald
                                status.startsWith("Error") -> CyberRose
                                status == "Connecting..." -> CyberAmber
                                else -> CyberBorder
                            },
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    status == "Connected" -> CyberEmerald
                                    status.startsWith("Error") -> CyberRose
                                    status == "Connecting..." -> CyberAmber
                                    else -> CyberTextMuted
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (status == "Connected") "JARVIS LIVE ONLINE" else status.uppercase(),
                        color = CyberTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                // Top Controls: Flip Camera & Dev Settings
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { useFrontCamera = !useFrontCamera },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CyberCard.copy(alpha = 0.8f))
                            .border(1.dp, CyberBorder, CircleShape)
                    ) {
                        Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Switch Camera", tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }

                    IconButton(
                        onClick = { showDevConsole = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CyberCard.copy(alpha = 0.8f))
                            .border(1.dp, CyberBorder, CircleShape)
                    ) {
                        Icon(Icons.Default.Terminal, contentDescription = "Terminal Config", tint = NeonPurple, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Real-time telemetry badges
            if (status == "Connected") {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CyberBadge(text = "FRAMES: $framesSent", color = NeonCyan)
                    CyberBadge(text = "LATENCY: ~140ms", color = ElectricBlue)
                    CyberBadge(text = "AUDIO: 24kHz PUCK", color = NeonPurple)
                }
            }
        }

        // Bottom HUD: Waveform, Tool Execution Log, Transcript & Jarvis PTT button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Autonomous Agent Action Banner
            AnimatedVisibility(visible = lastToolAction != null) {
                lastToolAction?.let { action ->
                    Surface(
                        color = NeonPurple.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = action,
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Real-time Audio Waveform Visualizer
            if (status == "Connected") {
                CyberWaveformVisualizer(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(32.dp)
                        .padding(bottom = 8.dp),
                    barCount = 20,
                    activeColor = NeonCyan,
                    isActive = amplitude > 0.05f || transcript.isNotEmpty()
                )
            }

            // Jarvis Model Transcript Bubble
            AnimatedVisibility(visible = transcript.isNotEmpty()) {
                Surface(
                    color = CyberCard.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "JULES COPILOT",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = transcript,
                            color = CyberTextPrimary,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Big Glowing Jarvis Toggle Action Button
            val isConnected = status == "Connected"
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        if (isConnected) Brush.linearGradient(listOf(CyberRose, Color(0xFF991B1B)))
                        else Brush.linearGradient(listOf(NeonCyan, ElectricBlue))
                    )
                    .border(
                        2.dp,
                        if (isConnected) CyberRose.copy(alpha = 0.8f) else NeonCyan.copy(alpha = 0.8f),
                        CircleShape
                    )
            ) {
                IconButton(
                    onClick = { viewModel.toggleLive() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.CallEnd else Icons.Default.Videocam,
                        contentDescription = "Toggle Jarvis Live",
                        tint = if (isConnected) Color.White else CyberBackground,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isConnected) "TAP TO DISENGAGE COPILOT" else "INITIALIZE JARVIS MULTIMODAL LIVE",
                color = if (isConnected) CyberRose else NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }
    }
}
