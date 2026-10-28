package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.room.SavedPhrase
import com.example.data.translation.Language
import com.example.ui.dev.DevConsoleDialog
import kotlinx.coroutines.flow.collectLatest
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslateScreen(viewModel: TranslateViewModel = viewModel()) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val primaryLang by viewModel.primaryLang.collectAsState()
    val targetLang by viewModel.targetLang.collectAsState()
    val isTranslating by viewModel.isTranslating.collectAsState()
    val conversation by viewModel.conversation.collectAsState()
    val lensResult by viewModel.lensResult.collectAsState()
    val isLensAnalyzing by viewModel.isLensAnalyzing.collectAsState()
    val savedPhrases by viewModel.savedPhrases.collectAsState()
    val fullscreenPhrase by viewModel.fullscreenPhrase.collectAsState()
    val swapAnimationCounter by viewModel.swapAnimationCounter.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showPrimaryLangPicker by remember { mutableStateOf(false) }
    var showTargetLangPicker by remember { mutableStateOf(false) }
    var showTextInputDialog by remember { mutableStateOf<Boolean?>(null) } // true for primary, false for target
    var showDevConsole by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Fullscreen Large Card Modal (Show to Local)
    fullscreenPhrase?.let { phrase ->
        Dialog(
            onDismissRequest = { viewModel.showFullscreenCard(null) },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "👀 Arată ecranul localnicului:",
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, Color(0xFF06B6D4), RoundedCornerShape(28.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = phrase,
                                color = Color.White,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = 44.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                    Button(
                        onClick = { viewModel.showFullscreenCard(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Închide")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Închide Ecranul Mare", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Bar
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFF06B6D4).copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Translate,
                                    contentDescription = "Traducere",
                                    tint = Color(0xFF06B6D4),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "WanderAI Translator",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Universal Travel Communicator",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedTab == 0 && conversation.isNotEmpty()) {
                                IconButton(onClick = { viewModel.clearConversation() }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Curăță conversația", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { showDevConsole = true }) {
                                Icon(Icons.Default.Terminal, contentDescription = "Developer Settings & CLI", tint = Color(0xFF06B6D4))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Mode Navigation Tabs
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { viewModel.setTab(0) },
                            text = { Text("Conversație Live", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                            icon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { viewModel.setTab(1) },
                            text = { Text("Meniu / Cameră", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                            icon = { Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { viewModel.setTab(2) },
                            text = { Text("Ghid Offline", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                            icon = { Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
            }

            // Language Selector Bar (Visible for Conversation Mode & Phrasebook)
            if (selectedTab == 0 || selectedTab == 2) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Primary Lang Button
                        Surface(
                            onClick = { showPrimaryLangPicker = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(primaryLang.flag, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    primaryLang.nameRo,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        // Swap Button
                        IconButton(
                            onClick = { viewModel.swapLanguages() },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = "Schimbă limbile",
                                tint = Color(0xFF06B6D4)
                            )
                        }

                        // Target Lang Button
                        Surface(
                            onClick = { showTargetLangPicker = true },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(targetLang.flag, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    targetLang.nameRo,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> DualConversationTab(
                        conversation = conversation,
                        isTranslating = isTranslating,
                        primaryLang = primaryLang,
                        targetLang = targetLang,
                        swapCounter = swapAnimationCounter,
                        onSwapLanguages = { viewModel.swapLanguages() },
                        onSpeak = { text, lang -> viewModel.speakText(text, lang) },
                        onShowFullscreen = { text -> viewModel.showFullscreenCard(text) },
                        onStartInput = { isPrimary -> showTextInputDialog = isPrimary }
                    )
                    1 -> VisualLensTab(
                        lensResult = lensResult,
                        isAnalyzing = isLensAnalyzing,
                        onAnalyzeBitmap = { base64 -> viewModel.analyzeCameraImage(base64) },
                        onSpeak = { text -> viewModel.speakText(text, Language.findByCode("ro")) }
                    )
                    2 -> OfflinePhrasebookTab(
                        savedPhrases = savedPhrases,
                        primaryLang = primaryLang,
                        targetLang = targetLang,
                        onSpeak = { text, code -> viewModel.speakText(text, Language.findByCode(code)) },
                        onShowFullscreen = { text -> viewModel.showFullscreenCard(text) },
                        onToggleFavorite = { phrase -> viewModel.toggleFavorite(phrase) },
                        onDelete = { id -> viewModel.deletePhrase(id) }
                    )
                }
            }
        }
    }

    // Input Dialog for Speech / Text Translation
    showTextInputDialog?.let { isPrimary ->
        var inputText by remember { mutableStateOf("") }
        val source = if (isPrimary) primaryLang else targetLang
        val dest = if (isPrimary) targetLang else primaryLang

        AlertDialog(
            onDismissRequest = { showTextInputDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(source.flag, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isPrimary) "Vorbește sau scrie (Tu)" else "Răspuns Localnic (${source.nameRo})")
                }
            },
            text = {
                Column {
                    Text(
                        "Traduce în ${dest.flag} ${dest.nameRo} cu asistență fonetică și sfaturi culturale:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text(if (isPrimary) "ex: Cât costă biletul până în centru?" else "Introduceți textul în ${source.nameRo}...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val text = inputText.trim()
                        if (text.isNotEmpty()) {
                            viewModel.translateMessage(text, isPrimary)
                            showTextInputDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isPrimary) Color(0xFF06B6D4) else Color(0xFFF59E0B))
                ) {
                    Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Traduce & Pronunță")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextInputDialog = null }) {
                    Text("Anulează")
                }
            }
        )
    }

    // Language Pickers
    if (showPrimaryLangPicker) {
        LanguagePickerModal(
            currentLang = primaryLang,
            title = "Alege limba ta principală",
            onSelect = {
                viewModel.setPrimaryLanguage(it)
                showPrimaryLangPicker = false
            },
            onDismiss = { showPrimaryLangPicker = false }
        )
    }

    if (showTargetLangPicker) {
        LanguagePickerModal(
            currentLang = targetLang,
            title = "Alege limba destinației / localnicului",
            onSelect = {
                viewModel.setTargetLanguage(it)
                showTargetLangPicker = false
            },
            onDismiss = { showTargetLangPicker = false }
        )
    }

    if (showDevConsole) {
        DevConsoleDialog(onDismiss = { showDevConsole = false })
    }
}

@Composable
fun DualConversationTab(
    conversation: List<ConversationMessage>,
    isTranslating: Boolean,
    primaryLang: Language,
    targetLang: Language,
    swapCounter: Int,
    onSwapLanguages: () -> Unit,
    onSpeak: (String, Language) -> Unit,
    onShowFullscreen: (String) -> Unit,
    onStartInput: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Conversation Feed
        if (conversation.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFF06B6D4).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = Color(0xFF06B6D4),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Traducere Față-în-Față Live",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Apasă pe unul dintre butoanele de jos pentru a vorbi. Aplicația va traduce automat textul, îl va pronunța cu voce tare și îți va oferi un ghid fonetic ușor de rostit.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(conversation) { msg ->
                    ConversationBubble(
                        message = msg,
                        primaryLang = primaryLang,
                        targetLang = targetLang,
                        onSpeak = onSpeak,
                        onShowFullscreen = onShowFullscreen
                    )
                }
                if (isTranslating) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF06B6D4))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("WanderAI traduce cu atenție la nuanțe culturale...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }

        // Push-to-Talk Dual Action Controls
        // Push-to-Talk Dual Action Controls with Persistent Two-Way Language Toggle Dock
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Persistent Language Quick-Switch Dock
                val rotationAngle by animateFloatAsState(
                    targetValue = swapCounter * 180f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "SwapRotation"
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Source Language Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(primaryLang.flag, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = primaryLang.nameRo,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("Sursă (Tu)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF06B6D4), fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Interactive Animated Quick-Switch Button Toggle
                        FilledTonalButton(
                            onClick = onSwapLanguages,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF06B6D4).copy(alpha = 0.2f),
                                contentColor = Color(0xFF06B6D4)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = "Schimbă limbile rapid",
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer(rotationZ = rotationAngle)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Comută ⇄", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // Target Language Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = targetLang.nameRo,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text("Țintă (Localnic)", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF59E0B), fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(targetLang.flag, fontSize = 20.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Traveler Button
                    Button(
                        onClick = { onStartInput(true) },
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4))
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Vorbește Tu", modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("Tu (${primaryLang.flag})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Apasă să vorbești", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }

                    // Local Speaker Button
                    Button(
                        onClick = { onStartInput(false) },
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                    ) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = "Răspunde Localnic", modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("Localnic (${targetLang.flag})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Apasă pentru răspuns", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationBubble(
    message: ConversationMessage,
    primaryLang: Language,
    targetLang: Language,
    onSpeak: (String, Language) -> Unit,
    onShowFullscreen: (String) -> Unit
) {
    val destLang = if (message.isPrimaryUser) targetLang else primaryLang
    val bubbleColor = if (message.isPrimaryUser) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    } else {
        Color(0xFFF59E0B).copy(alpha = 0.15f)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = bubbleColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (message.isPrimaryUser) Color(0xFF06B6D4) else Color(0xFFF59E0B)
                )

                Row {
                    IconButton(
                        onClick = { onSpeak(message.translatedText, destLang) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Pronunță audio", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { onShowFullscreen(message.translatedText) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Fullscreen, contentDescription = "Arată mare", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message.originalText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message.translatedText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (message.phonetic.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🗣️ Pronunție: ${message.phonetic}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF06B6D4),
                    fontWeight = FontWeight.Medium
                )
            }

            if (message.culturalTip.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "💡 Sfat călătorie: ${message.culturalTip}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun VisualLensTab(
    lensResult: LensAnalysisResult?,
    isAnalyzing: Boolean,
    onAnalyzeBitmap: (String) -> Unit,
    onSpeak: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            selectedImageBitmap = it
            val outputStream = ByteArrayOutputStream()
            it.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            onAnalyzeBitmap(base64)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFF06B6D4).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Camera & Meniu Lens",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Fotografiază un meniu de restaurant, o etichetă de produs sau un indicator stradal. WanderAI traduce textul, explică ingredientele și alergenii culinari.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { photoLauncher.launch(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fotografiază Meniu / Indicator", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (isAnalyzing) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = Color(0xFF06B6D4), strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Gemini analizează imaginea...", fontWeight = FontWeight.Bold)
                            Text("Traducere și detecție ingrediente/alergeni", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        lensResult?.let { result ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF06B6D4).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFF06B6D4).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "🌐 ${result.detectedLanguage}",
                                    color = Color(0xFF06B6D4),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            IconButton(onClick = { onSpeak(result.translatedText) }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Ascultă", tint = Color(0xFF06B6D4))
                            }
                        }

                        if (result.rawExtracted.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Text detectat: \"${result.rawExtracted}\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = result.translatedText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (result.foodDescription.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.12f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "🍽️ Detalii Culinare & Alergeni:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFFF59E0B)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = result.foodDescription,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        if (result.culturalTip.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "💡 Obiceiuri & Etichetă: ${result.culturalTip}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF06B6D4)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OfflinePhrasebookTab(
    savedPhrases: List<SavedPhrase>,
    primaryLang: Language,
    targetLang: Language,
    onSpeak: (String, String) -> Unit,
    onShowFullscreen: (String) -> Unit,
    onToggleFavorite: (SavedPhrase) -> Unit,
    onDelete: (Int) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Toate") }
    val categories = listOf("Toate", "Urgențe", "Restaurant", "Transport", "Cumpărături", "Conversație")

    val filtered = remember(savedPhrases, selectedCategory) {
        if (selectedCategory == "Toate") savedPhrases
        else savedPhrases.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Categories Filter Bar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, fontSize = 12.sp) }
                )
            }
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Nicio frază salvată în această categorie.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(filtered, key = { it.id }) { phrase ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = if (phrase.category == "Urgențe") Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF06B6D4).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = phrase.category,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (phrase.category == "Urgențe") Color(0xFFEF4444) else Color(0xFF06B6D4),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = { onToggleFavorite(phrase) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            if (phrase.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = "Favorit",
                                            tint = if (phrase.isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onSpeak(phrase.translatedText, phrase.targetLanguage) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = "Pronunță", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { onShowFullscreen(phrase.translatedText) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Fullscreen, contentDescription = "Arată mare", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { onDelete(phrase.id) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Șterge", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = phrase.originalText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = phrase.translatedText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (phrase.phonetic.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "🗣️ ${phrase.phonetic}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF06B6D4)
                                )
                            }

                            if (phrase.culturalNote.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "💡 ${phrase.culturalNote}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LanguagePickerModal(
    currentLang: Language,
    title: String,
    onSelect: (Language) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                items(Language.SUPPORTED_LANGUAGES) { lang ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(lang) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(lang.flag, fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(lang.nameRo, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(lang.nativeName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (lang.code == currentLang.code) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF06B6D4))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Închide")
            }
        }
    )
}
