package net.lefil.fdroid

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Verifie la chaine complete sur Android sans aucun acces au VPS.
 * La reponse JSON et le cookie sont fictifs ; les notifications sont interceptees.
 */
@RunWith(AndroidJUnit4::class)
class AtriumInboxBridgeTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    private class FakeResponse(url: URL, private val data: String) : HttpURLConnection(url) {
        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy() = false
        override fun getResponseCode() = 200
        override fun getContentType() = "application/json"
        override fun getInputStream(): InputStream =
            ByteArrayInputStream(data.toByteArray(Charsets.UTF_8))
    }

    @Test fun cookieEtReponseSontReliesALaNotificationSansDoublon() {
        val prefs = context.getSharedPreferences("lefil_atrium_inbox", Context.MODE_PRIVATE)
        assertTrue(prefs.edit().clear().commit())
        try {
            var body = """{"recipient":"compte-test","events":[{"id":31}]}"""
            var authenticatedCalls = 0
            val session = AtriumSessionClient(
                readCookies = {
                    authenticatedCalls++
                    "humhub_session=fiction"
                },
                openConnection = { FakeResponse(it, body) }
            )
            val published = mutableListOf<Long>()
            val bridge = AtriumInboxBridge(context, session) { event ->
                published.add(event.id)
                true
            }
            val endpoint = "https://team.le-fil.net/messenger-contacts/notification"
            assertEquals(AtriumInboxSync.Result.Success(0), bridge.syncOnce(endpoint))
            body = """{"recipient":"compte-test","events":[{"id":31},{"id":32}]}"""
            assertEquals(AtriumInboxSync.Result.Success(1), bridge.syncOnce(endpoint))
            assertEquals(AtriumInboxSync.Result.Success(0), bridge.syncOnce(endpoint))
            assertEquals(listOf(32L), published)
            assertEquals(3, authenticatedCalls)
        } finally {
            prefs.edit().clear().commit()
        }
    }

    @Test fun centQuaranteMessagesSontRecuperesEnDeuxLotsSansPerte() {
        val prefs = context.getSharedPreferences("lefil_atrium_inbox", Context.MODE_PRIVATE)
        assertTrue(prefs.edit().clear().commit())
        try {
            var lastAvailable = 100
            val requestedAfter = mutableListOf<Int>()
            val session = AtriumSessionClient(
                readCookies = { "humhub_session=fiction" },
                openConnection = { url ->
                    val after = url.query.substringAfter("after=").toInt()
                    requestedAfter.add(after)
                    val ids = if (after == 0) {
                        ((lastAvailable - 99)..lastAvailable).toList()
                    } else {
                        ((after + 1)..minOf(after + 100, lastAvailable)).toList()
                            .filter { it <= lastAvailable }
                    }
                    val events = ids.joinToString(",") { """{"id":$it}""" }
                    FakeResponse(url, """{"recipient":"compte-test","events":[$events]}""")
                }
            )
            val published = mutableListOf<Long>()
            val bridge = AtriumInboxBridge(context, session) {
                published.add(it.id)
                true
            }
            val endpoint = "https://team.le-fil.net/index.php?r=messenger-contacts%2Fle-fil-notification%2Findex"

            assertEquals(AtriumInboxSync.Result.Success(0), bridge.syncOnce(endpoint))
            lastAvailable = 240
            assertEquals(AtriumInboxSync.Result.Success(100), bridge.syncOnce(endpoint))
            assertEquals(AtriumInboxSync.Result.Success(40), bridge.syncOnce(endpoint))
            assertEquals(listOf(0, 100, 200), requestedAfter)
            assertEquals((101L..240L).toList(), published)
        } finally {
            prefs.edit().clear().commit()
        }
    }

    @Test fun endpointAvecCurseurInjecteEstRefuseSansReseau() {
        val prefs = context.getSharedPreferences("lefil_atrium_inbox", Context.MODE_PRIVATE)
        assertTrue(prefs.edit().clear().commit())
        try {
            var opened = false
            val session = AtriumSessionClient(
                readCookies = { "humhub_session=fiction" },
                openConnection = { opened = true; FakeResponse(it, "{}") }
            )
            val bridge = AtriumInboxBridge(context, session) { true }
            val result = bridge.syncOnce("https://team.le-fil.net/index.php?after=500")
            assertTrue(result === AtriumInboxSync.Result.TransportError)
            assertTrue(!opened)
        } finally {
            prefs.edit().clear().commit()
        }
    }

    @Test fun endpointNonAutoriseNePublieAucunMessage() {
        val prefs = context.getSharedPreferences("lefil_atrium_inbox", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        try {
            var opened = false
            val session = AtriumSessionClient(
                readCookies = { "session=fiction" },
                openConnection = { opened = true; FakeResponse(it, "{}") }
            )
            var published = 0
            val bridge = AtriumInboxBridge(context, session) { published++; true }
            val result = bridge.syncOnce("https://intrus.example/notifications")
            assertTrue(result === AtriumInboxSync.Result.TransportError)
            assertEquals(0, published)
            assertTrue(!opened)
        } finally {
            prefs.edit().clear().commit()
        }
    }
}
