package net.lefil.fdroid

import java.net.URI
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * Gotify WebSocket client transport, built on OkHttp like gotify/android.
 * Inert until a private per-device client token and the server URL are provisioned.
 * This class does not provision accounts, store credentials or modify the VPS.
 */
internal class GotifyStreamTransport(
    private val request: Request,
    private val sockets: WebSocket.Factory,
    private val scheduler: ScheduledExecutorService,
    private val onNotice: (PushNotice) -> Unit,
    private val onConnected: () -> Unit = {},
    private val onAuthRequired: () -> Unit = {}
) {
    private var active = false
    private var generation = 0L
    private var failures = 0
    private var socket: WebSocket? = null

    companion object {
        private val tokenPattern = Regex("[A-Za-z0-9_.-]{16,512}")

        /** Gotify accepts X-Gotify-Key. Never place client tokens in URLs or logs. */
        fun buildRequest(serverBase: String, clientToken: String): Request? = runCatching {
            if (!tokenPattern.matches(clientToken)) return null
            val uri = URI(serverBase)
            if (uri.scheme != "https" || uri.userInfo != null ||
                (uri.port != -1 && uri.port != 443) ||
                uri.rawQuery != null || uri.rawFragment != null ||
                (uri.rawPath != "" && uri.rawPath != "/")) return null
            val host = uri.host ?: return null
            if (!host.endsWith(".le-fil.net", ignoreCase = true)) return null
            Request.Builder()
                .url("https://$host/stream")
                .header("X-Gotify-Key", clientToken)
                .get().build()
        }.getOrNull()

        fun newClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(1, TimeUnit.MINUTES)
            .build()
    }

    fun start() {
        val seq = synchronized(this) {
            if (active) return
            active = true
            failures = 0
            ++generation
        }
        connect(seq)
    }

    fun stop() {
        val old = synchronized(this) {
            active = false
            generation++
            socket.also { socket = null }
        }
        old?.close(1000, "Le Fil stopped")
    }

    private fun connect(seq: Long) {
        if (!isCurrent(seq)) return
        val opened = sockets.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (!isCurrent(seq)) return
                synchronized(this@GotifyStreamTransport) { failures = 0 }
                runCatching { onConnected() }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (!isCurrent(seq)) return
                GotifyNoticeParser.parse(text)?.let { notice ->
                    if (isCurrent(seq)) runCatching { onNotice(notice) }
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                retry(seq, false)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                retry(seq, response?.code == 401 || response?.code == 403)
            }
        })
        synchronized(this) {
            if (active && generation == seq) socket = opened
            else opened.close(1000, "Le Fil stopped")
        }
    }

    private fun isCurrent(seq: Long): Boolean = synchronized(this) {
        active && generation == seq
    }

    private fun retry(seq: Long, unauthorized: Boolean) {
        val next: Pair<Long, Long>? = synchronized(this) {
            if (!active || generation != seq) return
            socket = null
            if (unauthorized) {
                active = false
                generation++
                null
            } else {
                // Bounded backoff; no exact alarms or busy retry loops.
                failures = (failures + 1).coerceAtMost(7)
                val delay = (5_000L * (1L shl (failures - 1))).coerceAtMost(300_000L)
                Pair(++generation, delay)
            }
        }
        if (next == null) {
            runCatching { onAuthRequired() }
            return
        }
        runCatching {
            scheduler.schedule({ connect(next.first) }, next.second, TimeUnit.MILLISECONDS)
        }
    }
}
