package com.example.web

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.performance.MemoryMonitor
import com.example.performance.PerformanceProfile
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokPink
import com.example.ui.theme.TvSurfaceOverlay

class TikTokWebController(
  private val sessionManager: TikTokWebSessionManager,
  val bridge: TikTokWebBridge,
  private val memoryMonitor: MemoryMonitor,
) {
  var webView: WebView? = null
    private set

  private val _currentUrl = mutableStateOf("https://www.tiktok.com/foryou")
  val currentUrl: String get() = _currentUrl.value

  private val _isLoading = mutableStateOf(false)
  val isLoading: Boolean get() = _isLoading.value

  private val _lastWebError = mutableStateOf<String?>(null)
  val lastWebError: String? get() = _lastWebError.value

  internal fun attachWebView(view: WebView) {
    webView = view
    sessionManager.configureCookiePersistence(view)
  }

  fun loadUrl(url: String) {
    if (!TikTokWebDomainPolicy.isAllowedNavigationUrl(url, allowIdentityProviders = true)) {
      _lastWebError.value = "Domínio bloqueado pela política de segurança HTTPS: $url"
      memoryMonitor.recordDiagnostic("Segurança Web", "Navegação bloqueada para domínio não confiável.")
      return
    }
    _lastWebError.value = null
    _currentUrl.value = url
    webView?.loadUrl(url)
  }

  fun reload() {
    _lastWebError.value = null
    webView?.reload()
  }

  fun evaluateBridgeScript(script: String) {
    val view = webView ?: return
    if (!TikTokWebDomainPolicy.isBridgeAllowedOnUrl(view.url)) return
    view.evaluateJavascript(script, null)
  }

  fun simulateTapAtNormalized(normX: Float, normY: Float) {
    val view = webView ?: return
    if (view.width <= 0 || view.height <= 0) return
    val x = normX.coerceIn(0.02f, 0.98f) * view.width
    val y = normY.coerceIn(0.02f, 0.98f) * view.height
    val now = SystemClock.uptimeMillis()
    val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0)
    val up = MotionEvent.obtain(now, now + 45L, MotionEvent.ACTION_UP, x, y, 0)
    view.dispatchTouchEvent(down)
    view.dispatchTouchEvent(up)
    down.recycle()
    up.recycle()
  }

  fun pauseMediaForBackground() {
    webView?.let { view ->
      if (TikTokWebDomainPolicy.isBridgeAllowedOnUrl(view.url)) {
        view.evaluateJavascript("document.querySelectorAll('video').forEach(function(v){v.pause();});", null)
      }
      view.onPause()
    }
    sessionManager.flushCookies()
  }

  fun resumeMedia() {
    webView?.onResume()
  }

  fun clearCache(includeDisk: Boolean = true) {
    webView?.clearCache(includeDisk)
  }

  fun destroyWebView() {
    sessionManager.flushCookies()
    webView?.let { view ->
      view.stopLoading()
      view.removeJavascriptInterface(TikTokWebBridge.JS_INTERFACE_NAME)
      (view.parent as? ViewGroup)?.removeView(view)
      view.destroy()
    }
    webView = null
  }

  internal fun updateLoading(loading: Boolean, url: String?) {
    _isLoading.value = loading
    if (!url.isNullOrBlank()) _currentUrl.value = url
  }

  internal fun recordWebError(message: String) {
    _lastWebError.value = message
    memoryMonitor.recordDiagnostic("WebView", message)
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TikTokWebViewSurface(
  controller: TikTokWebController,
  initialUrl: String,
  performanceProfile: PerformanceProfile,
  autoplay: Boolean,
  virtualPointerActive: Boolean,
  pointerNormalizedX: Float,
  pointerNormalizedY: Float,
  onFullscreenViewChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  allowWebViewFocus: Boolean = true,
) {
  val context = LocalContext.current
  var customFullscreenView by remember { mutableStateOf<View?>(null) }

  val desktopTvUserAgent =
    "Mozilla/5.0 (X11; Linux aarch64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

  val webView = remember(context) {
    WebView(context).apply {
      layoutParams =
        FrameLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT,
        )
      setBackgroundColor(android.graphics.Color.BLACK)
      isFocusable = allowWebViewFocus
      isFocusableInTouchMode = allowWebViewFocus
      descendantFocusability =
        if (allowWebViewFocus) ViewGroup.FOCUS_AFTER_DESCENDANTS else ViewGroup.FOCUS_BLOCK_DESCENDANTS

      settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        mediaPlaybackRequiresUserGesture = !autoplay
        useWideViewPort = true
        loadWithOverviewMode = true
        setSupportZoom(false)
        builtInZoomControls = false
        displayZoomControls = false
        allowFileAccess = false
        allowContentAccess = false
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        cacheMode =
          if (performanceProfile == PerformanceProfile.ECONOMIC) {
            WebSettings.LOAD_DEFAULT
          } else {
            WebSettings.LOAD_DEFAULT
          }
        userAgentString = desktopTvUserAgent
      }

      addJavascriptInterface(controller.bridge, TikTokWebBridge.JS_INTERFACE_NAME)

      webChromeClient =
        object : WebChromeClient() {
          override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            customFullscreenView = view
            onFullscreenViewChange(view != null)
          }

          override fun onHideCustomView() {
            customFullscreenView = null
            onFullscreenViewChange(false)
          }
        }

      webViewClient =
        object : WebViewClient() {
          override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val target = request?.url?.toString()
            return !TikTokWebDomainPolicy.isAllowedNavigationUrl(target, allowIdentityProviders = true)
          }

          override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            controller.updateLoading(true, url)
          }

          override fun onPageFinished(view: WebView?, url: String?) {
            controller.updateLoading(false, url)
            if (TikTokWebDomainPolicy.isBridgeAllowedOnUrl(url)) {
              view?.evaluateJavascript(
                TikTokWebBridge.buildObserverInjectionScript(performanceProfile.reduceWebAnimations),
                null,
              )
            }
          }

          override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            if (request?.isForMainFrame == true) {
              controller.recordWebError("Erro ao carregar TikTok Web: ${error?.description ?: "falha de rede"}")
            }
          }

          override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
            handler?.cancel()
            controller.recordWebError("Certificado TLS/SSL inválido bloqueado por segurança.")
          }

          override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
            controller.recordWebError("Processo WebView encerrado por pressão de memória; recarregando com perfil leve.")
            controller.destroyWebView()
            return true
          }
        }

      controller.attachWebView(this)
      loadUrl(initialUrl)
    }
  }

  LaunchedEffect(autoplay) {
    webView.settings.mediaPlaybackRequiresUserGesture = !autoplay
  }

  LaunchedEffect(allowWebViewFocus) {
    webView.isFocusable = allowWebViewFocus
    webView.isFocusableInTouchMode = allowWebViewFocus
    webView.descendantFocusability =
      if (allowWebViewFocus) ViewGroup.FOCUS_AFTER_DESCENDANTS else ViewGroup.FOCUS_BLOCK_DESCENDANTS
    if (!allowWebViewFocus) {
      webView.clearFocus()
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      controller.pauseMediaForBackground()
    }
  }

  Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
    AndroidView(
      factory = { webView },
      update = { view ->
        view.isFocusable = allowWebViewFocus
        view.isFocusableInTouchMode = allowWebViewFocus
      },
      modifier = Modifier.fillMaxSize(),
    )

    customFullscreenView?.let { fsView ->
      AndroidView(
        factory = { fsView },
        modifier = Modifier.fillMaxSize(),
      )
    }

    if (virtualPointerActive) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = pointerNormalizedX * size.width
        val cy = pointerNormalizedY * size.height
        drawCircle(
          color = TikTokCyan.copy(alpha = 0.32f),
          radius = 22.dp.toPx(),
          center = Offset(cx, cy),
        )
        drawCircle(
          color = TikTokPink,
          radius = 10.dp.toPx(),
          center = Offset(cx, cy),
        )
        drawCircle(
          color = Color.White,
          radius = 4.dp.toPx(),
          center = Offset(cx, cy),
        )
      }
      Text(
        text = "Ponteiro Virtual ativo • Setas movem • OK clica • Voltar desativa",
        color = Color.White,
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .padding(top = 12.dp)
            .background(TvSurfaceOverlay)
            .padding(horizontal = 14.dp, vertical = 6.dp),
      )
    }
  }
}
