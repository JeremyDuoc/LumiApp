package com.jeremy.lumi.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jeremy.lumi.ui.screens.home.HomeScreen
import com.jeremy.lumi.ui.screens.settings.SettingsScreen
import com.jeremy.lumi.ui.screens.calendar.CalendarScreen
import com.jeremy.lumi.ui.screens.forum.ForumScreen
import com.jeremy.lumi.ui.screens.chat.ChatScreen
import com.jeremy.lumi.ui.screens.chat.ConversationsScreen
import com.jeremy.lumi.ui.screens.insights.InsightsScreen
import com.jeremy.lumi.ui.screens.onboarding.OnboardingScreen
import com.jeremy.lumi.ui.screens.splash.SplashScreen
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel

// ─────────────────────────────────────────────
//  Grafo raíz: Splash → Main
// ─────────────────────────────────────────────

/**
 * Punto de entrada de la UI. Define el grafo de navegación completo:
 *
 * ```
 * AppNavGraph
 *  ├── splash   → SplashScreen
 *  └── main     → MainTabsScreen  (contiene el NavHost con pestañas)
 * ```
 *
 * [AppRoutes.SPLASH] es el `startDestination`, garantizando que la animación
 * de entrada se muestre siempre en el primer arranque.
 */
@Composable
fun AppNavGraph() {
    val rootNavController = rememberNavController()

    NavHost(
        navController = rootNavController,
        startDestination = AppRoutes.SPLASH
    ) {
        // ── Splash ──────────────────────────────────────────────────────────
        composable(route = AppRoutes.SPLASH) {
            // Leemos si el onboarding ya fue completado para decidir el destino
            val vm: NavDecisionViewModel = hiltViewModel()
            val onboardingDone by vm.onboardingDone.collectAsState()

            SplashScreen(
                onSplashFinished = {
                    val destination = if (onboardingDone) AppRoutes.MAIN else AppRoutes.ONBOARDING
                    rootNavController.navigate(destination) {
                        popUpTo(AppRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // ── Onboarding (solo la primera vez) ────────────────────────────────
        composable(route = AppRoutes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    rootNavController.navigate(AppRoutes.MAIN) {
                        popUpTo(AppRoutes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        // ── Main (tabs + bottom nav) ─────────────────────────────────────────
        composable(route = AppRoutes.MAIN) {
            MainScreen()
        }

        // ── Insights / Estadísticas ──────────────────────────────────────────
        composable(route = AppRoutes.INSIGHTS) {
            InsightsScreen(
                onNavigateBack = { rootNavController.popBackStack() }
            )
        }
    }
}

// ─────────────────────────────────────────────
//  Pantalla principal con bottom navigation
// ─────────────────────────────────────────────

/**
 * Contiene el [Scaffold] con la barra de navegación inferior y el
 * [NavHost] de pestañas. Se navega aquí después del Splash.
 */
@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { paddingValues ->
        // Aquí es donde las pantallas se inyectan dinámicamente
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    // Insights es ahora una pestaña interna: navegar a ella directamente
                    onNavigateToInsights = {
                        navController.navigate(BottomNavItem.Insights.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(BottomNavItem.Calendar.route) {
                CalendarScreen()
            }
            composable(BottomNavItem.Insights.route) {
                InsightsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("chat") { 
                ConversationsScreen(
                    onNavigateToChat = { thread ->
                        navController.navigate("chat/$thread")
                    }
                ) 
            }
            composable("chat/{thread}") { backStackEntry ->
                val thread = backStackEntry.arguments?.getString("thread") ?: "lumi"
                ChatScreen(
                    thread = thread,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(BottomNavItem.Forum.route) {
                ForumScreen()
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

// ─────────────────────────────────────────────
//  Barra de navegación inferior
// ─────────────────────────────────────────────

@Composable
fun BottomNavigationBar(navController: NavHostController, chatViewModel: com.jeremy.lumi.ui.screens.chat.ChatViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Calendar,
        BottomNavItem.Insights,
        BottomNavItem.Chat,
        BottomNavItem.Settings
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val unreadCount by chatViewModel.unreadCount.collectAsState()

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true

            NavigationBarItem(
                icon = { 
                    if (item == BottomNavItem.Chat && unreadCount > 0) {
                        BadgedBox(badge = { Badge { Text(unreadCount.toString()) } }) {
                            Icon(imageVector = item.icon, contentDescription = stringResource(id = item.titleRes))
                        }
                    } else {
                        Icon(imageVector = item.icon, contentDescription = stringResource(id = item.titleRes))
                    }
                },
                label = { Text(text = stringResource(id = item.titleRes)) },
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        // Evita crear múltiples copias de la misma pantalla en la pila
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Evita recargar si tocamos la pestaña en la que ya estamos
                        launchSingleTop = true
                        // Restaura el estado previo de esa pestaña
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}