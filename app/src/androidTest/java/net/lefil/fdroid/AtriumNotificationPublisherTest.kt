package net.lefil.fdroid

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Controle sur emulateur de la notification systeme Android.
 * Flux fictif, session fictive, aucun appel reseau ni compte utilisateur.
 */
@RunWith(AndroidJUnit4::class)
class AtriumNotificationPublisherTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val manager get() = context.getSystemService(NotificationManager::class.java)

    @Test fun unEvenementValideAfficheUneNotificationSansContenuPrive() {
        val id = 90001L
        val tag = AtriumNotificationPublisher.NOTIFICATION_TAG_PREFIX + id
        try {
            assertTrue(AtriumNotificationPublisher(context).publish(AtriumInboxEvent(id)))
            val posted = manager.activeNotifications.firstOrNull { it.tag == tag }
            assertNotNull("La notification Talk doit etre affichee", posted)
            val title = posted!!.notification.extras.getCharSequence("android.title")?.toString()
            val body = posted.notification.extras.getCharSequence("android.text")?.toString()
            assertEquals("Nouveau message dans Talk", title)
            assertEquals("Ouvrir Le Fil pour lire le message", body)
            assertFalse(body.orEmpty().contains("secret"))
        } finally {
            manager.cancel(tag, 2001)
        }
    }

    @Test fun unIdentifiantInvalideNeDeclencheAucuneNotification() {
        assertFalse(AtriumNotificationPublisher(context).publish(AtriumInboxEvent(0L)))
    }
}
