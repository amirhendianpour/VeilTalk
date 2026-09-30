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
import java.util.concurrent.atomic.AtomicLong

class StompClient(private val okHttpClient: OkHttpClient) {

    private var webSocket: WebSocket? = null
    private val clientScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var heartbeatJob: Job? = null
    private val lastServerActivityTime = AtomicLong(0L)

    interface Listener {
        fun onStompConnected()
        fun onStompFrame(frame: StompFrame)
        fun onStompError(message: String)
        fun onSocketClosed()
    }

    fun connect(url: String, connectHeaders: Map<String, String>, listener: Listener) {
        stopHeartbeat()
        val request = Request.Builder().url(url).build()
        lastServerActivityTime.set(System.currentTimeMillis())

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                lastServerActivityTime.set(System.currentTimeMillis())
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
                lastServerActivityTime.set(System.currentTimeMillis())
                if (text.isBlank() || text == "\n" || text == "\r\n") return // heartbeat های ارسالی از سرور
                val frame = StompFrame.decode(text) ?: return
                when (frame.command) {
                    "CONNECTED" -> {
                        startHeartbeat(listener)
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

    private fun startHeartbeat(listener: Listener) {
        stopHeartbeat()
        lastServerActivityTime.set(System.currentTimeMillis())
        heartbeatJob = clientScope.launch {
            while (isActive) {
                delay(5000)
                val now = System.currentTimeMillis()
                
                // اگر از سمت سرور بیش از ۲۵ ثانیه هیچ دیتایی (فریم یا پینگ) دریافت نشده باشد، سوکت معلق تلقی می‌شود
                if (now - lastServerActivityTime.get() > 25000) {
                    val currentWs = webSocket
                    webSocket = null
                    currentWs?.cancel()
                    stopHeartbeat()
                    listener.onSocketClosed()
                    break
                }

                val sent = webSocket?.send("\n") ?: false
                if (!sent) {
                    stopHeartbeat()
                    listener.onSocketClosed()
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
        try {
            webSocket?.send(StompFrame("DISCONNECT", emptyMap(), "").encode())
            webSocket?.close(1000, "Client disconnect")
        } catch (_: Exception) {}
        webSocket = null
    }
}