package net.lefil.fdroid

import org.junit.Assert.*
import org.junit.Test

class MessageLinksTest {
    @Test fun notificationDestinationsMustUseExactHttpsOrigins() {
        for (url in listOf("https://social.le-fil.net/message/12", "https://team.le-fil.net/mail/mail/index?id=31", "https://team.le-fil.net/index.php?r=mail%2Fmail%2Findex&id=31")) {
            assertTrue(url, MessageLinks.trusted(url))
        }
    }
    @Test fun rejectsInjectedSchemesHostsCredentialsAndPorts() {
        for (url in listOf("http://team.le-fil.net/", "https://team.le-fil.net.evil.test/", "https://evil.test/", "https://user@team.le-fil.net/", "https://team.le-fil.net:8443/", "javascript:alert(1)", "file:///sdcard/file", "https://team.le-fil.net\\@evil.test/", "//team.le-fil.net/", "https://le-fil.net/", "")) {
            assertFalse(url, MessageLinks.trusted(url))
        }
    }
}
