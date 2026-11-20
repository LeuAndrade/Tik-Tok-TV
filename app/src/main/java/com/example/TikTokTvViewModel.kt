package com.example

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.CombinedAuthState
import com.example.auth.QrAuthorizationState
import com.example.auth.SecureTokenStore
import com.example.auth.TikTokAuthRepository
import com.example.auth.TikTokDisplayVideo
import com.example.auth.WebSessionStatus
import com.example.navigation.ActiveOverlay
import com.example.navigation.SearchCategory
import com.example.navigation.TvDestination
import com.example.performance.DeviceCapabilities
import com.example.performance.DeviceCapabilityDetector
import com.example.performance.MemoryMonitor
import com.example.performance.MemorySnapshot
import com.example.performance.PerformanceProfile
import com.example.performance.PlaybackDiagnosticEntry
import com.example.settings.TvSettingsRepository
import com.example.settings.TvSettingsState
import com.example.web.ActiveVideoMetadata
import com.example.web.TikTokWebBridge
import com.example.web.TikTokWebController
import com.example.web.TikTokWebDomainPolicy
import com.example.web.TikTokWebSessionManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TikTokTvUiState(
  val currentDestination: TvDestination = TvDestination.FOR_YOU,
  val activeOverlay: ActiveOverlay = ActiveOverlay.NONE,
  val virtualPointerActive: Boolean = false,
  val pointerX: Float = 0.5f,
  val pointerY: Float = 0.5f,
  val statusBannerMessage: String? = null,
  val searchQuery: String = "",
  val searchCategory: SearchCategory = SearchCategory.ALL,
  val displayVideos: List<TikTokDisplayVideo> = emptyList(),
  val isLoadingDisplayVideos: Boolean = false,
  val displayVideosError: String? = null,
  val activeNativeStreamUrl: String? = null,
  val activeNativeStreamTitle: String = "",
)

class TikTokTvViewModel(application: Application) : AndroidViewModel(application) {

  private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val tokenStore = SecureTokenStore(application, moshi)
  val authRepository = TikTokAuthRepository(tokenStore)
  val settingsRepository = TvSettingsRepository(application)
  val memoryMonitor = MemoryMonitor(application)
  val deviceCapabilities: DeviceCapabilities = DeviceCapabilityDetector.detect(application)

  val webSessionManager = TikTokWebSessionManager()
  val webBridge = TikTokWebBridge()
  val webController = TikTokWebController(webSessionManager, webBridge, memoryMonitor)

  private val _uiState = MutableStateFlow(TikTokTvUiState())
  val uiState: StateFlow<TikTokTvUiState> = _uiState.asStateFlow()

  val settingsState: StateFlow<TvSettingsState> =
    settingsRepository.settings.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = TvSettingsState(performanceProfile = deviceCapabilities.recommendedProfile),
    )

  val memorySnapshot: StateFlow<MemorySnapshot> = memoryMonitor.snapshot
  val diagnostics: StateFlow<List<PlaybackDiagnosticEntry>> = memoryMonitor.diagnostics
  val qrState: StateFlow<QrAuthorizationState> = authRepository.qrState
  val videoMetadata: StateFlow<ActiveVideoMetadata> = webBridge.metadata

  val combinedAuthState: StateFlow<CombinedAuthState> =
    combine(
      authRepository.tokenBundleFlow,
      authRepository.userProfileFlow,
      webSessionManager.webStatus,
      webBridge.metadata,
    ) { tokens, profile, webStatus, metadata ->
      CombinedAuthState(
        oauthConnected = tokens != null && !tokens.isRefreshTokenExpired(),
        oauthTokenExpiresAtMs = tokens?.let { it.obtainedAtEpochMs + it.expiresInSeconds * 1000L },
        oauthScopes = tokens?.scope.orEmpty(),
        userProfile = profile,
        webSessionStatus =
          if (metadata.isSecurityChallenge) WebSessionStatus.SECURITY_CHALLENGE else webStatus,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = CombinedAuthState(),
    )

  private var qrPollingJob: Job? = null

  init {
    viewModelScope.launch {
      settingsRepository.initializeRecommendedProfileIfNeeded(deviceCapabilities.recommendedProfile)
    }
    webSessionManager.refreshSessionStatus()

    viewModelScope.launch {
      webBridge.actionResults.collectLatest { result ->
        if (result.message.isNotBlank()) {
          showStatusBanner(result.message)
        }
        if (result.newState == "login_required") {
          webSessionManager.refreshSessionStatus()
        }
      }
    }

    viewModelScope.launch {
      webBridge.metadata.collectLatest { meta ->
        webSessionManager.refreshSessionStatus(
          pageUrl = meta.videoUrl,
          securityChallengeDetected = meta.isSecurityChallenge,
        )
      }
    }
  }

  fun navigateTo(destination: TvDestination) {
    _uiState.update {
      it.copy(
        currentDestination = destination,
        activeOverlay = ActiveOverlay.NONE,
        activeNativeStreamUrl = null,
      )
    }
    if (destination != TvDestination.SETTINGS && destination != TvDestination.LOGIN) {
      webController.loadUrl(destination.webUrl)
    }
    if (destination == TvDestination.PROFILE || destination == TvDestination.FAVORITES) {
      refreshAuthorizedProfileAndVideos()
    }
    memoryMonitor.refreshMemorySnapshot()
  }

  fun openOverlay(overlay: ActiveOverlay) {
    _uiState.update { it.copy(activeOverlay = overlay) }
  }

  fun closeOverlay() {
    _uiState.update { it.copy(activeOverlay = ActiveOverlay.NONE) }
  }

  fun handleBackPress(): Boolean {
    val state = _uiState.value
    if (state.virtualPointerActive) {
      _uiState.update { it.copy(virtualPointerActive = false) }
      return true
    }
    if (state.activeOverlay != ActiveOverlay.NONE) {
      _uiState.update { it.copy(activeOverlay = ActiveOverlay.NONE) }
      return true
    }
    if (state.activeNativeStreamUrl != null) {
      _uiState.update { it.copy(activeNativeStreamUrl = null) }
      return true
    }
    if (state.currentDestination != TvDestination.FOR_YOU) {
      navigateTo(TvDestination.FOR_YOU)
      return true
    }
    if (settingsState.value.confirmBeforeExit) {
      _uiState.update { it.copy(activeOverlay = ActiveOverlay.EXIT_CONFIRM) }
      return true
    }
    return false
  }

  fun toggleVirtualPointer() {
    _uiState.update {
      val next = !it.virtualPointerActive
      it.copy(
        virtualPointerActive = next,
        activeOverlay = ActiveOverlay.NONE,
        statusBannerMessage =
          if (next) "Ponteiro Virtual ativado. Use as setas e OK."
          else "Ponteiro Virtual desativado.",
      )
    }
  }

  fun moveVirtualPointer(dx: Float, dy: Float) {
    val speed = settingsState.value.virtualPointerSensitivity
    _uiState.update {
      it.copy(
        pointerX = (it.pointerX + dx * speed).coerceIn(0.02f, 0.98f),
        pointerY = (it.pointerY + dy * speed).coerceIn(0.02f, 0.98f),
      )
    }
  }

  fun clickVirtualPointer() {
    val state = _uiState.value
    webController.simulateTapAtNormalized(state.pointerX, state.pointerY)
  }

  fun stepVideo(direction: Int) {
    webController.evaluateBridgeScript(TikTokWebBridge.buildStepVideoScript(direction))
  }

  fun togglePlayPause() {
    webController.evaluateBridgeScript(TikTokWebBridge.buildPlayPauseScript())
  }

  fun toggleMute() {
    webController.evaluateBridgeScript(TikTokWebBridge.buildToggleMuteScript())
  }

  fun performWebVideoAction(actionName: String) {
    webController.evaluateBridgeScript(TikTokWebBridge.buildToggleActionScript(actionName))
  }

  fun openCommentsPanel() {
    webController.evaluateBridgeScript(TikTokWebBridge.buildToggleActionScript("comments"))
    _uiState.update { it.copy(activeOverlay = ActiveOverlay.COMMENTS_PANEL) }
  }

  fun submitComment(text: String) {
    if (text.isBlank()) {
      showStatusBanner("Digite um comentário antes de enviar.")
      return
    }
    webController.evaluateBridgeScript(TikTokWebBridge.buildPostCommentScript(text))
  }

  fun executeSearch(query: String, category: SearchCategory) {
    val trimmed = query.trim()
    _uiState.update { it.copy(searchQuery = trimmed, searchCategory = category) }
    if (trimmed.isBlank()) return
    viewModelScope.launch {
      settingsRepository.recordSearchQuery(trimmed)
    }
    val encoded = android.net.Uri.encode(trimmed)
    val url =
      when (category) {
        SearchCategory.ALL -> "https://www.tiktok.com/search?q=$encoded"
        SearchCategory.VIDEOS -> "https://www.tiktok.com/search/video?q=$encoded"
        SearchCategory.USERS -> "https://www.tiktok.com/search/user?q=$encoded"
        SearchCategory.HASHTAGS -> "https://www.tiktok.com/tag/${trimmed.removePrefix("#")}"
        SearchCategory.SOUNDS -> "https://www.tiktok.com/search?q=$encoded"
        SearchCategory.LIVES -> "https://www.tiktok.com/search/live?q=$encoded"
      }
    webController.loadUrl(url)
  }

  fun openCreatorProfile(handleOrUrl: String) {
    val clean = handleOrUrl.trim().removePrefix("@")
    val url =
      if (handleOrUrl.startsWith("https://")) {
        TikTokWebDomainPolicy.sanitizeShareUrl(handleOrUrl)
      } else if (clean.isNotBlank()) {
        "https://www.tiktok.com/@$clean"
      } else {
        "https://www.tiktok.com/profile"
      }
    _uiState.update {
      it.copy(
        currentDestination = TvDestination.PROFILE,
        activeOverlay = ActiveOverlay.NONE,
      )
    }
    webController.loadUrl(url)
  }

  fun openTikTokDirectLogin() {
    _uiState.update {
      it.copy(
        currentDestination = TvDestination.LOGIN,
        activeOverlay = ActiveOverlay.NONE,
      )
    }
    webController.loadUrl("https://www.tiktok.com/login")
  }

  fun loadTikTokWebQrLogin() {
    webController.loadUrl("https://www.tiktok.com/login/qrcode")
  }

  fun startOAuthQrAuthorization() {
    qrPollingJob?.cancel()
    qrPollingJob =
      viewModelScope.launch {
        authRepository.startQrCodeLoginFlow().collectLatest { state ->
          if (state is QrAuthorizationState.Confirmed) {
            showStatusBanner("Login Kit autorizado para ${state.profile?.displayName ?: "sua conta"}!")
            refreshAuthorizedProfileAndVideos()
          }
        }
      }
  }

  fun cancelOAuthQrAuthorization() {
    qrPollingJob?.cancel()
    authRepository.resetQrState()
  }

  fun refreshAuthorizedProfileAndVideos() {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoadingDisplayVideos = true, displayVideosError = null) }
      authRepository.fetchAndPersistUserInfo()
      val result = authRepository.fetchAuthorizedUserVideos()
      result
        .onSuccess { data ->
          _uiState.update {
            it.copy(
              displayVideos = data.videos,
              isLoadingDisplayVideos = false,
              displayVideosError = null,
            )
          }
        }
        .onFailure { err ->
          _uiState.update {
            it.copy(
              isLoadingDisplayVideos = false,
              displayVideosError = err.message,
            )
          }
        }
    }
  }

  fun openDisplayVideo(video: TikTokDisplayVideo) {
    val targetUrl =
      when {
        video.embedLink.startsWith("https://") -> video.embedLink
        video.shareUrl.startsWith("https://") -> video.shareUrl
        else -> "https://www.tiktok.com"
      }
    _uiState.update {
      it.copy(
        currentDestination = TvDestination.FOR_YOU,
        activeOverlay = ActiveOverlay.NONE,
        activeNativeStreamUrl = null,
      )
    }
    webController.loadUrl(targetUrl)
  }

  fun copyShareLinkToClipboard(url: String) {
    val safeUrl = TikTokWebDomainPolicy.sanitizeShareUrl(url)
    val cm = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText("Link do TikTok", safeUrl))
    showStatusBanner("Link copiado: $safeUrl")
  }

  fun clearWebAndImageCaches() {
    webController.clearCache(includeDisk = true)
    memoryMonitor.recordDiagnostic("Cache", "Cache de navegador e mídia limpos manualmente.")
    memoryMonitor.refreshMemorySnapshot()
    showStatusBanner("Cache limpo com sucesso.")
  }

  fun setPerformanceProfile(profile: PerformanceProfile) {
    viewModelScope.launch {
      settingsRepository.setPerformanceProfile(profile)
      showStatusBanner("Perfil ${profile.displayName} aplicado.")
    }
  }

  fun updatePlaybackSetting(
    autoplay: Boolean? = null,
    loopVideo: Boolean? = null,
    blurredAmbient: Boolean? = null,
    fullscreenHorizontal: Boolean? = null,
    autoQuality: Boolean? = null,
  ) {
    viewModelScope.launch {
      settingsRepository.updatePlaybackSetting(
        autoplay = autoplay,
        loopVideo = loopVideo,
        blurredAmbient = blurredAmbient,
        fullscreenHorizontal = fullscreenHorizontal,
        autoQuality = autoQuality,
      )
    }
  }

  fun updateCacheLimitMb(limitMb: Int) {
    viewModelScope.launch {
      settingsRepository.updateCacheLimitMb(limitMb)
    }
  }

  fun updateNavigationSetting(
    pointerSensitivity: Float? = null,
    scrollSpeed: Float? = null,
    confirmBeforeExit: Boolean? = null,
    textScale: Float? = null,
  ) {
    viewModelScope.launch {
      settingsRepository.updateNavigationSetting(
        pointerSensitivity = pointerSensitivity,
        scrollSpeed = scrollSpeed,
        confirmBeforeExit = confirmBeforeExit,
        textScale = textScale,
      )
    }
  }

  fun clearSearchHistory() {
    viewModelScope.launch {
      settingsRepository.clearSearchHistory()
      showStatusBanner("Histórico de pesquisa limpo.")
    }
  }

  fun logoutAndWipeLocalData() {
    qrPollingJob?.cancel()
    viewModelScope.launch {
      authRepository.logoutAndWipeOAuth()
      webSessionManager.wipeAllWebSessionData(getApplication(), webController.webView)
      settingsRepository.clearSearchHistory()
      memoryMonitor.clearDiagnostics()
      _uiState.update {
        it.copy(
          displayVideos = emptyList(),
          displayVideosError = null,
          activeOverlay = ActiveOverlay.NONE,
          currentDestination = TvDestination.LOGIN,
        )
      }
      showStatusBanner("Sessões OAuth e cookies web excluídos deste aparelho.")
    }
  }

  fun showStatusBanner(message: String) {
    _uiState.update { it.copy(statusBannerMessage = message) }
  }

  fun dismissStatusBanner() {
    _uiState.update { it.copy(statusBannerMessage = null) }
  }

  override fun onCleared() {
    qrPollingJob?.cancel()
    webController.destroyWebView()
    super.onCleared()
  }
}
