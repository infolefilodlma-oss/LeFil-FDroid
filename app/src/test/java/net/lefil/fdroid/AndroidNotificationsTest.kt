package net.lefil.fdroid

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.testing.TestListenableWorkerBuilder
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Android API simulation, not evidence of delivery on a phone or during Doze. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34, 35])
class AndroidNotificationsTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val source = object : MessageSource {
        override val key = "atrium"
        override val displayName = "Talk"
        override fun fetch(): FetchResult = error("No live fetch in this test")
    }
    private val item = UnreadItem("31", "Marie", "https://atrium.le-fil.net/mail/mail/index?id=31", "Bonjour")

    private fun worker(): MessageCheckWorker = TestListenableWorkerBuilder<MessageCheckWorker>(context).build()

    @Test fun groupedNotificationOpensTheMostRecentConversation() {
        assertTrue(worker().notifyBatch(listOf(source to item)))
        val manager = shadowOf(context.getSystemService(NotificationManager::class.java))
        val notification = manager.getNotification(1)
        assertNotNull(notification)
        val open = shadowOf(notification.contentIntent).savedIntent
        assertEquals(MainActivity::class.java.name, open.component?.className)
        assertEquals(item.url, open.getStringExtra(MainActivity.EXTRA_URL))
        assertEquals("open-atrium-31", open.action)
    }

    @Test fun textReflectsTheTotalCountAcrossSources() {
        val second = item.copy(id = "32")
        worker().notifyBatch(listOf(source to item, source to second))
        val manager = shadowOf(context.getSystemService(NotificationManager::class.java))
        assertEquals(
            "Vous avez 2 nouveaux messages",
            manager.getNotification(1).extras.getCharSequence(android.app.Notification.EXTRA_TEXT).toString()
        )
    }

    @Test fun singularWordingForExactlyOneMessage() {
        worker().notifyBatch(listOf(source to item))
        val manager = shadowOf(context.getSystemService(NotificationManager::class.java))
        assertEquals(
            "Vous avez 1 nouveau message",
            manager.getNotification(1).extras.getCharSequence(android.app.Notification.EXTRA_TEXT).toString()
        )
    }

    @Test fun secondCycleReplacesRatherThanStacks() {
        worker().notifyBatch(listOf(source to item))
        worker().notifyBatch(listOf(source to item.copy(id = "32")))
        val manager = shadowOf(context.getSystemService(NotificationManager::class.java))
        assertEquals(1, manager.allNotifications.size)
    }

    @Test fun disabledNotificationsRemainEligibleForRetry() {
        shadowOf(context.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(false)
        assertFalse(worker().notifyBatch(listOf(source to item)))
    }

    @Test fun refusesExternalConversationUrls() {
        assertFalse(worker().notifyBatch(listOf(source to item.copy(url = "https://evil.test/"))))
    }

    @Test fun blockedMessageChannelDoesNotAcknowledgeDelivery() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("lefil_messages", "Messages", NotificationManager.IMPORTANCE_NONE))
        assertFalse(worker().notifyBatch(listOf(source to item)))
    }
}
