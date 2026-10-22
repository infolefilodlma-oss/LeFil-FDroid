package net.lefil.fdroid

import android.content.Context
import okhttp3.OkHttpClient
import java.util.concurrent.ScheduledExecutorService

/**
 * Activates Gotify's official /stream transport only after this device has a
 * private client token. A trusted server-side pairing step must provision this
 * token separately; the APK never carries an administrator or application token.
 *
 * Without pairing, this component is inert and the existing fallback is kept.
 */
internal class GotifyPushManager(
    context: Context,
    private val scheduler: ScheduledExecutorService,
    private val notify: (PushNotice) -> Boolean
) {
    private val prefs = context.getSharedPreferences("lefil_push", Context.MODE_PRIVATE)
    private var client: OkHttpClient? = null
    private var stream: GotifyStreamTransport? = null
    private val ledger = PushNoticeLedger(
        load = { prefs.getStringSet("delivered_ids", emptySet())?.toSet().orEmpty() },
        save = { values -> prefs.edit().putStringSet("delivered_ids", HashSet(values)).apply() },
        notify = notify
    )

    fun start(): Boolean {
        val server = prefs.getString("server", null) ?: return false
        val token = prefs.getString("client_token", null) ?: return false
        val request = GotifyStreamTransport.buildRequest(server, token) ?: return false
        val socketClient = GotifyStreamTransport.newClient()
        client = socketClient
        stream = GotifyStreamTransport(request, socketClient, scheduler, { notice ->
            ledger.accept(notice)
        }).also { it.start() }
        return true
    }

    fun stop() {
        stream?.stop()
        stream = null
        client?.dispatcher?.executorService?.shutdown()
        client?.connectionPool?.evictAll()
        client = null
    }
}

/** Deduplicate stable server event IDs across websocket reconnects/app restarts. */
internal class PushNoticeLedger(
    private val load: () -> Set<String>,
    private val save: (Set<String>) -> Unit,
    private val notify: (PushNotice) -> Boolean
) {
    @Synchronized fun accept(notice: PushNotice): Boolean {
        val key = notice.source + ":" + notice.eventId
        val known = load()
        if (key in known) return false
        // Failed or blocked Android notifications must remain eligible for retry.
        if (!notify(notice)) return false
        // Bounded history, kept in app-private storage. No private message text.
        save((known + key).toList().takeLast(512).toSet())
        return true
    }
}
