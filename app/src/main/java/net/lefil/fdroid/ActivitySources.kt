package net.lefil.fdroid

import org.json.JSONObject
import java.security.MessageDigest

/** Sources de notifications natives des serveurs, distinctes des messageries privées. */
internal object ActivityParsers {
    private fun fingerprint(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
            .take(12).joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }

    fun agora(body: String): FetchResult {
        return try {
        val data = JSONObject(body).optJSONObject("result") ?: return FetchResult.Failed
        val notifications = data.optJSONArray("notifications") ?: return FetchResult.Failed
        val found = linkedMapOf<String, UnreadItem>()
        for (i in 0 until notifications.length()) {
            val n = notifications.optJSONObject(i) ?: continue
            val seen = when (val flag = n.opt("seen")) {
                is Boolean -> flag
                is Number -> flag.toInt() != 0
                is String -> flag == "1" || flag.equals("true", ignoreCase = true)
                else -> true
            }
            if (seen) continue
            val url = n.optString("href")
            if (!MessageLinks.trusted(url) || !url.startsWith("https://agora.le-fil.net/")) continue
            val timestamp = n.optString("timestamp")
            if (timestamp.isBlank()) continue
            val label = n.optString("plaintext").ifBlank { "Nouvelle activité sur Agora" }
            val id = fingerprint(url + "|" + timestamp + "|" + label)
            found[id] = UnreadItem(id, label.take(120), url)
        }
        // Ping does not guarantee an exhaustive notification history.
        FetchResult.Unread(found.values.toList(), complete = false)
        } catch (_: Exception) { FetchResult.Failed }
    }

    private val entry = Regex("""<a\b([^>]*\bdata-notification-id=["'](\d+)["'][^>]*)>([\s\S]*?)</a>""", RegexOption.IGNORE_CASE)
    private val classes = Regex("""\bclass=["']([^"']*)["']""", RegexOption.IGNORE_CASE)

    fun atrium(body: String): FetchResult {
        return try {
        val data = JSONObject(body)
        val count = data.optInt("newNotifications", -1)
        if (count < 0 || !data.has("output") || data.isNull("output")) return FetchResult.Failed
        if (count == 0) return FetchResult.Unread(emptyList(), complete = false)
        val html = data.optString("output")
        val found = linkedMapOf<String, UnreadItem>()
        for (m in entry.findAll(html)) {
            val attrs = m.groupValues[1]
            val classNames = classes.find(attrs)?.groupValues?.get(1)?.split(Regex("""\s+""")).orEmpty()
            if ("new" !in classNames) continue
            val id = m.groupValues[2]
            val label = UnreadParsers.cleanText(m.groupValues[3]).ifBlank { "Nouvelle activité sur Atrium" }
            val url = "https://atrium.le-fil.net/index.php?r=notification%2Fentry&id=" + id
            found[id] = UnreadItem(id, label.take(120), url)
        }
        if (found.isEmpty()) FetchResult.Failed
        else FetchResult.Unread(found.values.toList(), complete = false)
        } catch (_: Exception) { FetchResult.Failed }
    }
}

internal class AgoraActivitySource(private val http: SessionTransport = SessionHttp()): MessageSource {
    override val key = "agora_activity"
    override val displayName = "Agora"
    override val alertLabel = "nouvelle activité"
    override fun fetch(): FetchResult {
        val r = http.get("https://agora.le-fil.net/ping") ?: return FetchResult.Failed
        if (r.status in 300..399 || r.status == 401 || r.status == 403 ||
            UnreadParsers.isLoginPage(r.body)) return FetchResult.LoginRequired
        if (r.status != 200) return FetchResult.Failed
        return ActivityParsers.agora(r.body)
    }
}

internal class AtriumActivitySource(private val http: SessionTransport = SessionHttp()): MessageSource {
    override val key = "atrium_activity"
    override val displayName = "Atrium"
    override val alertLabel = "nouvelle activité"
    override fun fetch(): FetchResult {
        val r = http.get("https://atrium.le-fil.net/index.php?r=notification%2Flist%2Findex")
            ?: return FetchResult.Failed
        if (r.status in 300..399 || r.status == 401 || r.status == 403 ||
            UnreadParsers.isLoginPage(r.body)) return FetchResult.LoginRequired
        if (r.status != 200) return FetchResult.Failed
        return ActivityParsers.atrium(r.body)
    }
}
