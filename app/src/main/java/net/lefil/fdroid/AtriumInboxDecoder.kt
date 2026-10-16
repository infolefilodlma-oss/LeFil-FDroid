package net.lefil.fdroid

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Contrat minimal PREPARATOIRE pour les notifications privees Atrium.
 *
 * Ce decodeur ne contacte aucun serveur et ne declenche aucune notification.
 * Le schema final devra etre confirme avec LeFilNotificationController.php
 * (actuellement hors production) AVANT tout raccordement reseau.
 *
 * Format attendu : {"recipient":"GUID", "events":[{"id":123}, ...]}.
 * Aucun contenu de message prive n'est lu ni conserve.
 */
internal data class AtriumInboxEvent(val id: Long)

internal data class AtriumInboxBatch(
    val recipient: String,
    val events: List<AtriumInboxEvent>
)

internal object AtriumInboxDecoder {
    private const val MAX_EVENTS = 100
    private const val MAX_RECIPIENT_LENGTH = 128

    fun decode(body: String): AtriumInboxBatch? {
        return try {
            val document = JSONObject(body)
            val recipient = document.optString("recipient", "").trim()
            if (recipient.isEmpty() || recipient.length > MAX_RECIPIENT_LENGTH) return null

            val events: JSONArray = document.optJSONArray("events") ?: return null
            if (events.length() > MAX_EVENTS) return null

            val seen = HashSet<Long>()
            val parsed = ArrayList<AtriumInboxEvent>(events.length())
            for (index in 0 until events.length()) {
                val obj = events.optJSONObject(index) ?: return null
                val rawId = obj.opt("id") ?: return null
                val id = when (rawId) {
                    is Int -> rawId.toLong()
                    is Long -> rawId
                    is String -> {
                        if (!rawId.matches(Regex("[1-9][0-9]{0,17}"))) return null
                        rawId.toLongOrNull() ?: return null
                    }
                    else -> return null
                }
                if (id <= 0L || !seen.add(id)) return null
                parsed.add(AtriumInboxEvent(id))
            }
            AtriumInboxBatch(recipient, parsed.sortedBy { it.id })
        } catch (_: JSONException) {
            null
        }
    }
}
