package com.example.service

enum class SmartActionType(val displayName: String, val voiceConfirmation: String) {
    OPEN_SETTINGS("Open Settings", "Opening Settings"),
    OPEN_WIFI("Open Wi-Fi Settings", "Opening Wi-Fi Settings"),
    OPEN_BLUETOOTH("Open Bluetooth Settings", "Opening Bluetooth Settings"),
    OPEN_APP_INFO("Open App Info", "Opening App Info"),
    OPEN_BATTERY("Open Battery Settings", "Opening Battery Settings"),
    OPEN_LOCATION("Open Location Settings", "Opening Location Settings"),
    GO_HOME("Go Home", "Returning to Hello Nova dashboard"),
    CHECK_NOTIFICATIONS("Check Notifications", ""),
    ANSWER_CALL("Answer Call", "Answering transmissions"),
    DECLINE_CALL("Decline Call", "Declining incoming transmission")
}
