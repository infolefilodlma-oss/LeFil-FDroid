package net.lefil.fdroid

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.RenderProcessGoneDetail
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import android.view.Menu
import android.widget.Toast

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
        private const val STATE_LAST_URL = "site_last_url"
        const val EXTRA_URL = "net.lefil.fdroid.OPEN_URL"
    }

    private var webView: WebView? = null
    private var lastSiteUrl = FIL_URL

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted ->
        if (granted) enableWatch() else Toast.makeText(this, "Notifications refusées : activez-les dans les réglages Android pour recevoir les messages.", Toast.LENGTH_LONG).show()
    }

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.app_root)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        configureMenu()
        configureWebView(findViewById(R.id.site_webview))
        findViewById<View>(R.id.reload_site).setOnClickListener { loadSite(lastSiteUrl) }

        val messageUrl = messageUrlFrom(intent)
        if (messageUrl != null) {
            loadSite(messageUrl)
        } else {
            lastSiteUrl = savedInstanceState?.getString(STATE_LAST_URL) ?: FIL_URL
            val history = savedInstanceState?.getBundle(STATE_WEBVIEW)?.let { webView?.restoreState(it) }
            if (history == null) loadSite(lastSiteUrl)
        }

        MessageWatchService.start(this)
        offerNotificationsOnce()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView?.canGoBack() == true) {
                        webView?.goBack()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    private fun configureWebView(view: WebView) {
        this.webView = view
        val webView = view
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
                if (uri.scheme == "https" && isFilHost(uri.host)) {
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
                if (view !== this@MainActivity.webView) return
                if (url != null) {
                    val uri = Uri.parse(url)
                    if (uri.scheme == "https" && isFilHost(uri.host)) lastSiteUrl = url
                }
                progress.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String?) {
                if (view !== this@MainActivity.webView) return
                progress.visibility = View.GONE
                CookieManager.getInstance().flush()
                // Just logged in to Agora or Atrium: pair this phone for native notifications without waiting.
                val host = url?.let { Uri.parse(it).host }
                if (host == "agora.le-fil.net" || host == "atrium.le-fil.net") MessageWatchService.pollNow(this@MainActivity)
            }

            @RequiresApi(26)
            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                val current = view === this@MainActivity.webView
                if (current) this@MainActivity.webView = null
                (view.parent as? ViewGroup)?.removeView(view)
                view.destroy()
                if (current) {
                    val callback = pendingFileCallback
                    pendingFileCallback = null
                    runCatching { callback?.onReceiveValue(null) }
                    progress.visibility = View.GONE
                    findViewById<View>(R.id.site_renderer_error).visibility = View.VISIBLE
                }
                return true
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
                    pendingFileCallback?.onReceiveValue(null)
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
    }

    private fun loadSite(url: String) {
        lastSiteUrl = url
        val view = webView ?: WebView(this).also { replacement ->
            replacement.id = R.id.site_webview
            replacement.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            findViewById<FrameLayout>(R.id.site_container).addView(replacement, 0)
            configureWebView(replacement)
        }
        findViewById<View>(R.id.site_renderer_error).visibility = View.GONE
        findViewById<View>(R.id.site_progress).visibility = View.VISIBLE
        view.loadUrl(url)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        messageUrlFrom(intent)?.let { loadSite(it) }
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
        MessageWatchService.pollNow(this)
    }

    private fun messageUrlFrom(intent: Intent?): String? {
        val url = intent?.getStringExtra(EXTRA_URL) ?: return null
        return url.takeIf(MessageLinks::trusted)
    }

    private fun configureMenu() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.title = "Le Fil"
        val destinations = listOf(
            "Accueil" to FIL_URL,
            "Agora" to "https://agora.le-fil.net/",
            "Atrium" to "https://atrium.le-fil.net/",
            "Talk (Atrium)" to "https://atrium.le-fil.net/index.php?r=mail%2Fmail%2Findex"
        )
        destinations.forEachIndexed { index, (label, url) ->
            toolbar.menu.add(Menu.NONE, index + 1, index, label).setOnMenuItemClickListener {
                loadSite(url); true
            }
        }
        toolbar.menu.add("Notifications des messages").setOnMenuItemClickListener {
            if (MessageWatchService.enabled(this)) {
                AlertDialog.Builder(this).setTitle("Notifications activées")
                    .setMessage("La surveillance utilise votre connexion Agora et Atrium. Elle peut être retardée par la veille ou les restrictions Android.")
                    .setPositiveButton("Désactiver") { _, _ -> MessageWatchService.stop(this) }
                    .setNegativeButton("Conserver", null).show()
            } else {
                AlertDialog.Builder(this).setTitle("Activer les notifications ?")
                    .setMessage("Connectez-vous à Agora et à Atrium dans l’application. Une notification permanente signale la vérification des messages, environ chaque minute lorsque le téléphone est actif. Android peut retarder la réception en veille. Aucun service Google n’est utilisé.")
                    .setPositiveButton("Activer") { _, _ -> askNotificationPermission() }
                    .setNegativeButton("Plus tard", null).show()
            }
            true
        }
        toolbar.menu.add("Réglages des notifications").setOnMenuItemClickListener {
            val settings = if (Build.VERSION.SDK_INT >= 26) Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
            runCatching { startActivity(settings) }; true
        }
        toolbar.menu.add("Batterie et arrière-plan").setOnMenuItemClickListener {
            AlertDialog.Builder(this).setTitle("Réception en arrière-plan")
                .setMessage("La veille, l’économie de batterie, une session expirée ou un arrêt forcé peuvent interrompre les vérifications. Après un arrêt forcé, rouvrez l’application. Vous pouvez adapter les réglages de batterie d’Android ; la réception instantanée n’est pas garantie.")
                .setPositiveButton("Réglages Android") { _, _ ->
                    runCatching { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                }.setNegativeButton("Fermer", null).show()
            true
        }
    }

    private fun offerNotificationsOnce() {
        val prefs = getSharedPreferences("lefil_watch", Context.MODE_PRIVATE)
        if (MessageWatchService.enabled(this) || prefs.getBoolean("notification_intro_seen", false)) return
        prefs.edit().putBoolean("notification_intro_seen", true).apply()
        AlertDialog.Builder(this)
            .setTitle("Recevoir les messages du Fil ?")
            .setMessage("Le Fil peut vous avertir des messages et nouvelles activités d’Agora, d’Atrium et de Talk lorsque vous êtes connecté aux deux services. La veille du téléphone peut retarder certains messages.")
            .setPositiveButton("Activer") { _, _ -> askNotificationPermission() }
            .setNegativeButton("Plus tard", null)
            .show()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else enableWatch()
    }

    private fun enableWatch() {
        MessageWatchService.setEnabled(this, true)
        if (!MessageWatchService.start(this)) {
            MessageWatchService.setEnabled(this, false)
            Toast.makeText(this, "Android n’a pas autorisé le démarrage. Réessayez avec l’application ouverte.", Toast.LENGTH_LONG).show()
        } else offerBatteryExemption()
    }

    private fun offerBatteryExemption() {
        val power = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        if (power.isIgnoringBatteryOptimizations(packageName)) return
        AlertDialog.Builder(this)
            .setTitle("Messages même téléphone verrouillé")
            .setMessage("Android peut suspendre les messages pendant la veille. Vous pouvez autoriser Le Fil à vérifier les messages en arrière-plan. Cela consomme davantage de batterie et ne garantit pas une réception instantanée sur tous les téléphones.")
            .setPositiveButton("Autoriser") { _, _ ->
                val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName"))
                if (runCatching { startActivity(request) }.isFailure) {
                    runCatching { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                }
            }
            .setNegativeButton("Plus tard", null)
            .show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_LAST_URL, lastSiteUrl)
        webView?.let { current ->
            val state = Bundle()
            current.saveState(state)
            outState.putBundle(STATE_WEBVIEW, state)
        }
    }
}
