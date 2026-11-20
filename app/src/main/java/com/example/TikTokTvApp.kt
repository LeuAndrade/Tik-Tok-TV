package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.LoginScreen
import com.example.comments.CommentsSidePanel
import com.example.feed.FeedScreen
import com.example.feed.VideoActionsSideBar
import com.example.feed.VideoContextMenuOverlay
import com.example.live.LiveScreen
import com.example.messaging.InboxScreen
import com.example.navigation.ActiveOverlay
import com.example.navigation.TvDestination
import com.example.navigation.TvNavigationDrawerOverlay
import com.example.profile.ProfileAndFavoritesScreen
import com.example.search.SearchScreen
import com.example.settings.SettingsScreen
import com.example.sharing.PublishVideoScreen
import com.example.sharing.ShareVideoOverlay
import com.example.ui.components.TvConfirmExitOverlay
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokTvTheme
import com.example.ui.theme.TvBlack
import com.example.ui.theme.TvSurfaceOverlay
import com.example.ui.theme.TvTextPrimary
import kotlinx.coroutines.delay

@Composable
fun TikTokTvApp(
  viewModel: TikTokTvViewModel,
  onExitApplication: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val settings by viewModel.settingsState.collectAsStateWithLifecycle()
  val authState by viewModel.nativeAuthState.collectAsStateWithLifecycle()
  val qrState by viewModel.qrState.collectAsStateWithLifecycle()
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
          .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
      when (uiState.currentDestination) {
        TvDestination.HOME,
        TvDestination.FOR_YOU,
        TvDestination.FOLLOWING,
        TvDestination.EXPLORE -> {
          FeedScreen(
            destination = uiState.currentDestination,
            authState = authState,
            displayVideos = uiState.displayVideos,
            activeVideo = uiState.activeVideo,
            activeVideoIndex = uiState.activeVideoIndex,
            isPaused = uiState.isPlayerPaused,
            isMuted = uiState.isPlayerMuted,
            isLoadingVideos = uiState.isLoadingDisplayVideos,
            videosError = uiState.displayVideosError,
            activeOverlay = uiState.activeOverlay,
            settings = settings,
            memoryMonitor = viewModel.memoryMonitor,
            onOpenMenu = { viewModel.openOverlay(ActiveOverlay.NAVIGATION_MENU) },
            onOpenVideoActions = { viewModel.openOverlay(ActiveOverlay.ACTION_BAR) },
            onOpenContextMenu = { viewModel.openOverlay(ActiveOverlay.CONTEXT_MENU) },
            onStepVideo = viewModel::stepVideo,
            onTogglePlayPause = viewModel::togglePlayPause,
            onSelectDisplayVideo = viewModel::selectDisplayVideo,
            onRefreshDisplayApi = viewModel::refreshAuthorizedProfileAndVideos,
            onOpenLogin = { viewModel.navigateTo(TvDestination.LOGIN) },
            onOpenPublish = { viewModel.navigateTo(TvDestination.PUBLISH) },
            onOpenProfile = { viewModel.navigateTo(TvDestination.PROFILE) },
            onOpenShare = { viewModel.openOverlay(ActiveOverlay.SHARE_PANEL) },
            onPlayerError = viewModel::showStatusBanner,
          )
        }
        TvDestination.LIVES -> {
          LiveScreen(
            onOpenMenu = { viewModel.openOverlay(ActiveOverlay.NAVIGATION_MENU) },
            onOpenHome = { viewModel.navigateTo(TvDestination.HOME) },
            onOpenLogin = { viewModel.navigateTo(TvDestination.LOGIN) },
          )
        }
        TvDestination.SEARCH -> {
          SearchScreen(
            initialQuery = uiState.searchQuery,
            selectedCategory = uiState.searchCategory,
            searchHistory = settings.searchHistory,
            authState = authState,
            results = uiState.searchResults,
            isSearching = uiState.isSearching,
            searchError = uiState.searchError,
            onExecuteSearch = viewModel::executeSearch,
            onSelectVideo = viewModel::selectDisplayVideo,
            onClearHistory = viewModel::clearSearchHistory,
            onOpenMenu = { viewModel.openOverlay(ActiveOverlay.NAVIGATION_MENU) },
            onOpenLogin = { viewModel.navigateTo(TvDestination.LOGIN) },
          )
        }
        TvDestination.INBOX -> {
          InboxScreen(
            onOpenMenu = { viewModel.openOverlay(ActiveOverlay.NAVIGATION_MENU) },
            onOpenHome = { viewModel.navigateTo(TvDestination.HOME) },
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
            onOpenMenu = { viewModel.openOverlay(ActiveOverlay.NAVIGATION_MENU) },
            onOpenLogin = { viewModel.navigateTo(TvDestination.LOGIN) },
            onRefreshDisplayApi = viewModel::refreshAuthorizedProfileAndVideos,
            onSelectDisplayVideo = viewModel::selectDisplayVideo,
          )
        }
        TvDestination.PUBLISH -> {
          PublishVideoScreen(
            authState = authState,
            creatorInfo = uiState.creatorInfo,
            publishState = uiState.publishState,
            selectedLocalUri = uiState.selectedPublishUri,
            settings = settings,
            memoryMonitor = viewModel.memoryMonitor,
            onSelectLocalUri = viewModel::selectLocalPublishUri,
            onLoadCreatorInfo = viewModel::loadCreatorInfo,
            onPublishVideo = viewModel::publishLocalVideo,
            onOpenMenu = { viewModel.openOverlay(ActiveOverlay.NAVIGATION_MENU) },
            onOpenLogin = { viewModel.navigateTo(TvDestination.LOGIN) },
          )
        }
        TvDestination.SETTINGS -> {
          SettingsScreen(
            settings = settings,
            authState = authState,
            capabilities = viewModel.deviceCapabilities,
            memorySnapshot = memorySnapshot,
            mediaCacheUsageMb = uiState.mediaCacheUsageLabel,
            diagnostics = diagnostics,
            onOpenMenu = { viewModel.openOverlay(ActiveOverlay.NAVIGATION_MENU) },
            onUpdatePlayback = viewModel::updatePlaybackSetting,
            onSelectDecoderMode = viewModel::setDecoderMode,
            onSelectProfile = viewModel::setPerformanceProfile,
            onUpdateCacheLimit = viewModel::updateCacheLimitMb,
            onClearCache = viewModel::clearAllCaches,
            onUpdateNavigation = viewModel::updateNavigationSetting,
            onOpenLogin = { viewModel.navigateTo(TvDestination.LOGIN) },
            onLogoutAndWipe = viewModel::logoutAndWipeLocalData,
          )
        }
        TvDestination.LOGIN -> {
          LoginScreen(
            authState = authState,
            qrState = qrState,
            onStartOAuthQr = viewModel::startOAuthQrAuthorization,
            onCancelOAuthQr = viewModel::cancelOAuthQrAuthorization,
            onExploreApp = { viewModel.navigateTo(TvDestination.HOME) },
          )
        }
      }

      // Auto-hiding Collapsible Native Navigation Drawer
      TvNavigationDrawerOverlay(
        visible = uiState.activeOverlay == ActiveOverlay.NAVIGATION_MENU,
        currentDestination = uiState.currentDestination,
        authState = authState,
        onSelectDestination = viewModel::navigateTo,
        onOpenLogin = { viewModel.navigateTo(TvDestination.LOGIN) },
        onDismiss = viewModel::closeOverlay,
      )

      // Video Actions Panel (DPAD_RIGHT)
      VideoActionsSideBar(
        visible = uiState.activeOverlay == ActiveOverlay.ACTION_BAR,
        video = uiState.activeVideo,
        isPaused = uiState.isPlayerPaused,
        isMuted = uiState.isPlayerMuted,
        onPreviousVideo = { viewModel.stepVideo(-1) },
        onNextVideo = { viewModel.stepVideo(1) },
        onTogglePlayPause = viewModel::togglePlayPause,
        onOpenComments = viewModel::openCommentsPanel,
        onOpenShare = { viewModel.openOverlay(ActiveOverlay.SHARE_PANEL) },
        onOpenCreatorProfile = { viewModel.navigateTo(TvDestination.PROFILE) },
        onToggleMute = viewModel::toggleMute,
        onBlockedSocialAction = viewModel::notifyBlockedSocialAction,
        onDismiss = viewModel::closeOverlay,
      )

      // Comments Side Panel
      CommentsSidePanel(
        visible = uiState.activeOverlay == ActiveOverlay.COMMENTS_PANEL,
        video = uiState.activeVideo,
        onOpenShareQr = { viewModel.openOverlay(ActiveOverlay.SHARE_PANEL) },
        onDismiss = viewModel::closeOverlay,
      )

      // Share & Send-to-Phone QR Overlay
      ShareVideoOverlay(
        visible = uiState.activeOverlay == ActiveOverlay.SHARE_PANEL,
        videoTitle = uiState.activeVideo?.title.orEmpty(),
        videoUrl = uiState.activeVideo?.bestShareUrl ?: "https://www.tiktok.com",
        onCopyLink = viewModel::copyShareLinkToClipboard,
        onDismiss = viewModel::closeOverlay,
      )

      // Context Menu Overlay (Long Press OK / Menu)
      VideoContextMenuOverlay(
        visible = uiState.activeOverlay == ActiveOverlay.CONTEXT_MENU,
        video = uiState.activeVideo,
        blurredAmbientEnabled = settings.blurredAmbientBackground,
        onPlayPause = viewModel::togglePlayPause,
        onToggleAmbient = {
          viewModel.updatePlaybackSetting(blurredAmbient = !settings.blurredAmbientBackground)
        },
        onCopyLink = {
          viewModel.copyShareLinkToClipboard(uiState.activeVideo?.bestShareUrl ?: "https://www.tiktok.com")
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
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp),
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
