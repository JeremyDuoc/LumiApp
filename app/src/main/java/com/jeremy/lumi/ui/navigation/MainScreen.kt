package com.jeremy.lumi.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.BlendMode
import androidx.navigation.compose.*
import com.jeremy.lumi.ui.theme.LocalBrandGradient
import com.jeremy.lumi.ui.screens.home.HomeScreen
import com.jeremy.lumi.ui.screens.partner.PartnerPairingScreen
import com.jeremy.lumi.ui.screens.settings.SettingsScreen
import com.jeremy.lumi.ui.screens.calendar.CalendarScreen
import com.jeremy.lumi.ui.screens.forum.ForumScreen
import com.jeremy.lumi.ui.screens.chat.ChatScreen
import com.jeremy.lumi.ui.screens.chat.ConversationsScreen
import com.jeremy.lumi.ui.screens.insights.InsightsScreen
import com.jeremy.lumi.ui.screens.onboarding.OnboardingScreen
import com.jeremy.lumi.ui.screens.splash.SplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  Grafo raíz: Splash â†’ Main
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/**
 * Punto de entrada de la UI. Define el grafo de navegación completo.
 *
 * Todos los usuarios (incluidos los observadores) van a MAIN después del
 * onboarding. La lógica de qué muestra el Home según el rol del usuario
 * es responsabilidad de HomeViewModel y HomeScreen.
 */
@Composable
fun AppNavGraph() {
    val rootNavController = rememberNavController()

    NavHost(
        navController = rootNavController,
        startDestination = AppRoutes.SPLASH,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
        exitTransition = { fadeOut(tween(300)) },
        popEnterTransition = { fadeIn(tween(300)) },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
    ) {
        // â”€â”€ Splash â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        composable(route = AppRoutes.SPLASH) {
            val vm: NavDecisionViewModel = hiltViewModel()
            val onboardingDone by vm.onboardingDone.collectAsStateWithLifecycle()

            SplashScreen(
                onboardingState = onboardingDone,
                onSplashFinished = { state ->
                    val destination = if (state == false) AppRoutes.ONBOARDING else AppRoutes.MAIN
                    rootNavController.navigate(destination) {
                        popUpTo(AppRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // â”€â”€ Onboarding (solo la primera vez) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        composable(route = AppRoutes.ONBOARDING) {
            OnboardingScreen(
                onComplete = {
                    rootNavController.navigate(AppRoutes.MAIN) {
                        popUpTo(AppRoutes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        // ——— Main (tabs + bottom nav) — para TODOS los usuarios —————————————————
        composable(route = AppRoutes.MAIN) {
            MainScreen(
                onNavigateToPartner = {
                    rootNavController.navigate(AppRoutes.PARTNER_HUB)
                }
            )
        }

        // ——— Insights / Estadísticas —————————————————————————————————————————
        composable(route = AppRoutes.INSIGHTS) {
            InsightsScreen(
                onNavigateBack = { rootNavController.popBackStack() }
            )
        }
        
        // ——— Partner Pairing (Moviendo a la raíz para manejar deep links correctamente) ———
        composable(
            route = AppRoutes.PARTNER,
            deepLinks = listOf(
                androidx.navigation.navDeepLink { uriPattern = "https://lumi.app/pair/{code}" },
                androidx.navigation.navDeepLink { uriPattern = "lumi://pair/{code}" }
            )
        ) { backStackEntry ->
            val initialCode = backStackEntry.arguments?.getString("code")
            PartnerPairingScreen(
                initialCode = initialCode?.takeIf { it.isNotBlank() },
                onNavigateBack = { rootNavController.popBackStack() }
            )
        }
        // ——— Shared Diary ——————————————————————————————————————————————————————
        composable("shared_diary/{linkId}") { backStack ->
            val linkId = backStack.arguments?.getString("linkId") ?: return@composable
            val viewModel: com.jeremy.lumi.ui.screens.partner.PartnerViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(linkId) { viewModel.observeDiary(linkId) }

            com.jeremy.lumi.ui.screens.partner.SharedDiaryScreen(
                currentUid = uiState.currentUid ?: "",
                myName = uiState.currentUserName ?: "Yo",
                myPhase = com.jeremy.lumi.domain.model.CyclePhase.UNKNOWN, // Fase actual
                entries = uiState.diaryEntries,
                isSending = uiState.isDiarySending,
                onSend = { text -> viewModel.sendDiaryEntry(linkId, text, com.jeremy.lumi.domain.model.CyclePhase.UNKNOWN) },
                onBack = { rootNavController.popBackStack() }
            )
        }

        // ——— Partner Hub (Nuevo Modo Pareja) ———————————————————————————————————
        composable(route = AppRoutes.PARTNER_HUB) {
            val viewModel: com.jeremy.lumi.ui.screens.partner.PartnerViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            // Disparador de reacciones
            val careReactionState = com.jeremy.lumi.ui.screens.partner.rememberCareReactionState()

            Box(modifier = Modifier.fillMaxSize()) {
                com.jeremy.lumi.ui.screens.partner.PartnerConnectionScreen(
                    uiState = uiState,
                    onOpenStory = { link -> rootNavController.navigate("story_detail/${link.linkId}") },
                    onSendCareAction = { linkId, action -> 
                        viewModel.sendCareAction(linkId, action)
                        careReactionState.fire(action)
                    },
                    onOpenAddPartner = { rootNavController.navigate(AppRoutes.PARTNER.replace("?code={code}", "")) },
                    onOpenDiary = { link -> rootNavController.navigate("shared_diary/${link.linkId}") },
                    onOpenDualCalendar = { link -> rootNavController.navigate("dual_calendar/${link.linkId}") }
                )
                
                // Capa superior para animaciones
                com.jeremy.lumi.ui.screens.partner.CareReactionOverlay(state = careReactionState)
            }
        }

        // ——— Story Detail ——————————————————————————————————————————————————————
        composable("story_detail/{linkId}") { backStack ->
            val linkId = backStack.arguments?.getString("linkId") ?: return@composable
            val viewModel: com.jeremy.lumi.ui.screens.partner.PartnerViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val link = uiState.activeLinks.find { it.linkId == linkId }

            if (link != null) {
                com.jeremy.lumi.ui.screens.partner.StoryDetailScreen(
                    link = link,
                    currentUid = uiState.currentUid ?: "",
                    onSendCareAction = { action -> viewModel.sendCareAction(link.linkId, action) },
                    onClose = { rootNavController.popBackStack() }
                )
            }
        }

        // ——— Dual Calendar —————————————————————————————————————————————————————
        composable("dual_calendar/{linkId}") { backStack ->
            val linkId = backStack.arguments?.getString("linkId") ?: return@composable
            val viewModel: com.jeremy.lumi.ui.screens.partner.PartnerViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val link = uiState.activeLinks.find { it.linkId == linkId }

            if (link != null) {
                com.jeremy.lumi.ui.screens.partner.DualCalendarScreen(
                    myName = uiState.currentUserName ?: "Yo",
                    partnerName = link.relationLabel.ifBlank { "Pareja" },
                    days = emptyList(),
                    onBack = { rootNavController.popBackStack() }
                )
            }
        }
    }
}

// ——————————————————————————————————————————————————————————————————————————————
//  Pantalla principal con bottom navigation
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/**
 * Contiene el [Scaffold] con la barra de navegación inferior y el
 * [NavHost] de pestañas. Se navega aquí después del Splash para
 * TODOS los usuarios (normales y observadores).
 */
@Composable
fun MainScreen(
    onNavigateToPartner: () -> Unit = {}
) {
    val navController = rememberNavController()
    
    val partnerViewModel: com.jeremy.lumi.ui.screens.partner.PartnerViewModel = hiltViewModel()
    val isObserver by remember(partnerViewModel) {
        partnerViewModel.uiState.map { it.isObserverOnly }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)

    val tabs = remember(isObserver) {
        if (isObserver) {
            listOf(
                BottomNavItem.Home,
                BottomNavItem.Chat,
                BottomNavItem.Settings
            )
        } else {
            listOf(
                BottomNavItem.Home,
                BottomNavItem.Calendar,
                BottomNavItem.Insights,
                BottomNavItem.Chat,
                BottomNavItem.Settings
            )
        }
    }
    
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    
    // Fix: Prevent crash when tabs size changes (e.g., normal to observer)
    LaunchedEffect(tabs.size) {
        if (pagerState.currentPage >= tabs.size && tabs.isNotEmpty()) {
            pagerState.scrollToPage(0)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute == "tabs"

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
            ) {
                AnimatedLumiBottomBar(
                    pagerState = pagerState,
                    tabs = tabs,
                    onTabSelected = { index ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "tabs",
            modifier = Modifier
                .padding(if (showBottomBar) paddingValues else PaddingValues(0.dp))
                .consumeWindowInsets(if (showBottomBar) paddingValues else PaddingValues(0.dp)),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
        ) {
            composable("tabs") {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    when (tabs[page]) {
                        BottomNavItem.Home -> HomeScreen(
                            onNavigateToInsights = {
                                val insightsIndex = tabs.indexOf(BottomNavItem.Insights)
                                if (insightsIndex >= 0) {
                                    coroutineScope.launch { pagerState.animateScrollToPage(insightsIndex) }
                                }
                            },
                            onNavigateToPartner = {
                                onNavigateToPartner()
                            }
                        )
                        BottomNavItem.Calendar -> CalendarScreen()
                        BottomNavItem.Insights -> InsightsScreen(
                            onNavigateBack = {
                                coroutineScope.launch { pagerState.animateScrollToPage(0) }
                            }
                        )
                        BottomNavItem.Chat -> ConversationsScreen(
                            onNavigateToChat = { thread ->
                                navController.navigate("chat/$thread")
                            }
                        )
                        BottomNavItem.Settings -> SettingsScreen()
                        else -> {}
                    }
                }
            }
            // Ruta partner eliminada y movida a AppNavGraph para arreglar Deep Links
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
        }
    }
}

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
//  Barra de navegación inferior
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
fun AnimatedLumiBottomBar(
    pagerState: PagerState,
    tabs: List<BottomNavItem>,
    onTabSelected: (Int) -> Unit,
    chatViewModel: com.jeremy.lumi.ui.screens.chat.ChatViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val unreadCount by chatViewModel.unreadCount.collectAsStateWithLifecycle()

    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth().drawBehind {
            drawLine(
                color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.15f),
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                strokeWidth = 1.dp.toPx()
            )
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding() // Respeta los gestos del sistema (pantalla completa)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, item ->
                val selected = pagerState.currentPage == index
                
                AnimatedBottomNavItem(
                    item = item,
                    selected = selected,
                    unreadCount = if (item == BottomNavItem.Chat) unreadCount else 0,
                    onClick = {
                        if (!selected) {
                            onTabSelected(index)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AnimatedBottomNavItem(
    item: BottomNavItem,
    selected: Boolean,
    unreadCount: Int,
    onClick: () -> Unit
) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    
    val brandGradient = LocalBrandGradient.current
    val isBrandActive = selected && brandGradient != null

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Desactivar ripple por defecto para que sea fluido
                onClick = onClick
            )
            .padding(horizontal = if (selected) 16.dp else 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Ícono con badge
            if (unreadCount > 0) {
                BadgedBox(
                    badge = { Badge(containerColor = MaterialTheme.colorScheme.error) { Text(unreadCount.toString()) } }
                ) {
                    if (isBrandActive) {
                        // Trick to render vector with gradient
                        Icon(
                            imageVector = item.icon,
                            contentDescription = stringResource(id = item.titleRes),
                            tint = Color.White,
                            modifier = Modifier.size(24.dp).graphicsLayer(alpha = 0.99f).drawWithCache {
                                onDrawWithContent {
                                    drawContent()
                                    drawRect(brandGradient!!, blendMode = androidx.compose.ui.graphics.BlendMode.SrcAtop)
                                }
                            }
                        )
                    } else {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = stringResource(id = item.titleRes),
                            tint = contentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                if (isBrandActive) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(id = item.titleRes),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp).graphicsLayer(alpha = 0.99f).drawWithCache {
                            onDrawWithContent {
                                drawContent()
                                drawRect(brandGradient!!, blendMode = androidx.compose.ui.graphics.BlendMode.SrcAtop)
                            }
                        }
                    )
                } else {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(id = item.titleRes),
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Texto animado
            AnimatedVisibility(
                visible = selected
            ) {
                if (isBrandActive) {
                    Text(
                        text = stringResource(id = item.titleRes),
                        style = TextStyle(
                            brush = brandGradient,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                } else {
                    Text(
                        text = stringResource(id = item.titleRes),
                        color = contentColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
