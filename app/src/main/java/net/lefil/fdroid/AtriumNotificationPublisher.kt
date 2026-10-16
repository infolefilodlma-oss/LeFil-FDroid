package net.lefil.fdroid

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Notification Android native pour un nouveau message prive Talk / Atrium.
 *
 * Ne contient jamais le texte du message, l'identite de son auteur ou
 * le cookie d'authentification. Ne contacte aucun serveur.
 * Utilisee uniquement si un evenement authentifie a ete valide en amont.
 */
internal class AtriumNotificationPublisher(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "lefil_atrium_private_messages"
        const val NOTIFICATION_TAG_PREFIX = "atrium-message-"
        private const val NOTIFICATION_ID = 2001
    }

    fun publish(event: AtriumInboxEvent): Boolean {
        if (event.id <= 0L) return false

        return try {
            val manager = NotificationManagerCompat.from(context)
            if (!manager.areNotificationsEnabled()) return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val systemManager = context.getSystemService(NotificationManager::class.java)
                systemManager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Messages prives Atrium (Talk)",
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                )
            }

            // En l'absence d'URL de conversation verifiee dans la reponse
            // du serveur, ouvrir l'application sans inventer de lien.
            val openApp = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val intent = PendingIntent.getActivity(
                context,
                2001,
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setContentTitle("Nouveau message dans Talk")
                .setContentText("Ouvrir Le Fil pour lire le message")
                .setContentIntent(intent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .build()

            manager.notify("$NOTIFICATION_TAG_PREFIX${event.id}", NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            // Permissions refusees : le suivi local NE doit PAS avancer.
            false
        } catch (_: RuntimeException) {
            false
        }
    }
}
