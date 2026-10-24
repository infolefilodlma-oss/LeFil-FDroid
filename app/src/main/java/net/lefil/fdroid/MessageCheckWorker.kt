package net.lefil.fdroid

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Vérification périodique (~15 minutes, planifiée par MessageWatch via
 * WorkManager) d'Agora (Friendica), Atrium (HumHub) et Talk (messagerie
 * HumHub d'Atrium). Une seule notification groupée par cycle ; jamais
 * d'appel au serveur avant connexion (voir SessionHttp.get, MessageSources.kt).
 */
class MessageCheckWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    companion object {
        internal const val MESSAGE_CHANNEL = "lefil_messages"
        internal const val MESSAGE_NOTIFICATION_ID = 1
    }

    override suspend fun doWork(): Result {
        if (!MessageWatch.enabled(applicationContext)) return Result.success()
        createChannel()
        val prefs = applicationContext.getSharedPreferences("lefil_watch", Context.MODE_PRIVATE)
        val tracker = UnreadTracker(
            load = { k -> prefs.getStringSet("unread_$k", emptySet()) ?: emptySet() },
            save = { k, ids -> prefs.edit().putStringSet("unread_$k", HashSet(ids)).apply() }
        )
        val sources = listOf<MessageSource>(AgoraSource(), AgoraActivitySource(), AtriumSource(), AtriumActivitySource())
        MessageBatchPoller(sources, tracker, ::notifyBatch).poll()
        return Result.success()
    }

    internal fun notifyBatch(items: List<Pair<MessageSource, UnreadItem>>): Boolean {
        val nm = NotificationManagerCompat.from(applicationContext)
        if (Build.VERSION.SDK_INT >= 26 && applicationContext.getSystemService(NotificationManager::class.java)
                .getNotificationChannel(MESSAGE_CHANNEL)?.importance == NotificationManager.IMPORTANCE_NONE) return false
        if (!nm.areNotificationsEnabled()) return false
        val target = items.lastOrNull { MessageLinks.trusted(it.second.url) } ?: return false
        val open = Intent(applicationContext, MainActivity::class.java)
            .setAction("open-${target.first.key}-${target.second.id}")
            .putExtra(MainActivity.EXTRA_URL, target.second.url)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(applicationContext, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val count = items.size
        val text = if (count <= 1) "Vous avez 1 nouveau message" else "Vous avez $count nouveaux messages"
        val n = NotificationCompat.Builder(applicationContext, MESSAGE_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("Le Fil")
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(pending).setAutoCancel(true).build()
        return try { nm.notify(MESSAGE_NOTIFICATION_ID, n); true } catch (_: SecurityException) { false }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(MESSAGE_CHANNEL, "Messages et activités", NotificationManager.IMPORTANCE_HIGH)
        )
    }
}
