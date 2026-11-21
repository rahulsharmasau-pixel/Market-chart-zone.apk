package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CandlestickChart
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CandlestickChart
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Announcement
import com.example.data.model.NotificationCategory
import com.example.data.model.ReactionEmoji
import com.example.ui.theme.LocalMarketExtendedColors
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class BottomTab(
    val route: String,
    val label: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
) {
    HOME("home", "Home", Icons.Rounded.Home, Icons.Outlined.Home),
    TRADES("trades", "Trades", Icons.Rounded.CandlestickChart, Icons.Outlined.CandlestickChart),
    MARKET("market", "Market", Icons.Rounded.Insights, Icons.Outlined.Insights),
    COMMUNITY("community", "Community", Icons.Rounded.Forum, Icons.Outlined.Forum),
    TOOLS("tools", "Tools", Icons.Rounded.Calculate, Icons.Outlined.Calculate)
}

@Composable
fun MarketBottomNavBar(
    currentRoute: String,
    onSelectTab: (BottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMarketExtendedColors.current
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, ext.cardBorder),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomTab.entries.forEach { tab ->
                val selected = currentRoute == tab.route
                val pillColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                    label = "pillColor"
                )
                val iconTint by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "iconTint"
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onSelectTab(tab) }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .testTag("nav_tab_${tab.route}")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .then(
                                if (selected) {
                                    Modifier.shadow(
                                        elevation = 12.dp,
                                        shape = CircleShape,
                                        ambientColor = MaterialTheme.colorScheme.primary,
                                        spotColor = MaterialTheme.colorScheme.primary
                                    )
                                } else Modifier
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(pillColor)
                            .then(
                                if (selected) {
                                    Modifier.border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                        RoundedCornerShape(16.dp)
                                    )
                                } else Modifier
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (selected) tab.activeIcon else tab.inactiveIcon,
                            contentDescription = tab.label,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = iconTint,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun InitialsAvatar(
    initials: String,
    onClick: (() -> Unit)? = null,
    sizeDp: Int = 44
) {
    val brush = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary
        )
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(brush)
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable(onClick = onClick)
                        .minimumInteractiveComponentSize()
                        .testTag("avatar_account_button")
                } else Modifier
            )
    ) {
        Text(
            text = initials.take(2).uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun NotificationBellButton(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalMarketExtendedColors.current
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ext.cardBorder),
        modifier = modifier
            .size(48.dp)
            .testTag("notification_bell_button")
    ) {
        Box(contentAlignment = Alignment.Center) {
            BadgedBox(
                badge = {
                    if (unreadCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(
                                text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun PostImageBanner(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    if (imageUrl.isNullOrBlank()) return
    val ext = LocalMarketExtendedColors.current
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(188.dp)
            .clip(shape)
            .border(1.dp, ext.cardBorder, shape)
    ) {
        when (imageUrl) {
            "local:img_chart_eurusd" -> Image(
                painter = painterResource(id = R.drawable.img_chart_eurusd),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            "local:img_chart_xauusd" -> Image(
                painter = painterResource(id = R.drawable.img_chart_xauusd),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            "local:img_brand_hero" -> Image(
                painter = painterResource(id = R.drawable.img_brand_hero),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            else -> AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.img_brand_hero),
                error = painterResource(id = R.drawable.img_chart_eurusd),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReactionBar(
    fireCount: Long,
    rocketCount: Long,
    hundredCount: Long,
    bullCount: Long,
    userSelectedEmojis: List<String>,
    onToggleEmoji: (String) -> Unit,
    compact: Boolean = false
) {
    val ext = LocalMarketExtendedColors.current
    val counts = mapOf(
        ReactionEmoji.FIRE to fireCount,
        ReactionEmoji.ROCKET to rocketCount,
        ReactionEmoji.HUNDRED to hundredCount,
        ReactionEmoji.BULL to bullCount
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReactionEmoji.all.forEach { (key, glyph) ->
            val selected = userSelectedEmojis.contains(key)
            val count = counts[key] ?: 0L
            Surface(
                onClick = { onToggleEmoji(key) },
                shape = RoundedCornerShape(50),
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                } else {
                    ext.elevatedCard
                },
                border = BorderStroke(
                    1.dp,
                    if (selected) MaterialTheme.colorScheme.primary else ext.cardBorder
                ),
                modifier = Modifier.testTag("reaction_${key}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(
                        horizontal = if (compact) 10.dp else 12.dp,
                        vertical = if (compact) 5.dp else 7.dp
                    )
                ) {
                    Text(text = glyph, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(
    announcements: List<Announcement>,
    mutedCategories: List<String>,
    onDismiss: () -> Unit,
    onAnnouncementClick: (Announcement) -> Unit
) {
    val ext = LocalMarketExtendedColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Notifications & Alerts",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close notifications")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val visible = announcements.filter { !mutedCategories.contains(it.category) }
            if (visible.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ext.elevatedCard,
                    border = BorderStroke(1.dp, ext.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You're all caught up",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "New VIP trade signals, chart analyses, and announcements appear here in real time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    modifier = Modifier.height(380.dp)
                ) {
                    items(visible, key = { it.id }) { alert ->
                        val cat = NotificationCategory.fromKey(alert.category)
                        Surface(
                            onClick = { onAnnouncementClick(alert) },
                            shape = RoundedCornerShape(16.dp),
                            color = ext.elevatedCard,
                            border = BorderStroke(1.dp, ext.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = cat.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Text(
                                        text = formatTimeAgo(alert.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = alert.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = alert.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!alert.imageUrl.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    PostImageBanner(
                                        imageUrl = alert.imageUrl,
                                        contentDescription = alert.title,
                                        modifier = Modifier.height(120.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatTimeAgo(timestamp: Timestamp?): String {
    val date = timestamp?.toDate() ?: return "Just now"
    val diffMs = (System.currentTimeMillis() - date.time).coerceAtLeast(0L)
    val mins = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
    val days = TimeUnit.MILLISECONDS.toDays(diffMs)
    return when {
        mins < 1 -> "Just now"
        mins < 60 -> "${mins}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
}

fun formatFeedTimestamp(timestamp: Timestamp?): String {
    val date = timestamp?.toDate() ?: Date()
    val targetCal = Calendar.getInstance().apply { time = date }
    val todayCal = Calendar.getInstance()
    val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)

    return when {
        targetCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
            targetCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR) ->
            "Today $timeFormat"
        targetCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
            targetCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR) ->
            "Yesterday $timeFormat"
        else -> SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault()).format(date)
    }
}
