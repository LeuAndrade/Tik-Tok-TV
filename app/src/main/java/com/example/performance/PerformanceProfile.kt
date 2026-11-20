package com.example.performance

enum class PerformanceProfile(
  val displayName: String,
  val description: String,
  val maxImageCacheMb: Int,
  val maxMediaCacheMb: Int,
  val enableBlurEffects: Boolean,
  val enableAnimations: Boolean,
  val preloadNextVideo: Boolean,
  val minBufferMs: Int,
  val maxBufferMs: Int,
) {
  ECONOMIC(
    displayName = "Econômico",
    description = "Otimização máxima para Fire TV Stick / Android TV com 1 GB de RAM: menos efeitos, cache reduzido e pré-carregamento mínimo.",
    maxImageCacheMb = 16,
    maxMediaCacheMb = 24,
    enableBlurEffects = false,
    enableAnimations = false,
    preloadNextVideo = false,
    minBufferMs = 1500,
    maxBufferMs = 5000,
  ),
  BALANCED(
    displayName = "Equilibrado",
    description = "Ajuste automático para boa fluidez e consumo moderado de RAM.",
    maxImageCacheMb = 32,
    maxMediaCacheMb = 48,
    enableBlurEffects = true,
    enableAnimations = true,
    preloadNextVideo = false,
    minBufferMs = 2500,
    maxBufferMs = 10000,
  ),
  QUALITY(
    displayName = "Qualidade",
    description = "Melhor qualidade visual, efeitos ambientes e buffering amplo em aparelhos com mais RAM.",
    maxImageCacheMb = 64,
    maxMediaCacheMb = 96,
    enableBlurEffects = true,
    enableAnimations = true,
    preloadNextVideo = true,
    minBufferMs = 4000,
    maxBufferMs = 20000,
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
