package net.lefil.fdroid

import android.webkit.WebView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** API 24 has no renderer-exit callback; adding recovery must preserve startup. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24])
class AndroidLegacyStartupTest {
    @Test fun android7StillOpensTheHomePage() {
        MessageWatchService.setEnabled(RuntimeEnvironment.getApplication(), false)
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val webView = controller.get().findViewById<WebView>(R.id.site_webview)
        assertNotNull(webView)
        assertEquals("https://le-fil.net/", shadowOf(webView).lastLoadedUrl)
        controller.pause().stop().destroy()
    }
}
