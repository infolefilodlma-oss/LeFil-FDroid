package net.lefil.fdroid

import org.junit.Assert.*
import org.junit.Test

class MessagePollerTest {
    private val store = mutableMapOf<String, Set<String>>()
    private val tracker = UnreadTracker({ store[it] ?: emptySet() }, { key, value -> store[key] = value })
    private val item = UnreadItem("31", "Marie", "https://team.le-fil.net/mail/mail/index?id=31", "Bonjour")
    private var result: FetchResult = FetchResult.Unread(listOf(item))
    private val source = object : MessageSource {
        override val key = "atrium"
        override val displayName = "Talk"
        override fun fetch() = result
    }
    @Test fun postsOnlyOnceForSameVersion() {
        var posted = 0
        val poller = MessagePoller(listOf(source), tracker) { _, _ -> posted++; true }
        poller.poll(); poller.poll()
        assertEquals(1, posted)
        result = FetchResult.Unread(listOf(item.copy(version = "Nouveau")))
        poller.poll()
        assertEquals(2, posted)
    }
    @Test fun retriesWhenNotificationIsDenied() {
        var attempts = 0
        val poller = MessagePoller(listOf(source), tracker) { _, _ -> ++attempts > 1 }
        poller.poll()
        assertTrue(tracker.newlyUnread("atrium", listOf(item)).isNotEmpty())
        poller.poll(); poller.poll()
        assertEquals(2, attempts)
    }
    @Test fun networkAndLoginFailuresPreserveHistory() {
        val poller = MessagePoller(listOf(source), tracker) { _, _ -> true }
        poller.poll()
        val before = store.toMap()
        result = FetchResult.Failed
        assertTrue(poller.poll().unavailable)
        assertEquals(before, store)
        result = FetchResult.LoginRequired
        assertTrue(poller.poll().loginRequired)
        assertEquals(before, store)
    }
    @Test fun confirmedReadRemovesKnownUnreadState() {
        val poller = MessagePoller(listOf(source), tracker) { _, _ -> true }
        poller.poll()
        result = FetchResult.Unread(emptyList())
        poller.poll()
        assertEquals(listOf(item), tracker.newlyUnread("atrium", listOf(item)))
    }
    @Test fun truncatedInboxDoesNotReNotifyPreviouslyHiddenConversation() {
        val poller = MessagePoller(listOf(source), tracker) { _, _ -> true }
        poller.poll()
        result = FetchResult.Unread(emptyList(), complete = false)
        poller.poll()
        assertTrue(tracker.newlyUnread("atrium", listOf(item)).isEmpty())
    }
    @Test fun sourceKeysDoNotShareHistory() {
        tracker.remember("agora", listOf(item))
        assertEquals(listOf(item), tracker.newlyUnread("atrium", listOf(item)))
    }
    @Test fun historySurvivesPollerRecreation() {
        MessagePoller(listOf(source), tracker) { _, _ -> true }.poll()
        val restored = UnreadTracker({ store[it] ?: emptySet() }, { key, value -> store[key] = value })
        var posts = 0
        MessagePoller(listOf(source), restored) { _, _ -> posts++; true }.poll()
        assertEquals(0, posts)
    }
    @Test fun thrownTransportFailureDoesNotBlockTheOtherService() {
        val broken = object : MessageSource {
            override val key = "agora"
            override val displayName = "Agora"
            override fun fetch(): FetchResult = error("network")
        }
        var posts = 0
        val status = MessagePoller(listOf(broken, source), tracker) { _, _ -> posts++; true }.poll()
        assertTrue(status.unavailable)
        assertEquals(1, posts)
    }
    @Test fun replacingVersionInPartialListDoesNotGrowHistoryForSameConversation() {
        tracker.remember("atrium", listOf(item), complete = false)
        tracker.remember("atrium", listOf(item.copy(version = "Suivant")), complete = false)
        assertEquals(1, store.getValue("atrium").size)
    }
}
