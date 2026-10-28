package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CreateScreen(viewModel: CreateViewModel = viewModel()) {
    val isLoading by viewModel.isLoading.collectAsState()
    val resultText by viewModel.resultText.collectAsState()
    val generatedBitmap by viewModel.generatedBitmap.collectAsState()

    var prompt by remember { mutableStateOf("") }
    var selectedSize by remember { mutableStateOf("1K") }
    val sizes = listOf("1K", "2K", "4K")
    
    var selectedAspectRatio by remember { mutableStateOf("16:9") }
    val aspectRatios = listOf("1:1", "16:9", "9:16", "3:4", "4:3", "2:3", "3:2", "21:9")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("WanderAI Content Studio", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Prompt for Image/Video") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Select Resolution:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sizes.forEach { size ->
                FilterChip(
                    selected = (selectedSize == size),
                    onClick = { selectedSize = size },
                    label = { Text(size) }
                )
            }
        }

        Text("Aspect Ratio:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            aspectRatios.forEach { ratio ->
                FilterChip(
                    selected = (selectedAspectRatio == ratio),
                    onClick = { selectedAspectRatio = ratio },
                    label = { Text(ratio) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { viewModel.generateImage(prompt, selectedSize, selectedAspectRatio) },
                enabled = !isLoading && prompt.isNotBlank(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Generate Image")
            }

            Button(
                onClick = { viewModel.generateVideo(prompt, selectedAspectRatio) },
                enabled = !isLoading && prompt.isNotBlank(),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Veo 3 Video")
            }
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator()
        }

        resultText?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // --- REAL VISUAL CANVAS RENDERING ---
        generatedBitmap?.let { bitmap ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Generated Travel Asset",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
