package com.example.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.api.TikTokPlatformCapabilities
import com.example.auth.NativeAuthState
import com.example.performance.DeviceCapabilities
import com.example.performance.MemorySnapshot
import com.example.performance.PerformanceProfile
import com.example.performance.PlaybackDiagnosticEntry
import com.example.player.DecoderMode
import com.example.ui.components.TvFocusableButton
import com.example.ui.components.TvFocusableTile
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokPink
import com.example.ui.theme.TvBlack
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary

@Composable
fun SettingsScreen(
  settings: TvSettingsState,
  authState: NativeAuthState,
  capabilities: DeviceCapabilities,
  memorySnapshot: MemorySnapshot,
  mediaCacheUsageMb: String,
  diagnostics: List<PlaybackDiagnosticEntry>,
  onOpenMenu: () -> Unit,
  onUpdatePlayback: (autoplay: Boolean?, loop: Boolean?, ambient: Boolean?, fullscreenHorizontal: Boolean?, autoQuality: Boolean?) -> Unit,
  onSelectDecoderMode: (DecoderMode) -> Unit,
  onSelectProfile: (PerformanceProfile) -> Unit,
  onUpdateCacheLimit: (Int) -> Unit,
  onClearCache: () -> Unit,
  onUpdateNavigation: (scrollSpeed: Float?, confirmExit: Boolean?, textScale: Float?) -> Unit,
  onOpenLogin: () -> Unit,
  onLogoutAndWipe: () -> Unit,
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(14.dp),
    modifier =
      Modifier
        .fillMaxSize()
        .background(TvBlack)
        .padding(24.dp)
        .verticalScroll(rememberScrollState())
        .testTag("settings_screen"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      TvFocusableButton(
        text = "Menu",
        icon = Icons.Default.Menu,
        onClick = onOpenMenu,
        testTag = "settings_menu_button",
      )
      Text(
        text = "Configurações Nativas",
        color = TvTextPrimary,
        style = MaterialTheme.typography.displaySmall,
      )
    }

    // 1. Conta
    SettingsSectionCard(title = "Conta & Autenticação OAuth") {
      Text(
        text =
          if (authState.oauthConnected) {
            "Conectado via Login Kit QR v2: ${authState.userProfile?.displayName ?: "Usuário TikTok"} (@${authState.userProfile?.username ?: "conta"})"
          } else {
            "Desconectado — conecte sua conta via Login Kit QR v2 para consultar perfil, seus vídeos e publicar."
          },
        color = TikTokCyan,
        style = MaterialTheme.typography.bodyMedium,
      )
      if (authState.oauthScopes.isNotBlank()) {
        Text(
          text = "Escopos ativos: ${authState.oauthScopes}",
          color = TvTextSecondary,
          style = MaterialTheme.typography.labelSmall,
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TvFocusableButton(
          text = if (authState.oauthConnected) "Reconectar via QR Code" else "Entrar usando QR Code",
          icon = Icons.Default.Refresh,
          isPrimary = true,
          onClick = onOpenLogin,
          testTag = "setting_reconnect_button",
        )
        TvFocusableButton(
          text = "Sair e excluir dados locais",
          icon = Icons.AutoMirrored.Filled.Logout,
          onClick = onLogoutAndWipe,
          testTag = "setting_logout_wipe_button",
        )
      }
    }

    // 2. Player Media3 & Decodificadores
    SettingsSectionCard(title = "Player Nativo Media3 / ExoPlayer") {
      Text(
        text = "Modo de decodificação MediaCodec",
        color = TikTokCyan,
        style = MaterialTheme.typography.labelLarge,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        DecoderMode.entries.forEach { mode ->
          TvFocusableButton(
            text = mode.displayName,
            isSelected = settings.decoderMode == mode,
            onClick = { onSelectDecoderMode(mode) },
            modifier = Modifier.weight(1f),
            testTag = "setting_decoder_${mode.name.lowercase()}",
          )
        }
      }
      Text(
        text = settings.decoderMode.description,
        color = TvTextSecondary,
        style = MaterialTheme.typography.bodyMedium,
      )

      TvFocusableTile(
        title = "Reprodução automática",
        subtitle = "Inicia automaticamente quando houver mídia nativa compatível",
        trailingBadge = if (settings.autoplay) "Ativado" else "Desativado",
        selected = settings.autoplay,
        onClick = { onUpdatePlayback(!settings.autoplay, null, null, null, null) },
        testTag = "setting_autoplay",
      )
      TvFocusableTile(
        title = "Repetição contínua",
        subtitle = "Mantém o vídeo em loop no Media3 ExoPlayer",
        trailingBadge = if (settings.loopVideo) "Ativado" else "Desativado",
        selected = settings.loopVideo,
        onClick = { onUpdatePlayback(null, !settings.loopVideo, null, null, null) },
        testTag = "setting_loop",
      )
      TvFocusableTile(
        title = "Fundo ambiente suave (vídeos verticais)",
        subtitle = "Recomendado manter desativado em Fire TV Stick com 1 GB de RAM",
        trailingBadge = if (settings.blurredAmbientBackground) "Ativado" else "Desativado",
        selected = settings.blurredAmbientBackground,
        onClick = { onUpdatePlayback(null, null, !settings.blurredAmbientBackground, null, null) },
        testTag = "setting_ambient_blur",
      )
      TvFocusableTile(
        title = "Tela cheia automática em vídeos horizontais",
        trailingBadge = if (settings.fullscreenHorizontal) "Ativado" else "Desativado",
        selected = settings.fullscreenHorizontal,
        onClick = { onUpdatePlayback(null, null, null, !settings.fullscreenHorizontal, null) },
        testTag = "setting_fullscreen_horizontal",
      )
      TvFocusableTile(
        title = "Qualidade adaptativa automática",
        trailingBadge = if (settings.autoQuality) "Automática" else "Padrão",
        selected = settings.autoQuality,
        onClick = { onUpdatePlayback(null, null, null, null, !settings.autoQuality) },
        testTag = "setting_auto_quality",
      )
    }

    // 3. Desempenho
    SettingsSectionCard(title = "Desempenho & Fire TV Stick") {
      Text(
        text =
          "Perfil recomendado (${capabilities.totalRamMb} MB RAM): ${capabilities.recommendedProfile.displayName}",
        color = TikTokCyan,
        style = MaterialTheme.typography.bodyMedium,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        PerformanceProfile.entries.forEach { profile ->
          TvFocusableButton(
            text = profile.displayName,
            isSelected = settings.performanceProfile == profile,
            onClick = { onSelectProfile(profile) },
            modifier = Modifier.weight(1f),
            testTag = "setting_profile_${profile.name.lowercase()}",
          )
        }
      }
      Text(
        text = settings.performanceProfile.description,
        color = TvTextSecondary,
        style = MaterialTheme.typography.bodyMedium,
      )

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(24, 48, 96).forEach { limit ->
          TvFocusableButton(
            text = "Cache ${limit} MB",
            isSelected = settings.cacheLimitMb == limit,
            onClick = { onUpdateCacheLimit(limit) },
            testTag = "setting_cache_${limit}",
          )
        }
        TvFocusableButton(
          text = "Limpar caches ($mediaCacheUsageMb)",
          icon = Icons.Default.DeleteSweep,
          onClick = onClearCache,
          testTag = "setting_clear_cache",
        )
      }

      Text(
        text = "Codecs de hardware detectados: ${capabilities.supportedCodecSummary}",
        color = TvTextSecondary,
        style = MaterialTheme.typography.bodyMedium,
      )
    }

    // 4. Controle & Acessibilidade
    SettingsSectionCard(title = "Controle Remoto & Acessibilidade") {
      Text(
        text = "Navegação 100% espacial por D-pad (setas, OK, segurar OK, Menu e Voltar — sem ponteiro/mouse).",
        color = TvTextSecondary,
        style = MaterialTheme.typography.bodyMedium,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TvFocusableButton(
          text = "Texto Compacto (0.9x)",
          isSelected = settings.textScale < 0.95f,
          onClick = { onUpdateNavigation(null, null, 0.9f) },
        )
        TvFocusableButton(
          text = "Texto Padrão (1.0x)",
          isSelected = settings.textScale in 0.95f..1.1f,
          onClick = { onUpdateNavigation(null, null, 1.0f) },
        )
        TvFocusableButton(
          text = "Texto Grande (1.2x)",
          isSelected = settings.textScale > 1.1f,
          onClick = { onUpdateNavigation(null, null, 1.2f) },
        )
      }
      TvFocusableTile(
        title = "Confirmar antes de sair do aplicativo",
        trailingBadge = if (settings.confirmBeforeExit) "Ativado" else "Desativado",
        selected = settings.confirmBeforeExit,
        onClick = { onUpdateNavigation(null, !settings.confirmBeforeExit, null) },
        testTag = "setting_confirm_exit",
      )
    }

    // 5. Sistema & APIs
    SettingsSectionCard(title = "Sistema, Hardware & Matriz de APIs") {
      Text(
        text = "Aplicativo: TikTok Native TV v${BuildConfig.VERSION_NAME} (100% Kotlin + Compose + Media3)",
        color = TvTextPrimary,
        style = MaterialTheme.typography.bodyMedium,
      )
      Text(
        text =
          "Sistema: ${capabilities.osVersionLabel} • Aparelho: ${capabilities.deviceModelLabel} • Resolução: ${capabilities.screenResolutionLabel} (${capabilities.screenDensityDpi} dpi)",
        color = TvTextSecondary,
        style = MaterialTheme.typography.bodyMedium,
      )
      Text(
        text = "Arquiteturas: ${capabilities.primaryAbi} (${capabilities.supportedAbis.joinToString()})",
        color = TvTextSecondary,
        style = MaterialTheme.typography.bodyMedium,
      )
      Text(
        text = "RAM: ${memorySnapshot.availableRamMb} MB livre / ${memorySnapshot.totalRamMb} MB total (Heap ${memorySnapshot.usedHeapMb}/${memorySnapshot.maxHeapMb} MB)",
        color = TvTextSecondary,
        style = MaterialTheme.typography.bodyMedium,
      )
      Text(
        text = "Recursos oficiais ativos: ${TikTokPlatformCapabilities.matrix.count { it.supportLevel.name == "NATIVE_SUPPORTED" }} endpoints nativos / ${TikTokPlatformCapabilities.matrix.count { it.supportLevel.name != "NATIVE_SUPPORTED" }} recursos restritos ou bloqueados pelo TikTok.",
        color = TikTokCyan,
        style = MaterialTheme.typography.bodyMedium,
      )

      if (diagnostics.isNotEmpty()) {
        Text(
          text = "Último evento (${diagnostics.size}): ${diagnostics.first().timestamp} [${diagnostics.first().category}] ${diagnostics.first().message}",
          color = TikTokPink,
          style = MaterialTheme.typography.bodyMedium,
        )
      } else {
        Text(
          text = "Diagnóstico de reprodução e rede: nenhuma falha registrada.",
          color = TvTextSecondary,
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
private fun SettingsSectionCard(
  title: String,
  content: @Composable () -> Unit,
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(TvSurface)
        .padding(18.dp),
  ) {
    Text(
      text = title,
      color = TvTextPrimary,
      style = MaterialTheme.typography.titleLarge,
    )
    content()
  }
}
