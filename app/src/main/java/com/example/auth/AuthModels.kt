package com.example.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TikTokTokenBundle(
  @Json(name = "access_token") val accessToken: String,
  @Json(name = "refresh_token") val refreshToken: String,
  @Json(name = "open_id") val openId: String,
  @Json(name = "expires_in") val expiresInSeconds: Long,
  @Json(name = "refresh_expires_in") val refreshExpiresInSeconds: Long,
  @Json(name = "scope") val scope: String,
  @Json(name = "token_type") val tokenType: String = "Bearer",
  val obtainedAtEpochMs: Long = System.currentTimeMillis(),
) {
  fun isAccessTokenExpired(nowMs: Long = System.currentTimeMillis()): Boolean {
    val expiresAt = obtainedAtEpochMs + (expiresInSeconds * 1000L)
    return nowMs >= (expiresAt - 60_000L)
  }

  fun isRefreshTokenExpired(nowMs: Long = System.currentTimeMillis()): Boolean {
    val expiresAt = obtainedAtEpochMs + (refreshExpiresInSeconds * 1000L)
    return nowMs >= expiresAt
  }
}

@JsonClass(generateAdapter = true)
data class TikTokUserProfile(
  @Json(name = "open_id") val openId: String = "",
  @Json(name = "union_id") val unionId: String = "",
  @Json(name = "avatar_url") val avatarUrl: String = "",
  @Json(name = "avatar_url_100") val avatarUrl100: String = "",
  @Json(name = "display_name") val displayName: String = "",
  @Json(name = "username") val username: String = "",
  @Json(name = "bio_description") val bioDescription: String = "",
  @Json(name = "profile_deep_link") val profileDeepLink: String = "",
  @Json(name = "is_verified") val isVerified: Boolean = false,
  @Json(name = "follower_count") val followerCount: Long = 0L,
  @Json(name = "following_count") val followingCount: Long = 0L,
  @Json(name = "likes_count") val likesCount: Long = 0L,
  @Json(name = "video_count") val videoCount: Long = 0L,
)

@JsonClass(generateAdapter = true)
data class TikTokDisplayVideo(
  @Json(name = "id") val id: String,
  @Json(name = "create_time") val createTime: Long = 0L,
  @Json(name = "cover_image_url") val coverImageUrl: String = "",
  @Json(name = "share_url") val shareUrl: String = "",
  @Json(name = "video_description") val videoDescription: String = "",
  @Json(name = "duration") val duration: Int = 0,
  @Json(name = "height") val height: Int = 0,
  @Json(name = "width") val width: Int = 0,
  @Json(name = "title") val title: String = "",
  @Json(name = "embed_link") val embedLink: String = "",
  @Json(name = "like_count") val likeCount: Long = 0L,
  @Json(name = "comment_count") val commentCount: Long = 0L,
  @Json(name = "share_count") val shareCount: Long = 0L,
  @Json(name = "view_count") val viewCount: Long = 0L,
)

sealed interface QrAuthorizationState {
  data object Idle : QrAuthorizationState
  data object Requesting : QrAuthorizationState
  data class WaitingScan(
    val scanQrCodeUrl: String,
    val clientTicket: String,
    val csrfState: String,
    val expiresInSeconds: Int,
  ) : QrAuthorizationState
  data class ScannedPendingConfirm(
    val scanQrCodeUrl: String,
    val clientTicket: String,
  ) : QrAuthorizationState
  data class Confirmed(val profile: TikTokUserProfile?) : QrAuthorizationState
  data object Expired : QrAuthorizationState
  data class Declined(val reason: String) : QrAuthorizationState
  data class Error(val message: String, val isConfigurationMissing: Boolean = false) : QrAuthorizationState
}

enum class WebSessionStatus(val label: String) {
  UNKNOWN("Verificando sessão web"),
  GUEST("Sessão pública (sem login web)"),
  AUTHENTICATED("Sessão web TikTok ativa"),
  SECURITY_CHALLENGE("Verificação de segurança solicitada pelo TikTok"),
}

data class CombinedAuthState(
  val oauthConnected: Boolean = false,
  val oauthTokenExpiresAtMs: Long? = null,
  val oauthScopes: String = "",
  val userProfile: TikTokUserProfile? = null,
  val webSessionStatus: WebSessionStatus = WebSessionStatus.UNKNOWN,
  val webHandle: String? = null,
  val authErrorMessage: String? = null,
)
