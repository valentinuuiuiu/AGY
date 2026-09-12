package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.services.AppMcpTool

data class IntegrationTile(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val description: String,
    val actionType: String // "intent", "location", "api"
)

@Composable
fun NexusScreen(viewModel: NexusViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mcpTools by viewModel.mcpTools.collectAsState()
    val ragStatus by viewModel.ragStatus.collectAsState()
    val sshOutput by viewModel.sshOutput.collectAsState()
    var sshCommand by remember { mutableStateOf("") }
    var sshPassword by remember { mutableStateOf("") }
    var locationState by remember { mutableStateOf("Location: Standby") }

    val integrations = listOf(
        IntegrationTile("sheets", "Google Sheets", Icons.Filled.TableChart, "Direct Data Sync", "intent"),
        IntegrationTile("notebook", "NotebookLLM", Icons.Filled.Book, "RAG & Brain Access", "intent"),
        IntegrationTile("maps", "Google Maps", Icons.Filled.Map, "Navigation & Routing", "intent"),
        IntegrationTile("places", "Google Places", Icons.Filled.Place, "POIs & Business Info", "api"),
        IntegrationTile("video", "YouTube / Video", Icons.Filled.PlayArrow, "Media & Tutorials", "intent"),
        IntegrationTile("music", "YT Music", Icons.Filled.MusicNote, "Audio & Frequencies", "intent"),
        IntegrationTile("location", "Live Location", Icons.Filled.GpsFixed, locationState, "location"),
        IntegrationTile("drive", "Google Drive", Icons.Filled.Cloud, "Cloud Storage Sync", "intent")
    )

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            locationState = "Location: Acquired (Simulated Data)"
        } else {
            locationState = "Location: Permission Denied"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "AGY Nexus Core",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Agentic Intergalactic Entity Dashboard. Accessing all active modules, integrations, and global API hooks.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = { viewModel.scanApps() },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Text("Scan Installed Apps (Register as MCP Tools)")
        }

        Button(
            onClick = { viewModel.queryBrain("System Check") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("Ping NotebookLLM (RAG: $ragStatus)")
        }

        // --- SSH Terminal Block ---
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Tailscale SSH Terminal (100.91.238.121)", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = sshCommand,
                    onValueChange = { sshCommand = it },
                    label = { Text("Command (e.g., ls -la, htop)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sshPassword,
                    onValueChange = { sshPassword = it },
                    label = { Text("Pop!_OS Password") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { viewModel.executeRemoteCommand(sshCommand, sshPassword) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Execute on Host")
                }
                Text(
                    text = sshOutput,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth().heightIn(max = 100.dp)
                )
            }
        }
        // --------------------------

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(integrations) { item ->
                IntegrationCard(item = item) {
                    when (item.actionType) {
                        "intent" -> {
                            val url = when(item.id) {
                                "sheets" -> "https://docs.google.com/spreadsheets"
                                "notebook" -> "https://notebooklm.google.com/"
                                "maps" -> "https://maps.google.com/"
                                "video" -> "https://youtube.com/"
                                "music" -> "https://music.youtube.com/"
                                "drive" -> "https://drive.google.com/"
                                else -> "https://google.com/"
                            }
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                        "location" -> {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                        "api" -> {
                            // Placeholder for Places API
                        }
                    }
                }
            }
        }
        
        if (mcpTools.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Registered MCP Webhooks (${mcpTools.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(mcpTools.size) { index ->
                    val tool = mcpTools[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { viewModel.launchApp(tool.packageName) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = tool.appName, fontWeight = FontWeight.Bold)
                            Text(text = tool.packageName, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IntegrationCard(item: IntegrationTile, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
