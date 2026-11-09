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
        fun start(context:Context) {
            runCatching { ContextCompat.startForegroundService(context,Intent(context,MessageWatchService::class.java)) }
        }
        fun pollNow(context:Context) {
            runCatching { context.startService(Intent(context,MessageWatchService::class.java).setAction(ACTION_POLL)) }
        }
    }
    private var executor:ScheduledExecutorService?=null
    private lateinit var tracker:UnreadTracker
    private var loginNeeded=false
    private val sources=listOf<MessageSource>(AgoraSource(),AtriumSource())
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
        if(executor==null) {
            executor=Executors.newSingleThreadScheduledExecutor().also {
                it.scheduleWithFixedDelay({pollSafe()},0,60,TimeUnit.SECONDS)
            }
        } else if(intent?.action==ACTION_POLL) executor?.execute {pollSafe()}
        scheduleAlarm()
        return START_STICKY
    }
    override fun onBind(intent:Intent?):IBinder?=null
    override fun onDestroy() {executor?.shutdownNow();executor=null;super.onDestroy()}
    private fun pollSafe() {
        val power=getSystemService(Context.POWER_SERVICE) as PowerManager
        val wake=power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"LeFil:poll")
        wake.acquire(60_000L)
        try { pollAll() } catch(_:Exception) {} finally {if(wake.isHeld)wake.release()}
    }
    @Synchronized private fun pollAll() {
        var needLogin=false
        for(source in sources) {
            when(val result=runCatching{source.fetch()}.getOrDefault(FetchResult.Failed)) {
                is FetchResult.Unread -> {
                    val fresh=tracker.newlyUnread(source.key,result.items)
                    val successes=fresh.map { item -> item.id to showMessage(source,item) }.toMap()
                    val previous=result.items.map{it.id}.toSet()
                    val accepted=previous.filter { id -> id !in fresh.map{it.id} || successes[id]==true }.toSet()
                    tracker.remember(source.key,result.items.filter{it.id in accepted})
                }
                FetchResult.LoginRequired -> needLogin=true
                FetchResult.Failed -> Unit
            }
        }
        if(needLogin!=loginNeeded) {
            loginNeeded=needLogin
            runCatching {NotificationManagerCompat.from(this).notify(STATUS_ID,statusNotification())}
        }
    }
    private fun showMessage(source:MessageSource,item:UnreadItem):Boolean {
        val nm=NotificationManagerCompat.from(this)
        if(!nm.areNotificationsEnabled()) return false
        val open=Intent(this,MainActivity::class.java).setAction("open-${source.key}-${item.id}")
            .putExtra(MainActivity.EXTRA_URL,item.url)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val requestCode=(source.key+":"+item.id).hashCode()
        val pending=PendingIntent.getActivity(this,requestCode,open,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n=NotificationCompat.Builder(this,MESSAGE_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("${source.displayName} — nouveau message")
            .setContentText(item.label.ifBlank{"Touchez pour lire"})
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(pending).setAutoCancel(true).build()
        return try {nm.notify("${source.key}-${item.id}",0,n);true} catch(_:Exception){false}
    }
    private fun statusNotification():Notification {
        val pending=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this,STATUS_CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("Le Fil")
            .setContentText(if(loginNeeded) "Touchez pour vous reconnecter" else "Surveillance des messages Agora et Atrium")
            .setOngoing(true).setShowWhen(false).setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(pending).build()
    }
    private fun createChannels() {
        if(Build.VERSION.SDK_INT<26)return
        val manager=getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(STATUS_CHANNEL,"Le Fil actif",NotificationManager.IMPORTANCE_MIN))
        manager.createNotificationChannel(NotificationChannel(MESSAGE_CHANNEL,"Messages privés",NotificationManager.IMPORTANCE_HIGH))
    }
    private fun scheduleAlarm() {
        val alarm=getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending=PendingIntent.getBroadcast(this,0,Intent(this,PollAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val at=SystemClock.elapsedRealtime()+5*60_000L
        if(Build.VERSION.SDK_INT>=23) alarm.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,at,pending)
        else alarm.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,at,pending)
    }
}
