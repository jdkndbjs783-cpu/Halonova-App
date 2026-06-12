package com.example.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log

object SmartActionExecutor {

    fun execute(context: Context, actionType: SmartActionType): Boolean {
        return try {
            val intent = when (actionType) {
                SmartActionType.OPEN_SETTINGS -> {
                    Intent(Settings.ACTION_SETTINGS)
                }
                SmartActionType.OPEN_WIFI -> {
                    Intent(Settings.ACTION_WIFI_SETTINGS)
                }
                SmartActionType.OPEN_BLUETOOTH -> {
                    Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                }
                SmartActionType.OPEN_APP_INFO -> {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                }
                SmartActionType.OPEN_BATTERY -> {
                    try {
                        Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
                    } catch (e: Exception) {
                        Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
                    }
                }
                SmartActionType.OPEN_LOCATION -> {
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                }
                SmartActionType.GO_HOME -> {
                    context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                }
                SmartActionType.CHECK_NOTIFICATIONS -> {
                    null
                }
                SmartActionType.ANSWER_CALL -> {
                    null
                }
                SmartActionType.DECLINE_CALL -> {
                    null
                }
            }
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            true
        } catch (e: Exception) {
            Log.e("SmartActionExecutor", "Failed to launch intent for action ${actionType.displayName}: ${e.message}")
            false
        }
    }
}
