package com.example

import android.content.ComponentCallbacks2
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels

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

  override fun onPause() {
    viewModel.webController.pauseMediaForBackground()
    super.onPause()
  }

  override fun onResume() {
    super.onResume()
    viewModel.webController.resumeMedia()
    viewModel.webSessionManager.refreshSessionStatus()
  }

  override fun onTrimMemory(level: Int) {
    super.onTrimMemory(level)
    if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
      viewModel.webController.clearCache(includeDisk = false)
      viewModel.memoryMonitor.recordDiagnostic("Memória", "Limpeza automática de cache por pressão de RAM (nível $level).")
      viewModel.memoryMonitor.refreshMemorySnapshot()
    }
  }
}
