package net.lefil.fdroid

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
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
class AndroidRendererRecoveryTest {
    private val crashed = object : RenderProcessGoneDetail() {
        override fun didCrash() = true
        override fun rendererPriorityAtExit() = WebView.RENDERER_PRIORITY_IMPORTANT
    }
    @Before fun disableWatch() {
        MessageWatchService.setEnabled(RuntimeEnvironment.getApplication(), false)
    }
    @Test fun rendererExitIsHandledWithoutKillingTheActivity() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        val old = activity.findViewById<WebView>(R.id.site_webview)
        assertTrue(old.webViewClient.onRenderProcessGone(old, crashed))
        assertFalse(activity.isFinishing)
        assertNull(old.parent)
        assertNull(activity.findViewById<WebView>(R.id.site_webview))
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.site_renderer_error).visibility)
        assertEquals(View.GONE, activity.findViewById<View>(R.id.site_progress).visibility)
        controller.pause().stop().destroy()
    }

    @Test fun retryCreatesANewRendererAndKeepsTheLastConversation() {
        val url = "https://team.le-fil.net/index.php?r=mail%2Fmail%2Findex&id=31"
        val controller = Robolectric.buildActivity(MainActivity::class.java, Intent().putExtra(MainActivity.EXTRA_URL, url)).setup()
        val activity = controller.get()
        val old = activity.findViewById<WebView>(R.id.site_webview)
        old.webViewClient.onRenderProcessGone(old, crashed)
        activity.findViewById<Button>(R.id.reload_site).performClick()
        val replacement = activity.findViewById<WebView>(R.id.site_webview)
        assertNotSame(old, replacement)
        assertEquals(url, shadowOf(replacement).lastLoadedUrl)
        assertEquals(View.GONE, activity.findViewById<View>(R.id.site_renderer_error).visibility)
        assertTrue(replacement.webViewClient.onRenderProcessGone(replacement, crashed))
        assertFalse(activity.isFinishing)
        assertNull(activity.findViewById<WebView>(R.id.site_webview))
        controller.pause().stop().destroy()
    }

    @Test fun aNewNotificationCanOpenAfterRendererExit() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        val old = activity.findViewById<WebView>(R.id.site_webview)
        old.webViewClient.onRenderProcessGone(old, crashed)
        val url = "https://social.le-fil.net/message/12"
        controller.newIntent(Intent().putExtra(MainActivity.EXTRA_URL, url))
        assertEquals(url, shadowOf(activity.findViewById<WebView>(R.id.site_webview)).lastLoadedUrl)
        assertEquals(View.GONE, activity.findViewById<View>(R.id.site_renderer_error).visibility)
        controller.pause().stop().destroy()
    }

    @Test fun savedStateDoesNotUseTheDestroyedWebView() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        val url = "https://social.le-fil.net/message/12"
        controller.newIntent(Intent().putExtra(MainActivity.EXTRA_URL, url))
        val old = activity.findViewById<WebView>(R.id.site_webview)
        old.webViewClient.onRenderProcessGone(old, crashed)
        val state = Bundle()
        controller.saveInstanceState(state).pause().stop().destroy()
        val restored = Robolectric.buildActivity(MainActivity::class.java).setup(state)
        assertEquals(url, shadowOf(restored.get().findViewById<WebView>(R.id.site_webview)).lastLoadedUrl)
        restored.pause().stop().destroy()
    }
}
