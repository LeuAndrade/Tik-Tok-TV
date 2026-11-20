package com.example

import android.content.ComponentCallbacks2
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.example.player.VideoCacheManager

class MainActivity : ComponentActivity() {

  private val viewModel: TikTokTvViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      BackHandler {
        if (!viewModel.handleBackPress()) {
          finish()
        }
      }

      TikTokTvApp(
        viewModel = viewModel,
        onExitApplication = { finish() },
      )
    }
  }

  @OptIn(ExperimentalCoilApi::class)
  override fun onTrimMemory(level: Int) {
    super.onTrimMemory(level)
    if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
      runCatching { imageLoader.memoryCache?.clear() }
      if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
        VideoCacheManager.clearCache(applicationContext)
      }
      viewModel.memoryMonitor.recordDiagnostic("Memória", "Limpeza automática de cache por pressão de RAM (nível $level).")
      viewModel.memoryMonitor.refreshMemorySnapshot()
      viewModel.refreshMediaCacheLabel()
    }
  }
}
