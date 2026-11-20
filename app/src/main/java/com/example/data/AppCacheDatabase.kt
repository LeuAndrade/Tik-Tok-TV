package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.auth.TikTokDisplayVideo
import com.example.auth.TikTokUserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "cached_profile")
data class CachedProfileEntity(
  @PrimaryKey val openId: String,
  val unionId: String,
  val avatarUrl: String,
  val displayName: String,
  val username: String,
  val bioDescription: String,
  val profileDeepLink: String,
  val isVerified: Boolean,
  val followerCount: Long,
  val followingCount: Long,
  val likesCount: Long,
  val videoCount: Long,
  val cachedAtEpochMs: Long,
)

@Entity(tableName = "cached_videos")
data class CachedVideoEntity(
  @PrimaryKey val id: String,
  val createTime: Long,
  val coverImageUrl: String,
  val shareUrl: String,
  val videoDescription: String,
  val duration: Int,
  val height: Int,
  val width: Int,
  val title: String,
  val embedLink: String,
  val likeCount: Long,
  val commentCount: Long,
  val shareCount: Long,
  val viewCount: Long,
  val cachedAtEpochMs: Long,
)

@Dao
interface TikTokCacheDao {
  @Query("SELECT * FROM cached_profile LIMIT 1")
  fun observeProfile(): Flow<CachedProfileEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertProfile(profile: CachedProfileEntity)

  @Query("DELETE FROM cached_profile")
  suspend fun clearProfile()

  @Query("SELECT * FROM cached_videos ORDER BY createTime DESC")
  fun observeVideos(): Flow<List<CachedVideoEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertVideos(videos: List<CachedVideoEntity>)

  @Query("DELETE FROM cached_videos")
  suspend fun clearVideos()
}

@Database(
  entities = [CachedProfileEntity::class, CachedVideoEntity::class],
  version = 1,
  exportSchema = false,
)
abstract class AppCacheDatabase : RoomDatabase() {
  abstract fun cacheDao(): TikTokCacheDao

  companion object {
    @Volatile private var instance: AppCacheDatabase? = null

    fun getInstance(context: Context): AppCacheDatabase {
      return instance ?: synchronized(this) {
        instance
          ?: Room.databaseBuilder(
            context.applicationContext,
            AppCacheDatabase::class.java,
            "tiktok_native_tv_cache.db",
          ).build().also { instance = it }
      }
    }
  }
}

class TikTokCacheRepository(private val dao: TikTokCacheDao) {

  val cachedProfile: Flow<TikTokUserProfile?> =
    dao.observeProfile().map { entity ->
      entity?.let {
        TikTokUserProfile(
          openId = it.openId,
          unionId = it.unionId,
          avatarUrl = it.avatarUrl,
          avatarUrl100 = it.avatarUrl,
          displayName = it.displayName,
          username = it.username,
          bioDescription = it.bioDescription,
          profileDeepLink = it.profileDeepLink,
          isVerified = it.isVerified,
          followerCount = it.followerCount,
          followingCount = it.followingCount,
          likesCount = it.likesCount,
          videoCount = it.videoCount,
        )
      }
    }

  val cachedVideos: Flow<List<TikTokDisplayVideo>> =
    dao.observeVideos().map { list ->
      list.map { item ->
        TikTokDisplayVideo(
          id = item.id,
          createTime = item.createTime,
          coverImageUrl = item.coverImageUrl,
          shareUrl = item.shareUrl,
          videoDescription = item.videoDescription,
          duration = item.duration,
          height = item.height,
          width = item.width,
          title = item.title,
          embedLink = item.embedLink,
          likeCount = item.likeCount,
          commentCount = item.commentCount,
          shareCount = item.shareCount,
          viewCount = item.viewCount,
        )
      }
    }

  suspend fun cacheProfile(profile: TikTokUserProfile) {
    if (profile.openId.isBlank()) return
    dao.upsertProfile(
      CachedProfileEntity(
        openId = profile.openId,
        unionId = profile.unionId,
        avatarUrl = profile.avatarUrl.ifBlank { profile.avatarUrl100 },
        displayName = profile.displayName,
        username = profile.username,
        bioDescription = profile.bioDescription,
        profileDeepLink = profile.profileDeepLink,
        isVerified = profile.isVerified,
        followerCount = profile.followerCount,
        followingCount = profile.followingCount,
        likesCount = profile.likesCount,
        videoCount = profile.videoCount,
        cachedAtEpochMs = System.currentTimeMillis(),
      ),
    )
  }

  suspend fun replaceCachedVideos(videos: List<TikTokDisplayVideo>) {
    val now = System.currentTimeMillis()
    dao.clearVideos()
    if (videos.isNotEmpty()) {
      dao.upsertVideos(
        videos.map { video ->
          CachedVideoEntity(
            id = video.id,
            createTime = video.createTime,
            coverImageUrl = video.coverImageUrl,
            shareUrl = video.shareUrl,
            videoDescription = video.videoDescription,
            duration = video.duration,
            height = video.height,
            width = video.width,
            title = video.title,
            embedLink = video.embedLink,
            likeCount = video.likeCount,
            commentCount = video.commentCount,
            shareCount = video.shareCount,
            viewCount = video.viewCount,
            cachedAtEpochMs = now,
          )
        },
      )
    }
  }

  suspend fun clearAllCache() {
    dao.clearProfile()
    dao.clearVideos()
  }
}
