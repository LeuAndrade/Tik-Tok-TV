package com.example.player

import android.net.Uri

enum class DecoderMode(
  val displayName: String,
  val description: String,
) {
  AUTOMATIC(
    displayName = "Automático",
    description = "Seleciona automaticamente o decodificador MediaCodec ideal do aparelho.",
  ),
  HARDWARE_STANDARD(
    displayName = "Hardware padrão",
    description = "Prioriza decodificação por hardware do sistema (H.264/HEVC/VP9/AV1) sem extensões.",
  ),
  LOW_LATENCY(
    displayName = "Baixa latência",
    description = "Reduz buffers iniciais para resposta rápida ao D-pad quando suportado pelo hardware.",
  ),
}

object MediaSourceValidator {

  private val directMediaExtensions = setOf(".mp4", ".m3u8", ".mpd", ".webm", ".mkv", ".mov")

  /**
   * TikTok Display API v2 returns embed_link / share_url pointing to HTML pages.
   * Per architecture rules, we NEVER feed HTML embed URLs into ExoPlayer as if they were MP4 streams.
   * ExoPlayer is used strictly for local content:// / file:// media or verified direct media streams.
   */
  fun isPlayableNativeStream(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    val parsed = runCatching { Uri.parse(url.trim()) }.getOrNull() ?: return false
    val scheme = parsed.scheme?.lowercase().orEmpty()
    if (scheme == "content" || scheme == "file") return true
    if (scheme != "https") return false

    val host = parsed.host?.lowercase().orEmpty()
    val path = parsed.path?.lowercase().orEmpty()

    if (host.endsWith("tiktok.com") &&
      (path.contains("/embed") || path.contains("/video/") || path.contains("/static/profile-video"))
    ) {
      return false
    }

    return directMediaExtensions.any { ext -> path.endsWith(ext) } ||
      url.contains("mime_type=video", ignoreCase = true)
  }
}
