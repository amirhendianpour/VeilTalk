package com.example.veiltalk.feature.call.data

import android.content.Context
import com.example.veiltalk.common.model.CallKind
import com.example.veiltalk.common.model.CallSignal
import com.example.veiltalk.common.model.CallSignalType
import com.example.veiltalk.common.model.CallStatus
import com.example.veiltalk.core.di.ApplicationScope
import com.example.veiltalk.core.websocket.StompManager
import com.example.veiltalk.feature.call.data.dto.CallSignalDto
import com.example.veiltalk.feature.call.data.webrtc.WebRtcClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import org.webrtc.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import android.os.Handler
import android.os.Looper

data class CallUiSnapshot(
    val status: CallStatus = CallStatus.IDLE,
    val callType: CallKind? = null,
    val remoteUser: String? = null,
    val isMuted: Boolean = false,
    val isCameraOff: Boolean = false
)

@Singleton
class CallRepository @Inject constructor(
    private val stompManager: StompManager,
    private val json: Json,
    @ApplicationContext private val appContext: Context,
    @ApplicationScope private val scope: CoroutineScope
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val _uiState = MutableStateFlow(CallUiSnapshot())
    val uiState: StateFlow<CallUiSnapshot> = _uiState.asStateFlow()

    private val _localVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val localVideoTrack: StateFlow<VideoTrack?> = _localVideoTrack.asStateFlow()

    private val _remoteVideoTrack = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrack: StateFlow<VideoTrack?> = _remoteVideoTrack.asStateFlow()

    val eglBaseContext: EglBase.Context get() = webRtcClient.eglBaseContext

    private var webRtcClient = WebRtcClient(appContext)
    private var callId: String = ""
    private var pendingOffer: CallSignal? = null
    private val iceQueue = mutableListOf<IceCandidate>()

    init {
        stompManager.framesForDestination("/user/queue/call")
            .onEach { frame -> handleSignal(frame.body) }
            .launchIn(scope)
    }

    private fun handleSignal(rawBody: String) {
        val dto = runCatching { json.decodeFromString<CallSignalDto>(rawBody) }.getOrNull() ?: return
        val signal = dto.toDomain() ?: return

        when (signal.type) {
            CallSignalType.OFFER -> {
                if (_uiState.value.status != CallStatus.IDLE) {
                    sendSignal(CallSignal(CallSignalType.BUSY, to = signal.from ?: return, callId = signal.callId))
                    return
                }
                callId = signal.callId
                pendingOffer = signal
                _uiState.value = _uiState.value.copy(
                    status = CallStatus.RINGING,
                    callType = signal.callType ?: CallKind.AUDIO,
                    remoteUser = signal.from
                )
            }
            CallSignalType.ANSWER -> {
                signal.sdp?.let { raw ->
                    decodeSdp(raw)?.let { sdp -> webRtcClient.setRemoteDescription(sdp) }
                }
            }
            CallSignalType.ICE_CANDIDATE -> {
                signal.candidate?.let { raw ->
                    val candidate = decodeIceCandidate(raw) ?: return
                    webRtcClient.addIceCandidate(candidate)
                }
            }
            CallSignalType.REJECT, CallSignalType.END, CallSignalType.BUSY -> {
                cleanup()
            }
        }
    }

    fun startCall(recipient: String, kind: CallKind = CallKind.AUDIO) {
        if (_uiState.value.status != CallStatus.IDLE) return

        callId = UUID.randomUUID().toString()
        _uiState.value = _uiState.value.copy(status = CallStatus.CALLING, callType = kind, remoteUser = recipient)

        setupPeerConnection(withVideo = kind == CallKind.VIDEO)

        webRtcClient.createOffer { sdp ->
            sendSignal(
                CallSignal(
                    type = CallSignalType.OFFER,
                    to = recipient,
                    sdp = encodeSdp(sdp),
                    callId = callId,
                    callType = kind
                )
            )
        }
    }

    fun acceptCall() {
        val offer = pendingOffer ?: return
        val from = offer.from ?: return
        val rawSdp = offer.sdp ?: return
        val kind = offer.callType ?: CallKind.AUDIO

        val remoteSdp = decodeSdp(rawSdp) ?: return

        setupPeerConnection(withVideo = kind == CallKind.VIDEO)
        webRtcClient.setRemoteDescription(remoteSdp)

        iceQueue.forEach { webRtcClient.addIceCandidate(it) }
        iceQueue.clear()

        webRtcClient.createAnswer { answerSdp ->
            sendSignal(
                CallSignal(
                    type = CallSignalType.ANSWER,
                    to = from,
                    sdp = encodeSdp(answerSdp),
                    callId = callId,
                    callType = kind
                )
            )
        }

        _uiState.value = _uiState.value.copy(status = CallStatus.CONNECTED)
        pendingOffer = null
    }

    fun rejectCall() {
        pendingOffer?.from?.let { from ->
            sendSignal(CallSignal(CallSignalType.REJECT, to = from, callId = callId))
        }
        cleanup()
    }

    fun endCall() {
        _uiState.value.remoteUser?.let { user ->
            sendSignal(CallSignal(CallSignalType.END, to = user, callId = callId))
        }
        cleanup()
    }

    fun toggleMute() {
        val newMuted = !_uiState.value.isMuted
        webRtcClient.setAudioEnabled(!newMuted)
        _uiState.value = _uiState.value.copy(isMuted = newMuted)
    }

    fun toggleCamera() {
        val newCameraOff = !_uiState.value.isCameraOff
        webRtcClient.setVideoEnabled(!newCameraOff)
        _uiState.value = _uiState.value.copy(isCameraOff = newCameraOff)
    }

    private fun setupPeerConnection(withVideo: Boolean) {
        webRtcClient.initFactory()
        webRtcClient.createPeerConnection()

        webRtcClient.onIceCandidate = { candidate ->
            _uiState.value.remoteUser?.let { remote ->
                sendSignal(
                    CallSignal(
                        type = CallSignalType.ICE_CANDIDATE,
                        to = remote,
                        candidate = encodeIceCandidate(candidate),
                        callId = callId
                    )
                )
            }
        }

        webRtcClient.onRemoteTrack = { track ->
            if (track is VideoTrack) {
                _remoteVideoTrack.value = track
            }
        }

        webRtcClient.onConnectionStateChanged = { state ->
            when (state) {
                PeerConnection.PeerConnectionState.CONNECTED -> {
                    _uiState.value = _uiState.value.copy(status = CallStatus.CONNECTED)
                }
                PeerConnection.PeerConnectionState.DISCONNECTED,
                PeerConnection.PeerConnectionState.FAILED,
                PeerConnection.PeerConnectionState.CLOSED -> {
                    cleanup()
                }
                else -> {}
            }
        }

        webRtcClient.attachLocalMedia(withVideo)
        _localVideoTrack.value = webRtcClient.localVideoTrack
    }

    private fun sendSignal(signal: CallSignal) {
        val dto = signal.toDto()
        val destination = when (signal.type) {
            CallSignalType.OFFER -> "/app/call/offer"
            CallSignalType.ANSWER -> "/app/call/answer"
            CallSignalType.ICE_CANDIDATE -> "/app/call/ice-candidate"
            CallSignalType.END -> "/app/call/end"
            CallSignalType.REJECT, CallSignalType.BUSY -> "/app/call/reject"
        }
        stompManager.publish(destination, json.encodeToString(CallSignalDto.serializer(), dto))
    }

    private fun cleanup() {
        mainHandler.post {
            webRtcClient.close()
            webRtcClient = WebRtcClient(appContext)
            _localVideoTrack.value = null
            _remoteVideoTrack.value = null
            pendingOffer = null
            iceQueue.clear()
            callId = ""
            _uiState.value = CallUiSnapshot()
        }
    }

    private fun encodeIceCandidate(candidate: IceCandidate): String {
        return json.encodeToString(
            kotlinx.serialization.json.JsonObject.serializer(),
            kotlinx.serialization.json.buildJsonObject {
                put("sdpMid", kotlinx.serialization.json.JsonPrimitive(candidate.sdpMid))
                put("sdpMLineIndex", kotlinx.serialization.json.JsonPrimitive(candidate.sdpMLineIndex))
                put("candidate", kotlinx.serialization.json.JsonPrimitive(candidate.sdp))
            }
        )
    }

    private fun decodeIceCandidate(raw: String): IceCandidate? {
        return try {
            val obj = json.parseToJsonElement(raw).let { it as kotlinx.serialization.json.JsonObject }
            IceCandidate(
                obj["sdpMid"]?.let { (it as kotlinx.serialization.json.JsonPrimitive).content },
                obj["sdpMLineIndex"]?.let { (it as kotlinx.serialization.json.JsonPrimitive).content.toInt() } ?: 0,
                obj["candidate"]?.let { (it as kotlinx.serialization.json.JsonPrimitive).content }
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun encodeSdp(sdp: SessionDescription): String {
        return json.encodeToString(
            kotlinx.serialization.json.JsonObject.serializer(),
            kotlinx.serialization.json.buildJsonObject {
                put("type", kotlinx.serialization.json.JsonPrimitive(sdp.type.canonicalForm()))
                put("sdp", kotlinx.serialization.json.JsonPrimitive(sdp.description))
            }
        )
    }

    private fun decodeSdp(raw: String): SessionDescription? {
        return try {
            val obj = json.parseToJsonElement(raw) as kotlinx.serialization.json.JsonObject
            val typeStr = (obj["type"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return null
            val sdpStr = (obj["sdp"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return null
            val type = when (typeStr) {
                "offer" -> SessionDescription.Type.OFFER
                "answer" -> SessionDescription.Type.ANSWER
                "pranswer" -> SessionDescription.Type.PRANSWER
                "rollback" -> SessionDescription.Type.ROLLBACK
                else -> return null
            }
            SessionDescription(type, sdpStr)
        } catch (e: Exception) {
            null
        }
    }
}

private fun CallSignal.toDto() = CallSignalDto(
    type = type.name,
    from = from,
    to = to,
    sdp = sdp,
    candidate = candidate,
    callId = callId,
    callType = callType?.name
)

private fun CallSignalDto.toDomain(): CallSignal? {
    val typeEnum = runCatching { CallSignalType.valueOf(type) }.getOrNull() ?: return null
    return CallSignal(
        type = typeEnum,
        from = from,
        to = to,
        sdp = sdp,
        candidate = candidate,
        callId = callId,
        callType = callType?.let { runCatching { CallKind.valueOf(it) }.getOrNull() }
    )
}