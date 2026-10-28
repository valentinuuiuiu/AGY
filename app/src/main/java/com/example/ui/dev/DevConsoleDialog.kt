package com.example.ui.dev

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.config.ApiConfigManager
import com.example.data.config.ApiSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevConsoleDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val configManager = remember { ApiConfigManager.getInstance(context) }
    var settings by remember { mutableStateOf(configManager.getSettings()) }
    var activeTab by remember { mutableStateOf(0) } // 0 = Mini-Terminal, 1 = GUI Form

    // Terminal State
    var terminalInput by remember { mutableStateOf("") }
    val terminalLogs = remember {
        mutableStateListOf(
            "WanderAI Dev Terminal v1.2",
            "Scrie 'help' pentru lista comenzilor sau 'env' pentru configuratia activa.",
            "wanderai@android:~$ status"
        )
    }

    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            tonalElevation = 16.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = null,
                            tint = Color(0xFF06B6D4),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Developer Console & API Keys",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Închide", tint = Color(0xFF94A3B8))
                    }
                }

                // Mode Selector Tabs
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color(0xFF06B6D4),
                    divider = {}
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("📟 Mini-Terminal (CLI)", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("⚙️ settings.json / Formular", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                }

                // Toast Feedback
                toastMessage?.let { msg ->
                    Surface(
                        color = Color(0xFF06B6D4).copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            color = Color(0xFF06B6D4),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (activeTab == 0) {
                        // Terminal Tab
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            // Quick Command Chips
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val commands = listOf("help", "env", "test-ping", "export-json", "export-shell", "clear")
                                items(commands) { cmd ->
                                    Surface(
                                        onClick = {
                                            executeTerminalCommand(
                                                cmd = cmd,
                                                configManager = configManager,
                                                logs = terminalLogs,
                                                onSettingsUpdated = { settings = it }
                                            )
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF1E293B),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                                    ) {
                                        Text(
                                            text = cmd,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            color = Color(0xFF38BDF8),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Terminal Output Box
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF020617)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(terminalLogs) { log ->
                                        Text(
                                            text = log,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            color = if (log.startsWith("wanderai@android:~$")) Color(0xFF38BDF8)
                                            else if (log.startsWith("ERROR:")) Color(0xFFF87171)
                                            else Color(0xFF4ADE80)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Terminal Input Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = terminalInput,
                                    onValueChange = { terminalInput = it },
                                    placeholder = { Text("ex: export GEMINI_API_KEY=AIzaSy...", color = Color(0xFF64748B), fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Color.White),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF1E293B),
                                        unfocusedContainerColor = Color(0xFF0F172A),
                                        focusedBorderColor = Color(0xFF06B6D4),
                                        unfocusedBorderColor = Color(0xFF334155)
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = {
                                        if (terminalInput.isNotBlank()) {
                                            executeTerminalCommand(
                                                cmd = terminalInput.trim(),
                                                configManager = configManager,
                                                logs = terminalLogs,
                                                onSettingsUpdated = { settings = it }
                                            )
                                            terminalInput = ""
                                        }
                                    })
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (terminalInput.isNotBlank()) {
                                            executeTerminalCommand(
                                                cmd = terminalInput.trim(),
                                                configManager = configManager,
                                                logs = terminalLogs,
                                                onSettingsUpdated = { settings = it }
                                            )
                                            terminalInput = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0xFF06B6D4), RoundedCornerShape(12.dp))
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Trimite", tint = Color.Black)
                                }
                            }
                        }
                    } else {
                        // GUI Form Tab
                        var geminiKey by remember { mutableStateOf(settings.geminiApiKey) }
                        var openaiKey by remember { mutableStateOf(settings.openaiApiKey) }
                        var baseUrl by remember { mutableStateOf(settings.baseUrl) }
                        var modelName by remember { mutableStateOf(settings.modelName) }
                        var provider by remember { mutableStateOf(settings.provider) }
                        var showKeys by remember { mutableStateOf(false) }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Active AI Provider:", fontWeight = FontWeight.Bold, color = Color.White)
                                    Row {
                                        FilterChip(
                                            selected = provider == "gemini",
                                            onClick = { provider = "gemini" },
                                            label = { Text("Gemini Native") }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        FilterChip(
                                            selected = provider == "openai",
                                            onClick = { provider = "openai" },
                                            label = { Text("OpenAI / v1") }
                                        )
                                    }
                                }
                            }

                            item {
                                OutlinedTextField(
                                    value = geminiKey,
                                    onValueChange = { geminiKey = it },
                                    label = { Text("Gemini API Key") },
                                    visualTransformation = if (showKeys) VisualTransformation.None else PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    trailingIcon = {
                                        IconButton(onClick = { showKeys = !showKeys }) {
                                            Icon(if (showKeys) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF94A3B8))
                                        }
                                    }
                                )
                            }

                            item {
                                OutlinedTextField(
                                    value = openaiKey,
                                    onValueChange = { openaiKey = it },
                                    label = { Text("OpenAI / Compatible API Key") },
                                    visualTransformation = if (showKeys) VisualTransformation.None else PasswordVisualTransformation(),
                                    placeholder = { Text("sk-...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            item {
                                OutlinedTextField(
                                    value = baseUrl,
                                    onValueChange = { baseUrl = it },
                                    label = { Text("API Base URL") },
                                    placeholder = { Text("https://generativelanguage.googleapis.com/") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    PresetChip("Google Gemini") { baseUrl = "https://generativelanguage.googleapis.com/" }
                                    PresetChip("OpenAI") { baseUrl = "https://api.openai.com/v1/" }
                                    PresetChip("OpenRouter") { baseUrl = "https://openrouter.ai/api/v1/" }
                                }
                            }

                            item {
                                OutlinedTextField(
                                    value = modelName,
                                    onValueChange = { modelName = it },
                                    label = { Text("Model Name") },
                                    placeholder = { Text("gemini-3.8-flash") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    PresetChip("gemini-3.8-flash") { modelName = "gemini-3.8-flash" }
                                    PresetChip("gemini-3.5-flash-lite") { modelName = "gemini-3.5-flash-lite" }
                                    PresetChip("gpt-4o") { modelName = "gpt-4o" }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Save button
                                    Button(
                                        onClick = {
                                            val updated = ApiSettings(
                                                geminiApiKey = geminiKey.trim(),
                                                openaiApiKey = openaiKey.trim(),
                                                baseUrl = baseUrl.trim(),
                                                modelName = modelName.trim(),
                                                provider = provider
                                            )
                                            configManager.saveSettings(updated)
                                            settings = updated
                                            toastMessage = "Configurația a fost salvată cu succes!"
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Salvează Config", fontWeight = FontWeight.Bold)
                                    }

                                    // Copy JSON button
                                    OutlinedButton(
                                        onClick = {
                                            val json = configManager.exportSettingsJson()
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("settings.json", json))
                                            toastMessage = "settings.json copiat în clipboard!"
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copiază JSON")
                                    }

                                    // Import JSON button
                                    OutlinedButton(
                                        onClick = { showImportDialog = true },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Importă")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Importă settings.json") },
            text = {
                OutlinedTextField(
                    value = importText,
                    onValueChange = { importText = it },
                    placeholder = { Text("{\"gemini_api_key\": \"...\", \"base_url\": \"...\"}") },
                    minLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = configManager.importSettingsJson(importText.trim())
                        if (success) {
                            settings = configManager.getSettings()
                            toastMessage = "Setările au fost importate cu succes!"
                            showImportDialog = false
                        } else {
                            toastMessage = "Format JSON invalid!"
                        }
                    }
                ) {
                    Text("Aplică")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Anulează")
                }
            }
        )
    }
}

@Composable
private fun PresetChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFF1E293B)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF38BDF8),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

private fun executeTerminalCommand(
    cmd: String,
    configManager: ApiConfigManager,
    logs: MutableList<String>,
    onSettingsUpdated: (ApiSettings) -> Unit
) {
    logs.add("wanderai@android:~$ $cmd")
    val trimmed = cmd.trim()

    when {
        trimmed == "help" -> {
            logs.add("Comenzi disponibile:")
            logs.add("  export GEMINI_API_KEY=\"<key>\"")
            logs.add("  export OPENAI_API_KEY=\"<key>\"")
            logs.add("  export BASE_URL=\"<url>\"")
            logs.add("  export MODEL=\"<model_name>\"")
            logs.add("  export PROVIDER=\"gemini|openai\"")
            logs.add("  env | status       - Afișează variabilele curente")
            logs.add("  export-json        - Printează JSON settings complet")
            logs.add("  export-shell       - Printează comenzi bash export")
            logs.add("  test-ping          - Verifică conectivitatea API")
            logs.add("  reset              - Resetează setările la valorile default")
            logs.add("  clear              - Curăță ecranul terminalului")
        }
        trimmed == "clear" -> {
            logs.clear()
            logs.add("wanderai@android:~$")
        }
        trimmed == "status" || trimmed == "env" -> {
            val s = configManager.getSettings()
            val maskedGemini = if (s.geminiApiKey.length > 8) s.geminiApiKey.take(4) + "••••••••" + s.geminiApiKey.takeLast(4) else if (s.geminiApiKey.isEmpty()) "(nelocalizată)" else "••••"
            val maskedOpenAi = if (s.openaiApiKey.length > 8) s.openaiApiKey.take(4) + "••••••••" + s.openaiApiKey.takeLast(4) else if (s.openaiApiKey.isEmpty()) "(nelocalizată)" else "••••"
            logs.add("PROVIDER=${s.provider}")
            logs.add("MODEL=${s.modelName}")
            logs.add("BASE_URL=${s.baseUrl}")
            logs.add("GEMINI_API_KEY=$maskedGemini")
            logs.add("OPENAI_API_KEY=$maskedOpenAi")
        }
        trimmed.startsWith("export GEMINI_API_KEY=") -> {
            val value = trimmed.removePrefix("export GEMINI_API_KEY=").trim('"', '\'', ' ')
            val current = configManager.getSettings()
            val updated = current.copy(geminiApiKey = value)
            configManager.saveSettings(updated)
            onSettingsUpdated(updated)
            logs.add("SUCCESS: GEMINI_API_KEY actualizată.")
        }
        trimmed.startsWith("export OPENAI_API_KEY=") -> {
            val value = trimmed.removePrefix("export OPENAI_API_KEY=").trim('"', '\'', ' ')
            val current = configManager.getSettings()
            val updated = current.copy(openaiApiKey = value)
            configManager.saveSettings(updated)
            onSettingsUpdated(updated)
            logs.add("SUCCESS: OPENAI_API_KEY actualizată.")
        }
        trimmed.startsWith("export BASE_URL=") -> {
            val value = trimmed.removePrefix("export BASE_URL=").trim('"', '\'', ' ')
            val current = configManager.getSettings()
            val updated = current.copy(baseUrl = value)
            configManager.saveSettings(updated)
            onSettingsUpdated(updated)
            logs.add("SUCCESS: BASE_URL setat la: $value")
        }
        trimmed.startsWith("export MODEL=") -> {
            val value = trimmed.removePrefix("export MODEL=").trim('"', '\'', ' ')
            val current = configManager.getSettings()
            val updated = current.copy(modelName = value)
            configManager.saveSettings(updated)
            onSettingsUpdated(updated)
            logs.add("SUCCESS: MODEL setat la: $value")
        }
        trimmed.startsWith("export PROVIDER=") -> {
            val value = trimmed.removePrefix("export PROVIDER=").trim('"', '\'', ' ').lowercase()
            val current = configManager.getSettings()
            val updated = current.copy(provider = if (value.contains("openai")) "openai" else "gemini")
            configManager.saveSettings(updated)
            onSettingsUpdated(updated)
            logs.add("SUCCESS: PROVIDER setat la: ${updated.provider}")
        }
        trimmed == "export-json" -> {
            logs.add("=== settings.json ===")
            configManager.exportSettingsJson().lines().forEach { logs.add(it) }
        }
        trimmed == "export-shell" -> {
            logs.add("=== shell export ===")
            configManager.exportShellCommands().lines().forEach { logs.add(it) }
        }
        trimmed == "test-ping" -> {
            val s = configManager.getSettings()
            logs.add("Testing connection to ${s.baseUrl}...")
            logs.add("Active model: ${s.modelName} (Provider: ${s.provider})")
            if (s.geminiApiKey.isNotBlank() || s.openaiApiKey.isNotBlank()) {
                logs.add("SUCCESS: API Key găsită și pregătită pentru apeluri.")
            } else {
                logs.add("WARNING: Nicio cheie API configurată!")
            }
        }
        trimmed == "reset" -> {
            configManager.resetToDefaults()
            val def = configManager.getSettings()
            onSettingsUpdated(def)
            logs.add("Setările au fost resetate la valorile implicite.")
        }
        else -> {
            logs.add("ERROR: Comandă necunoscută: '$trimmed'. Scrie 'help' pentru instrucțiuni.")
        }
    }
}
