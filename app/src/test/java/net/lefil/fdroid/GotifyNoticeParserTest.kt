package net.lefil.fdroid

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class GotifyNoticeParserTest {
    private fun event(
        source: String = "talk",
        id: String = "message-123",
        url: String = "https://atrium.le-fil.net/index.php?r=mail%2Fmail%2Findex&id=123",
        appId: Int = 7
    ): String = JSONObject()
        .put("id", 981)
        .put("appid", appId)
        .put("message", "Texte privé qui ne doit pas apparaître sur écran verrouillé")
        .put("title", "Message")
        .put("extras", JSONObject().put("lefil::notification", JSONObject()
            .put("source", source).put("event_id", id).put("url", url)))
        .toString()

    @Test fun acceptsTalkMessageWithoutExposingItsBody() {
        val n = GotifyNoticeParser.parse(event())!!
        assertEquals("talk", n.source)
        assertEquals("message-123", n.eventId)
        assertEquals(981L, n.gotifyMessageId)
        assertEquals("Nouveau message dans Talk", n.label)
        assertFalse(n.label.contains("Texte privé"))
    }

    @Test fun acceptsEachServiceOnItsOwnHost() {
        val agora = GotifyNoticeParser.parse(event("agora", "a1", "https://agora.le-fil.net/display/42"))
        val atrium = GotifyNoticeParser.parse(event("atrium", "b2", "https://atrium.le-fil.net/index.php?r=notification%2Fentry&id=1"))
        assertNotNull(agora)
        assertNotNull(atrium)
    }

    @Test fun rejectsExternalAndCrossServiceLinks() {
        assertNull(GotifyNoticeParser.parse(event(url = "https://evil.test/")))
        assertNull(GotifyNoticeParser.parse(event(source = "agora", url = "https://atrium.le-fil.net/")))
        assertNull(GotifyNoticeParser.parse(event(url = "https://atrium.le-fil.net.evil.test/")))
        assertNull(GotifyNoticeParser.parse(event(url = "http://atrium.le-fil.net/")))
    }

    @Test fun rejectsMalformedAndUnexpectedEvents() {
        assertNull(GotifyNoticeParser.parse("{}"))
        assertNull(GotifyNoticeParser.parse("not json"))
        assertNull(GotifyNoticeParser.parse(event(source = "unknown")))
        assertNull(GotifyNoticeParser.parse(event(id = "")))
        assertNull(GotifyNoticeParser.parse(event(id = "x".repeat(129))))
        assertNull(GotifyNoticeParser.parse(event(appId = 0)))
        assertNull(GotifyNoticeParser.parse("x".repeat(20_000)))
        assertNull(GotifyNoticeParser.parse(JSONObject().put("id", 1).put("appid", 2).put("message", "test").toString()))
    }
}
