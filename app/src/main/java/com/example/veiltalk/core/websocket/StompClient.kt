package com.example.veiltalk.core.websocket

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class StompClient(private val okHttpClient: OkHttpClient) {

    private var webSocket: WebSocket? = null

    interface Listener {
        fun onStompConnected()
        fun onStompFrame(frame: StompFrame)
        fun onStompError(message: String)
        fun onSocketClosed()
    }

    fun connect(url: String, connectHeaders: Map<String, String>, listener: Listener) {
        val request = Request.Builder().url(url).build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                val connectFrame = StompFrame(
                    command = "CONNECT",
                    headers = connectHeaders + mapOf(
                        "accept-version" to "1.1,1.2",
                        "heart-beat" to "0,0"
                    ),
                    body = ""
                )
                webSocket.send(connectFrame.encode())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.isBlank()) return // heartbeat احتمالی
                val frame = StompFrame.decode(text) ?: return
                when (frame.command) {
                    "CONNECTED" -> listener.onStompConnected()
                    "MESSAGE" -> listener.onStompFrame(frame)
                    "ERROR" -> listener.onStompError(frame.body)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                listener.onSocketClosed()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                listener.onStompError(t.message ?: "خطا در اتصال")
                listener.onSocketClosed()
            }
        })
    }

    fun send(frame: StompFrame) {
        webSocket?.send(frame.encode())
    }

    fun close() {
        webSocket?.send(StompFrame("DISCONNECT", emptyMap(), "").encode())
        webSocket?.close(1000, "Client disconnect")
        webSocket = null
    }
}