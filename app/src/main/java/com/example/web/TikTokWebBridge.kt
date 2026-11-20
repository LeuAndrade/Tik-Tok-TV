package com.example.web

import android.webkit.JavascriptInterface
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

@JsonClass(generateAdapter = true)
data class WebCommentItem(
  val author: String = "",
  val text: String = "",
  val likesText: String = "",
  val timestamp: String = "",
)

@JsonClass(generateAdapter = true)
data class ActiveVideoMetadata(
  val videoUrl: String = "https://www.tiktok.com",
  val authorHandle: String = "",
  val authorDisplayName: String = "",
  val description: String = "",
  val likeCountText: String = "",
  val commentCountText: String = "",
  val shareCountText: String = "",
  val isLiked: Boolean = false,
  val isReposted: Boolean = false,
  val isFavorited: Boolean = false,
  val isFollowing: Boolean = false,
  val isPaused: Boolean = false,
  val isMuted: Boolean = false,
  val isHorizontal: Boolean = false,
  val hasVideoElement: Boolean = false,
  val isSecurityChallenge: Boolean = false,
  val isLoginWall: Boolean = false,
  val comments: List<WebCommentItem> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class TikTokWebActionResult(
  val action: String,
  val success: Boolean,
  val newState: String = "",
  val message: String = "",
)

class TikTokWebBridge {

  private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val metadataAdapter = moshi.adapter(ActiveVideoMetadata::class.java)
  private val actionResultAdapter = moshi.adapter(TikTokWebActionResult::class.java)

  private val _metadata = MutableStateFlow(ActiveVideoMetadata())
  val metadata: StateFlow<ActiveVideoMetadata> = _metadata.asStateFlow()

  private val _actionResults = MutableSharedFlow<TikTokWebActionResult>(extraBufferCapacity = 16)
  val actionResults: SharedFlow<TikTokWebActionResult> = _actionResults.asSharedFlow()

  @JavascriptInterface
  fun onMetadataUpdate(jsonPayload: String) {
    runCatching { metadataAdapter.fromJson(jsonPayload) }
      .getOrNull()
      ?.let { parsed ->
        _metadata.value = parsed
      }
  }

  @JavascriptInterface
  fun onActionResult(jsonPayload: String) {
    runCatching { actionResultAdapter.fromJson(jsonPayload) }
      .getOrNull()
      ?.let { result ->
        _actionResults.tryEmit(result)
      }
  }

  companion object {
    const val JS_INTERFACE_NAME = "TikTokTvBridge"

    fun buildObserverInjectionScript(reduceAnimations: Boolean): String =
      """
      (function() {
        try {
          if (${if (reduceAnimations) "true" else "false"} && !document.getElementById('tt-tv-econ-style')) {
            var style = document.createElement('style');
            style.id = 'tt-tv-econ-style';
            style.textContent = '*, *::before, *::after { transition-duration: 0.01ms !important; animation-duration: 0.01ms !important; }';
            document.head && document.head.appendChild(style);
          }

          window.__ttTvCollectState = function() {
            var video = document.querySelector('video');
            var authorEl = document.querySelector('[data-e2e="browse-username"], [data-e2e="video-author-uniqueid"], a[href^="/@"]');
            var descEl = document.querySelector('[data-e2e="browse-video-desc"], [data-e2e="video-desc"]');
            var likeBtn = document.querySelector('[data-e2e="like-icon"], [data-e2e="browse-like-icon"], button[aria-label*="Like"], button[aria-label*="Curtir"]');
            var likeCountEl = document.querySelector('[data-e2e="like-count"], [data-e2e="browse-like-count"]');
            var commentCountEl = document.querySelector('[data-e2e="comment-count"], [data-e2e="browse-comment-count"]');
            var shareCountEl = document.querySelector('[data-e2e="share-count"]');
            var favBtn = document.querySelector('[data-e2e="undefined-icon"], button[aria-label*="Favorite"], button[aria-label*="Favorit"], button[aria-label*="Salvar"]');
            var repostBtn = document.querySelector('[data-e2e="repost-icon"], button[aria-label*="Repost"], button[aria-label*="Republicar"]');
            var followBtn = document.querySelector('[data-e2e="follow-button"], [data-e2e="browse-follow"]');

            var commentNodes = Array.prototype.slice.call(document.querySelectorAll('[data-e2e="comment-item"], [class*="DivCommentItemContainer"]')).slice(0, 25);
            var comments = commentNodes.map(function(node) {
              var u = node.querySelector('[data-e2e="comment-username-1"], a[href^="/@"], span');
              var t = node.querySelector('[data-e2e="comment-level-1"], p');
              var l = node.querySelector('[data-e2e="comment-like-count"]');
              return {
                author: u ? (u.innerText || '').trim().slice(0, 60) : '',
                text: t ? (t.innerText || '').trim().slice(0, 400) : (node.innerText || '').trim().slice(0, 400),
                likesText: l ? (l.innerText || '').trim() : '',
                timestamp: ''
              };
            }).filter(function(c) { return c.text.length > 0; });

            var href = window.location.href || 'https://www.tiktok.com';
            var captcha = !!(document.querySelector('#captcha-verify-image, [id*="captcha"], .captcha_verify_container'));
            var loginWall = !!(document.querySelector('[data-e2e="login-modal"], #loginContainer'));
            var isHorizontal = !!(video && video.videoWidth > 0 && video.videoHeight > 0 && video.videoWidth > video.videoHeight);

            var payload = {
              videoUrl: href,
              authorHandle: authorEl ? (authorEl.innerText || '').trim().slice(0, 60) : '',
              authorDisplayName: '',
              description: descEl ? (descEl.innerText || '').trim().slice(0, 280) : '',
              likeCountText: likeCountEl ? (likeCountEl.innerText || '').trim() : '',
              commentCountText: commentCountEl ? (commentCountEl.innerText || '').trim() : '',
              shareCountText: shareCountEl ? (shareCountEl.innerText || '').trim() : '',
              isLiked: likeBtn ? (likeBtn.getAttribute('aria-pressed') === 'true' || (likeBtn.innerHTML || '').indexOf('#FE2C55') !== -1) : false,
              isReposted: repostBtn ? (repostBtn.getAttribute('aria-pressed') === 'true') : false,
              isFavorited: favBtn ? (favBtn.getAttribute('aria-pressed') === 'true') : false,
              isFollowing: followBtn ? ((followBtn.innerText || '').toLowerCase().indexOf('seguindo') !== -1 || (followBtn.innerText || '').toLowerCase().indexOf('following') !== -1) : false,
              isPaused: video ? video.paused : false,
              isMuted: video ? video.muted : false,
              isHorizontal: isHorizontal,
              hasVideoElement: !!video,
              isSecurityChallenge: captcha,
              isLoginWall: loginWall,
              comments: comments
            };
            if (window.$JS_INTERFACE_NAME && window.$JS_INTERFACE_NAME.onMetadataUpdate) {
              window.$JS_INTERFACE_NAME.onMetadataUpdate(JSON.stringify(payload));
            }
          };

          if (!window.__ttTvIntervalStarted) {
            window.__ttTvIntervalStarted = true;
            setInterval(window.__ttTvCollectState, 1200);
          }
          window.__ttTvCollectState();
        } catch (err) {}
      })();
      """.trimIndent()

    fun buildPlayPauseScript(): String =
      """
      (function() {
        var v = document.querySelector('video');
        if (!v) {
          window.$JS_INTERFACE_NAME.onActionResult(JSON.stringify({action:'play_pause',success:false,newState:'',message:'Nenhum reprodutor de vídeo ativo na página atual.'}));
          return;
        }
        if (v.paused) {
          v.play().then(function() {
            window.$JS_INTERFACE_NAME.onActionResult(JSON.stringify({action:'play_pause',success:true,newState:'playing',message:'Reprodução retomada'}));
          }).catch(function(e) {
            window.$JS_INTERFACE_NAME.onActionResult(JSON.stringify({action:'play_pause',success:false,newState:'paused',message:'Player exige interação na página: ' + e}));
          });
        } else {
          v.pause();
          window.$JS_INTERFACE_NAME.onActionResult(JSON.stringify({action:'play_pause',success:true,newState:'paused',message:'Vídeo pausado'}));
        }
        window.__ttTvCollectState && window.__ttTvCollectState();
      })();
      """.trimIndent()

    fun buildStepVideoScript(direction: Int): String =
      """
      (function() {
        var dir = $direction;
        var btnSelector = dir > 0
          ? '[data-e2e="arrow-right"], button[aria-label*="Next"], button[aria-label*="Próximo"], button[data-e2e="feed-next"]'
          : '[data-e2e="arrow-left"], button[aria-label*="Previous"], button[aria-label*="Anterior"], button[data-e2e="feed-prev"]';
        var btn = document.querySelector(btnSelector);
        if (btn) {
          btn.click();
          window.$JS_INTERFACE_NAME.onActionResult(JSON.stringify({action:'step_video',success:true,newState: dir > 0 ? 'next' : 'prev',message:''}));
        } else {
          window.scrollBy({ top: dir * Math.round(window.innerHeight * 0.85), behavior: 'smooth' });
          window.$JS_INTERFACE_NAME.onActionResult(JSON.stringify({action:'step_video',success:true,newState: dir > 0 ? 'next_scroll' : 'prev_scroll',message:''}));
        }
        setTimeout(function() { window.__ttTvCollectState && window.__ttTvCollectState(); }, 450);
      })();
      """.trimIndent()

    fun buildToggleActionScript(actionName: String): String {
      val selector =
        when (actionName) {
          "like" -> "[data-e2e=\"like-icon\"], [data-e2e=\"browse-like-icon\"], button[aria-label*=\"Like\"], button[aria-label*=\"Curtir\"]"
          "repost" -> "[data-e2e=\"repost-icon\"], button[aria-label*=\"Repost\"], button[aria-label*=\"Republicar\"]"
          "favorite" -> "[data-e2e=\"undefined-icon\"], button[aria-label*=\"Favorite\"], button[aria-label*=\"Favorit\"], button[aria-label*=\"Salvar\"]"
          "follow" -> "[data-e2e=\"follow-button\"], [data-e2e=\"browse-follow\"]"
          "comments" -> "[data-e2e=\"comment-icon\"], [data-e2e=\"browse-comment-icon\"], button[aria-label*=\"Comment\"], button[aria-label*=\"Coment\"]"
          else -> ""
        }
      return """
      (function() {
        var el = document.querySelector('$selector');
        if (!el) {
          window.$JS_INTERFACE_NAME.onActionResult(JSON.stringify({
            action:'$actionName',
            success:false,
            newState:'',
            message:'Botão não encontrado nesta tela do TikTok Web. Use o Ponteiro Virtual ou faça login na sessão web.'
          }));
          return;
        }
        var clickable = el.closest('button') || el;
        clickable.click();
        setTimeout(function() {
          window.__ttTvCollectState && window.__ttTvCollectState();
          var wall = document.querySelector('[data-e2e="login-modal"], #loginContainer');
          if (wall) {
            window.$JS_INTERFACE_NAME.onActionResult(JSON.stringify({
              action:'$actionName',
              success:false,
              newState:'login_required',
              message:'O TikTok solicitou autenticação web para concluir esta ação.'
            }));
          } else {
            window.$JS_INTERFACE_NAME.onActionResult(JSON.stringify({
              action:'$actionName',
              success:true,
              newState:'executed',
              message:'Ação enviada à sessão real do TikTok.'
            }));
          }
        }, 550);
      })();
      """.trimIndent()
    }

    fun buildPostCommentScript(rawComment: String): String {
      val escaped =
        rawComment
          .replace("\\", "\\\\")
          .replace("\"", "\\\"")
          .replace("\n", "\\n")
          .replace("\r", "")
      return """
      (function() {
        var commentText = "$escaped";
        var input = document.querySelector('[data-e2e="comment-input"] [contenteditable="true"], [contenteditable="true"][data-text], div[role="textbox"]');
        var postBtn = document.querySelector('[data-e2e="comment-post"], button[aria-label*="Post"], button[aria-label*="Publicar"]');
        if (!input || !postBtn) {
          window.$JS_INTERFACE_NAME.onActionResult(JSON.stringify({
            action:'post_comment',
            success:false,
            newState:'',
            message:'Campo de comentário não disponível nesta página ou exige sessão web conectada.'
          }));
          return;
        }
        input.focus();
        document.execCommand('insertText', false, commentText);
        input.dispatchEvent(new Event('input', { bubbles: true }));
        setTimeout(function() {
          if (postBtn.disabled || postBtn.getAttribute('aria-disabled') === 'true') {
            window.$JS_INTERFACE_NAME.onActionResult(JSON.stringify({
              action:'post_comment',
              success:false,
              newState:'',
              message:'Não foi possível habilitar o envio na sessão web atual.'
            }));
            return;
          }
          postBtn.click();
          setTimeout(function() {
            window.__ttTvCollectState && window.__ttTvCollectState();
            window.$JS_INTERFACE_NAME.onActionResult(JSON.stringify({
              action:'post_comment',
              success:true,
              newState:'submitted',
              message:'Comentário enviado ao TikTok Web.'
            }));
          }, 800);
        }, 300);
      })();
      """.trimIndent()
    }

    fun buildToggleMuteScript(): String =
      """
      (function() {
        var v = document.querySelector('video');
        if (!v) return;
        v.muted = !v.muted;
        window.$JS_INTERFACE_NAME.onActionResult(JSON.stringify({
          action:'mute',
          success:true,
          newState: v.muted ? 'muted' : 'unmuted',
          message: v.muted ? 'Áudio silenciado' : 'Áudio ativado'
        }));
        window.__ttTvCollectState && window.__ttTvCollectState();
      })();
      """.trimIndent()
  }
}
