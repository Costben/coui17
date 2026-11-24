// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.layout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.suqi8.coui.kmp.anim.folmeSpring
import io.github.suqi8.coui.kmp.basic.AppBarBlurOverlayDefaults
import io.github.suqi8.coui.kmp.basic.CapsuleOutlineShape
import io.github.suqi8.coui.kmp.basic.GradientStrokeStyle
import io.github.suqi8.coui.kmp.basic.InnerShadowStyle
import io.github.suqi8.coui.kmp.basic.MaterialShadowEdgeDefaults
import io.github.suqi8.coui.kmp.basic.MaterialStrokeParams
import io.github.suqi8.coui.kmp.basic.PopupBarrierBoundary
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.capsuleRimCornerWeight
import io.github.suqi8.coui.kmp.basic.couiGlassSurface
import io.github.suqi8.coui.kmp.basic.materialShadowEdge
import io.github.suqi8.coui.kmp.basic.materialShadowEdgeBySize
import io.github.suqi8.coui.kmp.blur.BlendColorEntry
import io.github.suqi8.coui.kmp.blur.BlurBlendMode
import io.github.suqi8.coui.kmp.blur.BlurDefaults
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.shader.RuntimeShader
import io.github.suqi8.coui.kmp.shader.asBrush
import io.github.suqi8.coui.kmp.shader.isRuntimeShaderSupported
import io.github.suqi8.coui.kmp.squircle.SquircleDefaults
import io.github.suqi8.coui.kmp.squircle.SquircleShape
import io.github.suqi8.coui.kmp.squircle.isSquircleEnabled
import io.github.suqi8.coui.kmp.squircle.squircleSurface
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.LocalDismissState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

/**
 * Internal shared layout logic for `OverlayBottomSheet` and `WindowBottomSheet`.
 *
 * @param show Whether the bottom sheet is currently shown.
 * @param backgroundColor The background color of the bottom sheet.
 * @param cornerRadius The corner radius of the top corners of the bottom sheet.
 * @param sheetMaxWidth Additional maximum-width cap applied on top of the COUI responsive
 *   grid width (full width on compact windows, a 6-column grid span on medium/expanded ones).
 * @param outsideMargin The margin outside the bottom sheet.
 * @param insideMargin The margin inside the bottom sheet.
 * @param dragHandleColor The color of the drag handle.
 * @param popupHost A composable that provides the container (e.g., DialogLayout or Dialog).
 *   It receives the visibility state and the inner content composable.
 * @param modifier The modifier to be applied to the bottom sheet content.
 * @param title Optional title centered in the top toolbar of the bottom sheet.
 * @param subtitle Optional subtitle centered below [title] in the top toolbar.
 * @param startAction Optional [Composable] to display on the start side of the toolbar.
 * @param endAction Optional [Composable] to display on the end side of the toolbar.
 * @param enableWindowDim Whether to dim the window behind the bottom sheet.
 * @param showDragHandle Whether to draw the drag handle bar at the top of the sheet. ColorOS 17
 *   panels hide it by default (`false`), keeping a slim transparent strip so the panel stays
 *   draggable while toolbar actions sit 15–16dp below the top edge.
 * @param onDismissRequest The callback when the user tries to dismiss the bottom sheet.
 * @param onDismissFinished Invoked when the hide animation completes; not invoked if the hide
 *   is cancelled mid-flight (e.g., by [show] toggling back to true).
 * @param defaultWindowInsetsPadding Whether to apply default window insets padding.
 * @param showInMaxHeight Whether the panel takes the maximum height instead of wrapping its content.
 * @param allowDismiss Whether to allow dismissing the sheet via drag or back gesture.
 * @param dismissOnClickOutside Whether tapping outside the panel dismisses it while [allowDismiss] is true.
 * @param draggable Whether the panel follows drag gestures.
 * @param enablePullUp Whether the panel can be pulled above its resting position with resistance.
 * @param enableNestedScroll Whether to enable nested scrolling for the content.
 * @param topScrollBlur Whether scrolled content fades to transparent along the top of the content
 *   viewport. Nothing is blurred or tinted: as on ColorOS 17, the body is clipped below the header.
 * @param topInset Optional top inset override. If null, calculated from window insets.
 * @param panelPaddingBottom Extra padding the panel adds under its content on top of
 *   [insideMargin.height]. None by default, as on ColorOS 17, where the platform's 60dp foot sits
 *   below the screen edge. It is dropped while the IME is up, where it would only read as dead
 *   space above the keyboard.
 * @param dismissOffsetFraction How far the panel has to be dragged down, as a fraction of its own
 *   height, before releasing it dismisses the sheet. See [BottomSheetDefaults.DismissOffsetFraction].
 * @param content The content of the bottom sheet.
 */
@Suppress("ktlint:compose:modifier-not-used-at-root")
@Composable
internal fun BottomSheetContentLayout(
    show: Boolean,
    backgroundColor: Color,
    cornerRadius: Dp,
    sheetMaxWidth: Dp,
    outsideMargin: DpSize,
    insideMargin: DpSize,
    dragHandleColor: Color,
    popupHost: @Composable (visible: Boolean, content: @Composable () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    startAction: @Composable (() -> Unit)? = null,
    endAction: @Composable (() -> Unit)? = null,
    enableWindowDim: Boolean = true,
    showDragHandle: Boolean = false,
    onDismissRequest: (() -> Unit)? = null,
    onDismissFinished: (() -> Unit)? = null,
    defaultWindowInsetsPadding: Boolean = true,
    showInMaxHeight: Boolean = false,
    allowDismiss: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    draggable: Boolean = true,
    enablePullUp: Boolean = true,
    enableNestedScroll: Boolean = true,
    topScrollBlur: Boolean = true,
    topInset: Dp? = null,
    panelPaddingBottom: Dp = BottomSheetDefaults.PanelPaddingBottom,
    dismissOffsetFraction: Float = BottomSheetDefaults.DismissOffsetFraction,
    content: @Composable () -> Unit,
) {
    val animationProgress = remember { Animatable(0f, visibilityThreshold = 0.0001f) }
    val dragOffsetY = remember { Animatable(0f) }
    val contentScrollOffsetPx = remember { mutableFloatStateOf(0f) }
    val currentOnDismissFinished by rememberUpdatedState(onDismissFinished)
    val internalVisible = remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val sheetHeightPx = remember { mutableIntStateOf(0) }

    LaunchedEffect(show) {
        // COUI samples the panel/container heights when the animation starts; mirror that
        // with a one-shot read (no recomposition subscription).
        val containerPx = windowInfo.containerSize.height.toFloat()
        val peekPx = with(density) { PanelPeekHeight.toPx() }
        val travelPx = sheetHeightPx.intValue.takeIf { it > 0 }?.toFloat() ?: containerPx
        if (show) {
            internalVisible.value = true
            dragOffsetY.snapTo(0f)
            contentScrollOffsetPx.floatValue = 0f
            animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = couiPanelSlideSpring(
                    entering = true,
                    travelPx = travelPx,
                    containerPx = containerPx,
                    peekPx = peekPx,
                ),
            )
        } else {
            if (!internalVisible.value) return@LaunchedEffect
            if (dragOffsetY.value > 0f) {
                // Sheet already dragged off-screen — snap immediately
                animationProgress.snapTo(0f)
            } else {
                // Button/back dismiss — animate normally
                animationProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = couiPanelSlideSpring(
                        entering = false,
                        travelPx = travelPx,
                        containerPx = containerPx,
                        peekPx = peekPx,
                    ),
                )
            }
            internalVisible.value = false
            currentOnDismissFinished?.invoke()
        }
    }

    if (!show && !internalVisible.value) return

    val coroutineScope = rememberCoroutineScope()
    val dimAlpha = remember { mutableFloatStateOf(1f) }
    val dragSnapChannel = remember { Channel<Float>(capacity = Channel.CONFLATED) }
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)

    val requestDismiss: () -> Unit = remember {
        { currentOnDismissRequest?.invoke() }
    }

    val resetGesture: suspend () -> Unit = {
        dragOffsetY.animateTo(0f, animationSpec = tween(durationMillis = 150))
        animate(dimAlpha.floatValue, 1f, animationSpec = tween(durationMillis = 150)) { value, _ ->
            dimAlpha.floatValue = value
        }
    }

    LaunchedEffect(dragOffsetY) {
        for (target in dragSnapChannel) dragOffsetY.snapTo(target)
    }

    popupHost(internalVisible.value) {
        val navigationEventState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationBackHandler(
            state = navigationEventState,
            isBackEnabled = show,
            onBackCancelled = { coroutineScope.launch { resetGesture() } },
            onBackCompleted = {
                if (allowDismiss) {
                    coroutineScope.launch {
                        val windowHeightPx = windowInfo.containerSize.height.toFloat()
                        animateDismissOffScreen(
                            dragOffsetY = dragOffsetY,
                            sheetHeightPx = sheetHeightPx.intValue,
                            windowHeightPx = windowHeightPx,
                            peekPx = with(density) { PanelPeekHeight.toPx() },
                            dimAlpha = dimAlpha,
                        ) {
                            requestDismiss()
                        }
                    }
                } else {
                    coroutineScope.launch { resetGesture() }
                }
            },
        )

        LaunchedEffect(allowDismiss) {
            // Collect inside a single coroutine so the per-frame `transitionState` ticks during a
            // back gesture do not cancel/relaunch the LaunchedEffect on every progress update.
            snapshotFlow { navigationEventState.transitionState }
                .collect { transitionState ->
                    if (
                        transitionState is NavigationEventTransitionState.InProgress &&
                        transitionState.direction == NavigationEventTransitionState.TRANSITIONING_BACK
                    ) {
                        val maxOffset = if (sheetHeightPx.intValue > 0) sheetHeightPx.intValue.toFloat() else 500f
                        val offset = transitionState.latestEvent.progress * maxOffset
                        val finalOffset = if (!allowDismiss) offset * 0.1f else offset
                        dragSnapChannel.trySend(finalOffset)
                        if (allowDismiss) dimAlpha.floatValue = 1f - transitionState.latestEvent.progress
                    }
                }
        }

        if (enableWindowDim) {
            // COUI drives the mask alpha with the same spring as the panel slide,
            // which multiplying by `animationProgress` reproduces here.
            val baseColor = COUITheme.colorScheme.windowDimming
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(baseColor.copy(alpha = baseColor.alpha * dimAlpha.floatValue * animationProgress.value))
                    },
            )
        }

        val outsideDismissible = allowDismiss && dismissOnClickOutside
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Key on the flags so the gesture coroutine restarts with the latest closure
                // when the consumer toggles them; otherwise the in-flight detector keeps the stale value.
                .then(
                    if (!show) {
                        Modifier
                    } else {
                        Modifier
                            .pointerInput(outsideDismissible) {
                                detectTapGestures(
                                    onTap = {
                                        if (outsideDismissible) {
                                            requestDismiss()
                                        }
                                    },
                                )
                            }
                            .semantics {
                                if (outsideDismissible) {
                                    onClick(label = "Dismiss") {
                                        requestDismiss()
                                        true
                                    }
                                }
                            }
                    },
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val sheetModifier = modifier.graphicsLayer {
                val progress = animationProgress.value
                val currentHeight = sheetHeightPx.intValue.toFloat()
                val windowHeightPx = windowInfo.containerSize.height.toFloat()
                val baseOffset = if (currentHeight > 0) currentHeight else windowHeightPx
                translationY = baseOffset * (1f - progress) + dragOffsetY.value
            }

            BottomSheetContent(
                title = title,
                subtitle = subtitle,
                backgroundColor = backgroundColor,
                cornerRadius = cornerRadius,
                sheetMaxWidth = sheetMaxWidth,
                outsideMargin = outsideMargin,
                insideMargin = insideMargin,
                defaultWindowInsetsPadding = defaultWindowInsetsPadding,
                dragHandleColor = dragHandleColor,
                showDragHandle = showDragHandle,
                showInMaxHeight = showInMaxHeight,
                allowDismiss = allowDismiss,
                draggable = draggable,
                enablePullUp = enablePullUp,
                topScrollBlur = topScrollBlur,
                sheetHeightPx = sheetHeightPx,
                dragOffsetY = dragOffsetY,
                contentScrollOffsetPx = contentScrollOffsetPx,
                dimAlpha = dimAlpha,
                dragSnapChannel = dragSnapChannel,
                onDismissRequest = {
                    if (allowDismiss) {
                        requestDismiss()
                    }
                },
                modifier = sheetModifier,
                topInset = topInset,
                panelPaddingBottom = panelPaddingBottom,
                dismissOffsetFraction = dismissOffsetFraction,
                enableNestedScroll = enableNestedScroll,
                allowTouches = show,
                startAction = startAction?.let { action ->
                    { CompositionLocalProvider(LocalDismissState provides requestDismiss) { action() } }
                },
                endAction = endAction?.let { action ->
                    { CompositionLocalProvider(LocalDismissState provides requestDismiss) { action() } }
                },
                content = {
                    CompositionLocalProvider(LocalDismissState provides requestDismiss) {
                        PopupBarrierBoundary(content)
                    }
                },
            )
        }
    }
}

@Suppress("ktlint:compose:modifier-not-used-at-root")
@Composable
internal fun BottomSheetContent(
    title: String?,
    backgroundColor: Color,
    cornerRadius: Dp,
    sheetMaxWidth: Dp,
    outsideMargin: DpSize,
    insideMargin: DpSize,
    defaultWindowInsetsPadding: Boolean,
    dragHandleColor: Color,
    allowDismiss: Boolean,
    sheetHeightPx: MutableIntState,
    dragOffsetY: Animatable<Float, *>,
    dimAlpha: MutableFloatState,
    dragSnapChannel: Channel<Float>,
    onDismissRequest: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showDragHandle: Boolean = false,
    showInMaxHeight: Boolean = false,
    draggable: Boolean = true,
    enablePullUp: Boolean = true,
    topScrollBlur: Boolean = true,
    contentScrollOffsetPx: MutableFloatState = remember { mutableFloatStateOf(0f) },
    topInset: Dp? = null,
    panelPaddingBottom: Dp = BottomSheetDefaults.PanelPaddingBottom,
    enableNestedScroll: Boolean = true,
    dismissOffsetFraction: Float = BottomSheetDefaults.DismissOffsetFraction,
    allowTouches: Boolean = true,
    startAction: @Composable (() -> Unit)? = null,
    endAction: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val windowInfo = LocalWindowInfo.current
    // `containerDpSize` converts at the platform density, so under a `LocalDensity` override (a
    // ColorOS app pinned to its system density, say) it disagrees with every other dp here and the
    // height cap lets the sheet run up under the status bar. Convert the pixels with ours instead.
    val windowHeight = with(density) { windowInfo.containerSize.height.toDp() }
    // Do NOT use windowHeight as a remember key — IME resize would recreate handlers and trigger dismiss.
    val currentWindowHeight by rememberUpdatedState(windowHeight)
    val coroutineScope = rememberCoroutineScope()

    val settlingJob = remember { mutableStateOf<Job?>(null) }
    val isSettling = remember { mutableStateOf(false) }
    val calculatedTopInset = if (topInset != null) {
        topInset
    } else {
        val statusBars = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val captionBar = WindowInsets.captionBar.asPaddingValues().calculateTopPadding()
        val displayCutout = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
        maxOf(statusBars, captionBar, displayCutout)
    }

    val calculateNewOffset = remember(allowDismiss, enablePullUp, sheetHeightPx.intValue, currentWindowHeight, density) {
        { current: Float, delta: Float ->
            val upLimitPx = if (enablePullUp) {
                pullUpLimitPx(
                    windowHeightPx = with(density) { currentWindowHeight.toPx() },
                    sheetHeightPx = sheetHeightPx.intValue.toFloat(),
                    density = density,
                )
            } else {
                0f
            }
            val newOffset = current + delta
            if (newOffset < 0) {
                // COUIBottomSheetDialog's pull-up listener drags the parent padding by 0.2 of the
                // pull, clamped to `coui_panel_pull_up_max_offset` and to the panel top.
                (current + delta * PULL_UP_FRICTION).coerceIn(-upLimitPx, 0f)
            } else if (newOffset >= 0 && !allowDismiss) {
                val dampingFactor = 0.1f
                val dampedAmount = if (delta > 0) delta * dampingFactor else delta
                (current + dampedAmount).coerceAtLeast(0f)
            } else {
                newOffset
            }
        }
    }

    val visibleSheetHeightPx: () -> Float = {
        val total = sheetHeightPx.intValue.toFloat()
        val imePx = imeInsets.getBottom(density).toFloat()
        (total - imePx).coerceAtLeast(1f)
    }

    val updateDimAlpha = remember(allowDismiss, density) {
        { offset: Float ->
            val thresholdPx = if (sheetHeightPx.intValue > 0) visibleSheetHeightPx() else 500f
            val alpha = if (offset >= 0 && allowDismiss) {
                1f - (offset / thresholdPx).coerceIn(0f, 1f)
            } else {
                1f
            }
            dimAlpha.floatValue = alpha
        }
    }

    // Settlement logic
    val performSettle: (Float) -> Unit = remember(allowDismiss, density, dismissOffsetFraction) {
        { velocity ->
            settlingJob.value?.cancel()
            isSettling.value = true
            settlingJob.value = coroutineScope.launch {
                val currentOffset = dragOffsetY.value
                val windowHeightPx = with(density) { currentWindowHeight.toPx() }
                val visiblePx = visibleSheetHeightPx()
                // COUIBottomSheetBehavior.shouldHide only releases the panel once it sits below the
                // collapsed (peek) offset — that is, once almost the whole panel has been dragged
                // off-screen — and then also requires a fling or a drag past half the screen. On a
                // tall panel the first condition is barely reachable, so the panel reads as
                // impossible to dismiss. Closing after `dismissOffsetFraction` of the panel's
                // visible height above the IME, with the 0.1s release projection counted in, is
                // what the gesture implies; a downward fling of `SIGNIFICANT_VELOCITY_PX` still
                // dismisses at any travel.
                val projected = currentOffset + velocity * VELOCITY_PROJECTION_SECONDS
                val shouldDismiss = allowDismiss && sheetHeightPx.intValue > 0 &&
                    (projected >= visiblePx * dismissOffsetFraction || velocity > SIGNIFICANT_VELOCITY_PX)

                try {
                    if (shouldDismiss) {
                        if (currentOffset >= windowHeightPx) {
                            onDismissRequest?.invoke()
                        } else {
                            animateDismissOffScreen(
                                dragOffsetY = dragOffsetY,
                                sheetHeightPx = sheetHeightPx.intValue,
                                windowHeightPx = windowHeightPx,
                                peekPx = with(density) { PanelPeekHeight.toPx() },
                                dimAlpha = dimAlpha,
                                velocity = velocity,
                            ) {
                                onDismissRequest?.invoke()
                            }
                        }
                    } else {
                        val effectiveVelocity = if (!allowDismiss && velocity > 0) 0f else velocity
                        dragOffsetY.animateTo(
                            targetValue = 0f,
                            animationSpec = folmeSpring(damping = 1f, response = 0.4f),
                            initialVelocity = effectiveVelocity,
                        ) {
                            updateDimAlpha(value)
                        }
                        dimAlpha.floatValue = 1f
                    }
                } catch (_: CancellationException) {
                    // Animation is interrupted
                } finally {
                    // Reset state after animation completes
                    isSettling.value = false
                }
            }
        }
    }

    // Nested scroll logic: always observes child scroll consumption to drive the top scroll blur
    // and fading edge, and forwards drag-to-dismiss gestures when `enableNestedScroll && draggable`.
    val nestedScrollConnection = remember(enableNestedScroll, draggable, allowDismiss, dismissOffsetFraction, density) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!enableNestedScroll || !draggable) return Offset.Zero

                // Allow interruption whenever settling
                if (isSettling.value) {
                    settlingJob.value?.cancel()
                    isSettling.value = false
                }

                val delta = available.y
                // If the sheet is offset, prioritize restoring its position
                if (delta < 0 && dragOffsetY.value > 0) {
                    val newOffset = calculateNewOffset(dragOffsetY.value, delta).coerceAtLeast(0f)
                    val consumedY = dragOffsetY.value - newOffset
                    if (consumedY != 0f) {
                        dragSnapChannel.trySend(newOffset)
                        updateDimAlpha(newOffset)
                        return Offset(0f, consumedY * -1f)
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // Track scrolled content displacement so the top progressive blur and fade ramp in
                // smoothly once the child leaves rest and clear cleanly back to 0 at the top edge.
                if (consumed.y != 0f) {
                    contentScrollOffsetPx.floatValue = (contentScrollOffsetPx.floatValue - consumed.y).coerceAtLeast(0f)
                }
                if (available.y > 0f && consumed.y >= 0f) {
                    contentScrollOffsetPx.floatValue = 0f
                }

                if (!enableNestedScroll || !draggable) return Offset.Zero

                val delta = available.y
                if (delta > 0) {
                    if (!allowDismiss) return Offset.Zero

                    if (isSettling.value) {
                        settlingJob.value?.cancel()
                        isSettling.value = false
                    }

                    val newOffset = calculateNewOffset(dragOffsetY.value, delta)
                    dragSnapChannel.trySend(newOffset)
                    updateDimAlpha(newOffset)

                    // Dismiss immediately if dragged beyond window height
                    val windowHeightPx = with(density) { currentWindowHeight.toPx() }
                    if (newOffset > windowHeightPx) {
                        performSettle(0f)
                        return available
                    }

                    return available
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!enableNestedScroll || !draggable || isSettling.value) return Velocity.Zero

                // Take over fling if the sheet is offset
                if (dragOffsetY.value > 0) {
                    performSettle(available.y)
                    return available
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (!enableNestedScroll || !draggable || isSettling.value) return Velocity.Zero

                if (dragOffsetY.value > 0) {
                    performSettle(available.y)
                    return available
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    BottomSheetColumn(
        title = title,
        subtitle = subtitle,
        startAction = startAction,
        endAction = endAction,
        backgroundColor = backgroundColor,
        cornerRadius = cornerRadius,
        sheetMaxWidth = sheetMaxWidth,
        outsideMargin = outsideMargin,
        insideMargin = insideMargin,
        defaultWindowInsetsPadding = defaultWindowInsetsPadding,
        dragHandleColor = dragHandleColor,
        showDragHandle = showDragHandle,
        showInMaxHeight = showInMaxHeight,
        allowDismiss = allowDismiss,
        draggable = draggable,
        enablePullUp = enablePullUp,
        topScrollBlur = topScrollBlur,
        windowHeight = windowHeight,
        topInset = calculatedTopInset,
        panelPaddingBottom = panelPaddingBottom,
        sheetHeightPx = sheetHeightPx,
        dragOffsetY = dragOffsetY,
        contentScrollOffsetPx = contentScrollOffsetPx,
        nestedScrollConnection = nestedScrollConnection,
        coroutineScope = coroutineScope,
        dragSnapChannel = dragSnapChannel,
        onSettle = performSettle,
        onUpdateAlpha = updateDimAlpha,
        modifier = modifier,
        allowTouches = allowTouches,
        content = content,
    )
}

@Suppress("ktlint:compose:modifier-not-used-at-root")
@Composable
private fun BottomSheetColumn(
    title: String?,
    subtitle: String?,
    startAction: (@Composable () -> Unit)?,
    endAction: (@Composable () -> Unit)?,
    backgroundColor: Color,
    cornerRadius: Dp,
    sheetMaxWidth: Dp,
    outsideMargin: DpSize,
    insideMargin: DpSize,
    defaultWindowInsetsPadding: Boolean,
    dragHandleColor: Color,
    showDragHandle: Boolean,
    showInMaxHeight: Boolean,
    allowDismiss: Boolean,
    draggable: Boolean,
    enablePullUp: Boolean,
    topScrollBlur: Boolean,
    windowHeight: Dp,
    topInset: Dp,
    panelPaddingBottom: Dp,
    sheetHeightPx: MutableIntState,
    dragOffsetY: Animatable<Float, *>,
    contentScrollOffsetPx: MutableFloatState,
    nestedScrollConnection: NestedScrollConnection,
    coroutineScope: CoroutineScope,
    dragSnapChannel: Channel<Float>,
    onSettle: (velocity: Float) -> Unit,
    onUpdateAlpha: (Float) -> Unit,
    modifier: Modifier = Modifier,
    allowTouches: Boolean = true,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    // COUI grid width plus `sheetMaxWidth` as an additional caller-supplied cap.
    val windowSize = LocalWindowInfo.current.containerDpSize
    val effectiveMaxWidth = remember(windowSize, sheetMaxWidth) {
        min(sheetMaxWidth, couiPanelMaxWidth(windowSize))
    }
    val maxSheetHeight = windowHeight - topInset
    // ColorOS 17 frosts every panel: `COUIBottomSheetDialog.initBlurParams()` feeds
    // `COUIBackgroundBlurBuilder` the two-tone recipe `coui_bottomdialog_list_mix_blur_*` +
    // `coui_bottomdialog_list_blend_blur_*` at `coui_bottomdialog_blur_radius`, and
    // `updateBlurAlphaParams()` scales the mix alpha with the reveal progress. Below Android 13 —
    // or with no backdrop — the surface falls back to the flat `squircleSurface` fill.
    val panelShape = SquircleShape(
        topStart = cornerRadius,
        topEnd = cornerRadius,
        bottomEnd = 0.dp,
        bottomStart = 0.dp,
        squircleEnabled = isSquircleEnabled(),
        weight = BottomSheetDefaults.CornerWeight,
    )
    val panelSurface = Modifier.couiGlassSurface(
        backdrop = LocalBackdrop.current,
        shape = panelShape,
        containerColor = backgroundColor,
        fallback = Modifier.squircleSurface(
            color = backgroundColor,
            topStart = cornerRadius,
            topEnd = cornerRadius,
            bottomEnd = 0.dp,
            bottomStart = 0.dp,
            weight = BottomSheetDefaults.CornerWeight,
        ),
        blurRadius = BottomSheetDefaults.blurRadius(),
        blendColors = BottomSheetDefaults.blurColors(),
    )
    val topScrollAlpha: () -> Float = remember(density, contentScrollOffsetPx) {
        { AppBarBlurOverlayDefaults.scrollAlpha(contentScrollOffsetPx.floatValue, density) }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        OverscrollBackground(
            dragOffsetY = dragOffsetY,
            sheetMaxWidth = effectiveMaxWidth,
            outsideMargin = outsideMargin,
            backgroundColor = backgroundColor,
        )

        Column(
            modifier = modifier
                .then(
                    if (allowTouches) {
                        Modifier.pointerInput(Unit) { detectTapGestures { /* Consume click */ } }
                    } else {
                        Modifier
                    },
                )
                .nestedScroll(nestedScrollConnection)
                .widthIn(max = effectiveMaxWidth)
                .fillMaxWidth()
                .then(
                    if (showInMaxHeight) {
                        Modifier.height(maxSheetHeight)
                    } else {
                        Modifier
                            .wrapContentHeight()
                            .heightIn(max = maxSheetHeight)
                    },
                )
                .onGloballyPositioned { coordinates ->
                    // Capture the full rendered sheet surface height (including any inner IME
                    // padding) so enter/exit translations slide the entire panel off-screen.
                    sheetHeightPx.intValue = coordinates.size.height
                }
                .padding(horizontal = outsideMargin.width)
                .then(panelSurface)
                // ColorOS 17 rims every panel, not just dialogs:
                // `COUIBottomSheetDialog.applyShadowEdgeParams()` installs the same
                // `EDGE_STYLE_2` gradient with `SHADOW_STYLE_2` and `weight_16_1_large`. The edge
                // has to sit inside the surface so its rim paints over the fill. The shader takes
                // a single corner radius, so the square bottom corners stay with the surface: the
                // rim's bottom arcs land outside the sheet, which is flush with the window edge.
                .materialShadowEdge(
                    stroke = BottomSheetDefaults.stroke(),
                    cornerWeight = BottomSheetDefaults.CornerWeight,
                    cornerRadius = cornerRadius,
                    edgeStyle = GradientStrokeStyle.EDGE_STYLE_2,
                    shadowStyle = InnerShadowStyle.SHADOW_STYLE_2,
                )
                // Keep the sheet surface anchored to the window bottom behind the IME and lift
                // only the inner content, avoiding a seam between the sheet edge and a rounded IME.
                .then(if (defaultWindowInsetsPadding) Modifier.imePadding() else Modifier)
                .padding(horizontal = insideMargin.width)
                .padding(
                    // An extra foot gap only exists while the keyboard is away; above the IME it
                    // would read as dead space, so it is dropped.
                    bottom = insideMargin.height +
                        if (imeInsets.getBottom(density) > 0) 0.dp else panelPaddingBottom,
                ),
        ) {
            DragHandleArea(
                dragHandleColor = dragHandleColor,
                showDragHandle = showDragHandle,
                allowDismiss = allowDismiss,
                draggable = draggable,
                pullUpLimitPx = if (enablePullUp) {
                    pullUpLimitPx(
                        windowHeightPx = with(density) { windowHeight.toPx() },
                        sheetHeightPx = sheetHeightPx.intValue.toFloat(),
                        density = density,
                    )
                } else {
                    0f
                },
                dragOffsetY = dragOffsetY,
                coroutineScope = coroutineScope,
                dragSnapChannel = dragSnapChannel,
                onSettle = onSettle,
                onUpdateAlpha = onUpdateAlpha,
            )

            if (title != null || subtitle != null || startAction != null || endAction != null) {
                TitleAndActionsRow(
                    title = title,
                    subtitle = subtitle,
                    startAction = startAction,
                    endAction = endAction,
                )
            }

            BottomSheetScrollableBody(
                topScrollBlur = topScrollBlur,
                scrollFraction = topScrollAlpha,
                modifier = if (showInMaxHeight) {
                    Modifier.weight(1f).fillMaxWidth()
                } else {
                    Modifier.weight(1f, fill = false).fillMaxWidth()
                },
                content = content,
            )
        }
    }
}

/**
 * Wraps the sheet's body with an optional transparent fading edge that ramps in as scrollable
 * content moves past the top of the viewport.
 *
 * The caller's [content] is stacked in a [Column], exactly as it was laid out directly inside the
 * sheet column before this wrapper existed, so a multi-child body never piles up at `(0, 0)`.
 *
 * ColorOS 17 never blurs or tints scrolled-under panel content: `coui_panel_layout` stacks the
 * `bottom_sheet_toolbar` (`background="@null"`) above `panel_container` in a plain vertical
 * `LinearLayout`, `COUIPanelFragment` only toggles the toolbar's button material
 * (`setIsDrawableMaterialEffect`), and the only blur `COUIBottomSheetDialog` owns is the panel's own
 * background (`COUIBackgroundBlurBuilder`). The header is therefore not floating, so the top of the
 * body viewport is always *visible content*: a blurred copy of that content (the former progressive
 * band) smeared the first child (a title, a card's lit rim) into a bright grey bar instead of
 * hiding anything. The body simply scrolls out at the header's bottom edge as on the device, and
 * [topScrollBlur] only adds a transparent fading edge of [BottomSheetDefaults.TopScrollFadeHeight]:
 * it never paints a colour of its own and lets the panel's material show through, so it reads the
 * same over a light or a dark panel.
 *
 * @param topScrollBlur Whether the scrolled content fades out along the top edge.
 * @param scrollFraction The scroll-driven fraction, `0` at rest. At `0` the body is drawn as is.
 */
@Composable
internal fun BottomSheetScrollableBody(
    topScrollBlur: Boolean,
    scrollFraction: () -> Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.then(
            if (topScrollBlur) {
                Modifier.bottomSheetTopFadeEdge(
                    scrollFraction = scrollFraction,
                    fadeLength = BottomSheetDefaults.TopScrollFadeHeight,
                )
            } else {
                Modifier
            },
        ),
    ) {
        content()
    }
}

/**
 * Top vertical fading edge driven by [scrollFraction], matching `DialogScrollFadingEdge`:
 * fades scrolled content smoothly to transparent at the top edge while keeping zero offscreen
 * layer cost at rest (`scrollFraction() <= 0f`).
 */
private fun Modifier.bottomSheetTopFadeEdge(
    scrollFraction: () -> Float,
    fadeLength: Dp,
): Modifier = this
    .graphicsLayer {
        compositingStrategy = if (scrollFraction() > 0f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
    }
    .drawWithContent {
        drawContent()
        val fraction = scrollFraction().coerceIn(0f, 1f)
        val fadePx = (fadeLength.toPx() * fraction).coerceAtMost(size.height / 2f)
        if (fadePx <= 0f) return@drawWithContent
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startY = 0f,
                endY = fadePx,
            ),
            size = Size(size.width, fadePx),
            blendMode = BlendMode.DstIn,
        )
    }

/**
 * Renders the elastic background fill that follows the sheet upward when the user
 * drags past the top of its contents (negative `dragOffsetY`). Lives in its own
 * composable so the only state it reads — `dragOffsetY.value` — invalidates this
 * subtree alone, leaving the surrounding [BottomSheetColumn] free of per-frame
 * recomposition during a drag.
 */
@Composable
private fun BoxScope.OverscrollBackground(
    dragOffsetY: Animatable<Float, *>,
    sheetMaxWidth: Dp,
    outsideMargin: DpSize,
    backgroundColor: Color,
) {
    val density = LocalDensity.current
    val overscrollOffsetPx by remember { derivedStateOf { (-dragOffsetY.value).coerceAtLeast(0f) } }
    if (overscrollOffsetPx > 0f) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = sheetMaxWidth)
                .fillMaxWidth()
                .height(with(density) { overscrollOffsetPx.toDp() } + 1.dp)
                .padding(horizontal = outsideMargin.width)
                .background(backgroundColor),
        )
    }
}

/** Easing curve matching COUIEaseInterpolator (cubic-bezier 0.33, 0, 0.67, 1). */
private val CouiEaseEasing = CubicBezierEasing(0.33f, 0f, 0.67f, 1f)

/**
 * Peek height used to normalize the panel travel fraction.
 * COUI `coui_panel_default_peek_height_in_gesture`.
 */
private val PanelPeekHeight = 82.dp

/** `coui_panel_pull_up_max_offset`: the furthest the panel can be pulled above its top edge. */
private val PanelPullUpMaxOffset = 28.dp

/** `COUIBottomSheetDialog`'s pull-up listener moves the parent padding by 0.2 of the drag. */
private const val PULL_UP_FRICTION = 0.2f

/** `COUIBottomSheetBehavior` projects the release velocity 0.1s ahead before comparing. */
private const val VELOCITY_PROJECTION_SECONDS = 0.1f

/** `COUIBottomSheetBehavior.onViewReleased`'s `f4 > 500.0f` downward-fling threshold, in px/s. */
private const val SIGNIFICANT_VELOCITY_PX = 500f

/**
 * `COUIBottomSheetDialog.getPanelPullUpListener().onDragging` clamps the pull to
 * `min(coui_panel_pull_up_max_offset, panel top)`.
 */
private fun pullUpLimitPx(windowHeightPx: Float, sheetHeightPx: Float, density: Density): Float {
    val panelTopPx = (windowHeightPx - sheetHeightPx).coerceAtLeast(0f)
    return minOf(with(density) { PanelPullUpMaxOffset.toPx() }, panelTopPx)
}

/**
 * Spring spec of the COUI panel slide, `COUIBottomSheetDialog.getTranslationResponse`: damping
 * ratio 1 and a response interpolated by the travel fraction `(travel - peek) / (container - peek)`
 * between 0.25 and 0.45 when showing, between 0.18 and 0.37 when hiding.
 */
private fun <T> couiPanelSlideSpring(
    entering: Boolean,
    travelPx: Float,
    containerPx: Float,
    peekPx: Float,
): SpringSpec<T> {
    val small = if (entering) 0.25f else 0.18f
    val large = if (entering) 0.45f else 0.37f
    val denominator = containerPx - peekPx
    val fraction = if (denominator > 0f) ((travelPx - peekPx) / denominator).coerceIn(0f, 1f) else 1f
    return folmeSpring(damping = 1f, response = small + (large - small) * fraction)
}

/**
 * Maximum panel width from the COUI responsive layout grid: full width on compact windows
 * (< 600dp), otherwise a 6-column span of the 8-column (margin 24dp, gutter 8dp) or
 * 12-column (margin 40dp, gutter 12dp) grid, chosen by the COUI window size classes.
 */
private fun couiPanelMaxWidth(windowSize: DpSize): Dp {
    val width = windowSize.width.value
    val height = windowSize.height.value
    if (width < 600f) return Dp.Infinity
    val eightColumns = width < 840f || height < 480f || (height >= 900f && height > width && width < 960f)
    return if (eightColumns) {
        ((width - 2f * 24f - 7f * 8f) / 8f * 6f + 5f * 8f).dp
    } else {
        ((width - 2f * 40f - 11f * 12f) / 12f * 6f + 5f * 12f).dp
    }
}

private fun CoroutineScope.animateHandlePressDown(pressAlpha: Animatable<Float, *>) {
    // COUIPanelPressHelper.startAnim: bgAlpha 0 -> 1, 200ms, COUIEaseInterpolator.
    launch { pressAlpha.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 200, easing = CouiEaseEasing)) }
}

private fun CoroutineScope.animateHandlePressRelease(pressAlpha: Animatable<Float, *>) {
    // COUIPanelPressHelper.endAnim: bgAlpha -> 0, 200ms, COUIEaseInterpolator.
    launch { pressAlpha.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 200, easing = CouiEaseEasing)) }
}

/**
 * Height of the transparent drag strip left in place when a panel hides its drag handle.
 * COUI panels that ship no drag view (`coui_panel_content_layout` with a toolbar only) still start
 * their toolbar one top inset below the panel edge.
 */
private val HiddenHandleStripHeight = 10.dp

/**
 * Drag handle strip at the top of the sheet, matching the COUI panel drag view: a fixed
 * capsule bar that never deforms; pressing or dragging fades in a rounded press background
 * centered behind it. Drag amounts are forwarded into [dragSnapChannel].
 *
 * Press/release fading is delegated to [animateHandlePressDown] / [animateHandlePressRelease]
 * so the same launch is not rewritten in each of the four entry points
 * (tap-press, tap-release, drag-start, drag-stop).
 */
@Composable
private fun DragHandleArea(
    dragHandleColor: Color,
    showDragHandle: Boolean,
    allowDismiss: Boolean,
    draggable: Boolean,
    pullUpLimitPx: Float,
    dragOffsetY: Animatable<Float, *>,
    coroutineScope: CoroutineScope,
    dragSnapChannel: Channel<Float>,
    onSettle: (velocity: Float) -> Unit,
    onUpdateAlpha: (Float) -> Unit,
) {
    val pressAlpha = remember { Animatable(0f) }
    val squircleEnabled = isSquircleEnabled()
    val handleShape = remember(squircleEnabled) { CapsuleOutlineShape(squircleEnabled = squircleEnabled) }
    val pressShape = remember(squircleEnabled) { SquircleShape(16.dp, squircleEnabled = squircleEnabled) }
    // COUI couiColorPress has no dedicated scheme slot; derive it from onSurface.
    val pressColor = COUITheme.colorScheme.onSurface.copy(alpha = 0.12f)

    // COUI panels without a visible handle still reserve a strip at the top, so the first content
    // row keeps the same offset from the panel edge as a handled panel.
    val stripHeight = if (showDragHandle) 24.dp else HiddenHandleStripHeight

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(stripHeight)
            .then(if (showDragHandle && draggable) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
            .then(
                if (draggable) {
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    coroutineScope.animateHandlePressDown(pressAlpha)
                                    val released = tryAwaitRelease()
                                    if (released) {
                                        coroutineScope.animateHandlePressRelease(pressAlpha)
                                    }
                                },
                            )
                        }
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { dragAmount ->
                                val newOffset = dragOffsetY.value + dragAmount
                                val finalOffset = if (newOffset < 0) {
                                    // COUIBottomSheetDialog's pull-up listener drags the parent padding by 0.2
                                    // of the pull, clamped to `coui_panel_pull_up_max_offset` and the panel top.
                                    (dragOffsetY.value + dragAmount * PULL_UP_FRICTION).coerceIn(-pullUpLimitPx, 0f)
                                } else if (newOffset >= 0 && !allowDismiss) {
                                    // Damping down if not dismissible
                                    val dampedAmount = if (dragAmount > 0) dragAmount * 0.1f else dragAmount
                                    (dragOffsetY.value + dampedAmount).coerceAtLeast(0f)
                                } else {
                                    newOffset
                                }

                                dragSnapChannel.trySend(finalOffset)
                                onUpdateAlpha(finalOffset)
                            },
                            onDragStarted = {
                                coroutineScope.animateHandlePressDown(pressAlpha)
                            },
                            onDragStopped = { velocity ->
                                coroutineScope.animateHandlePressRelease(pressAlpha)
                                // Delegate the settle logic to the shared function
                                onSettle(velocity)
                            },
                        )
                } else {
                    Modifier
                },
            )
            // COUI coui_panel_drag_view_shadow_margin_top: aligns the band center with the bar center.
            .padding(top = if (showDragHandle) 4.dp else 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (showDragHandle) {
            // COUI press feedback background, faded in/out while the handle is touched.
            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(20.dp)
                    .clip(pressShape)
                    .drawBehind {
                        drawRect(pressColor.copy(alpha = pressColor.alpha * pressAlpha.value))
                    },
            )
            // COUI drag bar: a fixed continuous-curve capsule, no press deformation.
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(handleShape)
                    .drawBehind {
                        drawRect(dragHandleColor)
                    },
            )
        }
    }
}

/** Vertical padding applied to the centered title block inside [TitleAndActionsRow]. */
private val SheetTitlePaddingVertical = 10.dp

/**
 * Single-row panel toolbar aligned with `upstream/coui17` (`Widget.COUI.Toolbar.Panel`):
 * optional [startAction] at the start, optional [endAction] at the end, and a horizontally
 * centered [title] (with optional [subtitle] stacked below it) symmetrically bounded clear of
 * both actions.
 */
@Composable
private fun TitleAndActionsRow(
    title: String?,
    subtitle: String?,
    startAction: (@Composable () -> Unit)?,
    endAction: (@Composable () -> Unit)?,
) {
    Layout(
        content = {
            Box { startAction?.invoke() }
            Box { endAction?.invoke() }
            Box {
                if (title != null || subtitle != null) {
                    Column(
                        modifier = Modifier.padding(vertical = SheetTitlePaddingVertical, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (title != null) {
                            Text(
                                text = title,
                                color = COUITheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                style = COUITheme.textStyles.title4,
                            )
                        }
                        if (subtitle != null) {
                            if (title != null) {
                                Spacer(Modifier.height(2.dp))
                            }
                            Text(
                                text = subtitle,
                                color = COUITheme.colorScheme.onSurfaceVariantSummary,
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                style = COUITheme.textStyles.footnote2,
                            )
                        }
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = BottomSheetDefaults.HeaderHeight),
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val startPlaceable = measurables[0].measure(loose)
        val endPlaceable = measurables[1].measure(loose)
        val width = if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            startPlaceable.width + endPlaceable.width
        }
        val side = maxOf(startPlaceable.width, endPlaceable.width)
        val titlePlaceable = measurables[2].measure(
            loose.copy(maxWidth = (width - 2 * side).coerceAtLeast(0)),
        )
        val height = maxOf(
            constraints.minHeight,
            startPlaceable.height,
            endPlaceable.height,
            titlePlaceable.height,
        )
        layout(width, height) {
            startPlaceable.placeRelative(0, (height - startPlaceable.height) / 2)
            endPlaceable.placeRelative(width - endPlaceable.width, (height - endPlaceable.height) / 2)
            titlePlaceable.placeRelative((width - titlePlaceable.width) / 2, (height - titlePlaceable.height) / 2)
        }
    }
}

/**
 * Animate the sheet off-screen, updating dim alpha along the way.
 * Dismisses as soon as the sheet leaves the viewport (offset >= sheetHeight).
 */
private suspend fun CoroutineScope.animateDismissOffScreen(
    dragOffsetY: Animatable<Float, *>,
    sheetHeightPx: Int,
    windowHeightPx: Float,
    peekPx: Float,
    dimAlpha: MutableFloatState,
    velocity: Float = 0f,
    onDismiss: () -> Unit,
) {
    val sheetHeight = sheetHeightPx.toFloat()
    val thresholdPx = if (sheetHeight > 0) sheetHeight else 500f
    val settleJob = launch {
        dragOffsetY.animateTo(
            targetValue = windowHeightPx,
            animationSpec = couiPanelSlideSpring(
                entering = false,
                travelPx = if (sheetHeight > 0) sheetHeight else windowHeightPx,
                containerPx = windowHeightPx,
                peekPx = peekPx,
            ),
            initialVelocity = velocity,
        ) {
            dimAlpha.floatValue = (1f - (value / thresholdPx).coerceIn(0f, 1f))
        }
    }
    snapshotFlow { dragOffsetY.value }
        .first { sheetHeight > 0 && it >= sheetHeight }
    settleJob.cancel()
    onDismiss()
}

/** `color/coui_color_controls_light` (`#29000000`), the light-mode drag handle tint. */
private val DragHandleColorLight = Color(0x29000000)

/** `color/coui_color_controls_dark` (`#40ffffff`), the dark-mode drag handle tint. */
private val DragHandleColorDark = Color(0x40FFFFFF)

object BottomSheetDefaults {

    /**
     * The default background color of the bottom sheet. COUI tints the panel with
     * `couiColorSurface`, which the `background` slot matches in both modes.
     *
     * The sheet is not filled with it unless the glass pass is unavailable; on Android 13+ the
     * surface is a blur tinted by [blurColors].
     */
    @Composable
    fun backgroundColor() = COUITheme.colorScheme.background

    /**
     * The spring `COUIBottomSheetDialog.createContentChangeSpringAnimation()` builds for a panel
     * whose content changes height: `SPRING_ANIM_CONTENT_CHANGE_RESPONSE` (0.4s), bounce 0 and
     * `SPRING_ANIM_CONTENT_CHANGE_MINIMUM_VISIBLE_CHANGE` (95). The native dialog only exposes it
     * as a factory for callers that animate their own content, so it is not applied to the sheet.
     */
    val ContentChangeSpring: SpringSpec<Float> = folmeSpring(
        damping = 1f,
        response = 0.4f,
        visibilityThreshold = 95f,
    )

    /**
     * The panel blur radius, in px as the platform declares it.
     *
     * `COUIBottomSheetDialog.initBlurParams()` reads `coui_bottomdialog_blur_radius` (400) straight
     * from `Resources.getInteger` and hands it to the blur shader, which works in device pixels.
     * [blurRadius] converts it to the dp the Compose blur takes, so the strength stays the same
     * fraction of the screen on any density.
     */
    val NativeBlurRadiusPx = 400f

    /**
     * [NativeBlurRadiusPx] expressed in dp for the current display, clamped to
     * [BlurDefaults.MaxBlurRadius].
     *
     * Full strength, like the native panel: on a ColorOS 17 device panel the flat part varies by
     * about one level over a page that varies by 23.
     */
    @Composable
    fun blurRadius(): Float {
        val density = LocalDensity.current.density
        return (NativeBlurRadiusPx / density).coerceIn(0f, BlurDefaults.MaxBlurRadius)
    }

    /**
     * The tint of the light panel over its blur.
     *
     * The native recipe is `coui_bottomdialog_list_blend_blur_light` (`#376E6E6E`) and
     * `coui_bottomdialog_list_mix_blur_light` (`#DCC8C8C8`), handed to
     * `OplusBlurParam.setMaterialParams`, which weighs them as material params rather than
     * compositing them — stacked with `SrcOver` the two greys could never reach the live panel's
     * (240,240,240). [io.github.suqi8.coui.kmp.blur.textureBlur] only has `SrcOver`, so the layer is
     * fitted to the measured output instead: a neutral grey at the native mix's 86% weight, which
     * lands on 240 over a light page and lets the blurred page tint the remaining 14%.
     */
    val BlurColorsLight: List<BlendColorEntry> = listOf(
        BlendColorEntry(color = Color(0xDCF3F3F3)),
    )

    /**
     * The tint of the dark panel over its blur: the native recipe itself, not a fit.
     *
     * `COUIBottomSheetDialog.applyBlurParams()` sets mode 2 in night mode with
     * `coui_bottomdialog_list_blend_blur_dark` (`#B30F0F0F`) and
     * `coui_bottomdialog_list_mix_blur_dark` (`#A4515151`), which BlurService's composite shader
     * reads as "luminosity + dodge" — see `DialogDefaults.BlurColorsDark`. The panel keeps the
     * page's hue at a dark luminance, so content behind it shows through as colour, and over the
     * dark page it lands on the measured (22,22,22).
     */
    val BlurColorsDark: List<BlendColorEntry> = listOf(
        BlendColorEntry(color = Color(0xB30F0F0F), mode = BlurBlendMode.Luminosity),
        BlendColorEntry(color = Color(0xA4515151), mode = BlurBlendMode.ColorDodge),
    )

    /**
     * [BlurColorsLight] or, in dark theme, [BlurColorsDark] behind a `windowDimming` layer.
     *
     * The platform blurs what lies under the panel's window, and the dim layer is one of those
     * layers, so the native recipe works on the dimmed page: over the gallery's grey dark page the
     * undimmed input lands the panel on (85,85,85) instead of the system's ~(40,40,40). Blurring then
     * dimming equals dimming then blurring, so the dim leads the layer list. The light layers are
     * fitted to the measured output, which already carries the dim, and stay as they are.
     */
    @Composable
    fun blurColors(): List<BlendColorEntry> {
        val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
        if (!isDark) return BlurColorsLight
        val dim = COUITheme.colorScheme.windowDimming
        return remember(dim) { listOf(BlendColorEntry(color = dim)) + BlurColorsDark }
    }

    /**
     * The `u_commonArray[10]` corner weight of a panel.
     *
     * `COUIBottomSheetDialog` feeds the shadow-edge shader `R.dimen.weight_16_1_large` = 3.0, and
     * the panel silhouette is drawn at the same weight, so the shader's corner geometry agrees with
     * the drawn outline.
     */
    val CornerWeight = SquircleDefaults.WeightLarge

    /**
     * The default material edge of the panel.
     *
     * `COUIBottomSheetDialog.applyShadowEdgeParams()` uses the `EDGE_STYLE_2` gradient with
     * `SHADOW_STYLE_2`, a 2dp / 0.15 rim with a 0.1 inner-shadow fade in dark mode and 4dp / 0.8
     * with 0.4 in light mode.
     */
    @Composable
    fun stroke(): MaterialStrokeParams = if (COUITheme.colorScheme.background.luminance() < 0.5f) {
        MaterialStrokeParams(
            edgeAlpha = 0.15f,
            edgeWidth = 2.dp,
            shadowFadeIn = 0.1f,
        )
    } else {
        MaterialStrokeParams(
            edgeAlpha = 0.8f,
            edgeWidth = 4.dp,
            shadowFadeIn = 0.4f,
        )
    }

    /**
     * The default color of the drag handle.
     *
     * `coui_panel_view_layout` tints `drag_img` with `panelDragViewTintColor`, which resolves to
     * `?attr/couiColorControls`: `#29000000` in light mode and `#40ffffff` in dark mode. The
     * `coui_panel_bar_view_color` of `COUIPanelBarView` (`#ccbdbdbd` at night) belongs to the
     * tiny-screen layout, which hosts that view instead of the image.
     */
    @Composable
    fun dragHandleColor(): Color {
        val dark = COUITheme.colorScheme.background.luminance() < 0.5f
        return if (dark) DragHandleColorDark else DragHandleColorLight
    }

    /**
     * The default corner radius of the top corners of the bottom sheet.
     *
     * `COUIBottomSheetDialog` resolves it in the constructor's tail: on
     * `COUIVersionUtil.getOSVersionCode() > 37` it takes
     * `R.dimen.coui_round_corner_xxxxl_radius_large_16_1` = 45dp, otherwise the 16-era
     * `R.dimen.coui_round_corner_xxxxl` = 32dp, and feeds the same value to
     * `COUIShadowEdgeDrawable.setWeight(R.dimen.weight_16_1_large)` = 3.0 plus
     * `setCornerType(2)`. The value is therefore identical to the dialog panel's, and so is
     * [CornerWeight]. The bottom corners stay square, matching the outline COUI extends
     * off-screen.
     */
    val cornerRadius = 45.dp

    /**
     * The default maximum width cap of the bottom sheet. Unbounded by default: the effective
     * width is governed by the COUI responsive layout grid (full width on compact windows,
     * a 6-column grid span on medium/expanded windows). Pass a finite value to cap it further.
     */
    val maxWidth = Dp.Infinity

    /**
     * The default margin outside the bottom sheet.
     */
    val outsideMargin = DpSize(0.dp, 0.dp)

    /**
     * The default margin inside the bottom sheet: 16dp on ColorOS 17.
     */
    val insideMargin = DpSize(16.dp, 0.dp)

    /**
     * Gap between the toolbar and the title: 80px = 3dp.
     */
    val TitleTopGap = 3.dp

    /**
     * Ink height of the 28sp title: 105px = 27dp.
     */
    val TitleHeight = 27.dp

    /**
     * Gap below the title: 29px = 12dp.
     */
    val TitleBottomGap = 12.dp

    /**
     * The toolbar button rim: `MaterialStrokeParams(edgeAlpha = 0.5f, edgeWidth = 2.dp, shadowFadeIn = 0.03f)`.
     */
    val ToolbarButtonStroke: MaterialStrokeParams = MaterialStrokeParams(
        edgeAlpha = 0.5f,
        edgeWidth = 2.dp,
        shadowFadeIn = 0.03f,
    )

    /**
     * The visible padding the panel adds under its content, on top of [insideMargin]: none.
     *
     * `COUIBottomSheetDialog.updatePaddingBottom()` does pad the sheet frame by
     * `dimen/coui_panel_padding_bottom` = 60dp on phones, but `COUIBottomSheetBehavior` places the
     * frame at `parentHeight - (height - panelPaddingBottom)` (`fitToContentsOffset`), so those
     * 60dp hang below the screen edge and the content ends flush with it. Measured on ColorOS 17,
     * the frame overruns the screen by exactly 60dp. This sheet fills a pull-up with its own
     * backdrop instead, so it has nothing to hang off-screen.
     */
    val PanelPaddingBottom = 0.dp

    /**
     * How far the panel has to be dragged down, as a fraction of its own height, before releasing
     * it dismisses the sheet.
     *
     * `COUIBottomSheetBehavior` only releases a panel that already sits below its collapsed (peek)
     * offset and then travels another half peek, which on a tall panel means dragging almost all of
     * it off-screen — the gesture reads as unable to close it. Half the panel is the point where
     * the drag stops looking like a scroll and starts looking like a dismissal.
     */
    val DismissOffsetFraction = 0.5f

    /**
     * The height of the title row, `coui_list_dialog_toolbar_height` = 50dp.
     *
     * A COUI panel's title bar is a `COUIToolbar` laid out at this fixed height, so the action
     * buttons sit on the panel's vertical centre line rather than on a padding-derived row.
     */
    val HeaderHeight = 50.dp

    /**
     * Height of the top transparent fading edge applied to scrolled sheet content.
     */
    val TopScrollFadeHeight = 20.dp

    /**
     * The side inset of the title row, `toolbar_center_menu_padding_horizontal_compat` = 16dp in
     * `values-v37`.
     */
    val HeaderHorizontalPadding = 16.dp

    /**
     * The size of a header action button, `coui_action_menu_item_min_width` = 40dp in
     * `values-v37` (it drops from the 48dp of the pre-API-37 builds).
     */
    val ActionButtonSize = 40.dp

    /**
     * The icon inside a header action button, 24dp — 60% of [ActionButtonSize], inset 8dp.
     */
    val ActionButtonIconSize = 24.dp

    /**
     * The gap between two action buttons on the same side,
     * `coui_actionbar_menuitemview_item_spacing` = 10dp in `values-v37`.
     */
    val ActionButtonSpacing = 10.dp

    /**
     * The press wash of a header action button, `?couiColorPress` = `#1F000000` in light theme.
     *
     * `ToolbarMaterialEffectDelegate` gives the item a transparent `ColorDrawable(0)` background
     * and paints this wash — as a **circle** of radius `min(w, h) / 2`, i.e. 20dp on a 40dp button —
     * only while it is pressed. A COUI toolbar action has no resting fill.
     */
    @Composable
    fun actionButtonPressColor(): Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) {
        Color(0x33FFFFFF)
    } else {
        Color(0x1F000000)
    }

    /**
     * The resting disc of a header action button, `?attr/couiColorControls` — the same circle the
     * platform paints behind a close cross (`coui_ic_toptips_close`, #29000000 light / #40FFFFFF
     * dark).
     *
     * The toolbar item itself carries no fill; on a light panel its `TYPE_FRAMEWORK_CIRCLE_1` rim is
     * a 1.9dp white line at alpha 0.2, which measures +2 of 255 against the panel and reads as no
     * circle at all. The disc is what makes the pair read as buttons.
     */
    @Composable
    fun actionButtonContainerColor(): Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) {
        Color(0x40FFFFFF)
    } else {
        Color(0x29000000)
    }
}

/**
 * A header action button of a COUI panel, i.e. the tick / cross / icon button in the toolbar.
 *
 * @param onClick Invoked when the button is clicked.
 * @param modifier The modifier to be applied to the button.
 * @param enabled Whether the button accepts input.
 * @param content The icon, usually a 24dp [androidx.compose.material3.Icon] or a COUI icon.
 */
@Composable
fun SheetActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    SheetToolbarButtonSurface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Box(
            modifier = Modifier.size(BottomSheetDefaults.ActionButtonSize),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

/**
 * The capsule close button in the panel toolbar: 40dp height, full radius.
 *
 * @param onClick Invoked when the button is clicked.
 * @param modifier The modifier to be applied to the button.
 * @param text The button label, defaults to `"关闭"`.
 * @param enabled Whether the button accepts input.
 */
@Composable
fun SheetCloseCapsule(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "关闭",
    enabled: Boolean = true,
) {
    SheetToolbarButtonSurface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Box(
            modifier = Modifier
                .height(BottomSheetDefaults.ActionButtonSize)
                // 16dp a side keeps the capsule at the native 60dp for a two-character label.
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = TrimmedTitleLineHeightStyle,
                fontSize = 14.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Medium,
                color = COUITheme.colorScheme.onBackground,
            )
        }
    }
}

/**
 * The toolbar button surface shared by [SheetActionButton] and [SheetCloseCapsule], with the
 * ColorOS 17 panel button dress: circular/capsule press wash, material stroke rim, and dithered
 * halo shadow.
 */
@Composable
internal fun SheetToolbarButtonSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
    val fill = if (isDark) Color(0x20FFFFFF) else Color.Transparent
    val halo = if (isDark) {
        Shadow(
            radius = 16.dp,
            color = Color.White,
            offset = DpOffset(0.dp, 6.dp),
            alpha = 0.05f,
        )
    } else {
        Shadow(
            radius = 16.dp,
            color = Color.Black,
            offset = DpOffset(0.dp, 6.dp),
            alpha = 0.12f,
        )
    }
    val squircleEnabled = isSquircleEnabled()
    val capsuleShape = remember(squircleEnabled) { CapsuleOutlineShape(squircleEnabled = squircleEnabled) }
    val haloShader = rememberToolbarHaloShader()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val wash = remember { Animatable(0f) }
    LaunchedEffect(pressed) {
        wash.animateTo(
            targetValue = if (pressed) 1f else 0f,
            animationSpec = folmeSpring(damping = 1f, response = 0.3f),
        )
    }
    val pressColor = BottomSheetDefaults.actionButtonPressColor()
    val currentOnClick by rememberUpdatedState(onClick)

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (haloShader != null) {
                        Modifier.ditheredHalo(haloShader, halo, continuousCurve = squircleEnabled)
                    } else {
                        Modifier
                            .drawWithContent {
                                val capsule = Path().apply {
                                    addOutline(capsuleShape.createOutline(size, layoutDirection, this@drawWithContent))
                                }
                                clipPath(capsule, ClipOp.Difference) { this@drawWithContent.drawContent() }
                            }
                            .dropShadow(capsuleShape, halo)
                    },
                ),
        )
        Box(
            modifier = Modifier
                .clip(capsuleShape)
                .background(fill)
                .drawBehind {
                    if (wash.value > 0f) {
                        drawRect(color = pressColor.copy(alpha = pressColor.alpha * wash.value))
                    }
                }
                .materialShadowEdgeBySize(
                    stroke = BottomSheetDefaults.ToolbarButtonStroke,
                    cornerWeightFor = { width, height ->
                        if (squircleEnabled) {
                            capsuleRimCornerWeight(width, height, height / 2f, squircleEnabled = true)
                        } else {
                            MaterialShadowEdgeDefaults.CapsuleCornerWeight
                        }
                    },
                    edgeStyle = GradientStrokeStyle.EDGE_STYLE_1,
                    shadowStyle = InnerShadowStyle.SHADOW_STYLE_1,
                )
                .then(
                    if (enabled) {
                        Modifier.clickable(
                            role = Role.Button,
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { currentOnClick() },
                        )
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun rememberToolbarHaloShader(): RuntimeShader? {
    if (!isRuntimeShaderSupported()) return null
    return remember { runCatching { RuntimeShader(TOOLBAR_HALO_SHADER) }.getOrNull() }
}

/** Skia's blur-radius-to-sigma factor, `1 / sqrt(3)`. */
private const val BLUR_RADIUS_TO_SIGMA = 0.57735f

private const val TOOLBAR_HALO_SHADER = """
    uniform float2 size;
    uniform float2 offset;
    uniform float sigma;
    uniform float aa;
    uniform float4 color;
    uniform float continuousCurve;

    float couiSegmentCapsuleDelta(float2 p, float2 b) {
        if (b.x <= b.y) {
            return 0.0;
        }
        float R = b.y;
        float s = b.x - abs(p.x);
        float tail = min(1.5, b.x / R);
        float s0 = 0.2250555 * R;
        float s1 = tail * R;
        if (s <= s0 || s >= s1) {
            return 0.0;
        }
        float u = (s - s0) / (s1 - s0);
        float u2_1u2 = u * u * (1.0 - u) * (1.0 - u);
        return u2_1u2 * (0.003318 + u * (1.529298 - 1.888829 * u)) * R;
    }

    float sdCapsule(float2 p, float2 halfSize) {
        float r = min(halfSize.x, halfSize.y);
        float2 q = abs(p) - halfSize + r;
        float d = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
        if (continuousCurve > 0.5) {
            d += couiSegmentCapsuleDelta(p, halfSize);
        }
        return d;
    }

    float erfApprox(float x) {
        float x2 = x * x;
        float t = 0.147 * x2;
        float v = sqrt(1.0 - exp(-x2 * (1.2732395 + t) / (1.0 + t)));
        return x < 0.0 ? -v : v;
    }

    float ign(float2 p) {
        return fract(52.9829189 * fract(dot(p, float2(0.06711056, 0.00583715))));
    }

    half4 main(float2 xy) {
        float2 halfSize = size * 0.5;
        float shadow = 0.5 - 0.5 * erfApprox(sdCapsule(xy - halfSize - offset, halfSize) / (sigma * 1.41421356));
        float outside = smoothstep(-aa, aa, sdCapsule(xy - halfSize, halfSize));
        float a = shadow * outside * color.a;
        // Dither only where the halo is, so the noise never lifts the panel around it.
        a += (ign(xy) - 0.5) * (2.0 / 255.0) * step(0.5 / 255.0, a);
        a = clamp(a, 0.0, 1.0);
        return half4(half3(color.rgb) * a, a);
    }
"""

private fun Modifier.ditheredHalo(
    shader: RuntimeShader,
    halo: Shadow,
    continuousCurve: Boolean = true,
): Modifier = drawWithCache {
    val sigma = halo.radius.toPx() * BLUR_RADIUS_TO_SIGMA
    val offsetX = halo.offset.x.toPx()
    val offsetY = halo.offset.y.toPx()
    // Three sigma past the furthest edge, where the Gaussian is below 0.2% of its peak.
    val extent = 3f * sigma + maxOf(abs(offsetX), abs(offsetY))
    shader.setFloatUniform("size", size.width, size.height)
    shader.setFloatUniform("offset", offsetX, offsetY)
    shader.setFloatUniform("sigma", sigma)
    shader.setFloatUniform("aa", 1f)
    shader.setFloatUniform("color", halo.color.red, halo.color.green, halo.color.blue, halo.alpha)
    shader.setFloatUniform("continuousCurve", if (continuousCurve) 1f else 0f)
    val brush = shader.asBrush()
    onDrawBehind {
        drawRect(
            brush = brush,
            topLeft = Offset(-extent, -extent),
            size = Size(size.width + 2f * extent, size.height + 2f * extent),
        )
    }
}

private val TrimmedTitleLineHeightStyle: TextStyle = TextStyle(
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)
