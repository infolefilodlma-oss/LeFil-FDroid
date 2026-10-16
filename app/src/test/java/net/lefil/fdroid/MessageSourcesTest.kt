package net.lefil.fdroid

import org.junit.Assert.*
import org.junit.Test

class MessageSourcesTest {
    private val agoraHtml = """<div class="mail-list-outside-wrapper"><strong><a href="message/12">Marie</a></strong><p class="text-muted">2</p></div>"""
    private val talkHtml = """<a class="messagePreviewEntry unread" data-message-id="31" href="/mail/mail/index?id=31"><h4>Marie <time>12:00</time></h4><div class="mail-last-entry">Bonjour</div></a>"""
    private fun reply(status: Int = 200, body: String = "") = SessionHttp.Reply(status, body)
    private fun talk(vararg replies: SessionHttp.Reply?): FetchResult {
        val queue = replies.toMutableList()
        return AtriumSource(SessionTransport { queue.removeAt(0) }).fetch()
    }
    @Test fun agoraReadsAuthenticatedInbox() {
        val result = AgoraSource(SessionTransport { reply(body = agoraHtml) }).fetch() as FetchResult.Unread
        assertEquals("12", result.items.single().id)
        assertEquals("2", result.items.single().version)
    }
    @Test fun agoraDoesNotTreatAnErrorPageAsEmptyInbox() {
        assertSame(FetchResult.Failed, AgoraSource(SessionTransport { reply(body = "Maintenance") }).fetch())
    }
    @Test fun agoraRequiresLoginForRedirectsAndDeniedAccess() {
        for (code in listOf(302, 401, 403)) {
            assertSame(FetchResult.LoginRequired, AgoraSource(SessionTransport { reply(code) }).fetch())
        }
    }
    @Test fun agoraRecognizesLoginForm() {
        assertSame(FetchResult.LoginRequired, AgoraSource(SessionTransport { reply(body = """<input type="password">""") }).fetch())
    }
    @Test fun agoraNetworkFailureIsNotAnEmptyInbox() {
        assertSame(FetchResult.Failed, AgoraSource(SessionTransport { null }).fetch())
    }
    @Test fun talkUsesHumHubSessionRoutes() {
        val urls = mutableListOf<String>()
        val result = AtriumSource(SessionTransport {
            urls += it
            if (it.endsWith("notification-list")) reply(body = talkHtml) else reply(body = """{"newMessages":1}""")
        }).fetch() as FetchResult.Unread
        assertEquals("31", result.items.single().id)
        assertEquals(listOf("https://team.le-fil.net/mail/mail/notification-list", "https://team.le-fil.net/mail/mail/get-new-message-count-json"), urls)
    }
    @Test fun talkFallsBackToIndexPhpRouting() {
        assertEquals(1, (talk(reply(404), reply(body = talkHtml), reply(body = """{"newMessages":1}""")) as FetchResult.Unread).items.size)
    }
    @Test fun talkAcceptsJsonWrappedHtml() {
        val json = org.json.JSONObject().put("html", talkHtml).toString()
        assertEquals("31", (talk(reply(body = json), reply(body = """{"newMessages":1}""")) as FetchResult.Unread).items.single().id)
    }
    @Test fun talkDoesNotAssignACounterToArbitraryConversations() {
        assertTrue((talk(reply(body = talkHtml), reply(body = """{"newMessages":0}""")) as FetchResult.Unread).items.isEmpty())
    }
    @Test fun talkRejectsMissingOrMalformedCounters() {
        for (count in listOf(null, reply(500), reply(body = "error"))) {
            assertSame(FetchResult.Failed, talk(reply(body = talkHtml), count))
        }
    }
    @Test fun talkRecognizesLoginWithoutResettingUnreadState() {
        assertSame(FetchResult.LoginRequired, talk(reply(302)))
        assertSame(FetchResult.LoginRequired, talk(reply(body = """<form><input type="password"></form>""")))
    }
    @Test fun talkRejectsUnknownResponses() {
        assertSame(FetchResult.Failed, talk(reply(body = "maintenance")))
        assertSame(FetchResult.Failed, talk(reply(body = "{}")))
    }
    @Test fun talkAcceptsConfirmedEmptyState() {
        assertTrue((talk(reply(body = "<div>There are no messages yet.</div>"), reply(body = """{"newMessages":0}""")) as FetchResult.Unread).items.isEmpty())
    }

    @Test fun talkMarksTruncatedListsAsIncomplete() {
        assertFalse((talk(reply(body = talkHtml), reply(body = """{"newMessages":6}""")) as FetchResult.Unread).complete)
    }
    @Test fun emptyAgoraInboxHasTheComposeHeader() {
        val result = AgoraSource(SessionTransport { reply(body = """<div id="message-new"><a href="/message/new">New message</a></div>""") }).fetch()
        assertTrue((result as FetchResult.Unread).items.isEmpty())
        assertFalse(result.complete)
    }
}
