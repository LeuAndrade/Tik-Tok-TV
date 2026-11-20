package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.auth.QrAuthorizationState
import com.example.auth.QrCheckRequest
import com.example.auth.QrCheckResponse
import com.example.auth.QrCodeGenerator
import com.example.auth.QrStartRequest
import com.example.auth.QrStartResponse
import com.example.auth.RefreshTokenRequest
import com.example.auth.SecureTokenStore
import com.example.auth.TikTokAuthBackendApi
import com.example.auth.TikTokAuthRepository
import com.example.auth.TikTokDisplayApi
import com.example.auth.TikTokDisplayVideo
import com.example.auth.TikTokTokenBundle
import com.example.auth.TikTokUserProfile
import com.example.auth.UserInfoData
import com.example.auth.UserInfoResponse
import com.example.auth.VideoListData
import com.example.auth.VideoListRequest
import com.example.auth.VideoListResponse
import com.example.auth.VideoQueryRequest
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthRepositoryAndQrTest {

  private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

  @Test
  fun `qr code generator builds non-null bitmap for valid tiktok url`() {
    val bmp = QrCodeGenerator.generateBitmap("https://www.tiktok.com/@creator/video/123456", 256)
    assertNotNull(bmp)
    assertEquals(256, bmp!!.width)
    assertEquals(256, bmp.height)
  }

  @Test
  fun `qr authorization flow transitions waiting to scanned to confirmed and fetches profile`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val tokenStore = SecureTokenStore(context, moshi)
    tokenStore.clearAll()

    var pollCount = 0
    val fakeBackend =
      object : TikTokAuthBackendApi {
        override suspend fun startQrAuthorization(request: QrStartRequest): Response<QrStartResponse> =
          Response.success(
            QrStartResponse(
              scanQrCodeUrl = "aweme://authorize?client_ticket=${request.clientTicket}",
              token = "qr_tok_1",
              clientTicket = request.clientTicket,
              state = request.state,
              expiresIn = 60,
            ),
          )

        override suspend fun checkQrAuthorization(request: QrCheckRequest): Response<QrCheckResponse> {
          pollCount++
          return when (pollCount) {
            1 -> Response.success(QrCheckResponse(status = "scanned", clientTicket = request.clientTicket, state = request.state))
            else ->
              Response.success(
                QrCheckResponse(
                  status = "confirmed",
                  clientTicket = request.clientTicket,
                  state = request.state,
                  tokenBundle =
                    TikTokTokenBundle(
                      accessToken = "act.valid_123",
                      refreshToken = "rft.valid_456",
                      openId = "open_999",
                      expiresInSeconds = 86400,
                      refreshExpiresInSeconds = 31536000,
                      scope = "user.info.basic,video.list",
                    ),
                ),
              )
          }
        }

        override suspend fun refreshAccessToken(request: RefreshTokenRequest): Response<TikTokTokenBundle> =
          Response.success(
            TikTokTokenBundle(
              accessToken = "act.rotated_789",
              refreshToken = "rft.rotated_012",
              openId = "open_999",
              expiresInSeconds = 86400,
              refreshExpiresInSeconds = 31536000,
              scope = "user.info.basic,video.list",
            ),
          )

        override suspend fun revokeToken(request: com.example.auth.RevokeTokenRequest): Response<Unit> =
          Response.success(Unit)
      }

    val fakeDisplayApi =
      object : TikTokDisplayApi {
        override suspend fun getUserInfo(bearerToken: String, fields: String): Response<UserInfoResponse> =
          Response.success(
            UserInfoResponse(
              data =
                UserInfoData(
                  user = TikTokUserProfile(openId = "open_999", displayName = "Criador Real", followerCount = 4200),
                ),
            ),
          )

        override suspend fun listUserVideos(
          bearerToken: String,
          fields: String,
          request: VideoListRequest,
        ): Response<VideoListResponse> =
          Response.success(
            VideoListResponse(
              data = VideoListData(videos = listOf(TikTokDisplayVideo(id = "vid_1", title = "Teste TV"))),
            ),
          )

        override suspend fun queryVideos(
          bearerToken: String,
          fields: String,
          request: VideoQueryRequest,
        ): Response<VideoListResponse> = Response.success(VideoListResponse())
      }

    val repo =
      TikTokAuthRepository(
        tokenStore = tokenStore,
        backendBaseUrl = "https://auth.example.com",
        backendApiOverride = fakeBackend,
        displayApiOverride = fakeDisplayApi,
      )

    val emissions = repo.startQrCodeLoginFlow().toList()
    assertTrue(emissions.any { it is QrAuthorizationState.WaitingScan })
    assertTrue(emissions.any { it is QrAuthorizationState.ScannedPendingConfirm })
    val confirmed = emissions.last() as QrAuthorizationState.Confirmed
    assertEquals("Criador Real", confirmed.profile?.displayName)

    val videosResult = repo.fetchAuthorizedUserVideos()
    assertTrue(videosResult.isSuccess)
    assertEquals(1, videosResult.getOrNull()?.videos?.size)

    repo.logoutAndWipeOAuth()
    assertEquals(null, tokenStore.getTokenBundle())
  }

  @Test
  fun `expired access token rotates automatically via refresh token`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val tokenStore = SecureTokenStore(context, moshi)
    tokenStore.clearAll()
    tokenStore.saveTokenBundle(
      TikTokTokenBundle(
        accessToken = "act.old",
        refreshToken = "rft.old",
        openId = "open_1",
        expiresInSeconds = 10,
        refreshExpiresInSeconds = 36000,
        scope = "user.info.basic",
        obtainedAtEpochMs = System.currentTimeMillis() - 120_000L,
      ),
    )

    val fakeBackend =
      object : TikTokAuthBackendApi {
        override suspend fun startQrAuthorization(request: QrStartRequest) = error("unused")
        override suspend fun checkQrAuthorization(request: QrCheckRequest) = error("unused")
        override suspend fun revokeToken(request: com.example.auth.RevokeTokenRequest) = Response.success(Unit)
        override suspend fun refreshAccessToken(request: RefreshTokenRequest): Response<TikTokTokenBundle> {
          assertEquals("rft.old", request.refreshToken)
          return Response.success(
            TikTokTokenBundle(
              accessToken = "act.fresh",
              refreshToken = "rft.fresh",
              openId = "open_1",
              expiresInSeconds = 86400,
              refreshExpiresInSeconds = 31536000,
              scope = "user.info.basic",
            ),
          )
        }
      }

    val repo =
      TikTokAuthRepository(
        tokenStore = tokenStore,
        backendBaseUrl = "https://auth.example.com",
        backendApiOverride = fakeBackend,
      )

    val freshToken = repo.ensureValidAccessToken()
    assertEquals("act.fresh", freshToken)
    assertFalse(tokenStore.getTokenBundle()!!.isAccessTokenExpired())
  }
}
