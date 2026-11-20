package com.example.profile

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import com.example.auth.CombinedAuthState
import com.example.auth.TikTokDisplayVideo
import com.example.performance.PerformanceProfile
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
import com.example.web.TikTokWebController
import com.example.web.TikTokWebViewSurface

@Composable
fun ProfileAndFavoritesScreen(
  initialShowFavoritesFocus: Boolean,
  authState: CombinedAuthState,
  displayVideos: List<TikTokDisplayVideo>,
  isLoadingVideos: Boolean,
  videosError: String?,
  webController: TikTokWebController,
  performanceProfile: PerformanceProfile,
  virtualPointerActive: Boolean,
  pointerX: Float,
  pointerY: Float,
  onOpenMenu: () -> Unit,
  onOpenLogin: () -> Unit,
  onRefreshDisplayApi: () -> Unit,
  onSelectDisplayVideo: (TikTokDisplayVideo) -> Unit,
  onToggleVirtualPointer: () -> Unit,
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
    // Header with real Display API v2 summary + Navigation tabs
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
          contentDescription = "Foto de perfil",
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
        Text(
          text = profile?.displayName?.ifBlank { "Perfil TikTok" } ?: "Perfil & Favoritos TikTok",
          color = TvTextPrimary,
          style = MaterialTheme.typography.titleLarge,
        )
        if (profile != null) {
          Text(
            text =
              "@${profile.username.ifBlank { profile.openId.take(10) }} • ${profile.followerCount} seguidores • ${profile.followingCount} seguindo • ${profile.likesCount} curtidas",
            color = TikTokCyan,
            style = MaterialTheme.typography.bodyMedium,
          )
        } else {
          Text(
            text = authState.webSessionStatus.label,
            color = TvTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
          )
        }
      }

      TvFocusableButton(
        text = "Perfil Web",
        isSelected = selectedTab == 0,
        onClick = {
          selectedTab = 0
          webController.loadUrl("https://www.tiktok.com/profile")
        },
        testTag = "profile_tab_web",
      )

      TvFocusableButton(
        text = "Vídeos API (${displayVideos.size})",
        isSelected = selectedTab == 1,
        onClick = {
          selectedTab = 1
          onRefreshDisplayApi()
        },
        testTag = "profile_tab_api_videos",
      )

      TvFocusableButton(
        text = if (virtualPointerActive) "Ponteiro ON" else "Ponteiro",
        isSelected = virtualPointerActive,
        onClick = onToggleVirtualPointer,
        testTag = "profile_pointer_button",
      )
    }

    if (selectedTab == 1) {
      // Official Display API v2 User Video List
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier =
          Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TvSurface)
            .padding(20.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = "Vídeos publicados (TikTok Display API v2)",
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

        if (!authState.oauthConnected) {
          TvInfoBanner(
            message = "Conecte via 'QR Code Login Kit v2' para listar seus vídeos oficiais pela Display API, ou use a aba 'Perfil Web' para ver publicações, republicações, favoritos e curtidas.",
            accentColor = TikTokPink,
          )
          TvFocusableButton(
            text = "Conectar Login Kit / QR Code",
            isPrimary = true,
            onClick = onOpenLogin,
            testTag = "profile_open_login_button",
          )
        } else if (isLoadingVideos) {
          CircularProgressIndicator(color = TikTokCyan)
        } else if (!videosError.isNullOrBlank()) {
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
      // Official TikTok Web Profile (supports Publicações, Republicações, Favoritos, Curtidas, Seguir/Deixar de seguir)
      Box(
        modifier =
          Modifier
            .weight(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TvSurface)
            .border(1.dp, TikTokCyan.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
      ) {
        TikTokWebViewSurface(
          controller = webController,
          initialUrl = "https://www.tiktok.com/profile",
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
