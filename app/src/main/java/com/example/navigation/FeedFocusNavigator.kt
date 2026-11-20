package com.example.navigation

import android.view.KeyEvent as NativeKeyEvent
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.TikTokCyan

interface FeedDpadCallbacks {
  fun onPreviousVideo()
  fun onNextVideo()
  fun onTogglePlayPause()
  fun onOpenNavigationDrawer()
  fun onOpenVideoActions()
  fun onOpenContextMenu()
}

@Composable
fun FeedDpadFocusHost(
  focusRequester: FocusRequester,
  isOverlayOpen: Boolean,
  virtualPointerActive: Boolean,
  focusTriggerKey: Any = Unit,
  menuButtonFocusRequester: FocusRequester? = null,
  actionsButtonFocusRequester: FocusRequester? = null,
  onMoveVirtualPointer: (dx: Float, dy: Float) -> Unit,
  onClickVirtualPointer: () -> Unit,
  onDisableVirtualPointer: () -> Unit,
  callbacks: FeedDpadCallbacks,
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.(isFeedFocused: Boolean) -> Unit,
) {
  var isFeedFocused by remember { mutableStateOf(false) }
  var longPressConsumed by remember { mutableStateOf(false) }

  LaunchedEffect(isOverlayOpen, virtualPointerActive, focusTriggerKey) {
    if (!isOverlayOpen) {
      runCatching { focusRequester.requestFocus() }
    }
  }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .testTag("feed_focus_host")
        .focusRequester(focusRequester)
        .focusProperties {
          canFocus = !isOverlayOpen
          if (menuButtonFocusRequester != null) {
            left = menuButtonFocusRequester
          }
          if (actionsButtonFocusRequester != null) {
            right = actionsButtonFocusRequester
          }
        }
        .onFocusChanged { state ->
          isFeedFocused = state.isFocused
        }
        .onPreviewKeyEvent { keyEvent ->
          if (!virtualPointerActive) return@onPreviewKeyEvent false
          handleVirtualPointerKeyEvent(
            keyEvent = keyEvent,
            onMoveVirtualPointer = onMoveVirtualPointer,
            onClickVirtualPointer = onClickVirtualPointer,
            onDisableVirtualPointer = onDisableVirtualPointer,
          )
        }
        .onKeyEvent { keyEvent ->
          if (isOverlayOpen || virtualPointerActive) return@onKeyEvent false
          handleFeedDpadKeyEvent(
            keyEvent = keyEvent,
            longPressConsumed = longPressConsumed,
            onUpdateLongPressConsumed = { longPressConsumed = it },
            callbacks = callbacks,
          )
        }
        .focusable()
        .then(
          if (isFeedFocused && !isOverlayOpen && !virtualPointerActive) {
            Modifier.border(2.dp, TikTokCyan.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
          } else {
            Modifier.border(0.dp, Color.Transparent)
          },
        ),
  ) {
    content(isFeedFocused)
  }
}

internal fun handleVirtualPointerKeyEvent(
  keyEvent: KeyEvent,
  pointerStep: Float = 0.035f,
  onMoveVirtualPointer: (dx: Float, dy: Float) -> Unit,
  onClickVirtualPointer: () -> Unit,
  onDisableVirtualPointer: () -> Unit,
): Boolean {
  if (keyEvent.type != KeyEventType.KeyDown) return false
  return when (keyEvent.key) {
    Key.DirectionUp -> {
      onMoveVirtualPointer(0f, -pointerStep)
      true
    }
    Key.DirectionDown -> {
      onMoveVirtualPointer(0f, pointerStep)
      true
    }
    Key.DirectionLeft -> {
      onMoveVirtualPointer(-pointerStep, 0f)
      true
    }
    Key.DirectionRight -> {
      onMoveVirtualPointer(pointerStep, 0f)
      true
    }
    Key.DirectionCenter,
    Key.Enter,
    Key.NumPadEnter -> {
      onClickVirtualPointer()
      true
    }
    Key.Back,
    Key.Escape -> {
      onDisableVirtualPointer()
      true
    }
    else -> false
  }
}

internal fun handleFeedDpadKeyEvent(
  keyEvent: KeyEvent,
  longPressConsumed: Boolean,
  onUpdateLongPressConsumed: (Boolean) -> Unit,
  callbacks: FeedDpadCallbacks,
): Boolean {
  val native = keyEvent.nativeKeyEvent
  val isOkKey =
    keyEvent.key == Key.DirectionCenter ||
      keyEvent.key == Key.Enter ||
      keyEvent.key == Key.NumPadEnter

  if (keyEvent.type == KeyEventType.KeyDown) {
    if (isOkKey) {
      val isLong = (native.flags and NativeKeyEvent.FLAG_LONG_PRESS) != 0 || native.repeatCount >= 4
      if (isLong) {
        if (!longPressConsumed) {
          onUpdateLongPressConsumed(true)
          callbacks.onOpenContextMenu()
        }
        return true
      }
      onUpdateLongPressConsumed(false)
      return true
    }

    return when (keyEvent.key) {
      Key.DirectionUp,
      Key.ChannelUp,
      Key.MediaPrevious -> {
        callbacks.onPreviousVideo()
        true
      }
      Key.DirectionDown,
      Key.ChannelDown,
      Key.MediaNext -> {
        callbacks.onNextVideo()
        true
      }
      Key.DirectionLeft -> {
        callbacks.onOpenNavigationDrawer()
        true
      }
      Key.DirectionRight -> {
        callbacks.onOpenVideoActions()
        true
      }
      Key.MediaPlayPause,
      Key.Spacebar -> {
        callbacks.onTogglePlayPause()
        true
      }
      Key.Menu -> {
        callbacks.onOpenContextMenu()
        true
      }
      else -> false
    }
  }

  if (keyEvent.type == KeyEventType.KeyUp && isOkKey) {
    if (longPressConsumed) {
      onUpdateLongPressConsumed(false)
    } else {
      callbacks.onTogglePlayPause()
    }
    return true
  }

  return false
}
