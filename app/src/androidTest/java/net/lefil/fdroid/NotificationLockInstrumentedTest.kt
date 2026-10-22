package net.lefil.fdroid

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Only activates the existing foreground watch on an emulator.
 * The workflow locks the screen afterwards and checks the real Android process.
 * No real login or delivery of a private message is claimed by this test.
 */
@RunWith(AndroidJUnit4::class)
class NotificationLockInstrumentedTest {
    @Test fun activateWatcherForLockedScreenCheck() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        MessageWatchService.setEnabled(context, true)
        assertTrue(MessageWatchService.start(context))
    }
}
