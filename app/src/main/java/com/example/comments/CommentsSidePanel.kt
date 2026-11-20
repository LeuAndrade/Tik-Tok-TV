package com.example.comments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.feed.ActiveVideoItem
import com.example.ui.components.TvFocusableButton
import com.example.ui.components.TvInfoBanner
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokPink
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceElevated
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary

@Composable
fun CommentsSidePanel(
  visible: Boolean,
  video: ActiveVideoItem?,
  onOpenShareQr: () -> Unit,
  onDismiss: () -> Unit,
) {
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn() + slideInHorizontally { it / 2 },
    exit = fadeOut() + slideOutHorizontally { it / 2 },
  ) {
    val closeFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
      runCatching { closeFocusRequester.requestFocus() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
      // Right 40% panel keeps player visible on left 60%
      Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier =
          Modifier
            .align(Alignment.CenterEnd)
            .fillMaxWidth(0.40f)
            .fillMaxHeight()
            .background(TvSurface.copy(alpha = 0.96f))
            .border(1.dp, TikTokCyan.copy(alpha = 0.35f))
            .padding(20.dp)
            .testTag("comments_side_panel"),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Comentários (${video?.commentCount ?: 0})",
              color = TvTextPrimary,
              style = MaterialTheme.typography.titleLarge,
            )
            if (!video?.authorHandle.isNullOrBlank()) {
              Text(
                text = video?.authorHandle.orEmpty(),
                color = TikTokCyan,
                style = MaterialTheme.typography.bodyMedium,
              )
            }
          }
          TvFocusableButton(
            text = "Fechar",
            icon = Icons.Default.Close,
            onClick = onDismiss,
            modifier = Modifier.focusRequester(closeFocusRequester),
            testTag = "comments_close_button",
          )
        }

        Column(
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier =
            Modifier
              .weight(1f)
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(TvSurfaceElevated)
              .padding(16.dp),
        ) {
          Text(
            text = "Contador real retornado pela Display API v2: ${video?.commentCount ?: 0} comentários.",
            color = TikTokCyan,
            style = MaterialTheme.typography.titleMedium,
          )
          TvInfoBanner(
            message =
              "Restrição da API oficial: a listagem textual de comentários pertence à Research API (restrita a pesquisa acadêmica nos EUA/EEE/RU/CA/CH), e não há API pública do TikTok para publicar comentários a partir de clientes nativos de TV.",
            accentColor = TikTokPink,
          )
          Text(
            text =
              "Para ler ou responder comentários diretamente no aplicativo oficial TikTok do celular, abra o QR Code de compartilhamento abaixo.",
            color = TvTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
          )
        }

        TvFocusableButton(
          text = "Abrir QR Code deste vídeo para o celular",
          icon = Icons.Default.Share,
          isPrimary = true,
          onClick = onOpenShareQr,
          modifier = Modifier.fillMaxWidth(),
          testTag = "comments_open_qr_button",
        )
      }
    }
  }
}
