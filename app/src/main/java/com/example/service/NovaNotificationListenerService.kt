package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.HelloNovaApplication
import com.example.data.NotificationEntity
import com.example.data.CommunicationEventEntity
import com.example.data.ServiceLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NovaNotificationListenerService : NotificationListenerService() {

    companion object {
        private var isConnected = false
        fun isServiceConnected(): Boolean {
            return isConnected
        }
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: "System"
        
        // Skip self-notifications from Hello Nova (especially the Foreground Service sticky notification)
        if (packageName == packageNameOrEmpty()) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        if (title.isEmpty() && text.isEmpty()) return

        val appCleanName = cleanPackageName(packageName)

        scope.launch {
            try {
                val repo = (application as HelloNovaApplication).repository
                
                // 1. Save to notification table
                val notification = NotificationEntity(
                    appName = appCleanName,
                    title = title,
                    message = text,
                    timestamp = System.currentTimeMillis()
                )
                repo.insertNotification(notification)

                // 2. Save unified CommunicationEvent
                val commEvent = CommunicationEventEntity(
                    type = "NOTIFICATION",
                    senderOrApp = appCleanName,
                    title = title,
                    details = text,
                    timestamp = System.currentTimeMillis()
                )
                repo.insertCommunicationEvent(commEvent)

                if (appCleanName == "WhatsApp") {
                    AppVoiceAnnouncer.announcedWhatsAppState.tryEmit(Pair(title, text))
                    val phrase = "Sir, $title sent a WhatsApp message. Sir, would you like me to read it?"
                    AppVoiceAnnouncer.announce(phrase)
                }

                // 3. Add to system Service logs
                repo.insertServiceLog(
                    ServiceLogEntity(
                        eventType = "Notification Status",
                        message = "INFRA_NOTIF // Intercepted: $appCleanName: \"$title\""
                    )
                )
            } catch (e: Exception) {
                Log.e("NotifListener", "Failed to persist notification details: ${e.message}")
            }
        }
    }

    private fun packageNameOrEmpty(): String {
        return try {
            packageName
        } catch (e: Exception) {
            "com.example"
        }
    }

    private fun cleanPackageName(pkg: String): String {
        return when {
            pkg.contains("android.gm") -> "Gmail"
            pkg.contains("whatsapp") -> "WhatsApp"
            pkg.contains("slack") -> "Slack"
            pkg.contains("messenger") -> "Messenger"
            pkg.contains("telegram") -> "Telegram"
            pkg.contains("twitter") || pkg.contains("x.android") -> "X (Twitter)"
            pkg.contains("instagram") -> "Instagram"
            pkg.contains("viber") -> "Viber"
            pkg.contains("discord") -> "Discord"
            pkg.contains("skype") -> "Skype"
            else -> pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
