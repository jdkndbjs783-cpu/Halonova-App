package com.example.service

enum class AssistantState(val displayName: String) {
    STANDBY("IDLE"),
    ARMED("ARMED"),
    LISTENING("LISTENING"),
    PROCESSING("PROCESSING"),
    SPEAKING("SPEAKING"),
    RECOVERY("RECOVERY")
}
