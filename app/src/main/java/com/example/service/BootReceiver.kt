package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver", "onReceive: ${intent.action}")
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            try {
                val serviceIntent = Intent(context, NovaForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                Log.d("BootReceiver", "BootReceiver successfully completed and started NovaForegroundService.")
            } catch (e: Exception) {
                Log.e("BootReceiver", "Error starting NovaForegroundService from boot receiver: ${e.message}")
            }
        }
    }
}
