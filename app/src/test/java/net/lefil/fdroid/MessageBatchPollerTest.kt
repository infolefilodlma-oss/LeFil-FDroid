package net.lefil.fdroid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

private fun source(key: String, result: FetchResult) = object : MessageSource {
    override val key = key
    override val displayName = key
    override fun fetch(): FetchResult = result
}

private fun tracker(): UnreadTracker {
    val store = mutableMapOf<String, Set<String>>()
    return UnreadTracker(load = { store[it] ?: emptySet() }, save = { k, v -> store[k] = v })
}

class MessageBatchPollerTest {
    @Test fun aggregatesFreshItemsFromEverySourceIntoOnePublishCall() {
        val a = source("agora", FetchResult.Unread(listOf(UnreadItem("1", "a", "https://agora.le-fil.net/message/1"))))
        val b = source("atrium", FetchResult.Unread(listOf(UnreadItem("2", "b", "https://atrium.le-fil.net/index.php?r=mail%2Fmail%2Findex&id=2"))))
        var published: List<Pair<MessageSource, UnreadItem>>? = null
        val status = MessageBatchPoller(listOf(a, b), tracker()) { published = it; true }.poll()
        assertEquals(2, published?.size)
        assertEquals(2, status.notified)
    }

    @Test fun alreadyKnownItemsAreNotRepublished() {
        val t = tracker()
        val item = UnreadItem("1", "a", "https://agora.le-fil.net/message/1")
        val a = source("agora", FetchResult.Unread(listOf(item)))
        MessageBatchPoller(listOf(a), t) { true }.poll()
        var publishCalled = false
        val status = MessageBatchPoller(listOf(a), t) { publishCalled = true; true }.poll()
        assertFalse(publishCalled)
        assertEquals(0, status.notified)
    }

    @Test fun failedPublishKeepsFreshItemsEligibleForRetry() {
        val t = tracker()
        val item = UnreadItem("1", "a", "https://agora.le-fil.net/message/1")
        val a = source("agora", FetchResult.Unread(listOf(item)))
        MessageBatchPoller(listOf(a), t) { false }.poll()
        var retryPublished: List<Pair<MessageSource, UnreadItem>>? = null
        MessageBatchPoller(listOf(a), t) { retryPublished = it; true }.poll()
        assertEquals(1, retryPublished?.size)
    }

    @Test fun loginRequiredAndUnavailableAreReportedWithoutPublishing() {
        val a = source("agora", FetchResult.LoginRequired)
        val b = source("atrium", FetchResult.Failed)
        var called = false
        val status = MessageBatchPoller(listOf(a, b), tracker()) { called = true; true }.poll()
        assertFalse(called)
        assertEquals(true, status.loginRequired)
        assertEquals(true, status.unavailable)
    }
}
