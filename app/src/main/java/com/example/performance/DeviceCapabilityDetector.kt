package com.example.performance

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import android.webkit.WebView

data class HardwareDecoderInfo(
  val mimeType: String,
  val codecName: String,
  val isHardwareAccelerated: Boolean,
)

data class DeviceCapabilities(
  val totalRamMb: Long,
  val availableRamMb: Long,
  val isLowRamDevice: Boolean,
  val isFireOs: Boolean,
  val isAndroidTv: Boolean,
  val hasTouchscreen: Boolean,
  val osVersionLabel: String,
  val deviceModelLabel: String,
  val primaryAbi: String,
  val supportedAbis: List<String>,
  val webViewVersion: String,
  val hardwareDecoders: List<HardwareDecoderInfo>,
  val recommendedProfile: PerformanceProfile,
)

object DeviceCapabilityDetector {

  fun detect(context: Context): DeviceCapabilities {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    activityManager?.getMemoryInfo(memInfo)

    val totalRamMb = (memInfo.totalMem / (1024L * 1024L)).coerceAtLeast(512L)
    val availableRamMb = (memInfo.availMem / (1024L * 1024L)).coerceAtLeast(0L)
    val isLowRam = activityManager?.isLowRamDevice ?: (totalRamMb <= 1536L)

    val pm = context.packageManager
    val isFireOs =
      Build.MANUFACTURER.equals("Amazon", ignoreCase = true) ||
        pm.hasSystemFeature("amazon.hardware.fire_tv")
    val isAndroidTv =
      pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
        pm.hasSystemFeature("android.software.leanback_only")
    val hasTouchscreen = pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)

    val abis = Build.SUPPORTED_ABIS?.toList().orEmpty()
    val primaryAbi = abis.firstOrNull() ?: "armeabi-v7a"

    val webViewPkg =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        runCatching { WebView.getCurrentWebViewPackage()?.versionName }.getOrNull()
      } else {
        null
      }
    val webViewVersion = webViewPkg ?: "Sistema WebView padrão"

    val decoders = detectHardwareDecoders()
    val osPrefix = if (isFireOs) "Fire OS / Android" else "Android"
    val osVersionLabel = "$osPrefix ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    val deviceModelLabel = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

    return DeviceCapabilities(
      totalRamMb = totalRamMb,
      availableRamMb = availableRamMb,
      isLowRamDevice = isLowRam,
      isFireOs = isFireOs,
      isAndroidTv = isAndroidTv,
      hasTouchscreen = hasTouchscreen,
      osVersionLabel = osVersionLabel,
      deviceModelLabel = deviceModelLabel,
      primaryAbi = primaryAbi,
      supportedAbis = abis,
      webViewVersion = webViewVersion,
      hardwareDecoders = decoders,
      recommendedProfile = PerformanceProfile.recommendedForRamMb(totalRamMb, isLowRam),
    )
  }

  private fun detectHardwareDecoders(): List<HardwareDecoderInfo> {
    val targetMimes = setOf("video/avc", "video/hevc", "video/x-vnd.on2.vp9", "video/av01")
    return runCatching {
      val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
      list.codecInfos
        .asSequence()
        .filter { !it.isEncoder }
        .flatMap { info ->
          val isHw =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
              info.isHardwareAccelerated
            } else {
              !info.name.startsWith("OMX.google.", ignoreCase = true) &&
                !info.name.startsWith("c2.android.", ignoreCase = true)
            }
          info.supportedTypes
            .asSequence()
            .filter { it in targetMimes }
            .map { mime ->
              HardwareDecoderInfo(
                mimeType = mime,
                codecName = info.name,
                isHardwareAccelerated = isHw,
              )
            }
        }
        .distinctBy { "${it.mimeType}:${it.codecName}" }
        .take(12)
        .toList()
    }.getOrDefault(emptyList())
  }
}
