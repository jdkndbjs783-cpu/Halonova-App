package com.example.service

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat

data class PermissionStatusReport(
    val microphoneGranted: Boolean = false,
    val cameraGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
    val contactsGranted: Boolean = false,
    val phoneGranted: Boolean = false,
    val smsGranted: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val overlayGranted: Boolean = false,
    val batteryOptimizationsUnrestricted: Boolean = false,
    val autoStartSupported: Boolean = false,
    val autoStartGranted: Boolean = false,
    val bluetoothGranted: Boolean = false,
    val securityScore: Int = 0,
    val healthStatus: String = "CRITICAL", // OPTIMAL, WARNING, CRITICAL
    val emergencyProtectionMode: Boolean = false,
    val missingPermissionsCount: Int = 0,
    val activePermissionsCount: Int = 0
)

class PermissionIntelligenceEngine(private val context: Context) {

    fun generateSecurityReport(): PermissionStatusReport {
        val micro = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val cam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        
        val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Automatic post-notification privileges on legacy devices
        }
        
        val contacts = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val phone = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val sms = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        
        val blue = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }

        // Accessibility Check
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val isAccessEnabled = am?.run {
            isEnabled || getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).isNotEmpty()
        } ?: false

        // Overlay Permission Check
        val canDraw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }

        // Battery Optimization
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isIgnoringBattery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        } else {
            true
        }

        // Auto Start
        val autoStartIntent = getAutoStartIntent(context)
        val isAutoStartSupported = autoStartIntent != null
        // Since we can't query the real state programmatically, we treat it as warning/false if system supports it but user needs to configure it customly, or set to true by default if not supported.
        val isAutoStartConfigured = !isAutoStartSupported || context.getSharedPreferences("nova_stability_prefs", Context.MODE_PRIVATE).getBoolean("auto_start_configured", false)

        // Tally up score and count
        var score = 0
        var activeCount = 0
        var missingCount = 0

        // Weights: 10 points for each of the 10 major security channels
        if (micro) { score += 10; activeCount++ } else { missingCount++ }
        if (canDraw) { score += 10; activeCount++ } else { missingCount++ }
        if (cam) { score += 10; activeCount++ } else { missingCount++ }
        if (notif) { score += 10; activeCount++ } else { missingCount++ }
        if (contacts) { score += 10; activeCount++ } else { missingCount++ }
        if (phone) { score += 10; activeCount++ } else { missingCount++ }
        if (sms) { score += 10; activeCount++ } else { missingCount++ }
        if (isAccessEnabled) { score += 10; activeCount++ } else { missingCount++ }
        if (isIgnoringBattery) { score += 10; activeCount++ } else { missingCount++ }
        if (blue) { score += 10; activeCount++ } else { missingCount++ }

        // Nova Emergency Protection Level criteria:
        // Enter Emergency Protection mode if highly critical operations are completely disabled:
        // microphone, overlay, or notifications.
        val emergency = !micro || !canDraw || !notif

        val health = when {
            score >= 90 -> "OPTIMAL"
            score >= 60 -> "WARNING"
            else -> "CRITICAL"
        }

        return PermissionStatusReport(
            microphoneGranted = micro,
            cameraGranted = cam,
            notificationsGranted = notif,
            contactsGranted = contacts,
            phoneGranted = phone,
            smsGranted = sms,
            accessibilityEnabled = isAccessEnabled,
            overlayGranted = canDraw,
            batteryOptimizationsUnrestricted = isIgnoringBattery,
            autoStartSupported = isAutoStartSupported,
            autoStartGranted = isAutoStartConfigured,
            bluetoothGranted = blue,
            securityScore = score,
            healthStatus = health,
            emergencyProtectionMode = emergency,
            missingPermissionsCount = missingCount,
            activePermissionsCount = activeCount
        )
    }

    fun getRecoveryIntent(permissionKey: String): Intent? {
        return try {
            when (permissionKey) {
                "MICROPHONE", "CAMERA", "CONTACTS", "PHONE", "SMS", "NOTIFICATIONS", "BLUETOOTH" -> {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }
                "OVERLAY" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    } else null
                }
                "BATTERY" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    } else null
                }
                "ACCESSIBILITY" -> {
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }
                "AUTOSTART" -> {
                    val intent = getAutoStartIntent(context)
                    if (intent != null) {
                        // Mark as configured when they trigger it
                        context.getSharedPreferences("nova_stability_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("auto_start_configured", true)
                            .apply()
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        intent
                    } else {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            // Fallback setting details
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }

    private fun getAutoStartIntent(context: Context): Intent? {
        val intents = listOf(
            Intent().setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            Intent().setClassName("com.asus.mobilemanager", "com.asus.mobilemanager.autostart.AutoStartActivity"),
            Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
            Intent().setClassName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
            Intent().setClassName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
            Intent().setClassName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            Intent().setClassName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")
        )
        for (intent in intents) {
            try {
                if (context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()) {
                    return intent
                }
            } catch (e: Exception) {
                // ignore query errors
            }
        }
        return null
    }
}
