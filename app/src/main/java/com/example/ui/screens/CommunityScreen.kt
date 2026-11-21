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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CommunityMessage
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.InitialsAvatar
import com.example.ui.components.formatTimeAgo
import com.example.ui.theme.LocalMarketExtendedColors

private val COMMUNITY_RULES = listOf(
    "1. Treat everyone with respect",
    "2. No abusive or critical language",
    "3. Learn, share and grow together",
    "4. Keep it about the markets"
)

@Composable
fun CommunityScreen(
    profile: UserProfile?,
    isCreator: Boolean,
    messagesState: UiState<List<CommunityMessage>>,
    onAcceptGuidelines: () -> Unit,
    onSendMessage: (String, String, () -> Unit) -> Unit,
    onDeleteMessage: (String) -> Unit
) {
    if (profile == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    if (!profile.joinedGuidelines) {
        CommunityGuidelinesGate(onJoinCommunity = onAcceptGuidelines)
    } else {
        CommunityFeedContent(
            currentUserId = profile.userId,
            isSuspended = profile.suspended,
            isCreator = isCreator,
            messagesState = messagesState,
            onSendMessage = onSendMessage,
            onDeleteMessage = onDeleteMessage
        )
    }
}

@Composable
private fun CommunityGuidelinesGate(
    onJoinCommunity: () -> Unit
) {
    val ext = LocalMarketExtendedColors.current
    var agreed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text(
                        text = "Community Guidelines",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Before entering the trading floor",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, ext.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    COMMUNITY_RULES.forEach { rule ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ext.elevatedCard,
                            border = BorderStroke(1.dp, ext.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = rule,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ext.warning.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, ext.warning.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Note: Moderators can remove posts and suspend members who break these rules.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { agreed = !agreed }
                    .padding(vertical = 8.dp)
                    .testTag("guidelines_agree_checkbox_row")
            ) {
                Checkbox(
                    checked = agreed,
                    onCheckedChange = { agreed = it },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("guidelines_agree_checkbox")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "I've read and agree to follow these guidelines",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onJoinCommunity,
                enabled = agreed,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("join_community_button")
            ) {
                Text(
                    text = "Join the community",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CommunityFeedContent(
    currentUserId: String,
    isSuspended: Boolean,
    isCreator: Boolean,
    messagesState: UiState<List<CommunityMessage>>,
    onSendMessage: (String, String, () -> Unit) -> Unit,
    onDeleteMessage: (String) -> Unit
) {
    val ext = LocalMarketExtendedColors.current
    var messageInput by remember { mutableStateOf("") }
    var tickerTag by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(
            text = "Trader Community",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Share setups, chart levels, and market commentary",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isSuspended) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ext.bearish.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, ext.bearish.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Rounded.Block, contentDescription = null, tint = ext.bearish)
                    Text(
                        text = "Your account is currently suspended from posting in the community.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.bearish
                    )
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, ext.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = { Text("Share a chart setup or market thought...") },
                        maxLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("community_message_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = tickerTag,
                            onValueChange = { tickerTag = it.uppercase().take(14) },
                            placeholder = { Text("Pair (e.g. EUR/USD)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (messageInput.isNotBlank()) {
                                    onSendMessage(messageInput, tickerTag) {
                                        messageInput = ""
                                        tickerTag = ""
                                    }
                                }
                            },
                            enabled = messageInput.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("community_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Post message",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Post")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        when (messagesState) {
            is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            is UiState.Error -> ErrorInlineCard(messagesState.message)
            is UiState.Success -> {
                if (messagesState.data.isEmpty()) {
                    EmptyInlineCard(
                        icon = Icons.Rounded.Forum,
                        message = "No community posts yet. Start the market discussion!"
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(messagesState.data, key = { it.id }) { msg ->
                            Surface(
                                shape = RoundedCornerShape(18.dp),
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
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            InitialsAvatar(
                                                initials = msg.authorInitials,
                                                sizeDp = 36
                                            )
                                            Column {
                                                Text(
                                                    text = msg.authorName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = formatTimeAgo(msg.createdAt),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (msg.tickerTag.isNotBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                                ) {
                                                    Text(
                                                        text = msg.tickerTag,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                            if (msg.userId == currentUserId || isCreator) {
                                                IconButton(onClick = { onDeleteMessage(msg.id) }) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.DeleteOutline,
                                                        contentDescription = "Delete message",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = msg.body,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
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
