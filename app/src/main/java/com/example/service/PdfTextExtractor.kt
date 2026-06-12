package com.example.service

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.lang.StringBuilder

object PdfTextExtractor {
    private const val TAG = "PdfTextExtractor"

    fun extract(context: Context, uri: Uri): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return "Empty or inaccessible stream."
            inputStream.use { stream ->
                // Limit buffer to 2MB to prevent OOM
                val bytes = stream.readNBytesLimit(2 * 1024 * 1024)
                if (bytes.isEmpty()) {
                    return "Zero bytes read from document stream."
                }
                val extracted = parsePdfBytes(bytes)
                if (extracted.isBlank()) {
                    "No visible text extracted. (PDF might be scanned or image-based)"
                } else {
                    extracted
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse PDF text safely", e)
            "Error extracting text from PDF: ${e.message}"
        }
    }

    private fun InputStream.readNBytesLimit(limit: Int): ByteArray {
        val bos = ByteArrayOutputStream()
        val buf = ByteArray(4096)
        var totalRead = 0
        var read: Int
        while (this.read(buf).also { read = it } != -1) {
            if (totalRead + read > limit) {
                val remaining = limit - totalRead
                if (remaining > 0) {
                    bos.write(buf, 0, remaining)
                }
                break
            }
            bos.write(buf, 0, read)
            totalRead += read
        }
        return bos.toByteArray()
    }

    private fun parsePdfBytes(bytes: ByteArray): String {
        val result = StringBuilder()
        var i = 0
        val n = bytes.size
        var inParentheses = false
        val currentString = StringBuilder()

        while (i < n) {
            val b = bytes[i]
            val c = b.toInt().toChar()
            if (c == '(' && !inParentheses) {
                inParentheses = true
                currentString.setLength(0)
            } else if (c == ')' && inParentheses) {
                inParentheses = false
                val decoded = currentString.toString().trim()
                if (isUserVisibleText(decoded)) {
                    result.append(decoded).append(" ")
                }
            } else if (inParentheses) {
                // Parse standard escapes in PDF strings
                if (c == '\\' && i + 1 < n) {
                    val next = bytes[i + 1].toInt().toChar()
                    if (next == '(' || next == ')' || next == '\\') {
                        currentString.append(next)
                        i += 2
                        continue
                    }
                }
                if (b in 32..126 || b == 10.toByte() || b == 13.toByte() || b == 9.toByte()) {
                    currentString.append(c)
                }
            }
            i++
        }

        val cleaned = result.toString().trim().replace(Regex("\\s+"), " ")
        if (cleaned.length < 20) {
            // Fallback scanner to catch general printable strings
            return extractPrintableFallback(bytes)
        }
        return cleaned
    }

    private fun isUserVisibleText(text: String): Boolean {
        if (text.isEmpty() || text.length < 2) return false
        val lower = text.lowercase()
        if (lower.startsWith("/") || lower.startsWith("font") || lower.startsWith("procset")) return false
        if (lower.contains("adobe") || lower.contains("identity") || lower.contains("xml") || lower.contains("uuid") || lower.contains("metadata")) return false
        if (text.all { !it.isLetterOrDigit() && !it.isWhitespace() }) return false
        return true
    }

    private fun extractPrintableFallback(bytes: ByteArray): String {
        val sb = StringBuilder()
        val currentToken = StringBuilder()
        for (b in bytes) {
            val c = b.toInt().toChar()
            if (b in 32..126) {
                currentToken.append(c)
            } else {
                if (currentToken.length >= 4) {
                    val candidate = currentToken.toString().trim()
                    if (isUserVisibleText(candidate)) {
                        sb.append(candidate).append("\n")
                    }
                }
                currentToken.setLength(0)
            }
        }
        if (currentToken.length >= 4) {
            val candidate = currentToken.toString().trim()
            if (isUserVisibleText(candidate)) {
                sb.append(candidate)
            }
        }
        val text = sb.toString().trim()
        return if (text.length > 5000) text.substring(0, 5000) + "... [Truncated]" else text
    }
}
