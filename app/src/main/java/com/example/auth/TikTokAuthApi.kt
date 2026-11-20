package com.example.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class QrStartRequest(
  @Json(name = "client_ticket") val clientTicket: String,
  @Json(name = "state") val state: String,
  @Json(name = "scope") val scope: String = "user.info.basic,user.info.profile,user.info.stats,video.list",
)

@JsonClass(generateAdapter = true)
data class QrStartResponse(
  @Json(name = "scan_qrcode_url") val scanQrCodeUrl: String,
  @Json(name = "token") val token: String,
  @Json(name = "client_ticket") val clientTicket: String,
  @Json(name = "state") val state: String,
  @Json(name = "expires_in") val expiresIn: Int = 300,
)

@JsonClass(generateAdapter = true)
data class QrCheckRequest(
  @Json(name = "token") val token: String,
  @Json(name = "client_ticket") val clientTicket: String,
  @Json(name = "state") val state: String,
)

@JsonClass(generateAdapter = true)
data class QrCheckResponse(
  @Json(name = "status") val status: String,
  @Json(name = "client_ticket") val clientTicket: String? = null,
  @Json(name = "state") val state: String? = null,
  @Json(name = "error") val error: String? = null,
  @Json(name = "token_bundle") val tokenBundle: TikTokTokenBundle? = null,
)

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
  @Json(name = "refresh_token") val refreshToken: String,
)

@JsonClass(generateAdapter = true)
data class RevokeTokenRequest(
  @Json(name = "token") val token: String,
)

@JsonClass(generateAdapter = true)
data class DisplayApiError(
  @Json(name = "code") val code: String = "ok",
  @Json(name = "message") val message: String = "",
  @Json(name = "log_id") val logId: String = "",
)

@JsonClass(generateAdapter = true)
data class UserInfoData(
  @Json(name = "user") val user: TikTokUserProfile? = null,
)

@JsonClass(generateAdapter = true)
data class UserInfoResponse(
  @Json(name = "data") val data: UserInfoData? = null,
  @Json(name = "error") val error: DisplayApiError? = null,
)

@JsonClass(generateAdapter = true)
data class VideoListRequest(
  @Json(name = "cursor") val cursor: Long? = null,
  @Json(name = "max_count") val maxCount: Int = 20,
)

@JsonClass(generateAdapter = true)
data class VideoListData(
  @Json(name = "videos") val videos: List<TikTokDisplayVideo> = emptyList(),
  @Json(name = "cursor") val cursor: Long? = null,
  @Json(name = "has_more") val hasMore: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class VideoListResponse(
  @Json(name = "data") val data: VideoListData? = null,
  @Json(name = "error") val error: DisplayApiError? = null,
)

@JsonClass(generateAdapter = true)
data class VideoQueryFilters(
  @Json(name = "video_ids") val videoIds: List<String>,
)

@JsonClass(generateAdapter = true)
data class VideoQueryRequest(
  @Json(name = "filters") val filters: VideoQueryFilters,
)

interface TikTokAuthBackendApi {
  @POST("api/tiktok/qr/start")
  suspend fun startQrAuthorization(@Body request: QrStartRequest): Response<QrStartResponse>

  @POST("api/tiktok/qr/status")
  suspend fun checkQrAuthorization(@Body request: QrCheckRequest): Response<QrCheckResponse>

  @POST("api/tiktok/oauth/refresh")
  suspend fun refreshAccessToken(@Body request: RefreshTokenRequest): Response<TikTokTokenBundle>

  @POST("api/tiktok/oauth/revoke")
  suspend fun revokeToken(@Body request: RevokeTokenRequest): Response<Unit>
}

interface TikTokDisplayApi {
  @GET("v2/user/info/")
  suspend fun getUserInfo(
    @Header("Authorization") bearerToken: String,
    @Query("fields") fields: String = "open_id,union_id,avatar_url,avatar_url_100,display_name,bio_description,profile_deep_link,is_verified,follower_count,following_count,likes_count,video_count,username",
  ): Response<UserInfoResponse>

  @POST("v2/video/list/")
  suspend fun listUserVideos(
    @Header("Authorization") bearerToken: String,
    @Query("fields") fields: String = "id,create_time,cover_image_url,share_url,video_description,duration,height,width,title,embed_link,like_count,comment_count,share_count,view_count",
    @Body request: VideoListRequest = VideoListRequest(),
  ): Response<VideoListResponse>

  @POST("v2/video/query/")
  suspend fun queryVideos(
    @Header("Authorization") bearerToken: String,
    @Query("fields") fields: String = "id,create_time,cover_image_url,share_url,video_description,duration,height,width,title,embed_link,like_count,comment_count,share_count,view_count",
    @Body request: VideoQueryRequest,
  ): Response<VideoListResponse>
}
