package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Insights
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.MarketAnalysisPost
import com.example.data.model.UiState
import com.example.ui.components.NotificationBellButton
import com.example.ui.components.PostImageBanner
import com.example.ui.components.ReactionBar
import com.example.ui.components.formatFeedTimestamp
import com.example.ui.theme.LocalMarketExtendedColors

@Composable
fun MarketScreen(
    analysesState: UiState<List<MarketAnalysisPost>>,
    userReactions: Map<String, List<String>>,
    unreadAlertsCount: Int,
    selectedAnalysisId: String?,
    onSelectAnalysis: (MarketAnalysisPost?) -> Unit,
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
                    text = "Market Analysis",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Chart breakdowns, support & resistance zones",
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
                        .testTag("market_search_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (showSearch) Icons.Rounded.Close else Icons.Rounded.Search,
                            contentDescription = "Search market analysis",
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
                placeholder = { Text("Search pair, timeframe, or keyword...") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .testTag("market_search_input")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (analysesState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            is UiState.Error -> ErrorInlineCard(analysesState.message)
            is UiState.Success -> {
                val filtered = analysesState.data.filter { item ->
                    val q = query.trim()
                    q.isEmpty() ||
                        item.title.contains(q, ignoreCase = true) ||
                        item.pair.contains(q, ignoreCase = true) ||
                        item.timeframe.contains(q, ignoreCase = true) ||
                        item.summary.contains(q, ignoreCase = true)
                }
                if (filtered.isEmpty()) {
                    EmptyInlineCard(
                        icon = Icons.Rounded.Insights,
                        message = if (query.isBlank()) "No market analysis posts yet." else "No charts matched \"$query\"."
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filtered, key = { it.id }) { analysis ->
                            MarketAnalysisCard(
                                analysis = analysis,
                                selectedEmojis = userReactions[analysis.id].orEmpty(),
                                onClick = { onSelectAnalysis(analysis) },
                                onToggleEmoji = { emoji -> onToggleReaction(analysis.id, emoji) }
                            )
                        }
                    }
                }

                val activeDetail = selectedAnalysisId?.let { id ->
                    analysesState.data.firstOrNull { it.id == id }
                }
                if (activeDetail != null) {
                    AnalysisDetailSheet(
                        analysis = activeDetail,
                        selectedEmojis = userReactions[activeDetail.id].orEmpty(),
                        onToggleEmoji = { emoji -> onToggleReaction(activeDetail.id, emoji) },
                        onDismiss = { onSelectAnalysis(null) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketAnalysisCard(
    analysis: MarketAnalysisPost,
    selectedEmojis: List<String>,
    onClick: () -> Unit,
    onToggleEmoji: (String) -> Unit
) {
    val ext = LocalMarketExtendedColors.current
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ext.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("analysis_card_${analysis.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = analysis.pair,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ext.elevatedCard,
                        border = BorderStroke(1.dp, ext.cardBorder)
                    ) {
                        Text(
                            text = analysis.timeframe,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Text(
                    text = formatFeedTimestamp(analysis.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = analysis.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!analysis.imageUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                PostImageBanner(
                    imageUrl = analysis.imageUrl,
                    contentDescription = analysis.title
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = analysis.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (analysis.resistanceLevels.isNotEmpty() || analysis.supportLevels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                KeyLevelsGrid(
                    resistances = analysis.resistanceLevels,
                    supports = analysis.supportLevels
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            ReactionBar(
                fireCount = analysis.reactionFire,
                rocketCount = analysis.reactionRocket,
                hundredCount = analysis.reactionHundred,
                bullCount = analysis.reactionBull,
                userSelectedEmojis = selectedEmojis,
                onToggleEmoji = onToggleEmoji
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeyLevelsGrid(
    resistances: List<String>,
    supports: List<String>
) {
    val ext = LocalMarketExtendedColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (resistances.isNotEmpty()) {
            LevelColumnBox(
                title = "Resistance",
                levels = resistances,
                accent = ext.bearish,
                modifier = Modifier.weight(1f)
            )
        }
        if (supports.isNotEmpty()) {
            LevelColumnBox(
                title = "Support",
                levels = supports,
                accent = ext.bullish,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LevelColumnBox(
    title: String,
    levels: List<String>,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val ext = LocalMarketExtendedColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ext.elevatedCard,
        border = BorderStroke(1.dp, ext.cardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                levels.forEach { lvl ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accent.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = lvl,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalysisDetailSheet(
    analysis: MarketAnalysisPost,
    selectedEmojis: List<String>,
    onToggleEmoji: (String) -> Unit,
    onDismiss: () -> Unit
) {
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
                    text = "${analysis.pair} • ${analysis.timeframe}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close detail")
                }
            }
            Text(
                text = analysis.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatFeedTimestamp(analysis.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!analysis.imageUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                PostImageBanner(
                    imageUrl = analysis.imageUrl,
                    contentDescription = analysis.title,
                    modifier = Modifier.height(210.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = analysis.summary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (analysis.resistanceLevels.isNotEmpty() || analysis.supportLevels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                KeyLevelsGrid(
                    resistances = analysis.resistanceLevels,
                    supports = analysis.supportLevels
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            ReactionBar(
                fireCount = analysis.reactionFire,
                rocketCount = analysis.reactionRocket,
                hundredCount = analysis.reactionHundred,
                bullCount = analysis.reactionBull,
                userSelectedEmojis = selectedEmojis,
                onToggleEmoji = onToggleEmoji
            )
        }
    }
}
