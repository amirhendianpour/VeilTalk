package com.example.veiltalk.core.websocket

data class StompFrame(
    val command: String,
    val headers: Map<String, String>,
    val body: String
) {
    fun encode(): String {
        val sb = StringBuilder()
        sb.append(command).append('\n')
        headers.forEach { (k, v) -> sb.append(k).append(':').append(v).append('\n') }
        sb.append('\n')
        sb.append(body)
        sb.append('\u0000')
        return sb.toString()
    }

    companion object {
        fun decode(raw: String): StompFrame? {
            val content = raw.trimEnd('\u0000')
            if (content.isBlank()) return null
            val lines = content.split('\n')
            val command = lines.getOrNull(0) ?: return null

            val headers = mutableMapOf<String, String>()
            var i = 1
            while (i < lines.size && lines[i].isNotEmpty()) {
                val idx = lines[i].indexOf(':')
                if (idx > 0) {
                    headers[lines[i].substring(0, idx)] = lines[i].substring(idx + 1)
                }
                i++
            }
            val body = if (i + 1 <= lines.size) lines.subList(i + 1, lines.size).joinToString("\n") else ""
            return StompFrame(command, headers, body)
        }
    }
}