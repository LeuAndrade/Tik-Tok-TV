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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.auth.WebSessionStatus
import com.example.ui.components.TvFocusableButton
import com.example.ui.components.TvInfoBanner
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokPink
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceElevated
import com.example.ui.theme.TvTextMuted
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.web.ActiveVideoMetadata

@Composable
fun CommentsSidePanel(
  visible: Boolean,
  metadata: ActiveVideoMetadata,
  webSessionStatus: WebSessionStatus,
  onSubmitComment: (String) -> Unit,
  onOpenWebLogin: () -> Unit,
  onDismiss: () -> Unit,
) {
  var commentInput by remember { mutableStateOf("") }

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
      // Right panel keeps the video playing visibly on the left 60% of the TV screen
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
          Column {
            Text(
              text = "Comentários ${if (metadata.commentCountText.isNotBlank()) "(${metadata.commentCountText})" else ""}",
              color = TvTextPrimary,
              style = MaterialTheme.typography.titleLarge,
            )
            if (metadata.authorHandle.isNotBlank()) {
              Text(
                text = metadata.authorHandle,
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

        if (webSessionStatus != WebSessionStatus.AUTHENTICATED) {
          TvInfoBanner(
            message = "Para publicar comentários reais na sua conta, conecte a sessão web do TikTok.",
            accentColor = TikTokPink,
          )
          TvFocusableButton(
            text = "Entrar no TikTok para comentar",
            isPrimary = true,
            onClick = onOpenWebLogin,
            modifier = Modifier.fillMaxWidth(),
            testTag = "comments_login_button",
          )
        }

        if (metadata.comments.isEmpty()) {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(TvSurfaceElevated)
                .padding(16.dp),
          ) {
            Text(
              text =
                "Nenhum comentário visível extraído ainda. Quando o vídeo possui comentários públicos no TikTok Web, eles aparecem aqui automaticamente.",
              color = TvTextSecondary,
              style = MaterialTheme.typography.bodyMedium,
            )
          }
        } else {
          LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
          ) {
            items(metadata.comments) { comment ->
              Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TvSurfaceElevated)
                    .padding(12.dp),
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                      Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF262634)),
                  ) {
                    Icon(
                      imageVector = Icons.Default.Person,
                      contentDescription = null,
                      tint = TikTokCyan,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = comment.author.ifBlank { "Usuário TikTok" },
                    color = TikTokCyan,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                  )
                  if (comment.likesText.isNotBlank()) {
                    Icon(
                      imageVector = Icons.Default.FavoriteBorder,
                      contentDescription = null,
                      tint = TvTextMuted,
                      modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = comment.likesText,
                      color = TvTextMuted,
                      style = MaterialTheme.typography.labelSmall,
                    )
                  }
                }
                Text(
                  text = comment.text,
                  color = TvTextPrimary,
                  style = MaterialTheme.typography.bodyMedium,
                )
              }
            }
          }
        }

        OutlinedTextField(
          value = commentInput,
          onValueChange = { commentInput = it },
          label = { Text("Escrever comentário") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
          keyboardActions =
            KeyboardActions(
              onSend = {
                if (commentInput.isNotBlank()) {
                  onSubmitComment(commentInput)
                  commentInput = ""
                }
              },
            ),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedBorderColor = TikTokCyan,
              unfocusedBorderColor = Color(0xFF38384A),
              focusedTextColor = TvTextPrimary,
              unfocusedTextColor = TvTextPrimary,
              focusedLabelColor = TikTokCyan,
              unfocusedLabelColor = TvTextSecondary,
            ),
          modifier = Modifier.fillMaxWidth().testTag("comment_input_field"),
        )

        TvFocusableButton(
          text = "Enviar comentário",
          icon = Icons.AutoMirrored.Filled.Send,
          isPrimary = true,
          enabled = commentInput.isNotBlank(),
          onClick = {
            onSubmitComment(commentInput)
            commentInput = ""
          },
          modifier = Modifier.fillMaxWidth(),
          testTag = "comment_send_button",
        )
      }
    }
  }
}
