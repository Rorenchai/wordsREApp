package com.example.study.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.study.ui.dictionary.DictionaryScreen
import com.example.study.ui.favorites.FavoritesScreen
import com.example.study.ui.games.GamesScreen
import com.example.study.ui.listening.ListeningScreen
import com.example.study.ui.match.MatchScreen
import com.example.study.ui.plan.PlanScreen
import com.example.study.ui.plan.PlanStudyMode
import com.example.study.ui.plan.PlanStudyScreen
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
    val currentRoute = currentDestination?.route?.substringBefore("?")

    Scaffold(
        bottomBar = {
            if (currentRoute !in setOf(
                    "dictionary", "quiz", "listening", "spelling", "match", "wrongbook", "review", "vocab",
                    "plan", "plannew", "planreview"
                )
            ) {
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
                            icon = {
                                // 选中页签图标弹跳放大，强化当前页面反馈
                                val iconScale by animateFloatAsState(
                                    targetValue = if (selected) 1.12f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    label = "nav-icon-scale"
                                )
                                Icon(
                                    item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    }
                                )
                            },
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
            enterTransition = {
                fadeIn(tween(durationMillis = 220)) +
                    slideInHorizontally(tween(durationMillis = 260), initialOffsetX = { it / 14 })
            },
            exitTransition = {
                fadeOut(tween(durationMillis = 160)) +
                    slideOutHorizontally(tween(durationMillis = 200), targetOffsetX = { -it / 14 })
            },
            popEnterTransition = {
                fadeIn(tween(durationMillis = 200)) +
                    slideInHorizontally(tween(durationMillis = 240), initialOffsetX = { -it / 14 })
            },
            popExitTransition = {
                fadeOut(tween(durationMillis = 160)) +
                    slideOutHorizontally(tween(durationMillis = 200), targetOffsetX = { it / 14 })
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable("study") {
                StudyScreen(
                    onOpenDictionary = {
                        navController.navigate("dictionary") { launchSingleTop = true }
                    },
                    onOpenPlan = {
                        navController.navigate("plan") { launchSingleTop = true }
                    }
                )
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
                    onOpenMatch = { planMode ->
                        navController.navigate("match?planMode=$planMode") { launchSingleTop = true }
                    },
                    onOpenQuiz = { planMode ->
                        navController.navigate("quiz?planMode=$planMode") { launchSingleTop = true }
                    },
                    onOpenListening = { planMode ->
                        navController.navigate("listening?planMode=$planMode") { launchSingleTop = true }
                    },
                    onOpenSpelling = { planMode ->
                        navController.navigate("spelling?planMode=$planMode") { launchSingleTop = true }
                    }
                )
            }
            composable("settings") { SettingsScreen() }
            composable("dictionary") {
                DictionaryScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = "quiz?planMode={planMode}",
                arguments = listOf(navArgument("planMode") {
                    type = NavType.BoolType
                    defaultValue = false
                })
            ) { entry ->
                QuizScreen(
                    onBack = { navController.popBackStack() },
                    planMode = entry.arguments?.getBoolean("planMode") ?: false
                )
            }
            composable(
                route = "listening?planMode={planMode}",
                arguments = listOf(navArgument("planMode") {
                    type = NavType.BoolType
                    defaultValue = false
                })
            ) { entry ->
                ListeningScreen(
                    onBack = { navController.popBackStack() },
                    planMode = entry.arguments?.getBoolean("planMode") ?: false
                )
            }
            composable(
                route = "spelling?planMode={planMode}",
                arguments = listOf(navArgument("planMode") {
                    type = NavType.BoolType
                    defaultValue = false
                })
            ) { entry ->
                SpellingScreen(
                    onBack = { navController.popBackStack() },
                    planMode = entry.arguments?.getBoolean("planMode") ?: false
                )
            }
            composable(
                route = "match?planMode={planMode}",
                arguments = listOf(navArgument("planMode") {
                    type = NavType.BoolType
                    defaultValue = false
                })
            ) { entry ->
                MatchScreen(
                    onBack = { navController.popBackStack() },
                    planMode = entry.arguments?.getBoolean("planMode") ?: false
                )
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
            composable("plan") {
                PlanScreen(
                    onBack = { navController.popBackStack() },
                    onStartNew = { navController.navigate("plannew") { launchSingleTop = true } },
                    onStartReview = { navController.navigate("planreview") { launchSingleTop = true } }
                )
            }
            composable("plannew") {
                PlanStudyScreen(
                    mode = PlanStudyMode.NEW,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("planreview") {
                PlanStudyScreen(
                    mode = PlanStudyMode.REVIEW,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}