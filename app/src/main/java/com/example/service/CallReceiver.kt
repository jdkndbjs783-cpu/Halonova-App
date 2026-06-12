package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.example.HelloNovaApplication
import com.example.data.CommunicationEventEntity
import com.example.data.ServiceLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class CallReceiver : BroadcastReceiver() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""
            Log.d("CallReceiver", "Phone state changed: $state, incoming number: $incomingNumber")

            val app = context.applicationContext as? HelloNovaApplication
            val repo = app?.repository

            scope.launch {
                try {
                    // Resolve contact name from database if possible
                    val contactName = if (incomingNumber.isNotEmpty() && repo != null) {
                        val matching = repo.searchContacts(incomingNumber)
                        if (matching.isNotEmpty()) {
                            matching.first().displayName
                        } else {
                            getContactName(incomingNumber)
                        }
                    } else {
                        getContactName(incomingNumber)
                    }

                    when (state) {
                        TelephonyManager.EXTRA_STATE_RINGING -> {
                            activeCallState.value = "RINGING"
                            activeCallerName.value = contactName
                            activeCallerNumber.value = incomingNumber
                            wasRinging = true
                            wasAnswered = false
                            ringingCaller = contactName
                            ringingNumber = incomingNumber
                            handleIncomingCall(context, contactName, incomingNumber)
                        }
                        TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                            activeCallState.value = "ACTIVE"
                            wasAnswered = true
                            if (incomingNumber.isNotEmpty()) {
                                activeCallerName.value = contactName
                                activeCallerNumber.value = incomingNumber
                            }
                        }
                        TelephonyManager.EXTRA_STATE_IDLE -> {
                            activeCallState.value = "DISCONNECTED"
                            if (wasRinging && !wasAnswered) {
                                val missedCaller = ringingCaller
                                val missedNum = ringingNumber
                                Log.d("CallReceiver", "Missed call detected from $missedCaller ($missedNum)")
                                scope.launch {
                                    try {
                                        repo?.insertCommunicationEvent(
                                            CommunicationEventEntity(
                                                type = "CALL",
                                                senderOrApp = missedCaller,
                                                title = "MISSED_CALL",
                                                details = if (missedNum.isNotEmpty()) "Missed call from $missedCaller ($missedNum)" else "Missed call from secure terminal",
                                                timestamp = System.currentTimeMillis()
                                            )
                                        )
                                        repo?.insertServiceLog(
                                            ServiceLogEntity(
                                                eventType = "Call Event",
                                                message = "COMM_LINK // Missed voice transmission: missed call from [$missedCaller]"
                                            )
                                        )
                                        val phrase = "Sir, you missed a call from $missedCaller."
                                        AppVoiceAnnouncer.announce(phrase)
                                    } catch (e: Exception) {
                                        Log.e("CallReceiver", "Failed to save missed call: ${e.message}")
                                    }
                                }
                            }
                            wasRinging = false
                            wasAnswered = false
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CallReceiver", "Error while processing phone state", e)
                }
            }
        }
    }

    private fun handleIncomingCall(context: Context, contactName: String, number: String) {
        val app = context.applicationContext as? HelloNovaApplication ?: return
        val repo = app.repository
        
        scope.launch {
            try {
                // 1. Insert Call Event into Room database
                val callEvent = CommunicationEventEntity(
                    type = "CALL",
                    senderOrApp = contactName,
                    title = "INCOMING_CALL",
                    details = if (number.isNotEmpty()) "Line Terminal Active (Num: $number)" else "Line Terminal Active (Secure Node)",
                    timestamp = System.currentTimeMillis()
                )
                repo.insertCommunicationEvent(callEvent)

                // 2. Insert to Service log event table
                repo.insertServiceLog(
                    ServiceLogEntity(
                        eventType = "Call Event",
                        message = "COMM_LINK // Incoming voice transmission: connection from secure node [$contactName]"
                    )
                )
            } catch (e: Exception) {
                Log.e("CallReceiver", "Failed to handle incoming call logging: ${e.message}")
            }
        }
    }

    companion object {
        val activeCallState = MutableStateFlow("STANDBY")
        val activeCallerName = MutableStateFlow<String?>(null)
        val activeCallerNumber = MutableStateFlow<String?>(null)

        var wasRinging = false
        var wasAnswered = false
        var ringingCaller = "Unknown"
        var ringingNumber = ""

        fun getContactName(number: String): String {
            if (number.trim().isEmpty()) return "Secure Quantum Node"
            val clean = number.trim()
            return when {
                clean.contains("5550199") || clean.contains("jarvis") -> "Chief Operator Jarvis"
                clean.contains("5550100") || clean.contains("zero") -> "Command Center Zero"
                clean.contains("5550102") || clean.contains("kane") -> "Commander Kane"
                clean.contains("111") || clean.contains("nova") -> "Nova Network Uplink"
                else -> "Unidentified Agent ($clean)"
            }
        }
    }
}
