package net.lefil.fdroid

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ScheduledExecutorService

/** One private Gotify account per service: Agora (Friendica) and Atrium/Talk (HumHub). */
internal enum class PushRealm(val key: String, val enrollUrl: String) {
    HUMHUB("humhub", "https://atrium.le-fil.net/index.php?r=lefil-gotify%2Fenroll"),
    FRIENDICA("friendica", "https://agora.le-fil.net/lefilgotify");

    companion object {
        /** Existing polling sources covered by each realm once native push is active. */
        fun of(source: MessageSource): PushRealm =
            if (source.key.startsWith("agora")) FRIENDICA else HUMHUB
    }
}

internal data class PushCredentials(val server: String, val token: String)

/**
 * Activates Gotify's official /stream transport only after this device has a
 * private client token, obtained by the member's own logged-in session
 * (see GotifyEnrollment); the APK never carries an administrator or application token.
 *
 * Without pairing, this component is inert and the existing fallback is kept.
 */
internal class GotifyPushManager(
    context: Context,
    private val scheduler: ScheduledExecutorService,
    private val notify: (PushNotice) -> Boolean,
    private val onCredentialsLost: () -> Unit = {}
) {
    private val prefs = context.getSharedPreferences("lefil_push", Context.MODE_PRIVATE)
    private var client: OkHttpClient? = null
    private val streams = mutableMapOf<PushRealm, GotifyStreamTransport>()
    private val ledger = PushNoticeLedger(
        // Insertion order is kept so that the bounded history forgets the oldest events first.
        load = { prefs.getString("delivered_order", null)?.split('\n')?.filter { it.isNotEmpty() }?.toCollection(LinkedHashSet()) ?: LinkedHashSet() },
        save = { values -> prefs.edit().putString("delivered_order", values.joinToString("\n")).apply() },
        notify = notify
    )

    fun credentials(realm: PushRealm): PushCredentials? {
        val server = prefs.getString("server_" + realm.key, null) ?: return null
        val token = prefs.getString("token_" + realm.key, null) ?: return null
        return PushCredentials(server, token).takeIf { GotifyStreamTransport.buildRequest(server, token) != null }
    }

    fun configuredRealms(): Set<PushRealm> = PushRealm.values().filter { credentials(it) != null }.toSet()

    /** A new token starts with a silent catch-up: old server history is never replayed as alerts. */
    fun store(realm: PushRealm, credentials: PushCredentials) {
        prefs.edit().putString("server_" + realm.key, credentials.server)
            .putString("token_" + realm.key, credentials.token)
            .putBoolean("primed_" + realm.key, false)
            .remove("cursor_" + realm.key).commit()
    }

    fun clear(realm: PushRealm) {
        prefs.edit().remove("server_" + realm.key).remove("token_" + realm.key).commit()
    }

    @Synchronized fun start(): Boolean {
        stopLocked()
        val socketClient = GotifyStreamTransport.newClient()
        for (realm in PushRealm.values()) {
            val creds = credentials(realm) ?: continue
            val request = GotifyStreamTransport.buildRequest(creds.server, creds.token) ?: continue
            val stream = GotifyStreamTransport(request, socketClient, scheduler,
                { notice -> ledger.accept(notice) },
                onConnected = { runCatching { scheduler.execute { catchUp(realm, request, socketClient) } } },
                onAuthRequired = {
                    // Token revoked (logout, device removed): forget it and fall back to polling/re-enrollment.
                    clear(realm)
                    runCatching { onCredentialsLost() }
                })
            streams[realm] = stream
            stream.start()
        }
        if (streams.isEmpty()) {
            shutdown(socketClient)
            return false
        }
        client = socketClient
        return true
    }

    @Synchronized fun stop() = stopLocked()

    private fun stopLocked() {
        streams.values.forEach { it.stop() }
        streams.clear()
        client?.let { shutdown(it) }
        client = null
    }

    private fun shutdown(socketClient: OkHttpClient) {
        socketClient.dispatcher.executorService.shutdown()
        socketClient.connectionPool.evictAll()
    }

    /** Messages that arrived while the device was offline, asleep or the app stopped. */
    private fun catchUp(realm: PushRealm, stream: Request, socketClient: OkHttpClient) {
        runCatching {
            val url = stream.url.newBuilder().encodedPath("/message")
                .addQueryParameter("limit", GotifyCatchUp.PAGE.toString()).build()
            val request = Request.Builder().url(url).header("X-Gotify-Key", stream.header("X-Gotify-Key") ?: return)
                .get().build()
            val body = socketClient.newCall(request).execute().use { reply ->
                if (!reply.isSuccessful) return
                reply.body?.string() ?: return
            }
            val cursor = prefs.getLong("cursor_" + realm.key, 0L)
            val prime = !prefs.getBoolean("primed_" + realm.key, false)
            val next = GotifyCatchUp.nextCursor(body, cursor, prime) { notice ->
                ledger.known(notice) || ledger.accept(notice)
            } ?: return
            prefs.edit().putLong("cursor_" + realm.key, next).putBoolean("primed_" + realm.key, true).apply()
        }
    }
}

/** Pure catch-up logic over Gotify's GET /message page (newest first, ids increasing over time). */
internal object GotifyCatchUp {
    const val PAGE = 100

    /**
     * Returns the new cursor (highest Gotify id fully handled), or null for an invalid page.
     * When [prime] is true nothing is shown: the cursor jumps to the newest existing message.
     * Processing stops at the first notice Android refused, so it is retried next time.
     */
    fun nextCursor(body: String, cursor: Long, prime: Boolean, deliver: (PushNotice) -> Boolean): Long? {
        val messages = runCatching { org.json.JSONObject(body).optJSONArray("messages") }.getOrNull() ?: return null
        val pending = (0 until messages.length()).mapNotNull { messages.optJSONObject(it) }
            .filter { it.optLong("id", 0L) > cursor }
            .sortedBy { it.optLong("id", 0L) }
        if (prime) return maxOf(cursor, pending.maxOfOrNull { it.optLong("id", 0L) } ?: cursor)
        var next = cursor
        for (message in pending) {
            val notice = GotifyNoticeParser.parse(message.toString())
            if (notice != null && !deliver(notice)) break
            next = message.optLong("id", next)
        }
        return next
    }
}

/** Deduplicate stable server event IDs across websocket reconnects/app restarts. */
internal class PushNoticeLedger(
    private val load: () -> Set<String>,
    private val save: (Set<String>) -> Unit,
    private val notify: (PushNotice) -> Boolean
) {
    private fun key(notice: PushNotice) = notice.source + ":" + notice.eventId

    @Synchronized fun known(notice: PushNotice): Boolean = key(notice) in load()

    @Synchronized fun accept(notice: PushNotice): Boolean {
        val key = key(notice)
        val known = load()
        if (key in known) return false
        // Failed or blocked Android notifications must remain eligible for retry.
        if (!notify(notice)) return false
        // Bounded history, kept in app-private storage. No private message text.
        save((known + key).toList().takeLast(512).toCollection(LinkedHashSet()))
        return true
    }
}
