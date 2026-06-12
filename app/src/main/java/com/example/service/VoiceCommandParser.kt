package com.example.service

sealed class CommandResult {
    data class ActionCommand(val actionType: SmartActionType) : CommandResult()
    data class SearchContactCommand(val contactName: String) : CommandResult()
    data class CreateDraftCommand(val contactName: String) : CommandResult()
    data class OpenAppCommand(val appName: String) : CommandResult()
    data class OpenBrowserCommand(val url: String) : CommandResult()
    data class ShowRecentContactsCommand(val rawInput: String) : CommandResult()
    data class ChangeLanguageCommand(val language: String) : CommandResult()
    data class ToggleVoiceFeedbackCommand(val enabled: Boolean) : CommandResult()
    data class InitiateCallCommand(val contactName: String) : CommandResult()
    data class SwitchAssistantCommand(val assistant: String) : CommandResult()
    data class ReadWhatsAppCommand(val rawInput: String = "") : CommandResult()
    data class ReadSmsCommand(val rawInput: String = "") : CommandResult()
    data class ReadMissedCallsCommand(val rawInput: String = "") : CommandResult()
    data class UnknownCommand(val rawInput: String) : CommandResult()
    
    // Phase 23 Command Additions
    data class CloseAppCommand(val appName: String) : CommandResult()
    data class SwitchAppCommand(val appName: String) : CommandResult()
    data class LaunchShortcutCommand(val appName: String, val shortcutName: String) : CommandResult()
    data class YouTubeSearchCommand(val searchQuery: String) : CommandResult()
    data class YouTubeControlCommand(val action: String) : CommandResult()
    data class WhatsAppOpenChatCommand(val contactName: String) : CommandResult()
    data class WhatsAppSendMessageCommand(val contactName: String, val message: String) : CommandResult()
    data class WhatsAppSendImageCommand(val contactName: String) : CommandResult()
    data class TakeScreenshotCommand(val rawInput: String = "") : CommandResult()
    data class WhatsAppShareScreenshotCommand(val contactName: String) : CommandResult()
    data class OpenScreenshotGalleryCommand(val rawInput: String = "") : CommandResult()
    data class FileSearchCommand(val query: String, val fileType: String) : CommandResult()
    data class OpenFileCommand(val fileName: String) : CommandResult()
    data class ShowRecentFilesCommand(val rawInput: String = "") : CommandResult()
    data class ExecuteAutomationSequenceCommand(val sequenceName: String) : CommandResult()
}

interface CommandMatcher {
    fun match(input: String): SmartActionType?
}

class PhraseCommandMatcher : CommandMatcher {
    override fun match(input: String): SmartActionType? {
        val lower = input.lowercase().trim()
        
        return when {
            // "Open Settings"
            lower.contains("open settings") || lower == "settings" -> SmartActionType.OPEN_SETTINGS
            
            // "Open Wi-Fi"
            lower.contains("open wifi") || lower.contains("open wi-fi") || lower.contains("wifi settings") || lower.contains("wi-fi settings") -> SmartActionType.OPEN_WIFI
            
            // "Open Bluetooth"
            lower.contains("open bluetooth") || lower.contains("bluetooth settings") -> SmartActionType.OPEN_BLUETOOTH
            
            // "Open Battery Settings"
            lower.contains("open battery") || lower.contains("battery settings") || lower.contains("power settings") -> SmartActionType.OPEN_BATTERY
            
            // "Open Location Settings"
            lower.contains("open location") || lower.contains("location settings") || lower.contains("gps settings") -> SmartActionType.OPEN_LOCATION
            
            // "Open App Info"
            lower.contains("app info") || lower.contains("application info") || lower.contains("open app info") -> SmartActionType.OPEN_APP_INFO
            
            // "Go Home"
            lower.contains("go home") || lower == "home" || lower.contains("return home") || lower.contains("return to home") || lower.contains("back home") -> SmartActionType.GO_HOME

            // "Check my notifications"
            lower.contains("check my notifications") || 
            lower.contains("check notifications") || 
            lower.contains("read my notifications") || 
            lower.contains("read notifications") ||
            lower.contains("read recent notifications") ||
            lower.contains("what notifications arrived") ||
            lower.contains("any new notifications") ||
            lower.contains("any new alerts") -> SmartActionType.CHECK_NOTIFICATIONS

            // Call answering and declining commands
            lower.contains("answer call") || lower.contains("accept call") || lower.contains("pick up") || lower.contains("answer incoming call") -> SmartActionType.ANSWER_CALL
            lower.contains("decline call") || lower.contains("reject call") || lower.contains("hang up") || lower.contains("ignore call") || lower.contains("decline incoming call") -> SmartActionType.DECLINE_CALL

            else -> null
        }
    }
}

class VoiceCommandParser(private val matchers: List<CommandMatcher> = listOf(PhraseCommandMatcher())) {
    fun parse(input: String): CommandResult {
        val lower = input.lowercase().trim()

        // 1. Smart YouTube Controls (Checking prefixes and suffixes)
        if (lower.startsWith("play ") && (lower.endsWith(" on youtube") || lower.contains("on youtube"))) {
            var q = input.substring(5).trim()
            if (q.lowercase().endsWith(" on youtube")) {
                q = q.substring(0, q.length - 11).trim()
            } else if (q.lowercase().contains("on youtube")) {
                q = q.replace("on youtube", "").trim()
            }
            return CommandResult.YouTubeSearchCommand(q)
        }
        if (lower.startsWith("search ") && lower.contains(" on youtube")) {
            var q = input.substring(7).trim()
            if (q.lowercase().endsWith(" on youtube")) {
                q = q.substring(0, q.length - 11).trim()
            }
            return CommandResult.YouTubeSearchCommand(q)
        }
        if (lower == "pause youtube" || lower == "pause video" || lower == "pause") {
            return CommandResult.YouTubeControlCommand("PAUSE")
        }
        if (lower == "resume youtube" || lower == "resume video" || lower == "resume" || lower == "play") {
            return CommandResult.YouTubeControlCommand("RESUME")
        }
        if (lower.contains("next video") || lower.contains("skip video") || lower == "next") {
            return CommandResult.YouTubeControlCommand("NEXT")
        }
        if (lower.contains("previous video") || lower == "previous" || lower == "prev") {
            return CommandResult.YouTubeControlCommand("PREVIOUS")
        }
        if (lower.contains("volume up") || lower.contains("increase volume")) {
            return CommandResult.YouTubeControlCommand("VOLUME_UP")
        }
        if (lower.contains("volume down") || lower.contains("decrease volume")) {
            return CommandResult.YouTubeControlCommand("VOLUME_DOWN")
        }
        if (lower.contains("fullscreen") || lower.contains("toggle fullscreen")) {
            return CommandResult.YouTubeControlCommand("FULLSCREEN")
        }

        // 2. WhatsApp specialized controls
        if (lower.startsWith("open chat with ") || lower.startsWith("open whatsapp chat with ")) {
            val prefix = if (lower.startsWith("open chat with ")) "open chat with " else "open whatsapp chat with "
            val contact = input.substring(prefix.length).trim()
            return CommandResult.WhatsAppOpenChatCommand(contact)
        }
        if (lower.startsWith("open chat ") || lower.startsWith("whatsapp chat ")) {
            val prefix = if (lower.startsWith("open chat ")) "open chat " else "whatsapp chat "
            val contact = input.substring(prefix.length).trim()
            return CommandResult.WhatsAppOpenChatCommand(contact)
        }
        if (lower.startsWith("send message to ") && lower.contains(" saying ")) {
            val idx = lower.indexOf(" saying ")
            val contactPart = input.substring(16, idx).trim()
            val messagePart = input.substring(idx + 8).trim()
            return CommandResult.WhatsAppSendMessageCommand(contactPart, messagePart)
        }
        if (lower.startsWith("send message to ") && lower.contains(" text ")) {
            val idx = lower.indexOf(" text ")
            val contactPart = input.substring(16, idx).trim()
            val messagePart = input.substring(idx + 6).trim()
            return CommandResult.WhatsAppSendMessageCommand(contactPart, messagePart)
        }
        if (lower.startsWith("whatsapp ") && lower.contains(" saying ")) {
            val idx = lower.indexOf(" saying ")
            val contactPart = input.substring(9, idx).trim()
            val messagePart = input.substring(idx + 8).trim()
            return CommandResult.WhatsAppSendMessageCommand(contactPart, messagePart)
        }
        // Send screenshot / images
        if (lower.contains("send screenshot to ") || lower.contains("share screenshot to ") || lower.contains("share screenshot with ")) {
            val prefix = if (lower.contains("send screenshot to ")) "send screenshot to " else if (lower.contains("share screenshot to ")) "share screenshot to " else "share screenshot with "
            val startIdx = lower.indexOf(prefix) + prefix.length
            val contactVal = input.substring(startIdx).trim()
            return CommandResult.WhatsAppShareScreenshotCommand(contactVal)
        }
        if (lower.contains("send image to ") || lower.contains("share image with ")) {
            val prefix = if (lower.contains("send image to ")) "send image to " else "share image with "
            val startIdx = lower.indexOf(prefix) + prefix.length
            val contactVal = input.substring(startIdx).trim()
            return CommandResult.WhatsAppSendImageCommand(contactVal)
        }

        // 3. Close & Switch App Commands
        if (lower.startsWith("close ") || lower.startsWith("terminate ") || lower.startsWith("kill ")) {
            val prefix = if (lower.startsWith("close ")) "close " else if (lower.startsWith("terminate ")) "terminate " else "kill "
            val app = input.substring(prefix.length).trim()
            return CommandResult.CloseAppCommand(app)
        }
        if (lower.startsWith("switch to ")) {
            val app = input.substring(10).trim()
            return CommandResult.SwitchAppCommand(app)
        }
        if (lower.startsWith("launch shortcut ") || lower.startsWith("open shortcut ")) {
            val prefix = if (lower.startsWith("launch shortcut ")) "launch shortcut " else "open shortcut "
            val rest = input.substring(prefix.length).trim()
            val parts = rest.split(" on ", " in ", " for ", limit = 2)
            if (parts.size == 2) {
                return CommandResult.LaunchShortcutCommand(parts[1].trim(), parts[0].trim())
            } else {
                return CommandResult.LaunchShortcutCommand("", parts[0].trim())
            }
        }

        // 4. Screenshot / Screen capture
        if (lower.contains("take screenshot") || lower.contains("capture screen") || lower == "screenshot" || lower.contains("take a screenshot")) {
            return CommandResult.TakeScreenshotCommand(input)
        }
        if (lower.contains("screenshot gallery") || lower.contains("open screenshot gallery") || lower.contains("view screenshots") || lower.contains("show screenshots")) {
            return CommandResult.OpenScreenshotGalleryCommand(input)
        }

        // 5. File Intelligence Engine
        if (lower.startsWith("search file ") || lower.startsWith("search files ") || lower.startsWith("find file ")) {
            val prefix = if (lower.startsWith("search files ")) "search files " else if (lower.startsWith("search file ")) "search file " else "find file "
            val query = input.substring(prefix.length).trim()
            return CommandResult.FileSearchCommand(query, "ALL")
        }
        if (lower.startsWith("search image ") || lower.startsWith("search images ") || lower.startsWith("find image ") || lower.startsWith("find images ")) {
            val prefix = if (lower.startsWith("search images ")) "search images " else if (lower.startsWith("search image ")) "search image " else if (lower.startsWith("find images ")) "find images " else "find image "
            val query = input.substring(prefix.length).trim()
            return CommandResult.FileSearchCommand(query, "IMAGE")
        }
        if (lower.startsWith("search document ") || lower.startsWith("search documents ") || lower.startsWith("find document ") || lower.startsWith("find documents ")) {
            val prefix = if (lower.startsWith("search documents ")) "search documents " else if (lower.startsWith("search document ")) "search document " else if (lower.startsWith("find documents ")) "find documents " else "find document "
            val query = input.substring(prefix.length).trim()
            return CommandResult.FileSearchCommand(query, "DOCUMENT")
        }
        if (lower.startsWith("open file ") || lower.startsWith("open document ")) {
            val prefix = if (lower.startsWith("open file ")) "open file " else "open document "
            val query = input.substring(prefix.length).trim()
            return CommandResult.OpenFileCommand(query)
        }
        if (lower.contains("show recent files") || lower.contains("list recent files") || lower == "recent files") {
            return CommandResult.ShowRecentFilesCommand(input)
        }

        // 6. Automation sequences
        if (lower.contains("morning mode")) {
            return CommandResult.ExecuteAutomationSequenceCommand("Morning Mode")
        }
        if (lower.contains("work mode")) {
            return CommandResult.ExecuteAutomationSequenceCommand("Work Mode")
        }

        // Match communication AI queries (Phase 21.3)
        if (lower.contains("read latest whatsapp") || lower.contains("read whatsapp") || lower == "read message") {
            return CommandResult.ReadWhatsAppCommand(input)
        }
        if (lower.contains("read latest sms") || lower.contains("read sms") || lower == "read messages") {
            return CommandResult.ReadSmsCommand(input)
        }
        if (lower.contains("missed calls") || lower.contains("check missed calls") || lower.contains("read missed calls") || 
            lower.contains("any missed calls") || lower.contains("summarize missed calls") || lower.contains("missed call summary")) {
            return CommandResult.ReadMissedCallsCommand(input)
        }

        // Match assistant switching commands first
        if (lower.contains("nova, send zoya") || lower.contains("switch to zoya") || lower == "zoya") {
            return CommandResult.SwitchAssistantCommand("ZOYA")
        }
        if (lower.contains("zoya, send nova") || lower.contains("switch to nova") || lower == "nova") {
            return CommandResult.SwitchAssistantCommand("NOVA")
        }

        // Match show recent contacts command
        if (lower.contains("show recent contacts") || lower.contains("recent contacts") || lower.contains("view recent contacts")) {
            return CommandResult.ShowRecentContactsCommand(input)
        }

        // Match voice language setting commands
        if (lower.contains("change language to bengali") || lower.contains("change voice to bengali")) {
            return CommandResult.ChangeLanguageCommand("bn")
        }
        if (lower.contains("change language to english") || lower.contains("change voice to english")) {
            return CommandResult.ChangeLanguageCommand("en")
        }

        // Match voice feedback setting commands
        if (lower.contains("enable voice feedback") || lower.contains("turn on voice feedback")) {
            return CommandResult.ToggleVoiceFeedbackCommand(true)
        }
        if (lower.contains("disable voice feedback") || lower.contains("turn off voice feedback")) {
            return CommandResult.ToggleVoiceFeedbackCommand(false)
        }

        // Match play music queries (Phase 21.3 fallback)
        if (lower.contains("play music") || lower.contains("play some music") || lower.contains("start music") || lower.contains("play lofi")) {
            return CommandResult.OpenBrowserCommand("https://www.youtube.com/results?search_query=lofi+beats")
        }

        // 0. Check for browser URL and application opening commands
        if (lower.startsWith("open ") || lower.startsWith("launch ")) {
            val candidate = if (lower.startsWith("open ")) {
                input.substring(5).trim()
            } else {
                input.substring(7).trim()
            }
            if (candidate.isNotEmpty()) {
                val lowerCand = candidate.lowercase()
                val isWebLink = lowerCand.startsWith("http://") || 
                               lowerCand.startsWith("https://") || 
                               lowerCand.endsWith(".com") || 
                               lowerCand.endsWith(".org") || 
                               lowerCand.endsWith(".net") || 
                               lowerCand.endsWith(".io") || 
                               lowerCand.endsWith(".gov") || 
                               lowerCand.endsWith(".edu") ||
                               lowerCand.contains(".com/") ||
                               lowerCand.contains(".org/")
                
                if (isWebLink) {
                    val formattedUrl = if (lowerCand.startsWith("http://") || lowerCand.startsWith("https://")) {
                        candidate
                    } else {
                        "https://$candidate"
                    }
                    return CommandResult.OpenBrowserCommand(formattedUrl)
                }
                
                return CommandResult.OpenAppCommand(candidate)
            }
        }

        // 1. Check for drafting commands (E.g. "Create a message for [Contact Name]")
        val draftPrefixes = listOf(
            "create a message for ",
            "send a message to ",
            "write a message to ",
            "draft a message to ",
            "draft a message for ",
            "send message to ",
            "message for ",
            "message to "
        )
        for (prefix in draftPrefixes) {
            if (lower.startsWith(prefix)) {
                val contactPart = input.substring(prefix.length).trim()
                if (contactPart.isNotEmpty()) {
                    return CommandResult.CreateDraftCommand(contactPart)
                }
            }
        }

        // 1.8. Check for call initiation commands (E.g. "Call Tony Stark", "Dial Commander Kane")
        val callPrefixes = listOf(
            "call ",
            "dial ",
            "initiate call to ",
            "start call with "
        )
        for (prefix in callPrefixes) {
            if (lower.startsWith(prefix)) {
                val contactPart = input.substring(prefix.length).trim()
                if (contactPart.isNotEmpty()) {
                    return CommandResult.InitiateCallCommand(contactPart)
                }
            }
        }

        // 2. Check for contact search commands (E.g. "Find [Contact Name]", "Search Contact [Contact Name]")
        val searchPrefixes = listOf(
            "search for ",
            "find contact ",
            "find ",
            "search contact ",
            "search "
        )
        for (prefix in searchPrefixes) {
            if (lower.startsWith(prefix)) {
                val contactPart = input.substring(prefix.length).trim()
                if (contactPart.isNotEmpty()) {
                    return CommandResult.SearchContactCommand(contactPart)
                }
            }
        }

        // 3. Fallback to standard command matchers
        for (matcher in matchers) {
            val matchedType = matcher.match(input)
            if (matchedType != null) {
                return CommandResult.ActionCommand(matchedType)
            }
        }
        return CommandResult.UnknownCommand(input)
    }
}
