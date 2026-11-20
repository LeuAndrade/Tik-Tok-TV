package com.example.auth

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

class TikTokAuthRepository(
  private val tokenStore: SecureTokenStore,
  private val backendBaseUrl: String = BuildConfig.TIKTOK_AUTH_BACKEND_URL,
  private val backendApiOverride: TikTokAuthBackendApi? = null,
  private val displayApiOverride: TikTokDisplayApi? = null,
) {
  private val random = SecureRandom()

  private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

  private val httpClient: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .writeTimeout(15, TimeUnit.SECONDS)
      .build()

  private val backendApi: TikTokAuthBackendApi? by lazy {
    if (backendApiOverride != null) return@lazy backendApiOverride
    if (!isBackendConfigured(backendBaseUrl)) return@lazy null
    val normalized = if (backendBaseUrl.endsWith("/")) backendBaseUrl else "$backendBaseUrl/"
    Retrofit.Builder()
      .baseUrl(normalized)
      .client(httpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(TikTokAuthBackendApi::class.java)
  }

  private val displayApi: TikTokDisplayApi by lazy {
    displayApiOverride
      ?: Retrofit.Builder()
        .baseUrl("https://open.tiktokapis.com/")
        .client(httpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(TikTokDisplayApi::class.java)
  }

  private val _qrState = MutableStateFlow<QrAuthorizationState>(QrAuthorizationState.Idle)
  val qrState: StateFlow<QrAuthorizationState> = _qrState.asStateFlow()

  val tokenBundleFlow: Flow<TikTokTokenBundle?> = tokenStore.tokenBundleFlow
  val userProfileFlow: Flow<TikTokUserProfile?> = tokenStore.userProfileFlow

  fun isBackendConfigured(url: String = backendBaseUrl): Boolean {
    val clean = url.trim()
    return clean.startsWith("https://") &&
      !clean.contains("your-tiktok-tv-auth-backend.example.com", ignoreCase = true) ||
      clean.startsWith("http://10.0.2.2") ||
      clean.startsWith("http://localhost")
  }

  fun generateClientTicket(length: Int = 16): String {
    val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    return buildString(length) {
      repeat(length) {
        append(chars[random.nextInt(chars.length)])
      }
    }
  }

  fun generateCsrfState(): String = generateClientTicket(24)

  fun startQrCodeLoginFlow(): Flow<QrAuthorizationState> = flow {
    val api = backendApi
    if (api == null) {
      val errorState =
        QrAuthorizationState.Error(
          message = "Backend de autenticação OAuth não configurado (TIKTOK_AUTH_BACKEND_URL). Use o login oficial via WebView TikTok abaixo ou configure o auth-backend.",
          isConfigurationMissing = true,
        )
      _qrState.value = errorState
      emit(errorState)
      return@flow
    }

    _qrState.value = QrAuthorizationState.Requesting
    emit(QrAuthorizationState.Requesting)

    val clientTicket = generateClientTicket()
    val csrfState = generateCsrfState()

    val startResp =
      try {
        api.startQrAuthorization(
          QrStartRequest(
            clientTicket = clientTicket,
            state = csrfState,
          ),
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        val state = QrAuthorizationState.Error("Falha de conexão ao solicitar QR Code: ${e.message ?: "erro de rede"}")
        _qrState.value = state
        emit(state)
        return@flow
      }

    if (!startResp.isSuccessful || startResp.body() == null) {
      val state = QrAuthorizationState.Error("Erro HTTP ${startResp.code()} ao gerar QR Code de autorização TikTok.")
      _qrState.value = state
      emit(state)
      return@flow
    }

    val qrBody = startResp.body()!!
    if (qrBody.state != csrfState || qrBody.clientTicket != clientTicket) {
      val state = QrAuthorizationState.Error("Falha na validação CSRF/client_ticket do QR Code.")
      _qrState.value = state
      emit(state)
      return@flow
    }

    val waiting =
      QrAuthorizationState.WaitingScan(
        scanQrCodeUrl = qrBody.scanQrCodeUrl,
        clientTicket = clientTicket,
        csrfState = csrfState,
        expiresInSeconds = qrBody.expiresIn,
      )
    _qrState.value = waiting
    emit(waiting)

    val deadlineMs = System.currentTimeMillis() + (qrBody.expiresIn * 1000L)
    while (System.currentTimeMillis() < deadlineMs) {
      delay(3000L)
      val pollResp =
        try {
          api.checkQrAuthorization(
            QrCheckRequest(
              token = qrBody.token,
              clientTicket = clientTicket,
              state = csrfState,
            ),
          )
        } catch (e: CancellationException) {
          throw e
        } catch (_: Exception) {
          continue
        }

      if (!pollResp.isSuccessful) continue
      val pollBody = pollResp.body() ?: continue

      if (pollBody.state != null && pollBody.state != csrfState) {
        val err = QrAuthorizationState.Error("Divergência de estado CSRF detectada durante polling.")
        _qrState.value = err
        emit(err)
        return@flow
      }

      when (pollBody.status.lowercase()) {
        "new" -> Unit
        "scanned" -> {
          val scanned = QrAuthorizationState.ScannedPendingConfirm(qrBody.scanQrCodeUrl, clientTicket)
          _qrState.value = scanned
          emit(scanned)
        }
        "confirmed" -> {
          val tokens = pollBody.tokenBundle
          if (tokens == null) {
            val err = QrAuthorizationState.Error("Autorização confirmada, porém o backend não retornou o token OAuth.")
            _qrState.value = err
            emit(err)
            return@flow
          }
          tokenStore.saveTokenBundle(tokens)
          val profile = fetchAndPersistUserInfo(tokens.accessToken)
          val confirmed = QrAuthorizationState.Confirmed(profile)
          _qrState.value = confirmed
          emit(confirmed)
          return@flow
        }
        "expired" -> {
          _qrState.value = QrAuthorizationState.Expired
          emit(QrAuthorizationState.Expired)
          return@flow
        }
        "refused", "declined", "cancelled" -> {
          val declined = QrAuthorizationState.Declined(pollBody.error ?: "Autorização recusada no aplicativo TikTok.")
          _qrState.value = declined
          emit(declined)
          return@flow
        }
        else -> {
          if (!pollBody.error.isNullOrBlank()) {
            val err = QrAuthorizationState.Error(pollBody.error)
            _qrState.value = err
            emit(err)
            return@flow
          }
        }
      }
    }

    _qrState.value = QrAuthorizationState.Expired
    emit(QrAuthorizationState.Expired)
  }

  fun resetQrState() {
    _qrState.value = QrAuthorizationState.Idle
  }

  suspend fun ensureValidAccessToken(): String? {
    val current = tokenStore.getTokenBundle() ?: return null
    if (!current.isAccessTokenExpired()) {
      return current.accessToken
    }
    if (current.isRefreshTokenExpired()) {
      return null
    }
    val api = backendApi ?: return null
    val refreshResp =
      runCatching {
        api.refreshAccessToken(RefreshTokenRequest(current.refreshToken))
      }.getOrNull() ?: return null

    if (refreshResp.isSuccessful && refreshResp.body() != null) {
      val rotated = refreshResp.body()!!
      tokenStore.saveTokenBundle(rotated)
      return rotated.accessToken
    }
    return null
  }

  suspend fun fetchAndPersistUserInfo(explicitAccessToken: String? = null): TikTokUserProfile? {
    val accessToken = explicitAccessToken ?: ensureValidAccessToken() ?: return null
    val resp = runCatching { displayApi.getUserInfo("Bearer $accessToken") }.getOrNull() ?: return null
    if (!resp.isSuccessful) return null
    val user = resp.body()?.data?.user ?: return null
    tokenStore.saveUserProfile(user)
    return user
  }

  suspend fun fetchAuthorizedUserVideos(cursor: Long? = null, maxCount: Int = 20): Result<VideoListData> {
    val accessToken =
      ensureValidAccessToken()
        ?: return Result.failure(IllegalStateException("Sessão OAuth TikTok não conectada ou expirada."))
    return try {
      val resp =
        displayApi.listUserVideos(
          bearerToken = "Bearer $accessToken",
          request = VideoListRequest(cursor = cursor, maxCount = maxCount.coerceIn(1, 20)),
        )
      if (!resp.isSuccessful) {
        Result.failure(IllegalStateException("Display API HTTP ${resp.code()}"))
      } else {
        val body = resp.body()
        val apiError = body?.error
        if (apiError != null && apiError.code != "ok" && apiError.code.isNotBlank()) {
          Result.failure(IllegalStateException("${apiError.code}: ${apiError.message}"))
        } else {
          Result.success(body?.data ?: VideoListData())
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun logoutAndWipeOAuth() {
    val current = tokenStore.getTokenBundle()
    if (current != null) {
      runCatching {
        backendApi?.revokeToken(RevokeTokenRequest(current.accessToken))
      }
    }
    tokenStore.clearAll()
    _qrState.value = QrAuthorizationState.Idle
  }
}
