package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Chat : Screen("chat", "Chat", Icons.Filled.Email)
    object Create : Screen("create", "Create", Icons.Filled.Edit)
    object Voice : Screen("voice", "Voice", Icons.Filled.Call)
    object Nexus : Screen("nexus", "Nexus", Icons.Filled.Star)
    object Profile : Screen("profile", "Profile", Icons.Filled.Person)
}
