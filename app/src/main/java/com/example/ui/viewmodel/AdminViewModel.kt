package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MarketRepository
import com.example.data.model.IssueReport
import com.example.data.model.TradeDirection
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AdminGateState {
    data object Checking : AdminGateState
    data object NeedsOneTimeSetup : AdminGateState
    data object NeedsLogin : AdminGateState
    data object Unlocked : AdminGateState
    data class AccessDenied(val reason: String) : AdminGateState
}

class AdminViewModel(
    private val repository: MarketRepository
) : ViewModel() {

    private val _gateState = MutableStateFlow<AdminGateState>(AdminGateState.Checking)
    val gateState: StateFlow<AdminGateState> = _gateState.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _errorBanner = MutableStateFlow<String?>(null)
    val errorBanner: StateFlow<String?> = _errorBanner.asStateFlow()

    private val _remainingSessionSeconds = MutableStateFlow(SESSION_TIMEOUT_SECONDS)
    val remainingSessionSeconds: StateFlow<Int> = _remainingSessionSeconds.asStateFlow()

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents = _toastEvents.asSharedFlow()

    val usersState: StateFlow<UiState<List<UserProfile>>> = repository.observeAllUsersForAdmin()
        .map<List<UserProfile>, UiState<List<UserProfile>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Admin users list error", root)
            emit(UiState.Error(root.message ?: "Failed to load users"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val reportsState: StateFlow<UiState<List<IssueReport>>> = repository.observeAllReportsForAdmin()
        .map<List<IssueReport>, UiState<List<IssueReport>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Admin reports list error", root)
            emit(UiState.Error(root.message ?: "Failed to load issue reports"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    init {
        checkAdminGate()
        startSessionTicker()
    }

    fun checkAdminGate() {
        if (!repository.isCreatorAccount()) {
            _gateState.value = AdminGateState.AccessDenied("Admin access is restricted to the app creator.")
            return
        }
        viewModelScope.launch {
            _gateState.value = AdminGateState.Checking
            _errorBanner.value = null
            repository.getAdminConfig()
                .onSuccess { cfg ->
                    _gateState.value = if (cfg == null) {
                        AdminGateState.NeedsOneTimeSetup
                    } else {
                        AdminGateState.NeedsLogin
                    }
                }
                .onFailure { err ->
                    _gateState.value = AdminGateState.AccessDenied(
                        err.localizedMessage ?: "Unable to verify admin status"
                    )
                }
        }
    }

    private fun startSessionTicker() {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                if (_gateState.value == AdminGateState.Unlocked) {
                    val next = _remainingSessionSeconds.value - 1
                    if (next <= 0) {
                        signOutAdmin("Admin session timed out after inactivity.")
                    } else {
                        _remainingSessionSeconds.value = next
                    }
                }
            }
        }
    }

    fun touchActivity() {
        if (_gateState.value == AdminGateState.Unlocked) {
            _remainingSessionSeconds.value = SESSION_TIMEOUT_SECONDS
        }
    }

    fun completeOneTimeSetup(adminUserId: String, password: String, confirmPassword: String) {
        if (!repository.isCreatorAccount()) {
            _errorBanner.value = "Only the app creator can initialize admin credentials."
            return
        }
        if (password != confirmPassword) {
            _errorBanner.value = "Passwords do not match."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            _errorBanner.value = null
            repository.createAdminConfigOnce(adminUserId, password)
                .onSuccess {
                    _remainingSessionSeconds.value = SESSION_TIMEOUT_SECONDS
                    _gateState.value = AdminGateState.Unlocked
                    _toastEvents.tryEmit("Admin credentials created permanently. Welcome!")
                }
                .onFailure { err ->
                    _errorBanner.value = err.localizedMessage ?: "Failed to create admin credentials"
                }
            _isBusy.value = false
        }
    }

    fun signInAdmin(adminUserId: String, password: String) {
        viewModelScope.launch {
            _isBusy.value = true
            _errorBanner.value = null
            repository.verifyAdminLogin(adminUserId, password)
                .onSuccess {
                    _remainingSessionSeconds.value = SESSION_TIMEOUT_SECONDS
                    _gateState.value = AdminGateState.Unlocked
                }
                .onFailure { err ->
                    _errorBanner.value = err.localizedMessage ?: "Admin sign-in failed"
                }
            _isBusy.value = false
        }
    }

    fun signOutAdmin(reason: String? = null) {
        _gateState.value = AdminGateState.NeedsLogin
        _remainingSessionSeconds.value = SESSION_TIMEOUT_SECONDS
        if (reason != null) {
            _errorBanner.value = reason
        }
    }

    fun changeAdminPassword(currentPassword: String, newPassword: String, confirmPassword: String, onSuccess: () -> Unit) {
        touchActivity()
        if (newPassword != confirmPassword) {
            _toastEvents.tryEmit("New passwords do not match")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            repository.changeAdminPassword(currentPassword, newPassword)
                .onSuccess {
                    _toastEvents.tryEmit("Admin password updated")
                    onSuccess()
                }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Failed to update password") }
            _isBusy.value = false
        }
    }

    fun saveVipTrade(
        existingId: String?,
        title: String,
        body: String,
        pair: String,
        direction: TradeDirection,
        entryPrice: String,
        stopLoss: String,
        takeProfit: String,
        imageUrl: String?,
        onSuccess: () -> Unit
    ) {
        touchActivity()
        viewModelScope.launch {
            _isBusy.value = true
            repository.saveVipTrade(
                existingId = existingId,
                title = title,
                body = body,
                pair = pair,
                direction = direction,
                entryPrice = entryPrice,
                stopLoss = stopLoss,
                takeProfit = takeProfit,
                imageUrl = imageUrl
            ).onSuccess {
                _toastEvents.tryEmit(if (existingId == null) "VIP trade published & alert queued" else "VIP trade updated")
                onSuccess()
            }.onFailure {
                _toastEvents.tryEmit(it.localizedMessage ?: "Failed to save VIP trade")
            }
            _isBusy.value = false
        }
    }

    fun deleteVipTrade(tradeId: String) {
        touchActivity()
        viewModelScope.launch {
            repository.deleteVipTrade(tradeId)
                .onSuccess { _toastEvents.tryEmit("VIP trade deleted") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Failed to delete trade") }
        }
    }

    fun saveMarketAnalysis(
        existingId: String?,
        title: String,
        pair: String,
        timeframe: String,
        summary: String,
        supportCsv: String,
        resistanceCsv: String,
        imageUrl: String?,
        onSuccess: () -> Unit
    ) {
        touchActivity()
        val supports = supportCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val resistances = resistanceCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
        viewModelScope.launch {
            _isBusy.value = true
            repository.saveMarketAnalysis(
                existingId = existingId,
                title = title,
                pair = pair,
                timeframe = timeframe,
                summary = summary,
                supportLevels = supports,
                resistanceLevels = resistances,
                imageUrl = imageUrl
            ).onSuccess {
                _toastEvents.tryEmit(if (existingId == null) "Analysis published & alert queued" else "Analysis updated")
                onSuccess()
            }.onFailure {
                _toastEvents.tryEmit(it.localizedMessage ?: "Failed to save analysis")
            }
            _isBusy.value = false
        }
    }

    fun deleteMarketAnalysis(analysisId: String) {
        touchActivity()
        viewModelScope.launch {
            repository.deleteMarketAnalysis(analysisId)
                .onSuccess { _toastEvents.tryEmit("Market analysis deleted") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Failed to delete analysis") }
        }
    }

    fun sendManualPush(title: String, message: String, imageUrl: String?, onSuccess: () -> Unit) {
        touchActivity()
        viewModelScope.launch {
            _isBusy.value = true
            repository.sendManualPushAnnouncement(title, message, imageUrl)
                .onSuccess {
                    _toastEvents.tryEmit("Push announcement dispatched")
                    onSuccess()
                }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Failed to send announcement") }
            _isBusy.value = false
        }
    }

    fun toggleUserSuspension(userId: String, currentlySuspended: Boolean) {
        touchActivity()
        viewModelScope.launch {
            repository.setUserSuspended(userId, !currentlySuspended)
                .onSuccess {
                    _toastEvents.tryEmit(if (currentlySuspended) "Member unsuspended" else "Member suspended")
                }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Failed to update member") }
        }
    }

    fun deleteUserByAdmin(userId: String) {
        touchActivity()
        viewModelScope.launch {
            repository.deleteUserAccountByAdmin(userId)
                .onSuccess { _toastEvents.tryEmit("User account & data removed") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Failed to remove user") }
        }
    }

    fun deleteCommunityMessage(messageId: String) {
        touchActivity()
        viewModelScope.launch {
            repository.deleteCommunityMessage(messageId)
                .onSuccess { _toastEvents.tryEmit("Community post removed") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Failed to remove post") }
        }
    }

    fun resolveReport(reportId: String, adminNote: String) {
        touchActivity()
        viewModelScope.launch {
            repository.resolveIssueReport(reportId, adminNote)
                .onSuccess { _toastEvents.tryEmit("Report marked resolved") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Failed to update report") }
        }
    }

    fun saveLegalContent(docId: String, title: String, body: String) {
        touchActivity()
        viewModelScope.launch {
            _isBusy.value = true
            repository.saveAppContent(docId, title, body)
                .onSuccess { _toastEvents.tryEmit("Content updated") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Failed to save content") }
            _isBusy.value = false
        }
    }

    fun seedStarterContent() {
        touchActivity()
        viewModelScope.launch {
            _isBusy.value = true
            repository.seedStarterContentIfEmpty()
                .onSuccess { _toastEvents.tryEmit("Starter trades and analyses ready") }
                .onFailure { _toastEvents.tryEmit(it.localizedMessage ?: "Could not seed content") }
            _isBusy.value = false
        }
    }

    private companion object {
        const val TAG = "AdminVM"
        const val SESSION_TIMEOUT_SECONDS = 15 * 60
    }
}
