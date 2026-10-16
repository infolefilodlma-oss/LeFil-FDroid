package net.lefil.fdroid

import org.junit.Assert.*
import org.junit.Test

class UnreadParsersTest {
    @Test fun agora() {
        val html = """<strong><a href="message/12">Marie</a></strong>
           <a href="message/9">Lu</a>"""
        val items=UnreadParsers.parseAgora(html,"https://social.le-fil.net")
        assertEquals(listOf("12"),items.map{it.id})
        assertEquals("https://social.le-fil.net/message/12",items[0].url)
    }
    @Test fun atrium() {
        val html = """<a href="/mail/mail/index?id=31" class="messagePreviewEntry entry unread" data-message-id="31">
             <h4>Marie <time>1 min</time></h4></a>
             <a href="/mail/mail/index?id=30" class="messagePreviewEntry entry" data-message-id="30">
             <h4>Lu <time>2 min</time></h4></a>"""
        val items=UnreadParsers.parseAtrium(html,"https://team.le-fil.net")
        assertEquals(listOf("31"),items.map{it.id})
    }
    @Test fun countAndTracker() {
        assertEquals(2,UnreadParsers.parseNewMessageCount("""{"newMessages":2}"""))
        val store=hashMapOf<String,Set<String>>()
        val tracker=UnreadTracker({store[it]?:emptySet()},{k,v->store[k]=v})
        val message=UnreadItem("1","A","url")
        assertEquals(1,tracker.newlyUnread("agora",listOf(message)).size)
        tracker.remember("agora",listOf(message))
        assertTrue(tracker.newlyUnread("agora",listOf(message)).isEmpty())
    }
}
