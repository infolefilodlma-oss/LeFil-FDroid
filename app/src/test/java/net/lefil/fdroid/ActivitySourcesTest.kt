package net.lefil.fdroid

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ActivitySourcesTest {
    private fun reply(code: Int = 200, body: String = "") = SessionHttp.Reply(code, body)

    @Test fun agoraPingNotifiesOnlyUnreadActivityWithTrustedLink() {
        val body = JSONObject().put("result", JSONObject().put("notifications",
            org.json.JSONArray()
                .put(JSONObject().put("href", "https://agora.le-fil.net/display/42")
                    .put("timestamp", "123").put("seen", false).put("plaintext", "Marie vous mentionne"))
                .put(JSONObject().put("href", "https://agora.le-fil.net/display/43")
                    .put("timestamp", "124").put("seen", true).put("plaintext", "Déjà lu"))
                .put(JSONObject().put("href", "https://evil.test/item")
                    .put("timestamp", "125").put("seen", false).put("plaintext", "Lien externe"))
        )).toString()
        val result = AgoraActivitySource(SessionTransport { reply(body = body) }).fetch() as FetchResult.Unread
        assertEquals(1, result.items.size)
        assertEquals("https://agora.le-fil.net/display/42", result.items.single().url)
        assertFalse(result.complete)
    }

    @Test fun agoraBadResponseDoesNotPretendEverythingWasRead() {
        assertSame(FetchResult.Failed, AgoraActivitySource(SessionTransport { reply(body = "maintenance") }).fetch())
        assertSame(FetchResult.LoginRequired, AgoraActivitySource(SessionTransport { reply(302) }).fetch())
    }

    @Test fun atriumNotificationsAreIndependentFromTalkAndExcludeSeen() {
        val html = """<a class="d-flex new" href="/index.php?r=notification%2Fentry&id=31" data-notification-id="31"><div>Marie a commenté</div></a><a class="d-flex" href="/x" data-notification-id="32">Déjà lu</a>"""
        val body = JSONObject().put("newNotifications", 1).put("output", html).toString()
        val result = AtriumActivitySource(SessionTransport { reply(body = body) }).fetch() as FetchResult.Unread
        assertEquals(1, result.items.size)
        assertEquals("31", result.items.single().id)
        assertEquals("https://atrium.le-fil.net/index.php?r=notification%2Fentry&id=31", result.items.single().url)
        assertFalse(result.complete)
    }

    @Test fun atriumDoesNotAcknowledgeInvisibleNotifications() {
        val emptyHtml = """<a class="d-flex" data-notification-id="3">Déjà lu</a>"""
        val body = JSONObject().put("newNotifications", 4).put("output", emptyHtml).toString()
        assertSame(FetchResult.Failed, AtriumActivitySource(SessionTransport { reply(body = body) }).fetch())
    }

    @Test fun atriumAcceptsZeroAndRejectsUnknownResponses() {
        val empty = JSONObject().put("newNotifications", 0).put("output", "").toString()
        val result = AtriumActivitySource(SessionTransport { reply(body = empty) }).fetch() as FetchResult.Unread
        assertTrue(result.items.isEmpty())
        assertFalse(result.complete)
        assertSame(FetchResult.Failed, AtriumActivitySource(SessionTransport { reply(body = "{}") }).fetch())
        assertSame(FetchResult.LoginRequired, AtriumActivitySource(SessionTransport { reply(403) }).fetch())
    }
}
