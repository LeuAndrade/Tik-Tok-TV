package com.example.performance

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaCodecList
import android.os.Build

data class HardwareDecoderInfo(
  val mimeType: String,
  val codecName: String,
  val isHardwareAccelerated: Boolean,
) {
  val friendlyCodecLabel: String
    get() =
      when (mimeType) {
        "video/avc" -> "H.264"
        "video/hevc" -> "H.265/HEVC"
        "video/x-vnd.on2.vp9" -> "VP9"
        "video/av01" -> "AV1"
        else -> mimeType
      }
}

data class DeviceCapabilities(
  val totalRamMb: Long,
  val availableRamMb: Long,
  val isLowRamDevice: Boolean,
  val isFireOs: Boolean,
  val isAndroidTv: Boolean,
  val hasTouchscreen: Boolean,
  val hasDpadNavigation: Boolean,
  val screenResolutionLabel: String,
  val screenDensityDpi: Int,
  val osVersionLabel: String,
  val deviceModelLabel: String,
  val primaryAbi: String,
  val supportedAbis: List<String>,
  val hardwareDecoders: List<HardwareDecoderInfo>,
  val recommendedProfile: PerformanceProfile,
) {
  val supportedCodecSummary: String
    get() {
      val hw = hardwareDecoders.filter { it.isHardwareAccelerated }.map { it.friendlyCodecLabel }.distinct()
      return if (hw.isNotEmpty()) hw.joinToString(", ") else "MediaCodec padrão do sistema"
    }
}

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
    val hasDpadNavigation = isAndroidTv || isFireOs || !hasTouchscreen

    val metrics = context.resources.displayMetrics
    val resolutionLabel = "${metrics.widthPixels}x${metrics.heightPixels}"
    val densityDpi = metrics.densityDpi

    val abis = Build.SUPPORTED_ABIS?.toList().orEmpty()
    val primaryAbi = abis.firstOrNull() ?: "armeabi-v7a"

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
      hasDpadNavigation = hasDpadNavigation,
      screenResolutionLabel = resolutionLabel,
      screenDensityDpi = densityDpi,
      osVersionLabel = osVersionLabel,
      deviceModelLabel = deviceModelLabel,
      primaryAbi = primaryAbi,
      supportedAbis = abis,
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
        .take(16)
        .toList()
    }.getOrDefault(emptyList())
  }
}
