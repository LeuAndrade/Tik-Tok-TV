package com.example.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.auth.NativeAuthState
import com.example.auth.TikTokDisplayVideo
import com.example.ui.components.TvFocusableButton
import com.example.ui.components.TvFocusableTile
import com.example.ui.components.TvInfoBanner
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokPink
import com.example.ui.theme.TvBlack
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceElevated
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary

@Composable
fun ProfileAndFavoritesScreen(
  initialShowFavoritesFocus: Boolean,
  authState: NativeAuthState,
  displayVideos: List<TikTokDisplayVideo>,
  isLoadingVideos: Boolean,
  videosError: String?,
  onOpenMenu: () -> Unit,
  onOpenLogin: () -> Unit,
  onRefreshDisplayApi: () -> Unit,
  onSelectDisplayVideo: (TikTokDisplayVideo) -> Unit,
) {
  var selectedTab by remember(initialShowFavoritesFocus) {
    mutableIntStateOf(if (initialShowFavoritesFocus) 1 else 0)
  }

  Column(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier =
      Modifier
        .fillMaxSize()
        .background(TvBlack)
        .padding(20.dp)
        .testTag("profile_screen"),
  ) {
    // Header with real Display API v2 profile
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      modifier =
        Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(TvSurface)
          .padding(16.dp),
    ) {
      TvFocusableButton(
        text = "Menu",
        icon = Icons.Default.Menu,
        onClick = onOpenMenu,
        testTag = "profile_menu_button",
      )

      val profile = authState.userProfile
      if (profile != null && profile.avatarUrl.isNotBlank()) {
        AsyncImage(
          model = profile.avatarUrl,
          contentDescription = "Foto de perfil oficial",
          modifier = Modifier.size(56.dp).clip(CircleShape),
        )
      } else {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.size(56.dp).clip(CircleShape).background(TvSurfaceElevated),
        ) {
          Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = TikTokCyan,
          )
        }
      }

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = profile?.displayName?.ifBlank { "Perfil TikTok" } ?: "Perfil & Biblioteca Nativa",
            color = TvTextPrimary,
            style = MaterialTheme.typography.titleLarge,
          )
          if (profile?.isVerified == true) {
            Icon(
              imageVector = Icons.Default.Verified,
              contentDescription = "Conta verificada",
              tint = TikTokCyan,
              modifier = Modifier.size(18.dp),
            )
          }
        }

        if (profile != null) {
          Text(
            text =
              "@${profile.username.ifBlank { profile.openId.take(10) }} • ${profile.followerCount} seguidores • ${profile.followingCount} seguindo • ${profile.likesCount} curtidas • ${profile.videoCount} vídeos",
            color = TikTokCyan,
            style = MaterialTheme.typography.bodyMedium,
          )
          if (profile.bioDescription.isNotBlank()) {
            Text(
              text = profile.bioDescription,
              color = TvTextSecondary,
              style = MaterialTheme.typography.bodyMedium,
              maxLines = 2,
            )
          }
        } else {
          Text(
            text = "Conecte sua conta OAuth Login Kit QR v2 para sincronizar seu perfil real.",
            color = TvTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }

      TvFocusableButton(
        text = "Publicações (${displayVideos.size})",
        isSelected = selectedTab == 0,
        onClick = { selectedTab = 0 },
        testTag = "profile_tab_api_videos",
      )

      TvFocusableButton(
        text = "Favoritos / Curtidos",
        isSelected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        testTag = "profile_tab_favorites_restricted",
      )
    }

    if (selectedTab == 0) {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier =
          Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TvSurface)
            .border(1.dp, TikTokCyan.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(20.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "Vídeos publicados (GET /v2/user/info/ + POST /v2/video/list/)",
            color = TvTextPrimary,
            style = MaterialTheme.typography.titleLarge,
          )
          TvFocusableButton(
            text = "Atualizar",
            icon = Icons.Default.Refresh,
            onClick = onRefreshDisplayApi,
            testTag = "profile_refresh_api_button",
          )
        }

        if (!authState.oauthConnected && displayVideos.isEmpty()) {
          TvInfoBanner(
            message = "Entre usando QR Code Login Kit v2 para listar seus vídeos oficiais da conta e mantê-los disponíveis no cache Room.",
            accentColor = TikTokPink,
          )
          TvFocusableButton(
            text = "Conectar via QR Code",
            icon = Icons.Default.QrCode2,
            isPrimary = true,
            onClick = onOpenLogin,
            testTag = "profile_open_login_button",
          )
        } else if (isLoadingVideos) {
          CircularProgressIndicator(color = TikTokCyan)
        } else if (!videosError.isNullOrBlank() && displayVideos.isEmpty()) {
          TvInfoBanner(
            message = "Não foi possível carregar via Display API: $videosError",
            accentColor = TikTokPink,
          )
        } else if (displayVideos.isEmpty()) {
          Text(
            text = "Nenhum vídeo público retornado para esta conta pela Display API v2.",
            color = TvTextSecondary,
            style = MaterialTheme.typography.bodyLarge,
          )
        } else {
          LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
          ) {
            items(displayVideos, key = { it.id }) { video ->
              TvFocusableTile(
                title = video.title.ifBlank { video.videoDescription.ifBlank { "Vídeo #${video.id}" } },
                subtitle = "${video.viewCount} visualizações • ${video.likeCount} curtidas • ${video.commentCount} comentários",
                icon = Icons.Default.PlayArrow,
                trailingBadge = "${video.duration}s",
                onClick = { onSelectDisplayVideo(video) },
              )
            }
          }
        }
      }
    } else {
      Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier =
          Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TvSurface)
            .border(1.dp, TikTokPink.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(24.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Icon(
            imageVector = Icons.Default.Bookmark,
            contentDescription = null,
            tint = TikTokPink,
            modifier = Modifier.size(30.dp),
          )
          Text(
            text = "Favoritos, Republicações e Curtidos indisponíveis na API pública",
            color = TikTokPink,
            style = MaterialTheme.typography.titleLarge,
          )
        }
        Text(
          text =
            "A Display API v2 oficial permite apenas listar os vídeos próprios publicados pelo usuário conectado (/v2/video/list/). O TikTok não disponibiliza endpoints autorizados de terceiros para consultar ou gravar favoritos, republicações ou histórico de curtidas.",
          color = TvTextSecondary,
          style = MaterialTheme.typography.bodyLarge,
        )
        TvFocusableButton(
          text = "Ver suas publicações reais da API",
          isPrimary = true,
          onClick = { selectedTab = 0 },
          testTag = "profile_switch_to_videos_button",
        )
      }
    }
  }
}
