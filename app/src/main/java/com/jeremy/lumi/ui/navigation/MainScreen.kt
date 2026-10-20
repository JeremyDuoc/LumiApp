package com.jeremy.lumi.ui.navigation

import com.jeremy.lumi.ui.theme.LumiMotion
import androidx.compose.ui.geometry.Rect
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.withFrameNanos
import com.jeremy.lumi.ui.screens.onboarding.LumiThemeTokensV7
import com.jeremy.lumi.ui.screens.onboarding.LocalLumiOnboardingTokens
import com.jeremy.lumi.ui.screens.onboarding.LumiAtmosphericTransition
import androidx.compose.animation.core.animateFloatAsState
import kotlin.math.abs
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.geometry.Offset
import com.jeremy.lumi.ui.components.captureThemeRevealSnapshot
import com.jeremy.lumi.ui.components.LumiThemeSnapshotReveal
import com.jeremy.lumi.ui.theme.LocalThemeRevealTrigger
import com.jeremy.lumi.ui.theme.ThemeRevealTrigger
import com.jeremy.lumi.ui.screens.settings.SettingsViewModel
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
import androidx.navigation.compose.*
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
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
private data class LumiHandoff(
    val sourceBounds: Rect?,
    val theme: LumiThemeTokensV7
)

@Composable
fun AppNavGraph() {
    val rootNavController = rememberNavController()
    var handoff by remember { mutableStateOf<LumiHandoff?>(null) }
    var mainNavigationStarted by remember { mutableStateOf(false) }
    var homeBrandBounds by remember { mutableStateOf<Rect?>(null) }
    var handoffFinishing by remember { mutableStateOf(false) }
    var homeContentReady by remember { mutableStateOf(false) }
    var homeExpectsBrand by remember { mutableStateOf(true) }
    var homeRevealAuthorized by remember { mutableStateOf(false) }
    var brandArrived by remember { mutableStateOf(false) }
    var skipHandoff by remember { mutableStateOf(false) }
    val handoffScope = rememberCoroutineScope()

    // La animación reside FUERA del NavHost, y sobrevive al cambio de ruta.
    fun navigateToRealHome() {
        if (mainNavigationStarted) return
        mainNavigationStarted = true
        rootNavController.navigate(AppRoutes.MAIN) {
            popUpTo(AppRoutes.ONBOARDING) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun finishHandoff() {
        if (handoffFinishing) return
        handoffFinishing = true
        navigateToRealHome()
        handoffScope.launch {
            withFrameNanos { } // Permitir componer MAIN antes de quitar el velo.
            withFrameNanos { }
            handoff = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    NavHost(
        navController = rootNavController,
        startDestination = AppRoutes.SPLASH,
        enterTransition = {
            if (initialState.destination.route == AppRoutes.ONBOARDING && targetState.destination.route == AppRoutes.MAIN) {
                // El velo atmosférico gestiona la transición visual.
                EnterTransition.None
            } else slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
        },
        exitTransition = {
            if (initialState.destination.route == AppRoutes.ONBOARDING && targetState.destination.route == AppRoutes.MAIN) {
                ExitTransition.None
            } else fadeOut(tween(300))
        },
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
                onComplete = { navigateToRealHome() },
                onCeremonyStart = { bounds, selectedTokens ->
                    if (handoff == null) {
                        homeBrandBounds = null
                        homeContentReady = false
                        homeExpectsBrand = true
                        homeRevealAuthorized = false
                        brandArrived = false
                        skipHandoff = false
                        mainNavigationStarted = false
                        handoffFinishing = false
                        handoff = LumiHandoff(bounds, selectedTokens)
                    }
                }
            )
        }

        // ——— Main (tabs + bottom nav) — para TODOS los usuarios —————————————————
        composable(route = AppRoutes.MAIN) {
            MainScreen(
                onHomeBrandBounds = { rect -> homeBrandBounds = rect },
                suppressHomeBrandDuringHandoff = handoff != null && !brandArrived,
                allowHomeEntrance = handoff == null || homeRevealAuthorized,
                onHomeReady = { expectsBrand ->
                    homeExpectsBrand = expectsBrand
                    homeContentReady = true
                },
                onNavigateToPartner = {
                    if (com.jeremy.lumi.BuildConfig.ENABLE_BONDS) {
                        rootNavController.navigate(AppRoutes.BONDS_HOME)
                    }
                },
                onNavigateToBackup = {
                    rootNavController.navigate(AppRoutes.BACKUP)
                },
                onOpenBondsPairing = {
                    rootNavController.navigate(AppRoutes.BONDS_PAIRING)
                },
                onOpenBondsSharedCalendar = {
                    rootNavController.navigate(AppRoutes.BONDS_SHARED_CALENDAR)
                },
                onOpenBondsMemberDetail = { linkId ->
                    rootNavController.navigate("bonds_member_detail/$linkId")
                },
                onOpenBondsSendGesture = { linkId, action ->
                    rootNavController.navigate("bonds_send_gesture/$action?linkId=$linkId")
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
        
        // Deshabilitar por completo las rutas/deep links de Bonds en release.
        if (com.jeremy.lumi.BuildConfig.ENABLE_BONDS) {
        // ——— Onboarding del Modo Pareja (redirección automática hacia Bonds si viene con código) ———
        composable(
            route = AppRoutes.PARTNER,
            deepLinks = listOf(
                navDeepLink { uriPattern = "https://lumi.app/legacy/pair/{code}" },
                navDeepLink { uriPattern = "lumi://legacy/pair/{code}" }
            )
        ) { backStackEntry ->
            val initialCode = backStackEntry.arguments?.getString("code")
            if (!initialCode.isNullOrBlank()) {
                LaunchedEffect(initialCode) {
                    rootNavController.navigate("${AppRoutes.BONDS_PAIRING}?joinCode=$initialCode") {
                        popUpTo(AppRoutes.PARTNER) { inclusive = true }
                    }
                }
            } else {
                com.jeremy.lumi.ui.screens.partner.PartnerOnboardingScreen(
                    initialCode = null,
                    onNavigateBack = { rootNavController.popBackStack() },
                    onFinished = {
                        rootNavController.navigate(AppRoutes.BONDS_HOME) {
                            popUpTo(AppRoutes.PARTNER) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
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
                onToggleDemoMode = { bondsViewModel.toggleDemoMode() },
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
                onToggleDemoMode = { bondsViewModel.toggleDemoMode() },
                onNavigateBack = { rootNavController.popBackStack() }
            )
        }

        composable(
            route = "${AppRoutes.BONDS_PAIRING}?joinCode={joinCode}",
            arguments = listOf(navArgument("joinCode") {
                nullable = true
                defaultValue = null
                type = NavType.StringType
            }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "https://lumi.app/pair/{joinCode}" },
                navDeepLink { uriPattern = "lumi://pair/{joinCode}" },
                navDeepLink { uriPattern = "https://lumi.app/bonds/pair/{joinCode}" },
                navDeepLink { uriPattern = "lumi://bonds/pair/{joinCode}" }
            )
        ) { backStackEntry ->
            val joinCode = backStackEntry.arguments?.getString("joinCode")
            val bondsViewModel: BondsViewModel = hiltViewModel()
            val uiState by bondsViewModel.uiState.collectAsStateWithLifecycle()

            BondsPairingPrivacyScreen(
                currentInviteCode = uiState.pendingInviteCode,
                initialJoinCode = joinCode,
                initialPrivacy = uiState.myDefaultPrivacy,
                isObserverOnly = uiState.isObserverOnly,
                onGenerateCode = { bondsViewModel.createInvitation() },
                onJoinWithCode = { code, privacy, onComplete ->
                    bondsViewModel.joinWithCode(code, privacy, onComplete)
                },
                onUpdatePrivacy = { privacy ->
                    bondsViewModel.updateDefaultPrivacy(privacy)
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
            val allMembers = if (uiState.members.isNotEmpty()) uiState.members else if (uiState.isDemoMode) BondsPreviewFixtures.sampleMembers else emptyList()
            val recipient = allMembers.find { it.linkId == linkIdParam } ?: uiState.selectedMember ?: allMembers.firstOrNull()

            BondsSendGestureScreen(
                initialAction = action,
                recipient = recipient,
                allMembers = allMembers,
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
            val members = if (uiState.members.isNotEmpty()) uiState.members else if (uiState.isDemoMode) BondsPreviewFixtures.sampleMembers else emptyList()

            BondsSharedCalendarScreen(
                members = members,
                onNavigateBack = { rootNavController.popBackStack() }
            )
        }

        composable(route = AppRoutes.BONDS_MEMBER_DETAIL) { backStackEntry ->
            val linkId = backStackEntry.arguments?.getString("linkId") ?: return@composable
            val bondsViewModel: BondsViewModel = hiltViewModel()
            val uiState by bondsViewModel.uiState.collectAsStateWithLifecycle()
            val member = uiState.members.find { it.linkId == linkId } ?: BondsPreviewFixtures.sampleMembers.find { it.linkId == linkId }

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
        } // ENABLE_BONDS: sin rutas de pareja en release
    }

    val activeHandoff = handoff
    if (activeHandoff != null) {
        BackHandler { skipHandoff = true }
        CompositionLocalProvider(LocalLumiOnboardingTokens provides activeHandoff.theme) {
            LumiAtmosphericTransition(
                sourceBrandBounds = activeHandoff.sourceBounds,
                targetBrandBounds = homeBrandBounds,
                revealUnderlyingHome = true,
                homeReady = homeContentReady,
                targetBrandRequired = homeExpectsBrand,
                forceSkip = skipHandoff,
                onRevealStart = { homeRevealAuthorized = true },
                onBrandArrived = { brandArrived = true },
                // At 460ms the curtain is opaque: mount exactly one real Home.
                onCurtainOpaque = { navigateToRealHome() },
                onFinished = { finishHandoff() }
            )
        }
    }
    } // Box que mantiene el overlay visible entre destinos
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
    onHomeBrandBounds: ((Rect) -> Unit)? = null,
    suppressHomeBrandDuringHandoff: Boolean = false,
    allowHomeEntrance: Boolean = true,
    onHomeReady: ((Boolean) -> Unit)? = null,
    onNavigateToPartner: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onOpenBondsPairing: () -> Unit = {},
    onOpenBondsSharedCalendar: () -> Unit = {},
    onOpenBondsMemberDetail: (String) -> Unit = {},
    onOpenBondsSendGesture: (String, String) -> Unit = { _, _ -> }
) {
    val navController = rememberNavController()
    
    val isObserver = if (com.jeremy.lumi.BuildConfig.ENABLE_BONDS) {
        val partnerViewModel: com.jeremy.lumi.ui.screens.partner.PartnerViewModel = hiltViewModel()
        val observerFlow = remember(partnerViewModel) {
            partnerViewModel.uiState.map { it.isObserverOnly }.distinctUntilChanged()
        }
        val observer by observerFlow.collectAsStateWithLifecycle(initialValue = false)
        observer
    } else false

    val tabs = remember(isObserver) {
        if (!com.jeremy.lumi.BuildConfig.ENABLE_BONDS) {
            listOf(
                BottomNavItem.Home,
                BottomNavItem.Calendar,
                BottomNavItem.Insights,
                BottomNavItem.Settings
            )
        } else if (isObserver) {
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
                BottomNavItem.Forum
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

    val activeAppTheme = com.jeremy.lumi.ui.theme.LocalActiveLumiTheme.current
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val sceneView = LocalView.current
    var themeOldFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    var themeRevealCenter by remember { mutableStateOf<Offset?>(null) }

    val themeRevealTrigger = remember(sceneView, settingsViewModel, activeAppTheme) {
        ThemeRevealTrigger { targetPalette, origin ->
            if (targetPalette != activeAppTheme) {
                themeOldFrame = captureThemeRevealSnapshot(sceneView)
                themeRevealCenter = origin
                settingsViewModel.changeTheme(targetPalette)
            }
        }
    }

    // Retorno suave a la pestaña Inicio si se presiona el botón Back del sistema
    BackHandler(enabled = showBottomBar && pagerState.currentPage != 0) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    CompositionLocalProvider(
        LocalThemeRevealTrigger provides themeRevealTrigger
    ) {
    Box(Modifier.fillMaxSize()) {
    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar && allowHomeEntrance,
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
                    Box(Modifier.fillMaxSize().graphicsLayer {
                        val distance = abs((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)
                        alpha = 1f - 0.15f * distance
                        scaleX = 1f - 0.015f * distance
                        scaleY = scaleX
                    }) {
                    when (tabs[page]) {
                        BottomNavItem.Home -> HomeScreen(
                            onBrandBounds = onHomeBrandBounds,
                            suppressBrandDuringHandoff = suppressHomeBrandDuringHandoff,
                            allowEntrance = allowHomeEntrance,
                            onHomeReady = onHomeReady,
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
                                } else if (com.jeremy.lumi.BuildConfig.ENABLE_BONDS) {
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
                                onOpenMemberDetail = { member -> onOpenBondsMemberDetail(member.linkId) },
                                onOpenSendGesture = { member, action -> onOpenBondsSendGesture(member?.linkId ?: "", action.name) },
                                onOpenPairingPrivacy = onOpenBondsPairing,
                                onOpenSharedCalendar = onOpenBondsSharedCalendar,
                                onToggleDemoMode = { bondsViewModel.toggleDemoMode() },
                                onNavigateBack = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(0) }
                                }
                            )
                        }
                        BottomNavItem.Forum -> ForumScreen()
                        BottomNavItem.Chat -> ConversationsScreen(
                            onNavigateBack = {
                                coroutineScope.launch { pagerState.animateScrollToPage(0) }
                            },
                            onNavigateToChat = { thread ->
                                navController.navigate("chat/$thread")
                            }
                        )
                        BottomNavItem.Settings -> SettingsScreen(
                            onNavigateBack = {
                                coroutineScope.launch { pagerState.animateScrollToPage(0) }
                            },
                            onNavigateToBackup = onNavigateToBackup,
                            onNavigateToPartner = onNavigateToPartner
                        )
                        else -> {}
                    }
                    }
                }
            }
            composable("settings") {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToBackup = onNavigateToBackup,
                    onNavigateToPartner = onNavigateToPartner
                )
            }
            composable("conversations") {
                ConversationsScreen(
                    onNavigateBack = { navController.popBackStack() },
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
    }    // Revelación circular snapshot idéntica a la del onboarding (690 ms, cubic-bezier(.2, .83, .2, 1))
    themeOldFrame?.let { old ->
        LumiThemeSnapshotReveal(
            oldFrame = old,
            sourceCenter = themeRevealCenter,
            onFinished = { themeOldFrame = null }
        )
    }
    } // Box de la navegación interna
    } // CompositionLocalProvider

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
        animationSpec = LumiMotion.SpatialColorSpring,
        label = "bottom_nav_color"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val iconScale = animateFloatAsState(if (selected) 1.12f else 1f, LumiMotion.SpatialSpring, label = "nav_icon")

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
            modifier = Modifier.size(24.dp).graphicsLayer {
                scaleX = iconScale.value
                scaleY = iconScale.value
            },
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
