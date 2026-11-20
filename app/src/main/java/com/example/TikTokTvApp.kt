package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.LoginScreen
import com.example.comments.CommentsSidePanel
import com.example.messaging.InboxScreen
import com.example.navigation.ActiveOverlay
import com.example.navigation.FeedDpadCallbacks
import com.example.navigation.FeedDpadFocusHost
import com.example.navigation.TvDestination
import com.example.navigation.TvNavigationDrawerOverlay
import com.example.navigation.handleVirtualPointerKeyEvent
import com.example.profile.ProfileAndFavoritesScreen
import com.example.search.SearchScreen
import com.example.settings.SettingsScreen
import com.example.ui.components.TvConfirmExitOverlay
import com.example.ui.components.TvFocusableButton
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokPink
import com.example.ui.theme.TvBlack
import com.example.ui.theme.TvSurfaceOverlay
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.ui.theme.TikTokTvTheme
import com.example.web.ShareVideoOverlay
import com.example.web.TikTokWebViewSurface
import com.example.web.VideoActionsSideBar
import com.example.web.VideoContextMenuOverlay
import kotlinx.coroutines.delay

@Composable
fun TikTokTvApp(
  viewModel: TikTokTvViewModel,
  onExitApplication: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val settings by viewModel.settingsState.collectAsStateWithLifecycle()
  val authState by viewModel.combinedAuthState.collectAsStateWithLifecycle()
  val qrState by viewModel.qrState.collectAsStateWithLifecycle()
  val metadata by viewModel.videoMetadata.collectAsStateWithLifecycle()
  val memorySnapshot by viewModel.memorySnapshot.collectAsStateWithLifecycle()
  val diagnostics by viewModel.diagnostics.collectAsStateWithLifecycle()

  LaunchedEffect(uiState.statusBannerMessage) {
    if (uiState.statusBannerMessage != null) {
      delay(3500L)
      viewModel.dismissStatusBanner()
    }
  }

  TikTokTvTheme(textScale = settings.textScale) {
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(TvBlack)
          .windowInsetsPadding(WindowInsets.safeDrawing)
          .onPreviewKeyEvent { keyEvent ->
            if (uiState.virtualPointerActive && uiState.activeOverlay == ActiveOverlay.NONE) {
              handleVirtualPointerKeyEvent(
                keyEvent = keyEvent,
                onMoveVirtualPointer = viewModel::moveVirtualPointer,
                onClickVirtualPointer = viewModel::clickVirtualPointer,
                onDisableVirtualPointer = viewModel::toggleVirtualPointer,
              )
            } else {
              false
            }
          },
    ) {
      when (uiState.currentDestination) {
        TvDestination.FOR_YOU,
        TvDestination.FOLLOWING,
        TvDestination.LIVES -> {
          FeedPlayerStage(
            destination = uiState.currentDestination,
            viewModel = viewModel,
            uiState = uiState,
            settings = settings,
            metadata = metadata,
          )
        }
        TvDestination.SEARCH -> {
          SearchScreen(
            initialQuery = uiState.searchQuery,
            selectedCategory = uiState.searchCategory,
            searchHistory = settings.searchHistory,
            webController = viewModel.webController,
            performanceProfile = settings.performanceProfile,
            virtualPointerActive = uiState.virtualPointerActive,
            pointerX = uiState.pointerX,
            pointerY = uiState.pointerY,
            onExecuteSearch = viewModel::executeSearch,
            onClearHistory = viewModel::clearSearchHistory,
            onOpenMenu = { viewModel.openOverlay(ActiveOverlay.NAVIGATION_MENU) },
            onToggleVirtualPointer = viewModel::toggleVirtualPointer,
          )
        }
        TvDestination.INBOX -> {
          InboxScreen(
            webSessionStatus = authState.webSessionStatus,
            webController = viewModel.webController,
            performanceProfile = settings.performanceProfile,
            virtualPointerActive = uiState.virtualPointerActive,
            pointerX = uiState.pointerX,
            pointerY = uiState.pointerY,
            onOpenMenu = { viewModel.openOverlay(ActiveOverlay.NAVIGATION_MENU) },
            onOpenLogin = viewModel::openTikTokDirectLogin,
            onToggleVirtualPointer = viewModel::toggleVirtualPointer,
          )
        }
        TvDestination.FAVORITES,
        TvDestination.PROFILE -> {
          ProfileAndFavoritesScreen(
            initialShowFavoritesFocus = uiState.currentDestination == TvDestination.FAVORITES,
            authState = authState,
            displayVideos = uiState.displayVideos,
            isLoadingVideos = uiState.isLoadingDisplayVideos,
            videosError = uiState.displayVideosError,
            webController = viewModel.webController,
            performanceProfile = settings.performanceProfile,
            virtualPointerActive = uiState.virtualPointerActive,
            pointerX = uiState.pointerX,
            pointerY = uiState.pointerY,
            onOpenMenu = { viewModel.openOverlay(ActiveOverlay.NAVIGATION_MENU) },
            onOpenLogin = viewModel::openTikTokDirectLogin,
            onRefreshDisplayApi = viewModel::refreshAuthorizedProfileAndVideos,
            onSelectDisplayVideo = viewModel::openDisplayVideo,
            onToggleVirtualPointer = viewModel::toggleVirtualPointer,
          )
        }
        TvDestination.SETTINGS -> {
          SettingsScreen(
            settings = settings,
            authState = authState,
            capabilities = viewModel.deviceCapabilities,
            memorySnapshot = memorySnapshot,
            diagnostics = diagnostics,
            onOpenMenu = { viewModel.openOverlay(ActiveOverlay.NAVIGATION_MENU) },
            onUpdatePlayback = viewModel::updatePlaybackSetting,
            onSelectProfile = viewModel::setPerformanceProfile,
            onUpdateCacheLimit = viewModel::updateCacheLimitMb,
            onClearCache = viewModel::clearWebAndImageCaches,
            onUpdateNavigation = viewModel::updateNavigationSetting,
            onOpenLogin = viewModel::openTikTokDirectLogin,
            onLogoutAndWipe = viewModel::logoutAndWipeLocalData,
          )
        }
        TvDestination.LOGIN -> {
          LoginScreen(
            authState = authState,
            qrState = qrState,
            webController = viewModel.webController,
            performanceProfile = settings.performanceProfile,
            onStartOAuthQr = viewModel::startOAuthQrAuthorization,
            onCancelOAuthQr = viewModel::cancelOAuthQrAuthorization,
            onLoadWebLogin = { viewModel.webController.loadUrl("https://www.tiktok.com/login") },
            onLoadWebQrLogin = viewModel::loadTikTokWebQrLogin,
            onExploreFeed = { viewModel.navigateTo(TvDestination.FOR_YOU) },
            onToggleVirtualPointer = viewModel::toggleVirtualPointer,
            virtualPointerActive = uiState.virtualPointerActive,
            pointerX = uiState.pointerX,
            pointerY = uiState.pointerY,
          )
        }
      }

      // Auto-hiding Collapsible Navigation Drawer
      TvNavigationDrawerOverlay(
        visible = uiState.activeOverlay == ActiveOverlay.NAVIGATION_MENU,
        currentDestination = uiState.currentDestination,
        authState = authState,
        onSelectDestination = viewModel::navigateTo,
        onOpenLogin = viewModel::openTikTokDirectLogin,
        onDismiss = viewModel::closeOverlay,
      )

      // Video Actions Panel (DPAD_RIGHT)
      VideoActionsSideBar(
        visible = uiState.activeOverlay == ActiveOverlay.ACTION_BAR,
        metadata = metadata,
        virtualPointerActive = uiState.virtualPointerActive,
        onLike = { viewModel.performWebVideoAction("like") },
        onOpenComments = viewModel::openCommentsPanel,
        onRepost = { viewModel.performWebVideoAction("repost") },
        onFavorite = { viewModel.performWebVideoAction("favorite") },
        onOpenShare = { viewModel.openOverlay(ActiveOverlay.SHARE_PANEL) },
        onOpenCreatorProfile = { viewModel.openCreatorProfile(metadata.authorHandle) },
        onToggleFollow = { viewModel.performWebVideoAction("follow") },
        onToggleMute = viewModel::toggleMute,
        onToggleVirtualPointer = viewModel::toggleVirtualPointer,
        onDismiss = viewModel::closeOverlay,
      )

      // Comments Side Panel (video stays playing on left)
      CommentsSidePanel(
        visible = uiState.activeOverlay == ActiveOverlay.COMMENTS_PANEL,
        metadata = metadata,
        webSessionStatus = authState.webSessionStatus,
        onSubmitComment = viewModel::submitComment,
        onOpenWebLogin = viewModel::openTikTokDirectLogin,
        onDismiss = viewModel::closeOverlay,
      )

      // Share & Send-to-Phone QR Overlay
      ShareVideoOverlay(
        visible = uiState.activeOverlay == ActiveOverlay.SHARE_PANEL,
        videoUrl = metadata.videoUrl,
        onCopyLink = viewModel::copyShareLinkToClipboard,
        onShareInTikTokInbox = { viewModel.navigateTo(TvDestination.INBOX) },
        onDismiss = viewModel::closeOverlay,
      )

      // Context Menu Overlay (Long Press OK)
      VideoContextMenuOverlay(
        visible = uiState.activeOverlay == ActiveOverlay.CONTEXT_MENU,
        metadata = metadata,
        blurredAmbientEnabled = settings.blurredAmbientBackground,
        virtualPointerActive = uiState.virtualPointerActive,
        onPlayPause = viewModel::togglePlayPause,
        onToggleAmbient = {
          viewModel.updatePlaybackSetting(blurredAmbient = !settings.blurredAmbientBackground)
        },
        onToggleVirtualPointer = viewModel::toggleVirtualPointer,
        onCopyLink = { viewModel.copyShareLinkToClipboard(metadata.videoUrl) },
        onReportContent = {
          viewModel.closeOverlay()
          viewModel.webController.loadUrl("https://www.tiktok.com/report")
        },
        onOpenSettings = { viewModel.navigateTo(TvDestination.SETTINGS) },
        onDismiss = viewModel::closeOverlay,
      )

      // Exit Confirm Dialog
      if (uiState.activeOverlay == ActiveOverlay.EXIT_CONFIRM) {
        TvConfirmExitOverlay(
          onConfirmExit = onExitApplication,
          onCancel = viewModel::closeOverlay,
        )
      }

      // Status Toast Banner
      AnimatedVisibility(
        visible = !uiState.statusBannerMessage.isNullOrBlank(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Alignment.TopCenter.let { Modifier.align(it).padding(top = 18.dp) },
      ) {
        Box(
          modifier =
            Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(TvSurfaceOverlay)
              .border(1.dp, TikTokCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
              .padding(horizontal = 20.dp, vertical = 10.dp)
              .testTag("status_banner"),
        ) {
          Text(
            text = uiState.statusBannerMessage.orEmpty(),
            color = TvTextPrimary,
            style = MaterialTheme.typography.labelLarge,
          )
        }
      }
    }
  }
}

@Composable
private fun FeedPlayerStage(
  destination: TvDestination,
  viewModel: TikTokTvViewModel,
  uiState: TikTokTvUiState,
  settings: com.example.settings.TvSettingsState,
  metadata: com.example.web.ActiveVideoMetadata,
) {
  val feedFocusRequester = remember { FocusRequester() }
  val menuButtonFocusRequester = remember { FocusRequester() }
  val actionsButtonFocusRequester = remember { FocusRequester() }
  val isOverlayOpen = uiState.activeOverlay != ActiveOverlay.NONE

  val feedDpadCallbacks =
    remember(viewModel) {
      object : FeedDpadCallbacks {
        override fun onPreviousVideo() {
          viewModel.stepVideo(-1)
        }

        override fun onNextVideo() {
          viewModel.stepVideo(1)
        }

        override fun onTogglePlayPause() {
          viewModel.togglePlayPause()
        }

        override fun onOpenNavigationDrawer() {
          viewModel.openOverlay(ActiveOverlay.NAVIGATION_MENU)
        }

        override fun onOpenVideoActions() {
          viewModel.openOverlay(ActiveOverlay.ACTION_BAR)
        }

        override fun onOpenContextMenu() {
          viewModel.openOverlay(ActiveOverlay.CONTEXT_MENU)
        }
      }
    }

  FeedDpadFocusHost(
    focusRequester = feedFocusRequester,
    isOverlayOpen = isOverlayOpen,
    virtualPointerActive = uiState.virtualPointerActive,
    focusTriggerKey = destination,
    menuButtonFocusRequester = menuButtonFocusRequester,
    actionsButtonFocusRequester = actionsButtonFocusRequester,
    onMoveVirtualPointer = viewModel::moveVirtualPointer,
    onClickVirtualPointer = viewModel::clickVirtualPointer,
    onDisableVirtualPointer = viewModel::toggleVirtualPointer,
    callbacks = feedDpadCallbacks,
  ) { _ ->
    Box(modifier = Modifier.fillMaxSize().testTag("feed_stage")) {
      // Optional ambient cyan/pink edge glow for vertical videos when enabled & not Economic mode
      if (settings.blurredAmbientBackground && !metadata.isHorizontal) {
        Box(
          modifier =
            Modifier
              .fillMaxSize()
              .background(
                Brush.horizontalGradient(
                  listOf(
                    TikTokCyan.copy(alpha = 0.08f),
                    Color.Black,
                    Color.Black,
                    TikTokPink.copy(alpha = 0.08f),
                  ),
                ),
              ),
        )
      }

      TikTokWebViewSurface(
        controller = viewModel.webController,
        initialUrl = destination.webUrl,
        performanceProfile = settings.performanceProfile,
        autoplay = settings.autoplay,
        virtualPointerActive = uiState.virtualPointerActive,
        pointerNormalizedX = uiState.pointerX,
        pointerNormalizedY = uiState.pointerY,
        onFullscreenViewChange = {},
        allowWebViewFocus = uiState.virtualPointerActive,
        modifier = Modifier.fillMaxSize(),
      )

      if (viewModel.webController.isLoading) {
        LinearProgressIndicator(
          color = TikTokCyan,
          trackColor = Color.Transparent,
          modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
        )
      }

      // Discreet HUD bar only when overlays are closed, leaving maximum 16:9 playback area
      if (uiState.activeOverlay == ActiveOverlay.NONE) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier =
            Modifier
              .fillMaxWidth()
              .align(Alignment.TopCenter)
              .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
          TvFocusableButton(
            text = destination.title,
            icon = Icons.Default.Menu,
            onClick = { viewModel.openOverlay(ActiveOverlay.NAVIGATION_MENU) },
            modifier =
              Modifier
                .focusRequester(menuButtonFocusRequester)
                .focusProperties {
                  down = feedFocusRequester
                  right = actionsButtonFocusRequester
                },
            testTag = "feed_open_menu_button",
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Box(
              modifier =
                Modifier
                  .clip(RoundedCornerShape(10.dp))
                  .background(TvSurfaceOverlay)
                  .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
              Text(
                text = "▲▼ Vídeo • OK Pausa • ◀ Menu • ▶ Ações • Segure OK Opções",
                color = TvTextSecondary,
                style = MaterialTheme.typography.labelSmall,
              )
            }
            TvFocusableButton(
              text = "Ações",
              icon = Icons.Default.MoreHoriz,
              onClick = { viewModel.openOverlay(ActiveOverlay.ACTION_BAR) },
              modifier =
                Modifier
                  .focusRequester(actionsButtonFocusRequester)
                  .focusProperties {
                    down = feedFocusRequester
                    left = menuButtonFocusRequester
                  },
              testTag = "feed_open_actions_button",
            )
          }
        }

      if (metadata.authorHandle.isNotBlank() || metadata.description.isNotBlank()) {
        Column(
          modifier =
            Modifier
              .align(Alignment.BottomStart)
              .padding(start = 24.dp, bottom = 22.dp, end = 220.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(TvSurfaceOverlay.copy(alpha = 0.72f))
              .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
          if (metadata.authorHandle.isNotBlank()) {
            Text(
              text = metadata.authorHandle,
              color = TikTokCyan,
              style = MaterialTheme.typography.titleMedium,
            )
          }
          if (metadata.description.isNotBlank()) {
            Text(
              text = metadata.description,
              color = TvTextPrimary,
              style = MaterialTheme.typography.bodyMedium,
              maxLines = 2,
            )
          }
        }
      }
    }

    val webErr = viewModel.webController.lastWebError
    if (!webErr.isNullOrBlank()) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier
            .align(Alignment.Center)
            .clip(RoundedCornerShape(16.dp))
            .background(TvSurfaceOverlay)
            .border(1.dp, TikTokPink, RoundedCornerShape(16.dp))
            .padding(24.dp),
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(text = webErr, color = TvTextPrimary, style = MaterialTheme.typography.bodyLarge)
          Spacer(modifier = Modifier.width(4.dp))
          TvFocusableButton(
            text = "Tentar novamente",
            isPrimary = true,
            onClick = { viewModel.webController.reload() },
            testTag = "web_error_retry_button",
          )
        }
      }
    }
  }
  }
}
