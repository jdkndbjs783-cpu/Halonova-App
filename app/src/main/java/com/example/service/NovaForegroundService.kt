package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.HelloNovaApplication
import com.example.MainActivity
import com.example.data.ServiceLogEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NovaForegroundService : Service() {

    enum class EngineState {
        OFFLINE,
        STARTING,
        ACTIVE
    }

    companion object {
        private const val TAG = "NovaForegroundService"
        const val CHANNEL_ID = "nova_engine_channel"
        const val NOTIFICATION_ID = 8808

        private val _engineState = MutableStateFlow(EngineState.OFFLINE)
        val engineState = _engineState.asStateFlow()
        
        fun updateState(state: EngineState) {
            _engineState.value = state
        }

        private val _notificationUptimeSeconds = MutableStateFlow(0L)
        val notificationUptimeSeconds = _notificationUptimeSeconds.asStateFlow()

        private val _notificationStatus = MutableStateFlow("ACTIVE")
        val notificationStatus = _notificationStatus.asStateFlow()

        private val _recoveryEventsCount = MutableStateFlow(0)
        val recoveryEventsCount = _recoveryEventsCount.asStateFlow()

        private val _serviceUptimeSeconds = MutableStateFlow(0L)
        val serviceUptimeSeconds = _serviceUptimeSeconds.asStateFlow()

        private val _batteryOptimizationStatus = MutableStateFlow("OPTIMIZED")
        val batteryOptimizationStatus = _batteryOptimizationStatus.asStateFlow()

        private val _systemHealthScore = MutableStateFlow(100)
        val systemHealthScore = _systemHealthScore.asStateFlow()

        fun incrementRecoveryEvents() {
            _recoveryEventsCount.value += 1
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var wakeWordEngine: BackgroundWakeWordEngine? = null
    private var stabilityCheckJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Service starting...")
        _engineState.value = EngineState.STARTING
        
        createNotificationChannel()
        val notification = createNotification()
        
        // Start foreground
        startForeground(NOTIFICATION_ID, notification)
        
        _engineState.value = EngineState.ACTIVE
        
        // Initialize background wake engine
        wakeWordEngine = BackgroundWakeWordEngine(this, serviceScope).apply {
            startEngine()
        }
        
        // Log start event to Room
        serviceScope.launch {
            try {
                val repo = (application as HelloNovaApplication).repository
                repo.insertServiceLog(
                    ServiceLogEntity(
                        eventType = "Service Started",
                        message = "Nova Engine active & persistent notification bound."
                    )
                )
                // Update persistent state in Room
                val currentPrefs = repo.getPreferences()
                repo.savePreferences(currentPrefs.copy(engineActive = true))
            } catch (e: Exception) {
                Log.e(TAG, "Error writing started log or preferences: ${e.message}")
            }
        }

        // Start stability monitoring loop
        val serviceStartTime = System.currentTimeMillis()
        _notificationStatus.value = "ACTIVE"
        _notificationUptimeSeconds.value = 0L
        _serviceUptimeSeconds.value = 0L

        stabilityCheckJob = serviceScope.launch {
            while (isActive) {
                delay(10000) // check system stability every 10 seconds
                try {
                    // Update uptime counters
                    val elapsed = (System.currentTimeMillis() - serviceStartTime) / 1000
                    _serviceUptimeSeconds.value = elapsed
                    _notificationUptimeSeconds.value = elapsed

                    // Notification check
                    val nManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    var isNotificationActive = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            isNotificationActive = nManager.activeNotifications.any { it.id == NOTIFICATION_ID }
                        } catch (e: Exception) {
                            isNotificationActive = true // fallback if security restriction prevents checking
                        }
                    } else {
                        isNotificationActive = true
                    }

                    if (!isNotificationActive) {
                        _notificationStatus.value = "RECREATED"
                        _recoveryEventsCount.value += 1
                        
                        // Recreate foreground or standard notification
                        try {
                            startForeground(NOTIFICATION_ID, createNotification())
                            val repo = (application as? HelloNovaApplication)?.repository
                            repo?.insertServiceLog(
                                ServiceLogEntity(
                                    eventType = "Notification Recovered",
                                    message = "Core protection detected missing notification. Successfully restored."
                                )
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error soft-recovering notification: ${e.message}")
                        }
                    } else {
                        _notificationStatus.value = "ACTIVE"
                    }

                    // Wake word engine check
                    val repo = (application as? HelloNovaApplication)?.repository
                    if (repo != null) {
                        val prefs = repo.getPreferences()
                        if (prefs.engineActive) {
                            if (wakeWordEngine == null || BackgroundWakeWordEngine.status.value == WakeStatus.DISABLED) {
                                _recoveryEventsCount.value += 1
                                launch(Dispatchers.Main) {
                                    if (wakeWordEngine == null) {
                                        wakeWordEngine = BackgroundWakeWordEngine(this@NovaForegroundService, serviceScope)
                                    }
                                    wakeWordEngine?.startEngine()
                                }
                                repo.insertServiceLog(
                                    ServiceLogEntity(
                                        eventType = "Wake Engine Reset",
                                        message = "System Recovery: Restored wake phrase engine listener."
                                    )
                                )
                            }
                        }
                    }

                    // Battery constraints status
                    val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
                    val isIgnoring = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        pm?.isIgnoringBatteryOptimizations(packageName) ?: true
                    } else true
                    _batteryOptimizationStatus.value = if (isIgnoring) "UNRESTRICTED" else "OPTIMIZED (RESTRICTED)"

                    // Compute health score
                    var score = 0
                    val micPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                        this@NovaForegroundService,
                        android.Manifest.permission.RECORD_AUDIO
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    
                    if (micPermission) score += 25
                    if (engineState.value == EngineState.ACTIVE) score += 25
                    if (_notificationStatus.value == "ACTIVE") score += 25
                    if (isIgnoring) score += 25
                    _systemHealthScore.value = score

                } catch (e: Exception) {
                    Log.e(TAG, "Error in stability monitor: ${e.message}")
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: Service running")
        // Return sticky so service is kept alive
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: Service stopping")
        _engineState.value = EngineState.OFFLINE
        
        stabilityCheckJob?.cancel()
        stabilityCheckJob = null

        wakeWordEngine?.stopEngine()
        wakeWordEngine = null
        
        // Log stop event to Room
        serviceScope.launch {
            try {
                val repo = (application as HelloNovaApplication).repository
                repo.insertServiceLog(
                    ServiceLogEntity(
                        eventType = "Service Stopped",
                        message = "Nova Engine safely decoupled."
                    )
                )
                val currentPrefs = repo.getPreferences()
                repo.savePreferences(currentPrefs.copy(engineActive = false))
            } catch (e: Exception) {
                Log.e(TAG, "Error writing stopped log or preferences: ${e.message}")
            }
        }
        
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Hello Nova Engine"
            val descriptionText = "Persistent sync notification for Nova Quantum Engine"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Hello Nova")
            .setContentText("Nova Engine Active")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
