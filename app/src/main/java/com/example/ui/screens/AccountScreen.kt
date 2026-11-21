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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppContentDoc
import com.example.data.model.NotificationCategory
import com.example.data.model.UserProfile
import com.example.ui.components.InitialsAvatar
import com.example.ui.theme.LocalMarketExtendedColors

@Composable
fun AccountScreen(
    profile: UserProfile?,
    isDarkMode: Boolean,
    isCreator: Boolean,
    appContent: Map<String, AppContentDoc>,
    onBack: () -> Unit,
    onUpdateName: (String) -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onToggleMutedCategory: (String) -> Unit,
    onOpenSystemNotifSettings: () -> Unit,
    onOpenBatteryOptimization: () -> Unit,
    onSubmitIssueReport: (String, String, () -> Unit) -> Unit,
    onOpenAdminPanel: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val ext = LocalMarketExtendedColors.current
    var showEditProfile by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var activeLegalDocKey by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ext.backgroundBrush)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("account_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Account & Settings",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Header Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            InitialsAvatar(
                                initials = profile?.initials() ?: "MC",
                                sizeDp = 56
                            )
                            Column {
                                Text(
                                    text = profile?.displayName?.ifBlank { "Trader" } ?: "Trader",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = profile?.email ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { showEditProfile = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit profile")
                        }
                    }
                }
            }

            // Appearance & Notifications
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Appearance & Alerts",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DarkMode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text("Dark theme", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = if (isDarkMode) "Deep navy & teal glow" else "Clean institutional light",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = onToggleDarkMode,
                                modifier = Modifier.testTag("appearance_theme_switch")
                            )
                        }

                        HorizontalDivider(
                            color = ext.cardBorder,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        Text(
                            text = "Push Notification Categories",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val muted = profile?.mutedCategories.orEmpty()
                        NotificationCategory.entries.forEach { cat ->
                            val enabled = !muted.contains(cat.key)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cat.label, style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = enabled,
                                    onCheckedChange = { onToggleMutedCategory(cat.key) },
                                    modifier = Modifier.testTag("mute_toggle_${cat.key}")
                                )
                            }
                        }

                        HorizontalDivider(
                            color = ext.cardBorder,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )

                        SettingsActionRow(
                            icon = Icons.Rounded.NotificationsActive,
                            title = "System Notification Settings",
                            subtitle = "Ensure heads-up, sound & lockscreen alerts are allowed",
                            onClick = onOpenSystemNotifSettings
                        )
                        SettingsActionRow(
                            icon = Icons.Rounded.BatteryChargingFull,
                            title = "Unrestricted Background Delivery",
                            subtitle = "Disable battery optimization so closed-app pushes arrive",
                            onClick = onOpenBatteryOptimization
                        )
                    }
                }
            }

            // Support & Legal
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Support & Legal",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsActionRow(
                            icon = Icons.AutoMirrored.Rounded.HelpOutline,
                            title = "Help and FAQs",
                            onClick = { activeLegalDocKey = "help_faqs" },
                            testTag = "help_faqs_row"
                        )
                        SettingsActionRow(
                            icon = Icons.Rounded.BugReport,
                            title = "Report an issue",
                            onClick = { showReportDialog = true },
                            testTag = "report_issue_row"
                        )
                        SettingsActionRow(
                            icon = Icons.Rounded.Description,
                            title = "Terms and conditions",
                            onClick = { activeLegalDocKey = "terms" },
                            testTag = "terms_row"
                        )
                        SettingsActionRow(
                            icon = Icons.Rounded.Policy,
                            title = "Privacy policy",
                            onClick = { activeLegalDocKey = "privacy" },
                            testTag = "privacy_row"
                        )
                    }
                }
            }

            // Creator-Only Admin Panel Entry
            if (isCreator) {
                item {
                    Card(
                        onClick = onOpenAdminPanel,
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_panel_entry_row")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "Admin Panel",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Manage signals, analyses, users, and alerts",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Sign Out & Delete Account
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign out", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ext.bearish.copy(alpha = 0.16f),
                            contentColor = ext.bearish
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("delete_account_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete account", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // App Version Footer
            item {
                Text(
                    text = "Market Chart Zone • ${stringResource(R.string.app_version_label)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            }
        }
    }

    if (showEditProfile) {
        var nameInput by remember { mutableStateOf(profile?.displayName ?: "") }
        AlertDialog(
            onDismissRequest = { showEditProfile = false },
            title = { Text("Edit Profile") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Display name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateName(nameInput)
                        showEditProfile = false
                    },
                    enabled = nameInput.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfile = false }) { Text("Cancel") }
            }
        )
    }

    if (showReportDialog) {
        var subject by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Report an Issue") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Describe the issue") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubmitIssueReport(subject, description) {
                            showReportDialog = false
                        }
                    },
                    enabled = subject.isNotBlank() && description.isNotBlank()
                ) { Text("Submit") }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account Permanently?") },
            text = {
                Text(
                    "This removes your Market Chart Zone profile, saved reactions, FCM notification tokens, and authentication credentials. This cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete Permanently") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (activeLegalDocKey != null) {
        val doc = appContent[activeLegalDocKey]
        val title = doc?.title?.ifBlank { defaultContentTitle(activeLegalDocKey!!) }
            ?: defaultContentTitle(activeLegalDocKey!!)
        val body = doc?.body?.ifBlank { defaultContentBody(activeLegalDocKey!!) }
            ?: defaultContentBody(activeLegalDocKey!!)
        AlertDialog(
            onDismissRequest = { activeLegalDocKey = null },
            title = { Text(title) },
            text = {
                Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(text = body, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = { activeLegalDocKey = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary,
    testTag: String? = null
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint)
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun defaultContentTitle(key: String): String = when (key) {
    "help_faqs" -> "Help & FAQs"
    "terms" -> "Terms & Conditions"
    "privacy" -> "Privacy Policy"
    else -> "Information"
}

fun defaultContentBody(key: String): String = when (key) {
    "help_faqs" -> """
        1. How do VIP Trades work?
        Each VIP signal includes the currency pair, direction (Long/Short), Entry, Stop Loss, and Take Profit levels. Tap any card to inspect full notes and chart visuals.

        2. Why aren't push notifications appearing immediately?
        Ensure notifications are enabled in Account > System Notification Settings and allow Unrestricted battery usage so Android delivers high-priority FCM messages when the app is closed.

        3. How do the Trading Calculators work?
        Use the Position Size calculator to find exact lot sizes from your account balance, risk percentage, and stop loss in pips. Use the Profit calculator to estimate P&L before entering a trade.
    """.trimIndent()
    "terms" -> """
        Market Chart Zone provides educational market chart analysis, trade ideas, and calculation tools. Nothing published in Market Chart Zone constitutes personalized financial or investment advice. Trading leveraged forex, commodities, and digital assets carries significant risk. You are solely responsible for your trading decisions and risk management.
    """.trimIndent()
    "privacy" -> """
        Market Chart Zone stores only the account details required to authenticate your session (name, email, user preferences, and device FCM tokens for push alerts). Device notification tokens are automatically cleared when you sign out or delete your account. You may permanently delete your account and associated records at any time from Account Settings.
    """.trimIndent()
    else -> ""
}
