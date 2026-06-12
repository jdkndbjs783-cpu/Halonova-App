package com.example.service

import android.content.Context
import com.example.data.UserRepository
import com.example.data.WakeEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WakeEventManager(
    private val context: Context,
    private val repository: UserRepository,
    private val scope: CoroutineScope
) {
    private val _lastWakeEvent = MutableStateFlow<WakeEventEntity?>(null)
    val lastWakeEvent = _lastWakeEvent.asStateFlow()

    private val _engineStartTime = MutableStateFlow(System.currentTimeMillis())
    val engineStartTime = _engineStartTime.asStateFlow()

    init {
        // Fetch the absolute most recent wake event from Room on start to populate UI and support Session Recovery
        scope.launch {
            repository.allWakeEventsFlow.collect { events ->
                if (events.isNotEmpty() && _lastWakeEvent.value == null) {
                    _lastWakeEvent.value = events.first()
                }
            }
        }
    }

    fun getEngineUptimeSeconds(): Long {
        return (System.currentTimeMillis() - _engineStartTime.value) / 1000
    }

    fun resetUptime() {
        _engineStartTime.value = System.currentTimeMillis()
    }

    suspend fun recordWakeEvent(eventName: String, resultStatus: String): WakeEventEntity {
        val event = WakeEventEntity(
            eventName = eventName,
            timestamp = System.currentTimeMillis(),
            resultStatus = resultStatus
        )
        repository.insertWakeEvent(event)
        _lastWakeEvent.value = event
        
        // Also log it in system services
        repository.insertServiceLog(
            com.example.data.ServiceLogEntity(
                eventType = "Wake Event Captured",
                message = "WAKE_EVENT // Event: [$eventName], Result: [$resultStatus]"
            )
        )
        
        return event
    }
}
