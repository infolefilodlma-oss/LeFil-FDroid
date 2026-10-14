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
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.net.HttpURLConnection
import java.net.URL

/**
 * PROTOTYPE expérimental — vérifie uniquement la faisabilité technique d'un
 * service de premier plan type specialUse recevant des évènements en
 * arrière-plan.
 *
 * Deux modes, strictement séparés :
 * - Mode normal (aucun extra fourni) : aucune connexion réseau, les deux
 *   évènements ("Agora"/"Atrium") sont générés localement en boucle, comme
 *   précédemment.
 * - Mode banc d'essai réseau (extra [EXTRA_TEST_SERVER_URL] fourni,
 *   utilisé uniquement par le test instrumenté `androidTest`) : une seule
 *   requête HTTP réelle est effectuée vers ce serveur éphémère, et une
 *   notification n'est émise QUE si une réponse exacte et attendue est
 *   reçue — jamais de notification sans évènement reçu.
 */
class SimulatedNotificationService : Service() {

    companion object {
        private const val TAG = "LeFilPrototype"
        private const val CHANNEL_FOREGROUND = "lefil_prototype_foreground"
        private const val CHANNEL_EVENTS = "lefil_prototype_events"
        private const val FOREGROUND_NOTIFICATION_ID = 1
        private const val NETWORK_EVENT_NOTIFICATION_ID = 2
        private const val INTERVAL_MS = 30_000L

        /** Extra de test uniquement : URL du serveur éphémère à interroger une fois. */
        const val EXTRA_TEST_SERVER_URL = "test_server_url"

        /** Seule valeur de réponse qui déclenche une notification de test. */
        const val TEST_TRIGGER_BODY = "agora_new_message"
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

        val testServerUrl = intent?.getStringExtra(EXTRA_TEST_SERVER_URL)
        if (testServerUrl != null) {
            // Mode banc d'essai réseau : pas de minuteur local, uniquement
            // une tentative réelle vers le serveur éphémère du test.
            pollTestServerOnce(testServerUrl)
        } else {
            handler.removeCallbacks(simulateEvent)
            handler.postDelayed(simulateEvent, INTERVAL_MS)
        }

        return START_STICKY
    }

    /**
     * Requête HTTP réelle unique vers le serveur éphémère du test
     * instrumenté. N'émet une notification que si la réponse correspond
     * exactement à [TEST_TRIGGER_BODY] — toute autre réponse, erreur ou
     * absence de connexion ne produit aucune notification.
     */
    private fun pollTestServerOnce(url: String) {
        Thread {
            val body = try {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5_000
                    readTimeout = 5_000
                }
                connection.inputStream.bufferedReader().use { it.readText() }.trim()
            } catch (e: Exception) {
                Log.i(TAG, "Aucun évènement reçu du serveur de test (ou erreur réseau) : ${e.message}")
                null
            }

            Log.i(TAG, "Réponse reçue du serveur de test : ${body ?: "(aucune)"}")

            if (body == TEST_TRIGGER_BODY) {
                handler.post { emitNetworkTriggeredEvent() }
            }
        }.start()
    }

    private fun emitNetworkTriggeredEvent() {
        Log.i(TAG, "Évènement réseau valide : émission de la notification Agora (test)")

        val notification = NotificationCompat.Builder(this, CHANNEL_EVENTS)
            .setContentTitle("Nouveau message Agora (test réseau)")
            .setContentText("Évènement reçu depuis le serveur de test — aucune donnée réelle")
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(NETWORK_EVENT_NOTIFICATION_ID, notification)
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
