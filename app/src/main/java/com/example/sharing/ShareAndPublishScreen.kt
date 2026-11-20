package com.example.sharing

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.api.ContentPublishState
import com.example.api.CreatorInfoData
import com.example.auth.NativeAuthState
import com.example.auth.QrCodeGenerator
import com.example.performance.MemoryMonitor
import com.example.player.NativeMedia3PlayerSurface
import com.example.settings.TvSettingsState
import com.example.ui.components.TvFocusableButton
import com.example.ui.components.TvFocusableTile
import com.example.ui.components.TvInfoBanner
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokPink
import com.example.ui.theme.TvBlack
import com.example.ui.theme.TvError
import com.example.ui.theme.TvSuccess
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceElevated
import com.example.ui.theme.TvSurfaceOverlay
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary

object TikTokShareUtils {
  fun sanitizeShareUrl(rawUrl: String): String {
    val uri = runCatching { Uri.parse(rawUrl.trim()) }.getOrNull() ?: return "https://www.tiktok.com"
    val host = uri.host?.lowercase().orEmpty()
    val scheme = uri.scheme?.lowercase().orEmpty()
    val isSafeDomain =
      host == "tiktok.com" ||
        host.endsWith(".tiktok.com") ||
        host == "vm.tiktok.com" ||
        host == "vt.tiktok.com"
    return if (scheme == "https" && isSafeDomain) {
      uri.toString()
    } else {
      "https://www.tiktok.com"
    }
  }

  fun launchAndroidShareChooser(context: Context, title: String, rawUrl: String) {
    val safeUrl = sanitizeShareUrl(rawUrl)
    val intent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title.ifBlank { "Vídeo do TikTok" })
        putExtra(Intent.EXTRA_TEXT, safeUrl)
      }
    runCatching {
      context.startActivity(Intent.createChooser(intent, "Compartilhar link oficial"))
    }
  }
}

@Composable
fun ShareVideoOverlay(
  visible: Boolean,
  videoTitle: String,
  videoUrl: String,
  onCopyLink: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
    val copyFocusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
      runCatching { copyFocusRequester.requestFocus() }
    }

    val safeUrl = remember(videoUrl) { TikTokShareUtils.sanitizeShareUrl(videoUrl) }
    val qrBitmap = remember(safeUrl) { QrCodeGenerator.generateBitmap(safeUrl, 360) }

    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier
          .fillMaxSize()
          .background(TvSurfaceOverlay)
          .testTag("share_video_overlay"),
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier =
          Modifier
            .widthIn(max = 560.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(TvSurface)
            .border(1.5.dp, TikTokCyan.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(26.dp),
      ) {
        Text(
          text = "Enviar para o celular & Compartilhar",
          color = TvTextPrimary,
          style = MaterialTheme.typography.headlineMedium,
        )
        if (videoTitle.isNotBlank()) {
          Text(
            text = videoTitle,
            color = TikTokCyan,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
          )
        }
        Text(
          text = "Escaneie este QR Code com a câmera do celular para abrir o link oficial no aplicativo TikTok:",
          color = TvTextSecondary,
          style = MaterialTheme.typography.bodyMedium,
        )

        if (qrBitmap != null) {
          Box(
            modifier =
              Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .padding(12.dp),
          ) {
            Image(
              bitmap = qrBitmap.asImageBitmap(),
              contentDescription = "QR Code para abrir este vídeo no celular",
              modifier = Modifier.size(185.dp),
            )
          }
        }

        Text(
          text = safeUrl,
          color = TikTokCyan,
          style = MaterialTheme.typography.labelSmall,
        )

        Row(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          TvFocusableButton(
            text = "Copiar link",
            icon = Icons.Default.ContentCopy,
            isPrimary = true,
            onClick = { onCopyLink(safeUrl) },
            modifier = Modifier.weight(1f).focusRequester(copyFocusRequester),
            testTag = "share_copy_link_button",
          )
          TvFocusableButton(
            text = "Compartilhar via Android",
            icon = Icons.Default.Share,
            onClick = { TikTokShareUtils.launchAndroidShareChooser(context, videoTitle, safeUrl) },
            modifier = Modifier.weight(1f),
            testTag = "share_android_chooser_button",
          )
        }

        TvFocusableButton(
          text = "Fechar",
          onClick = onDismiss,
          modifier = Modifier.fillMaxWidth(),
          testTag = "share_close_button",
        )
      }
    }
  }
}

@Composable
fun PublishVideoScreen(
  authState: NativeAuthState,
  creatorInfo: CreatorInfoData?,
  publishState: ContentPublishState,
  selectedLocalUri: Uri?,
  settings: TvSettingsState,
  memoryMonitor: MemoryMonitor,
  onSelectLocalUri: (Uri) -> Unit,
  onLoadCreatorInfo: () -> Unit,
  onPublishVideo: (
    uri: Uri,
    caption: String,
    privacyLevel: String,
    disableComment: Boolean,
    disableDuet: Boolean,
    disableStitch: Boolean,
    publishDirectly: Boolean,
  ) -> Unit,
  onOpenMenu: () -> Unit,
  onOpenLogin: () -> Unit,
) {
  var caption by remember { mutableStateOf("") }
  var publishDirectly by remember { mutableStateOf(false) }
  var selectedPrivacy by remember(creatorInfo) {
    mutableStateOf(creatorInfo?.privacyLevelOptions?.firstOrNull() ?: "SELF_ONLY")
  }
  var disableComment by remember { mutableStateOf(false) }
  var disableDuet by remember { mutableStateOf(false) }
  var disableStitch by remember { mutableStateOf(false) }

  // Zero-permission Android Photo Picker per Google Play policy
  val pickVideoLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        onSelectLocalUri(uri)
      }
    }

  Row(
    horizontalArrangement = Arrangement.spacedBy(20.dp),
    modifier =
      Modifier
        .fillMaxSize()
        .background(TvBlack)
        .padding(24.dp)
        .testTag("publish_video_screen"),
  ) {
    // Left pane: Content Posting API v2 form & controls
    Column(
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier =
        Modifier
          .weight(1f)
          .fillMaxHeight()
          .verticalScroll(rememberScrollState()),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        TvFocusableButton(
          text = "Menu",
          icon = Icons.Default.Menu,
          onClick = onOpenMenu,
          testTag = "publish_menu_button",
        )
        Text(
          text = "Publicar vídeo (Content Posting API v2)",
          color = TvTextPrimary,
          style = MaterialTheme.typography.headlineMedium,
        )
      }

      if (!authState.oauthConnected) {
        TvInfoBanner(
          message =
            "Conecte sua conta via Login Kit QR v2 com escopos video.upload ou video.publish para enviar vídeos diretamente da TV.",
          accentColor = TikTokPink,
        )
        TvFocusableButton(
          text = "Entrar usando QR Code",
          icon = Icons.Default.QrCode2,
          isPrimary = true,
          onClick = onOpenLogin,
          testTag = "publish_login_button",
        )
      } else {
        TvFocusableButton(
          text = "Consultar limites do criador (creator_info/query)",
          onClick = onLoadCreatorInfo,
          modifier = Modifier.fillMaxWidth(),
          testTag = "publish_load_creator_info_button",
        )

        if (creatorInfo != null) {
          TvInfoBanner(
            message =
              "Criador: ${creatorInfo.creatorNickname.ifBlank { creatorInfo.creatorUsername }} • Duração máxima: ${creatorInfo.maxVideoPostDurationSec}s",
            accentColor = TikTokCyan,
          )
        }
      }

      TvFocusableButton(
        text = if (selectedLocalUri == null) "Selecionar vídeo do dispositivo" else "Trocar vídeo selecionado",
        icon = Icons.Default.VideoFile,
        isPrimary = selectedLocalUri == null,
        onClick = {
          pickVideoLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
          )
        },
        modifier = Modifier.fillMaxWidth(),
        testTag = "publish_pick_video_button",
      )

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        TvFocusableButton(
          text = "Enviar como Rascunho (Inbox)",
          isSelected = !publishDirectly,
          onClick = { publishDirectly = false },
          modifier = Modifier.weight(1f),
          testTag = "publish_mode_inbox",
        )
        TvFocusableButton(
          text = "Publicação Direta",
          isSelected = publishDirectly,
          onClick = { publishDirectly = true },
          modifier = Modifier.weight(1f),
          testTag = "publish_mode_direct",
        )
      }

      if (publishDirectly) {
        OutlinedTextField(
          value = caption,
          onValueChange = { caption = it },
          label = { Text("Legenda / Título da publicação") },
          singleLine = true,
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = TikTokCyan,
              unfocusedBorderColor = Color(0xFF38384A),
              focusedTextColor = TvTextPrimary,
              unfocusedTextColor = TvTextPrimary,
              focusedLabelColor = TikTokCyan,
              unfocusedLabelColor = TvTextSecondary,
            ),
          modifier = Modifier.fillMaxWidth().testTag("publish_caption_field"),
        )

        val privacyOptions =
          creatorInfo?.privacyLevelOptions?.takeIf { it.isNotEmpty() }
            ?: listOf("PUBLIC_TO_EVERYONE", "MUTUAL_FOLLOW_FRIENDS", "SELF_ONLY")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          privacyOptions.take(3).forEach { level ->
            TvFocusableButton(
              text = level,
              isSelected = selectedPrivacy == level,
              onClick = { selectedPrivacy = level },
              modifier = Modifier.weight(1f),
            )
          }
        }

        TvFocusableTile(
          title = "Desativar comentários",
          trailingBadge = if (disableComment) "Sim" else "Não",
          selected = disableComment,
          onClick = { disableComment = !disableComment },
        )
        TvFocusableTile(
          title = "Desativar Dueto e Costurar (Duet / Stitch)",
          trailingBadge = if (disableDuet && disableStitch) "Sim" else "Não",
          selected = disableDuet && disableStitch,
          onClick = {
            val next = !(disableDuet && disableStitch)
            disableDuet = next
            disableStitch = next
          },
        )
      }

      when (publishState) {
        ContentPublishState.Idle -> Unit
        ContentPublishState.LoadingCreatorInfo -> {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CircularProgressIndicator(color = TikTokCyan, modifier = Modifier.size(22.dp))
            Text("Consultando creator_info/query...", color = TvTextSecondary)
          }
        }
        is ContentPublishState.Uploading -> {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "${publishState.stageLabel} (${publishState.progressPercent}%)",
              color = TikTokCyan,
              style = MaterialTheme.typography.labelLarge,
            )
            LinearProgressIndicator(
              progress = { publishState.progressPercent / 100f },
              color = TikTokCyan,
              modifier = Modifier.fillMaxWidth().height(8.dp),
            )
          }
        }
        is ContentPublishState.Processing -> {
          TvInfoBanner(
            message = "Processando no TikTok (publish_id: ${publishState.publishId} • ${publishState.remoteStatus})...",
            accentColor = TikTokCyan,
          )
        }
        is ContentPublishState.Published -> {
          TvInfoBanner(
            message = "Confirmado pelo TikTok: ${publishState.modeLabel} (ID ${publishState.publishId})",
            accentColor = TvSuccess,
          )
        }
        is ContentPublishState.Failed -> {
          TvInfoBanner(
            message = "Falha oficial da API: ${publishState.message}",
            accentColor = TvError,
          )
        }
      }

      TvFocusableButton(
        text = if (publishDirectly) "Publicar agora no TikTok" else "Enviar rascunho para o TikTok",
        icon = Icons.Default.CloudUpload,
        isPrimary = true,
        enabled = authState.oauthConnected && selectedLocalUri != null && publishState !is ContentPublishState.Uploading,
        onClick = {
          if (selectedLocalUri != null) {
            onPublishVideo(
              selectedLocalUri,
              caption,
              selectedPrivacy,
              disableComment,
              disableDuet,
              disableStitch,
              publishDirectly,
            )
          }
        },
        modifier = Modifier.fillMaxWidth(),
        testTag = "publish_submit_button",
      )
    }

    // Right pane: Native Media3 ExoPlayer local preview
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier
          .widthIn(min = 380.dp, max = 520.dp)
          .fillMaxHeight()
          .clip(RoundedCornerShape(18.dp))
          .background(TvSurface)
          .border(1.5.dp, TikTokCyan.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
    ) {
      if (selectedLocalUri != null) {
        NativeMedia3PlayerSurface(
          streamUrl = selectedLocalUri.toString(),
          performanceProfile = settings.performanceProfile,
          decoderMode = settings.decoderMode,
          cacheLimitMb = settings.cacheLimitMb,
          autoplay = true,
          loopVideo = true,
          isPaused = false,
          isMuted = false,
          isHorizontalFullscreen = settings.fullscreenHorizontal,
          memoryMonitor = memoryMonitor,
          onError = { memoryMonitor.recordDiagnostic("Preview", it) },
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.padding(24.dp),
        ) {
          Icon(
            imageVector = Icons.Default.VideoFile,
            contentDescription = null,
            tint = TikTokCyan,
            modifier = Modifier.size(48.dp),
          )
          Text(
            text = "Pré-visualização nativa Media3",
            color = TvTextPrimary,
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = "Selecione um arquivo de vídeo local via Android Photo Picker para visualizar no ExoPlayer antes do envio.",
            color = TvTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }
    }
  }
}
