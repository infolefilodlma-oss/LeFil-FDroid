package net.lefil.fdroid

import android.content.Context

/**
 * Assemble les composants deja valides : session HumHub du WebView,
 * reception d'evenements, suivi local et notification native Android.
 *
 * Aucun appel n'est lance automatiquement. Un controleur pourra appeler
 * syncOnce() hors du fil principal, UNIQUEMENT apres confirmation de l'URL
 * du point d'acces prive HumHub et du mecanisme de declenchement.
 */
internal class AtriumInboxBridge(
    context: Context,
    private val session: AtriumSessionClient = AtriumSessionClient(),
    private val publish: (AtriumInboxEvent) -> Boolean =
        AtriumNotificationPublisher(context.applicationContext)::publish
) {
    private val checkpoint = AtriumInboxCheckpoint(
        context.applicationContext.getSharedPreferences(
            "lefil_atrium_inbox",
            Context.MODE_PRIVATE
        )
    )

    @Synchronized
    fun syncOnce(verifiedEndpointUrl: String): AtriumInboxSync.Result {
        return AtriumInboxSync(
            fetch = { session.get(verifiedEndpointUrl) },
            checkpoint = checkpoint,
            publish = publish
        ).runOnce()
    }
}
