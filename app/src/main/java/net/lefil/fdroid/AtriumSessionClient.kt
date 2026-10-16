package net.lefil.fdroid

import android.webkit.CookieManager
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Transport en lecture seule pour un endpoint Atrium authentifie par la session WebView.
 * Ne demarre aucun service et ne transmet jamais les cookies a un autre hote.
 * Le endpoint et son raccordement aux notifications seront valides separement.
 */
internal class AtriumSessionClient(
    private val readCookies: (String) -> String? = { url -> CookieManager.getInstance().getCookie(url) },
    private val openConnection: (URL) -> HttpURLConnection = { url ->
        url.openConnection() as HttpURLConnection
    }
) {
    sealed class Result {
        data class Json(val body: String) : Result()
        object LoginRequired : Result()
        object InvalidUrl : Result()
        object InvalidResponse : Result()
        data class HttpError(val status: Int) : Result()
        object NetworkError : Result()
    }

    fun get(url: String): Result {
        val uri = try {
            URI(url)
        } catch (_: Exception) {
            return Result.InvalidUrl
        }
        if (!uri.scheme.equals("https", ignoreCase = true) ||
            !uri.host.equals("team.le-fil.net", ignoreCase = true) ||
            (uri.port != -1 && uri.port != 443) ||
            uri.userInfo != null || uri.fragment != null
        ) {
            return Result.InvalidUrl
        }

        val cookies = try {
            readCookies(url)?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        } ?: return Result.LoginRequired

        var connection: HttpURLConnection? = null
        return try {
            connection = openConnection(uri.toURL())
            connection.instanceFollowRedirects = false
            connection.requestMethod = "GET"
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.setRequestProperty("Cookie", cookies)
            connection.setRequestProperty("Accept", "application/json")
            when (val status = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val contentType = connection.contentType.orEmpty().lowercase(Locale.ROOT)
                    if (!contentType.startsWith("application/json")) {
                        Result.InvalidResponse
                    } else {
                        val stream = connection.inputStream
                        val output = ByteArrayOutputStream()
                        stream.use { input ->
                            val buffer = ByteArray(4096)
                            var size = 0
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                size += count
                                if (size > 65_536) return Result.InvalidResponse
                                output.write(buffer, 0, count)
                            }
                        }
                        Result.Json(String(output.toByteArray(), StandardCharsets.UTF_8))
                    }
                }
                301, 302, 303, 307, 308, 401, 403 -> Result.LoginRequired
                else -> Result.HttpError(status)
            }
        } catch (_: Exception) {
            Result.NetworkError
        } finally {
            connection?.disconnect()
        }
    }
}
