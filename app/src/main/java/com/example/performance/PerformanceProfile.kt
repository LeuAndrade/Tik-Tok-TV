package com.example.performance

enum class PerformanceProfile(
  val displayName: String,
  val description: String,
  val maxImageCacheMb: Int,
  val maxWebCacheMb: Int,
  val enableBlurEffects: Boolean,
  val enableAnimations: Boolean,
  val preloadNextVideo: Boolean,
  val minBufferMs: Int,
  val maxBufferMs: Int,
  val reduceWebAnimations: Boolean,
) {
  ECONOMIC(
    displayName = "Econômico",
    description = "Menor consumo de RAM (ideal para Fire TV Stick 1 GB), efeitos reduzidos e pré-carregamento mínimo.",
    maxImageCacheMb = 16,
    maxWebCacheMb = 24,
    enableBlurEffects = false,
    enableAnimations = false,
    preloadNextVideo = false,
    minBufferMs = 1500,
    maxBufferMs = 5000,
    reduceWebAnimations = true,
  ),
  BALANCED(
    displayName = "Equilibrado",
    description = "Ajuste automático para fluidez e consumo moderado de memória.",
    maxImageCacheMb = 32,
    maxWebCacheMb = 48,
    enableBlurEffects = true,
    enableAnimations = true,
    preloadNextVideo = false,
    minBufferMs = 2500,
    maxBufferMs = 10000,
    reduceWebAnimations = false,
  ),
  QUALITY(
    displayName = "Qualidade",
    description = "Prioriza qualidade visual, efeitos ambientes e buffering amplo em aparelhos com mais RAM.",
    maxImageCacheMb = 64,
    maxWebCacheMb = 96,
    enableBlurEffects = true,
    enableAnimations = true,
    preloadNextVideo = true,
    minBufferMs = 4000,
    maxBufferMs = 20000,
    reduceWebAnimations = false,
  );

  companion object {
    fun recommendedForRamMb(totalRamMb: Long, isLowRamFlag: Boolean): PerformanceProfile =
      when {
        isLowRamFlag || totalRamMb <= 1536L -> ECONOMIC
        totalRamMb <= 3072L -> BALANCED
        else -> QUALITY
      }
  }
}
