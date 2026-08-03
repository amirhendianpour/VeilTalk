package com.example.veiltalk.common.model

enum class CallSignalType { OFFER, ANSWER, ICE_CANDIDATE, END, REJECT, BUSY }
enum class CallKind { AUDIO, VIDEO }
enum class CallStatus { IDLE, CALLING, RINGING, CONNECTED }

data class CallSignal(
    val type: CallSignalType,
    val from: String? = null,
    val to: String,
    val sdp: String? = null,
    val candidate: String? = null,
    val callId: String,
    val callType: CallKind? = null
)