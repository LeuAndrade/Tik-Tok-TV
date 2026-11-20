package com.example.navigation

import android.view.KeyEvent

sealed interface RemoteAction {
  data object None : RemoteAction
  data object PreviousVideo : RemoteAction
  data object NextVideo : RemoteAction
  data object PlayPause : RemoteAction
  data object OpenNavigationDrawer : RemoteAction
  data object OpenVideoActions : RemoteAction
  data object OpenContextMenu : RemoteAction
  data object CloseOrBack : RemoteAction
  data class MoveVirtualPointer(val dx: Float, val dy: Float) : RemoteAction
  data object ClickVirtualPointer : RemoteAction
  data object DisableVirtualPointer : RemoteAction
}

object TvRemoteKeyHandler {

  fun resolveFeedKeyAction(
    keyCode: Int,
    action: Int,
    isLongPress: Boolean,
    activeOverlay: ActiveOverlay,
    currentDestination: TvDestination,
    virtualPointerActive: Boolean,
    pointerStep: Float = 0.035f,
  ): RemoteAction {
    if (virtualPointerActive) {
      if (action == KeyEvent.ACTION_DOWN) {
        return when (keyCode) {
          KeyEvent.KEYCODE_DPAD_UP -> RemoteAction.MoveVirtualPointer(0f, -pointerStep)
          KeyEvent.KEYCODE_DPAD_DOWN -> RemoteAction.MoveVirtualPointer(0f, pointerStep)
          KeyEvent.KEYCODE_DPAD_LEFT -> RemoteAction.MoveVirtualPointer(-pointerStep, 0f)
          KeyEvent.KEYCODE_DPAD_RIGHT -> RemoteAction.MoveVirtualPointer(pointerStep, 0f)
          KeyEvent.KEYCODE_DPAD_CENTER,
          KeyEvent.KEYCODE_ENTER,
          KeyEvent.KEYCODE_NUMPAD_ENTER -> RemoteAction.ClickVirtualPointer
          KeyEvent.KEYCODE_BACK,
          KeyEvent.KEYCODE_ESCAPE -> RemoteAction.DisableVirtualPointer
          else -> RemoteAction.None
        }
      }
      return RemoteAction.None
    }

    val isVideoFeedScreen =
      currentDestination == TvDestination.FOR_YOU ||
        currentDestination == TvDestination.FOLLOWING ||
        currentDestination == TvDestination.LIVES

    if (action == KeyEvent.ACTION_DOWN && isLongPress &&
      (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)
    ) {
      return RemoteAction.OpenContextMenu
    }

    if (action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_MENU) {
      return RemoteAction.OpenContextMenu
    }

    if (action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE)) {
      return if (activeOverlay != ActiveOverlay.NONE) {
        RemoteAction.CloseOrBack
      } else {
        RemoteAction.CloseOrBack
      }
    }

    if (!isVideoFeedScreen || activeOverlay != ActiveOverlay.NONE) {
      return RemoteAction.None
    }

    if (action != KeyEvent.ACTION_DOWN) return RemoteAction.None

    return when (keyCode) {
      KeyEvent.KEYCODE_DPAD_UP,
      KeyEvent.KEYCODE_CHANNEL_UP,
      KeyEvent.KEYCODE_MEDIA_PREVIOUS -> RemoteAction.PreviousVideo
      KeyEvent.KEYCODE_DPAD_DOWN,
      KeyEvent.KEYCODE_CHANNEL_DOWN,
      KeyEvent.KEYCODE_MEDIA_NEXT -> RemoteAction.NextVideo
      KeyEvent.KEYCODE_DPAD_CENTER,
      KeyEvent.KEYCODE_ENTER,
      KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
      KeyEvent.KEYCODE_SPACE -> RemoteAction.PlayPause
      KeyEvent.KEYCODE_DPAD_LEFT -> RemoteAction.OpenNavigationDrawer
      KeyEvent.KEYCODE_DPAD_RIGHT -> RemoteAction.OpenVideoActions
      else -> RemoteAction.None
    }
  }
}
