package net.lefil.fdroid

import java.net.URI

internal object MessageLinks {
    fun trusted(url: String): Boolean = runCatching {
        val uri = URI(url)
        uri.scheme == "https" && uri.host in setOf("agora.le-fil.net", "atrium.le-fil.net") &&
            uri.userInfo == null && (uri.port == -1 || uri.port == 443)
    }.getOrDefault(false)
}
