package net.lefil.fdroid

import android.webkit.CookieManager
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal sealed class FetchResult {
    data class Unread(val items:List<UnreadItem>, val complete: Boolean = true):FetchResult()
    object LoginRequired:FetchResult()
    object Failed:FetchResult()
}
internal interface MessageSource {
    val key:String
    val displayName:String
    fun fetch():FetchResult
}
internal fun interface SessionTransport {
    fun get(url: String): SessionHttp.Reply?
}
internal class SessionHttp : SessionTransport {
    data class Reply(val status:Int,val body:String)
    override fun get(url:String):Reply? {
        val u=URL(url)
        if (u.protocol!="https" || u.host !in listOf("agora.le-fil.net","atrium.le-fil.net")) return null
        var c:HttpURLConnection?=null
        return try {
            c=(u.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects=false
                connectTimeout=15000
                readTimeout=15000
                setRequestProperty("Accept","text/html,application/json")
                CookieManager.getInstance().getCookie(url)?.let { setRequestProperty("Cookie",it) }
            }
            val status=c.responseCode
            c.headerFields.filterKeys { it?.equals("Set-Cookie", ignoreCase = true) == true }
                .values.flatten().forEach { CookieManager.getInstance().setCookie(url, it) }
            CookieManager.getInstance().flush()
            val text=if(status in 200..299) c.inputStream.bufferedReader().use { it.readText() } else ""
            Reply(status,text)
        } catch (_:Exception) { null } finally { c?.disconnect() }
    }
}
internal class AgoraSource(private val http:SessionTransport=SessionHttp()):MessageSource {
    override val key="agora"
    override val displayName="Agora"
    override fun fetch():FetchResult {
        val r=http.get("https://agora.le-fil.net/message")?:return FetchResult.Failed
        if (r.status in 300..399 || r.status==401 || r.status==403 || UnreadParsers.isLoginPage(r.body)) return FetchResult.LoginRequired
        if(r.status!=200 || !UnreadParsers.hasAgoraInbox(r.body)) return FetchResult.Failed
        return FetchResult.Unread(UnreadParsers.parseAgora(r.body,"https://agora.le-fil.net"), complete = false)
    }
}
internal class AtriumSource(private val http:SessionTransport=SessionHttp()):MessageSource {
    override val key="atrium"
    override val displayName="Atrium"
    override fun fetch():FetchResult {
        val base="https://atrium.le-fil.net"
        for(path in listOf("/mail/mail/notification-list","/index.php?r=mail%2Fmail%2Fnotification-list")) {
            val r=http.get(base+path)?:return FetchResult.Failed
            if(r.status==404) continue
            if(r.status in 300..399 || r.status==401 || r.status==403) return FetchResult.LoginRequired
            if(r.status!=200) return FetchResult.Failed
            val html=if(r.body.trimStart().startsWith("{")) try {
                JSONObject(r.body).let { it.optString("html").ifEmpty { it.optString("output") } }
            } catch(_:Exception) { r.body } else r.body
            if(UnreadParsers.isLoginPage(html)) return FetchResult.LoginRequired
            if(!UnreadParsers.hasAtriumList(html)) return FetchResult.Failed
            val messages=UnreadParsers.parseAtrium(html,base)
            val count=http.get(base+path.replace("notification-list","get-new-message-count-json"))
                ?.takeIf { it.status==200 }?.let { UnreadParsers.parseNewMessageCount(it.body) }
            // The counter is a count, not a mapping from count to conversation identities.
            // Never arbitrarily take the first N messages.
            if(count==null) return FetchResult.Failed
            if(count==0) return FetchResult.Unread(emptyList())
            if(count<messages.size) return FetchResult.Failed
            return FetchResult.Unread(messages, complete = count == messages.size)
        }
        return FetchResult.Failed
    }
}
