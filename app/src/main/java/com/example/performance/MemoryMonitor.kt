package com.example.performance

import android.app.ActivityManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PlaybackDiagnosticEntry(
  val timestamp: String,
  val category: String,
  val message: String,
)

data class MemorySnapshot(
  val totalRamMb: Long = 0L,
  val availableRamMb: Long = 0L,
  val usedHeapMb: Long = 0L,
  val maxHeapMb: Long = 0L,
  val lowMemoryWarning: Boolean = false,
)

class MemoryMonitor(private val context: Context) {

  private val _snapshot = MutableStateFlow(sampleMemory())
  val snapshot: StateFlow<MemorySnapshot> = _snapshot.asStateFlow()

  private val _diagnostics = MutableStateFlow<List<PlaybackDiagnosticEntry>>(emptyList())
  val diagnostics: StateFlow<List<PlaybackDiagnosticEntry>> = _diagnostics.asStateFlow()

  fun refreshMemorySnapshot(): MemorySnapshot {
    val current = sampleMemory()
    _snapshot.value = current
    return current
  }

  fun recordDiagnostic(category: String, message: String) {
    val sanitized = sanitizeDiagnosticMessage(message)
    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    _diagnostics.update { existing ->
      (listOf(PlaybackDiagnosticEntry(time, category, sanitized)) + existing).take(25)
    }
  }

  fun clearDiagnostics() {
    _diagnostics.value = emptyList()
  }

  private fun sampleMemory(): MemorySnapshot {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    am?.getMemoryInfo(memInfo)
    val runtime = Runtime.getRuntime()
    val usedHeapMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L)
    val maxHeapMb = runtime.maxMemory() / (1024L * 1024L)
    return MemorySnapshot(
      totalRamMb = memInfo.totalMem / (1024L * 1024L),
      availableRamMb = memInfo.availMem / (1024L * 1024L),
      usedHeapMb = usedHeapMb,
      maxHeapMb = maxHeapMb,
      lowMemoryWarning = memInfo.lowMemory,
    )
  }

  companion object {
    fun sanitizeDiagnosticMessage(raw: String): String {
      return raw
        .replace(Regex("(access_token|refresh_token|client_secret|cookie)=[^&\\s]+", RegexOption.IGNORE_CASE), "$1=[REDACTED]")
        .replace(Regex("Bearer\\s+[A-Za-z0-9._-]+", RegexOption.IGNORE_CASE), "Bearer [REDACTED]")
        .take(220)
    }
  }
}
