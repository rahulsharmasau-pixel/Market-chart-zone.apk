package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.MarketRepository
import com.example.data.PreferencesManager
import com.example.data.model.NotificationCategory
import com.example.data.model.UiState
import com.example.notifications.NotificationHelper
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOutUser
import com.example.ui.components.BottomTab
import com.example.ui.components.MarketBottomNavBar
import com.example.ui.components.NotificationsSheet
import com.example.ui.screens.AccountScreen
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.CommunityScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MarketScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.screens.TradesScreen
import com.example.ui.theme.LocalMarketExtendedColors
import com.example.ui.theme.MarketChartZoneTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.MainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val ROUTE_ACCOUNT = "account"
private const val ROUTE_ADMIN = "admin"

data class DeepLinkTarget(
    val category: String,
    val targetId: String?
)

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {

    private val pendingDeepLink = mutableStateOf<DeepLinkTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.ensureNotificationChannel(this)
        extractDeepLink(intent)

        setContent {
            val context = LocalContext.current
            val prefs = remember { PreferencesManager(context.applicationContext) }
            val localDarkMode by prefs.darkModeFlow.collectAsStateWithLifecycle(initialValue = true)
            val currentUser by Firebase.auth.authStateFlow()
                .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                if (Firebase.auth.currentUser == null) {
                    val credentialManager = CredentialManager.create(context)
                    attemptAutoSignIn(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {},
                        onUnauthenticated = {},
                        scope = scope
                    )
                }
            }

            MarketChartZoneTheme(darkTheme = localDarkMode) {
                val user = currentUser
                if (user == null) {
                    AuthScreen(onAuthSuccess = {})
                } else {
                    key(user.uid) {
                        AuthenticatedMarketApp(
                            currentUserId = user.uid,
                            prefs = prefs,
                            localDarkMode = localDarkMode,
                            deepLink = pendingDeepLink.value,
                            onConsumeDeepLink = { pendingDeepLink.value = null }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractDeepLink(intent)
    }

    private fun extractDeepLink(intent: Intent?) {
        val category = intent?.getStringExtra(NotificationHelper.EXTRA_CATEGORY)
        if (!category.isNullOrBlank()) {
            val targetId = intent.getStringExtra(NotificationHelper.EXTRA_TARGET_ID)
                ?.takeIf { it.isNotBlank() }
            pendingDeepLink.value = DeepLinkTarget(category = category, targetId = targetId)
        }
    }
}

@Composable
private fun AuthenticatedMarketApp(
    currentUserId: String,
    prefs: PreferencesManager,
    localDarkMode: Boolean,
    deepLink: DeepLinkTarget?,
    onConsumeDeepLink: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val mainViewModel: MainViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                val repo = MarketRepository(db, Firebase.auth)
                MainViewModel(repo, PreferencesManager(app), currentUserId)
            }
        }
    )

    val profileState by mainViewModel.userProfile.collectAsStateWithLifecycle()
    val vipTradesState by mainViewModel.vipTrades.collectAsStateWithLifecycle()
    val analysesState by mainViewModel.marketAnalyses.collectAsStateWithLifecycle()
    val communityState by mainViewModel.communityMessages.collectAsStateWithLifecycle()
    val announcementsState by mainViewModel.announcements.collectAsStateWithLifecycle()
    val userReactions by mainViewModel.userReactions.collectAsStateWithLifecycle()
    val appContent by mainViewModel.appContent.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val effectiveDarkMode = profile?.darkMode ?: localDarkMode

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: BottomTab.HOME.route
    val snackbarHostState = remember { SnackbarHostState() }

    var showNotificationsSheet by remember { mutableStateOf(false) }
    var selectedTradeId by remember { mutableStateOf<String?>(null) }
    var selectedAnalysisId by remember { mutableStateOf<String?>(null) }
    var selectedCalculatorTab by remember { mutableIntStateOf(0) }

    // Notification & Battery prompts
    var hasNotifPerm by remember { mutableStateOf(NotificationHelper.hasNotificationPermission(context)) }
    var hasBatteryExemption by remember { mutableStateOf(NotificationHelper.isIgnoringBatteryOptimizations(context)) }
    var showNotifRationaleDialog by remember { mutableStateOf(false) }
    var showNotifDeniedSettingsDialog by remember { mutableStateOf(false) }
    var showBatteryDialog by remember { mutableStateOf(false) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPerm = granted
        if (!granted) {
            showNotifDeniedSettingsDialog = true
        } else if (!NotificationHelper.isIgnoringBatteryOptimizations(context)) {
            scope.launch {
                if (!prefs.batteryPromptDoneFlow.first()) {
                    showBatteryDialog = true
                }
            }
        }
    }

    // Collect snackbar events
    LaunchedEffect(Unit) {
        mainViewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Register FCM token and trigger first-launch notification rationale
    LaunchedEffect(currentUserId) {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            if (!token.isNullOrBlank()) {
                mainViewModel.registerFcmToken(token)
            }
        } catch (_: Exception) {
            // FCM token fetch may fail on environments without Play Services; handled gracefully
        }

        val notifDone = prefs.notificationPromptDoneFlow.first()
        if (!notifDone && !NotificationHelper.hasNotificationPermission(context)) {
            showNotifRationaleDialog = true
        } else if (!prefs.batteryPromptDoneFlow.first() && !NotificationHelper.isIgnoringBatteryOptimizations(context)) {
            showBatteryDialog = true
        }
    }

    // Deep-link routing from push notifications (cold start & warm tap)
    LaunchedEffect(deepLink) {
        val link = deepLink ?: return@LaunchedEffect
        when (link.category) {
            NotificationCategory.VIP_TRADES.key -> {
                selectedTradeId = link.targetId
                navController.navigate(BottomTab.TRADES.route) {
                    launchSingleTop = true
                }
            }
            NotificationCategory.MARKET_ANALYSIS.key -> {
                selectedAnalysisId = link.targetId
                navController.navigate(BottomTab.MARKET.route) {
                    launchSingleTop = true
                }
            }
            else -> {
                showNotificationsSheet = true
            }
        }
        onConsumeDeepLink()
    }

    val announcements = (announcementsState as? UiState.Success)?.data.orEmpty()
    val mutedCategories = profile?.mutedCategories.orEmpty()
    val unreadAlertsCount = announcements.count { !mutedCategories.contains(it.category) }
    val isBottomBarVisible = currentRoute in BottomTab.entries.map { it.route }

    MarketChartZoneTheme(darkTheme = effectiveDarkMode) {
        val ext = LocalMarketExtendedColors.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ext.backgroundBrush)
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (isBottomBarVisible) {
                        MarketBottomNavBar(
                            currentRoute = currentRoute,
                            onSelectTab = { tab ->
                                navController.navigate(tab.route) {
                                    popUpTo(BottomTab.HOME.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp)
                    .align(Alignment.Center)
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = BottomTab.HOME.route,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    composable(BottomTab.HOME.route) {
                        HomeScreen(
                            profile = profile,
                            vipTradesState = vipTradesState,
                            analysesState = analysesState,
                            unreadAlertsCount = unreadAlertsCount,
                            hasNotifPermission = hasNotifPerm,
                            hasBatteryExemption = hasBatteryExemption,
                            onRequestNotifSettings = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !NotificationHelper.hasNotificationPermission(context)
                                ) {
                                    showNotifRationaleDialog = true
                                } else {
                                    NotificationHelper.openAppNotificationSettings(context)
                                }
                            },
                            onRequestBatteryOptimization = {
                                NotificationHelper.requestDisableBatteryOptimization(context)
                                hasBatteryExemption = NotificationHelper.isIgnoringBatteryOptimizations(context)
                            },
                            onOpenAccount = { navController.navigate(ROUTE_ACCOUNT) },
                            onOpenNotifications = { showNotificationsSheet = true },
                            onSeeAllTrades = {
                                navController.navigate(BottomTab.TRADES.route) { launchSingleTop = true }
                            },
                            onOpenTrade = { trade ->
                                selectedTradeId = trade.id
                                navController.navigate(BottomTab.TRADES.route) { launchSingleTop = true }
                            },
                            onSeeAllMarket = {
                                navController.navigate(BottomTab.MARKET.route) { launchSingleTop = true }
                            },
                            onOpenAnalysis = { analysis ->
                                selectedAnalysisId = analysis.id
                                navController.navigate(BottomTab.MARKET.route) { launchSingleTop = true }
                            },
                            onOpenCalculatorTab = { tabIdx ->
                                selectedCalculatorTab = tabIdx
                                navController.navigate(BottomTab.TOOLS.route) { launchSingleTop = true }
                            }
                        )
                    }

                    composable(BottomTab.TRADES.route) {
                        TradesScreen(
                            vipTradesState = vipTradesState,
                            userReactions = userReactions,
                            unreadAlertsCount = unreadAlertsCount,
                            selectedTradeId = selectedTradeId,
                            onSelectTrade = { selectedTradeId = it?.id },
                            onToggleReaction = { id, emoji -> mainViewModel.toggleTradeReaction(id, emoji) },
                            onOpenNotifications = { showNotificationsSheet = true }
                        )
                    }

                    composable(BottomTab.MARKET.route) {
                        MarketScreen(
                            analysesState = analysesState,
                            userReactions = userReactions,
                            unreadAlertsCount = unreadAlertsCount,
                            selectedAnalysisId = selectedAnalysisId,
                            onSelectAnalysis = { selectedAnalysisId = it?.id },
                            onToggleReaction = { id, emoji -> mainViewModel.toggleAnalysisReaction(id, emoji) },
                            onOpenNotifications = { showNotificationsSheet = true }
                        )
                    }

                    composable(BottomTab.COMMUNITY.route) {
                        CommunityScreen(
                            profile = profile,
                            isCreator = mainViewModel.isCreator,
                            messagesState = communityState,
                            onAcceptGuidelines = { mainViewModel.acceptCommunityGuidelines() },
                            onSendMessage = { body, ticker, done ->
                                mainViewModel.sendCommunityMessage(body, ticker, done)
                            },
                            onDeleteMessage = { msgId -> mainViewModel.deleteCommunityMessage(msgId) }
                        )
                    }

                    composable(BottomTab.TOOLS.route) {
                        ToolsScreen(
                            selectedTabIndex = selectedCalculatorTab,
                            onSelectTab = { selectedCalculatorTab = it }
                        )
                    }

                    composable(ROUTE_ACCOUNT) {
                        AccountScreen(
                            profile = profile,
                            isDarkMode = effectiveDarkMode,
                            isCreator = mainViewModel.isCreator,
                            appContent = appContent,
                            onBack = { navController.popBackStack() },
                            onUpdateName = { mainViewModel.updateDisplayName(it) },
                            onToggleDarkMode = { mainViewModel.toggleDarkMode(it) },
                            onToggleMutedCategory = { cat ->
                                mainViewModel.toggleMutedCategory(cat, mutedCategories)
                            },
                            onOpenSystemNotifSettings = {
                                NotificationHelper.openAppNotificationSettings(context)
                            },
                            onOpenBatteryOptimization = {
                                NotificationHelper.requestDisableBatteryOptimization(context)
                            },
                            onSubmitIssueReport = { sub, desc, done ->
                                mainViewModel.submitIssueReport(sub, desc, done)
                            },
                            onOpenAdminPanel = {
                                if (mainViewModel.isCreator) {
                                    navController.navigate(ROUTE_ADMIN)
                                }
                            },
                            onSignOut = {
                                signOutUser(
                                    credentialManager = credentialManager,
                                    scope = scope,
                                    preSignOutCleanup = {
                                        val token = try {
                                            FirebaseMessaging.getInstance().token.await()
                                        } catch (_: Exception) { null }
                                        mainViewModel.removeFcmTokenBeforeSignOut(token)
                                    }
                                )
                            },
                            onDeleteAccount = {
                                mainViewModel.deleteOwnAccount(
                                    onDeleted = {
                                        signOutUser(credentialManager, scope)
                                    },
                                    onError = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            }
                        )
                    }

                    composable(ROUTE_ADMIN) {
                        if (mainViewModel.isCreator) {
                            val adminViewModel: AdminViewModel = viewModel(
                                key = "admin_$currentUserId",
                                factory = viewModelFactory {
                                    initializer {
                                        val app = checkNotNull(this[APPLICATION_KEY])
                                        val db = FirebaseFirestore.getInstance(
                                            app.getString(R.string.firestore_database_id)
                                        )
                                        AdminViewModel(MarketRepository(db, Firebase.auth))
                                    }
                                }
                            )
                            LaunchedEffect(adminViewModel) {
                                adminViewModel.toastEvents.collect { msg ->
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                            AdminPanelScreen(
                                adminViewModel = adminViewModel,
                                vipTradesState = vipTradesState,
                                analysesState = analysesState,
                                communityState = communityState,
                                announcementsState = announcementsState,
                                appContent = appContent,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }

        if (showNotificationsSheet) {
            NotificationsSheet(
                announcements = announcements,
                mutedCategories = mutedCategories,
                onDismiss = { showNotificationsSheet = false },
                onAnnouncementClick = { alert ->
                    showNotificationsSheet = false
                    when (alert.category) {
                        NotificationCategory.VIP_TRADES.key -> {
                            selectedTradeId = alert.targetId
                            navController.navigate(BottomTab.TRADES.route) { launchSingleTop = true }
                        }
                        NotificationCategory.MARKET_ANALYSIS.key -> {
                            selectedAnalysisId = alert.targetId
                            navController.navigate(BottomTab.MARKET.route) { launchSingleTop = true }
                        }
                    }
                }
            )
        }

        if (showNotifRationaleDialog) {
            AlertDialog(
                onDismissRequest = { showNotifRationaleDialog = false },
                title = { Text("Enable Signal Alerts") },
                text = { Text(stringResource(R.string.notification_permission_rationale)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showNotifRationaleDialog = false
                            scope.launch { prefs.markNotificationPromptDone() }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                NotificationHelper.openAppNotificationSettings(context)
                            }
                        }
                    ) { Text("Allow Notifications") }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showNotifRationaleDialog = false
                            scope.launch { prefs.markNotificationPromptDone() }
                        }
                    ) { Text("Not Now") }
                }
            )
        }

        if (showNotifDeniedSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showNotifDeniedSettingsDialog = false },
                title = { Text("Notifications Blocked") },
                text = {
                    Text("Push notifications are currently off. Open System Settings to allow alerts for VIP trades and market analysis.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showNotifDeniedSettingsDialog = false
                            NotificationHelper.openAppNotificationSettings(context)
                        }
                    ) { Text("Open Settings") }
                },
                dismissButton = {
                    TextButton(onClick = { showNotifDeniedSettingsDialog = false }) { Text("Dismiss") }
                }
            )
        }

        if (showBatteryDialog) {
            AlertDialog(
                onDismissRequest = { showBatteryDialog = false },
                title = { Text("Reliable Background Delivery") },
                text = { Text(stringResource(R.string.battery_optimization_rationale)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showBatteryDialog = false
                            scope.launch { prefs.markBatteryPromptDone() }
                            NotificationHelper.requestDisableBatteryOptimization(context)
                        }
                    ) { Text("Allow Unrestricted") }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showBatteryDialog = false
                            scope.launch { prefs.markBatteryPromptDone() }
                        }
                    ) { Text("Later") }
                }
            )
        }
    }
}
