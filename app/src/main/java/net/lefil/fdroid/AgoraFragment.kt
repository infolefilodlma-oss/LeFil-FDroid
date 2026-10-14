package net.lefil.fdroid

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment

/**
 * Accès réel à Agora (Friendica, social.le-fil.net) via le WebView officiel
 * Android. Aucune interface Friendica n'est reconstruite : seule la
 * navigation est adaptée au smartphone (plein écran dans l'onglet,
 * domaine restreint, bouton retour natif, session conservée).
 */
class AgoraFragment : Fragment(R.layout.fragment_agora) {

    companion object {
        private const val AGORA_HOST = "social.le-fil.net"
        private const val AGORA_URL = "https://social.le-fil.net/"
        private const val STATE_WEBVIEW = "agora_webview_state"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val webView = view.findViewById<WebView>(R.id.agora_webview)
        val progress = view.findViewById<ProgressBar>(R.id.agora_progress)

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
                if (uri.host == AGORA_HOST) {
                    return false
                }
                // Hors du domaine Agora (ex. lien externe partagé dans un
                // message) : ouvert par le navigateur du système, jamais
                // chargé dans ce WebView.
                runCatching {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
                return true
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                progress.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String?) {
                progress.visibility = View.GONE
                CookieManager.getInstance().flush()
            }
        }

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState.getBundle(STATE_WEBVIEW) ?: Bundle())
        } else {
            webView.loadUrl(AGORA_URL)
        }

        // Bouton retour natif : navigue dans l'historique Agora avant de
        // rendre la main au comportement par défaut (retour à l'onglet
        // précédent / sortie de l'application).
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val webView = view?.findViewById<WebView>(R.id.agora_webview)
        if (webView != null) {
            val state = Bundle()
            webView.saveState(state)
            outState.putBundle(STATE_WEBVIEW, state)
        }
    }
}
