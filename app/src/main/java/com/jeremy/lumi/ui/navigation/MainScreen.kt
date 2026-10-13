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
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.jeremy.lumi.ui.theme.LumiThemeTokens
import com.jeremy.lumi.ui.theme.LocalBrandGradient
import com.jeremy.lumi.ui.screens.home.HomeScreen
import com.jeremy.lumi.ui.screens.settings.SettingsScreen
import com.jeremy.lumi.ui.screens.calendar.CalendarScreen
import com.jeremy.lumi.ui.screens.forum.ForumScreen
import com.jeremy.lumi.ui.screens.chat.ChatScreen
import com.jeremy.lumi.ui.screens.chat.ConversationsScreen
import com.jeremy.lumi.ui.screens.insights.InsightsScreen
import com.jeremy.lumi.ui.screens.onboarding.OnboardingScreen
import com.jeremy.lumi.ui.screens.splash.SplashScreen
import com.jeremy.lumi.ui.screens.bonds.*
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
                    rootNavController.navigate(AppRoutes.BONDS_HOME)
                },
                onNavigateToBackup = {
                    rootNavController.navigate(AppRoutes.BACKUP)
                }
            )
        }

        // ——— Insights / Estadísticas —————————————————————————————————————————
        composable(route = AppRoutes.INSIGHTS) {
            InsightsScreen(
                onNavigateBack = { rootNavController.popBackStack() }
            )
        }

        // ——— Respaldo privado en Google Drive ————————————————————————————————
        composable(route = AppRoutes.BACKUP) {
            com.jeremy.lumi.ui.screens.settings.backup.BackupSettingsScreen(
                onNavigateBack = { rootNavController.popBackStack() }
            )
        }
        
        // ——— Onboarding del Modo Pareja (crear vínculo o unirse; deep links) ———
        composable(
            route = AppRoutes.PARTNER,
            deepLinks = listOf(
                androidx.navigation.navDeepLink { uriPattern = "https://lumi.app/pair/{code}" },
                androidx.navigation.navDeepLink { uriPattern = "lumi://pair/{code}" }
            )
        ) { backStackEntry ->
            val initialCode = backStackEntry.arguments?.getString("code")
            com.jeremy.lumi.ui.screens.partner.PartnerOnboardingScreen(
                initialCode = initialCode?.takeIf { it.isNotBlank() },
                onNavigateBack = { rootNavController.popBackStack() },
                onFinished = {
                    rootNavController.navigate(AppRoutes.PARTNER_HUB) {
                        popUpTo(AppRoutes.PARTNER) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        // ——— Diario Compartido —————————————————————————————————————————————————
        composable("shared_diary/{linkId}") { backStack ->
            val linkId = backStack.arguments?.getString("linkId") ?: return@composable
            val viewModel: com.jeremy.lumi.ui.screens.partner.PartnerViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            
            // Inyectamos HomeViewModel para obtener la fase real del usuario
            val homeViewModel: com.jeremy.lumi.ui.screens.home.HomeViewModel = hiltViewModel()
            val homeState by homeViewModel.uiState.collectAsStateWithLifecycle()
            val realPhase = homeState.currentPhase

            LaunchedEffect(linkId) { viewModel.observeDiary(linkId) }

            com.jeremy.lumi.ui.screens.partner.SharedDiaryScreen(
                currentUid = uiState.currentUid ?: "",
                myPhase = realPhase,
                entries = uiState.diaryEntries,
                isSending = uiState.isDiarySending,
                onSend = { text -> viewModel.sendDiaryEntry(linkId, text, realPhase) },
                onBack = { rootNavController.popBackStack() }
            )
        }

        // ── BONDS MODULE ROUTES ──────────────────────────────────────────────
        composable(route = AppRoutes.BONDS_HOME) {
            val bondsViewModel: BondsViewModel = hiltViewModel()
            val uiState by bondsViewModel.uiState.collectAsStateWithLifecycle()

            BondsHomeScreen(
                uiState = uiState,
                onSelectMember = { member -> bondsViewModel.selectMember(member) },
                onOpenMemberDetail = { member ->
                    rootNavController.navigate("bonds_member_detail/${member.linkId}")
                },
                onOpenSendGesture = { member, action ->
                    val linkId = member?.linkId ?: ""
                    rootNavController.navigate("bonds_send_gesture/${action.name}?linkId=$linkId")
                },
                onOpenPairingPrivacy = { rootNavController.navigate(AppRoutes.BONDS_PAIRING) },
                onOpenSharedCalendar = { rootNavController.navigate(AppRoutes.BONDS_SHARED_CALENDAR) },
                onNavigateBack = { rootNavController.popBackStack() }
            )
        }

        // Alias retrocompatible para PARTNER_HUB que renderiza BondsHomeScreen
        composable(route = AppRoutes.PARTNER_HUB) {
            val bondsViewModel: BondsViewModel = hiltViewModel()
            val uiState by bondsViewModel.uiState.collectAsStateWithLifecycle()

            BondsHomeScreen(
                uiState = uiState,
                onSelectMember = { member -> bondsViewModel.selectMember(member) },
                onOpenMemberDetail = { member ->
                    rootNavController.navigate("bonds_member_detail/${member.linkId}")
                },
                onOpenSendGesture = { member, action ->
                    val linkId = member?.linkId ?: ""
                    rootNavController.navigate("bonds_send_gesture/${action.name}?linkId=$linkId")
                },
                onOpenPairingPrivacy = { rootNavController.navigate(AppRoutes.BONDS_PAIRING) },
                onOpenSharedCalendar = { rootNavController.navigate(AppRoutes.BONDS_SHARED_CALENDAR) },
                onNavigateBack = { rootNavController.popBackStack() }
            )
        }

        composable(
            route = AppRoutes.BONDS_PAIRING,
            deepLinks = listOf(
                androidx.navigation.navDeepLink { uriPattern = "https://lumi.app/bonds/pair/{code}" },
                androidx.navigation.navDeepLink { uriPattern = "lumi://bonds/pair/{code}" }
            )
        ) { backStackEntry ->
            val initialCode = backStackEntry.arguments?.getString("code")
            val bondsViewModel: BondsViewModel = hiltViewModel()
            val uiState by bondsViewModel.uiState.collectAsStateWithLifecycle()

            BondsPairingPrivacyScreen(
                currentInviteCode = initialCode ?: uiState.pendingInviteCode,
                initialPrivacy = uiState.myDefaultPrivacy,
                isObserverOnly = uiState.isObserverOnly,
                onGenerateCode = { bondsViewModel.createInvitation() },
                onJoinWithCode = { code, privacy, onComplete ->
                    bondsViewModel.joinWithCode(code, privacy, onComplete)
                },
                onUpdatePrivacy = { privacy ->
                    uiState.selectedMember?.let {
                        bondsViewModel.updateLinkPrivacy(it.linkId, privacy)
                    }
                },
                onNavigateBack = { rootNavController.popBackStack() }
            )
        }

        composable(route = AppRoutes.BONDS_SEND_GESTURE) { backStackEntry ->
            val actionName = backStackEntry.arguments?.getString("action")
            val linkIdParam = backStackEntry.arguments?.getString("linkId")
            val action = actionName?.let {
                runCatching { com.jeremy.lumi.domain.model.CareAction.valueOf(it) }.getOrNull()
            } ?: com.jeremy.lumi.domain.model.CareAction.TEA

            val bondsViewModel: BondsViewModel = hiltViewModel()
            val uiState by bondsViewModel.uiState.collectAsStateWithLifecycle()
            val recipient = uiState.members.find { it.linkId == linkIdParam } ?: uiState.selectedMember

            BondsSendGestureScreen(
                initialAction = action,
                recipient = recipient,
                allMembers = uiState.members,
                isSending = uiState.isSendingGesture,
                onSend = { linkId, careAction, message, onComplete ->
                    bondsViewModel.sendGesture(linkId, careAction, message, onComplete)
                },
                onNavigateBack = { rootNavController.popBackStack() }
            )
        }

        composable(route = AppRoutes.BONDS_SHARED_CALENDAR) {
            val bondsViewModel: BondsViewModel = hiltViewModel()
            val uiState by bondsViewModel.uiState.collectAsStateWithLifecycle()

            BondsSharedCalendarScreen(
                members = uiState.members,
                onNavigateBack = { rootNavController.popBackStack() }
            )
        }

        composable(route = AppRoutes.BONDS_MEMBER_DETAIL) { backStackEntry ->
            val linkId = backStackEntry.arguments?.getString("linkId") ?: return@composable
            val bondsViewModel: BondsViewModel = hiltViewModel()
            val uiState by bondsViewModel.uiState.collectAsStateWithLifecycle()
            val member = uiState.members.find { it.linkId == linkId }

            if (member != null) {
                BondsPersonDetailScreen(
                    member = member,
                    onSendGesture = { action ->
                        rootNavController.navigate("bonds_send_gesture/${action.name}?linkId=${member.linkId}")
                    },
                    onOpenCalendar = { rootNavController.navigate(AppRoutes.BONDS_SHARED_CALENDAR) },
                    onUpdatePrivacy = { privacy ->
                        bondsViewModel.updateLinkPrivacy(member.linkId, privacy)
                    },
                    onUnlink = {
                        bondsViewModel.unlinkMember(member.linkId) {
                            rootNavController.popBackStack()
                        }
                    },
                    onNavigateBack = { rootNavController.popBackStack() }
                )
            }
        }

        // ——— Historia del vínculo ——————————————————————————————————————————————
        composable("story_detail/{linkId}") { backStack ->
            val linkId = backStack.arguments?.getString("linkId") ?: return@composable
            val viewModel: com.jeremy.lumi.ui.screens.partner.PartnerViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val link = uiState.activeLinks.find { it.linkId == linkId }

            if (link != null) {
                com.jeremy.lumi.ui.screens.partner.PartnerStoryScreen(
                    link = link,
                    myUid = uiState.currentUid ?: "",
                    uiState = uiState,
                    onSendCareAction = { action -> viewModel.sendCareAction(link.linkId, action) },
                    onClose = { rootNavController.popBackStack() }
                )
            }
        }

        // ——— Calendario Compartido —————————————————————————————————————————————
        composable("dual_calendar/{linkId}") { backStack ->
            val linkId = backStack.arguments?.getString("linkId") ?: return@composable
            com.jeremy.lumi.ui.screens.partner.DualCalendarScreen(
                linkId = linkId,
                onBack = { rootNavController.popBackStack() }
            )
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
    onNavigateToPartner: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {}
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
                BottomNavItem.Bonds,
                BottomNavItem.Chat,
                BottomNavItem.Settings
            )
        } else {
            listOf(
                BottomNavItem.Home,
                BottomNavItem.Calendar,
                BottomNavItem.Insights,
                BottomNavItem.Bonds
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
                            onNavigateToCalendar = {
                                val calIndex = tabs.indexOf(BottomNavItem.Calendar)
                                if (calIndex >= 0) {
                                    coroutineScope.launch { pagerState.animateScrollToPage(calIndex) }
                                }
                            },
                            onNavigateToPartner = {
                                val bondsIndex = tabs.indexOf(BottomNavItem.Bonds)
                                if (bondsIndex >= 0) {
                                    coroutineScope.launch { pagerState.animateScrollToPage(bondsIndex) }
                                } else {
                                    onNavigateToPartner()
                                }
                            },
                            onNavigateToSettings = {
                                navController.navigate("settings")
                            },
                            onNavigateToChat = {
                                navController.navigate("conversations")
                            }
                        )
                        BottomNavItem.Calendar -> CalendarScreen()
                        BottomNavItem.Insights -> InsightsScreen(
                            onNavigateBack = {
                                coroutineScope.launch { pagerState.animateScrollToPage(0) }
                            }
                        )
                        BottomNavItem.Bonds -> {
                            val bondsViewModel: BondsViewModel = hiltViewModel()
                            val bondsUiState by bondsViewModel.uiState.collectAsStateWithLifecycle()
                            BondsHomeScreen(
                                uiState = bondsUiState,
                                onSelectMember = { member -> bondsViewModel.selectMember(member) },
                                onOpenMemberDetail = { onNavigateToPartner() },
                                onOpenSendGesture = { _, _ -> onNavigateToPartner() },
                                onOpenPairingPrivacy = { onNavigateToPartner() },
                                onOpenSharedCalendar = { onNavigateToPartner() },
                                onNavigateBack = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(0) }
                                }
                            )
                        }
                        BottomNavItem.Chat -> ConversationsScreen(
                            onNavigateToChat = { thread ->
                                navController.navigate("chat/$thread")
                            }
                        )
                        BottomNavItem.Settings -> SettingsScreen(
                            onNavigateToBackup = onNavigateToBackup,
                            onNavigateToPartner = onNavigateToPartner
                        )
                        else -> {}
                    }
                }
            }
            composable("settings") {
                SettingsScreen(
                    onNavigateToBackup = onNavigateToBackup,
                    onNavigateToPartner = onNavigateToPartner
                )
            }
            composable("conversations") {
                ConversationsScreen(
                    onNavigateToChat = { thread ->
                        navController.navigate("chat/$thread")
                    }
                )
            }
            // Ruta partner eliminada y movida a AppNavGraph para arperiodor Deep Links
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
    val tokens = LumiThemeTokens.current
    val unreadCount by chatViewModel.unreadCount.collectAsStateWithLifecycle()

    Surface(
        color = tokens.nav,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = tokens.line,
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
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
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
                    },
                    modifier = Modifier.weight(1f)
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LumiThemeTokens.current
    val targetColor = if (selected) tokens.accent else tokens.dim
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 200),
        label = "bottom_nav_color"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 5.dp)
    ) {
        // Ícono con badge
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (unreadCount > 0) {
                BadgedBox(
                    badge = {
                        Badge(containerColor = tokens.rose) {
                            Text(unreadCount.toString(), color = Color.White, fontSize = 9.sp)
                        }
                    }
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(id = item.titleRes),
                        tint = animatedColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = item.icon,
                    contentDescription = stringResource(id = item.titleRes),
                    tint = animatedColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Etiqueta de texto debajo del ícono
        Text(
            text = stringResource(id = item.titleRes),
            color = animatedColor,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            letterSpacing = (-0.2).sp
        )
    }
}
