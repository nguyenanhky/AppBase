package com.viettel.appbase.core.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.viettel.appbase.MainActivity
import com.viettel.appbase.R
import com.viettel.appbase.core.storage.AppDataStore
import com.viettel.appbase.core.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber

class AppFirebaseMessagingService : FirebaseMessagingService() {
    private val appDataStore: AppDataStore by inject()
    private val applicationScope: CoroutineScope by inject()

    @Suppress("OVERRIDE_DEPRECATION") // Firebase marks it deprecated, but it is still the token callback.
    override fun onNewToken(token: String) {
        Timber.d("FCM token refreshed")
        // Also send the token to your backend here once the product targets individual users.
        applicationScope.launch { appDataStore.saveFcmToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["message"].orEmpty()
        if (body.isBlank()) return

        val notificationId = message.messageId?.hashCode() ?: System.currentTimeMillis().toInt()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            // Only our own scheme is accepted, e.g. data payload {"deep_link": "appbase://home"}.
            message.data[Constants.PUSH_DEEP_LINK_KEY]
                ?.takeIf { it.startsWith("${Constants.DEEP_LINK_SCHEME}://") }
                ?.let {
                    action = Intent.ACTION_VIEW
                    data = it.toUri()
                }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val canNotify = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (canNotify) {
            NotificationManagerCompat.from(this).notify(notificationId, notification)
        } else {
            Timber.d("Notification permission not granted")
        }
    }
}
