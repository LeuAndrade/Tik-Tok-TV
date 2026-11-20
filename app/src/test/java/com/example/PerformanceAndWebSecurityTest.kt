package com.example

import android.content.Context
import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import com.example.navigation.ActiveOverlay
import com.example.navigation.RemoteAction
import com.example.navigation.TvDestination
import com.example.navigation.TvRemoteKeyHandler
import com.example.performance.MemoryMonitor
import com.example.performance.PerformanceProfile
import com.example.web.TikTokWebDomainPolicy
import com.example.web.TikTokWebSessionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PerformanceAndWebSecurityTest {

  @Test
  fun `1GB Fire TV Stick selects ECONOMIC profile automatically`() {
    assertEquals(
      PerformanceProfile.ECONOMIC,
      PerformanceProfile.recommendedForRamMb(totalRamMb = 1024L, isLowRamFlag = true),
    )
    assertEquals(
      PerformanceProfile.ECONOMIC,
      PerformanceProfile.recommendedForRamMb(totalRamMb = 1400L, isLowRamFlag = false),
    )
    assertEquals(
      PerformanceProfile.BALANCED,
      PerformanceProfile.recommendedForRamMb(totalRamMb = 2048L, isLowRamFlag = false),
    )
    assertEquals(
      PerformanceProfile.QUALITY,
      PerformanceProfile.recommendedForRamMb(totalRamMb = 4096L, isLowRamFlag = false),
    )
  }

  @Test
  fun `web domain policy enforces HTTPS and blocks untrusted or cleartext hosts`() {
    assertTrue(TikTokWebDomainPolicy.isAllowedNavigationUrl("https://www.tiktok.com/foryou"))
    assertTrue(TikTokWebDomainPolicy.isAllowedNavigationUrl("https://m.tiktok.com/login"))
    assertTrue(TikTokWebDomainPolicy.isAllowedNavigationUrl("https://accounts.google.com/o/oauth2/auth"))
    assertFalse(TikTokWebDomainPolicy.isAllowedNavigationUrl("http://www.tiktok.com/foryou"))
    assertFalse(TikTokWebDomainPolicy.isAllowedNavigationUrl("https://evil-tiktok-phish.example.com"))
    assertFalse(TikTokWebDomainPolicy.isAllowedNavigationUrl("javascript:alert(1)"))
    assertFalse(TikTokWebDomainPolicy.isBridgeAllowedOnUrl("https://accounts.google.com/"))
    assertTrue(TikTokWebDomainPolicy.isBridgeAllowedOnUrl("https://www.tiktok.com/@creator"))
  }

  @Test
  fun `session manager detects auth cookie presence and diagnostic monitor redacts secrets`() {
    assertTrue(TikTokWebSessionManager.hasAuthSessionCookieNames("ttwid=abc; sessionid=xyz123; msToken=1"))
    assertFalse(TikTokWebSessionManager.hasAuthSessionCookieNames("ttwid=abc; msToken=1"))

    val redacted = MemoryMonitor.sanitizeDiagnosticMessage("Authorization: Bearer secret_abc access_token=12345")
    assertFalse(redacted.contains("secret_abc"))
    assertFalse(redacted.contains("12345"))
  }

  @Test
  fun `tv remote key handler maps feed actions, long-press ok, comments context, and virtual pointer`() {
    assertEquals(
      RemoteAction.PreviousVideo,
      TvRemoteKeyHandler.resolveFeedKeyAction(
        keyCode = KeyEvent.KEYCODE_DPAD_UP,
        action = KeyEvent.ACTION_DOWN,
        isLongPress = false,
        activeOverlay = ActiveOverlay.NONE,
        currentDestination = TvDestination.FOR_YOU,
        virtualPointerActive = false,
      ),
    )
    assertEquals(
      RemoteAction.NextVideo,
      TvRemoteKeyHandler.resolveFeedKeyAction(
        keyCode = KeyEvent.KEYCODE_DPAD_DOWN,
        action = KeyEvent.ACTION_DOWN,
        isLongPress = false,
        activeOverlay = ActiveOverlay.NONE,
        currentDestination = TvDestination.FOR_YOU,
        virtualPointerActive = false,
      ),
    )
    assertEquals(
      RemoteAction.PlayPause,
      TvRemoteKeyHandler.resolveFeedKeyAction(
        keyCode = KeyEvent.KEYCODE_DPAD_CENTER,
        action = KeyEvent.ACTION_DOWN,
        isLongPress = false,
        activeOverlay = ActiveOverlay.NONE,
        currentDestination = TvDestination.FOR_YOU,
        virtualPointerActive = false,
      ),
    )
    assertEquals(
      RemoteAction.OpenContextMenu,
      TvRemoteKeyHandler.resolveFeedKeyAction(
        keyCode = KeyEvent.KEYCODE_DPAD_CENTER,
        action = KeyEvent.ACTION_DOWN,
        isLongPress = true,
        activeOverlay = ActiveOverlay.NONE,
        currentDestination = TvDestination.FOR_YOU,
        virtualPointerActive = false,
      ),
    )
    // Inside Comments panel, Up/Down should NOT change video
    assertEquals(
      RemoteAction.None,
      TvRemoteKeyHandler.resolveFeedKeyAction(
        keyCode = KeyEvent.KEYCODE_DPAD_DOWN,
        action = KeyEvent.ACTION_DOWN,
        isLongPress = false,
        activeOverlay = ActiveOverlay.COMMENTS_PANEL,
        currentDestination = TvDestination.FOR_YOU,
        virtualPointerActive = false,
      ),
    )
    // Virtual pointer mode moves cursor
    val move =
      TvRemoteKeyHandler.resolveFeedKeyAction(
        keyCode = KeyEvent.KEYCODE_DPAD_RIGHT,
        action = KeyEvent.ACTION_DOWN,
        isLongPress = false,
        activeOverlay = ActiveOverlay.NONE,
        currentDestination = TvDestination.LOGIN,
        virtualPointerActive = true,
      )
    assertTrue(move is RemoteAction.MoveVirtualPointer)
  }

  @Test
  fun `memory monitor samples device memory cleanly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val monitor = MemoryMonitor(context)
    val snap = monitor.refreshMemorySnapshot()
    assertTrue(snap.maxHeapMb > 0L)
  }
}
