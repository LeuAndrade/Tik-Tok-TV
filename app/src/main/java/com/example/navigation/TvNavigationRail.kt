package com.example.navigation

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Login
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.auth.CombinedAuthState
import com.example.auth.WebSessionStatus
import com.example.ui.components.TvFocusableButton
import com.example.ui.components.TvFocusableTile
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokPink
import com.example.ui.theme.TvBlack
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceElevated
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary

private data class NavMenuItem(
  val destination: TvDestination,
  val icon: ImageVector,
  val subtitle: String,
  val testTag: String,
)

private val primaryMenuItems =
  listOf(
    NavMenuItem(TvDestination.FOR_YOU, Icons.Default.Home, "Feed personalizado", "nav_for_you"),
    NavMenuItem(TvDestination.FOLLOWING, Icons.Default.People, "Criadores seguidos", "nav_following"),
    NavMenuItem(TvDestination.LIVES, Icons.Default.LiveTv, "Transmissões ao vivo", "nav_lives"),
    NavMenuItem(TvDestination.SEARCH, Icons.Default.Search, "Vídeos, usuários e hashtags", "nav_search"),
    NavMenuItem(TvDestination.INBOX, Icons.Default.Mail, "Conversas e notificações", "nav_inbox"),
    NavMenuItem(TvDestination.FAVORITES, Icons.Default.Bookmark, "Salvos e coleções", "nav_favorites"),
    NavMenuItem(TvDestination.PROFILE, Icons.Default.Person, "Sua conta e vídeos", "nav_profile"),
    NavMenuItem(TvDestination.SETTINGS, Icons.Default.Settings, "Reprodução e desempenho", "nav_settings"),
  )

@Composable
fun TvNavigationDrawerOverlay(
  visible: Boolean,
  currentDestination: TvDestination,
  authState: CombinedAuthState,
  onSelectDestination: (TvDestination) -> Unit,
  onOpenLogin: () -> Unit,
  onDismiss: () -> Unit,
) {
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn() + slideInHorizontally { -it / 2 },
    exit = fadeOut() + slideOutHorizontally { -it / 2 },
  ) {
    val initialFocusRequester = remember { FocusRequester() }
    val targetFocusDestination =
      if (primaryMenuItems.any { it.destination == currentDestination }) {
        currentDestination
      } else {
        TvDestination.FOR_YOU
      }

    LaunchedEffect(Unit) {
      runCatching { initialFocusRequester.requestFocus() }
    }

    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(
            Brush.horizontalGradient(
              listOf(TvBlack.copy(alpha = 0.92f), TvBlack.copy(alpha = 0.55f), Color.Transparent),
            ),
          ),
    ) {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier =
          Modifier
            .width(340.dp)
            .fillMaxHeight()
            .background(TvSurface)
            .border(1.dp, TikTokCyan.copy(alpha = 0.25f))
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
            .testTag("tv_navigation_drawer"),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(TikTokCyan, TikTokPink))),
          ) {
            Text(
              text = "TV",
              color = TvBlack,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.ExtraBold,
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "TikTok TV",
              color = TvTextPrimary,
              style = MaterialTheme.typography.titleLarge,
            )
            Text(
              text =
                if (authState.webSessionStatus == WebSessionStatus.AUTHENTICATED) {
                  "Sessão Web conectada"
                } else if (authState.oauthConnected) {
                  authState.userProfile?.displayName ?: "Login Kit conectado"
                } else {
                  "Modo visitante"
                },
              color = if (authState.webSessionStatus == WebSessionStatus.AUTHENTICATED) TikTokCyan else TvTextSecondary,
              style = MaterialTheme.typography.labelSmall,
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        primaryMenuItems.forEach { item ->
          TvFocusableTile(
            title = item.destination.title,
            subtitle = item.subtitle,
            icon = item.icon,
            selected = currentDestination == item.destination,
            onClick = { onSelectDestination(item.destination) },
            modifier =
              if (item.destination == targetFocusDestination) {
                Modifier.focusRequester(initialFocusRequester)
              } else {
                Modifier
              },
            testTag = item.testTag,
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        TvFocusableButton(
          text = if (authState.webSessionStatus == WebSessionStatus.AUTHENTICATED) "Gerenciar conta / Entrar" else "Entrar com TikTok / QR",
          icon = Icons.Outlined.Login,
          isPrimary = authState.webSessionStatus != WebSessionStatus.AUTHENTICATED,
          onClick = onOpenLogin,
          modifier = Modifier.fillMaxWidth(),
          testTag = "nav_login_button",
        )

        TvFocusableButton(
          text = "Fechar menu (Voltar)",
          onClick = onDismiss,
          modifier = Modifier.fillMaxWidth(),
          testTag = "nav_close_button",
        )
      }
    }
  }
}
