package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.dev.DevConsoleDialog
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryBrainScreen(viewModel: MemoryBrainViewModel = viewModel()) {
    val metrics by viewModel.metrics.collectAsState()
    val isQuerying by viewModel.isQuerying.collectAsState()
    val lastAnswer by viewModel.lastAnswer.collectAsState()
    val notes by viewModel.travelNotes.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var userPrompt by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDevConsole by remember { mutableStateOf(false) }

    val quickQuestions = listOf(
        "Where should I go next based on my notes?",
        "Synthesize my entire travel journey",
        "What are my favorite food and culinary spots?",
        "Identify hidden patterns & safety tips"
    )

    if (showDevConsole) {
        DevConsoleDialog(onDismiss = { showDevConsole = false })
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var destination by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text("ENCODE NEW MEMORY", color = NeonCyan, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title / Place Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CyberBorder
                        )
                    )
                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = { Text("Destination / Category") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CyberBorder
                        )
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Observation / Recommendation") },
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CyberBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.addQuickNote(title, content, destination.ifBlank { "Global" })
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CyberBackground)
                ) {
                    Text("SAVE TO VAULT", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("CANCEL", color = CyberTextSecondary)
                }
            },
            containerColor = CyberCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        containerColor = CyberBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = NeonCyan,
                contentColor = CyberBackground,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Memory")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "INFINITE MEMORY BRAIN",
                            color = NeonCyan,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Gemini 1.5 Pro • 2M Token Context RAG",
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
            }

            // Real-time RAG Metrics Dashboard Card
            item {
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
                            CyberBadge(text = "RAG STATUS: ONLINE", color = NeonCyan)
                            Text(
                                text = "${metrics.estimatedTokens} tokens indexed",
                                color = NeonPurple,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // 3 Metric Pillars
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricCell("TRAVEL NOTES", metrics.totalNotes.toString(), NeonCyan)
                            MetricCell("PHRASES", metrics.totalPhrases.toString(), NeonPink)
                            MetricCell("ITINERARIES", metrics.totalItineraries.toString(), ElectricBlue)
                        }
                    }
                }
            }

            // Ask Infinite Brain Input
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .cyberGlass(cornerRadius = 16.dp)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "QUERY LOCAL RAG BRAIN",
                            color = CyberTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    }

                    OutlinedTextField(
                        value = userPrompt,
                        onValueChange = { userPrompt = it },
                        placeholder = { Text("Ask anything... e.g. Where should I go next?", color = CyberTextMuted, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CyberBorder,
                            focusedTextColor = CyberTextPrimary,
                            unfocusedTextColor = CyberTextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Quick prompt chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(quickQuestions) { q ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = CyberCardElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                                modifier = Modifier.clickable {
                                    userPrompt = q
                                    viewModel.queryBrain(q)
                                }
                            ) {
                                Text(
                                    text = q,
                                    color = CyberTextSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.queryBrain(userPrompt) },
                        enabled = !isQuerying && userPrompt.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CyberBackground),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isQuerying) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CyberBackground, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SYNTHESIZING RAG CONTEXT...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SYNTHESIZE FROM INFINITE MEMORY", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Brain Answer Display Card
            item {
                AnimatedVisibility(visible = lastAnswer != null) {
                    lastAnswer?.let { answer ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .cyberGlass(
                                    cornerRadius = 16.dp,
                                    borderBrush = androidx.compose.ui.graphics.SolidColor(NeonCyan.copy(alpha = 0.5f))
                                )
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CyberBadge(text = "SYNTHESIS RESULT", color = NeonCyan)
                                    Text(
                                        text = answer.memoryContextUsed,
                                        color = CyberTextMuted,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = answer.answer,
                                    color = CyberTextPrimary,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }

            // Memory Vault Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STORED MEMORY RECORDS (${notes.size})",
                        color = CyberTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "ROOM DB OFFLINE",
                        color = CyberEmerald,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Stored Memory Items List
            if (notes.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cyberGlass()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = CyberTextMuted, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No stored travel records in local database.", color = CyberTextSecondary, fontSize = 13.sp)
                            Text("Jules Copilot or AR Lens will automatically save observations here.", color = CyberTextMuted, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                items(notes) { note ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cyberGlass(cornerRadius = 14.dp)
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = note.title,
                                    color = NeonCyan,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row {
                                    IconButton(
                                        onClick = { viewModel.toggleFavorite(note.id, note.isFavorite) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            if (note.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = null,
                                            tint = if (note.isFavorite) NeonPink else CyberTextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteNote(note.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = CyberRose, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Text(
                                text = note.aiResponse,
                                color = CyberTextSecondary,
                                fontSize = 12.sp,
                                maxLines = 4
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Tag: ${note.tags}",
                                    color = CyberTextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = note.destination,
                                    color = NeonPurple,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun MetricCell(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = color,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = label,
            color = CyberTextMuted,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
