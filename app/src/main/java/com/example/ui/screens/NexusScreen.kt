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
import com.example.services.ToolStatus
import com.example.ui.dev.DevConsoleDialog
import com.example.ui.theme.*

@Composable
fun NexusScreen(viewModel: NexusViewModel = viewModel()) {
    val executionLogs by viewModel.executionLogs.collectAsState()
    val mcpTools by viewModel.mcpTools.collectAsState()
    val telemetry by viewModel.telemetryStatus.collectAsState()

    var showDevConsole by remember { mutableStateOf(false) }
    var showPlaceDialog by remember { mutableStateOf(false) }

    if (showDevConsole) {
        DevConsoleDialog(onDismiss = { showDevConsole = false })
    }

    if (showPlaceDialog) {
        var query by remember { mutableStateOf("") }
        var navigate by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPlaceDialog = false },
            title = { Text("TRIGGER MAPS NAVIGATION", color = NeonCyan, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter location or hidden gem for autonomous GPS navigation:", color = CyberTextSecondary, fontSize = 12.sp)
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("e.g. Shibuya Sky, Tokyo or Best Ramen nearby") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CyberBorder
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = navigate, onCheckedChange = { navigate = it })
                        Text("Launch Turn-by-Turn GPS Navigation", color = CyberTextPrimary, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (query.isNotBlank()) {
                            viewModel.searchPlaces(query, navigate)
                            showPlaceDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CyberBackground)
                ) {
                    Text("DISPATCH TO MAPS", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPlaceDialog = false }) { Text("CANCEL", color = CyberTextSecondary) }
            },
            containerColor = CyberCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        containerColor = CyberBackground
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
                            text = "AGY NEXUS COMMAND",
                            color = NeonCyan,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Gemini Agentic Tool Calling & System Hooks",
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

            // Real-time Telemetry Status Banner
            item {
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
                            CyberBadge(text = "AGENT RUNTIME: ACTIVE", color = NeonCyan)
                            IconButton(onClick = { viewModel.runTelemetryCheck() }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = NeonCyan, modifier = Modifier.size(16.dp))
                            }
                        }

                        Text(
                            text = telemetry ?: "Running hardware telemetry sweep...",
                            color = CyberTextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Quick Agent Action Triggers
            item {
                Text(
                    text = "AUTONOMOUS SYSTEM ACTIONS",
                    color = CyberTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionTile(
                        icon = Icons.Default.Navigation,
                        title = "Maps Navigation",
                        desc = "Launch GPS Intent",
                        color = NeonCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { showPlaceDialog = true }
                    )

                    ActionTile(
                        icon = Icons.Default.BookmarkAdd,
                        title = "Write to Memory",
                        desc = "Room DB Hook",
                        color = NeonPurple,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            viewModel.testSaveMemory(
                                "Hidden Alley Ramen",
                                "Autonomous agent discovery: authentic tonkotsu ramen shop with English ticket machine.",
                                "Food & Dining"
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionTile(
                        icon = Icons.Default.Apps,
                        title = "Scan Device MCP",
                        desc = "${mcpTools.size} Apps Registered",
                        color = ElectricBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.scanApps() }
                    )

                    ActionTile(
                        icon = Icons.Default.Sensors,
                        title = "Telemetry Sweep",
                        desc = "Battery & Network",
                        color = CyberAmber,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.runTelemetryCheck() }
                    )
                }
            }

            // Live Tool Calling Execution Timeline
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AGENT EXECUTION TIMELINE (${executionLogs.size})",
                        color = CyberTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    if (executionLogs.isNotEmpty()) {
                        Text(
                            text = "CLEAR LOGS",
                            color = CyberRose,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { viewModel.clearLogs() }
                        )
                    }
                }
            }

            if (executionLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cyberGlass()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = CyberTextMuted, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Agent tool execution stream standby.", color = CyberTextSecondary, fontSize = 13.sp)
                            Text("Calls initiated by Jarvis Live or manual triggers will appear here in real time.", color = CyberTextMuted, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                items(executionLogs) { log ->
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when (log.status) {
                                            ToolStatus.SUCCESS -> Icons.Default.CheckCircle
                                            ToolStatus.FAILED -> Icons.Default.Cancel
                                            ToolStatus.PENDING -> Icons.Default.HourglassTop
                                        },
                                        contentDescription = null,
                                        tint = when (log.status) {
                                            ToolStatus.SUCCESS -> CyberEmerald
                                            ToolStatus.FAILED -> CyberRose
                                            ToolStatus.PENDING -> CyberAmber
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = log.toolName,
                                        color = NeonCyan,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Text(
                                    text = log.timestamp,
                                    color = CyberTextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Text(
                                text = "Args: ${log.argumentsJson}",
                                color = CyberTextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            Text(
                                text = "Result: ${log.resultSummary}",
                                color = ElectricBlue,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Installed Tools Drawer (MCP)
            item {
                Text(
                    text = "REGISTERED DEVICE APPS (${mcpTools.size})",
                    color = CyberTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(mcpTools.take(15)) { app ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CyberCardElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberBorder),
                            modifier = Modifier.clickable { viewModel.launchApp(app.packageName) }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.AppShortcut, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = app.appName.take(14),
                                    color = CyberTextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Launch",
                                    color = NeonPurple,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
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
private fun ActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .cyberGlass(cornerRadius = 14.dp)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                color = CyberTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = desc,
                color = CyberTextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
