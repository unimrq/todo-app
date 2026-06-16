package com.example.todoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.todoapp.ui.calendar.CalendarScreen
import com.example.todoapp.ui.kanban.KanbanScreen
import com.example.todoapp.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TodoAppTheme {
                TodoApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoAppTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        primary = GitGreen,
        onPrimary = GitBg,
        primaryContainer = GitGreenDim,
        secondary = GitBlue,
        tertiary = GitPurple,
        background = GitBg,
        surface = GitSurface,
        surfaceVariant = GitBorder,
        onBackground = GitText,
        onSurface = GitText,
        onSurfaceVariant = GitTextSecondary,
        error = GitRed,
        outline = GitBorder
    )
    MaterialTheme(colorScheme = colorScheme, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoApp() {
    val navController = rememberNavController()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = GitSurface,
                contentColor = GitText
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {},
                    label = { Text("日历") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GitGreen,
                        selectedTextColor = GitGreen,
                        unselectedTextColor = GitTextSecondary,
                        indicatorColor = GitBorder
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {},
                    label = { Text("看板") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GitGreen,
                        selectedTextColor = GitGreen,
                        unselectedTextColor = GitTextSecondary,
                        indicatorColor = GitBorder
                    )
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> CalendarScreen(modifier = Modifier.padding(padding))
            1 -> KanbanScreen(modifier = Modifier.padding(padding))
        }
    }
}
