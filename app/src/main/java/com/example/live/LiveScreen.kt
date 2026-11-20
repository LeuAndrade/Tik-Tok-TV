package com.example.live

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.TvFocusableButton
import com.example.ui.components.TvInfoBanner
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokPink
import com.example.ui.theme.TvBlack
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary

@Composable
fun LiveScreen(
  onOpenMenu: () -> Unit,
  onOpenHome: () -> Unit,
  onOpenLogin: () -> Unit,
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier =
      Modifier
        .fillMaxSize()
        .background(TvBlack)
        .padding(28.dp)
        .verticalScroll(rememberScrollState())
        .testTag("live_screen"),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
      TvFocusableButton(
        text = "Menu",
        icon = Icons.Default.Menu,
        onClick = onOpenMenu,
        testTag = "live_menu_button",
      )
      Text(
        text = "Lives no TikTok Native TV",
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
          imageVector = Icons.Default.LiveTv,
          contentDescription = null,
          tint = TikTokPink,
          modifier = Modifier.size(34.dp),
        )
        Text(
          text = "Transmissões ao vivo não possuem API pública de TV",
          color = TikTokPink,
          style = MaterialTheme.typography.titleLarge,
        )
      }

      Text(
        text =
          "O TikTok não disponibiliza endpoints oficiais na Display API v2 ou SDKs públicos para listar transmissões ao vivo, ler chat ao vivo em tempo real ou fornecer streams HLS/FLV diretos para Media3/ExoPlayer em clientes de terceiros sem WebView.",
        color = TvTextSecondary,
        style = MaterialTheme.typography.bodyLarge,
      )

      TvInfoBanner(
        message =
          "Para evitar streams simulados ou scraping proibido, esta tela permanece 100% honesta até que o TikTok libere uma API oficial de Lives para Android TV.",
        accentColor = TikTokCyan,
      )

      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TvFocusableButton(
          text = "Voltar ao Início",
          icon = Icons.Default.Home,
          isPrimary = true,
          onClick = onOpenHome,
          testTag = "live_home_button",
        )
        TvFocusableButton(
          text = "Gerenciar conta OAuth",
          icon = Icons.Default.QrCode2,
          onClick = onOpenLogin,
        )
      }
    }
  }
}
