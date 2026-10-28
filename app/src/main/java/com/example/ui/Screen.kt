package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Translate : Screen("translate", "Translate", Icons.Filled.Translate)
    object Chat : Screen("chat", "Chat", Icons.Filled.Chat)
    object Voice : Screen("voice", "Voice", Icons.Filled.Call)
    object Create : Screen("create", "Create", Icons.Filled.AutoAwesome)
    object Live : Screen("live", "Live", Icons.Filled.Videocam)
    object Nexus : Screen("nexus", "Nexus", Icons.Filled.Explore)
    object Profile : Screen("profile", "Itineraries", Icons.Filled.Map)
}
