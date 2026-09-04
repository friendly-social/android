package friendly.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.WorkerThread
import com.google.firebase.messaging.RemoteMessage
import friendly.sdk.Authorization
import friendly.sdk.FileDescriptor
import friendly.sdk.FriendlyClient
import friendly.sdk.FriendlyNotificationsClient.DetailsResult
import friendly.sdk.NotificationDetails
import friendly.sdk.NotificationDetails.NewReply
import friendly.sdk.NotificationDetails.NewRequest
import friendly.sdk.NotificationId
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job)

    override fun onNewToken(token: String) {
        FirebaseKit.onNewToken()
    }

    private val notificationManager: NotificationManager
        get() = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    @WorkerThread
    override fun onMessageReceived(message: RemoteMessage) {
        if (!notificationManager.areNotificationsEnabled()) return

        scope.launch {
            val authStorage = AuthStorage(this@FirebaseMessagingService)

            val notification = fetchNotification(
                authorization = authStorage.getAuth(),
                message = message,
            )

            if (notification != null) {
                when (notification) {
                    is NewRequest -> showNewRequest(notification)
                    is NewReply -> showNewReply(notification)
                }
            }
        }
    }

    private suspend fun fetchNotification(
        authorization: Authorization,
        message: RemoteMessage,
    ): NotificationDetails? {
        val long = message.data.getValue("id").toLong()
        val notificationId = NotificationId(long)
        val result = client.notifications.details(authorization, notificationId)
        return when (result) {
            is DetailsResult.IOError, DetailsResult.Unauthorized -> null

            is DetailsResult.ServerError -> {
                Log.e(
                    "FirebaseMessagingService",
                    "Received ServerError: $result",
                )
                null
            }

            is DetailsResult.Success -> {
                result.notification
            }
        }
    }

    private fun showNewReply(notification: NewReply) {
        val channelId = notificationManager.ensureNewRequestChannelId()
        val title = R.string.notification_reply_from_title
        val post = notification.post
        if (post !is Plain) return
        val text = post.text.string
        val nickname = post.owner.nickname
        val avatar = post.owner.avatar
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
            putExtra(
                NotificationDestinationExtra.EXTRAS_KEY,
                NotificationDestinationExtra.Activity.raw(),
            )
        }
        val pendingIntent = PendingIntent
            .getActivity(
                this,
                0,
                intent,
                FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT,
            )
        val builder = Notification.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(title, nickname.string))
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        if (avatar != null) {
            builder.setLargeIcon(fetchAvatarBitmap(avatar))
        }
        val notificationId = NotificationsKit.getNextId()
        notificationManager.notify(notificationId, builder.build())
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
        val destination = when (notification.isMutual) {
            true -> NotificationDestinationExtra.Network
            false -> NotificationDestinationExtra.Feed
        }
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
            putExtra(
                NotificationDestinationExtra.EXTRAS_KEY,
                destination.raw(),
            )
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT,
        )
        val builder = Notification.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(title, nickname))
            .setContentText(getString(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        val avatar = notification.from.avatar
        if (avatar != null) {
            builder.setLargeIcon(fetchAvatarBitmap(avatar))
        }
        val notificationId = NotificationsKit.getNextId()
        notificationManager.notify(notificationId, builder.build())
    }

    private fun fetchAvatarBitmap(avatar: FileDescriptor): Bitmap? {
        val url = client.files
            .getEndpoint(avatar)
            .string.let(::URL)
        runCatching {
            val bitmap = BitmapFactory
                .decodeStream(url.openConnection().getInputStream())
                .getCircledBitmap()
            return bitmap
        }
        return null
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
