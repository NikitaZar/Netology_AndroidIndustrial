package ru.netology.nmedia.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.auth.AppAuth
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class FCMService : FirebaseMessagingService() {
    private val action = "action"
    private val content = "content"
    private val channelId = "remote"
    private val recipientId = "recipientId"
    private val gson = Gson()

    @Inject
    lateinit var auth: AppAuth

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_remote_name)
            val descriptionText = getString(R.string.channel_remote_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val pushString = gson.fromJson(message.data[content], PushString::class.java)
        Log.i("recipientId", "recipientId = ${pushString.recipientId}/ userId = ${auth.authStateFlow.value.id}")
        recipientIdCheck(pushString.recipientId) {
//            message.data[action]?.let {
//                when (Action.valueOf(it)) {
//                    Action.LIKE -> handleLike(
//                        gson.fromJson(
//                            message.data[content],
//                            Like::class.java
//                        )
//                    )
//                }
//            }
            message.data[content]?.let { handleString(pushString.content) }
        }
    }

    override fun onNewToken(token: String) {
        auth.sendPushToken(token)
        Log.i("token", token)
    }

    private fun handleLike(content: Like) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                getString(
                    R.string.notification_user_liked,
                    content.userName,
                    content.postAuthor,
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(this)
            .notify(Random.nextInt(100_000), notification)
    }

    private fun handleString(content: String) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(this)
            .notify(Random.nextInt(100_000), notification)
    }

    private fun recipientIdCheck(recipientId: Long?, notify: () -> Unit) {
        val id = auth.authStateFlow.value.id
        when {
            recipientId == id || recipientId == null -> {
                notify()
            }
            recipientId == 0L || (recipientId != id && recipientId != 0L) -> {
                auth.sendPushToken()
            }
            else -> return
        }
    }
}

enum class Action {
    LIKE,
}

data class Like(
    val userId: Long,
    val userName: String,
    val postId: Long,
    val postAuthor: String,
)

data class PushString(
    val recipientId: Long?,
    val content: String,
)

