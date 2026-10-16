package net.lefil.fdroid

/** Transport and notification delivery stay injectable; a failed poll never clears history. */
internal class MessagePoller(
    private val sources: List<MessageSource>,
    private val tracker: UnreadTracker,
    private val publish: (MessageSource, UnreadItem) -> Boolean
) {
    data class Status(val loginRequired: Boolean, val unavailable: Boolean)

    fun poll(): Status {
        var loginRequired = false
        var unavailable = false
        for (source in sources) {
            when (val result = runCatching { source.fetch() }.getOrDefault(FetchResult.Failed)) {
                is FetchResult.Unread -> {
                    val fresh = tracker.newlyUnread(source.key, result.items).toSet()
                    val accepted = result.items.filter { it !in fresh || publish(source, it) }
                    tracker.remember(source.key, accepted, result.complete)
                }
                FetchResult.LoginRequired -> loginRequired = true
                FetchResult.Failed -> unavailable = true
            }
        }
        return Status(loginRequired, unavailable)
    }
}
