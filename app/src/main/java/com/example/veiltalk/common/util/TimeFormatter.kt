package com.example.veiltalk.common.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

fun formatMessageTime(timestamp: String?): String {
    if (timestamp.isNullOrBlank()) return ""
    return try {
        val instant = Instant.parse(timestamp)
        val now = Instant.now()
        val zoneId = ZoneId.systemDefault()
        
        val date = instant.atZone(zoneId).toLocalDate()
        val today = now.atZone(zoneId).toLocalDate()
        
        when {
            date == today -> "امروز ساعت " + timeFormatter.format(instant)
            date == today.minusDays(1) -> "دیروز ساعت " + timeFormatter.format(instant)
            else -> DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(zoneId).format(instant)
        }
    } catch (e: Exception) {
        ""
    }
}