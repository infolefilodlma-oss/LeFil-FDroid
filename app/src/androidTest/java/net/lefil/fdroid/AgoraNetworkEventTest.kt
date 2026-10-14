package net.lefil.fdroid

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Banc d'essai émulateur : vérifie que l'application reçoit réellement un
 * évènement réseau ("Nouveau message Agora") émis par un serveur éphémère
 * local, et qu'aucune notification n'apparaît en l'absence d'un tel
 * évènement. Aucune donnée personnelle, aucun serveur de production.
 */
@RunWith(AndroidJUnit4::class)
class AgoraNetworkEventTest {

    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    private fun notificationManager(): NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    private fun hasAgoraTestNotification(): Boolean =
        notificationManager().activeNotifications.any { sbn ->
            val title = sbn.notification.extras.getCharSequence("android.title")?.toString() ?: ""
            title.contains("Agora")
        }

    @After
    fun tearDown() {
        context.stopService(Intent(context, SimulatedNotificationService::class.java))
        notificationManager().cancelAll()
    }

    @Test
    fun aucunEvenement_aucuneNotification() {
        val server = FakeEventServer("no_event")
        server.start()
        try {
            val intent = Intent(context, SimulatedNotificationService::class.java)
                .putExtra(
                    SimulatedNotificationService.EXTRA_TEST_SERVER_URL,
                    "http://127.0.0.1:${server.port}/"
                )
            context.startService(intent)

            Thread.sleep(3_000)

            Log.i("LeFilTest", "JOURNAL RECEPTION — test 1 (aucun évènement) : notification Agora présente = ${hasAgoraTestNotification()}")

            assertFalse(
                "Une notification Agora est apparue sans évènement réseau valide",
                hasAgoraTestNotification()
            )
        } finally {
            server.stop()
        }
    }

    @Test
    fun evenementReseauReel_declencheNotification() {
        val server = FakeEventServer(SimulatedNotificationService.TEST_TRIGGER_BODY)
        server.start()
        try {
            val intent = Intent(context, SimulatedNotificationService::class.java)
                .putExtra(
                    SimulatedNotificationService.EXTRA_TEST_SERVER_URL,
                    "http://127.0.0.1:${server.port}/"
                )
            context.startService(intent)

            var received = false
            repeat(10) {
                Thread.sleep(500)
                if (hasAgoraTestNotification()) received = true
            }

            Log.i("LeFilTest", "JOURNAL RECEPTION — test 2 (évènement réel) : notification Agora reçue = $received")

            assertTrue(
                "Aucune notification Agora reçue malgré l'évènement réseau émis par le serveur de test",
                received
            )
        } finally {
            server.stop()
        }
    }
}
