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
import com.example.auth.CombinedAuthState
import com.example.performance.DeviceCapabilities
import com.example.performance.MemorySnapshot
import com.example.performance.PerformanceProfile
import com.example.performance.PlaybackDiagnosticEntry
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
  authState: CombinedAuthState,
  capabilities: DeviceCapabilities,
  memorySnapshot: MemorySnapshot,
  diagnostics: List<PlaybackDiagnosticEntry>,
  onOpenMenu: () -> Unit,
  onUpdatePlayback: (autoplay: Boolean?, loop: Boolean?, ambient: Boolean?, fullscreenHorizontal: Boolean?, autoQuality: Boolean?) -> Unit,
  onSelectProfile: (PerformanceProfile) -> Unit,
  onUpdateCacheLimit: (Int) -> Unit,
  onClearCache: () -> Unit,
  onUpdateNavigation: (pointerSens: Float?, scrollSpeed: Float?, confirmExit: Boolean?, textScale: Float?) -> Unit,
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
        text = "Configurações Avançadas",
        color = TvTextPrimary,
        style = MaterialTheme.typography.displaySmall,
      )
    }

    // 1. Reprodução
    SettingsSectionCard(title = "Reprodução") {
      TvFocusableTile(
        title = "Reprodução automática",
        subtitle = "Inicia o vídeo ao abrir ou avançar no feed",
        trailingBadge = if (settings.autoplay) "Ativado" else "Desativado",
        selected = settings.autoplay,
        onClick = { onUpdatePlayback(!settings.autoplay, null, null, null, null) },
        testTag = "setting_autoplay",
      )
      TvFocusableTile(
        title = "Repetição de vídeos",
        subtitle = "Repete o vídeo continuamente ao terminar",
        trailingBadge = if (settings.loopVideo) "Ativado" else "Desativado",
        selected = settings.loopVideo,
        onClick = { onUpdatePlayback(null, !settings.loopVideo, null, null, null) },
        testTag = "setting_loop",
      )
      TvFocusableTile(
        title = "Fundo desfocado opcional (vídeos verticais)",
        subtitle = "Recomendado desativar em Fire TV Stick de 1 GB RAM",
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
        title = "Qualidade automática quando suportada pelo player",
        trailingBadge = if (settings.autoQuality) "Automática" else "Padrão",
        selected = settings.autoQuality,
        onClick = { onUpdatePlayback(null, null, null, null, !settings.autoQuality) },
        testTag = "setting_auto_quality",
      )
    }

    // 2. Desempenho
    SettingsSectionCard(title = "Desempenho & Fire TV Stick") {
      Text(
        text =
          "Recomendado automaticamente para este aparelho (${capabilities.totalRamMb} MB RAM): ${capabilities.recommendedProfile.displayName}",
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
            text = "Limite cache ${limit} MB",
            isSelected = settings.cacheLimitMb == limit,
            onClick = { onUpdateCacheLimit(limit) },
            testTag = "setting_cache_${limit}",
          )
        }
        TvFocusableButton(
          text = "Limpar cache agora",
          icon = Icons.Default.DeleteSweep,
          onClick = onClearCache,
          testTag = "setting_clear_cache",
        )
      }

      Text(
        text =
          "Decodificadores de hardware detectados: " +
            capabilities.hardwareDecoders
              .filter { it.isHardwareAccelerated }
              .joinToString { "${it.mimeType.removePrefix("video/")}(${it.codecName})" }
              .ifBlank { "Fallback MediaCodec padrão" },
        color = TvTextSecondary,
        style = MaterialTheme.typography.bodyMedium,
      )

      if (diagnostics.isNotEmpty()) {
        Text(
          text = "Diagnóstico recente (${diagnostics.size}): ${diagnostics.first().timestamp} [${diagnostics.first().category}] ${diagnostics.first().message}",
          color = TikTokPink,
          style = MaterialTheme.typography.bodyMedium,
        )
      } else {
        Text(
          text = "Diagnóstico de reprodução: nenhuma falha registrada.",
          color = TvTextSecondary,
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    }

    // 3. Navegação & Acessibilidade
    SettingsSectionCard(title = "Navegação & Acessibilidade") {
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TvFocusableButton(
          text = "Ponteiro Lento (0.6x)",
          isSelected = settings.virtualPointerSensitivity < 0.8f,
          onClick = { onUpdateNavigation(0.6f, null, null, null) },
        )
        TvFocusableButton(
          text = "Ponteiro Normal (1.0x)",
          isSelected = settings.virtualPointerSensitivity in 0.8f..1.3f,
          onClick = { onUpdateNavigation(1.0f, null, null, null) },
        )
        TvFocusableButton(
          text = "Ponteiro Rápido (1.6x)",
          isSelected = settings.virtualPointerSensitivity > 1.3f,
          onClick = { onUpdateNavigation(1.6f, null, null, null) },
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TvFocusableButton(
          text = "Texto Compacto (0.9x)",
          isSelected = settings.textScale < 0.95f,
          onClick = { onUpdateNavigation(null, null, null, 0.9f) },
        )
        TvFocusableButton(
          text = "Texto Padrão (1.0x)",
          isSelected = settings.textScale in 0.95f..1.1f,
          onClick = { onUpdateNavigation(null, null, null, 1.0f) },
        )
        TvFocusableButton(
          text = "Texto Grande (1.2x)",
          isSelected = settings.textScale > 1.1f,
          onClick = { onUpdateNavigation(null, null, null, 1.2f) },
        )
      }
      TvFocusableTile(
        title = "Confirmar antes de sair do aplicativo",
        trailingBadge = if (settings.confirmBeforeExit) "Ativado" else "Desativado",
        selected = settings.confirmBeforeExit,
        onClick = { onUpdateNavigation(null, null, !settings.confirmBeforeExit, null) },
        testTag = "setting_confirm_exit",
      )
    }

    // 4. Conta
    SettingsSectionCard(title = "Conta & Sessões") {
      Text(
        text =
          "Sessão Web: ${authState.webSessionStatus.label}  |  OAuth Login Kit: ${if (authState.oauthConnected) "Conectado (${authState.userProfile?.displayName ?: "OK"})" else "Desconectado"}",
        color = TikTokCyan,
        style = MaterialTheme.typography.bodyMedium,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TvFocusableButton(
          text = "Reconectar / Entrar",
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

    // 5. Informações do Sistema
    SettingsSectionCard(title = "Informações do Dispositivo") {
      Text(
        text = "Aplicativo: TikTok TV v${BuildConfig.VERSION_NAME} (cliente independente)",
        color = TvTextPrimary,
        style = MaterialTheme.typography.bodyMedium,
      )
      Text(
        text = "Sistema: ${capabilities.osVersionLabel} • Modelo: ${capabilities.deviceModelLabel}",
        color = TvTextSecondary,
        style = MaterialTheme.typography.bodyMedium,
      )
      Text(
        text = "Arquitetura: ${capabilities.primaryAbi} (${capabilities.supportedAbis.joinToString()})",
        color = TvTextSecondary,
        style = MaterialTheme.typography.bodyMedium,
      )
      Text(
        text = "RAM: ${memorySnapshot.availableRamMb} MB livre / ${memorySnapshot.totalRamMb} MB total (Heap ${memorySnapshot.usedHeapMb}/${memorySnapshot.maxHeapMb} MB)",
        color = TvTextSecondary,
        style = MaterialTheme.typography.bodyMedium,
      )
      Text(
        text = "Mecanismo WebView: ${capabilities.webViewVersion}",
        color = TvTextSecondary,
        style = MaterialTheme.typography.bodyMedium,
      )
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
