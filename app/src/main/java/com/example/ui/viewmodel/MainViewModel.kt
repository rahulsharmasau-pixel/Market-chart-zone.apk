package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MarketRepository
import com.example.data.PreferencesManager
import com.example.data.model.Announcement
import com.example.data.model.AppContentDoc
import com.example.data.model.CommunityMessage
import com.example.data.model.MarketAnalysisPost
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.VipTradePost
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: MarketRepository,
    private val preferencesManager: PreferencesManager,
    val currentUserId: String
) : ViewModel() {

    val isCreator: Boolean = repository.isCreatorAccount()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    private fun <T> Flow<T>.toUiStateFlow(label: String, initial: UiState<T> = UiState.Loading): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = initial
            )

    val userProfile: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId).toUiStateFlow("profile")

    val vipTrades: StateFlow<UiState<List<VipTradePost>>> =
        repository.observeVipTrades().toUiStateFlow("VIP trades")

    val marketAnalyses: StateFlow<UiState<List<MarketAnalysisPost>>> =
        repository.observeMarketAnalyses().toUiStateFlow("market analysis")

    val communityMessages: StateFlow<UiState<List<CommunityMessage>>> =
        repository.observeCommunityMessages().toUiStateFlow("community messages")

    val announcements: StateFlow<UiState<List<Announcement>>> =
        repository.observeAnnouncements().toUiStateFlow("notifications")

    val userReactions: StateFlow<Map<String, List<String>>> =
        repository.observeUserReactions(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(emptyMap())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyMap()
            )

    val appContent: StateFlow<Map<String, AppContentDoc>> =
        repository.observeAppContent()
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(emptyMap())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyMap()
            )

    init {
        viewModelScope.launch {
            val defaultDark = preferencesManager.darkModeFlow.first()
            repository.ensureUserProfile(defaultDark).onSuccess { profile ->
                preferencesManager.setDarkMode(profile.darkMode)
            }
            if (isCreator) {
                repository.seedStarterContentIfEmpty()
            }
        }
    }

    fun registerFcmToken(token: String) {
        viewModelScope.launch {
            repository.addFcmToken(token)
        }
    }

    suspend fun removeFcmTokenBeforeSignOut(token: String?) {
        repository.removeFcmToken(token)
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDarkMode(enabled)
            repository.updateDarkMode(enabled).onFailure {
                _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to update theme")
            }
        }
    }

    fun updateDisplayName(newName: String) {
        viewModelScope.launch {
            repository.updateProfileName(newName)
                .onSuccess { _snackbarEvents.tryEmit("Profile updated") }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to update profile") }
        }
    }

    fun acceptCommunityGuidelines() {
        viewModelScope.launch {
            repository.acceptCommunityGuidelines()
                .onSuccess { _snackbarEvents.tryEmit("Welcome to the Market Chart Zone community") }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Could not join community") }
        }
    }

    fun toggleMutedCategory(categoryKey: String, currentMuted: List<String>) {
        val next = if (currentMuted.contains(categoryKey)) {
            currentMuted - categoryKey
        } else {
            currentMuted + categoryKey
        }
        viewModelScope.launch {
            repository.updateMutedCategories(next)
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to update notification settings") }
        }
    }

    fun toggleTradeReaction(tradeId: String, emojiKey: String) {
        val current = userReactions.value[tradeId].orEmpty()
        viewModelScope.launch {
            repository.toggleReaction("vip_trades", tradeId, emojiKey, current)
                .onFailure { _snackbarEvents.tryEmit("Could not update reaction") }
        }
    }

    fun toggleAnalysisReaction(analysisId: String, emojiKey: String) {
        val current = userReactions.value[analysisId].orEmpty()
        viewModelScope.launch {
            repository.toggleReaction("market_analyses", analysisId, emojiKey, current)
                .onFailure { _snackbarEvents.tryEmit("Could not update reaction") }
        }
    }

    fun sendCommunityMessage(body: String, tickerTag: String, onSuccess: () -> Unit) {
        val profile = (userProfile.value as? UiState.Success)?.data
        val name = profile?.displayName?.ifBlank { "Trader" } ?: "Trader"
        val initials = profile?.initials() ?: "T"
        viewModelScope.launch {
            repository.sendCommunityMessage(name, initials, body, tickerTag)
                .onSuccess { onSuccess() }
                .onFailure {
                    _snackbarEvents.tryEmit(it.localizedMessage ?: "Unable to post message")
                }
        }
    }

    fun deleteCommunityMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteCommunityMessage(messageId)
                .onSuccess { _snackbarEvents.tryEmit("Message deleted") }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Could not delete message") }
        }
    }

    fun submitIssueReport(subject: String, description: String, onSuccess: () -> Unit) {
        val profile = (userProfile.value as? UiState.Success)?.data
        viewModelScope.launch {
            repository.submitIssueReport(
                userName = profile?.displayName ?: "Member",
                userEmail = profile?.email ?: "",
                subject = subject,
                description = description
            ).onSuccess {
                _snackbarEvents.tryEmit("Issue report submitted")
                onSuccess()
            }.onFailure {
                _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to submit report")
            }
        }
    }

    fun deleteOwnAccount(onDeleted: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            repository.deleteOwnAccount()
                .onSuccess { onDeleted() }
                .onFailure { err ->
                    val msg = err.localizedMessage
                        ?: "Please sign out and sign back in recently before deleting your account."
                    onError(msg)
                }
        }
    }

    private companion object {
        const val TAG = "MainVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
