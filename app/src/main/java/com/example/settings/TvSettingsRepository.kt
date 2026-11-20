package com.example.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.performance.PerformanceProfile
import com.example.player.DecoderMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tvSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "tiktok_tv_settings")

data class TvSettingsState(
  val autoplay: Boolean = true,
  val loopVideo: Boolean = true,
  val blurredAmbientBackground: Boolean = false,
  val fullscreenHorizontal: Boolean = true,
  val autoQuality: Boolean = true,
  val decoderMode: DecoderMode = DecoderMode.AUTOMATIC,
  val performanceProfile: PerformanceProfile = PerformanceProfile.BALANCED,
  val hasInitializedProfile: Boolean = false,
  val cacheLimitMb: Int = 48,
  val scrollSpeedMultiplier: Float = 1.0f,
  val confirmBeforeExit: Boolean = true,
  val textScale: Float = 1.0f,
  val searchHistory: List<String> = emptyList(),
)

class TvSettingsRepository(private val context: Context) {

  private object Keys {
    val AUTOPLAY = booleanPreferencesKey("autoplay")
    val LOOP_VIDEO = booleanPreferencesKey("loop_video")
    val BLURRED_AMBIENT = booleanPreferencesKey("blurred_ambient")
    val FULLSCREEN_HORIZONTAL = booleanPreferencesKey("fullscreen_horizontal")
    val AUTO_QUALITY = booleanPreferencesKey("auto_quality")
    val DECODER_MODE = stringPreferencesKey("decoder_mode")
    val PERFORMANCE_PROFILE = stringPreferencesKey("performance_profile")
    val HAS_INITIALIZED_PROFILE = booleanPreferencesKey("has_initialized_profile")
    val CACHE_LIMIT_MB = intPreferencesKey("cache_limit_mb")
    val SCROLL_SPEED = floatPreferencesKey("scroll_speed")
    val CONFIRM_EXIT = booleanPreferencesKey("confirm_exit")
    val TEXT_SCALE = floatPreferencesKey("text_scale")
    val SEARCH_HISTORY = stringPreferencesKey("search_history")
  }

  val settings: Flow<TvSettingsState> =
    context.tvSettingsDataStore.data.map { prefs ->
      val profileName = prefs[Keys.PERFORMANCE_PROFILE]
      val profile =
        PerformanceProfile.entries.firstOrNull { it.name == profileName }
          ?: PerformanceProfile.BALANCED
      val decoderName = prefs[Keys.DECODER_MODE]
      val decoderMode =
        DecoderMode.entries.firstOrNull { it.name == decoderName }
          ?: DecoderMode.AUTOMATIC
      val historyRaw = prefs[Keys.SEARCH_HISTORY].orEmpty()
      val history =
        if (historyRaw.isBlank()) emptyList()
        else historyRaw.split("\u001F").filter { it.isNotBlank() }.take(12)

      TvSettingsState(
        autoplay = prefs[Keys.AUTOPLAY] ?: true,
        loopVideo = prefs[Keys.LOOP_VIDEO] ?: true,
        blurredAmbientBackground = prefs[Keys.BLURRED_AMBIENT] ?: false,
        fullscreenHorizontal = prefs[Keys.FULLSCREEN_HORIZONTAL] ?: true,
        autoQuality = prefs[Keys.AUTO_QUALITY] ?: true,
        decoderMode = decoderMode,
        performanceProfile = profile,
        hasInitializedProfile = prefs[Keys.HAS_INITIALIZED_PROFILE] ?: false,
        cacheLimitMb = prefs[Keys.CACHE_LIMIT_MB] ?: profile.maxMediaCacheMb,
        scrollSpeedMultiplier = prefs[Keys.SCROLL_SPEED] ?: 1.0f,
        confirmBeforeExit = prefs[Keys.CONFIRM_EXIT] ?: true,
        textScale = prefs[Keys.TEXT_SCALE] ?: 1.0f,
        searchHistory = history,
      )
    }

  suspend fun initializeRecommendedProfileIfNeeded(recommended: PerformanceProfile) {
    context.tvSettingsDataStore.edit { prefs ->
      if (prefs[Keys.HAS_INITIALIZED_PROFILE] != true) {
        prefs[Keys.PERFORMANCE_PROFILE] = recommended.name
        prefs[Keys.BLURRED_AMBIENT] = recommended.enableBlurEffects
        prefs[Keys.CACHE_LIMIT_MB] = recommended.maxMediaCacheMb
        prefs[Keys.HAS_INITIALIZED_PROFILE] = true
      }
    }
  }

  suspend fun setPerformanceProfile(profile: PerformanceProfile) {
    context.tvSettingsDataStore.edit { prefs ->
      prefs[Keys.PERFORMANCE_PROFILE] = profile.name
      prefs[Keys.BLURRED_AMBIENT] = profile.enableBlurEffects
      prefs[Keys.CACHE_LIMIT_MB] = profile.maxMediaCacheMb
      prefs[Keys.HAS_INITIALIZED_PROFILE] = true
    }
  }

  suspend fun setDecoderMode(mode: DecoderMode) {
    context.tvSettingsDataStore.edit { prefs ->
      prefs[Keys.DECODER_MODE] = mode.name
    }
  }

  suspend fun updatePlaybackSetting(
    autoplay: Boolean? = null,
    loopVideo: Boolean? = null,
    blurredAmbient: Boolean? = null,
    fullscreenHorizontal: Boolean? = null,
    autoQuality: Boolean? = null,
  ) {
    context.tvSettingsDataStore.edit { prefs ->
      autoplay?.let { prefs[Keys.AUTOPLAY] = it }
      loopVideo?.let { prefs[Keys.LOOP_VIDEO] = it }
      blurredAmbient?.let { prefs[Keys.BLURRED_AMBIENT] = it }
      fullscreenHorizontal?.let { prefs[Keys.FULLSCREEN_HORIZONTAL] = it }
      autoQuality?.let { prefs[Keys.AUTO_QUALITY] = it }
    }
  }

  suspend fun updateCacheLimitMb(limitMb: Int) {
    context.tvSettingsDataStore.edit { prefs ->
      prefs[Keys.CACHE_LIMIT_MB] = limitMb.coerceIn(16, 256)
    }
  }

  suspend fun updateNavigationSetting(
    scrollSpeed: Float? = null,
    confirmBeforeExit: Boolean? = null,
    textScale: Float? = null,
  ) {
    context.tvSettingsDataStore.edit { prefs ->
      scrollSpeed?.let { prefs[Keys.SCROLL_SPEED] = it.coerceIn(0.5f, 2.0f) }
      confirmBeforeExit?.let { prefs[Keys.CONFIRM_EXIT] = it }
      textScale?.let { prefs[Keys.TEXT_SCALE] = it.coerceIn(0.85f, 1.3f) }
    }
  }

  suspend fun recordSearchQuery(query: String) {
    val clean = query.trim().replace("\u001F", "")
    if (clean.isBlank()) return
    context.tvSettingsDataStore.edit { prefs ->
      val current =
        prefs[Keys.SEARCH_HISTORY]
          .orEmpty()
          .split("\u001F")
          .filter { it.isNotBlank() && !it.equals(clean, ignoreCase = true) }
      val updated = (listOf(clean) + current).take(12)
      prefs[Keys.SEARCH_HISTORY] = updated.joinToString("\u001F")
    }
  }

  suspend fun clearSearchHistory() {
    context.tvSettingsDataStore.edit { prefs ->
      prefs.remove(Keys.SEARCH_HISTORY)
    }
  }
}
