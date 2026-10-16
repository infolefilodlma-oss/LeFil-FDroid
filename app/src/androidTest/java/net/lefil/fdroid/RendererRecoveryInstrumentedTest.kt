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

/** Uses the documented chrome://crash hook only in an account-free test emulator. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26)
class RendererRecoveryInstrumentedTest {
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

    @Test fun realRendererCrashesKeepTheAppOpenAndAllowRetryAndServiceSelection() {
        assertTrue("Run only in a disposable emulator without user accounts",
            Build.HARDWARE in setOf("ranchu", "goldfish"))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("lefil_watch", 0).edit().putBoolean("enabled", false).commit()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var failedView: WebView? = null
            scenario.onActivity { activity ->
                val current = activity.findViewById<WebView>(R.id.site_webview)
                failedView = current
                current.loadUrl("chrome://crash")
            }
            awaitRecovery(scenario)
            scenario.onActivity { activity ->
                assertFalse(activity.isFinishing)
                assertNull(activity.findViewById<WebView>(R.id.site_webview))
                assertTrue(activity.findViewById<Button>(R.id.reload_site).performClick())
                val replacement = activity.findViewById<WebView>(R.id.site_webview)
                assertNotNull(replacement)
                assertNotSame(failedView, replacement)
                assertEquals(View.GONE, activity.findViewById<View>(R.id.site_renderer_error).visibility)
                replacement.loadUrl("chrome://crash")
            }
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
