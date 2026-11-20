package com.example.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@OptIn(UnstableApi::class)
object VideoCacheManager {

  @Volatile private var cache: SimpleCache? = null
  @Volatile private var configuredMaxMb: Int = 48

  fun getCache(context: Context, maxCacheMb: Int): SimpleCache {
    val existing = cache
    if (existing != null && configuredMaxMb == maxCacheMb) {
      return existing
    }
    synchronized(this) {
      if (cache != null && configuredMaxMb == maxCacheMb) {
        return cache!!
      }
      cache?.release()
      cache = null
      configuredMaxMb = maxCacheMb.coerceIn(16, 256)
      val cacheDir = File(context.cacheDir, "media3_video_cache").apply { mkdirs() }
      val evictor = LeastRecentlyUsedCacheEvictor(configuredMaxMb.toLong() * 1024L * 1024L)
      val dbProvider = StandaloneDatabaseProvider(context.applicationContext)
      return SimpleCache(cacheDir, evictor, dbProvider).also { cache = it }
    }
  }

  fun buildDataSourceFactory(context: Context, maxCacheMb: Int): CacheDataSource.Factory {
    val httpFactory =
      DefaultHttpDataSource.Factory()
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(20_000)
        .setAllowCrossProtocolRedirects(false)
    val upstreamFactory = DefaultDataSource.Factory(context.applicationContext, httpFactory)
    return CacheDataSource.Factory()
      .setCache(getCache(context, maxCacheMb))
      .setUpstreamDataSourceFactory(upstreamFactory)
      .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
  }

  fun clearCache(context: Context) {
    synchronized(this) {
      runCatching { cache?.release() }
      cache = null
      File(context.cacheDir, "media3_video_cache").deleteRecursively()
    }
  }

  fun currentCacheBytes(context: Context): Long {
    val dir = File(context.cacheDir, "media3_video_cache")
    if (!dir.exists()) return 0L
    return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
  }
}
