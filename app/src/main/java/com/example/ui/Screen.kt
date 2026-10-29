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
    object Translate : Screen("translate", "Communicator", Icons.Filled.Translate)
    object Chat : Screen("chat", "Chat", Icons.Filled.Chat)
    object Voice : Screen("voice", "Voice", Icons.Filled.Call)
    object Create : Screen("create", "Generator", Icons.Filled.AutoAwesome)
    object Live : Screen("live", "Vision Live", Icons.Filled.Videocam)
    object Nexus : Screen("nexus", "Agent Session", Icons.Filled.Explore)
    object Profile : Screen("profile", "Memory", Icons.Filled.Map)
}
