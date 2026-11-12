package com.aliazouzi.arabicide.editor

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

// الأصل الذي يخدمه WebViewAssetLoader (موارد محلية فقط، بدون إنترنت)
private const val APP_ORIGIN = "https://appassets.androidplatform.net"

@Composable
fun EditorView(controller: EditorController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val webView = remember { createEditorWebView(context, controller) }

    DisposableEffect(webView) {
        controller.webView = webView
        onDispose {
            controller.webView = null
            controller.markDetached()
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.stopLoading()
            webView.destroy()
        }
    }

    AndroidView(factory = { webView }, modifier = modifier)
}

@SuppressLint("SetJavaScriptEnabled")
private fun createEditorWebView(context: Context, controller: EditorController): WebView {
    val assetLoader = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .build()

    val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    if (debuggable) WebView.setWebContentsDebuggingEnabled(true)

    val view = WebView(context)
    view.setBackgroundColor(0xFF1E1E1E.toInt())

    val s = view.settings
    s.javaScriptEnabled = true
    s.domStorageEnabled = true
    s.allowFileAccess = false
    s.allowContentAccess = false
    s.textZoom = 100
    s.setSupportZoom(false)
    s.builtInZoomControls = false

    view.webViewClient = object : WebViewClient() {
        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest,
        ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

        // لا نسمح بالتنقل إلى أي صفحة أخرى داخل المحرر
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
    }

    view.webChromeClient = object : WebChromeClient() {
        override fun onConsoleMessage(m: ConsoleMessage): Boolean {
            val src = m.sourceId().orEmpty().substringAfterLast('/')
            controller.onLog("[" + m.messageLevel() + "] " + m.message() + " (" + src + ":" + m.lineNumber() + ")")
            return true
        }
    }

    // الجسر: يقبل رسائل من أصل التطبيق المحلي فقط
    if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
        WebViewCompat.addWebMessageListener(
            view,
            "ArabicIDE",
            setOf(APP_ORIGIN),
        ) { _, message, _, _, _ ->
            val data = message.data
            if (data != null) controller.handleMessage(data)
        }
    } else {
        controller.onLog("هذا الـWebView لا يدعم WebMessageListener. حدّث Android System WebView.")
    }

    view.loadUrl("$APP_ORIGIN/assets/editor/index.html")
    return view
}
