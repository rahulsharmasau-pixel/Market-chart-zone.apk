package com.example.data

import android.content.Context
import com.example.R
import com.example.data.model.AdminCredentialConfig
import com.example.data.model.Announcement
import com.example.data.model.AppContentDoc
import com.example.data.model.CommunityMessage
import com.example.data.model.IssueReport
import com.example.data.model.MarketAnalysisPost
import com.example.data.model.NotificationCategory
import com.example.data.model.ReactionEmoji
import com.example.data.model.TradeDirection
import com.example.data.model.UserPostReaction
import com.example.data.model.UserProfile
import com.example.data.model.VipTradePost
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class MarketRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        FirebaseAuth.getInstance()
    )

    companion object {
        const val CREATOR_EMAIL = "s21903125@gmail.com"
        const val MAX_ADMIN_LOGIN_ATTEMPTS = 5
        const val ADMIN_LOCKOUT_MS = 15 * 60 * 1000L
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    fun isCreatorAccount(): Boolean {
        val user = auth.currentUser ?: return false
        return user.isEmailVerified && user.email.equals(CREATOR_EMAIL, ignoreCase = true)
    }

    private fun <T> Flow<T>.handleStreamErrors(
        operationType: OperationType,
        path: String,
        expectedUid: String? = null
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && (expectedUid == null || auth.currentUser?.uid == expectedUid)) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    suspend fun ensureUserProfile(defaultDarkMode: Boolean): Result<UserProfile> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val user = auth.currentUser!!
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (existing.exists()) {
                val parsed = existing.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfile(userId = uid)
                Result.success(parsed)
            } else {
                val fallbackName = user.displayName?.takeIf { it.isNotBlank() }
                    ?: user.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
                    ?: "Trader"
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to fallbackName.take(80),
                    "email" to (user.email ?: "").take(160),
                    "joinedGuidelines" to false,
                    "suspended" to false,
                    "fcmTokens" to emptyList<String>(),
                    "mutedCategories" to emptyList<String>(),
                    "darkMode" to defaultDarkMode,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                val created = docRef.get().await().toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfile(
                    id = uid,
                    userId = uid,
                    displayName = fallbackName,
                    email = user.email ?: "",
                    darkMode = defaultDarkMode
                )
                Result.success(created)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) null
                    else snap.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .handleStreamErrors(OperationType.GET, path, uid)
        )
    }

    suspend fun updateProfileName(newName: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val trimmed = newName.trim().take(80)
        if (trimmed.isBlank()) return Result.failure(IllegalArgumentException("Name cannot be empty"))
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "displayName" to trimmed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateDarkMode(enabled: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "darkMode" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun acceptCommunityGuidelines(): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "joinedGuidelines" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateMutedCategories(categories: List<String>): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "mutedCategories" to categories.distinct().take(10),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addFcmToken(token: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        if (token.isBlank()) return Result.success(Unit)
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) return Result.success(Unit)
            val current = (snap.get("fcmTokens") as? List<*>)
                ?.filterIsInstance<String>()
                ?.toMutableList() ?: mutableListOf()
            if (!current.contains(token)) {
                current.add(0, token)
                val bounded = current.take(10)
                docRef.update(
                    mapOf(
                        "fcmTokens" to bounded,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun removeFcmToken(token: String?): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) return Result.success(Unit)
            val current = (snap.get("fcmTokens") as? List<*>)
                ?.filterIsInstance<String>()
                ?.toMutableList() ?: mutableListOf()
            if (token != null) {
                current.remove(token)
            } else {
                current.clear()
            }
            docRef.update(
                mapOf(
                    "fcmTokens" to current,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteOwnAccount(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not signed in"))
        val uid = user.uid
        return try {
            val reactions = db.collection("users").document(uid).collection("reactions").get().await()
            for (doc in reactions.documents) {
                doc.reference.delete().await()
            }
            db.collection("users").document(uid).delete().await()
            user.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$uid")
            Result.failure(e)
        }
    }

    // --- Feeds ---

    fun observeVipTrades(): Flow<List<VipTradePost>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("vip_trades")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        VipTradePost::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .handleStreamErrors(OperationType.LIST, "vip_trades", uid)
        )
    }

    fun observeMarketAnalyses(): Flow<List<MarketAnalysisPost>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("market_analyses")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        MarketAnalysisPost::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .handleStreamErrors(OperationType.LIST, "market_analyses", uid)
        )
    }

    fun observeCommunityMessages(): Flow<List<CommunityMessage>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("community_messages")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        CommunityMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .handleStreamErrors(OperationType.LIST, "community_messages", uid)
        )
    }

    fun observeAnnouncements(): Flow<List<Announcement>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("announcements")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        Announcement::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .handleStreamErrors(OperationType.LIST, "announcements", uid)
        )
    }

    fun observeUserReactions(userId: String): Flow<Map<String, List<String>>> = flow {
        val uid = requireUserId()
        check(uid == userId)
        val path = "users/$userId/reactions"
        emitAll(
            db.collection("users").document(userId).collection("reactions")
                .snapshots()
                .map { snap ->
                    snap.documents.associate { doc ->
                        val parsed = doc.toObject(
                            UserPostReaction::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                        doc.id to (parsed?.emojis ?: emptyList())
                    }
                }
                .handleStreamErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeAppContent(): Flow<Map<String, AppContentDoc>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("app_content")
                .snapshots()
                .map { snap ->
                    snap.documents.associate { doc ->
                        val parsed = doc.toObject(
                            AppContentDoc::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        ) ?: AppContentDoc(id = doc.id)
                        doc.id to parsed
                    }
                }
                .handleStreamErrors(OperationType.LIST, "app_content", uid)
        )
    }

    // --- Reactions ---

    suspend fun toggleReaction(
        collection: String,
        postId: String,
        emojiKey: String,
        currentUserEmojis: List<String>
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val fieldName = when (emojiKey) {
            ReactionEmoji.FIRE -> "reactionFire"
            ReactionEmoji.ROCKET -> "reactionRocket"
            ReactionEmoji.HUNDRED -> "reactionHundred"
            ReactionEmoji.BULL -> "reactionBull"
            else -> return Result.failure(IllegalArgumentException("Unsupported emoji"))
        }
        val alreadyReacted = currentUserEmojis.contains(emojiKey)
        val updatedEmojis = if (alreadyReacted) {
            currentUserEmojis - emojiKey
        } else {
            (currentUserEmojis + emojiKey).distinct().take(10)
        }
        val postRef = db.collection(collection).document(postId)
        val userReactRef = db.collection("users").document(uid).collection("reactions").document(postId)

        return try {
            db.runTransaction { tx ->
                val postSnap = tx.get(postRef)
                val currentCount = (postSnap.getLong(fieldName) ?: 0L).coerceAtLeast(0L)
                val nextCount = if (alreadyReacted) {
                    (currentCount - 1L).coerceAtLeast(0L)
                } else {
                    currentCount + 1L
                }
                tx.update(postRef, fieldName, nextCount)
                tx.set(
                    userReactRef,
                    mapOf(
                        "postId" to postId,
                        "emojis" to updatedEmojis,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "$collection/$postId")
            Result.failure(e)
        }
    }

    // --- Community & Reports ---

    suspend fun sendCommunityMessage(
        authorName: String,
        authorInitials: String,
        body: String,
        tickerTag: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanBody = body.trim().take(1000)
        if (cleanBody.isBlank()) return Result.failure(IllegalArgumentException("Message cannot be empty"))
        val docRef = db.collection("community_messages").document()
        return try {
            val payload = mapOf(
                "userId" to uid,
                "authorName" to authorName.trim().ifBlank { "Trader" }.take(80),
                "authorInitials" to authorInitials.trim().ifBlank { "T" }.take(4),
                "body" to cleanBody,
                "tickerTag" to tickerTag.trim().take(24),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCommunityMessage(messageId: String): Result<Unit> {
        requireUserId()
        val path = "community_messages/$messageId"
        return try {
            db.collection("community_messages").document(messageId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun submitIssueReport(
        userName: String,
        userEmail: String,
        subject: String,
        description: String
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanSubject = subject.trim().take(120)
        val cleanDesc = description.trim().take(2000)
        if (cleanSubject.isBlank() || cleanDesc.isBlank()) {
            return Result.failure(IllegalArgumentException("Subject and description are required"))
        }
        val docRef = db.collection("issue_reports").document()
        return try {
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "userEmail" to userEmail.take(160),
                    "userName" to userName.ifBlank { "Member" }.take(80),
                    "subject" to cleanSubject,
                    "description" to cleanDesc,
                    "status" to "open",
                    "adminNote" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Admin Security & Setup ---

    suspend fun getAdminConfig(): Result<AdminCredentialConfig?> {
        requireUserId()
        val path = "admin_meta/config"
        return try {
            val snap = db.collection("admin_meta").document("config").get().await()
            if (!snap.exists()) {
                Result.success(null)
            } else {
                val parsed = snap.toObject(
                    AdminCredentialConfig::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                Result.success(parsed)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun createAdminConfigOnce(adminUserId: String, password: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val cleanId = adminUserId.trim()
        if (cleanId.length < 3 || cleanId.length > 64) {
            return Result.failure(IllegalArgumentException("Admin User ID must be 3–64 characters"))
        }
        if (password.length < 8) {
            return Result.failure(IllegalArgumentException("Password must be at least 8 characters"))
        }
        return try {
            val hashed = SecurityCrypto.hashPassword(password.toCharArray())
            val configRef = db.collection("admin_meta").document("config")
            val adminRef = db.collection("admins").document(uid)
            val batch = db.batch()
            batch.set(
                configRef,
                mapOf(
                    "creatorUid" to uid,
                    "adminUserId" to cleanId,
                    "passwordHash" to hashed.hashBase64,
                    "salt" to hashed.saltBase64,
                    "failedAttempts" to 0L,
                    "lockedUntilEpochMs" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (!adminRef.get().await().exists()) {
                batch.set(
                    adminRef,
                    mapOf(
                        "uid" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "admin_meta/config")
            Result.failure(e)
        }
    }

    suspend fun verifyAdminLogin(adminUserId: String, password: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val configRef = db.collection("admin_meta").document("config")
        return try {
            val snap = configRef.get().await()
            val config = snap.toObject(
                AdminCredentialConfig::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return Result.failure(IllegalStateException("Admin has not been configured yet"))

            val now = System.currentTimeMillis()
            if (config.lockedUntilEpochMs > now) {
                val minsLeft = ((config.lockedUntilEpochMs - now) / 60_000L) + 1
                return Result.failure(IllegalStateException("Too many failed attempts. Try again in $minsLeft min."))
            }

            val idMatches = adminUserId.trim() == config.adminUserId
            val pwMatches = SecurityCrypto.verifyPassword(
                password = password.toCharArray(),
                saltBase64 = config.salt,
                expectedHashBase64 = config.passwordHash
            )

            if (idMatches && pwMatches) {
                configRef.update(
                    mapOf(
                        "failedAttempts" to 0L,
                        "lockedUntilEpochMs" to 0L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                val adminRef = db.collection("admins").document(uid)
                if (!adminRef.get().await().exists()) {
                    adminRef.set(mapOf("uid" to uid, "createdAt" to FieldValue.serverTimestamp())).await()
                }
                Result.success(Unit)
            } else {
                val nextAttempts = config.failedAttempts + 1L
                val lockUntil = if (nextAttempts >= MAX_ADMIN_LOGIN_ATTEMPTS) {
                    now + ADMIN_LOCKOUT_MS
                } else {
                    0L
                }
                configRef.update(
                    mapOf(
                        "failedAttempts" to nextAttempts,
                        "lockedUntilEpochMs" to lockUntil,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                val msg = if (lockUntil > 0L) {
                    "Too many failed attempts. Admin login locked for 15 minutes."
                } else {
                    val left = (MAX_ADMIN_LOGIN_ATTEMPTS - nextAttempts).coerceAtLeast(1L)
                    "Invalid Admin credentials. $left attempt(s) remaining."
                }
                Result.failure(IllegalArgumentException(msg))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "admin_meta/config")
            Result.failure(e)
        }
    }

    suspend fun changeAdminPassword(currentPassword: String, newPassword: String): Result<Unit> {
        requireUserId()
        if (newPassword.length < 8) {
            return Result.failure(IllegalArgumentException("New password must be at least 8 characters"))
        }
        val configRef = db.collection("admin_meta").document("config")
        return try {
            val snap = configRef.get().await()
            val config = snap.toObject(
                AdminCredentialConfig::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: return Result.failure(IllegalStateException("Admin config missing"))

            val verified = SecurityCrypto.verifyPassword(
                password = currentPassword.toCharArray(),
                saltBase64 = config.salt,
                expectedHashBase64 = config.passwordHash
            )
            if (!verified) {
                return Result.failure(IllegalArgumentException("Current admin password is incorrect"))
            }
            val hashed = SecurityCrypto.hashPassword(newPassword.toCharArray())
            configRef.update(
                mapOf(
                    "passwordHash" to hashed.hashBase64,
                    "salt" to hashed.saltBase64,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "admin_meta/config")
            Result.failure(e)
        }
    }

    // --- Admin Content & Moderation Operations ---

    suspend fun saveVipTrade(
        existingId: String?,
        title: String,
        body: String,
        pair: String,
        direction: TradeDirection,
        entryPrice: String,
        stopLoss: String,
        takeProfit: String,
        imageUrl: String?
    ): Result<String> {
        val uid = requireUserId()
        val cleanTitle = title.trim().take(140)
        val cleanBody = body.trim().take(4000)
        if (cleanTitle.isBlank() || cleanBody.isBlank()) {
            return Result.failure(IllegalArgumentException("Title and signal details are required"))
        }
        val cleanImage = imageUrl?.trim()?.takeIf { it.isNotBlank() }?.take(1000)
        return try {
            if (existingId.isNullOrBlank()) {
                val docRef = db.collection("vip_trades").document()
                val payload = mapOf(
                    "title" to cleanTitle,
                    "body" to cleanBody,
                    "pair" to pair.trim().take(24),
                    "direction" to direction.name,
                    "entryPrice" to entryPrice.trim().take(32),
                    "stopLoss" to stopLoss.trim().take(32),
                    "takeProfit" to takeProfit.trim().take(32),
                    "imageUrl" to cleanImage,
                    "authorId" to uid,
                    "reactionFire" to 0L,
                    "reactionRocket" to 0L,
                    "reactionHundred" to 0L,
                    "reactionBull" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                docRef.set(payload).await()
                createAnnouncementInternal(
                    title = "VIP Signal: $cleanTitle",
                    message = "${pair.trim()} ${direction.label} — Entry ${entryPrice.trim()}".trim(),
                    category = NotificationCategory.VIP_TRADES.key,
                    targetId = docRef.id,
                    imageUrl = cleanImage,
                    authorId = uid
                )
                Result.success(docRef.id)
            } else {
                val docRef = db.collection("vip_trades").document(existingId)
                val updateMap = mutableMapOf<String, Any>(
                    "title" to cleanTitle,
                    "body" to cleanBody,
                    "pair" to pair.trim().take(24),
                    "direction" to direction.name,
                    "entryPrice" to entryPrice.trim().take(32),
                    "stopLoss" to stopLoss.trim().take(32),
                    "takeProfit" to takeProfit.trim().take(32),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanImage != null) {
                    updateMap["imageUrl"] = cleanImage
                } else {
                    updateMap["imageUrl"] = FieldValue.delete()
                }
                docRef.update(updateMap).await()
                Result.success(existingId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "vip_trades")
            Result.failure(e)
        }
    }

    suspend fun deleteVipTrade(tradeId: String): Result<Unit> {
        requireUserId()
        return try {
            db.collection("vip_trades").document(tradeId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "vip_trades/$tradeId")
            Result.failure(e)
        }
    }

    suspend fun saveMarketAnalysis(
        existingId: String?,
        title: String,
        pair: String,
        timeframe: String,
        summary: String,
        supportLevels: List<String>,
        resistanceLevels: List<String>,
        imageUrl: String?
    ): Result<String> {
        val uid = requireUserId()
        val cleanTitle = title.trim().take(160)
        val cleanSummary = summary.trim().take(4000)
        val cleanPair = pair.trim().take(24)
        val cleanTf = timeframe.trim().ifBlank { "15M" }.take(16)
        if (cleanTitle.isBlank() || cleanSummary.isBlank() || cleanPair.isBlank()) {
            return Result.failure(IllegalArgumentException("Title, pair, and analysis summary are required"))
        }
        val cleanImage = imageUrl?.trim()?.takeIf { it.isNotBlank() }?.take(1000)
        val supports = supportLevels.map { it.trim() }.filter { it.isNotBlank() }.take(10)
        val resistances = resistanceLevels.map { it.trim() }.filter { it.isNotBlank() }.take(10)

        return try {
            if (existingId.isNullOrBlank()) {
                val docRef = db.collection("market_analyses").document()
                val payload = mapOf(
                    "title" to cleanTitle,
                    "pair" to cleanPair,
                    "timeframe" to cleanTf,
                    "summary" to cleanSummary,
                    "supportLevels" to supports,
                    "resistanceLevels" to resistances,
                    "imageUrl" to cleanImage,
                    "authorId" to uid,
                    "reactionFire" to 0L,
                    "reactionRocket" to 0L,
                    "reactionHundred" to 0L,
                    "reactionBull" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                docRef.set(payload).await()
                createAnnouncementInternal(
                    title = "Market Analysis: $cleanTitle",
                    message = "$cleanPair ($cleanTf) chart outlook & key levels published.",
                    category = NotificationCategory.MARKET_ANALYSIS.key,
                    targetId = docRef.id,
                    imageUrl = cleanImage,
                    authorId = uid
                )
                Result.success(docRef.id)
            } else {
                val docRef = db.collection("market_analyses").document(existingId)
                val updateMap = mutableMapOf<String, Any>(
                    "title" to cleanTitle,
                    "pair" to cleanPair,
                    "timeframe" to cleanTf,
                    "summary" to cleanSummary,
                    "supportLevels" to supports,
                    "resistanceLevels" to resistances,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanImage != null) {
                    updateMap["imageUrl"] = cleanImage
                } else {
                    updateMap["imageUrl"] = FieldValue.delete()
                }
                docRef.update(updateMap).await()
                Result.success(existingId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "market_analyses")
            Result.failure(e)
        }
    }

    suspend fun deleteMarketAnalysis(analysisId: String): Result<Unit> {
        requireUserId()
        return try {
            db.collection("market_analyses").document(analysisId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "market_analyses/$analysisId")
            Result.failure(e)
        }
    }

    private suspend fun createAnnouncementInternal(
        title: String,
        message: String,
        category: String,
        targetId: String?,
        imageUrl: String?,
        authorId: String
    ) {
        val docRef = db.collection("announcements").document()
        val payload = mapOf(
            "title" to title.trim().take(140),
            "message" to message.trim().take(1000),
            "category" to category,
            "targetId" to targetId?.takeIf { it.isNotBlank() }?.take(128),
            "imageUrl" to imageUrl?.takeIf { it.isNotBlank() }?.take(1000),
            "authorId" to authorId,
            "createdAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
        docRef.set(payload).await()
    }

    suspend fun sendManualPushAnnouncement(
        title: String,
        message: String,
        imageUrl: String?
    ): Result<Unit> {
        val uid = requireUserId()
        if (title.isBlank() || message.isBlank()) {
            return Result.failure(IllegalArgumentException("Title and message are required"))
        }
        return try {
            createAnnouncementInternal(
                title = title,
                message = message,
                category = NotificationCategory.ANNOUNCEMENTS.key,
                targetId = null,
                imageUrl = imageUrl,
                authorId = uid
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "announcements")
            Result.failure(e)
        }
    }

    fun observeAllUsersForAdmin(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .handleStreamErrors(OperationType.LIST, "users", uid)
        )
    }

    suspend fun setUserSuspended(targetUserId: String, suspended: Boolean): Result<Unit> {
        requireUserId()
        return try {
            db.collection("users").document(targetUserId).update(
                mapOf(
                    "suspended" to suspended,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$targetUserId")
            Result.failure(e)
        }
    }

    suspend fun deleteUserAccountByAdmin(targetUserId: String): Result<Unit> {
        requireUserId()
        return try {
            db.collection("users").document(targetUserId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/$targetUserId")
            Result.failure(e)
        }
    }

    fun observeAllReportsForAdmin(): Flow<List<IssueReport>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("issue_reports")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        IssueReport::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .handleStreamErrors(OperationType.LIST, "issue_reports", uid)
        )
    }

    suspend fun resolveIssueReport(reportId: String, adminNote: String): Result<Unit> {
        requireUserId()
        return try {
            db.collection("issue_reports").document(reportId).update(
                mapOf(
                    "status" to "resolved",
                    "adminNote" to adminNote.trim().take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "issue_reports/$reportId")
            Result.failure(e)
        }
    }

    suspend fun saveAppContent(docId: String, title: String, body: String): Result<Unit> {
        val uid = requireUserId()
        if (docId !in listOf("help_faqs", "terms", "privacy")) {
            return Result.failure(IllegalArgumentException("Invalid content key"))
        }
        return try {
            db.collection("app_content").document(docId).set(
                mapOf(
                    "title" to title.trim().take(120),
                    "body" to body.trim().take(15000),
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "app_content/$docId")
            Result.failure(e)
        }
    }

    suspend fun seedStarterContentIfEmpty(): Result<Unit> {
        val uid = requireUserId()
        return try {
            val tradesSnap = db.collection("vip_trades").limit(1).get().await()
            if (tradesSnap.isEmpty) {
                val t1 = db.collection("vip_trades").document()
                t1.set(
                    mapOf(
                        "title" to "EUR/USD London Breakout Long",
                        "body" to "Price swept Asian session lows and reclaimed the 15M bullish order block with strong delta volume. Targeting prior weekly high liquidity.",
                        "pair" to "EUR/USD",
                        "direction" to "LONG",
                        "entryPrice" to "1.0842",
                        "stopLoss" to "1.0815",
                        "takeProfit" to "1.0915",
                        "imageUrl" to "local:img_chart_eurusd",
                        "authorId" to uid,
                        "reactionFire" to 18L,
                        "reactionRocket" to 12L,
                        "reactionHundred" to 9L,
                        "reactionBull" to 14L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                val t2 = db.collection("vip_trades").document()
                t2.set(
                    mapOf(
                        "title" to "XAU/USD NY Session Momentum",
                        "body" to "Gold rejected the 4H supply zone after CPI volatility. Looking for continuation toward the H4 imbalance below 2,645.",
                        "pair" to "XAU/USD",
                        "direction" to "SHORT",
                        "entryPrice" to "2674.50",
                        "stopLoss" to "2686.00",
                        "takeProfit" to "2645.00",
                        "imageUrl" to "local:img_chart_xauusd",
                        "authorId" to uid,
                        "reactionFire" to 24L,
                        "reactionRocket" to 15L,
                        "reactionHundred" to 11L,
                        "reactionBull" to 4L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val analysisSnap = db.collection("market_analyses").limit(1).get().await()
            if (analysisSnap.isEmpty) {
                val a1 = db.collection("market_analyses").document()
                a1.set(
                    mapOf(
                        "title" to "EUR/USD 15-Min Chart Analysis",
                        "pair" to "EUR/USD",
                        "timeframe" to "15M",
                        "summary" to "EUR/USD continues respecting an ascending intraday trendline. Watch for a clean 15-minute candle close above 1.0865 to confirm continuation toward 1.0920. Invalidated below 1.0805.",
                        "supportLevels" to listOf("1.0835", "1.0815", "1.0790"),
                        "resistanceLevels" to listOf("1.0865", "1.0895", "1.0920"),
                        "imageUrl" to "local:img_chart_eurusd",
                        "authorId" to uid,
                        "reactionFire" to 16L,
                        "reactionRocket" to 11L,
                        "reactionHundred" to 8L,
                        "reactionBull" to 13L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                val a2 = db.collection("market_analyses").document()
                a2.set(
                    mapOf(
                        "title" to "XAU/USD 4H Structural Outlook",
                        "pair" to "XAU/USD",
                        "timeframe" to "4H",
                        "summary" to "Gold is consolidating inside a tight high-timeframe range ahead of major macro releases. Key demand rests at 2,642 while institutional sellers defend 2,688.",
                        "supportLevels" to listOf("2,642.00", "2,625.00", "2,598.00"),
                        "resistanceLevels" to listOf("2,688.00", "2,705.00", "2,728.00"),
                        "imageUrl" to "local:img_chart_xauusd",
                        "authorId" to uid,
                        "reactionFire" to 21L,
                        "reactionRocket" to 14L,
                        "reactionHundred" to 10L,
                        "reactionBull" to 19L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "seed")
            Result.failure(e)
        }
    }
}
