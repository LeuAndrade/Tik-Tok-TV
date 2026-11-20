package com.example.web

import android.net.Uri

object TikTokWebDomainPolicy {

  private val tikTokSuffixes =
    listOf(
      "tiktok.com",
      "tiktokcdn.com",
      "tiktokcdn-us.com",
      "tiktokcdn-eu.com",
      "tiktokv.com",
      "byteoversea.com",
      "ttwstatic.com",
      "musical.ly",
      "ibytedtos.com",
    )

  private val trustedIdentityHosts =
    setOf(
      "accounts.google.com",
      "accounts.youtube.com",
      "appleid.apple.com",
      "www.facebook.com",
      "m.facebook.com",
      "web.facebook.com",
      "api.twitter.com",
      "twitter.com",
      "x.com",
      "access.line.me",
      "oauth.vk.com",
      "kauth.kakao.com",
    )

  fun isAllowedNavigationUrl(url: String?, allowIdentityProviders: Boolean = true): Boolean {
    if (url.isNullOrBlank()) return false
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
    val scheme = uri.scheme?.lowercase() ?: return false
    if (scheme == "about" && url == "about:blank") return true
    if (scheme != "https") return false

    val host = uri.host?.lowercase()?.trimEnd('.') ?: return false
    if (isTikTokHost(host)) return true
    if (allowIdentityProviders && host in trustedIdentityHosts) return true
    return false
  }

  fun isTikTokHost(host: String): Boolean {
    val clean = host.lowercase().trimEnd('.')
    return tikTokSuffixes.any { suffix ->
      clean == suffix || clean.endsWith(".$suffix")
    }
  }

  fun isBridgeAllowedOnUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
    if (uri.scheme?.lowercase() != "https") return false
    val host = uri.host?.lowercase()?.trimEnd('.') ?: return false
    return isTikTokHost(host)
  }

  fun sanitizeShareUrl(url: String?): String {
    if (url.isNullOrBlank()) return "https://www.tiktok.com"
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return "https://www.tiktok.com"
    if (uri.scheme?.lowercase() != "https") return "https://www.tiktok.com"
    val host = uri.host?.lowercase() ?: return "https://www.tiktok.com"
    return if (isTikTokHost(host)) url else "https://www.tiktok.com"
  }
}
