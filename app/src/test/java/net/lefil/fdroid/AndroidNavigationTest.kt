package net.lefil.fdroid

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
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

private fun fakeRequest(url: String, mainFrame: Boolean = true): WebResourceRequest =
    object : WebResourceRequest {
        override fun getUrl(): Uri = Uri.parse(url)
        override fun isForMainFrame() = mainFrame
        override fun isRedirect() = false
        override fun hasGesture() = false
        override fun getMethod() = "GET"
        override fun getRequestHeaders(): MutableMap<String, String> = mutableMapOf()
    }

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34, 35])
class AndroidNavigationTest {
    @Before fun disableLiveWatch() { MessageWatch.setEnabled(RuntimeEnvironment.getApplication(), false) }

    @Test fun coldLaunchOpensNotificationConversation() {
        val url = "https://atrium.le-fil.net/mail/mail/index?id=31"
        val intent = Intent().putExtra(MainActivity.EXTRA_URL, url)
        val controller = Robolectric.buildActivity(MainActivity::class.java, intent).setup()
        assertEquals(url, shadowOf(controller.get().findViewById<WebView>(R.id.site_webview)).lastLoadedUrl)
        controller.pause().stop().destroy()
    }
    @Test fun warmLaunchOpensTheNewConversation() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val url = "https://agora.le-fil.net/message/12"
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
        assertEquals("https://atrium.le-fil.net/index.php?r=mail%2Fmail%2Findex", shadowOf(controller.get().findViewById<WebView>(R.id.site_webview)).lastLoadedUrl)
        controller.pause().stop().destroy()
    }

    /**
     * Reproduit le parcours Talk réel : ce site HumHub n'a pas d'URL "jolies",
     * toutes ses routes (dont Talk) sont en ?r=controleur%2Faction. Une session
     * expirée pendant la connexion peut faire atterrir la page principale sur
     * ?r=notification%2Flist, qui ne renvoie que du JSON brut. Vérifie que cette
     * navigation n'affiche jamais ce JSON, sur le host réel de Talk (team.le-fil.net).
     */
    @Test fun notificationListNeverShownAsMainPageAfterTalkLogin() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val webView = controller.get().findViewById<WebView>(R.id.site_webview)
        webView.loadUrl("https://team.le-fil.net/index.php?r=mail%2Fmail%2Findex")
        val client = webView.webViewClient!!
        val bad = fakeRequest("https://team.le-fil.net/index.php?r=notification%2Flist")
        assertTrue(client.shouldOverrideUrlLoading(webView, bad))
        val landedUrl = shadowOf(webView).lastLoadedUrl
        assertFalse(landedUrl.contains("notification"))
        controller.pause().stop().destroy()
    }

    @Test fun notificationListSubFrameRequestIsNotIntercepted() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val webView = controller.get().findViewById<WebView>(R.id.site_webview)
        val client = webView.webViewClient!!
        val ajax = fakeRequest("https://atrium.le-fil.net/index.php?r=notification%2Flist", mainFrame = false)
        assertFalse(client.shouldOverrideUrlLoading(webView, ajax))
        controller.pause().stop().destroy()
    }
}
