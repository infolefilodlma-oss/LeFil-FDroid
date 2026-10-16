package net.lefil.fdroid

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * Interface unique de l'application : le site complet https://le-fil.net/
 * (WordPress, LWS) dans un WebView natif. Aucune interface de service n'est
 * reconstruite, aucun onglet artificiel : la navigation est celle du site
 * lui-même, y compris vers ses sous-domaines réels (Agora, Atrium, Talk,
 * Radio...). Seuls les liens réellement externes à le-fil.net s'ouvrent
 * dans le navigateur du téléphone.
 */
class MainActivity : AppCompatActivity(R.layout.activity_main) {

    companion object {
        private const val FIL_URL = "https://le-fil.net/"
        private const val STATE_WEBVIEW = "site_webview_state"
    }

    private lateinit var webView: WebView

    private var pendingFileCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        pendingFileCallback?.onReceiveValue(uri?.let { arrayOf(it) })
        pendingFileCallback = null
    }

    private fun isFilHost(host: String?): Boolean {
        return host != null && (host == "le-fil.net" || host.endsWith(".le-fil.net"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = findViewById(R.id.site_webview)
        val progress = findViewById<ProgressBar>(R.id.site_progress)

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: android.webkit.WebResourceRequest
            ): Boolean {
                val uri = request.url
                if (isFilHost(uri.host)) {
                    return false
                }
                // Hors du Fil (lien réellement externe) : ouvert par le
                // navigateur du système, jamais chargé dans ce WebView.
                runCatching {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
                return true
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                progress.visibility = android.view.View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String?) {
                progress.visibility = android.view.View.GONE
                CookieManager.getInstance().flush()
            }
        }

        // Nécessaire pour que les champs <input type="file"> des
        // formulaires du site (ex. dépôt d'avatar, pièce jointe) fonctionnent
        // réellement dans un WebView : sans ce rappel, ils restent inertes.
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                pendingFileCallback?.onReceiveValue(null)
                pendingFileCallback = filePathCallback
                runCatching {
                    fileChooserLauncher.launch("*/*")
                }.onFailure {
                    pendingFileCallback = null
                }
                return true
            }
        }

        // Téléchargements (ex. pièce jointe, export) : le WebView ne les
        // gère pas lui-même, ils sont transmis au système (navigateur ou
        // gestionnaire de téléchargement), sans permission supplémentaire.
        webView.setDownloadListener { url, _, _, _, _ ->
            runCatching {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState.getBundle(STATE_WEBVIEW) ?: Bundle())
        } else {
            webView.loadUrl(FIL_URL)
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )

    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val state = Bundle()
        webView.saveState(state)
        outState.putBundle(STATE_WEBVIEW, state)
    }
}
