package com.example.api

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.example.auth.DisplayApiError
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class CreatorInfoData(
  @Json(name = "creator_avatar_url") val creatorAvatarUrl: String = "",
  @Json(name = "creator_username") val creatorUsername: String = "",
  @Json(name = "creator_nickname") val creatorNickname: String = "",
  @Json(name = "privacy_level_options") val privacyLevelOptions: List<String> = emptyList(),
  @Json(name = "stitch_disabled") val stitchDisabled: Boolean = false,
  @Json(name = "duet_disabled") val duetDisabled: Boolean = false,
  @Json(name = "comment_disabled") val commentDisabled: Boolean = false,
  @Json(name = "max_video_post_duration_sec") val maxVideoPostDurationSec: Int = 60,
)

@JsonClass(generateAdapter = true)
data class CreatorInfoResponse(
  @Json(name = "data") val data: CreatorInfoData? = null,
  @Json(name = "error") val error: DisplayApiError? = null,
)

@JsonClass(generateAdapter = true)
data class PublishPostInfo(
  @Json(name = "title") val title: String,
  @Json(name = "privacy_level") val privacyLevel: String,
  @Json(name = "disable_duet") val disableDuet: Boolean = false,
  @Json(name = "disable_comment") val disableComment: Boolean = false,
  @Json(name = "disable_stitch") val disableStitch: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class PublishSourceInfo(
  @Json(name = "source") val source: String = "FILE_UPLOAD",
  @Json(name = "video_size") val videoSize: Long,
  @Json(name = "chunk_size") val chunkSize: Long,
  @Json(name = "total_chunk_count") val totalChunkCount: Int,
)

@JsonClass(generateAdapter = true)
data class DirectPostInitRequest(
  @Json(name = "post_info") val postInfo: PublishPostInfo,
  @Json(name = "source_info") val sourceInfo: PublishSourceInfo,
)

@JsonClass(generateAdapter = true)
data class InboxUploadInitRequest(
  @Json(name = "source_info") val sourceInfo: PublishSourceInfo,
)

@JsonClass(generateAdapter = true)
data class PublishInitData(
  @Json(name = "publish_id") val publishId: String = "",
  @Json(name = "upload_url") val uploadUrl: String = "",
)

@JsonClass(generateAdapter = true)
data class PublishInitResponse(
  @Json(name = "data") val data: PublishInitData? = null,
  @Json(name = "error") val error: DisplayApiError? = null,
)

@JsonClass(generateAdapter = true)
data class PublishStatusRequest(
  @Json(name = "publish_id") val publishId: String,
)

@JsonClass(generateAdapter = true)
data class PublishStatusData(
  @Json(name = "status") val status: String = "",
  @Json(name = "fail_reason") val failReason: String? = null,
  @Json(name = "publicaly_available_post_id") val publiclyAvailablePostIds: List<String> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class PublishStatusResponse(
  @Json(name = "data") val data: PublishStatusData? = null,
  @Json(name = "error") val error: DisplayApiError? = null,
)

interface TikTokContentPostingApi {
  @POST("v2/post/publish/creator_info/query/")
  suspend fun queryCreatorInfo(
    @Header("Authorization") bearerToken: String,
  ): Response<CreatorInfoResponse>

  @POST("v2/post/publish/video/init/")
  suspend fun initDirectPost(
    @Header("Authorization") bearerToken: String,
    @Body request: DirectPostInitRequest,
  ): Response<PublishInitResponse>

  @POST("v2/post/publish/inbox/video/init/")
  suspend fun initInboxDraftUpload(
    @Header("Authorization") bearerToken: String,
    @Body request: InboxUploadInitRequest,
  ): Response<PublishInitResponse>

  @POST("v2/post/publish/status/fetch/")
  suspend fun fetchPublishStatus(
    @Header("Authorization") bearerToken: String,
    @Body request: PublishStatusRequest,
  ): Response<PublishStatusResponse>
}

sealed interface ContentPublishState {
  data object Idle : ContentPublishState
  data object LoadingCreatorInfo : ContentPublishState
  data class Uploading(val progressPercent: Int, val stageLabel: String) : ContentPublishState
  data class Processing(val publishId: String, val remoteStatus: String) : ContentPublishState
  data class Published(val publishId: String, val modeLabel: String) : ContentPublishState
  data class Failed(val message: String) : ContentPublishState
}

class ContentPostingRepository(
  apiOverride: TikTokContentPostingApi? = null,
) {
  private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

  private val httpClient: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(20, TimeUnit.SECONDS)
      .readTimeout(45, TimeUnit.SECONDS)
      .writeTimeout(60, TimeUnit.SECONDS)
      .build()

  private val api: TikTokContentPostingApi =
    apiOverride
      ?: Retrofit.Builder()
        .baseUrl("https://open.tiktokapis.com/")
        .client(httpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(TikTokContentPostingApi::class.java)

  suspend fun queryCreatorInfo(accessToken: String): Result<CreatorInfoData> {
    return try {
      val resp = api.queryCreatorInfo("Bearer $accessToken")
      if (!resp.isSuccessful) {
        Result.failure(IllegalStateException("Creator Info HTTP ${resp.code()}"))
      } else {
        val body = resp.body()
        val apiError = body?.error
        if (apiError != null && apiError.code != "ok" && apiError.code.isNotBlank()) {
          Result.failure(IllegalStateException("${apiError.code}: ${apiError.message}"))
        } else {
          Result.success(body?.data ?: CreatorInfoData())
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun uploadAndPublishVideo(
    contentResolver: ContentResolver,
    videoUri: Uri,
    accessToken: String,
    caption: String,
    privacyLevel: String,
    disableComment: Boolean,
    disableDuet: Boolean,
    disableStitch: Boolean,
    publishDirectly: Boolean,
    onProgress: (ContentPublishState) -> Unit,
  ): Result<String> {
    return try {
      val fileSize =
        queryFileSize(contentResolver, videoUri)
          ?: return Result.failure(IllegalStateException("Não foi possível determinar o tamanho do vídeo selecionado."))

      if (fileSize <= 0L) {
        return Result.failure(IllegalStateException("O arquivo de vídeo está vazio."))
      }

      // Single chunk for videos <= 64 MB; otherwise 10 MB chunks per TikTok Content Posting spec
      val chunkSize = if (fileSize <= 64L * 1024L * 1024L) fileSize else 10L * 1024L * 1024L
      val totalChunks = ((fileSize + chunkSize - 1L) / chunkSize).toInt().coerceAtLeast(1)

      onProgress(ContentPublishState.Uploading(5, "Inicializando envio oficial no TikTok..."))

      val sourceInfo =
        PublishSourceInfo(
          source = "FILE_UPLOAD",
          videoSize = fileSize,
          chunkSize = chunkSize,
          totalChunkCount = totalChunks,
        )

      val initResp =
        if (publishDirectly) {
          api.initDirectPost(
            bearerToken = "Bearer $accessToken",
            request =
              DirectPostInitRequest(
                postInfo =
                  PublishPostInfo(
                    title = caption.take(2200),
                    privacyLevel = privacyLevel,
                    disableDuet = disableDuet,
                    disableComment = disableComment,
                    disableStitch = disableStitch,
                  ),
                sourceInfo = sourceInfo,
              ),
          )
        } else {
          api.initInboxDraftUpload(
            bearerToken = "Bearer $accessToken",
            request = InboxUploadInitRequest(sourceInfo = sourceInfo),
          )
        }

      if (!initResp.isSuccessful) {
        return Result.failure(IllegalStateException("Erro HTTP ${initResp.code()} ao inicializar upload."))
      }
      val body = initResp.body()
      val apiErr = body?.error
      if (apiErr != null && apiErr.code != "ok" && apiErr.code.isNotBlank()) {
        return Result.failure(IllegalStateException("${apiErr.code}: ${apiErr.message}"))
      }

      val initData = body?.data
      val uploadUrl = initData?.uploadUrl.orEmpty()
      val publishId = initData?.publishId.orEmpty()
      if (uploadUrl.isBlank() || publishId.isBlank()) {
        return Result.failure(IllegalStateException("TikTok não retornou upload_url ou publish_id válido."))
      }

      val mimeType = contentResolver.getType(videoUri) ?: "video/mp4"
      withContext(Dispatchers.IO) {
        contentResolver.openInputStream(videoUri)?.use { input ->
          var offset = 0L
          var chunkIndex = 0
          val buffer = ByteArray(chunkSize.toInt())
          while (offset < fileSize) {
            val toRead = minOf(chunkSize, fileSize - offset).toInt()
            var readTotal = 0
            while (readTotal < toRead) {
              val n = input.read(buffer, readTotal, toRead - readTotal)
              if (n <= 0) break
              readTotal += n
            }
            if (readTotal <= 0) break

            val endByte = offset + readTotal - 1L
            val chunkBytes = if (readTotal == buffer.size) buffer else buffer.copyOf(readTotal)
            val request =
              Request.Builder()
                .url(uploadUrl)
                .put(chunkBytes.toRequestBody(mimeType.toMediaType()))
                .header("Content-Range", "bytes $offset-$endByte/$fileSize")
                .header("Content-Type", mimeType)
                .build()

            httpClient.newCall(request).execute().use { putResp ->
              if (!putResp.isSuccessful) {
                throw IllegalStateException("Falha no chunk ${chunkIndex + 1}/$totalChunks (HTTP ${putResp.code})")
              }
            }

            offset += readTotal
            chunkIndex++
            val percent = ((offset.toDouble() / fileSize.toDouble()) * 80.0).toInt().coerceIn(10, 90)
            onProgress(ContentPublishState.Uploading(percent, "Enviando parte $chunkIndex de $totalChunks..."))
          }
        } ?: throw IllegalStateException("Não foi possível abrir o arquivo de vídeo selecionado.")
      }

      // Poll official status confirmation
      repeat(10) {
        onProgress(ContentPublishState.Processing(publishId, "PROCESSING_UPLOAD"))
        delay(2000L)
        val statusResp = api.fetchPublishStatus("Bearer $accessToken", PublishStatusRequest(publishId))
        val statusData = statusResp.body()?.data
        when (statusData?.status) {
          "PUBLISH_COMPLETE",
          "SEND_TO_USER_INBOX" -> {
            val modeLabel =
              if (publishDirectly) "Publicado no perfil TikTok" else "Enviado para rascunhos no TikTok do celular"
            onProgress(ContentPublishState.Published(publishId, modeLabel))
            return Result.success(publishId)
          }
          "FAILED" -> {
            val reason = statusData.failReason ?: "Falha confirmada pelo TikTok"
            return Result.failure(IllegalStateException(reason))
          }
        }
      }

      val fallbackMode =
        if (publishDirectly) "Envio concluído (processando no TikTok)" else "Enviado para a caixa de rascunhos"
      onProgress(ContentPublishState.Published(publishId, fallbackMode))
      Result.success(publishId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private fun queryFileSize(contentResolver: ContentResolver, uri: Uri): Long? {
    contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
      val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
      if (idx >= 0 && cursor.moveToFirst()) {
        return cursor.getLong(idx)
      }
    }
    return contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length.takeIf { len -> len > 0 } }
  }
}
