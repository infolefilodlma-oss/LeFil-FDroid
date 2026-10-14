package net.lefil.fdroid

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * PROTOTYPE expérimental — vérifie uniquement la faisabilité technique d'un
 * service de premier plan type specialUse recevant des évènements en
 * arrière-plan. Aucune connexion réseau, aucun serveur contacté, aucune
 * donnée réelle : les deux évènements ("Agora"/"Atrium") sont générés
 * localement en boucle, à titre de test uniquement.
 */
class SimulatedNotificationService : Service() {

    companion object {
        private const val CHANNEL_FOREGROUND = "lefil_prototype_foreground"
        private const val CHANNEL_EVENTS = "lefil_prototype_events"
        private const val FOREGROUND_NOTIFICATION_ID = 1
        private const val INTERVAL_MS = 30_000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var tick = 0

    private val simulateEvent = object : Runnable {
        override fun run() {
            emitFakeEvent()
            handler.postDelayed(this, INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        }

        handler.removeCallbacks(simulateEvent)
        handler.postDelayed(simulateEvent, INTERVAL_MS)

        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(simulateEvent)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_FOREGROUND,
                "Le Fil (prototype)",
                NotificationManager.IMPORTANCE_MIN
            )
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_EVENTS,
                "Évènements simulés (prototype)",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_FOREGROUND)
            .setContentTitle("Le Fil — prototype de notifications")
            .setContentText("Test technique en cours, aucune donnée réelle")
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun emitFakeEvent() {
        tick++
        val source = if (tick % 2 == 0) "Agora" else "Atrium"

        val notification = NotificationCompat.Builder(this, CHANNEL_EVENTS)
            .setContentTitle("Nouveau message $source (simulation)")
            .setContentText("Évènement fictif n°$tick — aucune donnée réelle, aucune connexion serveur")
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(1000 + tick, notification)
    }
}
