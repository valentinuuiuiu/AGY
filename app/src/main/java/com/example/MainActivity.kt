package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.Screen
import com.example.ui.auth.AuthScreen
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                WanderAIApp()
            }
        }
    }
}

@Composable
fun WanderAIApp() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(onAuthSuccess = { currentUser = Firebase.auth.currentUser })
    } else {
        MainAppContent()
    }
}

@Composable
fun MainAppContent() {
    val navController = rememberNavController()
    val screens = listOf(Screen.Translate, Screen.Create, Screen.Nexus, Screen.Live, Screen.Profile)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Atmospheric Background Glow
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.Center)
                    .offset(x = (-40).dp, y = (-40).dp)
                    .blur(80.dp)
                    .background(Color(0xFF6366F1).copy(alpha = 0.3f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .align(Alignment.Center)
                    .offset(x = 60.dp, y = 60.dp)
                    .blur(100.dp)
                    .background(Color(0xFF9333EA).copy(alpha = 0.3f), CircleShape)
            )

            NavHost(
                navController = navController,
                startDestination = Screen.Translate.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Translate.route) { TranslateScreen() }
                composable(Screen.Chat.route) { ChatScreen() }
                composable(Screen.Create.route) { CreateScreen() }
                composable(Screen.Nexus.route) { NexusScreen() }
                composable(Screen.Live.route) { GeminiLiveScreen() }
                composable(Screen.Voice.route) { VoiceScreen() }
                composable(Screen.Profile.route) { ProfileScreen() }
            }
        }
    }
}
