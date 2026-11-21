package com.example.notifications

import android.util.Log
import com.example.data.MarketRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MarketChartMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed")
        if (FirebaseAuth.getInstance().currentUser != null) {
            serviceScope.launch {
                val repo = MarketRepository(applicationContext)
                repo.addFcmToken(token)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val title = message.notification?.title ?: data["title"] ?: "Market Chart Zone"
        val body = message.notification?.body ?: data["body"] ?: "New market update available."
        val category = data["category"] ?: "announcements"
        val targetId = data["targetId"]

        NotificationHelper.showHighPriorityNotification(
            context = applicationContext,
            title = title,
            body = body,
            category = category,
            targetId = targetId
        )
    }

    private companion object {
        const val TAG = "MarketChartFCM"
    }
}
