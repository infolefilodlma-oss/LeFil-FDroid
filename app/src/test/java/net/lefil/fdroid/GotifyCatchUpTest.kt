package net.lefil.fdroid

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class GotifyCatchUpTest {
    private fun message(id: Long, event: String, source: String = "agora", host: String = "agora.le-fil.net") =
        JSONObject().put("id", id).put("appid", 3).put("message", "PRIVATE_BODY")
            .put("extras", JSONObject().put("lefil::notification", JSONObject()
                .put("source", source).put("event_id", event).put("url", "https://$host/message/$id")))

    /** Gotify GET /message returns the newest first. */
    private fun page(vararg ids: Long) = JSONObject().put("messages",
        JSONArray().apply { ids.sortedDescending().forEach { put(message(it, "e$it")) } }).toString()

    @Test fun deliversMissedMessagesOldestFirstAndAdvancesCursor() {
        val seen = mutableListOf<String>()
        val next = GotifyCatchUp.nextCursor(page(4, 5, 6, 7), 5, false) { seen += it.eventId; true }
        assertEquals(listOf("e6", "e7"), seen)
        assertEquals(7L, next)
    }

    @Test fun firstPairingIsSilentAndSkipsExistingHistory() {
        var shown = 0
        assertEquals(9L, GotifyCatchUp.nextCursor(page(2, 9, 5), 0, true) { shown++; true })
        assertEquals(0, shown)
    }

    @Test fun refusedNotificationStopsTheCursorForRetry() {
        val next = GotifyCatchUp.nextCursor(page(10, 11, 12), 9, false) { it.eventId != "e11" }
        assertEquals(10L, next)
    }

    @Test fun malformedOrForeignEventsAreSkippedNotRetriedForever() {
        val body = JSONObject().put("messages", JSONArray()
            .put(message(21, "ok"))
            .put(message(20, "bad", host = "evil.test"))).toString()
        val seen = mutableListOf<String>()
        assertEquals(21L, GotifyCatchUp.nextCursor(body, 19, false) { seen += it.eventId; true })
        assertEquals(listOf("ok"), seen)
    }

    @Test fun invalidPageKeepsCursorUntouched() {
        assertNull(GotifyCatchUp.nextCursor("<html>login</html>", 4, false) { true })
        assertEquals(4L, GotifyCatchUp.nextCursor(page(), 4, false) { true })
    }
}
