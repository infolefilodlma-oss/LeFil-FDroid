package net.lefil.fdroid

import org.junit.Assert.*
import org.junit.Test

class UnreadParsersTest {
    @Test fun agora() {
        val html = """<strong><a href="message/12">Marie</a></strong>
           <a href="message/9">Lu</a>"""
        val items=UnreadParsers.parseAgora(html,"https://agora.le-fil.net")
        assertEquals(listOf("12"),items.map{it.id})
        assertEquals("https://agora.le-fil.net/message/12",items[0].url)
    }
    @Test fun atrium() {
        val html = """<a href="/mail/mail/index?id=31" class="messagePreviewEntry entry unread" data-message-id="31">
             <h4>Marie <time>1 min</time></h4></a>
             <a href="/mail/mail/index?id=30" class="messagePreviewEntry entry" data-message-id="30">
             <h4>Lu <time>2 min</time></h4></a>"""
        val items=UnreadParsers.parseAtrium(html,"https://atrium.le-fil.net")
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

    @Test fun agoraVersionChangesWhenUnreadThreadReceivesAnotherMessage() {
        fun parse(count: Int) = UnreadParsers.parseAgora("""<div class="mail-list-outside-wrapper"><strong><a href="message/12">Marie</a></strong><p class="text-muted">$count messages</p></div>""", "https://agora.le-fil.net").single()
        assertEquals("2 messages", parse(2).version)
        assertNotEquals(parse(2).version, parse(3).version)
    }
    @Test fun standardFriendicaVersionIsReadFromMailCount() {
        val html = """<div class="mail-list-wrapper"><span class="mail-subject unseen"><a href="message/4">Sujet</a></span><span class="mail-count">3</span></div>"""
        assertEquals("3", UnreadParsers.parseAgora(html,"https://agora.le-fil.net").single().version)
    }
    @Test fun atriumVersionContainsLastEntry() {
        val html = """<a class="messagePreviewEntry unread" data-message-id="31" href="/mail/mail/index?id=31"><h4>Marie <time>12:00</time></h4><div class="mail-last-entry">Bonjour &amp; merci</div></a>"""
        assertEquals("Bonjour & merci", UnreadParsers.parseAtrium(html,"https://atrium.le-fil.net").single().version)
    }

    @Test fun missingTalkHrefUsesTheSupportedFrontControllerRoute() {
        val html = """<a class="messagePreviewEntry unread" data-message-id="31"><h4>Marie <time>12:00</time></h4><div class="mail-last-entry">Bonjour</div></a>"""
        val item = UnreadParsers.parseAtrium(html, "https://atrium.le-fil.net").single()
        assertEquals("https://atrium.le-fil.net/index.php?r=mail%2Fmail%2Findex&id=31", item.url)
    }
}
