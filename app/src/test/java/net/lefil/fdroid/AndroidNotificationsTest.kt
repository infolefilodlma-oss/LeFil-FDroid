package net.lefil.fdroid

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.android.controller.ServiceController

/** Android API simulation, not evidence of delivery on a phone or during Doze. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34, 35])
class AndroidNotificationsTest {
    private lateinit var controller: ServiceController<MessageWatchService>
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val source = object : MessageSource {
        override val key = "atrium"
        override val displayName = "Talk"
        override fun fetch(): FetchResult = error("No live fetch in this test")
    }
    private val item = UnreadItem("31", "Marie", "https://atrium.le-fil.net/mail/mail/index?id=31", "Bonjour")

    @Before fun createService() {
        MessageWatchService.setEnabled(context, true)
        controller = Robolectric.buildService(MessageWatchService::class.java).create()
    }
    @After fun destroyService() { controller.destroy() }

    @Test fun createsPersistentAndMessageChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        assertNotNull(manager.getNotificationChannel("lefil_status"))
        assertEquals(NotificationManager.IMPORTANCE_HIGH, manager.getNotificationChannel("lefil_messages").importance)
        assertNotNull(shadowOf(manager).getNotification(1))
    }
    @Test fun messageNotificationOpensExactConversation() {
        assertTrue(controller.get().showMessage(source, item))
        val manager = shadowOf(context.getSystemService(NotificationManager::class.java))
        val notification = manager.getNotification("atrium-31", 0)
        assertNotNull(notification)
        val open = shadowOf(notification.contentIntent).savedIntent
        assertEquals(MainActivity::class.java.name, open.component?.className)
        assertEquals(item.url, open.getStringExtra(MainActivity.EXTRA_URL))
        assertEquals("open-atrium-31", open.action)
    }
    @Test fun notificationsUseDifferentIntentsForDifferentConversations() {
        controller.get().showMessage(source, item)
        controller.get().showMessage(source, item.copy(id = "32", url = "https://atrium.le-fil.net/mail/mail/index?id=32"))
        val manager = shadowOf(context.getSystemService(NotificationManager::class.java))
        assertNotEquals(manager.getNotification("atrium-31", 0).contentIntent, manager.getNotification("atrium-32", 0).contentIntent)
    }
    @Test fun disabledNotificationsRemainEligibleForRetry() {
        shadowOf(context.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(false)
        assertFalse(controller.get().showMessage(source, item))
    }
    @Test fun refusesExternalConversationUrls() {
        assertFalse(controller.get().showMessage(source, item.copy(url = "https://evil.test/")))
    }
    @Test fun bootOnlyRestartsAnEnabledWatch() {
        MessageWatchService.setEnabled(context, false)
        val app = shadowOf(RuntimeEnvironment.getApplication())
        while (app.nextStartedService != null) { /* Drain prior service intents. */ }
        BootReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        assertNull(app.nextStartedService)
        MessageWatchService.setEnabled(context, true)
        BootReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        assertEquals(MessageWatchService::class.java.name, app.nextStartedService.component?.className)
    }
    @Test fun ignoresUnrelatedBroadcasts() {
        MessageWatchService.setEnabled(context, true)
        val app = shadowOf(RuntimeEnvironment.getApplication())
        while (app.nextStartedService != null) { }
        BootReceiver().onReceive(context, Intent("other"))
        assertNull(app.nextStartedService)
    }
    @Test fun disabledWatchDoesNotRunWhenAndroidRestartsService() {
        MessageWatchService.setEnabled(context, false)
        assertEquals(android.app.Service.START_NOT_STICKY, controller.get().onStartCommand(null, 0, 1))
    }

    @Test fun stoppingWatchPreventsLateNetworkResultsFromPosting() {
        MessageWatchService.stop(context)
        assertFalse(controller.get().showMessage(source, item))
    }
    @Test fun blockedMessageChannelDoesNotAcknowledgeDelivery() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel("lefil_messages")
        manager.createNotificationChannel(android.app.NotificationChannel("lefil_messages", "Messages", NotificationManager.IMPORTANCE_NONE))
        assertFalse(controller.get().showMessage(source, item))
    }
}
