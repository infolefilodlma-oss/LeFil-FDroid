package net.lefil.fdroid

import android.content.Context
import android.webkit.CookieManager
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.SecureRandom

/** POST to a Le Fil service with the member's own WebView session; never follows redirects. */
internal fun interface EnrollTransport {
    fun post(url: String, device: String): SessionHttp.Reply?
}

internal class SessionEnrollTransport : EnrollTransport {
    override fun post(url: String, device: String): SessionHttp.Reply? {
        val u = URL(url)
        if (u.protocol != "https" || u.host !in setOf("agora.le-fil.net", "atrium.le-fil.net")) return null
        val cookie = CookieManager.getInstance().getCookie(url) ?: return null
        var c: HttpURLConnection? = null
        return try {
            c = (u.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                instanceFollowRedirects = false
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Cookie", cookie)
                // Mandatory on the server: a foreign web page cannot send this header cross-site.
                setRequestProperty("X-LeFil-App", "enroll")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }
            c.outputStream.use { it.write(("device=" + URLEncoder.encode(device, "UTF-8")).toByteArray()) }
            val status = c.responseCode
            val text = if (status in 200..299) c.inputStream.bufferedReader().use { it.readText() } else ""
            SessionHttp.Reply(status, text)
        } catch (_: Exception) { null } finally { c?.disconnect() }
    }
}

internal object GotifyEnrollment {
    private val devicePattern = Regex("[a-f0-9]{64}")

    /** Random per-installation identifier; it identifies a phone, never a person. */
    fun deviceId(context: Context): String {
        val prefs = context.getSharedPreferences("lefil_push", Context.MODE_PRIVATE)
        prefs.getString("device_id", null)?.takeIf(devicePattern::matches)?.let { return it }
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val id = bytes.joinToString("") { "%02x".format(it) }
        prefs.edit().putString("device_id", id).commit()
        return id
    }

    /** Only a 200 JSON answer with a valid le-fil.net HTTPS server and token is accepted. */
    fun parse(reply: SessionHttp.Reply?): PushCredentials? = runCatching {
        if (reply == null || reply.status != 200 || reply.body.length > 4096) return null
        val json = JSONObject(reply.body)
        val server = json.optString("server")
        val token = json.optString("client_token")
        if (GotifyStreamTransport.buildRequest(server, token) == null) return null
        PushCredentials(server, token)
    }.getOrNull()

    fun enroll(realm: PushRealm, device: String, transport: EnrollTransport): PushCredentials? =
        parse(runCatching { transport.post(realm.enrollUrl, device) }.getOrNull())
}
