package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import androidx.tv.material3.darkColorScheme as tvDarkColorScheme

private val TikTokMaterialColorScheme =
  darkColorScheme(
    primary = TikTokPink,
    onPrimary = TvTextPrimary,
    secondary = TikTokCyan,
    onSecondary = TvBlack,
    tertiary = TikTokCyan,
    background = TvBlack,
    onBackground = TvTextPrimary,
    surface = TvSurface,
    onSurface = TvTextPrimary,
    surfaceVariant = TvSurfaceElevated,
    onSurfaceVariant = TvTextSecondary,
    error = TvError,
    onError = TvTextPrimary,
    outline = TvBorderSubtle,
  )

@OptIn(ExperimentalTvMaterial3Api::class)
private val TikTokTvMaterialColorScheme =
  tvDarkColorScheme(
    primary = TikTokPink,
    onPrimary = TvTextPrimary,
    secondary = TikTokCyan,
    onSecondary = TvBlack,
    tertiary = TikTokCyan,
    background = TvBlack,
    onBackground = TvTextPrimary,
    surface = TvSurface,
    onSurface = TvTextPrimary,
    surfaceVariant = TvSurfaceElevated,
    onSurfaceVariant = TvTextSecondary,
    error = TvError,
    onError = TvTextPrimary,
    border = TvBorderSubtle,
  )

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TikTokTvTheme(
  textScale: Float = 1.0f,
  content: @Composable () -> Unit,
) {
  val typography = buildTvTypography(textScale)
  TvMaterialTheme(
    colorScheme = TikTokTvMaterialColorScheme,
  ) {
    MaterialTheme(
      colorScheme = TikTokMaterialColorScheme,
      typography = typography,
      content = content,
    )
  }
}
