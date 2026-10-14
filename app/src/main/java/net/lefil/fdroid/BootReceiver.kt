package net.lefil.fdroid

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * PROTOTYPE expérimental — relance le service de test après redémarrage,
 * pour vérifier uniquement la faisabilité technique. Aucune connexion
 * réseau, aucune donnée réelle.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, SimulatedNotificationService::class.java)
            )
        }
    }
}
