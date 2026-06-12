package com.example.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AppVoiceAnnouncer {
    private val _announceFlow = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val announceFlow = _announceFlow.asSharedFlow()

    // Shared flow specifically for incoming state triggers
    val announcedSmsState = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 8) // Pair(senderName, messageText)
    val announcedWhatsAppState = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 8) // Pair(senderName, messageText)

    fun announce(massage: String) {
        _announceFlow.tryEmit(massage)
    }
}
