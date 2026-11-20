package com.example.messaging

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.auth.WebSessionStatus
import com.example.performance.PerformanceProfile
import com.example.ui.components.TvFocusableButton
import com.example.ui.components.TvFocusableTile
import com.example.ui.components.TvInfoBanner
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokPink
import com.example.ui.theme.TvBlack
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.web.TikTokWebController
import com.example.web.TikTokWebViewSurface

@Composable
fun InboxScreen(
  webSessionStatus: WebSessionStatus,
  webController: TikTokWebController,
  performanceProfile: PerformanceProfile,
  virtualPointerActive: Boolean,
  pointerX: Float,
  pointerY: Float,
  onOpenMenu: () -> Unit,
  onOpenLogin: () -> Unit,
  onToggleVirtualPointer: () -> Unit,
) {
  var showNotifications by remember { mutableStateOf(false) }

  Row(
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    modifier =
      Modifier
        .fillMaxSize()
        .background(TvBlack)
        .padding(20.dp)
        .testTag("inbox_screen"),
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier =
        Modifier
          .width(320.dp)
          .fillMaxHeight()
          .verticalScroll(rememberScrollState()),
    ) {
      TvFocusableButton(
        text = "Menu principal",
        icon = Icons.Default.Menu,
        onClick = onOpenMenu,
        modifier = Modifier.fillMaxWidth(),
        testTag = "inbox_menu_button",
      )

      Text(
        text = "Caixa de entrada",
        color = TvTextPrimary,
        style = MaterialTheme.typography.headlineMedium,
      )

      TvFocusableTile(
        title = "Mensagens privadas",
        subtitle = "Conversas diretas e vídeos compartilhados",
        icon = Icons.Default.Mail,
        selected = !showNotifications,
        onClick = {
          showNotifications = false
          webController.loadUrl("https://www.tiktok.com/messages")
        },
        testTag = "inbox_messages_tab",
      )

      TvFocusableTile(
        title = "Notificações",
        subtitle = "Curtidas, comentários e seguidores",
        icon = Icons.Default.Notifications,
        selected = showNotifications,
        onClick = {
          showNotifications = true
          webController.loadUrl("https://www.tiktok.com/notifications")
        },
        testTag = "inbox_notifications_tab",
      )

      TvFocusableTile(
        title = "Atualizar conversas",
        icon = Icons.Default.Refresh,
        onClick = { webController.reload() },
        testTag = "inbox_refresh_button",
      )

      TvFocusableTile(
        title = if (virtualPointerActive) "Desativar ponteiro virtual" else "Ativar ponteiro virtual",
        subtitle = "Ajuda a selecionar contatos e campo de mensagem",
        icon = Icons.Default.Mouse,
        selected = virtualPointerActive,
        onClick = onToggleVirtualPointer,
        testTag = "inbox_pointer_button",
      )

      if (webSessionStatus != WebSessionStatus.AUTHENTICATED) {
        TvInfoBanner(
          message = "A caixa de entrada real exige login na sessão web do TikTok. O aplicativo respeita restrições regionais, de idade e privacidade da sua conta.",
          accentColor = TikTokPink,
        )
        TvFocusableButton(
          text = "Entrar na conta TikTok",
          isPrimary = true,
          onClick = onOpenLogin,
          modifier = Modifier.fillMaxWidth(),
          testTag = "inbox_login_button",
        )
      } else {
        Text(
          text = "Dica de controle: ative o Ponteiro Virtual se algum contato ou botão Enviar não receber foco automático do D-pad.",
          color = TvTextSecondary,
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    }

    Box(
      modifier =
        Modifier
          .weight(1f)
          .fillMaxHeight()
          .clip(RoundedCornerShape(16.dp))
          .background(TvSurface)
          .border(1.dp, TikTokCyan.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
    ) {
      TikTokWebViewSurface(
        controller = webController,
        initialUrl = if (showNotifications) "https://www.tiktok.com/notifications" else "https://www.tiktok.com/messages",
        performanceProfile = performanceProfile,
        autoplay = true,
        virtualPointerActive = virtualPointerActive,
        pointerNormalizedX = pointerX,
        pointerNormalizedY = pointerY,
        onFullscreenViewChange = {},
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}
