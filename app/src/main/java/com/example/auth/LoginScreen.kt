package com.example.auth

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.navigation.LoginMethodTab
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
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary

@Composable
fun LoginScreen(
  authState: NativeAuthState,
  qrState: QrAuthorizationState,
  onStartOAuthQr: () -> Unit,
  onCancelOAuthQr: () -> Unit,
  onExploreApp: () -> Unit,
) {
  var selectedTab by remember { mutableStateOf(LoginMethodTab.QR_CODE) }

  Row(
    horizontalArrangement = Arrangement.spacedBy(24.dp),
    modifier =
      Modifier
        .fillMaxSize()
        .background(TvBlack)
        .padding(28.dp)
        .testTag("login_screen"),
  ) {
    // Left column: Native TV authentication methods
    Column(
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier =
        Modifier
          .width(420.dp)
          .fillMaxHeight()
          .verticalScroll(rememberScrollState()),
    ) {
      Text(
        text = "Entrar no TikTok Native TV",
        color = TvTextPrimary,
        style = MaterialTheme.typography.displaySmall,
      )
      Text(
        text = "Autenticação 100% nativa sem navegador embutido, WebView ou Custom Tabs.",
        color = TvTextSecondary,
        style = MaterialTheme.typography.bodyMedium,
      )

      TvFocusableTile(
        title = "Entrar usando QR Code",
        subtitle = "Login Kit QR v2 oficial: escaneie com o aplicativo TikTok do celular e autorize com segurança.",
        icon = Icons.Default.QrCode2,
        selected = selectedTab == LoginMethodTab.QR_CODE,
        onClick = {
          selectedTab = LoginMethodTab.QR_CODE
          onStartOAuthQr()
        },
        testTag = "login_method_qr_oauth",
      )

      TvFocusableTile(
        title = "Entrar com TikTok (SDK móvel)",
        subtitle = "Verifica compatibilidade do AuthApi móvel no Android TV / Fire TV Stick.",
        icon = Icons.Default.Login,
        selected = selectedTab == LoginMethodTab.TIKTOK_DIRECT,
        onClick = { selectedTab = LoginMethodTab.TIKTOK_DIRECT },
        testTag = "login_method_direct",
      )

      TvInfoBanner(
        message =
          "Escopos OAuth solicitados: user.info.basic, user.info.profile, user.info.stats, video.list, video.upload e video.publish.",
        accentColor = TikTokCyan,
      )

      if (authState.oauthConnected) {
        TvInfoBanner(
          message = "Conta conectada: ${authState.userProfile?.displayName ?: "Usuário TikTok"}",
          accentColor = TvSuccess,
        )
      }

      TvFocusableButton(
        text = "Explorar aplicativo na TV",
        icon = Icons.Default.Tv,
        isPrimary = true,
        onClick = onExploreApp,
        modifier = Modifier.fillMaxWidth(),
        testTag = "login_continue_feed_button",
      )
    }

    // Right column: Native OAuth QR Code v2 OR Direct SDK capability explanation
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier
          .weight(1f)
          .fillMaxHeight()
          .clip(RoundedCornerShape(18.dp))
          .background(TvSurface)
          .border(1.5.dp, TikTokCyan.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
    ) {
      if (selectedTab == LoginMethodTab.QR_CODE) {
        OAuthQrCodePanel(
          qrState = qrState,
          onRetry = onStartOAuthQr,
          onCancel = onCancelOAuthQr,
        )
      } else {
        DirectSdkUnavailablePanel(
          onSwitchToQr = {
            selectedTab = LoginMethodTab.QR_CODE
            onStartOAuthQr()
          },
        )
      }
    }
  }
}

@Composable
private fun DirectSdkUnavailablePanel(
  onSwitchToQr: () -> Unit,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier.padding(36.dp).testTag("direct_sdk_limitation_panel"),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier
          .size(72.dp)
          .clip(RoundedCornerShape(20.dp))
          .background(TvSurfaceElevated)
          .border(1.dp, TikTokPink.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
    ) {
      Icon(
        imageVector = Icons.Outlined.Lock,
        contentDescription = null,
        tint = TikTokPink,
        modifier = Modifier.size(36.dp),
      )
    }

    Text(
      text = "Entrar com TikTok (SDK Móvel) indisponível na TV",
      color = TvTextPrimary,
      style = MaterialTheme.typography.headlineMedium,
    )

    Text(
      text =
        "O SDK oficial AuthApi do TikTok para Android depende do aplicativo móvel do TikTok instalado no dispositivo ou de Chrome Custom Tabs (navegador embutido), que são proibidos neste cliente nativo e incompatíveis com televisores sem tela sensível ao toque.",
      color = TvTextSecondary,
      style = MaterialTheme.typography.bodyLarge,
    )

    TvInfoBanner(
      message =
        "Solução oficial recomendada para Android TV, Google TV e Fire TV Stick: utilize 'Entrar usando QR Code' (Login Kit QR v2) para confirmar diretamente no seu celular.",
      accentColor = TikTokCyan,
    )

    TvFocusableButton(
      text = "Usar Entrar usando QR Code agora",
      icon = Icons.Default.QrCode2,
      isPrimary = true,
      onClick = onSwitchToQr,
      testTag = "switch_to_qr_login_button",
    )
  }
}

@Composable
private fun OAuthQrCodePanel(
  qrState: QrAuthorizationState,
  onRetry: () -> Unit,
  onCancel: () -> Unit,
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier.padding(32.dp).testTag("oauth_qr_panel"),
  ) {
    Text(
      text = "Autorização TikTok Login Kit (QR v2)",
      color = TvTextPrimary,
      style = MaterialTheme.typography.headlineMedium,
    )

    when (qrState) {
      QrAuthorizationState.Idle -> {
        Text(
          text = "Gere um código QR oficial para aprovar esta TV diretamente no aplicativo TikTok do celular.",
          color = TvTextSecondary,
          style = MaterialTheme.typography.bodyLarge,
        )
        TvFocusableButton(
          text = "Gerar QR Code agora",
          icon = Icons.Default.QrCode2,
          isPrimary = true,
          onClick = onRetry,
          testTag = "oauth_qr_generate_button",
        )
      }
      QrAuthorizationState.Requesting -> {
        CircularProgressIndicator(color = TikTokCyan)
        Text(
          text = "Gerando client_ticket e solicitando QR Code ao servidor OAuth...",
          color = TvTextSecondary,
        )
      }
      is QrAuthorizationState.WaitingScan -> {
        val qrBitmap = remember(qrState.scanQrCodeUrl) { QrCodeGenerator.generateBitmap(qrState.scanQrCodeUrl, 420) }
        if (qrBitmap != null) {
          Box(
            modifier =
              Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(14.dp),
          ) {
            Image(
              bitmap = qrBitmap.asImageBitmap(),
              contentDescription = "Código QR para escanear com o aplicativo TikTok",
              modifier = Modifier.size(230.dp),
            )
          }
        }
        Text(
          text = "1. Abra o TikTok no celular  2. Escaneie este QR Code  3. Confirme a autorização",
          color = TvTextPrimary,
          style = MaterialTheme.typography.bodyLarge,
        )
        Text(
          text = "Ticket: ${qrState.clientTicket} • Expira em ${qrState.expiresInSeconds}s",
          color = TikTokCyan,
          style = MaterialTheme.typography.labelSmall,
        )
        TvFocusableButton(
          text = "Cancelar",
          onClick = onCancel,
          testTag = "oauth_qr_cancel_button",
        )
      }
      is QrAuthorizationState.ScannedPendingConfirm -> {
        CircularProgressIndicator(color = TikTokPink)
        Text(
          text = "QR Code escaneado! Aguardando confirmação no celular...",
          color = TikTokCyan,
          style = MaterialTheme.typography.titleMedium,
        )
      }
      is QrAuthorizationState.Confirmed -> {
        val profile = qrState.profile
        if (profile != null && profile.avatarUrl.isNotBlank()) {
          AsyncImage(
            model = profile.avatarUrl,
            contentDescription = "Foto de perfil do TikTok",
            modifier = Modifier.size(84.dp).clip(RoundedCornerShape(42.dp)),
          )
        }
        Text(
          text = "Conectado como ${profile?.displayName ?: "Usuário TikTok"}",
          color = TvSuccess,
          style = MaterialTheme.typography.titleLarge,
        )
        Text(
          text = "Tokens salvos com criptografia Android Keystore. Redirecionando para o Início...",
          color = TvTextSecondary,
          style = MaterialTheme.typography.bodyMedium,
        )
      }
      QrAuthorizationState.Expired -> {
        Text(
          text = "O QR Code expirou por segurança.",
          color = TvError,
          style = MaterialTheme.typography.titleMedium,
        )
        TvFocusableButton(
          text = "Gerar novo QR Code",
          icon = Icons.Default.Refresh,
          isPrimary = true,
          onClick = onRetry,
          testTag = "oauth_qr_retry_button",
        )
      }
      is QrAuthorizationState.Declined -> {
        Text(
          text = qrState.reason,
          color = TvError,
          style = MaterialTheme.typography.titleMedium,
        )
        TvFocusableButton(
          text = "Tentar novamente",
          icon = Icons.Default.Refresh,
          onClick = onRetry,
          testTag = "oauth_qr_retry_declined_button",
        )
      }
      is QrAuthorizationState.Error -> {
        Text(
          text = qrState.message,
          color = TvError,
          style = MaterialTheme.typography.bodyLarge,
        )
        if (qrState.isConfigurationMissing) {
          Text(
            text =
              "Hospede o serviço auth-backend/ e defina TIKTOK_AUTH_BACKEND_URL no painel Secrets para emitir tokens oficiais do TikTok Login Kit v2.",
            color = TvTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
          )
        }
        TvFocusableButton(
          text = "Tentar novamente",
          icon = Icons.Default.Refresh,
          onClick = onRetry,
          testTag = "oauth_qr_retry_error_button",
        )
      }
    }
  }
}
