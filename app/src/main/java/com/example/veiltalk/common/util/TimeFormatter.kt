package com.example.veiltalk.common.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

fun formatMessageTime(timestamp: String?): String {
    if (timestamp.isNullOrBlank()) return ""
    return try {
        val instant = Instant.parse(timestamp)
        timeFormatter.format(instant)
    } catch (e: Exception) {
        ""
    }
}