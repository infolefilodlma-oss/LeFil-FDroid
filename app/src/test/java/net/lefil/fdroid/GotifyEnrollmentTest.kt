package net.lefil.fdroid

import org.junit.Assert.*
import org.junit.Test

class GotifyEnrollmentTest {
    private val token = "Cabcdef0123456789xyz"

    @Test fun acceptsOnlyTheMembersOwnValidCredentials() {
        val ok = GotifyEnrollment.parse(SessionHttp.Reply(200,
            """{"subject":"humhub-x","server":"https://notifications.le-fil.net","client_token":"$token"}"""))
        assertEquals(PushCredentials("https://notifications.le-fil.net", token), ok)
    }

    @Test fun refusesLoginRedirectsErrorsAndForeignServers() {
        for (reply in listOf(
            null,
            SessionHttp.Reply(302, ""),
            SessionHttp.Reply(403, """{"error":"login"}"""),
            SessionHttp.Reply(404, "<html>Not found</html>"),
            SessionHttp.Reply(200, "<html>login</html>"),
            SessionHttp.Reply(200, """{"server":"https://evil.test","client_token":"$token"}"""),
            SessionHttp.Reply(200, """{"server":"http://notifications.le-fil.net","client_token":"$token"}"""),
            SessionHttp.Reply(200, """{"server":"https://notifications.le-fil.net","client_token":"short"}""")
        )) assertNull(reply?.toString(), GotifyEnrollment.parse(reply))
    }

    @Test fun enrollsEachServiceAtItsOwnEndpoint() {
        val urls = mutableListOf<String>()
        val transport = EnrollTransport { url, device ->
            urls += url
            assertEquals(64, device.length)
            SessionHttp.Reply(200, """{"server":"https://notifications.le-fil.net","client_token":"$token"}""")
        }
        val device = "a".repeat(64)
        assertNotNull(GotifyEnrollment.enroll(PushRealm.HUMHUB, device, transport))
        assertNotNull(GotifyEnrollment.enroll(PushRealm.FRIENDICA, device, transport))
        assertEquals(listOf("https://atrium.le-fil.net/index.php?r=lefil-gotify%2Fenroll",
            "https://agora.le-fil.net/lefilgotify"), urls)
    }

    @Test fun transportFailureIsNotFatal() {
        assertNull(GotifyEnrollment.enroll(PushRealm.HUMHUB, "b".repeat(64)) { _, _ -> throw IllegalStateException("offline") })
    }

    @Test fun pollingSourcesMapToTheirService() {
        assertEquals(PushRealm.FRIENDICA, PushRealm.of(AgoraSource()))
        assertEquals(PushRealm.FRIENDICA, PushRealm.of(AgoraActivitySource()))
        assertEquals(PushRealm.HUMHUB, PushRealm.of(AtriumSource()))
        assertEquals(PushRealm.HUMHUB, PushRealm.of(AtriumActivitySource()))
    }
}
