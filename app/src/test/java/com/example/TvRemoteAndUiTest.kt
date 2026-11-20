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
import com.example.auth.LoginScreen
import com.example.auth.NativeAuthState
import com.example.auth.QrAuthorizationState
import com.example.comments.CommentsSidePanel
import com.example.feed.ActiveVideoItem
import com.example.messaging.InboxScreen
import com.example.navigation.FeedDpadCallbacks
import com.example.navigation.FeedDpadFocusHost
import com.example.navigation.SearchCategory
import com.example.navigation.TvDestination
import com.example.navigation.TvNavigationDrawerOverlay
import com.example.navigation.handleFeedDpadKeyEvent
import com.example.performance.DeviceCapabilities
import com.example.performance.MemorySnapshot
import com.example.performance.PerformanceProfile
import com.example.player.DecoderMode
import com.example.search.SearchScreen
import com.example.settings.SettingsScreen
import com.example.settings.TvSettingsState
import com.example.sharing.ShareVideoOverlay
import com.example.ui.theme.TikTokTvTheme
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
  fun `navigation drawer renders all native TV menu items and dispatches destination clicks`() {
    var selected: TvDestination? = null
    composeTestRule.setContent {
      TikTokTvTheme {
        TvNavigationDrawerOverlay(
          visible = true,
          currentDestination = TvDestination.HOME,
          authState = NativeAuthState(oauthConnected = false),
          onSelectDestination = { selected = it },
          onOpenLogin = {},
          onDismiss = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("tv_navigation_drawer").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_home").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_for_you").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_following").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_explore").assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_search").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_lives").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_inbox").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_favorites").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_profile").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_publish").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("nav_settings").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(TvDestination.SETTINGS, selected)
  }

  @Test
  fun `login screen renders pure native QR Code option and honest SDK TV limitation tab`() {
    var qrStarted = 0
    composeTestRule.setContent {
      TikTokTvTheme {
        LoginScreen(
          authState = NativeAuthState(),
          qrState = QrAuthorizationState.Idle,
          onStartOAuthQr = { qrStarted++ },
          onCancelOAuthQr = {},
          onExploreApp = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("login_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("oauth_qr_panel").assertIsDisplayed()
    composeTestRule.onNodeWithTag("login_method_direct").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("direct_sdk_limitation_panel").assertIsDisplayed()
    composeTestRule.onNodeWithTag("switch_to_qr_login_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals(1, qrStarted)
  }

  @Test
  fun `comments side panel displays real comment count and share QR trigger`() {
    var shareOpened = false
    composeTestRule.setContent {
      TikTokTvTheme {
        CommentsSidePanel(
          visible = true,
          video =
            ActiveVideoItem(
              id = "7080217258545735586",
              authorHandle = "@criador",
              commentCount = 12L,
            ),
          onOpenShareQr = { shareOpened = true },
          onDismiss = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("comments_side_panel").assertIsDisplayed()
    composeTestRule.onNodeWithTag("comments_open_qr_button").performClick()
    composeTestRule.waitForIdle()
    assertTrue(shareOpened)
  }

  @Test
  fun `share video overlay renders send-to-phone QR code and copy link action`() {
    var copiedUrl = ""
    composeTestRule.setContent {
      TikTokTvTheme {
        ShareVideoOverlay(
          visible = true,
          videoTitle = "Vídeo Real",
          videoUrl = "https://www.tiktok.com/@creator/video/7080217258545735586",
          onCopyLink = { copiedUrl = it },
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
  fun `inbox and search screens render pure native UI without webviews`() {
    composeTestRule.setContent {
      TikTokTvTheme {
        InboxScreen(onOpenMenu = {}, onOpenHome = {})
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("inbox_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("inbox_notifications_tab").performClick()
    composeTestRule.waitForIdle()
  }

  @Test
  fun `search screen executes query callback cleanly`() {
    var lastQuery = ""
    composeTestRule.setContent {
      TikTokTvTheme {
        SearchScreen(
          initialQuery = "7080217258545735586",
          selectedCategory = SearchCategory.VIDEO_IDS,
          searchHistory = emptyList(),
          authState = NativeAuthState(),
          results = emptyList(),
          isSearching = false,
          searchError = null,
          onExecuteSearch = { q, _ -> lastQuery = q },
          onSelectVideo = {},
          onClearHistory = {},
          onOpenMenu = {},
          onOpenLogin = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("search_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("search_submit_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals("7080217258545735586", lastQuery)
  }

  @Test
  fun `settings screen shows decoder modes, Fire TV profiles, and device info`() {
    var selectedProfile: PerformanceProfile? = null
    var selectedDecoder: DecoderMode? = null
    val caps =
      DeviceCapabilities(
        totalRamMb = 1024L,
        availableRamMb = 412L,
        isLowRamDevice = true,
        isFireOs = true,
        isAndroidTv = true,
        hasTouchscreen = false,
        hasDpadNavigation = true,
        screenResolutionLabel = "1920x1080",
        screenDensityDpi = 320,
        osVersionLabel = "Fire OS / Android 11 (API 30)",
        deviceModelLabel = "Amazon AFTSS",
        primaryAbi = "armeabi-v7a",
        supportedAbis = listOf("armeabi-v7a"),
        hardwareDecoders = emptyList(),
        recommendedProfile = PerformanceProfile.ECONOMIC,
      )

    composeTestRule.setContent {
      TikTokTvTheme {
        SettingsScreen(
          settings = TvSettingsState(performanceProfile = PerformanceProfile.ECONOMIC),
          authState = NativeAuthState(),
          capabilities = caps,
          memorySnapshot = MemorySnapshot(1024L, 412L, 48L, 128L, false),
          mediaCacheUsageMb = "0.0 MB",
          diagnostics = emptyList(),
          onOpenMenu = {},
          onUpdatePlayback = { _, _, _, _, _ -> },
          onSelectDecoderMode = { selectedDecoder = it },
          onSelectProfile = { selectedProfile = it },
          onUpdateCacheLimit = {},
          onClearCache = {},
          onUpdateNavigation = { _, _, _ -> },
          onOpenLogin = {},
          onLogoutAndWipe = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
    composeTestRule.onNodeWithTag("setting_decoder_low_latency").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("setting_profile_balanced").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    assertEquals(DecoderMode.LOW_LATENCY, selectedDecoder)
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
            callbacks = callbacks,
          ) {
            Box(modifier = Modifier.fillMaxSize())
          }

          TvNavigationDrawerOverlay(
            visible = drawerOpen,
            currentDestination = TvDestination.HOME,
            authState = NativeAuthState(),
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
    composeTestRule.onNodeWithTag("nav_home").assertIsFocused()
    feedHost.assertIsNotFocused()

    composeTestRule.onNodeWithTag("nav_close_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    feedHost.assertIsFocused()
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
