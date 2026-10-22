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
}
