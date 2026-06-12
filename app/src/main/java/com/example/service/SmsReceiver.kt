package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.HelloNovaApplication
import com.example.data.CommunicationEventEntity
import com.example.data.ServiceLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        val latestSmsSender = MutableStateFlow<String?>(null)
        val latestSmsContent = MutableStateFlow<String?>(null)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val firstMessage = messages[0]
            val senderNum = firstMessage.originatingAddress ?: "Unknown Sender"
            
            val sb = java.lang.StringBuilder()
            for (msg in messages) {
                sb.append(msg.messageBody)
            }
            val content = sb.toString()

            Log.d("SmsReceiver", "SMS Received from $senderNum: $content")

            val app = context.applicationContext as? HelloNovaApplication
            val repo = app?.repository

            scope.launch {
                try {
                    // Resolve contact from Room database if search exists
                    val contactName = if (repo != null) {
                        val matching = repo.searchContacts(senderNum)
                        if (matching.isNotEmpty()) {
                            matching.first().displayName
                        } else {
                            senderNum
                        }
                    } else {
                        senderNum
                    }

                    latestSmsSender.value = contactName
                    latestSmsContent.value = content

                    if (repo != null) {
                        val smsEvent = CommunicationEventEntity(
                            type = "SMS",
                            senderOrApp = contactName,
                            title = "SMS_RECEIVED",
                            details = content,
                            timestamp = System.currentTimeMillis()
                        )
                        repo.insertCommunicationEvent(smsEvent)

                        repo.insertServiceLog(
                            ServiceLogEntity(
                                eventType = "SMS Status",
                                message = "SMS_MONITOR // Intercepted: $contactName: \"$content\""
                            )
                        )
                    }

                    // Speak: "Sir, you received a new message from [senderName]. It says: [content]"
                    val speakPhrase = "Sir, message received from $contactName. It says, $content"
                    AppVoiceAnnouncer.announce(speakPhrase)
                    AppVoiceAnnouncer.announcedSmsState.tryEmit(Pair(contactName, content))

                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Failed to process SMS broadcast: ${e.message}")
                }
            }
        }
    }
}
