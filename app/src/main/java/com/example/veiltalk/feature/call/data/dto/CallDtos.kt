package com.example.veiltalk.feature.call.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class CallSignalDto(
    val type: String,
    val from: String? = null,
    val to: String,
    val sdp: String? = null,
    val candidate: String? = null,
    val callId: String,
    val callType: String? = null
)