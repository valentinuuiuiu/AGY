package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.dev.DevConsoleDialog
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(viewModel: CreateViewModel = viewModel()) {
    val isLoading by viewModel.isLoading.collectAsState()
    val isEnhancing by viewModel.isEnhancing.collectAsState()
    val resultText by viewModel.resultText.collectAsState()
    val generatedBitmap by viewModel.generatedBitmap.collectAsState()
    val savedToVault by viewModel.savedToVault.collectAsState()

    var prompt by remember { mutableStateOf("") }
    var negativePrompt by remember { mutableStateOf("") }
    var showNegativePrompt by remember { mutableStateOf(false) }

    var selectedStyle by remember { mutableStateOf<StylePreset?>(null) }
    var selectedSize by remember { mutableStateOf("1K") }
    val sizes = listOf("1K", "2K", "4K")

    var selectedAspectRatio by remember { mutableStateOf("16:9") }
    val aspectRatios = listOf("1:1", "16:9", "9:16", "3:4", "4:3", "21:9")

    var showDevConsole by remember { mutableStateOf(false) }

    if (showDevConsole) {
        DevConsoleDialog(onDismiss = { showDevConsole = false })
    }

    Scaffold(
        containerColor = CyberBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "JULES CREATION STUDIO",
                        color = NeonCyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Imagen 3 & Veo 3.1 Creative Engine",
                        color = CyberTextSecondary,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = { showDevConsole = true },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CyberCard)
                        .border(1.dp, CyberBorder, CircleShape)
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = "Terminal", tint = NeonPurple, modifier = Modifier.size(18.dp))
                }
            }

            // Prompt Input Card with Gemini Magic
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .cyberGlass(cornerRadius = 16.dp)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PROMPT SYNTHESIZER",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        // Magic Enhance Button
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NeonPurple.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple),
                            modifier = Modifier.clickable(enabled = !isEnhancing && prompt.isNotBlank()) {
                                viewModel.enhancePrompt(prompt) { enhanced ->
                                    prompt = enhanced
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isEnhancing) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = NeonCyan, strokeWidth = 1.5.dp)
                                } else {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("MAGIC ENHANCE", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        placeholder = { Text("Describe the visual world... e.g. Neon rain-soaked street in Kyoto", color = CyberTextMuted, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Toggle Negative Prompt
                    Text(
                        text = if (showNegativePrompt) "- Hide Negative Prompt" else "+ Add Negative Prompt",
                        color = CyberTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickable { showNegativePrompt = !showNegativePrompt }
                    )

                    AnimatedVisibility(visible = showNegativePrompt) {
                        OutlinedTextField(
                            value = negativePrompt,
                            onValueChange = { negativePrompt = it },
                            placeholder = { Text("e.g. blurry, low quality, artifacts, watermark", color = CyberTextMuted, fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberRose,
                                unfocusedBorderColor = CyberBorder,
                                focusedTextColor = CyberTextPrimary,
                                unfocusedTextColor = CyberTextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // Style Presets Carousel
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "AESTHETIC STYLE PRESETS",
                    color = CyberTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(viewModel.stylePresets) { preset ->
                        val isSelected = selectedStyle == preset
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else CyberCard,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) NeonCyan else CyberBorder
                            ),
                            modifier = Modifier.clickable {
                                selectedStyle = if (isSelected) null else preset
                            }
                        ) {
                            Text(
                                text = preset.name,
                                color = if (isSelected) NeonCyan else CyberTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Aspect Ratio & Resolution Grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .cyberGlass(cornerRadius = 16.dp)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "CANVAS GEOMETRY & RESOLUTION",
                        color = CyberTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sizes.forEach { size ->
                            val isSelected = selectedSize == size
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) ElectricBlue.copy(alpha = 0.3f) else CyberCardElevated,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) ElectricBlue else CyberBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedSize = size }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = size,
                                        color = if (isSelected) NeonCyan else CyberTextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(aspectRatios) { ratio ->
                            val isSelected = selectedAspectRatio == ratio
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) NeonPurple.copy(alpha = 0.3f) else CyberCardElevated,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) NeonPurple else CyberBorder
                                ),
                                modifier = Modifier.clickable { selectedAspectRatio = ratio }
                            ) {
                                Text(
                                    text = ratio,
                                    color = if (isSelected) NeonPink else CyberTextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons: Generate Image / Generate Video
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.generateImage(prompt, selectedSize, selectedAspectRatio, selectedStyle, negativePrompt)
                    },
                    enabled = !isLoading && prompt.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CyberBackground),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SYNTHESIZE IMAGE", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Button(
                    onClick = { viewModel.generateVideo(prompt, selectedAspectRatio) },
                    enabled = !isLoading && prompt.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("GENERATE VEO", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            // Synthesis Status Banner
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .cyberGlass(cornerRadius = 12.dp)
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = NeonCyan, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = resultText ?: "Synthesizing visual asset...",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Generated Visual Result Card
            generatedBitmap?.let { bitmap ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .cyberGlass(cornerRadius = 16.dp)
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CyberBadge(text = "RENDER RESULT", color = CyberEmerald)
                            Text(
                                text = "$selectedAspectRatio • $selectedSize",
                                color = CyberTextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Generated AI Asset",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, CyberBorder, RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Button(
                            onClick = { viewModel.saveAssetToNotes(prompt) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(savedToVault ?: "SAVE ASSET TO INFINITE MEMORY", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            resultText?.takeIf { generatedBitmap == null && !isLoading }?.let { txt ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .cyberGlass(cornerRadius = 12.dp)
                        .padding(14.dp)
                ) {
                    Text(text = txt, color = CyberTextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
