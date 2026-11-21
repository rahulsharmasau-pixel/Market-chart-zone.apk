package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CandlestickChart
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TradeDirection
import com.example.data.model.UiState
import com.example.data.model.VipTradePost
import com.example.ui.components.NotificationBellButton
import com.example.ui.components.PostImageBanner
import com.example.ui.components.ReactionBar
import com.example.ui.components.formatFeedTimestamp
import com.example.ui.theme.LocalMarketExtendedColors

@Composable
fun TradesScreen(
    vipTradesState: UiState<List<VipTradePost>>,
    userReactions: Map<String, List<String>>,
    unreadAlertsCount: Int,
    selectedTradeId: String?,
    onSelectTrade: (VipTradePost?) -> Unit,
    onToggleReaction: (String, String) -> Unit,
    onOpenNotifications: () -> Unit
) {
    val ext = LocalMarketExtendedColors.current
    var showSearch by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "VIP Trades",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Curated high-conviction trade signals",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = {
                        showSearch = !showSearch
                        if (!showSearch) query = ""
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("trades_search_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (showSearch) Icons.Rounded.Close else Icons.Rounded.Search,
                            contentDescription = "Search VIP trades",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                NotificationBellButton(
                    unreadCount = unreadAlertsCount,
                    onClick = onOpenNotifications
                )
            }
        }

        AnimatedVisibility(visible = showSearch) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search pair, direction, or title...") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .testTag("trades_search_input")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (vipTradesState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            is UiState.Error -> ErrorInlineCard(vipTradesState.message)
            is UiState.Success -> {
                val filtered = vipTradesState.data.filter { trade ->
                    val q = query.trim()
                    q.isEmpty() ||
                        trade.title.contains(q, ignoreCase = true) ||
                        trade.pair.contains(q, ignoreCase = true) ||
                        trade.body.contains(q, ignoreCase = true) ||
                        trade.direction.contains(q, ignoreCase = true)
                }
                if (filtered.isEmpty()) {
                    EmptyInlineCard(
                        icon = Icons.Rounded.CandlestickChart,
                        message = if (query.isBlank()) "No VIP trades posted yet." else "No signals matched \"$query\"."
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filtered, key = { it.id }) { trade ->
                            VipTradeCard(
                                trade = trade,
                                selectedEmojis = userReactions[trade.id].orEmpty(),
                                onClick = { onSelectTrade(trade) },
                                onToggleEmoji = { emoji -> onToggleReaction(trade.id, emoji) }
                            )
                        }
                    }
                }

                val activeDetail = selectedTradeId?.let { id ->
                    vipTradesState.data.firstOrNull { it.id == id }
                }
                if (activeDetail != null) {
                    TradeDetailSheet(
                        trade = activeDetail,
                        selectedEmojis = userReactions[activeDetail.id].orEmpty(),
                        onToggleEmoji = { emoji -> onToggleReaction(activeDetail.id, emoji) },
                        onDismiss = { onSelectTrade(null) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VipTradeCard(
    trade: VipTradePost,
    selectedEmojis: List<String>,
    onClick: () -> Unit,
    onToggleEmoji: (String) -> Unit
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
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ext.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vip_trade_card_${trade.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (trade.pair.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ext.elevatedCard,
                            border = BorderStroke(1.dp, ext.cardBorder)
                        ) {
                            Text(
                                text = trade.pair,
                                style = MaterialTheme.typography.labelSmall,
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
                    text = formatFeedTimestamp(trade.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = trade.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = trade.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (trade.entryPrice.isNotBlank() || trade.stopLoss.isNotBlank() || trade.takeProfit.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PriceLevelChip("Entry", trade.entryPrice, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    PriceLevelChip("SL", trade.stopLoss, ext.bearish, Modifier.weight(1f))
                    PriceLevelChip("TP", trade.takeProfit, ext.bullish, Modifier.weight(1f))
                }
            }

            if (!trade.imageUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                PostImageBanner(
                    imageUrl = trade.imageUrl,
                    contentDescription = trade.title
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            ReactionBar(
                fireCount = trade.reactionFire,
                rocketCount = trade.reactionRocket,
                hundredCount = trade.reactionHundred,
                bullCount = trade.reactionBull,
                userSelectedEmojis = selectedEmojis,
                onToggleEmoji = onToggleEmoji
            )
        }
    }
}

@Composable
private fun PriceLevelChip(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val ext = LocalMarketExtendedColors.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ext.elevatedCard,
        border = BorderStroke(1.dp, ext.cardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value.ifBlank { "--" },
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TradeDetailSheet(
    trade: VipTradePost,
    selectedEmojis: List<String>,
    onToggleEmoji: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalMarketExtendedColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${trade.pair} • ${TradeDirection.fromRaw(trade.direction).label}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close detail")
                }
            }
            Text(
                text = trade.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatFeedTimestamp(trade.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!trade.imageUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                PostImageBanner(
                    imageUrl = trade.imageUrl,
                    contentDescription = trade.title,
                    modifier = Modifier.height(210.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PriceLevelChip("Entry", trade.entryPrice, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                PriceLevelChip("Stop Loss", trade.stopLoss, ext.bearish, Modifier.weight(1f))
                PriceLevelChip("Take Profit", trade.takeProfit, ext.bullish, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = trade.body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(18.dp))
            ReactionBar(
                fireCount = trade.reactionFire,
                rocketCount = trade.reactionRocket,
                hundredCount = trade.reactionHundred,
                bullCount = trade.reactionBull,
                userSelectedEmojis = selectedEmojis,
                onToggleEmoji = onToggleEmoji
            )
        }
    }
}
