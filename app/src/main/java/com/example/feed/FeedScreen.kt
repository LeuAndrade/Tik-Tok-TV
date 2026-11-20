package com.example.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.api.ApiSupportLevel
import com.example.api.TikTokPlatformCapabilities
import com.example.auth.NativeAuthState
import com.example.auth.QrCodeGenerator
import com.example.auth.TikTokDisplayVideo
import com.example.navigation.ActiveOverlay
import com.example.navigation.FeedDpadCallbacks
import com.example.navigation.FeedDpadFocusHost
import com.example.navigation.TvDestination
import com.example.performance.MemoryMonitor
import com.example.player.MediaSourceValidator
import com.example.player.NativeMedia3PlayerSurface
import com.example.settings.TvSettingsState
import com.example.ui.components.TvFocusableButton
import com.example.ui.components.TvFocusableTile
import com.example.ui.components.TvInfoBanner
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokPink
import com.example.ui.theme.TvBlack
import com.example.ui.theme.TvSuccess
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceElevated
import com.example.ui.theme.TvSurfaceOverlay
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary

data class ActiveVideoItem(
  val id: String = "",
  val title: String = "",
  val description: String = "",
  val authorName: String = "",
  val authorHandle: String = "",
  val coverUrl: String = "",
  val shareUrl: String = "",
  val embedLink: String = "",
  val nativeStreamUrl: String? = null,
  val durationSeconds: Int = 0,
  val width: Int = 0,
  val height: Int = 0,
  val likeCount: Long = 0L,
  val commentCount: Long = 0L,
  val shareCount: Long = 0L,
  val viewCount: Long = 0L,
  val isLocalPreview: Boolean = false,
) {
  val isHorizontal: Boolean
    get() = width > 0 && height > 0 && width >= height

  val hasPlayableNativeStream: Boolean
    get() = MediaSourceValidator.isPlayableNativeStream(nativeStreamUrl)

  val bestShareUrl: String
    get() = shareUrl.ifBlank { embedLink }

  companion object {
    fun fromDisplayVideo(video: TikTokDisplayVideo, authorName: String, authorHandle: String): ActiveVideoItem {
      return ActiveVideoItem(
        id = video.id,
        title = video.title.ifBlank { video.videoDescription.ifBlank { "Vídeo #${video.id}" } },
        description = video.videoDescription,
        authorName = authorName,
        authorHandle = authorHandle,
        coverUrl = video.coverImageUrl,
        shareUrl = video.shareUrl,
        embedLink = video.embedLink,
        nativeStreamUrl = null, // Display API v2 returns embed_link HTML, never raw MP4/HLS
        durationSeconds = video.duration,
        width = video.width,
        height = video.height,
        likeCount = video.likeCount,
        commentCount = video.commentCount,
        shareCount = video.shareCount,
        viewCount = video.viewCount,
        isLocalPreview = false,
      )
    }
  }
}

@Composable
fun FeedScreen(
  destination: TvDestination,
  authState: NativeAuthState,
  displayVideos: List<TikTokDisplayVideo>,
  activeVideo: ActiveVideoItem?,
  activeVideoIndex: Int,
  isPaused: Boolean,
  isMuted: Boolean,
  isLoadingVideos: Boolean,
  videosError: String?,
  activeOverlay: ActiveOverlay,
  settings: TvSettingsState,
  memoryMonitor: MemoryMonitor,
  onOpenMenu: () -> Unit,
  onOpenVideoActions: () -> Unit,
  onOpenContextMenu: () -> Unit,
  onStepVideo: (Int) -> Unit,
  onTogglePlayPause: () -> Unit,
  onSelectDisplayVideo: (Int) -> Unit,
  onRefreshDisplayApi: () -> Unit,
  onOpenLogin: () -> Unit,
  onOpenPublish: () -> Unit,
  onOpenProfile: () -> Unit,
  onOpenShare: () -> Unit,
  onPlayerError: (String) -> Unit,
) {
  if (destination == TvDestination.FOR_YOU ||
    destination == TvDestination.FOLLOWING ||
    destination == TvDestination.EXPLORE
  ) {
    RestrictedFeedScreen(
      destination = destination,
      onOpenMenu = onOpenMenu,
      onOpenHome = onOpenProfile,
      onOpenLogin = onOpenLogin,
      onOpenPublish = onOpenPublish,
    )
    return
  }

  // Destination is HOME: if user opened a video stage OR has a playable local/stream activeVideo, show Stage
  if (activeVideo != null && activeVideo.hasPlayableNativeStream) {
    NativeVideoPlayerStage(
      video = activeVideo,
      isPaused = isPaused,
      isMuted = isMuted,
      isOverlayOpen = activeOverlay != ActiveOverlay.NONE,
      settings = settings,
      memoryMonitor = memoryMonitor,
      onOpenMenu = onOpenMenu,
      onOpenVideoActions = onOpenVideoActions,
      onOpenContextMenu = onOpenContextMenu,
      onStepVideo = onStepVideo,
      onTogglePlayPause = onTogglePlayPause,
      onExitPlayer = { onSelectDisplayVideo(-1) },
      onPlayerError = onPlayerError,
    )
    return
  }

  // Native HOME Hub: Account summary, Display API v2 video catalog, active video inspector, & API capabilities
  Column(
    verticalArrangement = Arrangement.spacedBy(14.dp),
    modifier =
      Modifier
        .fillMaxSize()
        .background(TvBlack)
        .padding(24.dp)
        .testTag("home_screen"),
  ) {
    // Top bar
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      TvFocusableButton(
        text = "Menu",
        icon = Icons.Default.Menu,
        onClick = onOpenMenu,
        testTag = "home_menu_button",
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "TikTok Native TV — Início",
          color = TvTextPrimary,
          style = MaterialTheme.typography.headlineMedium,
        )
        Text(
          text =
            if (authState.oauthConnected) {
              "Conectado via OAuth Login Kit v2: ${authState.userProfile?.displayName ?: "Conta autorizada"}"
            } else {
              "Arquitetura 100% nativa (Jetpack Compose + Media3 + APIs oficiais)"
            },
          color = if (authState.oauthConnected) TikTokCyan else TvTextSecondary,
          style = MaterialTheme.typography.bodyMedium,
        )
      }
      TvFocusableButton(
        text = "Publicar vídeo",
        icon = Icons.Default.CloudUpload,
        onClick = onOpenPublish,
        testTag = "home_publish_button",
      )
      TvFocusableButton(
        text = if (authState.oauthConnected) "Atualizar API" else "Entrar via QR",
        icon = if (authState.oauthConnected) Icons.Default.Refresh else Icons.Default.QrCode2,
        isPrimary = true,
        onClick = { if (authState.oauthConnected) onRefreshDisplayApi() else onOpenLogin() },
        testTag = "home_primary_action_button",
      )
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(18.dp),
      modifier = Modifier.weight(1f).fillMaxWidth(),
    ) {
      // Left 55%: Authorized Display API v2 videos (cached in Room + refreshed live)
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier =
          Modifier
            .weight(0.55f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(TvSurface)
            .padding(18.dp),
      ) {
        Text(
          text = "Seus vídeos publicados (Display API v2)",
          color = TvTextPrimary,
          style = MaterialTheme.typography.titleLarge,
        )

        if (!authState.oauthConnected && displayVideos.isEmpty()) {
          TvInfoBanner(
            message =
              "Conecte sua conta via Login Kit QR v2 para consultar seus vídeos reais (POST /v2/video/list/) e manter o catálogo sincronizado no cache local Room.",
            accentColor = TikTokCyan,
          )
          TvFocusableButton(
            text = "Conectar conta via QR Code",
            icon = Icons.Default.QrCode2,
            isPrimary = true,
            onClick = onOpenLogin,
            testTag = "home_connect_qr_button",
          )
        } else if (isLoadingVideos) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CircularProgressIndicator(color = TikTokCyan, modifier = Modifier.size(22.dp))
            Text("Carregando POST /v2/video/list/...", color = TvTextSecondary)
          }
        } else if (!videosError.isNullOrBlank() && displayVideos.isEmpty()) {
          TvInfoBanner(message = videosError, accentColor = TikTokPink)
        } else if (displayVideos.isEmpty()) {
          Text(
            text = "Nenhum vídeo público retornado para esta conta na Display API v2.",
            color = TvTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
          )
        } else {
          LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
          ) {
            itemsIndexed(displayVideos, key = { _, item -> item.id }) { index, video ->
              TvFocusableTile(
                title = video.title.ifBlank { video.videoDescription.ifBlank { "Vídeo #${video.id}" } },
                subtitle = "${video.viewCount} views • ${video.likeCount} curtidas • ${video.commentCount} comentários",
                icon = Icons.Default.PlayArrow,
                trailingBadge = "${video.duration}s",
                selected = index == activeVideoIndex,
                onClick = { onSelectDisplayVideo(index) },
                testTag = "home_video_item_$index",
              )
            }
          }
        }
      }

      // Right 45%: Active Video Native Card + QR / Transparency Matrix
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier =
          Modifier
            .weight(0.45f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(TvSurface)
            .border(1.dp, TikTokCyan.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(18.dp)
            .verticalScroll(rememberScrollState()),
      ) {
        if (activeVideo != null) {
          Text(
            text = activeVideo.title.ifBlank { "Detalhes do vídeo" },
            color = TvTextPrimary,
            style = MaterialTheme.typography.titleLarge,
          )
          if (activeVideo.coverUrl.isNotBlank()) {
            AsyncImage(
              model = activeVideo.coverUrl,
              contentDescription = "Capa oficial do vídeo",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)),
            )
          }
          Text(
            text =
              "${activeVideo.viewCount} visualizações • ${activeVideo.likeCount} curtidas • ${activeVideo.commentCount} comentários • ${activeVideo.shareCount} compartilhamentos",
            color = TikTokCyan,
            style = MaterialTheme.typography.labelLarge,
          )
          TvInfoBanner(
            message =
              "Restrição da Display API v2: o endpoint oficial retorna embed_link HTML e não URL direta MP4/HLS. Sem WebView permitido, escaneie o QR Code ao lado para reproduzir no celular ou publique vídeos locais na aba Publicar.",
            accentColor = TikTokPink,
          )

          val shareQr =
            remember(activeVideo.bestShareUrl) {
              if (activeVideo.bestShareUrl.isNotBlank()) {
                QrCodeGenerator.generateBitmap(activeVideo.bestShareUrl, 240)
              } else {
                null
              }
            }
          if (shareQr != null) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Box(
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White).padding(8.dp),
              ) {
                Image(
                  bitmap = shareQr.asImageBitmap(),
                  contentDescription = "QR Code para abrir este vídeo no aplicativo TikTok",
                  modifier = Modifier.size(96.dp),
                )
              }
              Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                TvFocusableButton(
                  text = "Ações / Compartilhar",
                  icon = Icons.Default.Share,
                  isPrimary = true,
                  onClick = onOpenShare,
                  modifier = Modifier.fillMaxWidth(),
                  testTag = "home_share_selected_button",
                )
              }
            }
          }
        } else {
          Text(
            text = "Capacidades Oficiais da API TikTok",
            color = TvTextPrimary,
            style = MaterialTheme.typography.titleLarge,
          )
          TikTokPlatformCapabilities.matrix.take(6).forEach { cap ->
            val badgeColor =
              when (cap.supportLevel) {
                ApiSupportLevel.NATIVE_SUPPORTED -> TvSuccess
                ApiSupportLevel.RESTRICTED_ACADEMIC_OR_REGION -> TikTokCyan
                ApiSupportLevel.PLATFORM_BLOCKED -> TikTokPink
              }
            Column(
              verticalArrangement = Arrangement.spacedBy(4.dp),
              modifier =
                Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .background(TvSurfaceElevated)
                  .padding(10.dp),
            ) {
              Text(text = cap.featureName, color = TvTextPrimary, style = MaterialTheme.typography.labelLarge)
              Text(text = cap.supportLevel.label, color = badgeColor, style = MaterialTheme.typography.labelSmall)
              Text(text = cap.technicalSummary, color = TvTextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun NativeVideoPlayerStage(
  video: ActiveVideoItem,
  isPaused: Boolean,
  isMuted: Boolean,
  isOverlayOpen: Boolean,
  settings: TvSettingsState,
  memoryMonitor: MemoryMonitor,
  onOpenMenu: () -> Unit,
  onOpenVideoActions: () -> Unit,
  onOpenContextMenu: () -> Unit,
  onStepVideo: (Int) -> Unit,
  onTogglePlayPause: () -> Unit,
  onExitPlayer: () -> Unit,
  onPlayerError: (String) -> Unit,
) {
  val feedFocusRequester = remember { FocusRequester() }
  val menuButtonFocusRequester = remember { FocusRequester() }
  val actionsButtonFocusRequester = remember { FocusRequester() }

  val callbacks =
    remember(onStepVideo, onTogglePlayPause, onOpenMenu, onOpenVideoActions, onOpenContextMenu) {
      object : FeedDpadCallbacks {
        override fun onPreviousVideo() = onStepVideo(-1)
        override fun onNextVideo() = onStepVideo(1)
        override fun onTogglePlayPause() = onTogglePlayPause()
        override fun onOpenNavigationDrawer() = onOpenMenu()
        override fun onOpenVideoActions() = onOpenVideoActions()
        override fun onOpenContextMenu() = onOpenContextMenu()
      }
    }

  FeedDpadFocusHost(
    focusRequester = feedFocusRequester,
    isOverlayOpen = isOverlayOpen,
    focusTriggerKey = video.id,
    menuButtonFocusRequester = menuButtonFocusRequester,
    actionsButtonFocusRequester = actionsButtonFocusRequester,
    callbacks = callbacks,
  ) { _ ->
    Box(modifier = Modifier.fillMaxSize().background(Color.Black).testTag("feed_stage")) {
      if (settings.blurredAmbientBackground && !video.isHorizontal) {
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

      NativeMedia3PlayerSurface(
        streamUrl = video.nativeStreamUrl.orEmpty(),
        performanceProfile = settings.performanceProfile,
        decoderMode = settings.decoderMode,
        cacheLimitMb = settings.cacheLimitMb,
        autoplay = settings.autoplay,
        loopVideo = settings.loopVideo,
        isPaused = isPaused,
        isMuted = isMuted,
        isHorizontalFullscreen = settings.fullscreenHorizontal,
        memoryMonitor = memoryMonitor,
        onError = onPlayerError,
        modifier = Modifier.fillMaxSize(),
      )

      if (!isOverlayOpen) {
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
            text = "Menu",
            icon = Icons.Default.Menu,
            onClick = onOpenMenu,
            modifier =
              Modifier
                .focusRequester(menuButtonFocusRequester)
                .focusProperties {
                  down = feedFocusRequester
                  right = actionsButtonFocusRequester
                },
            testTag = "feed_open_menu_button",
          )

          Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
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
              text = "Voltar ao Início",
              onClick = onExitPlayer,
            )
            TvFocusableButton(
              text = "Ações",
              icon = Icons.Default.MoreHoriz,
              onClick = onOpenVideoActions,
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

        Column(
          modifier =
            Modifier
              .align(Alignment.BottomStart)
              .padding(start = 24.dp, bottom = 22.dp, end = 220.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(TvSurfaceOverlay.copy(alpha = 0.78f))
              .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
          if (video.authorHandle.isNotBlank()) {
            Text(text = video.authorHandle, color = TikTokCyan, style = MaterialTheme.typography.titleMedium)
          }
          Text(text = video.title, color = TvTextPrimary, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
        }
      }
    }
  }
}

@Composable
private fun RestrictedFeedScreen(
  destination: TvDestination,
  onOpenMenu: () -> Unit,
  onOpenHome: () -> Unit,
  onOpenLogin: () -> Unit,
  onOpenPublish: () -> Unit,
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier =
      Modifier
        .fillMaxSize()
        .background(TvBlack)
        .padding(28.dp)
        .verticalScroll(rememberScrollState())
        .testTag("restricted_feed_screen"),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
      TvFocusableButton(
        text = "Menu",
        icon = Icons.Default.Menu,
        onClick = onOpenMenu,
        testTag = "restricted_feed_menu_button",
      )
      Text(
        text = "${destination.title} — Indisponível na API Oficial",
        color = TvTextPrimary,
        style = MaterialTheme.typography.displaySmall,
      )
    }

    Column(
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier =
        Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(TvSurface)
          .border(1.5.dp, TikTokPink.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
          .padding(24.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
          imageVector = Icons.Outlined.Block,
          contentDescription = null,
          tint = TikTokPink,
          modifier = Modifier.size(34.dp),
        )
        Text(
          text = "Funcionalidade bloqueada por restrição oficial da plataforma TikTok",
          color = TikTokPink,
          style = MaterialTheme.typography.titleLarge,
        )
      }

      Text(
        text =
          "O TikTok não disponibiliza API nativa pública para entregar recomendações personalizadas do feed '${destination.title}' nem URLs diretas de streaming MP4/HLS para clientes de terceiros sem navegador/WebView. Em cumprimento à regra absoluta de aplicativo 100% nativo e dados reais, este aplicativo não simula feed falso nem carrega páginas web.",
        color = TvTextSecondary,
        style = MaterialTheme.typography.bodyLarge,
      )

      TvInfoBanner(
        message =
          "Recursos nativos autorizados disponíveis agora: Perfil & Meus Vídeos (Display API v2), Consulta por IDs, Login Kit QR v2 e Publicação de Vídeos locais (Content Posting API v2).",
        accentColor = TikTokCyan,
      )

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TvFocusableButton(
          text = "Ir para Perfil & Meus Vídeos",
          icon = Icons.Default.Person,
          isPrimary = true,
          onClick = onOpenHome,
          testTag = "restricted_open_profile_button",
        )
        TvFocusableButton(
          text = "Entrar via QR Code",
          icon = Icons.Default.QrCode2,
          onClick = onOpenLogin,
        )
        TvFocusableButton(
          text = "Publicar vídeo da TV",
          icon = Icons.Default.CloudUpload,
          onClick = onOpenPublish,
        )
      }
    }
  }
}

@Composable
fun VideoActionsSideBar(
  visible: Boolean,
  video: ActiveVideoItem?,
  isPaused: Boolean,
  isMuted: Boolean,
  onPreviousVideo: () -> Unit,
  onNextVideo: () -> Unit,
  onTogglePlayPause: () -> Unit,
  onOpenComments: () -> Unit,
  onOpenShare: () -> Unit,
  onOpenCreatorProfile: () -> Unit,
  onToggleMute: () -> Unit,
  onBlockedSocialAction: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn() + slideInHorizontally { it / 2 },
    exit = fadeOut() + slideOutHorizontally { it / 2 },
  ) {
    val firstFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
      runCatching { firstFocusRequester.requestFocus() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
          Modifier
            .align(Alignment.CenterEnd)
            .width(340.dp)
            .fillMaxHeight()
            .background(TvSurface.copy(alpha = 0.95f))
            .border(1.dp, TikTokPink.copy(alpha = 0.35f))
            .padding(18.dp)
            .verticalScroll(rememberScrollState())
            .testTag("video_actions_sidebar"),
      ) {
        Text(
          text = "Ações do Vídeo (Nativo)",
          color = TvTextPrimary,
          style = MaterialTheme.typography.titleLarge,
        )
        if (!video?.authorHandle.isNullOrBlank()) {
          Text(
            text = video?.authorHandle.orEmpty(),
            color = TikTokCyan,
            style = MaterialTheme.typography.labelLarge,
          )
        }
        Spacer(modifier = Modifier.height(2.dp))

        TvFocusableTile(
          title = if (isPaused) "Reproduzir" else "Pausar",
          icon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
          onClick = onTogglePlayPause,
          modifier = Modifier.focusRequester(firstFocusRequester),
          testTag = "action_play_pause_button",
        )
        TvFocusableTile(
          title = "Vídeo anterior / próximo",
          subtitle = "Troca entre vídeos da lista autorizada",
          icon = Icons.Default.SkipNext,
          onClick = onNextVideo,
          testTag = "action_next_button",
        )
        TvFocusableTile(
          title = "Vídeo anterior",
          icon = Icons.Default.SkipPrevious,
          onClick = onPreviousVideo,
          testTag = "action_prev_button",
        )
        TvFocusableTile(
          title = "Curtir",
          subtitle = "Bloqueado na API pública (leitura: ${video?.likeCount ?: 0})",
          icon = Icons.Default.Favorite,
          trailingBadge = "Restrito",
          onClick = { onBlockedSocialAction("Curtir") },
          testTag = "action_like_button",
        )
        TvFocusableTile(
          title = "Comentários",
          subtitle = "Total na API: ${video?.commentCount ?: 0}",
          icon = Icons.AutoMirrored.Filled.Comment,
          onClick = onOpenComments,
          testTag = "action_comments_button",
        )
        TvFocusableTile(
          title = "Republicar",
          subtitle = "Sem endpoint público oficial",
          icon = Icons.Default.Repeat,
          trailingBadge = "Restrito",
          onClick = { onBlockedSocialAction("Republicar") },
          testTag = "action_repost_button",
        )
        TvFocusableTile(
          title = "Favoritar",
          subtitle = "Sem endpoint público oficial",
          icon = Icons.Default.Bookmark,
          trailingBadge = "Restrito",
          onClick = { onBlockedSocialAction("Favoritar") },
          testTag = "action_favorite_button",
        )
        TvFocusableTile(
          title = "Compartilhar / QR celular",
          subtitle = "${video?.shareCount ?: 0} compartilhamentos",
          icon = Icons.Default.Share,
          onClick = onOpenShare,
          testTag = "action_share_button",
        )
        TvFocusableTile(
          title = "Perfil do criador",
          icon = Icons.Default.Person,
          onClick = onOpenCreatorProfile,
          testTag = "action_profile_button",
        )
        TvFocusableTile(
          title = if (isMuted) "Ativar som" else "Silenciar",
          icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
          onClick = onToggleMute,
          testTag = "action_mute_button",
        )

        Spacer(modifier = Modifier.height(4.dp))
        TvFocusableButton(
          text = "Voltar ao vídeo",
          onClick = onDismiss,
          modifier = Modifier.fillMaxWidth(),
          testTag = "action_close_button",
        )
      }
    }
  }
}

@Composable
fun VideoContextMenuOverlay(
  visible: Boolean,
  video: ActiveVideoItem?,
  blurredAmbientEnabled: Boolean,
  onPlayPause: () -> Unit,
  onToggleAmbient: () -> Unit,
  onCopyLink: () -> Unit,
  onOpenSettings: () -> Unit,
  onDismiss: () -> Unit,
) {
  AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
    val firstFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
      runCatching { firstFocusRequester.requestFocus() }
    }

    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier
          .fillMaxSize()
          .background(TvSurfaceOverlay)
          .testTag("video_context_menu"),
    ) {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier =
          Modifier
            .widthIn(max = 480.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(TvSurface)
            .border(1.5.dp, TikTokCyan.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(24.dp),
      ) {
        Text(
          text = "Opções do Vídeo (Nativo)",
          color = TvTextPrimary,
          style = MaterialTheme.typography.headlineMedium,
        )
        if (!video?.title.isNullOrBlank()) {
          Text(
            text = video?.title.orEmpty(),
            color = TikTokCyan,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
          )
        }
        TvFocusableTile(
          title = "Pausar / Reproduzir",
          icon = Icons.Default.Pause,
          onClick = {
            onPlayPause()
            onDismiss()
          },
          modifier = Modifier.focusRequester(firstFocusRequester),
          testTag = "context_play_pause_button",
        )
        TvFocusableTile(
          title = if (blurredAmbientEnabled) "Desativar fundo ambiente" else "Ativar fundo ambiente",
          subtitle = "Recomendado desligado em Fire TV Stick 1 GB",
          selected = blurredAmbientEnabled,
          onClick = onToggleAmbient,
          testTag = "context_ambient_button",
        )
        TvFocusableTile(
          title = "Copiar link oficial do vídeo",
          icon = Icons.Default.ContentCopy,
          onClick = {
            onCopyLink()
            onDismiss()
          },
          testTag = "context_copy_link_button",
        )
        TvFocusableTile(
          title = "Configurações do Player & Desempenho",
          icon = Icons.Default.Settings,
          onClick = onOpenSettings,
          testTag = "context_settings_button",
        )
        TvFocusableButton(
          text = "Fechar",
          onClick = onDismiss,
          modifier = Modifier.fillMaxWidth(),
          testTag = "context_close_button",
        )
      }
    }
  }
}
