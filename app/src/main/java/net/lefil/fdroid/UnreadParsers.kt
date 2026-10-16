package net.lefil.fdroid

internal object UnreadParsers {
    private val frio = Regex("""<strong>\s*<a\s+href="message/(\d+)"[^>]*>([\s\S]*?)</a>""")
    private val standard = Regex("""class="mail-subject unseen"\s*>\s*<a href="message/(\d+)"[^>]*>([\s\S]*?)</a>""")
    private val login = Regex("""type=["']password["']|name=["']password["']""")
    fun isLoginPage(html: String) = login.containsMatchIn(html)
    private val agoraCount = Regex("""<p class="text-muted">([\\s\\S]*?)</p>""")
    private val lastEntry = Regex("""<div class="mail-last-entry">([\\s\\S]*?)</div>""")
    private val tags = Regex("<[^>]*>")
    private val spaces = Regex("\\s+")
    private val entity = Regex("&(#x[0-9a-fA-F]+|#[0-9]+|[a-zA-Z]+);")
    fun cleanText(fragment: String): String = decodeEntities(fragment.replace(tags," ")).replace(spaces," ").trim()
    fun decodeEntities(s: String) = entity.replace(s) { m ->
        val code = m.groupValues[1]
        try {
            when {
                code.startsWith("#x",true) -> String(Character.toChars(code.substring(2).toInt(16)))
                code.startsWith("#") -> String(Character.toChars(code.substring(1).toInt()))
                else -> mapOf("amp" to "&","lt" to "<","gt" to ">","quot" to "\"","apos" to "'","nbsp" to " ","eacute" to "é","ccedil" to "ç")[code] ?: m.value
            }
        } catch (_: Exception) { m.value }
    }
    fun parseAgora(html: String, base: String): List<UnreadItem> {
        val found = linkedMapOf<String,UnreadItem>()
        for (regex in listOf(frio,standard)) for (m in regex.findAll(html)) {
            val id=m.groupValues[1]
            val blockEnd = html.indexOf("mail-list-outside-wrapper", m.range.last + 1).let { if (it < 0) html.length else it }
            val block = html.substring(m.range.last + 1, blockEnd)
            val version = agoraCount.find(block)?.groupValues?.get(1)?.let(::cleanText).orEmpty()
            found.putIfAbsent(id,UnreadItem(id,cleanText(m.groupValues[2]),"${base.trimEnd('/')}/message/$id",version))
        }
        return found.values.toList()
    }
    private val entry = Regex("""<a\s([^>]*\bmessagePreviewEntry\b[^>]*)>""")
    private val classes = Regex("""class="([^"]*)"""")
    private val id = Regex("""data-message-id="(\d+)"""")
    private val href = Regex("""href="([^"]+)"""")
    private val title = Regex("""<h4[^>]*>([\s\S]*?)<time""")
    fun hasAtriumList(html: String): Boolean = html.contains("messagePreviewEntry") || (!html.contains("<form") && !isLoginPage(html) && !html.contains("<html",true))
    fun parseAtrium(html: String, base: String): List<UnreadItem> {
        val found=linkedMapOf<String,UnreadItem>()
        val entries=entry.findAll(html).toList()
        for ((i,m) in entries.withIndex()) {
            val attrs=m.groupValues[1]
            val cs=classes.find(attrs)?.groupValues?.get(1)?.split(Regex("\\s+")) ?: continue
            if ("unread" !in cs) continue
            val mid=id.find(attrs)?.groupValues?.get(1) ?: continue
            val raw=href.find(attrs)?.groupValues?.get(1)?.let(::decodeEntities) ?: "/mail/mail/index?id=$mid"
            val url=when {
                raw.startsWith("https://team.le-fil.net/") -> raw
                raw.startsWith("https://") -> continue
                raw.startsWith("/") -> base.trimEnd('/')+raw
                else -> base.trimEnd('/')+"/"+raw
            }
            val end=if(i+1<entries.size) entries[i+1].range.first else html.length
            val section=html.substring(m.range.last+1,end)
            val version = lastEntry.find(section)?.groupValues?.get(1)?.let(::cleanText).orEmpty()
            found[mid]=UnreadItem(mid,title.find(section)?.groupValues?.get(1)?.let(::cleanText).orEmpty(),url,version)
        }
        return found.values.toList()
    }
    private val count=Regex(""""newMessages"\s*:\s*"?(\d+)""")
    fun parseNewMessageCount(s:String): Int? = count.find(s)?.groupValues?.get(1)?.toIntOrNull()
}
