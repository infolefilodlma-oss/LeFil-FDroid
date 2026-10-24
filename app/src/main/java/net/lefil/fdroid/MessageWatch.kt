package net.lefil.fdroid

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Remplace l'ancien service permanent + Gotify par une vérification planifiée
 * via WorkManager (15 minutes : minimum autorisé pour un travail périodique
 * Android, sans service de premier plan, sans dépendance Google).
 */
internal object MessageWatch {
    private const val PERIODIC_WORK = "lefil_message_watch"
    private const val ONE_TIME_WORK = "lefil_message_watch_now"
    private const val PREFS = "lefil_watch"

    fun enabled(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("enabled", false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("enabled", enabled).commit()
    }

    private fun constraints() = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    /** Enregistre le travail périodique (idempotent : WorkManager le reprend aussi après redémarrage). */
    fun start(context: Context): Boolean {
        if (!enabled(context)) return false
        return runCatching {
            val request = PeriodicWorkRequestBuilder<MessageCheckWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
            true
        }.getOrDefault(false)
    }

    fun stop(context: Context) {
        setEnabled(context, false)
        runCatching { WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK) }
    }

    /** Vérification immédiate (ex. juste après connexion) ; ne remplace pas la périodicité. */
    fun pollNow(context: Context) {
        if (!enabled(context)) return
        runCatching {
            val request = OneTimeWorkRequestBuilder<MessageCheckWorker>().setConstraints(constraints()).build()
            WorkManager.getInstance(context).enqueueUniqueWork(ONE_TIME_WORK, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
