package net.lefil.fdroid

/**
 * Comme MessagePoller, mais une seule notification regroupe tous les
 * messages/activités nouvellement non lus de ce cycle (toutes sources
 * confondues). Un échec d'envoi ne marque rien comme vu : tout est retenté
 * au cycle suivant.
 */
internal class MessageBatchPoller(
    private val sources: List<MessageSource>,
    private val tracker: UnreadTracker,
    private val publish: (List<Pair<MessageSource, UnreadItem>>) -> Boolean
) {
    data class Status(val loginRequired: Boolean, val unavailable: Boolean, val notified: Int)

    fun poll(): Status {
        var loginRequired = false
        var unavailable = false
        val perSource = mutableListOf<Triple<MessageSource, FetchResult.Unread, List<UnreadItem>>>()
        val fresh = mutableListOf<Pair<MessageSource, UnreadItem>>()
        for (source in sources) {
            when (val result = runCatching { source.fetch() }.getOrDefault(FetchResult.Failed)) {
                is FetchResult.Unread -> {
                    val sourceFresh = tracker.newlyUnread(source.key, result.items)
                    fresh += sourceFresh.map { source to it }
                    perSource += Triple(source, result, sourceFresh)
                }
                FetchResult.LoginRequired -> loginRequired = true
                FetchResult.Failed -> unavailable = true
            }
        }
        val success = fresh.isEmpty() || publish(fresh)
        for ((source, result, sourceFresh) in perSource) {
            val freshSet = sourceFresh.toSet()
            val accepted = result.items.filter { it !in freshSet || success }
            tracker.remember(source.key, accepted, result.complete)
        }
        return Status(loginRequired, unavailable, if (success) fresh.size else 0)
    }
}
