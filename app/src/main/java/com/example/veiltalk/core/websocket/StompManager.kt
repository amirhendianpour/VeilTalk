package com.example.veiltalk.core.websocket

import com.example.veiltalk.core.di.ApplicationScope
import com.example.veiltalk.core.network.ApiConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StompManager @Inject constructor(
    private val stompClient: StompClient,
    @ApplicationScope private val scope: CoroutineScope
) {
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _frames = MutableSharedFlow<StompFrame>(extraBufferCapacity = 64)
    val frames: SharedFlow<StompFrame> = _frames

    private var currentToken: String? = null
    private var subscriptionCounter = 0
    private var reconnectAttempt = 0

    // صف‌هایی که همیشه هنگام اتصال باید subscribe بشن — معادل subscribe های onConnect در وب
    private val fixedDestinations = listOf(
        "/user/queue/messages",
        "/user/queue/receipts",
        "/user/queue/typing",
        "/user/queue/group-history",
        "/user/queue/group-messages",
        "/user/queue/group-updates",
        "/user/queue/call"
    )

    fun connect(token: String) {
        if (_connectionState.value != ConnectionState.DISCONNECTED) return
        currentToken = token
        reconnectAttempt = 0
        openSocket(token)
    }

    private fun openSocket(token: String) {
        _connectionState.value = ConnectionState.CONNECTING

        val wsUrl = ApiConfig.WS_URL
            .replace("https://", "wss://")
            .replace("http://", "ws://") + "/websocket"

        stompClient.connect(
            url = wsUrl,
            connectHeaders = mapOf("Authorization" to "Bearer $token"),
            listener = object : StompClient.Listener {
                override fun onStompConnected() {
                    reconnectAttempt = 0
                    _connectionState.value = ConnectionState.CONNECTED
                    subscribeToFixedDestinations()
                    // معادل client.publish({destination:"/app/group/history"}) در وب
                    publish("/app/group/history", "{}")
                }

                override fun onStompFrame(frame: StompFrame) {
                    scope.launch { _frames.emit(frame) }
                }

                override fun onStompError(message: String) {
                    // در آینده می‌تونیم لاگ متمرکز بذاریم
                }

                override fun onSocketClosed() {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    scheduleReconnect()
                }
            }
        )
    }

    private fun subscribeToFixedDestinations() {
        fixedDestinations.forEach { destination ->
            subscriptionCounter++
            stompClient.send(
                StompFrame(
                    command = "SUBSCRIBE",
                    headers = mapOf("id" to "sub-$subscriptionCounter", "destination" to destination),
                    body = ""
                )
            )
        }
    }

    fun publish(destination: String, jsonBody: String) {
        if (_connectionState.value != ConnectionState.CONNECTED) return
        stompClient.send(
            StompFrame(
                command = "SEND",
                headers = mapOf("destination" to destination, "content-type" to "application/json"),
                body = jsonBody
            )
        )
    }

    private fun scheduleReconnect() {
        val token = currentToken ?: return
        reconnectAttempt++
        val delayMs = minOf(5000L * reconnectAttempt, 30000L) // backoff تدریجی تا سقف ۳۰ ثانیه
        scope.launch {
            delay(delayMs)
            if (_connectionState.value == ConnectionState.DISCONNECTED && currentToken != null) {
                openSocket(token)
            }
        }
    }

    fun disconnect() {
        currentToken = null
        stompClient.close()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    // فیلتر فریم‌های یک صف خاص — feature های بعدی (چت/گروه/تماس) از این استفاده می‌کنن
    fun framesForDestination(destination: String): Flow<StompFrame> =
        frames.filter { it.headers["destination"] == destination }
}