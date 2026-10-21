package net.lefil.fdroid

import org.junit.Assert.*
import org.junit.Test

class MessageLinksTest {
    @Test fun notificationDestinationsMustUseExactHttpsOrigins() {
        for (url in listOf("https://agora.le-fil.net/message/12", "https://atrium.le-fil.net/mail/mail/index?id=31", "https://atrium.le-fil.net/index.php?r=mail%2Fmail%2Findex&id=31")) {
            assertTrue(url, MessageLinks.trusted(url))
        }
    }
    @Test fun rejectsInjectedSchemesHostsCredentialsAndPorts() {
        for (url in listOf("http://atrium.le-fil.net/", "https://atrium.le-fil.net.evil.test/", "https://evil.test/", "https://user@atrium.le-fil.net/", "https://atrium.le-fil.net:8443/", "javascript:alert(1)", "file:///sdcard/file", "https://atrium.le-fil.net\\@evil.test/", "//atrium.le-fil.net/", "https://le-fil.net/", "")) {
            assertFalse(url, MessageLinks.trusted(url))
        }
    }
}
