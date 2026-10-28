package com.example.ui.screens

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.dev.DevConsoleDialog
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
        LiveCameraContent(viewModel)
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Permisiunile de Cameră și Microfon sunt necesare pentru Gemini Live.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                    Text("Acordă Permisiuni")
                }
            }
        }
    }
}

@Composable
fun LiveCameraContent(viewModel: GeminiLiveViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val status by viewModel.status.collectAsState()
    val transcript by viewModel.transcript.collectAsState()
    val framesSent by viewModel.framesSentCount.collectAsState()

    var useFrontCamera by remember { mutableStateOf(false) }
    var showDevConsole by remember { mutableStateOf(false) }

    if (showDevConsole) {
        DevConsoleDialog(onDismiss = { showDevConsole = false })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview with dynamic cameraSelector
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

        // Top HUD Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, start = 16.dp, end = 16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live Status Pill
            Surface(
                color = when {
                    status == "Connected" -> Color(0xFF10B981).copy(alpha = 0.85f)
                    status.startsWith("Error") -> Color(0xFFEF4444).copy(alpha = 0.85f)
                    status == "Connecting..." -> Color(0xFFF59E0B).copy(alpha = 0.85f)
                    else -> Color(0xFF0F172A).copy(alpha = 0.75f)
                },
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (status == "Connected") Color.White else Color.LightGray, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = status,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }

            // Top action buttons: Switch Camera & Dev Console
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { useFrontCamera = !useFrontCamera },
                    modifier = Modifier
                        .background(Color(0xFF0F172A).copy(alpha = 0.7f), CircleShape)
                        .size(42.dp)
                ) {
                    Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Schimbă Camera", tint = Color.White)
                }

                IconButton(
                    onClick = { showDevConsole = true },
                    modifier = Modifier
                        .background(Color(0xFF0F172A).copy(alpha = 0.7f), CircleShape)
                        .size(42.dp)
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = "Developer Settings", tint = Color(0xFF06B6D4))
                }
            }
        }

        // Bottom HUD Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Frame analysis counter badge
            if (status == "Connected") {
                Surface(
                    color = Color(0xFF020617).copy(alpha = 0.75f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Vision Live: $framesSent frame-uri analizate în timp real",
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Gemini Voice Response Transcript Bubble
            AnimatedVisibility(visible = transcript.isNotEmpty()) {
                Surface(
                    color = Color(0xFF0F172A).copy(alpha = 0.9f),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gemini Live:", color = Color(0xFF06B6D4), fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = transcript,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Big Live Call Button
            FloatingActionButton(
                onClick = { viewModel.toggleLive() },
                containerColor = if (status == "Connected") Color(0xFFEF4444) else Color(0xFF06B6D4),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = if (status == "Connected") Icons.Default.CallEnd else Icons.Default.Videocam,
                    contentDescription = "Toggle Gemini Live",
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (status == "Connected") "Apasă pentru oprire" else "Pornește Gemini Live Vision",
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}
