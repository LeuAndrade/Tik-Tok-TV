package com.example.web

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import com.example.auth.WebSessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TikTokWebSessionManager {

  private val _webStatus = MutableStateFlow(WebSessionStatus.UNKNOWN)
  val webStatus: StateFlow<WebSessionStatus> = _webStatus.asStateFlow()

  fun configureCookiePersistence(webView: WebView) {
    runCatching {
      val cookieManager = CookieManager.getInstance()
      cookieManager.setAcceptCookie(true)
      cookieManager.setAcceptThirdPartyCookies(webView, true)
      cookieManager.flush()
    }
  }

  fun refreshSessionStatus(pageUrl: String? = null, securityChallengeDetected: Boolean = false): WebSessionStatus {
    if (securityChallengeDetected) {
      _webStatus.value = WebSessionStatus.SECURITY_CHALLENGE
      return WebSessionStatus.SECURITY_CHALLENGE
    }
    val hasSessionCookie = checkHasAuthenticatedCookie()
    val status =
      if (hasSessionCookie) {
        WebSessionStatus.AUTHENTICATED
      } else {
        WebSessionStatus.GUEST
      }
    _webStatus.value = status
    return status
  }

  fun flushCookies() {
    runCatching { CookieManager.getInstance().flush() }
  }

  fun checkHasAuthenticatedCookie(): Boolean {
    val rawCookieHeader =
      runCatching { CookieManager.getInstance().getCookie("https://www.tiktok.com") }.getOrNull()
        ?: return false
    return hasAuthSessionCookieNames(rawCookieHeader)
  }

  fun wipeAllWebSessionData(context: Context, webView: WebView?) {
    runCatching {
      val cm = CookieManager.getInstance()
      cm.removeAllCookies(null)
      cm.removeSessionCookies(null)
      cm.flush()
    }
    runCatching { WebStorage.getInstance().deleteAllData() }
    runCatching {
      webView?.clearHistory()
      webView?.clearCache(true)
      webView?.clearFormData()
    }
    runCatching { context.deleteDatabase("webview.db") }
    runCatching { context.deleteDatabase("webviewCache.db") }
    _webStatus.value = WebSessionStatus.GUEST
  }

  companion object {
    internal fun hasAuthSessionCookieNames(cookieHeader: String): Boolean {
      if (cookieHeader.isBlank()) return false
      val keys =
        cookieHeader
          .split(";")
          .mapNotNull { part ->
            val idx = part.indexOf('=')
            if (idx > 0) part.substring(0, idx).trim() else null
          }
          .toSet()
      return "sessionid" in keys || "sid_tt" in keys || "uid_tt" in keys || "sessionid_ss" in keys
    }
  }
}
