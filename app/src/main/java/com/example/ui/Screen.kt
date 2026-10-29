package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Live : Screen("live", "Jarvis Live", Icons.Filled.Videocam)
    object ArLens : Screen("ar_lens", "AR Lens", Icons.Filled.CenterFocusStrong)
    object Nexus : Screen("nexus", "AGY Nexus", Icons.Filled.Bolt)
    object Brain : Screen("brain", "Infinite Brain", Icons.Filled.Psychology)
    object Studio : Screen("studio", "Studio", Icons.Filled.AutoAwesome)
    object Translate : Screen("translate", "Communicator", Icons.Filled.Translate)
    object Chat : Screen("chat", "Chat", Icons.Filled.Chat)
    object Voice : Screen("voice", "Voice", Icons.Filled.Call)
    object Profile : Screen("profile", "Memory", Icons.Filled.Map)
}
