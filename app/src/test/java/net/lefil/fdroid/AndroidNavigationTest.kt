package net.lefil.fdroid

import android.content.Intent
import android.webkit.WebView
import com.google.android.material.appbar.MaterialToolbar
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34, 35])
class AndroidNavigationTest {
    @Before fun disableLiveWatch() { MessageWatchService.setEnabled(RuntimeEnvironment.getApplication(), false) }

    @Test fun coldLaunchOpensNotificationConversation() {
        val url = "https://team.le-fil.net/mail/mail/index?id=31"
        val intent = Intent().putExtra(MainActivity.EXTRA_URL, url)
        val controller = Robolectric.buildActivity(MainActivity::class.java, intent).setup()
        assertEquals(url, shadowOf(controller.get().findViewById<WebView>(R.id.site_webview)).lastLoadedUrl)
        controller.pause().stop().destroy()
    }
    @Test fun warmLaunchOpensTheNewConversation() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val url = "https://social.le-fil.net/message/12"
        controller.newIntent(Intent().putExtra(MainActivity.EXTRA_URL, url))
        assertEquals(url, shadowOf(controller.get().findViewById<WebView>(R.id.site_webview)).lastLoadedUrl)
        controller.pause().stop().destroy()
    }
    @Test fun externalNotificationExtraDoesNotNavigateAwayFromHome() {
        val controller = Robolectric.buildActivity(MainActivity::class.java, Intent().putExtra(MainActivity.EXTRA_URL, "https://evil.test/")).setup()
        assertEquals("https://le-fil.net/", shadowOf(controller.get().findViewById<WebView>(R.id.site_webview)).lastLoadedUrl)
        controller.pause().stop().destroy()
    }
    @Test fun nativeMenuIncludesHumHubTalkAndBothServices() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val menu = controller.get().findViewById<MaterialToolbar>(R.id.toolbar).menu
        assertEquals("Agora", menu.findItem(2).title)
        assertEquals("Atrium", menu.findItem(3).title)
        assertEquals("Talk (Atrium)", menu.findItem(4).title)
        menu.performIdentifierAction(4, 0)
        assertEquals("https://team.le-fil.net/mail/mail/index", shadowOf(controller.get().findViewById<WebView>(R.id.site_webview)).lastLoadedUrl)
        controller.pause().stop().destroy()
    }
}
