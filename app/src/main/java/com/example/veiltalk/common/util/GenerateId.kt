package com.example.veiltalk.common.util

import kotlin.random.Random

fun generateId(): String {
    val timePart = System.currentTimeMillis().toString(36)
    val randomPart = Random.nextLong(0, Long.MAX_VALUE).toString(36).take(6)
    return timePart + randomPart
}