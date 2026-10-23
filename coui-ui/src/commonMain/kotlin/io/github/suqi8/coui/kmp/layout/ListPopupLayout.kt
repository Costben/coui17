// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.layout

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.suqi8.coui.kmp.basic.ListPopupContent
import io.github.suqi8.coui.kmp.basic.ListPopupDefaults
import io.github.suqi8.coui.kmp.basic.PopupPositionProvider
import io.github.suqi8.coui.kmp.basic.PreciseClickState
import io.github.suqi8.coui.kmp.basic.popupGravityPath
import io.github.suqi8.coui.kmp.basic.rememberListPopupLayoutInfo
import io.github.suqi8.coui.kmp.overlay.OverlayListPopup
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.LocalDismissState
import io.github.suqi8.coui.kmp.window.WindowListPopup
import kotlinx.coroutines.launch

/**
 * Internal shared layout logic for [OverlayListPopup] and [WindowListPopup].
 *
 * @param show Whether the popup is currently shown.
 * @param popupHost A composable that provides the popup container (e.g., PopupLayout or Dialog).
 *   It receives an internal "mounted" state (true from the moment the enter animation starts
 *   until the exit animation finishes) and the inner content composable. Hosts that
 *   conditionally render their container (e.g., a `Dialog`) should respect this flag and
 *   should NOT animate enter/exit themselves — entry/exit transitions are driven internally.
 *   `passTouches` turns true once a closing popup should let touches through to the page beneath;
 *   a host that blocks touches outside the panel has to stop doing so from then on.
 * @param popupModifier The modifier to be applied to the popup content area.
 * @param popupPositionProvider The [PopupPositionProvider] for positioning.
 * @param alignment The alignment of the popup.
 * @param enableWindowDim Whether to render and animate the window-dim layer. The outside-tap
 *   dismiss gesture is preserved regardless of this flag.
 * @param onDismissRequest The callback when the popup is dismissed.
 * @param onDismissFinished Invoked when the hide animation completes — after the content exit
 *   springs and the window-level exit fade, i.e. when the host is about to unmount; not invoked if
 *   the hide is cancelled mid-flight (e.g., by [show] toggling back to true).
 * @param maxHeight The maximum height of the popup.
 * @param minWidth The minimum width of the popup.
 * @param preciseClickState When non-null and carrying a recorded touch point, the anchor is
 *   collapsed to that point so the popup opens at the finger (COUI `PreciseClickHelper` plus
 *   `PopupMenuLocateHelper.setAnchor`). A null state, or one with no point recorded, keeps the
 *   anchor-centred placement.
 * @param content The content of the popup.
 */
@Composable
internal fun ListPopupLayout(
    show: Boolean,
    popupHost: @Composable (visible: Boolean, passTouches: () -> Boolean, content: @Composable () -> Unit) -> Unit,
    popupModifier: Modifier = Modifier,
    popupPositionProvider: PopupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
    alignment: PopupPositionProvider.Align = PopupPositionProvider.Align.Start,
    enableWindowDim: Boolean = true,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    maxHeight: Dp? = null,
    minWidth: Dp = ListPopupDefaults.MinWidth,
    preciseClickState: PreciseClickState? = null,
    enableGlass: Boolean = false,
    content: @Composable () -> Unit,
) {
    val fractionProgress = remember { Animatable(0f) }
    val alphaProgress = remember { Animatable(0f) }
    val pathProgress = remember { Animatable(0f) }
    val dimProgress = remember { Animatable(0f) }
    val backProgress = remember { Animatable(0f) }
    val windowAlpha = remember { Animatable(1f) }
    var windowExiting by remember { mutableStateOf(false) }
    val currentShow by rememberUpdatedState(show)
    // Derived so the per-frame exit alpha only recomposes the host once, when it crosses over.
    // Handing touches back the moment the close starts, not when its fade is nearly over: the
    // close spring runs ~340ms and `windowExiting` only flips after it settles, so keying off the
    // alpha left a third of a second where the page looked dismissed but swallowed every drag.
    // The host unmounts by itself, so a popup that is merely fading out must not hold input.
    val passTouches by remember {
        derivedStateOf {
            !currentShow ||
                windowExiting ||
                alphaProgress.value < ListPopupDefaults.ExitTouchPassThroughAlpha
        }
    }
    val currentOnDismiss by rememberUpdatedState(onDismissRequest)
    val currentOnDismissFinished by rememberUpdatedState(onDismissFinished)
    val coroutineScope = rememberCoroutineScope()
    val internalVisible = remember { mutableStateOf(false) }
    var popupContentSize by remember { mutableStateOf(IntSize.Zero) }
    var hostPositionInWindow by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(show) {
        if (show) {
            internalVisible.value = true
            windowExiting = false
            windowAlpha.snapTo(1f)
            backProgress.snapTo(0f)
            // COUI runs three independent waves open (runOpenWaves): path t, scale and alpha.
            launch { pathProgress.animateTo(1f, ListPopupDefaults.PathEnterAnimationSpec) }
            launch { fractionProgress.animateTo(1f, ListPopupDefaults.FractionAnimationSpec) }
            launch { alphaProgress.animateTo(1f, ListPopupDefaults.AlphaEnterAnimationSpec) }
            if (enableWindowDim) {
                launch { dimProgress.animateTo(1f, ListPopupDefaults.DimEnterAnimationSpec) }
            }
        } else {
            if (!internalVisible.value) return@LaunchedEffect
            launch { pathProgress.animateTo(0f, ListPopupDefaults.PathExitAnimationSpec) }
            launch { fractionProgress.animateTo(0f, ListPopupDefaults.FractionExitAnimationSpec) }
            if (enableWindowDim) {
                launch { dimProgress.animateTo(0f, ListPopupDefaults.DimExitAnimationSpec) }
            }
            // Alpha controls the master timing: once content fades out, unmount immediately.
            alphaProgress.animateTo(0f, ListPopupDefaults.AlphaExitAnimationSpec)
            // COUI does not remove the window together with the content: `COUIPopupListWindow.dismiss`
            // posts `forceDismiss` 350ms later (`postDelayedDismiss(mWindowAnimationDismissRunnable,
            // 350L)`), and only then does the window play `Animation.COUI.PopupListWindow.Dismiss` —
            // the 200ms `coui_popup_list_window_exit` alpha. The close opacity spring settles at
            // ~340ms, so the fade starts on an already-invisible panel and its only remaining job is
            // to set how long the host stays mounted.
            windowExiting = true
            windowAlpha.animateTo(0f, ListPopupDefaults.WindowExitAnimationSpec)
            // Force-settle remaining tracks so the next enter starts from a clean zero baseline.
            pathProgress.snapTo(0f)
            fractionProgress.snapTo(0f)
            alphaProgress.snapTo(0f)
            dimProgress.snapTo(0f)
            backProgress.snapTo(0f)
            internalVisible.value = false
            currentOnDismissFinished?.invoke()
        }
    }

    if (!show && !internalVisible.value) return

    var parentBounds by remember { mutableStateOf(IntRect.Zero) }

    Spacer(
        modifier = Modifier
            .onGloballyPositioned { childCoordinates ->
                childCoordinates.parentLayoutCoordinates?.let { parentLayoutCoordinates ->
                    val positionInWindow = parentLayoutCoordinates.positionInWindow()
                    parentBounds = IntRect(
                        left = positionInWindow.x.toInt(),
                        top = positionInWindow.y.toInt(),
                        right = positionInWindow.x.toInt() + parentLayoutCoordinates.size.width,
                        bottom = positionInWindow.y.toInt() + parentLayoutCoordinates.size.height,
                    )
                }
            },
    )

    if (parentBounds == IntRect.Zero) return

    // COUI PopupMenuLocateHelper.setAnchor collapses the anchor to the recorded touch point, so both
    // the horizontal centring and the below/above test run against that point. The point is already
    // in window coordinates, the same space parentBounds uses.
    val touchPoint = preciseClickState?.touchPoint ?: Offset.Unspecified
    val anchorBounds = if (touchPoint.isSpecified) {
        val x = touchPoint.x.toInt()
        val y = touchPoint.y.toInt()
        IntRect(left = x, top = y, right = x, bottom = y)
    } else {
        parentBounds
    }

    val layoutInfo = rememberListPopupLayoutInfo(
        alignment = alignment,
        popupPositionProvider = popupPositionProvider,
        parentBounds = anchorBounds,
        popupContentSize = popupContentSize,
    )

    // COUI PopupMotionAnimateKitController.rebuildGeom: the trajectory is rebuilt from the anchor
    // offset on every motion start, so it follows the menu when the anchor moves.
    val gravityPath = remember(anchorBounds, layoutInfo, popupContentSize) {
        popupGravityPath(anchorBounds, layoutInfo, popupContentSize)
    }

    val requestDismiss: () -> Unit = remember {
        { currentOnDismiss?.invoke() }
    }

    popupHost(internalVisible.value, { passTouches }) {
        val navigationEventState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationBackHandler(
            state = navigationEventState,
            isBackEnabled = show,
            onBackCancelled = {
                coroutineScope.launch {
                    backProgress.animateTo(0f, ListPopupDefaults.ResetAnimationSpec)
                }
            },
            onBackCompleted = { requestDismiss() },
        )

        LaunchedEffect(Unit) {
            // Collect inside a single coroutine so the per-frame `transitionState` ticks during a
            // back gesture do not cancel/relaunch the LaunchedEffect on every progress update.
            snapshotFlow { navigationEventState.transitionState }
                .collect { transitionState ->
                    if (
                        transitionState is NavigationEventTransitionState.InProgress &&
                        transitionState.direction == NavigationEventTransitionState.TRANSITIONING_BACK
                    ) {
                        backProgress.snapTo(transitionState.latestEvent.progress)
                    }
                }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            if (enableWindowDim) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = dimProgress.value * (1f - backProgress.value)
                        }
                        .background(COUITheme.colorScheme.windowDimming),
                )
            }
            Box(
                modifier = popupModifier
                    .fillMaxSize()
                    // The window-level exit fade (`Animation.COUI.PopupListWindow.Dismiss`) belongs
                    // to the popup window's own surface rather than to its content.
                    .graphicsLayer { alpha = windowAlpha.value }
                    .onGloballyPositioned { coordinates ->
                        hostPositionInWindow = coordinates.positionInWindow()
                    }
                    // COUI's PopupWindow is non-focusable with `setOutsideTouchable(true)`, so the
                    // window keeps passing outside taps through even while it plays its exit
                    // animation; once the panel has all but faded, this container must stop
                    // swallowing them rather than wait out the rest of the close.
                    .then(
                        if (passTouches) {
                            Modifier
                        } else {
                            Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { requestDismiss() },
                                )
                            }
                        },
                    )
                    .layout { measurable, constraints ->
                        val windowBounds = layoutInfo.windowBounds
                        val popupMargin = layoutInfo.popupMargin
                        val minHeightPx = ListPopupDefaults.MinPopupHeight.roundToPx()
                        // COUI COUIPopupListWindow.getMaxMainMenuHeight() is the available rect
                        // height, i.e. the window minus insets minus the top/bottom barriers, which
                        // windowBounds already carries. ListPopupColumn's measure policy then stops
                        // accumulating rows at this bound, as COUI's measurePopupWindow does.
                        val availableHeight = (windowBounds.height - popupMargin.top - popupMargin.bottom)
                            .coerceAtLeast(minHeightPx)
                        val placeable = measurable.measure(
                            constraints.copy(
                                maxHeight = maxHeight?.roundToPx()?.coerceIn(minHeightPx, availableHeight)
                                    ?: availableHeight,
                                minHeight = if (minHeightPx <= constraints.maxHeight) minHeightPx else constraints.maxHeight,
                                maxWidth = constraints.maxWidth,
                                minWidth = minWidth.roundToPx().coerceAtMost(constraints.maxWidth),
                            ),
                        )
                        val measuredSize = IntSize(placeable.width, placeable.height)

                        val calculatedOffset = popupPositionProvider.calculatePosition(
                            anchorBounds,
                            windowBounds,
                            layoutDirection,
                            measuredSize,
                            popupMargin,
                            alignment,
                        )

                        val adjustedOffset = IntOffset(
                            x = calculatedOffset.x - hostPositionInWindow.x.toInt(),
                            y = calculatedOffset.y - hostPositionInWindow.y.toInt(),
                        )

                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeable.place(adjustedOffset)
                        }
                    },
            ) {
                ListPopupContent(
                    popupContentSize = popupContentSize,
                    onPopupContentSizeChange = { popupContentSize = it },
                    fractionProgress = { fractionProgress.value * (1f - backProgress.value) },
                    alphaProgress = { alphaProgress.value * (1f - backProgress.value) },
                    pathProgress = { pathProgress.value * (1f - backProgress.value) },
                    gravityPath = gravityPath,
                    popupLayoutPosition = layoutInfo.popupLayoutPosition,
                    localTransformOrigin = layoutInfo.localTransformOrigin,
                    enableGlass = enableGlass,
                    interactionEnabled = show && backProgress.value == 0f,
                    content = {
                        CompositionLocalProvider(LocalDismissState provides requestDismiss) {
                            content()
                        }
                    },
                )
            }
        }
    }
}
