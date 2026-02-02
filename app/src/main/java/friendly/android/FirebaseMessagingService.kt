package friendly.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.BitmapFactory
import androidx.annotation.WorkerThread
import com.google.firebase.messaging.RemoteMessage
import friendly.sdk.FriendlyClient
import friendly.sdk.NotificationDetails
import friendly.sdk.NotificationDetails.NewRequest
import friendly.sdk.NotificationDetailsSerializable
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import kotlinx.serialization.json.Json
import java.net.URL

class FirebaseMessagingService :
    com.google.firebase.messaging.FirebaseMessagingService() {

    private val client = FriendlyClient.production(
        HttpClient(CIO) {
            install(Logging) {
                logger = Logger.ANDROID
                level = LogLevel.ALL
            }
        },
    )

    override fun onNewToken(token: String) {
        FirebaseKit.onNewToken()
    }

    private val notificationManager: NotificationManager
        get() = getSystemService(
            Context.NOTIFICATION_SERVICE,
        ) as NotificationManager

    @WorkerThread
    override fun onMessageReceived(message: RemoteMessage) {
        if (!notificationManager.areNotificationsEnabled()) return
        val details = message.data.getValue("details")
        val notification = decodeNotification(message)
        when (notification) {
            is NewRequest -> showNewRequest(notification)
        }
    }

    private fun decodeNotification(
        message: RemoteMessage,
    ): NotificationDetails {
        val details = message.data.getValue("details")
        val notification: NotificationDetailsSerializable =
            Json.decodeFromString(details)
        return notification.typed()
    }

    private fun showNewRequest(notification: NewRequest) {
        val channelId = notificationManager.ensureNewRequestChannelId()
        val title = if (notification.isMutual) {
            R.string.notification_mutual_request_title
        } else {
            R.string.notification_request_title
        }
        val text = if (notification.isMutual) {
            R.string.notification_mutual_request_text
        } else {
            R.string.notification_request_text
        }
        val nickname = notification.from.nickname.string
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = Notification.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(title, nickname))
            .setContentText(getString(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        val avatar = notification.from.avatar
        if (avatar != null) {
            val url = client.files
                .getEndpoint(avatar)
                .string.let(::URL)
            runCatching {
                val bitmap = BitmapFactory
                    .decodeStream(url.openConnection().getInputStream())
                    .getCircledBitmap()
                builder.setLargeIcon(bitmap)
            }
        }
        val notificationId = NotificationsKit.getNextId()
        notificationManager.notify(notificationId, builder.build())
    }

    private fun NotificationManager.ensureNewRequestChannelId(): String {
        val id = "new-request-channel"
        val name = getString(R.string.notification_request_channel_name)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(id, name, importance)
        createNotificationChannel(channel)
        return id
    }
}
