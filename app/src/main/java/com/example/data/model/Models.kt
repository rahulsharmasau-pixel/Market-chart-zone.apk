package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class TradeDirection(val label: String) {
    LONG("LONG"),
    SHORT("SHORT"),
    NEUTRAL("NEUTRAL");

    companion object {
        fun fromRaw(raw: String?): TradeDirection =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: NEUTRAL
    }
}

enum class NotificationCategory(val key: String, val label: String) {
    VIP_TRADES("vip_trades", "VIP Trades"),
    MARKET_ANALYSIS("market_analysis", "Market Analysis"),
    ANNOUNCEMENTS("announcements", "Announcements");

    companion object {
        fun fromKey(raw: String?): NotificationCategory =
            entries.firstOrNull { it.key == raw } ?: ANNOUNCEMENTS
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val joinedGuidelines: Boolean = false,
    val suspended: Boolean = false,
    val fcmTokens: List<String> = emptyList(),
    val mutedCategories: List<String> = emptyList(),
    val darkMode: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun initials(): String {
        val parts = displayName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> email.take(1).uppercase().ifBlank { "M" }
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "${parts.first().first()}${parts.last().first()}".uppercase()
        }
    }
}

@Keep
@IgnoreExtraProperties
data class AdminCredentialConfig(
    @DocumentId val id: String = "",
    val creatorUid: String = "",
    val adminUserId: String = "",
    val passwordHash: String = "",
    val salt: String = "",
    val failedAttempts: Long = 0L,
    val lockedUntilEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VipTradePost(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val pair: String = "",
    val direction: String = TradeDirection.LONG.name,
    val entryPrice: String = "",
    val stopLoss: String = "",
    val takeProfit: String = "",
    val imageUrl: String? = null,
    val authorId: String = "",
    val reactionFire: Long = 0L,
    val reactionRocket: Long = 0L,
    val reactionHundred: Long = 0L,
    val reactionBull: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MarketAnalysisPost(
    @DocumentId val id: String = "",
    val title: String = "",
    val pair: String = "",
    val timeframe: String = "15M",
    val summary: String = "",
    val supportLevels: List<String> = emptyList(),
    val resistanceLevels: List<String> = emptyList(),
    val imageUrl: String? = null,
    val authorId: String = "",
    val reactionFire: Long = 0L,
    val reactionRocket: Long = 0L,
    val reactionHundred: Long = 0L,
    val reactionBull: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CommunityMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorInitials: String = "",
    val body: String = "",
    val tickerTag: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val category: String = NotificationCategory.ANNOUNCEMENTS.key,
    val targetId: String? = null,
    val imageUrl: String? = null,
    val authorId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class IssueReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val subject: String = "",
    val description: String = "",
    val status: String = "open",
    val adminNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppContentDoc(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserPostReaction(
    @DocumentId val id: String = "",
    val postId: String = "",
    val emojis: List<String> = emptyList(),
    @ServerTimestamp val updatedAt: Timestamp? = null
)

object ReactionEmoji {
    const val FIRE = "fire"
    const val ROCKET = "rocket"
    const val HUNDRED = "hundred"
    const val BULL = "bull"

    val all = listOf(
        FIRE to "\uD83D\uDD25",
        ROCKET to "\uD83D\uDE80",
        HUNDRED to "\uD83D\uDCAF",
        BULL to "\uD83D\uDC02"
    )
}
