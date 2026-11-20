package com.example.messaging

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.TvFocusableButton
import com.example.ui.components.TvFocusableTile
import com.example.ui.components.TvInfoBanner
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokPink
import com.example.ui.theme.TvBlack
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary

@Composable
fun InboxScreen(
  onOpenMenu: () -> Unit,
  onOpenHome: () -> Unit,
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
          .width(340.dp)
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
        title = "Mensagens privadas (DMs)",
        subtitle = "Bloqueado pela API oficial do TikTok",
        icon = Icons.Default.Mail,
        selected = !showNotifications,
        onClick = { showNotifications = false },
        testTag = "inbox_messages_tab",
      )

      TvFocusableTile(
        title = "Notificações",
        subtitle = "Sem endpoint público de tempo real",
        icon = Icons.Default.Notifications,
        selected = showNotifications,
        onClick = { showNotifications = true },
        testTag = "inbox_notifications_tab",
      )

      TvFocusableButton(
        text = "Voltar ao Início",
        icon = Icons.Default.Home,
        isPrimary = true,
        onClick = onOpenHome,
        modifier = Modifier.fillMaxWidth(),
        testTag = "inbox_home_button",
      )
    }

    Column(
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier =
        Modifier
          .weight(1f)
          .fillMaxHeight()
          .clip(RoundedCornerShape(16.dp))
          .background(TvSurface)
          .border(1.dp, TikTokPink.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
          .padding(28.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
          imageVector = Icons.Outlined.Lock,
          contentDescription = null,
          tint = TikTokPink,
          modifier = Modifier.size(34.dp),
        )
        Text(
          text =
            if (showNotifications) {
              "Notificações em tempo real não disponibilizadas via API oficial"
            } else {
              "Mensagens privadas (Chat/DMs) indisponíveis em clientes nativos de terceiros"
            },
          color = TikTokPink,
          style = MaterialTheme.typography.titleLarge,
        )
      }

      Text(
        text =
          "O TikTok não oferece API pública oficial de chat interativo, recebimento/envio de mensagens privadas ou notificações em tempo real para aplicativos nativos de Android TV. A Data Portability API oficial restringe-se exclusivamente a exportação assíncrona de arquivos de dados pessoais para usuários elegíveis do EEE/Reino Unido (DMA).",
        color = TvTextSecondary,
        style = MaterialTheme.typography.bodyLarge,
      )

      TvInfoBanner(
        message =
          "Por conformidade com os requisitos do projeto, nenhuma conversa falsa é gerada localmente e nenhum navegador/WebView é utilizado.",
        accentColor = TikTokCyan,
      )
    }
  }
}
