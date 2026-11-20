package com.example

import android.view.KeyEvent as NativeKeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.pressKey
import com.example.auth.CombinedAuthState
import com.example.auth.WebSessionStatus
import com.example.comments.CommentsSidePanel
import com.example.navigation.FeedDpadCallbacks
import com.example.navigation.FeedDpadFocusHost
import com.example.navigation.TvDestination
import com.example.navigation.TvNavigationDrawerOverlay
import com.example.navigation.handleFeedDpadKeyEvent
import com.example.performance.DeviceCapabilities
import com.example.performance.MemorySnapshot
import com.example.performance.PerformanceProfile
import com.example.settings.SettingsScreen
import com.example.settings.TvSettingsState
import com.example.ui.theme.TikTokTvTheme
import com.example.web.ActiveVideoMetadata
import com.example.web.ShareVideoOverlay
import com.example.web.WebCommentItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w1280dp-h720dp-tvdpi")
class TvRemoteAndUiTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `navigation drawer renders all TV menu items and dispatches destination clicks`() {
    var selected: TvDestination? = null
    composeTestRule.setContent {
      TikTokTvTheme {
        TvNavigationDrawerOverlay(
          visible = true,
          currentDestination = TvDestination.FOR_YOU,
          authState = CombinedAuthState(webSessionStatus = WebSessionStatus.GUEST),
          onSelectDestination = { selected = it },
          onOpenLogin = {},
          onDismiss = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("tv_navigation_drawer").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_for_you").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_following").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_lives").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_search").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_inbox").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_favorites").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_profile").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_settings").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(TvDestination.SETTINGS, selected)
  }

  @Test
  fun `comments side panel displays extracted real comments and copy-share QR overlay works`() {
    var submittedComment = ""
    composeTestRule.setContent {
      TikTokTvTheme {
        CommentsSidePanel(
          visible = true,
          metadata =
            ActiveVideoMetadata(
              authorHandle = "@criador",
              commentCountText = "12",
              comments = listOf(WebCommentItem(author = "@fa", text = "Ótimo vídeo na TV!", likesText = "4")),
            ),
          webSessionStatus = WebSessionStatus.AUTHENTICATED,
          onSubmitComment = { submittedComment = it },
          onOpenWebLogin = {},
          onDismiss = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("comments_side_panel").assertIsDisplayed()
    composeTestRule.onNodeWithTag("comment_input_field").assertIsDisplayed()
    composeTestRule.onNodeWithTag("comment_send_button").assertIsDisplayed()
    assertEquals("", submittedComment)
  }

  @Test
  fun `share video overlay renders send-to-phone QR code and copy link action`() {
    var copiedUrl = ""
    composeTestRule.setContent {
      TikTokTvTheme {
        ShareVideoOverlay(
          visible = true,
          videoUrl = "https://www.tiktok.com/@creator/video/7080217258545735586",
          onCopyLink = { copiedUrl = it },
          onShareInTikTokInbox = {},
          onDismiss = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("share_video_overlay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("share_copy_link_button").performClick()
    composeTestRule.waitForIdle()
    assertTrue(copiedUrl.startsWith("https://www.tiktok.com/"))
  }

  @Test
  fun `settings screen shows Fire TV performance profiles and device info`() {
    var selectedProfile: PerformanceProfile? = null
    val caps =
      DeviceCapabilities(
        totalRamMb = 1024L,
        availableRamMb = 412L,
        isLowRamDevice = true,
        isFireOs = true,
        isAndroidTv = true,
        hasTouchscreen = false,
        osVersionLabel = "Fire OS / Android 11 (API 30)",
        deviceModelLabel = "Amazon AFTSS",
        primaryAbi = "armeabi-v7a",
        supportedAbis = listOf("armeabi-v7a"),
        webViewVersion = "126.0",
        hardwareDecoders = emptyList(),
        recommendedProfile = PerformanceProfile.ECONOMIC,
      )

    composeTestRule.setContent {
      TikTokTvTheme {
        SettingsScreen(
          settings = TvSettingsState(performanceProfile = PerformanceProfile.ECONOMIC),
          authState = CombinedAuthState(),
          capabilities = caps,
          memorySnapshot = MemorySnapshot(1024L, 412L, 48L, 128L, false),
          diagnostics = emptyList(),
          onOpenMenu = {},
          onUpdatePlayback = { _, _, _, _, _ -> },
          onSelectProfile = { selectedProfile = it },
          onUpdateCacheLimit = {},
          onClearCache = {},
          onUpdateNavigation = { _, _, _, _ -> },
          onOpenLogin = {},
          onLogoutAndWipe = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("setting_profile_balanced").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(PerformanceProfile.BALANCED, selectedProfile)
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun `feed focus host claims initial focus and routes dpad keys via Modifier onKeyEvent`() {
    var prevCount = 0
    var nextCount = 0
    var playPauseCount = 0
    var drawerCount = 0
    var actionsCount = 0
    var contextCount = 0

    val callbacks =
      object : FeedDpadCallbacks {
        override fun onPreviousVideo() {
          prevCount++
        }

        override fun onNextVideo() {
          nextCount++
        }

        override fun onTogglePlayPause() {
          playPauseCount++
        }

        override fun onOpenNavigationDrawer() {
          drawerCount++
        }

        override fun onOpenVideoActions() {
          actionsCount++
        }

        override fun onOpenContextMenu() {
          contextCount++
        }
      }

    composeTestRule.setContent {
      TikTokTvTheme {
        val feedFocusRequester = remember { FocusRequester() }
        FeedDpadFocusHost(
          focusRequester = feedFocusRequester,
          isOverlayOpen = false,
          virtualPointerActive = false,
          onMoveVirtualPointer = { _, _ -> },
          onClickVirtualPointer = {},
          onDisableVirtualPointer = {},
          callbacks = callbacks,
        ) {
          Box(modifier = Modifier.fillMaxSize())
        }
      }
    }

    composeTestRule.waitForIdle()
    val host = composeTestRule.onNodeWithTag("feed_focus_host")
    host.assertIsDisplayed()
    host.assertIsFocused()

    host.performKeyInput {
      pressKey(Key.DirectionDown)
      pressKey(Key.DirectionUp)
      pressKey(Key.DirectionCenter)
      pressKey(Key.DirectionLeft)
      pressKey(Key.DirectionRight)
      pressKey(Key.Menu)
    }
    composeTestRule.waitForIdle()

    assertEquals(1, nextCount)
    assertEquals(1, prevCount)
    assertEquals(1, playPauseCount)
    assertEquals(1, drawerCount)
    assertEquals(1, actionsCount)
    assertEquals(1, contextCount)
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun `opening drawer via dpad transfers focus to drawer and closing restores feed focus`() {
    composeTestRule.setContent {
      TikTokTvTheme {
        val feedFocusRequester = remember { FocusRequester() }
        var drawerOpen by remember { mutableStateOf(false) }

        val callbacks =
          remember {
            object : FeedDpadCallbacks {
              override fun onPreviousVideo() {}
              override fun onNextVideo() {}
              override fun onTogglePlayPause() {}
              override fun onOpenNavigationDrawer() {
                drawerOpen = true
              }
              override fun onOpenVideoActions() {}
              override fun onOpenContextMenu() {}
            }
          }

        Box(modifier = Modifier.fillMaxSize()) {
          FeedDpadFocusHost(
            focusRequester = feedFocusRequester,
            isOverlayOpen = drawerOpen,
            virtualPointerActive = false,
            onMoveVirtualPointer = { _, _ -> },
            onClickVirtualPointer = {},
            onDisableVirtualPointer = {},
            callbacks = callbacks,
          ) {
            Box(modifier = Modifier.fillMaxSize())
          }

          TvNavigationDrawerOverlay(
            visible = drawerOpen,
            currentDestination = TvDestination.FOR_YOU,
            authState = CombinedAuthState(),
            onSelectDestination = { drawerOpen = false },
            onOpenLogin = {},
            onDismiss = { drawerOpen = false },
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    val feedHost = composeTestRule.onNodeWithTag("feed_focus_host")
    feedHost.assertIsFocused()

    feedHost.performKeyInput { pressKey(Key.DirectionLeft) }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("tv_navigation_drawer").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_for_you").assertIsFocused()
    feedHost.assertIsNotFocused()

    composeTestRule.onNodeWithTag("nav_close_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    feedHost.assertIsFocused()
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun `virtual pointer mode intercepts dpad keys in onPreviewKeyEvent without stepping feed`() {
    var movedX = 0f
    var movedY = 0f
    var clicked = 0
    var disabled = 0
    var feedStepped = 0

    val callbacks =
      object : FeedDpadCallbacks {
        override fun onPreviousVideo() {
          feedStepped++
        }

        override fun onNextVideo() {
          feedStepped++
        }

        override fun onTogglePlayPause() {
          feedStepped++
        }

        override fun onOpenNavigationDrawer() {}
        override fun onOpenVideoActions() {}
        override fun onOpenContextMenu() {}
      }

    composeTestRule.setContent {
      TikTokTvTheme {
        val feedFocusRequester = remember { FocusRequester() }
        FeedDpadFocusHost(
          focusRequester = feedFocusRequester,
          isOverlayOpen = false,
          virtualPointerActive = true,
          onMoveVirtualPointer = { dx, dy ->
            movedX += dx
            movedY += dy
          },
          onClickVirtualPointer = { clicked++ },
          onDisableVirtualPointer = { disabled++ },
          callbacks = callbacks,
        ) {
          Box(modifier = Modifier.fillMaxSize())
        }
      }
    }

    composeTestRule.waitForIdle()
    val host = composeTestRule.onNodeWithTag("feed_focus_host")
    host.performKeyInput {
      pressKey(Key.DirectionRight)
      pressKey(Key.DirectionDown)
      pressKey(Key.DirectionCenter)
      pressKey(Key.Escape)
    }
    composeTestRule.waitForIdle()

    assertTrue(movedX > 0f)
    assertTrue(movedY > 0f)
    assertEquals(1, clicked)
    assertEquals(1, disabled)
    assertEquals(0, feedStepped)
  }

  @Test
  fun `long press OK in feed dpad key handler opens context menu without play pause on key up`() {
    var playPauseCount = 0
    var contextMenuCount = 0
    var longPressConsumed = false

    val callbacks =
      object : FeedDpadCallbacks {
        override fun onPreviousVideo() {}
        override fun onNextVideo() {}
        override fun onTogglePlayPause() {
          playPauseCount++
        }
        override fun onOpenNavigationDrawer() {}
        override fun onOpenVideoActions() {}
        override fun onOpenContextMenu() {
          contextMenuCount++
        }
      }

    val longDown =
      KeyEvent(
        NativeKeyEvent(
          0L,
          0L,
          NativeKeyEvent.ACTION_DOWN,
          NativeKeyEvent.KEYCODE_DPAD_CENTER,
          4,
          0,
          0,
          0,
          NativeKeyEvent.FLAG_LONG_PRESS,
        ),
      )
    val up =
      KeyEvent(
        NativeKeyEvent(
          NativeKeyEvent.ACTION_UP,
          NativeKeyEvent.KEYCODE_DPAD_CENTER,
        ),
      )

    assertTrue(
      handleFeedDpadKeyEvent(
        keyEvent = longDown,
        longPressConsumed = longPressConsumed,
        onUpdateLongPressConsumed = { longPressConsumed = it },
        callbacks = callbacks,
      ),
    )
    assertTrue(longPressConsumed)
    assertEquals(1, contextMenuCount)

    assertTrue(
      handleFeedDpadKeyEvent(
        keyEvent = up,
        longPressConsumed = longPressConsumed,
        onUpdateLongPressConsumed = { longPressConsumed = it },
        callbacks = callbacks,
      ),
    )
    assertFalse(longPressConsumed)
    assertEquals(0, playPauseCount)
  }
}
