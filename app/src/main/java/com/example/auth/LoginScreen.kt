package com.example.auth

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.performance.PerformanceProfile
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
import com.example.web.TikTokWebController
import com.example.web.TikTokWebViewSurface

@Composable
fun LoginScreen(
  authState: CombinedAuthState,
  qrState: QrAuthorizationState,
  webController: TikTokWebController,
  performanceProfile: PerformanceProfile,
  onStartOAuthQr: () -> Unit,
  onCancelOAuthQr: () -> Unit,
  onLoadWebLogin: () -> Unit,
  onLoadWebQrLogin: () -> Unit,
  onExploreFeed: () -> Unit,
  onToggleVirtualPointer: () -> Unit,
  virtualPointerActive: Boolean,
  pointerX: Float,
  pointerY: Float,
) {
  var selectedTab by remember { mutableStateOf(LoginMethodTab.TIKTOK_DIRECT) }

  Row(
    horizontalArrangement = Arrangement.spacedBy(24.dp),
    modifier =
      Modifier
        .fillMaxSize()
        .background(TvBlack)
        .padding(28.dp)
        .testTag("login_screen"),
  ) {
    // Left column: TV authentication choices & clear OAuth vs Web session explanation
    Column(
      verticalArrangement = Arrangement.spacedBy(12.dp),
      modifier =
        Modifier
          .width(420.dp)
          .fillMaxHeight()
          .verticalScroll(rememberScrollState()),
    ) {
      Text(
        text = "Conectar conta TikTok",
        color = TvTextPrimary,
        style = MaterialTheme.typography.displaySmall,
      )
      Text(
        text = "Escolha o método oficial de autenticação para TV ou Fire TV Stick.",
        color = TvTextSecondary,
        style = MaterialTheme.typography.bodyMedium,
      )

      TvFocusableTile(
        title = "Entrar com TikTok",
        subtitle = "Formulário oficial do TikTok (e-mail, telefone, usuário, provedores externos e 2FA).",
        icon = Icons.Default.Login,
        selected = selectedTab == LoginMethodTab.TIKTOK_DIRECT,
        onClick = {
          selectedTab = LoginMethodTab.TIKTOK_DIRECT
          onLoadWebLogin()
        },
        testTag = "login_method_direct",
      )

      TvFocusableTile(
        title = "Entrar usando o celular (Web QR)",
        subtitle = "Escaneie o QR oficial da sessão web para feed Para Você, curtidas, comentários e DMs.",
        icon = Icons.Default.QrCode2,
        selected = selectedTab == LoginMethodTab.QR_WEB,
        onClick = {
          selectedTab = LoginMethodTab.QR_WEB
          onLoadWebQrLogin()
        },
        testTag = "login_method_qr_web",
      )

      TvFocusableTile(
        title = "QR Code Login Kit v2 (API Oficial)",
        subtitle = "Autorização OAuth 2.0 via POST /v2/oauth/get_qrcode/ para perfil e Display API.",
        icon = Icons.Default.QrCode2,
        selected = selectedTab == LoginMethodTab.QR_OAUTH_API,
        onClick = {
          selectedTab = LoginMethodTab.QR_OAUTH_API
          onStartOAuthQr()
        },
        testTag = "login_method_qr_oauth",
      )

      TvInfoBanner(
        message =
          "Transparência de sessão: OAuth Login Kit autoriza apenas os escopos da API (perfil e seus vídeos). Para curtir, comentar, republicar e usar o chat no feed, é necessária a sessão web oficial do TikTok à direita.",
        accentColor = TikTokCyan,
      )

      if (authState.webSessionStatus == WebSessionStatus.AUTHENTICATED) {
        TvInfoBanner(
          message = "Sessão Web TikTok ativa e salva neste aparelho!",
          accentColor = TvSuccess,
        )
      }

      if (selectedTab != LoginMethodTab.QR_OAUTH_API) {
        TvFocusableButton(
          text = if (virtualPointerActive) "Desativar Ponteiro Virtual" else "Ativar Ponteiro Virtual (para captchas/2FA)",
          onClick = onToggleVirtualPointer,
          modifier = Modifier.fillMaxWidth(),
          testTag = "login_toggle_pointer_button",
        )
      }

      TvFocusableButton(
        text = "Ir para o feed da TV",
        icon = Icons.Default.Tv,
        isPrimary = true,
        onClick = onExploreFeed,
        modifier = Modifier.fillMaxWidth(),
        testTag = "login_continue_feed_button",
      )
    }

    // Right column: Official TikTok Web Login/QR OR Login Kit v2 QR Code display
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
      if (selectedTab == LoginMethodTab.QR_OAUTH_API) {
        OAuthQrCodePanel(
          qrState = qrState,
          onRetry = onStartOAuthQr,
          onCancel = onCancelOAuthQr,
        )
      } else {
        val targetUrl =
          if (selectedTab == LoginMethodTab.QR_WEB) {
            "https://www.tiktok.com/login/qrcode"
          } else {
            "https://www.tiktok.com/login"
          }
        TikTokWebViewSurface(
          controller = webController,
          initialUrl = targetUrl,
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
          text = "Solicite um código QR seguro para aprovar o acesso no TikTok do celular.",
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
          text = "Gerando client_ticket e solicitando QR Code ao servidor...",
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
          text = "Tokens OAuth salvos com criptografia Android Keystore.",
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
            text = "Para usar Login Kit v2, hospede a pasta auth-backend/ e configure TIKTOK_AUTH_BACKEND_URL no painel Secrets. Enquanto isso, as abas 'Entrar com TikTok' e 'QR Code da TV (Web)' funcionam diretamente com sua conta real!",
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
