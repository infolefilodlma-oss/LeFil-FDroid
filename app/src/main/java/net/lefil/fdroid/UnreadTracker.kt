package net.lefil.fdroid

/**
 * Mémorise, par messagerie, l'état des conversations non lues déjà signalées.
 *
 * Une notification part quand une conversation DEVIENT non lue, ou quand
 * elle reçoit un nouveau message alors qu'elle l'était déjà (son empreinte
 * change). Lue sur le site, elle sort de la liste. Rien n'est perdu : tant
 * qu'un message n'est pas lu, le serveur le garde « non lu ».
 *
 * Code Kotlin pur : le stockage est fourni par l'appelant.
 */
internal class UnreadTracker(
    private val load: (String) -> Set<String>,
    private val save: (String, Set<String>) -> Unit
) {
    private fun mark(item: UnreadItem) = item.id + "|" + item.version

    /** Conversations à notifier : nouvelles, ou avec un nouveau message. */
    fun newlyUnread(sourceKey: String, current: List<UnreadItem>): List<UnreadItem> {
        val known = load(sourceKey)
        return current.filter { mark(it) !in known }
    }

    /** À appeler après affichage effectif des notifications. */
    fun remember(sourceKey: String, current: List<UnreadItem>) {
        save(sourceKey, current.map { mark(it) }.toSet())
    }
}
