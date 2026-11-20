package com.example

import com.example.api.ApiSupportLevel
import com.example.api.TikTokPlatformCapabilities
import com.example.auth.TikTokTokenBundle
import com.example.feed.ActiveVideoItem
import com.example.performance.MemoryMonitor
import com.example.performance.PerformanceProfile
import com.example.player.DecoderMode
import com.example.player.MediaSourceValidator
import com.example.sharing.TikTokShareUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PerformanceAndNativeMediaTest {

  @Test
  fun `recommended performance profile selects economic mode for 1GB Fire TV Stick`() {
    assertEquals(PerformanceProfile.ECONOMIC, PerformanceProfile.recommendedForRamMb(1024L, true))
    assertEquals(PerformanceProfile.ECONOMIC, PerformanceProfile.recommendedForRamMb(1536L, false))
    assertEquals(PerformanceProfile.BALANCED, PerformanceProfile.recommendedForRamMb(2048L, false))
    assertEquals(PerformanceProfile.QUALITY, PerformanceProfile.recommendedForRamMb(4096L, false))
  }

  @Test
  fun `media source validator rejects html embed links and accepts native local or direct streams`() {
    assertFalse(MediaSourceValidator.isPlayableNativeStream("https://www.tiktok.com/embed/v2/7080217258545735586"))
    assertFalse(MediaSourceValidator.isPlayableNativeStream("https://www.tiktok.com/@creator/video/7080217258545735586"))
    assertFalse(MediaSourceValidator.isPlayableNativeStream("http://insecure.example.com/video.mp4"))
    assertFalse(MediaSourceValidator.isPlayableNativeStream(""))

    assertTrue(MediaSourceValidator.isPlayableNativeStream("content://media/external/video/media/42"))
    assertTrue(MediaSourceValidator.isPlayableNativeStream("file:///storage/emulated/0/Movies/sample.mp4"))
    assertTrue(MediaSourceValidator.isPlayableNativeStream("https://cdn.example.com/stream/master.m3u8"))
    assertTrue(MediaSourceValidator.isPlayableNativeStream("https://cdn.example.com/video.mp4"))
  }

  @Test
  fun `share url sanitizer allows only official https tiktok domains`() {
    assertEquals(
      "https://www.tiktok.com/@creator/video/123",
      TikTokShareUtils.sanitizeShareUrl("https://www.tiktok.com/@creator/video/123"),
    )
    assertEquals(
      "https://vm.tiktok.com/ZM123/",
      TikTokShareUtils.sanitizeShareUrl("https://vm.tiktok.com/ZM123/"),
    )
    assertEquals(
      "https://www.tiktok.com",
      TikTokShareUtils.sanitizeShareUrl("https://evil.example.com/phishing"),
    )
    assertEquals(
      "https://www.tiktok.com",
      TikTokShareUtils.sanitizeShareUrl("javascript:alert(1)"),
    )
  }

  @Test
  fun `memory monitor redacts oauth secrets and tokens from diagnostics`() {
    val sanitized =
      MemoryMonitor.sanitizeDiagnosticMessage(
        "Failed request access_token=secret123 refresh_token=abc Bearer eyJhbGciOi",
      )
    assertFalse(sanitized.contains("secret123"))
    assertFalse(sanitized.contains("eyJhbGciOi"))
    assertTrue(sanitized.contains("access_token=[REDACTED]"))
    assertTrue(sanitized.contains("Bearer [REDACTED]"))
  }

  @Test
  fun `platform capabilities matrix transparently classifies native vs blocked apis`() {
    assertTrue(
      TikTokPlatformCapabilities.matrix.any {
        it.supportLevel == ApiSupportLevel.NATIVE_SUPPORTED && it.featureName.contains("QR Code")
      },
    )
    assertTrue(
      TikTokPlatformCapabilities.matrix.any {
        it.supportLevel == ApiSupportLevel.PLATFORM_BLOCKED && it.featureName.contains("Para Você")
      },
    )
    assertEquals(3, DecoderMode.entries.size)
  }

  @Test
  fun `token bundle parses comma and space delimited scopes accurately`() {
    val bundle =
      TikTokTokenBundle(
        accessToken = "act",
        refreshToken = "rft",
        openId = "open_1",
        expiresInSeconds = 3600L,
        refreshExpiresInSeconds = 86400L,
        scope = "user.info.basic, video.list video.publish",
      )
    assertTrue(bundle.hasScope("user.info.basic"))
    assertTrue(bundle.hasScope("video.list"))
    assertTrue(bundle.hasScope("video.publish"))
    assertFalse(bundle.hasScope("research.data.basic"))

    val video = ActiveVideoItem(id = "1", width = 1920, height = 1080)
    assertTrue(video.isHorizontal)
    assertFalse(video.hasPlayableNativeStream)
  }
}
