package net.lefil.fdroid

import org.json.JSONObject

/**
 * Strict validation of a Gotify MessageExternal JSON event.
 * Contract: extras["lefil::notification"] = {
 *   "source": "agora" | "atrium" | "talk",
 *   "event_id": "<stable opaque id>",
 *   "url": "<HTTPS deep link for that service>"
 * }
 * This file neither connects to Gotify nor stores a client token.
 * Reference: https://github.com/gotify/server/blob/master/model/message.go
 */
internal data class PushNotice(
    val source: String,
    val eventId: String,
    val url: String,
    val label: String,
    val gotifyMessageId: Long
)

internal object GotifyNoticeParser {
    private val eventIdPattern = Regex("[A-Za-z0-9._:-]{1,128}")

    fun parse(json: String): PushNotice? = runCatching {
        if (json.length > 16_384) return null
        val root = JSONObject(json)
        val id = root.optLong("id", 0L)
        val appId = root.optLong("appid", 0L)
        if (id <= 0 || appId <= 0) return null
        val extras = root.optJSONObject("extras") ?: return null
        val details = extras.optJSONObject("lefil::notification") ?: return null
        val source = details.optString("source")
        val eventId = details.optString("event_id")
        val url = details.optString("url")
        if (source !in setOf("agora", "atrium", "talk") ||
            !eventIdPattern.matches(eventId) ||
            !MessageLinks.trusted(url)) return null

        // One service must not be able to forge a navigation link to another.
        val expectedHost = if (source == "agora") "agora.le-fil.net" else "atrium.le-fil.net"
        if (java.net.URI(url).host != expectedHost) return null

        // Do not leak private message bodies into the notification on a lock screen.
        val label = when (source) {
            "agora" -> "Nouvelle activité sur Agora"
            "atrium" -> "Nouvelle activité sur Atrium"
            else -> "Nouveau message dans Talk"
        }
        PushNotice(source, eventId, url, label, id)
    }.getOrNull()
}
