package net.lefil.fdroid

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class MessageWatchService: Service() {
    companion object {
        private const val STATUS_CHANNEL="lefil_status"
        private const val MESSAGE_CHANNEL="lefil_messages"
        private const val STATUS_ID=1
        const val ACTION_POLL="net.lefil.fdroid.POLL"
        fun enabled(context: Context) = context.getSharedPreferences("lefil_watch", Context.MODE_PRIVATE)
            .getBoolean("enabled", false)
        fun setEnabled(context: Context, enabled: Boolean) {
            context.getSharedPreferences("lefil_watch", Context.MODE_PRIVATE).edit().putBoolean("enabled", enabled).commit()
        }
        fun start(context:Context): Boolean {
            if (!enabled(context)) return false
            return runCatching { ContextCompat.startForegroundService(context,Intent(context,MessageWatchService::class.java)); true }.getOrDefault(false)
        }
        fun stop(context: Context) {
            setEnabled(context, false)
            context.stopService(Intent(context, MessageWatchService::class.java))
        }
        fun pollNow(context:Context) {
            if (!enabled(context)) return
            runCatching { context.startService(Intent(context,MessageWatchService::class.java).setAction(ACTION_POLL)) }
        }
    }
    private var executor:ScheduledExecutorService?=null
    private lateinit var tracker:UnreadTracker
    private var loginNeeded=false
    private var unavailable=false
    private val sources=listOf<MessageSource>(AgoraSource(),AgoraActivitySource(),AtriumSource(),AtriumActivitySource())
    override fun onCreate() {
        super.onCreate()
        val prefs=getSharedPreferences("lefil_watch",Context.MODE_PRIVATE)
        tracker=UnreadTracker(
            load={ k -> prefs.getStringSet("unread_$k",emptySet()) ?: emptySet() },
            save={ k, ids -> prefs.edit().putStringSet("unread_$k",HashSet(ids)).apply() }
        )
        createChannels()
        ServiceCompat.startForeground(this,STATUS_ID,statusNotification(),
            if(Build.VERSION.SDK_INT>=34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)
    }
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        if (!enabled(this)) { stopSelf(); return START_NOT_STICKY }
        if(executor==null) {
            executor=Executors.newSingleThreadScheduledExecutor().also {
                it.scheduleWithFixedDelay({pollSafe()},0,60,TimeUnit.SECONDS)
            }
        } else if(intent?.action==ACTION_POLL) executor?.execute {pollSafe()}
        return START_STICKY
    }
    override fun onBind(intent:Intent?):IBinder?=null
    override fun onDestroy() {executor?.shutdownNow();executor=null;super.onDestroy()}
    private fun pollSafe() {
        val power=getSystemService(Context.POWER_SERVICE) as PowerManager
        val wake=power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"LeFil:poll")
        wake.acquire(120_000L)
        try { pollAll() } catch(_:Exception) {} finally {if(wake.isHeld)wake.release()}
    }
    @Synchronized private fun pollAll() {
        val status = MessagePoller(sources, tracker, ::showMessage).poll()
        val changed = status.loginRequired != loginNeeded || status.unavailable != unavailable
        loginNeeded = status.loginRequired
        unavailable = status.unavailable
        if (changed) {
            try { NotificationManagerCompat.from(this).notify(STATUS_ID, statusNotification()) }
            catch (_: SecurityException) { /* Permission can be revoked while polling. */ }
        }
    }
    internal fun showMessage(source:MessageSource,item:UnreadItem):Boolean {
        if (!enabled(this)) return false
        val nm=NotificationManagerCompat.from(this)
        if (!MessageLinks.trusted(item.url)) return false
        if (Build.VERSION.SDK_INT >= 26 && getSystemService(NotificationManager::class.java)
                .getNotificationChannel(MESSAGE_CHANNEL)?.importance == NotificationManager.IMPORTANCE_NONE) return false
        if(!nm.areNotificationsEnabled()) return false
        val open=Intent(this,MainActivity::class.java).setAction("open-${source.key}-${item.id}")
            .putExtra(MainActivity.EXTRA_URL,item.url)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val requestCode=(source.key+":"+item.id).hashCode()
        val pending=PendingIntent.getActivity(this,requestCode,open,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n=NotificationCompat.Builder(this,MESSAGE_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("${source.displayName} — ${source.alertLabel}")
            .setContentText(item.label.ifBlank{"Touchez pour lire"})
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(pending).setAutoCancel(true).build()
        return try {nm.notify("${source.key}-${item.id}",0,n);true} catch(_:SecurityException){false}
    }
    private fun statusNotification():Notification {
        val pending=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this,STATUS_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("Le Fil")
            .setContentText(if(loginNeeded) "Touchez pour vous reconnecter" else if(unavailable) "Vérification indisponible — nouvel essai automatique" else "Surveillance Agora, Atrium et Talk")
            .setOngoing(true).setShowWhen(false).setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(pending).build()
    }
    private fun createChannels() {
        if(Build.VERSION.SDK_INT<26)return
        val manager=getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(STATUS_CHANNEL,"Le Fil actif",NotificationManager.IMPORTANCE_MIN))
        manager.createNotificationChannel(NotificationChannel(MESSAGE_CHANNEL,"Messages et activités",NotificationManager.IMPORTANCE_HIGH))
    }
}
