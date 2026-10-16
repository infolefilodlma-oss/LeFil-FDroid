package net.lefil.fdroid

import android.os.Build
import android.os.SystemClock
import android.view.View
import android.webkit.WebView
import android.widget.Button
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.appbar.MaterialToolbar
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Terminates a real renderer only in an account-free test emulator. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29)
class RendererRecoveryInstrumentedTest {
    private fun terminateRenderer(scenario: ActivityScenario<MainActivity>): WebView {
        lateinit var failedView: WebView
        scenario.onActivity { activity ->
            failedView = activity.findViewById(R.id.site_webview)
            // Local content starts the renderer independently of remote services or accounts.
            failedView.loadDataWithBaseURL("https://le-fil.net/", "<p>Renderer recovery test</p>",
                "text/html", "UTF-8", null)
        }
        val deadline = SystemClock.uptimeMillis() + 60_000
        while (SystemClock.uptimeMillis() < deadline) {
            var terminated = false
            scenario.onActivity {
                terminated = failedView.webViewRenderProcess?.terminate() == true
            }
            if (terminated) return failedView
            SystemClock.sleep(200)
        }
        throw AssertionError("Renderer did not start; no termination was injected")
    }

    private fun awaitRecovery(scenario: ActivityScenario<MainActivity>) {
        val deadline = SystemClock.uptimeMillis() + 60_000
        while (SystemClock.uptimeMillis() < deadline) {
            var recovered = false
            scenario.onActivity { activity ->
                recovered = activity.findViewById<View>(R.id.site_renderer_error).isShown
            }
            if (recovered) return
            SystemClock.sleep(200)
        }
        fail("Renderer exit did not leave the native recovery screen available")
    }

    @Test fun realRendererTerminationKeepsTheAppOpenAndAllowsRetryAndServiceSelection() {
        assertTrue("Run only in a disposable emulator without user accounts",
            Build.HARDWARE in setOf("ranchu", "goldfish"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("lefil_watch", 0).edit().putBoolean("enabled", false).commit()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val failedView = terminateRenderer(scenario)
            awaitRecovery(scenario)
            scenario.onActivity { activity ->
                assertFalse(activity.isFinishing)
                assertNull(activity.findViewById<WebView>(R.id.site_webview))
                assertTrue(activity.findViewById<Button>(R.id.reload_site).performClick())
                val replacement = activity.findViewById<WebView>(R.id.site_webview)
                assertNotNull(replacement)
                assertNotSame(failedView, replacement)
                assertEquals(View.GONE, activity.findViewById<View>(R.id.site_renderer_error).visibility)
            }
            terminateRenderer(scenario)
            awaitRecovery(scenario)
            scenario.onActivity { activity ->
                val menu = activity.findViewById<MaterialToolbar>(R.id.toolbar).menu
                assertTrue(menu.performIdentifierAction(4, 0))
                assertNotNull(activity.findViewById<WebView>(R.id.site_webview))
                assertEquals(View.GONE, activity.findViewById<View>(R.id.site_renderer_error).visibility)
                assertFalse(activity.isFinishing)
            }
        }
    }
}
