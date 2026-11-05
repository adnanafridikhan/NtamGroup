package com.ntamgroup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.print.PrintManager
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import java.io.ByteArrayInputStream

internal enum class CongressPage(val slug: String, val title: String) {
    QuickAccess("quick-access", "Quick Access"),
    Agenda("agenda", "Agenda"),
    Speakers("speakers", "Speakers"),
    NameTag("name-tag", "My Name Tag"),
    AlternateNameTag("name-tag-alternate", "My Name Tag"),
    Venue("venue", "Venue"),
    Voting("voting", "Live Voting"),
    Questions("ask-qa", "Ask Q&A"),
    Resources("resources", "Resources"),
    Exhibitors("exhibitors", "Exhibitors"),
    Profile("profile", "My Profile");

    companion object {
        fun fromSlug(slug: String?): CongressPage? = entries.firstOrNull { it.slug == slug }
    }
}

class CongressDetailActivity : ComponentActivity() {
    private var browser: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = CongressPage.fromSlug(intent.getStringExtra(PageExtra))
        if (page == null) {
            finish()
            return
        }

        title = page.title
        val bars = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        val webView = createCongressWebView(this, page) { handleAction(page, it) }
        browser = webView
        // Measure the WebView inside the system bars so fixed HTML footers stay visible.
        val content = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.rgb(4, 22, 51))
            addView(webView, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
            ))
        }
        setContentView(content)
        content.applyCongressInsets()
    }

    private fun handleAction(page: CongressPage, action: String?) {
        when (action) {
            "back" -> finish()
            "print" -> if (page == CongressPage.NameTag || page == CongressPage.AlternateNameTag) {
                browser?.let { webView ->
                    val job = "Congress Name Tag"
                    getSystemService(PrintManager::class.java).print(job, webView.createPrintDocumentAdapter(job), null)
                }
            }
            "directions" -> if (page == CongressPage.Venue) {
                val venue = Uri.encode("Dubai International Convention & Exhibition Center, Dubai")
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$venue")))
                } catch (_: android.content.ActivityNotFoundException) {
                    Toast.makeText(this, "No maps app is available", Toast.LENGTH_SHORT).show()
                }
            }
            else -> CongressPage.fromSlug(action)?.let { open(this, it) }
        }
    }

    override fun onDestroy() {
        browser?.let { webView ->
            (webView.parent as? android.view.ViewGroup)?.removeView(webView)
            webView.destroy()
        }
        browser = null
        super.onDestroy()
    }

    companion object {
        private const val PageExtra = "com.ntamgroup.CONGRESS_PAGE"

        internal fun open(activity: ComponentActivity, page: CongressPage) {
            activity.startActivity(Intent(activity, CongressDetailActivity::class.java).putExtra(PageExtra, page.slug))
        }
    }
}

private const val AssetHost = "appassets.androidplatform.net"
private val AssetName = Regex("[a-z][a-z0-9-]*\\.(html|css|js|woff2)")
internal fun createCongressWebView(
    context: Context,
    page: CongressPage,
    onLoaded: () -> Unit = {},
    onAction: (String?) -> Unit
): WebView = WebView(context).apply {
    setBackgroundColor(android.graphics.Color.rgb(4, 22, 51))
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        blockNetworkLoads = true
        allowFileAccess = false
        allowContentAccess = false
        javaScriptCanOpenWindowsAutomatically = false
    }
    webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            onLoaded()
        }

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse =
            serveAsset(context, request.url)

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url
            if (request.isForMainFrame && uri.scheme == "congress" && uri.host == "action") {
                onAction(uri.pathSegments.singleOrNull())
            }
            return true
        }
    }
    loadUrl("https://$AssetHost/${page.slug}.html")
}

internal fun View.applyCongressInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(
            WindowInsetsCompat.Type.systemBars() or
                WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime()
        )
        view.updatePadding(left = insets.left, top = insets.top, right = insets.right, bottom = insets.bottom)
        WindowInsetsCompat.CONSUMED
    }
    ViewCompat.requestApplyInsets(this)
}

private fun serveAsset(context: Context, uri: Uri): WebResourceResponse {
    val file = uri.pathSegments.singleOrNull()
    if (uri.scheme == "https" && uri.host == AssetHost && file != null && AssetName.matches(file)) {
        val mime = when (file.substringAfterLast('.')) {
            "html" -> "text/html"
            "css" -> "text/css"
            "js" -> "application/javascript"
            "woff2" -> "font/woff2"
            else -> null
        }
        if (mime != null) {
            val stream = try {
                context.assets.open("congress/$file")
            } catch (_: java.io.IOException) {
                null
            }
            if (stream != null) {
                val encoding = if (mime == "font/woff2") null else "UTF-8"
                return WebResourceResponse(mime, encoding, stream)
            }
        }
    }
    return WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", emptyMap(), ByteArrayInputStream(byteArrayOf()))
}

