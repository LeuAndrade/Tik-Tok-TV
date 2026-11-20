package com.example

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.example.api.ContentPostingRepository
import com.example.api.ContentPublishState
import com.example.api.CreatorInfoData
import com.example.auth.NativeAuthState
import com.example.auth.QrAuthorizationState
import com.example.auth.SecureTokenStore
import com.example.auth.TikTokAuthRepository
import com.example.auth.TikTokDisplayVideo
import com.example.data.AppCacheDatabase
import com.example.data.TikTokCacheRepository
import com.example.feed.ActiveVideoItem
import com.example.navigation.ActiveOverlay
import com.example.navigation.SearchCategory
import com.example.navigation.TvDestination
import com.example.performance.DeviceCapabilities
import com.example.performance.DeviceCapabilityDetector
import com.example.performance.MemoryMonitor
import com.example.performance.MemorySnapshot
import com.example.performance.PerformanceProfile
import com.example.performance.PlaybackDiagnosticEntry
import com.example.player.DecoderMode
import com.example.player.VideoCacheManager
import com.example.settings.TvSettingsRepository
import com.example.settings.TvSettingsState
import com.example.sharing.TikTokShareUtils
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
  val currentDestination: TvDestination = TvDestination.HOME,
  val activeOverlay: ActiveOverlay = ActiveOverlay.NONE,
  val statusBannerMessage: String? = null,
  val searchQuery: String = "",
  val searchCategory: SearchCategory = SearchCategory.VIDEO_IDS,
  val searchResults: List<TikTokDisplayVideo> = emptyList(),
  val isSearching: Boolean = false,
  val searchError: String? = null,
  val displayVideos: List<TikTokDisplayVideo> = emptyList(),
  val activeVideoIndex: Int = -1,
  val activeVideo: ActiveVideoItem? = null,
  val isPlayerPaused: Boolean = false,
  val isPlayerMuted: Boolean = false,
  val isLoadingDisplayVideos: Boolean = false,
  val displayVideosError: String? = null,
  val creatorInfo: CreatorInfoData? = null,
  val publishState: ContentPublishState = ContentPublishState.Idle,
  val selectedPublishUri: Uri? = null,
  val mediaCacheUsageLabel: String = "0 MB",
)

class TikTokTvViewModel(application: Application) : AndroidViewModel(application) {

  private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val tokenStore = SecureTokenStore(application, moshi)
  val authRepository = TikTokAuthRepository(tokenStore)
  val settingsRepository = TvSettingsRepository(application)
  val cacheRepository = TikTokCacheRepository(AppCacheDatabase.getInstance(application).cacheDao())
  val contentPostingRepository = ContentPostingRepository()
  val memoryMonitor = MemoryMonitor(application)
  val deviceCapabilities: DeviceCapabilities = DeviceCapabilityDetector.detect(application)

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

  val nativeAuthState: StateFlow<NativeAuthState> =
    combine(
      authRepository.tokenBundleFlow,
      authRepository.userProfileFlow,
      cacheRepository.cachedProfile,
    ) { tokens, storedProfile, cachedProfile ->
      NativeAuthState(
        oauthConnected = tokens != null && !tokens.isRefreshTokenExpired(),
        oauthTokenExpiresAtMs = tokens?.let { it.obtainedAtEpochMs + it.expiresInSeconds * 1000L },
        oauthScopes = tokens?.scope.orEmpty(),
        userProfile = storedProfile ?: cachedProfile,
      )
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = NativeAuthState(),
    )

  private var qrPollingJob: Job? = null

  init {
    viewModelScope.launch {
      settingsRepository.initializeRecommendedProfileIfNeeded(deviceCapabilities.recommendedProfile)
    }

    // Populate offline cache from Room DB
    viewModelScope.launch {
      cacheRepository.cachedVideos.collectLatest { cached ->
        if (cached.isNotEmpty() && _uiState.value.displayVideos.isEmpty()) {
          _uiState.update { state ->
            state.copy(displayVideos = cached)
          }
        }
      }
    }

    refreshMediaCacheLabel()
  }

  fun navigateTo(destination: TvDestination) {
    _uiState.update {
      it.copy(
        currentDestination = destination,
        activeOverlay = ActiveOverlay.NONE,
      )
    }
    if (destination == TvDestination.PROFILE || destination == TvDestination.HOME) {
      if (nativeAuthState.value.oauthConnected) {
        refreshAuthorizedProfileAndVideos()
      }
    }
    if (destination == TvDestination.PUBLISH && nativeAuthState.value.oauthConnected) {
      loadCreatorInfo()
    }
    refreshMediaCacheLabel()
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
    if (state.activeOverlay != ActiveOverlay.NONE) {
      _uiState.update { it.copy(activeOverlay = ActiveOverlay.NONE) }
      return true
    }
    if (state.activeVideo != null && state.activeVideo.hasPlayableNativeStream) {
      _uiState.update { it.copy(activeVideo = null, activeVideoIndex = -1) }
      return true
    }
    if (state.currentDestination != TvDestination.HOME) {
      navigateTo(TvDestination.HOME)
      return true
    }
    if (settingsState.value.confirmBeforeExit) {
      _uiState.update { it.copy(activeOverlay = ActiveOverlay.EXIT_CONFIRM) }
      return true
    }
    return false
  }

  fun selectDisplayVideo(index: Int) {
    if (index < 0) {
      _uiState.update { it.copy(activeVideoIndex = -1, activeVideo = null) }
      return
    }
    val videos = _uiState.value.displayVideos
    val target = videos.getOrNull(index) ?: return
    val profile = nativeAuthState.value.userProfile
    val activeItem =
      ActiveVideoItem.fromDisplayVideo(
        video = target,
        authorName = profile?.displayName ?: "Criador TikTok",
        authorHandle = profile?.username?.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "@tiktok",
      )
    _uiState.update {
      it.copy(
        currentDestination = TvDestination.HOME,
        activeOverlay = ActiveOverlay.NONE,
        activeVideoIndex = index,
        activeVideo = activeItem,
      )
    }
  }

  fun selectDisplayVideo(video: TikTokDisplayVideo) {
    val existingIdx = _uiState.value.displayVideos.indexOfFirst { it.id == video.id }
    if (existingIdx >= 0) {
      selectDisplayVideo(existingIdx)
    } else {
      val profile = nativeAuthState.value.userProfile
      val activeItem =
        ActiveVideoItem.fromDisplayVideo(
          video = video,
          authorName = profile?.displayName ?: "Criador TikTok",
          authorHandle = profile?.username?.takeIf { it.isNotBlank() }?.let { "@$it" } ?: "@tiktok",
        )
      _uiState.update {
        it.copy(
          currentDestination = TvDestination.HOME,
          activeOverlay = ActiveOverlay.NONE,
          activeVideoIndex = -1,
          activeVideo = activeItem,
        )
      }
    }
  }

  fun stepVideo(direction: Int) {
    val list = _uiState.value.displayVideos
    if (list.isEmpty()) {
      showStatusBanner("Nenhum vídeo carregado para avançar.")
      return
    }
    val current = _uiState.value.activeVideoIndex
    val nextIndex =
      if (current < 0) 0
      else ((current + direction) % list.size + list.size) % list.size
    selectDisplayVideo(nextIndex)
  }

  fun togglePlayPause() {
    _uiState.update { it.copy(isPlayerPaused = !it.isPlayerPaused) }
  }

  fun toggleMute() {
    _uiState.update {
      val next = !it.isPlayerMuted
      it.copy(
        isPlayerMuted = next,
        statusBannerMessage = if (next) "Som silenciado" else "Som ativado",
      )
    }
  }

  fun openCommentsPanel() {
    _uiState.update { it.copy(activeOverlay = ActiveOverlay.COMMENTS_PANEL) }
  }

  fun notifyBlockedSocialAction(actionLabel: String) {
    showStatusBanner("$actionLabel bloqueado: a API oficial do TikTok não autoriza gravação de ações sociais por terceiros.")
  }

  fun executeSearch(query: String, category: SearchCategory) {
    val trimmed = query.trim()
    _uiState.update { it.copy(searchQuery = trimmed, searchCategory = category, searchError = null) }
    if (trimmed.isBlank()) return

    viewModelScope.launch {
      settingsRepository.recordSearchQuery(trimmed)
      val tokens = trimmed.split(",", " ", ";").map { it.trim() }.filter { it.isNotBlank() }
      val allNumericIds = tokens.isNotEmpty() && tokens.all { id -> id.all { it.isDigit() } }

      if (category == SearchCategory.VIDEO_IDS && allNumericIds) {
        _uiState.update { it.copy(isSearching = true, searchError = null) }
        val result = authRepository.queryAuthorizedVideosByIds(tokens)
        result
          .onSuccess { found ->
            _uiState.update {
              it.copy(
                searchResults = found,
                isSearching = false,
                searchError = if (found.isEmpty()) "Nenhum vídeo encontrado na sua conta para os IDs informados." else null,
              )
            }
          }
          .onFailure { err ->
            _uiState.update {
              it.copy(
                searchResults = emptyList(),
                isSearching = false,
                searchError = err.message ?: "Falha ao consultar POST /v2/video/query/.",
              )
            }
          }
      } else {
        // Honest filter over already authorized/cached videos when non-ID text or restricted global search selected
        val filtered =
          _uiState.value.displayVideos.filter { video ->
            video.id.contains(trimmed, ignoreCase = true) ||
              video.title.contains(trimmed, ignoreCase = true) ||
              video.videoDescription.contains(trimmed, ignoreCase = true)
          }
        val notice =
          if (category != SearchCategory.VIDEO_IDS) {
            "Pesquisa global '${category.label}' indisponível na API pública comercial (Research API restrita). Exibindo apenas correspondências nos vídeos já autorizados da conta (${filtered.size})."
          } else {
            null
          }
        _uiState.update {
          it.copy(
            searchResults = filtered,
            isSearching = false,
            searchError = notice,
          )
        }
      }
    }
  }

  fun startOAuthQrAuthorization() {
    qrPollingJob?.cancel()
    qrPollingJob =
      viewModelScope.launch {
        authRepository.startQrCodeLoginFlow(
          requestedScope = "user.info.basic,user.info.profile,user.info.stats,video.list,video.upload,video.publish",
        ).collectLatest { state ->
          if (state is QrAuthorizationState.Confirmed) {
            state.profile?.let { cacheRepository.cacheProfile(it) }
            showStatusBanner("Login Kit autorizado para ${state.profile?.displayName ?: "sua conta"}!")
            refreshAuthorizedProfileAndVideos()
            _uiState.update {
              it.copy(
                currentDestination = TvDestination.HOME,
                activeOverlay = ActiveOverlay.NONE,
              )
            }
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
      val profile = authRepository.fetchAndPersistUserInfo()
      if (profile != null) {
        cacheRepository.cacheProfile(profile)
      }
      val result = authRepository.fetchAuthorizedUserVideos()
      result
        .onSuccess { data ->
          cacheRepository.replaceCachedVideos(data.videos)
          _uiState.update {
            it.copy(
              displayVideos = data.videos,
              isLoadingDisplayVideos = false,
              displayVideosError = null,
            )
          }
        }
        .onFailure { err ->
          memoryMonitor.recordDiagnostic("DisplayAPI", err.message ?: "Erro ao listar vídeos")
          _uiState.update {
            it.copy(
              isLoadingDisplayVideos = false,
              displayVideosError = err.message,
            )
          }
        }
    }
  }

  fun selectLocalPublishUri(uri: Uri) {
    _uiState.update {
      it.copy(
        selectedPublishUri = uri,
        publishState = ContentPublishState.Idle,
      )
    }
  }

  fun loadCreatorInfo() {
    viewModelScope.launch {
      val token = authRepository.ensureValidAccessToken()
      if (token.isNullOrBlank()) {
        showStatusBanner("Conecte sua conta via QR Code para consultar creator_info.")
        return@launch
      }
      _uiState.update { it.copy(publishState = ContentPublishState.LoadingCreatorInfo) }
      contentPostingRepository.queryCreatorInfo(token)
        .onSuccess { info ->
          _uiState.update {
            it.copy(
              creatorInfo = info,
              publishState = ContentPublishState.Idle,
            )
          }
        }
        .onFailure { err ->
          memoryMonitor.recordDiagnostic("ContentPosting", err.message ?: "Erro em creator_info")
          _uiState.update {
            it.copy(publishState = ContentPublishState.Failed(err.message ?: "Erro ao consultar creator_info"))
          }
        }
    }
  }

  fun publishLocalVideo(
    uri: Uri,
    caption: String,
    privacyLevel: String,
    disableComment: Boolean,
    disableDuet: Boolean,
    disableStitch: Boolean,
    publishDirectly: Boolean,
  ) {
    viewModelScope.launch {
      val token = authRepository.ensureValidAccessToken()
      if (token.isNullOrBlank()) {
        _uiState.update {
          it.copy(publishState = ContentPublishState.Failed("Sessão OAuth desconectada ou expirada."))
        }
        return@launch
      }
      val result =
        contentPostingRepository.uploadAndPublishVideo(
          contentResolver = getApplication<Application>().contentResolver,
          videoUri = uri,
          accessToken = token,
          caption = caption,
          privacyLevel = privacyLevel,
          disableComment = disableComment,
          disableDuet = disableDuet,
          disableStitch = disableStitch,
          publishDirectly = publishDirectly,
          onProgress = { state ->
            _uiState.update { it.copy(publishState = state) }
          },
        )
      result.onFailure { err ->
        val msg = err.message ?: "Falha no envio oficial para o TikTok."
        memoryMonitor.recordDiagnostic("Publish", msg)
        _uiState.update { it.copy(publishState = ContentPublishState.Failed(msg)) }
      }
    }
  }

  fun copyShareLinkToClipboard(url: String) {
    val safeUrl = TikTokShareUtils.sanitizeShareUrl(url)
    val cm = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText("Link oficial TikTok", safeUrl))
    showStatusBanner("Link copiado: $safeUrl")
  }

  @OptIn(ExperimentalCoilApi::class)
  fun clearAllCaches() {
    viewModelScope.launch {
      val app = getApplication<Application>()
      VideoCacheManager.clearCache(app)
      runCatching {
        app.imageLoader.memoryCache?.clear()
        app.imageLoader.diskCache?.clear()
      }
      cacheRepository.clearAllCache()
      refreshMediaCacheLabel()
      memoryMonitor.recordDiagnostic("Cache", "Caches Media3, Coil e Room limpos manualmente.")
      memoryMonitor.refreshMemorySnapshot()
      showStatusBanner("Caches locais limpos com sucesso.")
    }
  }

  fun refreshMediaCacheLabel() {
    val bytes = VideoCacheManager.currentCacheBytes(getApplication())
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    val label = String.format(java.util.Locale.US, "%.1f MB", mb)
    _uiState.update { it.copy(mediaCacheUsageLabel = label) }
  }

  fun setDecoderMode(mode: DecoderMode) {
    viewModelScope.launch {
      settingsRepository.setDecoderMode(mode)
      showStatusBanner("Decodificador alterado para ${mode.displayName}.")
    }
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
    scrollSpeed: Float? = null,
    confirmBeforeExit: Boolean? = null,
    textScale: Float? = null,
  ) {
    viewModelScope.launch {
      settingsRepository.updateNavigationSetting(
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

  @OptIn(ExperimentalCoilApi::class)
  fun logoutAndWipeLocalData() {
    qrPollingJob?.cancel()
    viewModelScope.launch {
      val app = getApplication<Application>()
      authRepository.logoutAndWipeOAuth()
      cacheRepository.clearAllCache()
      VideoCacheManager.clearCache(app)
      runCatching {
        app.imageLoader.memoryCache?.clear()
        app.imageLoader.diskCache?.clear()
      }
      settingsRepository.clearSearchHistory()
      memoryMonitor.clearDiagnostics()
      refreshMediaCacheLabel()
      _uiState.update {
        it.copy(
          displayVideos = emptyList(),
          displayVideosError = null,
          activeVideoIndex = -1,
          activeVideo = null,
          searchResults = emptyList(),
          creatorInfo = null,
          publishState = ContentPublishState.Idle,
          activeOverlay = ActiveOverlay.NONE,
          currentDestination = TvDestination.LOGIN,
        )
      }
      showStatusBanner("Sessão OAuth revogada e dados locais excluídos.")
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
    super.onCleared()
  }
}
