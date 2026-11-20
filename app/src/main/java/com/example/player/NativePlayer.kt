package com.example.player

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.performance.MemoryMonitor
import com.example.performance.PerformanceProfile

object PlaybackErrorClassifier {
  fun classify(error: PlaybackException): String {
    return when (error.errorCode) {
      PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
      PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
        "Falha de conexão com a internet durante a reprodução."
      PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
        "Mídia indisponível, removida ou com restrição de acesso."
      PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
      PlaybackException.ERROR_CODE_DECODING_FAILED ->
        "Falha de decodificação de hardware MediaCodec neste dispositivo."
      PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
      PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED ->
        "Formato de vídeo incompatível com MediaCodec nativo."
      else -> "Erro do player Media3 (${error.errorCodeName}): ${error.localizedMessage ?: "falha desconhecida"}"
    }
  }
}

@OptIn(UnstableApi::class)
fun buildTvExoPlayer(
  context: Context,
  profile: PerformanceProfile,
  decoderMode: DecoderMode,
  cacheLimitMb: Int,
  repeatSingle: Boolean,
  onPlaybackError: (String) -> Unit,
): ExoPlayer {
  val renderersFactory =
    DefaultRenderersFactory(context).apply {
      when (decoderMode) {
        DecoderMode.AUTOMATIC -> {
          setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
          setEnableDecoderFallback(true)
        }
        DecoderMode.HARDWARE_STANDARD -> {
          setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
          setEnableDecoderFallback(true)
        }
        DecoderMode.LOW_LATENCY -> {
          setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
          setEnableDecoderFallback(true)
          setEnableAudioFloatOutput(false)
        }
      }
    }

  val minBuffer =
    if (decoderMode == DecoderMode.LOW_LATENCY) 750 else profile.minBufferMs
  val maxBuffer =
    if (decoderMode == DecoderMode.LOW_LATENCY) 3000 else profile.maxBufferMs
  val playbackStartBuffer =
    if (decoderMode == DecoderMode.LOW_LATENCY) 400 else 1000
  val rebufferMs =
    if (decoderMode == DecoderMode.LOW_LATENCY) 800 else 2000

  val loadControl =
    DefaultLoadControl.Builder()
      .setBufferDurationsMs(minBuffer, maxBuffer, playbackStartBuffer, rebufferMs)
      .setPrioritizeTimeOverSizeThresholds(true)
      .build()

  val cacheDataSourceFactory = VideoCacheManager.buildDataSourceFactory(context, cacheLimitMb)
  val mediaSourceFactory = DefaultMediaSourceFactory(cacheDataSourceFactory)

  return ExoPlayer.Builder(context, renderersFactory)
    .setMediaSourceFactory(mediaSourceFactory)
    .setLoadControl(loadControl)
    .build()
    .apply {
      videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
      repeatMode = if (repeatSingle) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
      addListener(
        object : Player.Listener {
          override fun onPlayerError(error: PlaybackException) {
            onPlaybackError(PlaybackErrorClassifier.classify(error))
          }
        },
      )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun NativeMedia3PlayerSurface(
  streamUrl: String,
  performanceProfile: PerformanceProfile,
  decoderMode: DecoderMode,
  cacheLimitMb: Int,
  autoplay: Boolean,
  loopVideo: Boolean,
  isPaused: Boolean,
  isMuted: Boolean,
  isHorizontalFullscreen: Boolean,
  memoryMonitor: MemoryMonitor,
  onError: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (!MediaSourceValidator.isPlayableNativeStream(streamUrl)) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black))
    return
  }

  val context = LocalContext.current
  val exoPlayer =
    remember(performanceProfile, decoderMode, cacheLimitMb) {
      buildTvExoPlayer(
        context = context,
        profile = performanceProfile,
        decoderMode = decoderMode,
        cacheLimitMb = cacheLimitMb,
        repeatSingle = loopVideo,
        onPlaybackError = { err ->
          memoryMonitor.recordDiagnostic("Media3", err)
          onError(err)
        },
      )
    }

  val mediaSession =
    remember(exoPlayer) {
      runCatching { MediaSession.Builder(context, exoPlayer).build() }.getOrNull()
    }

  LaunchedEffect(streamUrl, autoplay, loopVideo) {
    exoPlayer.repeatMode = if (loopVideo) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    exoPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
    exoPlayer.prepare()
    exoPlayer.playWhenReady = autoplay && !isPaused
  }

  LaunchedEffect(isPaused) {
    exoPlayer.playWhenReady = !isPaused
  }

  LaunchedEffect(isMuted) {
    exoPlayer.volume = if (isMuted) 0f else 1f
  }

  DisposableEffect(exoPlayer, mediaSession) {
    onDispose {
      runCatching { mediaSession?.release() }
      exoPlayer.release()
    }
  }

  Box(modifier = modifier.fillMaxSize().background(Color.Black).testTag("native_media3_player")) {
    AndroidView(
      factory = { ctx ->
        PlayerView(ctx).apply {
          layoutParams =
            FrameLayout.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT,
            )
          useController = false
          resizeMode =
            if (isHorizontalFullscreen) {
              AspectRatioFrameLayout.RESIZE_MODE_FIT
            } else {
              AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
          player = exoPlayer
        }
      },
      update = { view ->
        view.player = exoPlayer
      },
      modifier = Modifier.fillMaxSize(),
    )
  }
}
