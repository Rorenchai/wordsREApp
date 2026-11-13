package com.example.study.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.study.ui.dictionary.DictionaryScreen
import com.example.study.ui.favorites.FavoritesScreen
import com.example.study.ui.games.GamesScreen
import com.example.study.ui.listening.ListeningScreen
import com.example.study.ui.match.MatchScreen
import com.example.study.ui.quiz.QuizScreen
import com.example.study.ui.review.ReviewScreen
import com.example.study.ui.spelling.SpellingScreen
import com.example.study.ui.vocab.VocabTestScreen
import com.example.study.ui.wrongbook.WrongBookScreen
import com.example.study.ui.records.RecordsScreen
import com.example.study.ui.settings.SettingsScreen
import com.example.study.ui.study.StudyScreen
import com.example.study.ui.theme.DuoBackground
import com.example.study.ui.theme.DuoGreen
import com.example.study.ui.theme.DuoTextSecondary

/** 底部导航的四个页签。 */
private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem("study", "学习", Icons.Filled.School),
    BottomNavItem("favorites", "收藏", Icons.Filled.Star),
    BottomNavItem("records", "记录", Icons.Filled.BarChart),
    BottomNavItem("games", "游戏", Icons.Filled.VideogameAsset),
    BottomNavItem("settings", "词库", Icons.AutoMirrored.Filled.MenuBook)
)

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            if (currentDestination?.route !in setOf("dictionary", "quiz", "listening", "spelling", "match", "wrongbook", "review", "vocab")) {
                NavigationBar(containerColor = DuoBackground) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = DuoGreen,
                                selectedTextColor = DuoGreen,
                                unselectedIconColor = DuoTextSecondary,
                                unselectedTextColor = DuoTextSecondary,
                                indicatorColor = DuoGreen.copy(alpha = 0.14f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "study",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable("study") {
                StudyScreen(onOpenDictionary = {
                    navController.navigate("dictionary") { launchSingleTop = true }
                })
            }
            composable("records") {
                RecordsScreen(
                    onOpenWrongBook = {
                        navController.navigate("wrongbook") { launchSingleTop = true }
                    },
                    onOpenVocab = {
                        navController.navigate("vocab") { launchSingleTop = true }
                    }
                )
            }
            composable("favorites") { FavoritesScreen() }
            composable("games") {
                GamesScreen(
                    onOpenMatch = { navController.navigate("match") { launchSingleTop = true } },
                    onOpenQuiz = { navController.navigate("quiz") { launchSingleTop = true } },
                    onOpenListening = { navController.navigate("listening") { launchSingleTop = true } },
                    onOpenSpelling = { navController.navigate("spelling") { launchSingleTop = true } }
                )
            }
            composable("settings") { SettingsScreen() }
            composable("dictionary") {
                DictionaryScreen(onBack = { navController.popBackStack() })
            }
            composable("quiz") {
                QuizScreen(onBack = { navController.popBackStack() })
            }
            composable("listening") {
                ListeningScreen(onBack = { navController.popBackStack() })
            }
            composable("spelling") {
                SpellingScreen(onBack = { navController.popBackStack() })
            }
            composable("match") {
                MatchScreen(onBack = { navController.popBackStack() })
            }
            composable("wrongbook") {
                WrongBookScreen(
                    onBack = { navController.popBackStack() },
                    onStartReview = { navController.navigate("review") { launchSingleTop = true } }
                )
            }
            composable("review") {
                ReviewScreen(onBack = { navController.popBackStack() })
            }
            composable("vocab") {
                VocabTestScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}