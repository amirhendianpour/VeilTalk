package com.example.veiltalk.core.websocket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class StompClient(private val okHttpClient: OkHttpClient) {

    private var webSocket: WebSocket? = null
    private val clientScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var heartbeatJob: Job? = null

    interface Listener {
        fun onStompConnected()
        fun onStompFrame(frame: StompFrame)
        fun onStompError(message: String)
        fun onSocketClosed()
    }

    fun connect(url: String, connectHeaders: Map<String, String>, listener: Listener) {
        stopHeartbeat()
        val request = Request.Builder().url(url).build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                val connectFrame = StompFrame(
                    command = "CONNECT",
                    headers = connectHeaders + mapOf(
                        "accept-version" to "1.1,1.2",
                        "heart-beat" to "10000,10000"
                    ),
                    body = ""
                )
                webSocket.send(connectFrame.encode())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.isBlank() || text == "\n" || text == "\r\n") return // heartbeat های ارسالی از سرور
                val frame = StompFrame.decode(text) ?: return
                when (frame.command) {
                    "CONNECTED" -> {
                        startHeartbeat()
                        listener.onStompConnected()
                    }
                    "MESSAGE" -> listener.onStompFrame(frame)
                    "ERROR" -> listener.onStompError(frame.body)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                stopHeartbeat()
                listener.onSocketClosed()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                stopHeartbeat()
                listener.onStompError(t.message ?: "خطا در اتصال")
                listener.onSocketClosed()
            }
        })
    }

    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatJob = clientScope.launch {
            while (isActive) {
                delay(10000)
                val sent = webSocket?.send("\n") ?: false
                if (!sent) {
                    // اگر ارسال پینگ ناموفق بود سوکت قطعه
                    break
                }
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    fun send(frame: StompFrame) {
        webSocket?.send(frame.encode())
    }

    fun close() {
        stopHeartbeat()
        webSocket?.send(StompFrame("DISCONNECT", emptyMap(), "").encode())
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
    }
}