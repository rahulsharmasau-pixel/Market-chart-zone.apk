package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CandlestickChart
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.MarketAnalysisPost
import com.example.data.model.ReactionEmoji
import com.example.data.model.TradeDirection
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.VipTradePost
import com.example.ui.components.InitialsAvatar
import com.example.ui.components.NotificationBellButton
import com.example.ui.components.PostImageBanner
import com.example.ui.components.formatTimeAgo
import com.example.ui.theme.LocalMarketExtendedColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private data class ClockZoneOption(val label: String, val zoneId: String)

private val CLOCK_ZONES = listOf(
    ClockZoneOption("Local", TimeZone.getDefault().id),
    ClockZoneOption("UTC", "UTC"),
    ClockZoneOption("NY", "America/New_York"),
    ClockZoneOption("LDN", "Europe/London")
)

@Composable
fun HomeScreen(
    profile: UserProfile?,
    vipTradesState: UiState<List<VipTradePost>>,
    analysesState: UiState<List<MarketAnalysisPost>>,
    unreadAlertsCount: Int,
    hasNotifPermission: Boolean,
    hasBatteryExemption: Boolean,
    onRequestNotifSettings: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenNotifications: () -> Unit,
    onSeeAllTrades: () -> Unit,
    onOpenTrade: (VipTradePost) -> Unit,
    onSeeAllMarket: () -> Unit,
    onOpenAnalysis: (MarketAnalysisPost) -> Unit,
    onOpenCalculatorTab: (Int) -> Unit
) {
    val ext = LocalMarketExtendedColors.current
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedZoneIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(15_000L)
        }
    }

    val activeZone = CLOCK_ZONES[selectedZoneIndex]
    val tz = remember(activeZone.zoneId) { TimeZone.getTimeZone(activeZone.zoneId) }
    val greeting = remember(nowMillis) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    val dateTimeText = remember(nowMillis, tz) {
        val fmt = SimpleDateFormat("EEE, MMM d • hh:mm a", Locale.getDefault())
        fmt.timeZone = tz
        "${fmt.format(Date(nowMillis))} (${activeZone.label})"
    }

    val firstName = profile?.displayName?.trim()?.substringBefore(" ")?.ifBlank { "Trader" } ?: "Trader"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    InitialsAvatar(
                        initials = profile?.initials() ?: "MC",
                        onClick = onOpenAccount,
                        sizeDp = 48
                    )
                    Column {
                        Text(
                            text = "$greeting, $firstName",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("home_greeting_text")
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            onClick = { selectedZoneIndex = (selectedZoneIndex + 1) % CLOCK_ZONES.size },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, ext.cardBorder),
                            modifier = Modifier.testTag("timezone_clock_pill")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = "Cycle timezone",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = dateTimeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                NotificationBellButton(
                    unreadCount = unreadAlertsCount,
                    onClick = onOpenNotifications
                )
            }
        }

        // Delivery reliability prompts if needed
        if (!hasNotifPermission || !hasBatteryExemption) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!hasNotifPermission) {
                        ReliabilityPromptCard(
                            icon = Icons.Rounded.NotificationsOff,
                            title = "Push notifications disabled",
                            body = "Enable notifications in settings so you never miss live VIP trades when the app is closed.",
                            actionLabel = "Enable Alerts",
                            onAction = onRequestNotifSettings
                        )
                    }
                    if (!hasBatteryExemption) {
                        ReliabilityPromptCard(
                            icon = Icons.Rounded.BatteryAlert,
                            title = "Keep alerts instant in background",
                            body = "Disable battery optimization for Market Chart Zone so signals arrive even when swiped away.",
                            actionLabel = "Allow Unrestricted",
                            onAction = onRequestBatteryOptimization
                        )
                    }
                }
            }
        }

        // VIP Trades Section
        item {
            SectionHeader(
                title = "VIP trades",
                actionText = "See all",
                onAction = onSeeAllTrades,
                testTag = "see_all_vip_trades"
            )
            Spacer(modifier = Modifier.height(10.dp))

            when (vipTradesState) {
                is UiState.Loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }

                is UiState.Error -> ErrorInlineCard(vipTradesState.message)

                is UiState.Success -> {
                    val latestTrades = vipTradesState.data.take(3)
                    if (latestTrades.isEmpty()) {
                        EmptyInlineCard(
                            icon = Icons.Rounded.CandlestickChart,
                            message = "No VIP trades posted yet. New signals will appear here."
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            latestTrades.forEach { trade ->
                                HomeVipTradeCard(
                                    trade = trade,
                                    onClick = { onOpenTrade(trade) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Market Analysis Horizontal Section
        item {
            SectionHeader(
                title = "Market analysis",
                actionText = "See all",
                onAction = onSeeAllMarket,
                testTag = "see_all_market_analysis"
            )
            Spacer(modifier = Modifier.height(10.dp))

            when (analysesState) {
                is UiState.Loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }

                is UiState.Error -> ErrorInlineCard(analysesState.message)

                is UiState.Success -> {
                    val items = analysesState.data.take(6)
                    if (items.isEmpty()) {
                        EmptyInlineCard(
                            icon = Icons.Rounded.Insights,
                            message = "No market analyses published yet."
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(items, key = { it.id }) { analysis ->
                                HomeAnalysisPreviewCard(
                                    analysis = analysis,
                                    onClick = { onOpenAnalysis(analysis) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Calculators Section
        item {
            SectionHeader(
                title = "Calculators",
                actionText = null,
                onAction = null
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CalculatorShortcutCard(
                    title = "Position size",
                    subtitle = "Lot size from risk % & SL",
                    icon = Icons.Rounded.Calculate,
                    onClick = { onOpenCalculatorTab(0) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("shortcut_position_size")
                )
                CalculatorShortcutCard(
                    title = "Profit",
                    subtitle = "P&L from entry & exit",
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    onClick = { onOpenCalculatorTab(1) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("shortcut_profit_calc")
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String?,
    onAction: (() -> Unit)?,
    testTag: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (actionText != null && onAction != null) {
            TextButton(
                onClick = onAction,
                modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeVipTradeCard(
    trade: VipTradePost,
    onClick: () -> Unit
) {
    val ext = LocalMarketExtendedColors.current
    val dir = TradeDirection.fromRaw(trade.direction)
    val badgeColor = when (dir) {
        TradeDirection.LONG -> ext.bullish
        TradeDirection.SHORT -> ext.bearish
        TradeDirection.NEUTRAL -> MaterialTheme.colorScheme.secondary
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ext.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (trade.pair.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ext.elevatedCard,
                            border = BorderStroke(1.dp, ext.cardBorder)
                        ) {
                            Text(
                                text = trade.pair,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeColor.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = dir.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    text = formatTimeAgo(trade.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = trade.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniReactionCount(ReactionEmoji.all[0].second, trade.reactionFire)
                MiniReactionCount(ReactionEmoji.all[1].second, trade.reactionRocket)
                MiniReactionCount(ReactionEmoji.all[2].second, trade.reactionHundred)
                MiniReactionCount(ReactionEmoji.all[3].second, trade.reactionBull)
            }
        }
    }
}

@Composable
private fun MiniReactionCount(emoji: String, count: Long) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = emoji, style = MaterialTheme.typography.bodySmall)
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HomeAnalysisPreviewCard(
    analysis: MarketAnalysisPost,
    onClick: () -> Unit
) {
    val ext = LocalMarketExtendedColors.current
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ext.cardBorder),
        modifier = Modifier.width(264.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (!analysis.imageUrl.isNullOrBlank()) {
                PostImageBanner(
                    imageUrl = analysis.imageUrl,
                    contentDescription = analysis.title,
                    modifier = Modifier.height(118.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = "${analysis.pair} • ${analysis.timeframe}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = formatTimeAgo(analysis.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = analysis.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = analysis.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CalculatorShortcutCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMarketExtendedColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ext.cardBorder),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReliabilityPromptCard(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    val ext = LocalMarketExtendedColors.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ext.warning.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, ext.warning.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ext.warning,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ext.warning,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(text = actionLabel, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun EmptyInlineCard(icon: ImageVector, message: String) {
    val ext = LocalMarketExtendedColors.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ext.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorInlineCard(message: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp)
        )
    }
}
