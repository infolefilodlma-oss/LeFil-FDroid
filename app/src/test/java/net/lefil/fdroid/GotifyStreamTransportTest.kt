package net.lefil.fdroid

import org.junit.Assert.*
import org.junit.Test

class GotifyStreamTransportTest {
    private const val token = "client_token_0123456789abcdef"

    @Test fun buildsAuthenticatedWebsocketWithoutLeakingTokenInUrl() {
        val r = GotifyStreamTransport.buildRequest("https://notifications.le-fil.net/", token)!!
        assertEquals("https://notifications.le-fil.net/stream", r.url.toString())
        assertEquals(token, r.header("X-Gotify-Key"))
        assertFalse(r.url.toString().contains(token))
    }

    @Test fun rejectsExternalInsecureAndMalformedServers() {
        for (url in listOf(
            "http://notifications.le-fil.net",
            "https://evil.test/",
            "https://notifications.le-fil.net.evil.test",
            "https://user@notifications.le-fil.net",
            "https://notifications.le-fil.net/?token=x",
            "https://notifications.le-fil.net/something",
            "https://notifications.le-fil.net:8080",
            "javascript:alert(1)"
        )) assertNull(url, GotifyStreamTransport.buildRequest(url, token))
    }

    @Test fun rejectsBlankTokensAndLineBreakInjection() {
        assertNull(GotifyStreamTransport.buildRequest("https://notifications.le-fil.net", ""))
        assertNull(GotifyStreamTransport.buildRequest("https://notifications.le-fil.net", "secret\nX-Fake: true"))
    }

    private class StubSocket(private val builtRequest: okhttp3.Request) : okhttp3.WebSocket {
        override fun request() = builtRequest
        override fun queueSize(): Long = 0
        override fun send(text: String) = true
        override fun send(bytes: okio.ByteString) = true
        override fun close(code: Int, reason: String?) = true
        override fun cancel() {}
    }

    @Test fun acceptsValidGotifyEventsOnlyWhileListening() {
        val scheduler = java.util.concurrent.ScheduledThreadPoolExecutor(1)
        try {
            val receivers = mutableListOf<okhttp3.WebSocketListener>()
            val sockets = object : okhttp3.WebSocket.Factory {
                override fun newWebSocket(request: okhttp3.Request, listener: okhttp3.WebSocketListener): okhttp3.WebSocket {
                    receivers += listener
                    return StubSocket(request)
                }
            }
            val delivered = mutableListOf<PushNotice>()
            val request = GotifyStreamTransport.buildRequest("https://notifications.le-fil.net", token)!!
            val client = GotifyStreamTransport(request, sockets, scheduler, { delivered += it })
            client.start()
            val event = org.json.JSONObject()
                .put("id", 25).put("appid", 1).put("message", "Private text")
                .put("extras", org.json.JSONObject().put("lefil::notification",
                    org.json.JSONObject().put("source", "agora").put("event_id", "f12")
                        .put("url", "https://agora.le-fil.net/display/12")))
                .toString()
            val fake = StubSocket(request)
            receivers.single().onMessage(fake, event)
            receivers.single().onMessage(fake, "{}")
            assertEquals(1, delivered.size)
            assertEquals("f12", delivered.single().eventId)
            client.stop()
            receivers.single().onMessage(fake, event)
            assertEquals(1, delivered.size)
        } finally {
            scheduler.shutdownNow()
        }
    }

    @Test fun schedulesReconnectOnTransientErrorButNotOnInvalidToken() {
        val scheduler = java.util.concurrent.ScheduledThreadPoolExecutor(1)
        try {
            val receivers = mutableListOf<okhttp3.WebSocketListener>()
            val sockets = object : okhttp3.WebSocket.Factory {
                override fun newWebSocket(request: okhttp3.Request, listener: okhttp3.WebSocketListener): okhttp3.WebSocket {
                    receivers += listener
                    return StubSocket(request)
                }
            }
            val request = GotifyStreamTransport.buildRequest("https://notifications.le-fil.net", token)!!
            val reconnecting = GotifyStreamTransport(request, sockets, scheduler, {})
            reconnecting.start()
            receivers.last().onFailure(StubSocket(request), java.io.IOException("offline"), null)
            assertEquals(1, scheduler.queue.size)
            reconnecting.stop()
            scheduler.queue.clear()
            var unauthorized = 0
            val rejected = GotifyStreamTransport(request, sockets, scheduler, {}, onAuthRequired = { unauthorized++ })
            rejected.start()
            val denied = okhttp3.Response.Builder().request(request)
                .protocol(okhttp3.Protocol.HTTP_1_1).code(401).message("Unauthorized").build()
            receivers.last().onFailure(StubSocket(request), java.io.IOException("unauthorized"), denied)
            assertEquals(1, unauthorized)
            assertTrue(scheduler.queue.isEmpty())
            rejected.stop()
        } finally {
            scheduler.shutdownNow()
        }
    }
}
