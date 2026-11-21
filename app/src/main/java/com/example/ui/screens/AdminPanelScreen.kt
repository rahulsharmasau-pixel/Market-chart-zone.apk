package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Announcement
import com.example.data.model.AppContentDoc
import com.example.data.model.CommunityMessage
import com.example.data.model.IssueReport
import com.example.data.model.MarketAnalysisPost
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.VipTradePost
import com.example.ui.components.formatFeedTimestamp
import com.example.ui.components.formatTimeAgo
import com.example.ui.theme.LocalMarketExtendedColors
import com.example.ui.viewmodel.AdminGateState
import com.example.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.Locale

private val ADMIN_TABS = listOf(
    "Dashboard",
    "VIP Trades",
    "Analysis",
    "Push Alerts",
    "Users & Mod",
    "Reports",
    "Content"
)

@Composable
fun AdminPanelScreen(
    adminViewModel: AdminViewModel,
    vipTradesState: UiState<List<VipTradePost>>,
    analysesState: UiState<List<MarketAnalysisPost>>,
    communityState: UiState<List<CommunityMessage>>,
    announcementsState: UiState<List<Announcement>>,
    appContent: Map<String, AppContentDoc>,
    onBack: () -> Unit
) {
    val ext = LocalMarketExtendedColors.current
    val gateState by adminViewModel.gateState.collectAsStateWithLifecycle()
    val isBusy by adminViewModel.isBusy.collectAsStateWithLifecycle()
    val errorBanner by adminViewModel.errorBanner.collectAsStateWithLifecycle()
    val remainingSeconds by adminViewModel.remainingSessionSeconds.collectAsStateWithLifecycle()
    val usersState by adminViewModel.usersState.collectAsStateWithLifecycle()
    val reportsState by adminViewModel.reportsState.collectAsStateWithLifecycle()

    var showChangePassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ext.backgroundBrush)
    ) {
        // Header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Admin Panel",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (gateState == AdminGateState.Unlocked) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val mins = remainingSeconds / 60
                    val secs = remainingSeconds % 60
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%02d:%02d", mins, secs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    IconButton(onClick = { showChangePassword = true }) {
                        Icon(Icons.Rounded.Key, contentDescription = "Change admin password")
                    }
                    IconButton(
                        onClick = { adminViewModel.signOutAdmin() },
                        modifier = Modifier.testTag("admin_sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Sign out admin")
                    }
                }
            }
        }

        when (val gate = gateState) {
            is AdminGateState.Checking -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            is AdminGateState.AccessDenied -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                ErrorInlineCard(gate.reason)
            }

            is AdminGateState.NeedsOneTimeSetup -> OneTimeAdminSetupCard(
                isBusy = isBusy,
                errorBanner = errorBanner,
                onSubmit = { id, pw, confirm ->
                    adminViewModel.completeOneTimeSetup(id, pw, confirm)
                }
            )

            is AdminGateState.NeedsLogin -> AdminLoginCard(
                isBusy = isBusy,
                errorBanner = errorBanner,
                onSignIn = { id, pw ->
                    adminViewModel.signInAdmin(id, pw)
                }
            )

            is AdminGateState.Unlocked -> {
                var selectedTab by remember { mutableIntStateOf(0) }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    items(ADMIN_TABS.size) { index ->
                        FilterChip(
                            selected = selectedTab == index,
                            onClick = {
                                adminViewModel.touchActivity()
                                selectedTab = index
                            },
                            label = { Text(ADMIN_TABS[index]) },
                            modifier = Modifier.testTag("admin_tab_$index")
                        )
                    }
                }

                when (selectedTab) {
                    0 -> AdminDashboardSection(
                        usersState = usersState,
                        vipTradesState = vipTradesState,
                        analysesState = analysesState,
                        reportsState = reportsState,
                        announcementsState = announcementsState,
                        onSeedContent = { adminViewModel.seedStarterContent() }
                    )
                    1 -> AdminVipTradesSection(
                        vipTradesState = vipTradesState,
                        onSave = { id, title, body, pair, dir, entry, sl, tp, img, done ->
                            adminViewModel.saveVipTrade(id, title, body, pair, dir, entry, sl, tp, img, done)
                        },
                        onDelete = { adminViewModel.deleteVipTrade(it) }
                    )
                    2 -> AdminMarketAnalysisSection(
                        analysesState = analysesState,
                        onSave = { id, title, pair, tf, sum, sup, res, img, done ->
                            adminViewModel.saveMarketAnalysis(id, title, pair, tf, sum, sup, res, img, done)
                        },
                        onDelete = { adminViewModel.deleteMarketAnalysis(it) }
                    )
                    3 -> AdminPushSection(
                        isBusy = isBusy,
                        announcementsState = announcementsState,
                        onSendPush = { title, msg, img, done ->
                            adminViewModel.sendManualPush(title, msg, img, done)
                        }
                    )
                    4 -> AdminUsersModerationSection(
                        usersState = usersState,
                        communityState = communityState,
                        onToggleSuspend = { uid, current -> adminViewModel.toggleUserSuspension(uid, current) },
                        onDeleteUser = { adminViewModel.deleteUserByAdmin(it) },
                        onDeleteMessage = { adminViewModel.deleteCommunityMessage(it) }
                    )
                    5 -> AdminReportsSection(
                        reportsState = reportsState,
                        onResolve = { id, note -> adminViewModel.resolveReport(id, note) }
                    )
                    6 -> AdminLegalContentSection(
                        appContent = appContent,
                        onSaveContent = { key, title, body -> adminViewModel.saveLegalContent(key, title, body) }
                    )
                }
            }
        }
    }

    if (showChangePassword) {
        ChangeAdminPasswordDialog(
            onDismiss = { showChangePassword = false },
            onChangePassword = { cur, newPw, conf ->
                adminViewModel.changeAdminPassword(cur, newPw, conf) {
                    showChangePassword = false
                }
            }
        )
    }
}

@Composable
private fun OneTimeAdminSetupCard(
    isBusy: Boolean,
    errorBanner: String?,
    onSubmit: (String, String, String) -> Unit
) {
    val ext = LocalMarketExtendedColors.current
    var adminId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AdminPanelSettings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Create Admin (One-Time Setup)",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Text(
                    text = "Set your Admin User ID and Password once. After creation, this setup screen disappears permanently and nobody else can create another admin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (errorBanner != null) {
                    ErrorInlineCard(errorBanner)
                }
                OutlinedTextField(
                    value = adminId,
                    onValueChange = { adminId = it },
                    label = { Text("Admin User ID (min 3 chars)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_admin_id_input")
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Admin Password (min 8 chars)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_admin_pw_input")
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text("Confirm Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_admin_confirm_input")
                )
                Button(
                    onClick = { onSubmit(adminId, password, confirm) },
                    enabled = !isBusy && adminId.length >= 3 && password.length >= 8 && confirm.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("create_admin_submit_button")
                ) {
                    Text("Create Admin Permanently", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AdminLoginCard(
    isBusy: Boolean,
    errorBanner: String?,
    onSignIn: (String, String) -> Unit
) {
    val ext = LocalMarketExtendedColors.current
    var adminId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Admin Sign In", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    text = "Protected by salted PBKDF2 hashing, 5-attempt lockout, and 15-minute inactivity timeout.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (errorBanner != null) {
                    ErrorInlineCard(errorBanner)
                }
                OutlinedTextField(
                    value = adminId,
                    onValueChange = { adminId = it },
                    label = { Text("Admin User ID") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_login_id_input")
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Admin Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_login_pw_input")
                )
                Button(
                    onClick = { onSignIn(adminId, password) },
                    enabled = !isBusy && adminId.isNotBlank() && password.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("admin_login_submit_button")
                ) {
                    Text("Unlock Admin Panel", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AdminDashboardSection(
    usersState: UiState<List<UserProfile>>,
    vipTradesState: UiState<List<VipTradePost>>,
    analysesState: UiState<List<MarketAnalysisPost>>,
    reportsState: UiState<List<IssueReport>>,
    announcementsState: UiState<List<Announcement>>,
    onSeedContent: () -> Unit
) {
    val ext = LocalMarketExtendedColors.current
    val totalUsers = (usersState as? UiState.Success)?.data?.size ?: 0
    val totalTrades = (vipTradesState as? UiState.Success)?.data?.size ?: 0
    val totalAnalyses = (analysesState as? UiState.Success)?.data?.size ?: 0
    val openReports = (reportsState as? UiState.Success)?.data?.count { it.status == "open" } ?: 0
    val recentAlerts = (announcementsState as? UiState.Success)?.data?.take(6).orEmpty()

    LazyColumn(
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile("Total Users", totalUsers.toString(), Modifier.weight(1f))
                StatTile("Open Reports", openReports.toString(), Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile("VIP Trades", totalTrades.toString(), Modifier.weight(1f))
                StatTile("Analyses", totalAnalyses.toString(), Modifier.weight(1f))
            }
        }

        if (totalTrades == 0 || totalAnalyses == 0) {
            item {
                OutlinedButton(
                    onClick = onSeedContent,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Seed Sample VIP Trades & Chart Analyses")
                }
            }
        }

        item {
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (recentAlerts.isEmpty()) {
            item {
                EmptyInlineCard(Icons.Rounded.NotificationsActive, "No recent broadcast activity yet.")
            }
        } else {
            items(recentAlerts, key = { it.id }) { alert ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(alert.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = formatTimeAgo(alert.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = alert.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    val ext = LocalMarketExtendedColors.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ext.cardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun AdminVipTradesSection(
    vipTradesState: UiState<List<VipTradePost>>,
    onSave: (
        id: String?,
        title: String,
        body: String,
        pair: String,
        dir: com.example.data.model.TradeDirection,
        entry: String,
        sl: String,
        tp: String,
        img: String?,
        onSuccess: () -> Unit
    ) -> Unit,
    onDelete: (String) -> Unit
) {
    val ext = LocalMarketExtendedColors.current
    var showCreate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<VipTradePost?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = { showCreate = true },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp)
                .testTag("admin_new_vip_trade_btn")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Create VIP Trade (Text & Image)")
        }

        val itemsList = (vipTradesState as? UiState.Success)?.data.orEmpty()
        LazyColumn(
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(itemsList, key = { it.id }) { trade ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(trade.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${trade.pair} • ${trade.direction} • ${formatFeedTimestamp(trade.createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row {
                            IconButton(onClick = { editing = trade }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit trade")
                            }
                            IconButton(onClick = { onDelete(trade.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete trade", tint = ext.bearish)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate || editing != null) {
        VipTradeEditorDialog(
            initial = editing,
            onDismiss = {
                showCreate = false
                editing = null
            },
            onSave = { t, b, p, d, e, s, tp, img ->
                onSave(editing?.id, t, b, p, d, e, s, tp, img) {
                    showCreate = false
                    editing = null
                }
            }
        )
    }
}

@Composable
private fun AdminMarketAnalysisSection(
    analysesState: UiState<List<MarketAnalysisPost>>,
    onSave: (
        id: String?,
        title: String,
        pair: String,
        tf: String,
        summary: String,
        supCsv: String,
        resCsv: String,
        img: String?,
        onSuccess: () -> Unit
    ) -> Unit,
    onDelete: (String) -> Unit
) {
    val ext = LocalMarketExtendedColors.current
    var showCreate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MarketAnalysisPost?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = { showCreate = true },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp)
                .testTag("admin_new_analysis_btn")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Create Market Analysis (Text & Image)")
        }

        val itemsList = (analysesState as? UiState.Success)?.data.orEmpty()
        LazyColumn(
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(itemsList, key = { it.id }) { item ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${item.pair} (${item.timeframe}) • ${formatFeedTimestamp(item.createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row {
                            IconButton(onClick = { editing = item }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit analysis")
                            }
                            IconButton(onClick = { onDelete(item.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete analysis", tint = ext.bearish)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate || editing != null) {
        AnalysisEditorDialog(
            initial = editing,
            onDismiss = {
                showCreate = false
                editing = null
            },
            onSave = { title, pair, tf, sum, sup, res, img ->
                onSave(editing?.id, title, pair, tf, sum, sup, res, img) {
                    showCreate = false
                    editing = null
                }
            }
        )
    }
}

@Composable
private fun AdminPushSection(
    isBusy: Boolean,
    announcementsState: UiState<List<Announcement>>,
    onSendPush: (String, String, String?, () -> Unit) -> Unit
) {
    val ext = LocalMarketExtendedColors.current
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }

    LazyColumn(
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, ext.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Send Push Notification to All Users", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Notification title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Notification message") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("Optional image URL (no video)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onSendPush(title, message, imageUrl.ifBlank { null }) {
                                title = ""
                                message = ""
                                imageUrl = ""
                            }
                        },
                        enabled = !isBusy && title.isNotBlank() && message.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("admin_send_push_button")
                    ) {
                        Text("Broadcast Push Notification", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        val list = (announcementsState as? UiState.Success)?.data.orEmpty()
        items(list, key = { it.id }) { item ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ext.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = item.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminUsersModerationSection(
    usersState: UiState<List<UserProfile>>,
    communityState: UiState<List<CommunityMessage>>,
    onToggleSuspend: (String, Boolean) -> Unit,
    onDeleteUser: (String) -> Unit,
    onDeleteMessage: (String) -> Unit
) {
    val ext = LocalMarketExtendedColors.current
    var search by remember { mutableStateOf("") }
    var userToDelete by remember { mutableStateOf<UserProfile?>(null) }
    val users = (usersState as? UiState.Success)?.data.orEmpty().filter { u ->
        val q = search.trim()
        q.isEmpty() || u.displayName.contains(q, true) || u.email.contains(q, true)
    }
    val messages = (communityState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search users by name or email...") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_users_search_input")
            )
        }

        item {
            Text(
                text = "Registered Users (${users.size})",
                style = MaterialTheme.typography.titleMedium
            )
        }

        items(users, key = { it.userId }) { user ->
            val joinedDate = user.createdAt?.toDate()?.let {
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(it)
            } ?: "Recent"
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ext.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.displayName, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = user.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Joined $joinedDate" + if (user.suspended) " • SUSPENDED" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (user.suspended) ext.bearish else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onToggleSuspend(user.userId, user.suspended) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (user.suspended) "Unsuspend" else "Suspend")
                        }
                        Button(
                            onClick = { userToDelete = user },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ext.bearish.copy(alpha = 0.16f),
                                contentColor = ext.bearish
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Delete user account")
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Community Moderation (${messages.size} posts)",
                style = MaterialTheme.typography.titleMedium
            )
        }

        items(messages.take(25), key = { it.id }) { msg ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, ext.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(msg.authorName, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = msg.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onDeleteMessage(msg.id) }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove post", tint = ext.bearish)
                    }
                }
            }
        }
    }

    if (userToDelete != null) {
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Delete User Account?") },
            text = {
                Text("Remove ${userToDelete!!.displayName} (${userToDelete!!.email}) and sign them out of their data?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteUser(userToDelete!!.userId)
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete User") }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AdminReportsSection(
    reportsState: UiState<List<IssueReport>>,
    onResolve: (String, String) -> Unit
) {
    val ext = LocalMarketExtendedColors.current
    val reports = (reportsState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (reports.isEmpty()) {
            item {
                EmptyInlineCard(Icons.Rounded.CheckCircle, "No user issue reports submitted yet.")
            }
        } else {
            items(reports, key = { it.id }) { rep ->
                val isResolved = rep.status == "resolved"
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(rep.subject, style = MaterialTheme.typography.titleSmall)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isResolved) ext.bullish.copy(alpha = 0.16f) else ext.warning.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = rep.status.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isResolved) ext.bullish else ext.warning,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = "${rep.userName} (${rep.userEmail})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(rep.description, style = MaterialTheme.typography.bodySmall)
                        if (!isResolved) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { onResolve(rep.id, "Resolved by Admin") },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Mark Resolved")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminLegalContentSection(
    appContent: Map<String, AppContentDoc>,
    onSaveContent: (String, String, String) -> Unit
) {
    val ext = LocalMarketExtendedColors.current
    val keys = listOf("help_faqs", "terms", "privacy")
    var activeKey by remember { mutableStateOf("help_faqs") }
    val currentDoc = appContent[activeKey]
    var title by remember(activeKey, currentDoc) {
        mutableStateOf(currentDoc?.title?.ifBlank { defaultContentTitle(activeKey) } ?: defaultContentTitle(activeKey))
    }
    var body by remember(activeKey, currentDoc) {
        mutableStateOf(currentDoc?.body?.ifBlank { defaultContentBody(activeKey) } ?: defaultContentBody(activeKey))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            keys.forEach { k ->
                FilterChip(
                    selected = activeKey == k,
                    onClick = { activeKey = k },
                    label = { Text(defaultContentTitle(k)) }
                )
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, ext.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Section title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Content body") },
                    minLines = 8,
                    maxLines = 16,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onSaveContent(activeKey, title, body) },
                    enabled = title.isNotBlank() && body.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save ${defaultContentTitle(activeKey)}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
