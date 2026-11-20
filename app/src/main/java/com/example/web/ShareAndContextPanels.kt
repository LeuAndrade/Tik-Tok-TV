package com.example.web

import android.content.Intent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import com.example.auth.QrCodeGenerator
import com.example.ui.components.TvFocusableButton
import com.example.ui.components.TvFocusableTile
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokPink
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceOverlay
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary

@Composable
fun VideoActionsSideBar(
  visible: Boolean,
  metadata: ActiveVideoMetadata,
  virtualPointerActive: Boolean,
  onLike: () -> Unit,
  onOpenComments: () -> Unit,
  onRepost: () -> Unit,
  onFavorite: () -> Unit,
  onOpenShare: () -> Unit,
  onOpenCreatorProfile: () -> Unit,
  onToggleFollow: () -> Unit,
  onToggleMute: () -> Unit,
  onToggleVirtualPointer: () -> Unit,
  onDismiss: () -> Unit,
) {
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn() + slideInHorizontally { it / 2 },
    exit = fadeOut() + slideOutHorizontally { it / 2 },
  ) {
    val firstActionFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
      runCatching { firstActionFocusRequester.requestFocus() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier =
          Modifier
            .align(Alignment.CenterEnd)
            .width(340.dp)
            .fillMaxHeight()
            .background(TvSurface.copy(alpha = 0.94f))
            .border(1.dp, TikTokPink.copy(alpha = 0.35f))
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
            .testTag("video_actions_panel"),
      ) {
        Text(
          text = "Ações do Vídeo",
          color = TvTextPrimary,
          style = MaterialTheme.typography.titleLarge,
        )
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
            color = TvTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
          )
        }

        TvFocusableTile(
          title = if (metadata.isLiked) "Descurtir" else "Curtir",
          trailingBadge = metadata.likeCountText.ifBlank { null },
          icon = Icons.Default.Favorite,
          selected = metadata.isLiked,
          onClick = onLike,
          modifier = Modifier.focusRequester(firstActionFocusRequester),
          testTag = "action_like_button",
        )

        TvFocusableTile(
          title = "Comentários",
          trailingBadge = metadata.commentCountText.ifBlank { null },
          icon = Icons.AutoMirrored.Filled.Comment,
          onClick = onOpenComments,
          testTag = "action_comments_button",
        )

        TvFocusableTile(
          title = if (metadata.isReposted) "Remover republicação" else "Republicar",
          icon = Icons.Default.Repeat,
          selected = metadata.isReposted,
          onClick = onRepost,
          testTag = "action_repost_button",
        )

        TvFocusableTile(
          title = if (metadata.isFavorited) "Remover dos favoritos" else "Salvar nos favoritos",
          icon = Icons.Default.Bookmark,
          selected = metadata.isFavorited,
          onClick = onFavorite,
          testTag = "action_favorite_button",
        )

        TvFocusableTile(
          title = "Compartilhar / Enviar ao celular",
          trailingBadge = metadata.shareCountText.ifBlank { null },
          icon = Icons.Default.Share,
          onClick = onOpenShare,
          testTag = "action_share_button",
        )

        TvFocusableTile(
          title = if (metadata.isFollowing) "Seguindo criador" else "Seguir criador",
          icon = Icons.Default.Person,
          selected = metadata.isFollowing,
          onClick = onToggleFollow,
          testTag = "action_follow_button",
        )

        TvFocusableTile(
          title = "Abrir perfil do criador",
          icon = Icons.Default.Person,
          onClick = onOpenCreatorProfile,
          testTag = "action_open_profile_button",
        )

        TvFocusableTile(
          title = if (metadata.isMuted) "Ativar áudio" else "Silenciar áudio",
          icon = if (metadata.isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
          onClick = onToggleMute,
          testTag = "action_mute_button",
        )

        TvFocusableTile(
          title = if (virtualPointerActive) "Desativar ponteiro virtual" else "Ponteiro virtual (D-pad)",
          icon = Icons.Default.Mouse,
          selected = virtualPointerActive,
          onClick = onToggleVirtualPointer,
          testTag = "action_pointer_button",
        )

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
fun ShareVideoOverlay(
  visible: Boolean,
  videoUrl: String,
  onCopyLink: (String) -> Unit,
  onShareInTikTokInbox: () -> Unit,
  onDismiss: () -> Unit,
) {
  if (!visible) return
  val context = LocalContext.current
  val safeUrl = TikTokWebDomainPolicy.sanitizeShareUrl(videoUrl)
  val qrBitmap = remember(safeUrl) { QrCodeGenerator.generateBitmap(safeUrl, 360) }
  val copyLinkFocusRequester = remember { FocusRequester() }

  LaunchedEffect(visible) {
    if (visible) {
      runCatching { copyLinkFocusRequester.requestFocus() }
    }
  }

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      Modifier
        .fillMaxSize()
        .background(TvSurfaceOverlay)
        .testTag("share_video_overlay"),
  ) {
    Row(
      horizontalArrangement = Arrangement.spacedBy(24.dp),
      verticalAlignment = Alignment.CenterVertically,
      modifier =
        Modifier
          .widthIn(max = 780.dp)
          .clip(RoundedCornerShape(20.dp))
          .background(TvSurface)
          .border(1.5.dp, TikTokCyan.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
          .padding(28.dp),
    ) {
      // Left: "Enviar ao celular" QR Code
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.width(240.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          androidx.compose.material3.Icon(
            imageVector = Icons.Default.QrCode2,
            contentDescription = null,
            tint = TikTokCyan,
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Enviar ao celular",
            color = TvTextPrimary,
            style = MaterialTheme.typography.titleLarge,
          )
        }
        if (qrBitmap != null) {
          Box(
            modifier =
              Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .padding(10.dp),
          ) {
            Image(
              bitmap = qrBitmap.asImageBitmap(),
              contentDescription = "QR Code do vídeo para abrir no celular",
              modifier = Modifier.size(180.dp),
            )
          }
        }
        Text(
          text = "Aponte a câmera do celular para abrir este vídeo no TikTok.",
          color = TvTextSecondary,
          style = MaterialTheme.typography.bodyMedium,
        )
      }

      // Right: Share options
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.weight(1f),
      ) {
        Text(
          text = "Compartilhar vídeo",
          color = TvTextPrimary,
          style = MaterialTheme.typography.headlineMedium,
        )
        Text(
          text = safeUrl,
          color = TikTokCyan,
          style = MaterialTheme.typography.labelSmall,
          maxLines = 2,
        )
        TvFocusableTile(
          title = "Compartilhar em conversa do TikTok",
          subtitle = "Abrir caixa de entrada autenticada",
          icon = Icons.Default.Mail,
          onClick = onShareInTikTokInbox,
          testTag = "share_inbox_button",
        )
        TvFocusableTile(
          title = "Copiar link do vídeo",
          icon = Icons.Default.ContentCopy,
          onClick = { onCopyLink(safeUrl) },
          modifier = Modifier.focusRequester(copyLinkFocusRequester),
          testTag = "share_copy_link_button",
        )
        TvFocusableTile(
          title = "Compartilhar em outros apps da TV",
          icon = Icons.Default.Share,
          onClick = {
            val intent =
              Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, safeUrl)
              }
            runCatching {
              context.startActivity(Intent.createChooser(intent, "Compartilhar vídeo TikTok"))
            }
          },
          testTag = "share_system_intent_button",
        )
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
fun VideoContextMenuOverlay(
  visible: Boolean,
  metadata: ActiveVideoMetadata,
  blurredAmbientEnabled: Boolean,
  virtualPointerActive: Boolean,
  onPlayPause: () -> Unit,
  onToggleAmbient: () -> Unit,
  onToggleVirtualPointer: () -> Unit,
  onCopyLink: () -> Unit,
  onReportContent: () -> Unit,
  onOpenSettings: () -> Unit,
  onDismiss: () -> Unit,
) {
  if (!visible) return

  val firstContextFocusRequester = remember { FocusRequester() }
  LaunchedEffect(visible) {
    if (visible) {
      runCatching { firstContextFocusRequester.requestFocus() }
    }
  }

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      Modifier
        .fillMaxSize()
        .background(TvSurfaceOverlay)
        .testTag("context_menu_overlay"),
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier =
        Modifier
          .widthIn(max = 480.dp)
          .clip(RoundedCornerShape(18.dp))
          .background(TvSurface)
          .border(1.5.dp, TikTokPink.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
          .padding(24.dp),
    ) {
      Text(
        text = "Menu Contextual",
        color = TvTextPrimary,
        style = MaterialTheme.typography.headlineMedium,
      )
      TvFocusableTile(
        title = if (metadata.isPaused) "Retomar vídeo" else "Pausar vídeo",
        icon = Icons.Default.PlayArrow,
        onClick = {
          onPlayPause()
          onDismiss()
        },
        modifier = Modifier.focusRequester(firstContextFocusRequester),
        testTag = "context_play_pause_button",
      )
      TvFocusableTile(
        title = if (blurredAmbientEnabled) "Desativar fundo ambiente" else "Ativar fundo ambiente em vídeos verticais",
        icon = Icons.Default.Settings,
        selected = blurredAmbientEnabled,
        onClick = {
          onToggleAmbient()
          onDismiss()
        },
        testTag = "context_ambient_button",
      )
      TvFocusableTile(
        title = if (virtualPointerActive) "Desativar Ponteiro Virtual" else "Ativar Ponteiro Virtual (D-pad)",
        icon = Icons.Default.Mouse,
        selected = virtualPointerActive,
        onClick = onToggleVirtualPointer,
        testTag = "context_pointer_button",
      )
      TvFocusableTile(
        title = "Copiar link do vídeo",
        icon = Icons.Default.ContentCopy,
        onClick = {
          onCopyLink()
          onDismiss()
        },
        testTag = "context_copy_button",
      )
      TvFocusableTile(
        title = "Denunciar conteúdo no TikTok",
        icon = Icons.Default.Flag,
        onClick = {
          onReportContent()
          onDismiss()
        },
        testTag = "context_report_button",
      )
      TvFocusableTile(
        title = "Configurações avançadas",
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
