package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.TikTokCyan
import com.example.ui.theme.TikTokPink
import com.example.ui.theme.TvBorderSubtle
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceElevated
import com.example.ui.theme.TvSurfaceOverlay
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary

@Composable
fun TvFocusableButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  isPrimary: Boolean = false,
  isSelected: Boolean = false,
  enabled: Boolean = true,
  testTag: String? = null,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()
  val scale by animateFloatAsState(targetValue = if (isFocused) 1.04f else 1.0f, label = "tvBtnScale")
  val borderColor by
    animateColorAsState(
      targetValue =
        when {
          isFocused -> TikTokCyan
          isSelected -> TikTokPink
          else -> TvBorderSubtle
        },
      label = "tvBtnBorder",
    )

  val bgColor =
    when {
      !enabled -> TvSurface.copy(alpha = 0.5f)
      isPrimary -> TikTokPink
      isFocused -> TvSurfaceElevated
      isSelected -> TikTokPink.copy(alpha = 0.22f)
      else -> TvSurface
    }

  val tagModifier = if (testTag != null) modifier.testTag(testTag) else modifier

  Surface(
    color = bgColor,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(if (isFocused) 2.5.dp else 1.dp, borderColor),
    modifier =
      tagModifier
        .scale(scale)
        .defaultMinSize(minWidth = 48.dp, minHeight = 50.dp)
        .clip(RoundedCornerShape(12.dp))
        .clickable(
          enabled = enabled,
          interactionSource = interactionSource,
          indication = null,
          role = Role.Button,
          onClick = onClick,
        ),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (isPrimary || isFocused) TvTextPrimary else TikTokCyan,
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
      }
      Text(
        text = text,
        color = if (enabled) TvTextPrimary else TvTextSecondary,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (isFocused || isPrimary) FontWeight.Bold else FontWeight.SemiBold,
      )
    }
  }
}

@Composable
fun TvFocusableTile(
  title: String,
  subtitle: String? = null,
  icon: ImageVector? = null,
  trailingBadge: String? = null,
  selected: Boolean = false,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  testTag: String? = null,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused by interactionSource.collectIsFocusedAsState()
  val borderColor by
    animateColorAsState(
      targetValue =
        when {
          isFocused -> TikTokCyan
          selected -> TikTokPink
          else -> TvBorderSubtle
        },
      label = "tvTileBorder",
    )

  val tagModifier = if (testTag != null) modifier.testTag(testTag) else modifier

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      tagModifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 56.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(
          if (isFocused) TvSurfaceElevated else if (selected) TikTokPink.copy(alpha = 0.16f) else TvSurface,
        )
        .border(if (isFocused) 2.5.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          role = Role.Button,
          onClick = onClick,
        )
        .padding(horizontal = 16.dp, vertical = 12.dp),
  ) {
    if (icon != null) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (isFocused) TikTokCyan else if (selected) TikTokPink else TvTextSecondary,
        modifier = Modifier.size(22.dp),
      )
      Spacer(modifier = Modifier.width(14.dp))
    }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        color = TvTextPrimary,
        style = MaterialTheme.typography.titleMedium,
      )
      if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          color = TvTextSecondary,
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    }
    if (!trailingBadge.isNullOrBlank()) {
      Spacer(modifier = Modifier.width(12.dp))
      Box(
        modifier =
          Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) TikTokPink else Color(0xFF262634))
            .padding(horizontal = 10.dp, vertical = 4.dp),
      ) {
        Text(
          text = trailingBadge,
          color = TvTextPrimary,
          style = MaterialTheme.typography.labelSmall,
        )
      }
    }
  }
}

@Composable
fun TvInfoBanner(
  message: String,
  accentColor: Color = TikTokCyan,
  modifier: Modifier = Modifier,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(
          Brush.horizontalGradient(
            listOf(accentColor.copy(alpha = 0.18f), TvSurfaceElevated),
          ),
        )
        .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
        .padding(horizontal = 14.dp, vertical = 10.dp),
  ) {
    Icon(
      imageVector = Icons.Outlined.Info,
      contentDescription = null,
      tint = accentColor,
      modifier = Modifier.size(20.dp),
    )
    Spacer(modifier = Modifier.width(10.dp))
    Text(
      text = message,
      color = TvTextPrimary,
      style = MaterialTheme.typography.bodyMedium,
    )
  }
}

@Composable
fun TvConfirmExitOverlay(
  onConfirmExit: () -> Unit,
  onCancel: () -> Unit,
) {
  val cancelFocusRequester = remember { FocusRequester() }
  LaunchedEffect(Unit) {
    runCatching { cancelFocusRequester.requestFocus() }
  }

  Box(
    contentAlignment = Alignment.Center,
    modifier =
      Modifier
        .fillMaxSize()
        .background(TvSurfaceOverlay)
        .testTag("exit_confirm_dialog"),
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp),
      modifier =
        Modifier
          .widthIn(max = 460.dp)
          .clip(RoundedCornerShape(18.dp))
          .background(TvSurface)
          .border(1.5.dp, TikTokCyan.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
          .padding(28.dp),
    ) {
      Text(
        text = "Sair do TikTok TV?",
        color = TvTextPrimary,
        style = MaterialTheme.typography.headlineMedium,
      )
      Text(
        text = "Sua sessão conectada continuará salva para a próxima abertura.",
        color = TvTextSecondary,
        style = MaterialTheme.typography.bodyMedium,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        TvFocusableButton(
          text = "Continuar assistindo",
          onClick = onCancel,
          isPrimary = true,
          modifier = Modifier.focusRequester(cancelFocusRequester),
          testTag = "exit_cancel_button",
        )
        TvFocusableButton(
          text = "Sair do app",
          onClick = onConfirmExit,
          testTag = "exit_confirm_button",
        )
      }
    }
  }
}
