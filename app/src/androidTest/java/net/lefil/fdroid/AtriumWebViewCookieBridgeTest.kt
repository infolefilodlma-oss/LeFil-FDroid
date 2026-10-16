package net.lefil.fdroid

import android.webkit.CookieManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Verifie sur Android le pont CookieManager(WebView) -> requete HTTPS.
 * Ne contacte pas le serveur Atrium et ne demande aucun compte.
 * Le cookie de test est supprime apres le controle.
 */
@RunWith(AndroidJUnit4::class)
class AtriumWebViewCookieBridgeTest {
    private val site = "https://team.le-fil.net/"
    private val endpoint = "https://team.le-fil.net/messenger-contacts/notification"
    private val cookieName = "lefil_android_bridge_probe"

    private class FakeJsonConnection(url: URL) : HttpURLConnection(url) {
        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy() = false
        override fun getResponseCode() = 200
        override fun getContentType() = "application/json"
        override fun getInputStream(): InputStream =
            ByteArrayInputStream("{\"recipient\":\"test\",\"events\":[]}".toByteArray())
    }

    @Test
    fun laSessionWebViewEstTransmiseAuTransportSansReseau() {
        val manager = CookieManager.getInstance()
        manager.setCookie(site, "$cookieName=ok; Secure; Path=/")
        manager.flush()
        try {
            val stored = manager.getCookie(endpoint).orEmpty()
            assertTrue("Cookie WebView non conserve", stored.contains("$cookieName=ok"))

            lateinit var connection: FakeJsonConnection
            val transport = AtriumSessionClient(openConnection = {
                FakeJsonConnection(it).also { fake -> connection = fake }
            })
            val result = transport.get(endpoint)

            assertTrue("Reponse JSON attendue", result is AtriumSessionClient.Result.Json)
            assertTrue(
                "Cookie WebView non transmis au transport HTTP",
                connection.getRequestProperty("Cookie").orEmpty().contains("$cookieName=ok")
            )
            assertTrue("Redirections non autorisees", !connection.instanceFollowRedirects)
        } finally {
            manager.setCookie(site, "$cookieName=; Max-Age=0; Secure; Path=/")
            manager.flush()
        }
    }
}
