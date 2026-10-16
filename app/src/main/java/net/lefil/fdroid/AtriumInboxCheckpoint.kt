package net.lefil.fdroid

import android.content.SharedPreferences

/**
 * Suivi local des messages deja signales.
 *
 * - Au premier lancement, cree un point de depart sans notifier l'historique.
 * - Au changement de compte, repart proprement sans divulgation inter-comptes.
 * - Ne confirme un message qu'apres sa notification effective par l'appelant.
 *
 * Inactif tant qu'aucun service de notification reel n'est raccorde.
 */
internal class AtriumInboxCheckpoint(private val store: SharedPreferences) {
    private companion object {
        const val KEY_RECIPIENT = "atrium_recipient"
        const val KEY_LAST_PUBLISHED_ID = "atrium_last_published_id"
    }

    fun pending(batch: AtriumInboxBatch): List<AtriumInboxEvent> {
        val previousRecipient = store.getString(KEY_RECIPIENT, null)
        if (previousRecipient != batch.recipient) {
            val lastKnownId = batch.events.lastOrNull()?.id ?: 0L
            // Si l'enregistrement echoue, ne produisons aucune notification.
            store.edit()
                .putString(KEY_RECIPIENT, batch.recipient)
                .putLong(KEY_LAST_PUBLISHED_ID, lastKnownId)
                .commit()
            return emptyList()
        }

        val lastPublishedId = store.getLong(KEY_LAST_PUBLISHED_ID, 0L)
        return batch.events.filter { it.id > lastPublishedId }
    }

    /** A appeler seulement apres emission effective des notifications. */
    fun confirmPublished(batch: AtriumInboxBatch, publishedId: Long): Boolean {
        if (publishedId <= 0L) return false
        if (store.getString(KEY_RECIPIENT, null) != batch.recipient) return false
        if (batch.events.none { it.id == publishedId }) return false

        val current = store.getLong(KEY_LAST_PUBLISHED_ID, 0L)
        if (publishedId <= current) return true
        return store.edit().putLong(KEY_LAST_PUBLISHED_ID, publishedId).commit()
    }
}
