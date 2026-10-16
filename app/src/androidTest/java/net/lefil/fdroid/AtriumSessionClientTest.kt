package net.lefil.fdroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/** Tests du transport HTTP, sans serveur distant ni compte personnel. */
class AtriumSessionClientTest {
    private class StubConnection(
        url: URL,
        private val status: Int = 200,
        private val type: String = "application/json; charset=utf-8",
        private val body: String = "{\"recipient\":\"test\",\"events\":[]}"
    ) : HttpURLConnection(url) {
        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy(): Boolean = false
        override fun getResponseCode(): Int = status
        override fun getContentType(): String = type
        override fun getInputStream(): InputStream = ByteArrayInputStream(body.toByteArray())
    }

    @Test fun unAutreDomaineEstRefuseAvantTouteLectureDesCookies() {
        var cookiesRead = false
        var connectionsOpened = false
        val client = AtriumSessionClient(
            readCookies = { cookiesRead = true; "session=secret" },
            openConnection = { connectionsOpened = true; StubConnection(it) }
        )
        assertTrue(client.get("https://other.example/notice") === AtriumSessionClient.Result.InvalidUrl)
        assertTrue(client.get("http://team.le-fil.net/notice") === AtriumSessionClient.Result.InvalidUrl)
        assertTrue(client.get("https://team.le-fil.net.attacker.example/notice") === AtriumSessionClient.Result.InvalidUrl)
        assertFalse(cookiesRead)
        assertFalse(connectionsOpened)
    }

    @Test fun sansSessionAucuneConnexionEstLancee() {
        var opened = false
        val client = AtriumSessionClient(
            readCookies = { null },
            openConnection = { opened = true; StubConnection(it) }
        )
        assertTrue(client.get("https://team.le-fil.net/messenger-contacts/notification") === AtriumSessionClient.Result.LoginRequired)
        assertFalse(opened)
    }

    @Test fun laSessionAtriumEstTransmiseSansAutoriserLesRedirections() {
        lateinit var connection: StubConnection
        val client = AtriumSessionClient(
            readCookies = { "session=private" },
            openConnection = { StubConnection(it).also { conn -> connection = conn } }
        )
        val result = client.get("https://team.le-fil.net/messenger-contacts/notification")
        assertTrue(result is AtriumSessionClient.Result.Json)
        assertEquals("session=private", connection.getRequestProperty("Cookie"))
        assertEquals("application/json", connection.getRequestProperty("Accept"))
        assertFalse(connection.instanceFollowRedirects)
    }

    @Test fun redirectionDeConnexionExigeUneNouvelleSession() {
        val client = AtriumSessionClient(
            readCookies = { "session=expired" },
            openConnection = { StubConnection(it, status = 302) }
        )
        assertTrue(client.get("https://team.le-fil.net/messenger-contacts/notification") === AtriumSessionClient.Result.LoginRequired)
    }

    @Test fun pageHtmlOuReponseTropGrandeEstRefusee() {
        val html = AtriumSessionClient(
            readCookies = { "session=valid" },
            openConnection = { StubConnection(it, type = "text/html") }
        )
        assertTrue(html.get("https://team.le-fil.net/notice") === AtriumSessionClient.Result.InvalidResponse)
        val huge = AtriumSessionClient(
            readCookies = { "session=valid" },
            openConnection = { StubConnection(it, body = "x".repeat(65_537)) }
        )
        assertTrue(huge.get("https://team.le-fil.net/notice") === AtriumSessionClient.Result.InvalidResponse)
    }
}
