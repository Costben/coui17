// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BasicTooltipDefaults
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.squircle.SquircleDefaults
import io.github.suqi8.coui.kmp.squircle.addSquircleRoundRect
import io.github.suqi8.coui.kmp.squircle.isSquircleEnabled
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.LocalContentColor
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.jvm.JvmInline
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds

/**
 * The preferred position of a tooltip relative to its anchor. The tooltip flips to the opposite
 * side when there is not enough room on the preferred side. [Start] / [End] resolve to [Left] /
 * [Right] following the layout direction.
 */
@JvmInline
value class TooltipAnchorPosition private constructor(private val value: Int) {
    override fun toString(): String = when (this) {
        Above -> "Above"
        Below -> "Below"
        Left -> "Left"
        Right -> "Right"
        Start -> "Start"
        End -> "End"
        else -> "Invalid"
    }

    companion object {
        val Above = TooltipAnchorPosition(0)
        val Below = TooltipAnchorPosition(1)
        val Left = TooltipAnchorPosition(2)
        val Right = TooltipAnchorPosition(3)
        val Start = TooltipAnchorPosition(4)
        val End = TooltipAnchorPosition(5)
    }
}

/**
 * Receiver scope for the [TooltipBox] tooltip content ([PlainTooltip] / [RichTooltip]). It exposes
 * the resolved anchor position and the anchor bounds so a caret can be drawn pointing at the anchor.
 */
@Stable
sealed interface TooltipScope {
    /** The preferred [TooltipAnchorPosition] of the tooltip relative to its anchor. */
    val positioning: TooltipAnchorPosition

    /** The anchor bounds in window coordinates, or null before the anchor is placed. */
    fun obtainAnchorBounds(): LayoutCoordinates?
}

@Stable
internal class TooltipPlacementState {
    var resolvedPosition: TooltipAnchorPosition? by mutableStateOf(null)
    var transformOrigin: TransformOrigin? by mutableStateOf(null)
    var anchorCenterInPopup: Offset? by mutableStateOf(null)
}

private class TooltipScopeImpl(
    override val positioning: TooltipAnchorPosition,
    val placementState: TooltipPlacementState?,
    val spacingPx: Int?,
    val edgeMarginPx: Int?,
    private val anchorBounds: () -> LayoutCoordinates?,
) : TooltipScope {
    override fun obtainAnchorBounds(): LayoutCoordinates? = anchorBounds()
}

/**
 * The state that controls the visibility of a [TooltipBox].
 *
 * Created via [rememberTooltipState]. When [isPersistent] is false the tooltip is dismissed
 * automatically after a short timeout; when true it stays visible until [dismiss] is called or the
 * user taps outside. A global [MutatorMutex] keeps only one tooltip visible at a time.
 */
@Stable
interface TooltipState {
    /** The transition driving the tooltip's enter / exit animation. */
    val transition: MutableTransitionState<Boolean>

    /** Whether the tooltip is currently visible (including while animating out). */
    val isVisible: Boolean

    /** Whether the tooltip stays visible until explicitly dismissed. */
    val isPersistent: Boolean

    /** Shows the tooltip, suspending until it is dismissed or superseded by another tooltip. */
    suspend fun show(mutatePriority: MutatePriority = MutatePriority.Default)

    /** Dismisses the tooltip. */
    fun dismiss()

    /** Cleans up the coroutine backing [show] when the tooltip leaves the composition. */
    fun onDispose()
}

@OptIn(ExperimentalFoundationApi::class)
@Stable
private class TooltipStateImpl(
    initialIsVisible: Boolean,
    override val isPersistent: Boolean,
    private val mutatorMutex: MutatorMutex,
) : TooltipState {
    override val transition: MutableTransitionState<Boolean> =
        MutableTransitionState(initialIsVisible)

    override val isVisible: Boolean
        // Keep the popup composed while shown or still animating. Gating on currentState would
        // hard-cut the exit when a dismiss interrupts an unfinished enter, since currentState only
        // flips to true once the enter has settled; !isIdle keeps it through the reversal so the
        // exit can animate out.
        get() = transition.targetState || !transition.isIdle

    /** continuation used to clean up */
    private var job: (CancellableContinuation<Unit>)? = null

    /**
     * Show the tooltip associated with the current [TooltipState]. When this method is called, all
     * of the other tooltips associated with [mutatorMutex] will be dismissed.
     *
     * @param mutatePriority [MutatePriority] to be used with [mutatorMutex].
     */
    override suspend fun show(mutatePriority: MutatePriority) {
        val cancellableShow: suspend () -> Unit = {
            suspendCancellableCoroutine { continuation ->
                transition.targetState = true
                job = continuation
            }
        }

        // Show associated tooltip for [TooltipDuration] amount of time
        // or until tooltip is explicitly dismissed depending on [isPersistent].
        mutatorMutex.mutate(mutatePriority) {
            try {
                if (isPersistent || mutatePriority == MutatePriority.UserInput) {
                    cancellableShow()
                } else {
                    withTimeout(BasicTooltipDefaults.TooltipDuration.milliseconds) { cancellableShow() }
                }
            } finally {
                if (mutatePriority != MutatePriority.PreventUserInput) {
                    // timeout or cancellation has occurred and we close out the current tooltip.
                    dismiss()
                }
            }
        }
    }

    /** Dismiss the tooltip associated with this [TooltipState] if it's currently being shown. */
    override fun dismiss() {
        transition.targetState = false
        // Always cancel the active show continuation so non-persistent tooltips triggered with
        // MutatePriority.UserInput (mouse hover or touch long press) release mutatorMutex too.
        job?.cancel()
    }

    /** Cleans up [mutatorMutex] when the tooltip associated with this state leaves Composition. */
    override fun onDispose() {
        job?.cancel()
    }
}

/**
 * Creates and remembers a [TooltipState].
 *
 * @param initialIsVisible whether the tooltip is initially visible.
 * @param isPersistent whether the tooltip stays visible until explicitly dismissed.
 * @param mutatorMutex the [MutatorMutex] used to ensure only one tooltip is shown at a time.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun rememberTooltipState(
    initialIsVisible: Boolean = false,
    isPersistent: Boolean = false,
    mutatorMutex: MutatorMutex = BasicTooltipDefaults.GlobalMutatorMutex,
): TooltipState = remember(isPersistent, mutatorMutex) {
    TooltipStateImpl(initialIsVisible, isPersistent, mutatorMutex)
}

/**
 * Spring driving the tooltip's exit scale. A spring (not a tween) honors the inbound velocity left
 * by the enter spring when a dismiss interrupts an unsettled enter, so the reversal stays smooth; a
 * tween would discard that velocity and produce a stall-and-reverse hitch.
 */
private val TooltipExitScaleSpec: FiniteAnimationSpec<Float> =
    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium, visibilityThreshold = 0.0001f)

/**
 * A tooltip box that anchors a [tooltip] (a [PlainTooltip] or [RichTooltip]) to its [content].
 *
 * The tooltip is shown on hover (desktop / web) or long press (touch), or programmatically via
 * [TooltipState.show]. It is built on the foundation tooltip primitives, so triggering, the global
 * single-tooltip behavior, and the auto-dismiss timeout match the platform behavior.
 *
 * @param positionProvider the [PopupPositionProvider] positioning the tooltip, usually from
 *   [TooltipDefaults.rememberTooltipPositionProvider].
 * @param tooltip the tooltip content.
 * @param state the [TooltipState] controlling visibility.
 * @param modifier the modifier applied to the anchor wrapper.
 * @param focusable whether the tooltip is focusable; true for interactive rich tooltips so an
 *   outside tap dismisses them and their actions are reachable.
 * @param enableUserInput whether hover / long press on the anchor shows the tooltip.
 * @param content the anchor content.
 */
@Composable
fun TooltipBox(
    positionProvider: PopupPositionProvider,
    tooltip: @Composable TooltipScope.() -> Unit,
    state: TooltipState,
    modifier: Modifier = Modifier,
    focusable: Boolean = false,
    enableUserInput: Boolean = true,
    content: @Composable () -> Unit,
) {
    var anchorBounds by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var popupBounds by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val layoutDirection = LocalLayoutDirection.current
    val tooltipProvider = positionProvider as? TooltipPopupPositionProvider
    val placementState = tooltipProvider?.placementState
    val positioning = tooltipProvider?.positioning ?: TooltipAnchorPosition.Below
    val scope = remember(positioning, placementState, tooltipProvider?.spacing, tooltipProvider?.edgeMargin) {
        TooltipScopeImpl(
            positioning = positioning,
            placementState = placementState,
            spacingPx = tooltipProvider?.spacing,
            edgeMarginPx = tooltipProvider?.edgeMargin,
        ) { anchorBounds }
    }
    val resolvedSide = placementState?.resolvedPosition ?: positioning.resolve(layoutDirection)
    val transformOrigin = placementState?.transformOrigin ?: run {
        val anchor = anchorBounds
        val popup = popupBounds
        if (anchor != null && popup != null && anchor.isAttached && popup.isAttached) {
            val anchorOnWindow = anchor.positionInWindow()
            val popupOnWindow = popup.positionInWindow()
            tooltipTransformOrigin(
                side = resolvedSide,
                relativeAnchorX = anchorOnWindow.x - popupOnWindow.x + anchor.size.width / 2f,
                relativeAnchorY = anchorOnWindow.y - popupOnWindow.y + anchor.size.height / 2f,
                popupSize = popup.size,
            )
        } else {
            defaultTooltipTransformOrigin(resolvedSide)
        }
    }

    val density = LocalDensity.current
    val shadowPadHorizontalPx = if (tooltipProvider != null) {
        with(density) { TooltipShadowPaddingHorizontal.roundToPx() }
    } else {
        0
    }
    val shadowPadTopPx = if (tooltipProvider != null) {
        with(density) { TooltipShadowPaddingTop.roundToPx() }
    } else {
        0
    }
    val shadowPadBottomPx = if (tooltipProvider != null) {
        with(density) { TooltipShadowPaddingBottom.roundToPx() }
    } else {
        0
    }
    val effectivePositionProvider = remember(
        positionProvider,
        shadowPadHorizontalPx,
        shadowPadTopPx,
        shadowPadBottomPx,
    ) {
        if (tooltipProvider != null) {
            ShadowPaddedTooltipPositionProvider(
                delegate = tooltipProvider,
                padLeftPx = shadowPadHorizontalPx,
                padTopPx = shadowPadTopPx,
                padRightPx = shadowPadHorizontalPx,
                padBottomPx = shadowPadBottomPx,
            )
        } else {
            positionProvider
        }
    }

    TooltipBoxImpl(
        positionProvider = effectivePositionProvider,
        clippingEnabled = tooltipProvider == null,
        tooltip = {
            val transition = rememberTransition(state.transition, label = "TooltipTransition")
            val alpha by transition.animateFloat(
                transitionSpec = {
                    if (targetState) {
                        ListPopupDefaults.AlphaEnterAnimationSpec
                    } else {
                        ListPopupDefaults.AlphaExitAnimationSpec
                    }
                },
                label = "TooltipAlpha",
            ) { visible -> if (visible) 1f else 0f }
            val scale by transition.animateFloat(
                transitionSpec = {
                    if (targetState) {
                        ListPopupDefaults.FractionAnimationSpec
                    } else {
                        TooltipExitScaleSpec
                    }
                },
                label = "TooltipScale",
            ) { visible -> if (visible) 1f else 0.8f }

            // Expand the popup window around the bubble so PopupShadow is never cropped by the
            // Android PopupWindow surface, while keeping unshrunk constraints for TooltipSurface.
            // Opacity rides on popupOpacity (expanded saveLayer) instead of graphicsLayer.alpha so
            // the exterior drop shadow does not clip to a hard rectangle during enter/exit.
            Box(
                modifier = Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(
                        width = placeable.width + shadowPadHorizontalPx * 2,
                        height = placeable.height + shadowPadTopPx + shadowPadBottomPx,
                    ) {
                        placeable.place(shadowPadHorizontalPx, shadowPadTopPx)
                    }
                },
            ) {
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { popupBounds = it }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.transformOrigin = transformOrigin
                            clip = false
                        }
                        .popupOpacity { alpha },
                ) {
                    scope.tooltip()
                }
            }
        },
        state = state,
        modifier = modifier.onGloballyPositioned { anchorBounds = it },
        focusable = focusable,
        enableUserInput = enableUserInput,
        content = content,
    )
}

private const val TOOLTIP_LONG_PRESS_LABEL = "Show tooltip"
private const val TOOLTIP_PANE_TITLE = "Tooltip"

/**
 * Transparent padding around the tooltip bubble inside its [Popup] window so [PopupShadow]
 * (`radius = 27.85dp`, `offset.y = 6.42dp`) can diffuse naturally on Android without being cut off
 * by the PopupWindow surface bounds.
 */
private val TooltipShadowPaddingHorizontal = 30.dp
private val TooltipShadowPaddingTop = 26.dp
private val TooltipShadowPaddingBottom = 38.dp

private class ShadowPaddedTooltipPositionProvider(
    private val delegate: TooltipPopupPositionProvider,
    private val padLeftPx: Int,
    private val padTopPx: Int,
    private val padRightPx: Int,
    private val padBottomPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val innerSize = IntSize(
            width = (popupContentSize.width - padLeftPx - padRightPx).coerceAtLeast(0),
            height = (popupContentSize.height - padTopPx - padBottomPx).coerceAtLeast(0),
        )
        val innerOffset = delegate.calculatePosition(anchorBounds, windowSize, layoutDirection, innerSize)
        return IntOffset(innerOffset.x - padLeftPx, innerOffset.y - padTopPx)
    }
}

/**
 * Miuix's tooltip primitive. Forked from the experimental foundation `BasicTooltipBox` so the mouse
 * hover gesture keeps a persistent tooltip open: when the cursor leaves the anchor the tooltip is
 * dismissed only if it is not persistent. This lets the cursor travel onto a persistent [RichTooltip]
 * to reach its action, matching the Material 3 tooltip behavior.
 */
@Composable
private fun TooltipBoxImpl(
    positionProvider: PopupPositionProvider,
    clippingEnabled: Boolean,
    tooltip: @Composable () -> Unit,
    state: TooltipState,
    modifier: Modifier = Modifier,
    focusable: Boolean = false,
    enableUserInput: Boolean = true,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    Box(
        modifier = modifier
            .tooltipGestures(enableUserInput, state)
            .tooltipAnchorSemantics(enableUserInput, state, scope),
    ) {
        if (state.isVisible) {
            TooltipPopup(
                positionProvider = positionProvider,
                state = state,
                focusable = focusable,
                clippingEnabled = clippingEnabled,
                content = tooltip,
            )
        }
        content()
    }

    DisposableEffect(state) { onDispose { state.onDispose() } }
}

@Composable
private fun TooltipPopup(
    positionProvider: PopupPositionProvider,
    state: TooltipState,
    focusable: Boolean,
    clippingEnabled: Boolean,
    content: @Composable () -> Unit,
) {
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = { if (state.isVisible) state.dismiss() },
        properties = PopupProperties(
            focusable = focusable,
            clippingEnabled = clippingEnabled,
        ),
    ) {
        Box(
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Assertive
                paneTitle = TOOLTIP_PANE_TITLE
            },
        ) {
            content()
        }
    }
}

/**
 * Shows the tooltip on long press (touch / stylus) and on mouse hover. The mouse
 * [PointerEventType.Exit] branch dismisses only non-persistent tooltips, so a persistent tooltip
 * stays open while the cursor moves from the anchor onto the tooltip to reach its action.
 */
private fun Modifier.tooltipGestures(enabled: Boolean, state: TooltipState): Modifier = if (enabled) {
    this
        .pointerInput(state) {
            coroutineScope {
                awaitEachGesture {
                    val pass = PointerEventPass.Initial
                    val inputType = awaitFirstDown(pass = pass).type
                    if (inputType == PointerType.Touch || inputType == PointerType.Stylus) {
                        try {
                            withTimeout(viewConfiguration.longPressTimeoutMillis) {
                                waitForUpOrCancellation(pass = pass)
                            }
                        } catch (_: PointerEventTimeoutCancellationException) {
                            // Long press detected: show the tooltip while held and consume the
                            // rest of the gesture until release so the anchor's own click handler
                            // does not also fire.
                            launch { state.show(MutatePriority.UserInput) }
                            do {
                                val changes = awaitPointerEvent(pass = pass).changes
                                for (i in changes.indices) {
                                    changes[i].consume()
                                }
                            } while (changes.any { it.pressed })
                            // Once the finger lifts, hand a non-persistent tooltip over to the
                            // default auto-dismiss timeout instead of leaving it pinned forever.
                            if (!state.isPersistent && state.isVisible) {
                                launch { state.show(MutatePriority.Default) }
                            }
                        }
                    }
                }
            }
        }
        .pointerInput(state) {
            coroutineScope {
                awaitPointerEventScope {
                    val pass = PointerEventPass.Main
                    while (true) {
                        val event = awaitPointerEvent(pass)
                        if (event.changes[0].type == PointerType.Mouse) {
                            when (event.type) {
                                PointerEventType.Enter ->
                                    launch { state.show(MutatePriority.UserInput) }

                                PointerEventType.Exit ->
                                    if (!state.isPersistent) state.dismiss()
                            }
                        }
                    }
                }
            }
        }
} else {
    this
}

private fun Modifier.tooltipAnchorSemantics(
    enabled: Boolean,
    state: TooltipState,
    scope: CoroutineScope,
): Modifier = if (enabled) {
    this.semantics(mergeDescendants = true) {
        onLongClick(
            label = TOOLTIP_LONG_PRESS_LABEL,
            action = {
                scope.launch { state.show() }
                true
            },
        )
    }
} else {
    this
}

/**
 * A plain tooltip that briefly describes an anchor with a short text label, using the Miuix inverse
 * surface style.
 *
 * @param modifier the modifier applied to the tooltip.
 * @param caretShape the bubble indicator drawn on the side facing the anchor; defaults to the COUI
 *   arrow, `null` keeps the caret-less Miuix bubble.
 * @param maxWidth the maximum width of the tooltip.
 * @param cornerRadius the corner radius of the tooltip.
 * @param containerColor the container color of the tooltip.
 * @param contentColor the content color of the tooltip.
 * @param insideMargin the margin inside the tooltip.
 * @param content the tooltip content, usually a short [Text].
 */
@Composable
fun TooltipScope.PlainTooltip(
    modifier: Modifier = Modifier,
    caretShape: Shape? = TooltipDefaults.caretShape(),
    maxWidth: Dp = TooltipDefaults.PlainTooltipMaxWidth,
    cornerRadius: Dp = TooltipDefaults.PlainTooltipCornerRadius,
    containerColor: Color = TooltipDefaults.plainTooltipContainerColor,
    contentColor: Color = TooltipDefaults.plainTooltipContentColor,
    insideMargin: PaddingValues = TooltipDefaults.PlainTooltipInsideMargin,
    content: @Composable () -> Unit,
) {
    TooltipSurface(
        caretShape = caretShape,
        maxWidth = maxWidth,
        cornerRadius = cornerRadius,
        containerColor = containerColor,
        insideMargin = insideMargin,
        modifier = modifier.semantics(mergeDescendants = false) {
            isTraversalGroup = true
            liveRegion = LiveRegionMode.Polite
        },
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/**
 * A rich tooltip that explains an anchor with an optional [title], body [text], and an optional
 * [action], using the Miuix surface-container card style.
 *
 * @param modifier the modifier applied to the tooltip.
 * @param title the optional title shown above the body.
 * @param action the optional action shown below the body.
 * @param caretShape the bubble indicator drawn on the side facing the anchor; defaults to the COUI
 *   arrow, `null` keeps the caret-less Miuix bubble.
 * @param maxWidth the maximum width of the tooltip.
 * @param cornerRadius the corner radius of the tooltip.
 * @param colors the [RichTooltipColors] of the tooltip.
 * @param insideMargin the margin inside the tooltip.
 * @param text the body content of the tooltip.
 */
@Composable
fun TooltipScope.RichTooltip(
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    caretShape: Shape? = TooltipDefaults.caretShape(),
    maxWidth: Dp = TooltipDefaults.RichTooltipMaxWidth,
    cornerRadius: Dp = TooltipDefaults.RichTooltipCornerRadius,
    colors: RichTooltipColors = TooltipDefaults.richTooltipColors(),
    insideMargin: PaddingValues = TooltipDefaults.RichTooltipInsideMargin,
    text: @Composable () -> Unit,
) {
    TooltipSurface(
        caretShape = caretShape,
        maxWidth = maxWidth,
        cornerRadius = cornerRadius,
        containerColor = colors.containerColor,
        insideMargin = insideMargin,
        modifier = modifier.semantics(mergeDescendants = false) {
            isTraversalGroup = true
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null) {
                CompositionLocalProvider(LocalContentColor provides colors.titleContentColor) {
                    title()
                }
            }
            CompositionLocalProvider(LocalContentColor provides colors.contentColor) {
                text()
            }
            if (action != null) {
                Box(modifier = Modifier.align(Alignment.End)) {
                    CompositionLocalProvider(LocalContentColor provides colors.actionContentColor) {
                        action()
                    }
                }
            }
        }
    }
}

@Composable
private fun TooltipScope.TooltipSurface(
    caretShape: Shape?,
    maxWidth: Dp,
    cornerRadius: Dp,
    containerColor: Color,
    insideMargin: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val placementState = (this as? TooltipScopeImpl)?.placementState
    val actualPosition = placementState?.resolvedPosition ?: positioning.resolve(layoutDirection)
    val caretSide = if (caretShape != null) actualPosition.caretSide else null
    val caretPadding = TooltipDefaults.CaretPadding
    val squircleEnabled = isSquircleEnabled()
    var selfBounds by remember { mutableStateOf<LayoutCoordinates?>(null) }

    // Prefer the unscaled anchor center computed by the position provider so the caret does not
    // drift during enter/exit scale transitions; fall back to layout coordinates for custom providers.
    val anchorCenter = placementState?.anchorCenterInPopup ?: run {
        val anchor = obtainAnchorBounds()
        val self = selfBounds
        if (anchor != null && self != null && anchor.isAttached && self.isAttached) {
            val anchorOnWindow = anchor.positionInWindow()
            val selfOnWindow = self.positionInWindow()
            Offset(
                x = anchorOnWindow.x - selfOnWindow.x + anchor.size.width / 2f,
                y = anchorOnWindow.y - selfOnWindow.y + anchor.size.height / 2f,
            )
        } else {
            null
        }
    }

    val bubbleShape = remember(caretSide, caretPadding, cornerRadius, anchorCenter, squircleEnabled) {
        TooltipBubbleShape(
            caretSide = caretSide,
            caretPadding = caretPadding,
            cornerRadius = cornerRadius,
            anchorCenter = anchorCenter,
            squircleEnabled = squircleEnabled,
        )
    }

    val tooltipScopeImpl = this as? TooltipScopeImpl
    val preferredSide = positioning.resolve(layoutDirection)
    Box(
        modifier = modifier
            .layout { measurable, constraints ->
                // COUIToolTips never lets the bubble reach mBubbleHorizontalPadding from the
                // viewport, and when placed on the Start/End (Left/Right) of its anchor it caps
                // the bubble width to the room available beside the anchor (COUIToolTips.sizePopupWindow).
                val edgeMarginPx = tooltipScopeImpl?.edgeMarginPx ?: TooltipDefaults.HorizontalEdgeMargin.roundToPx()
                val spacingPx = tooltipScopeImpl?.spacingPx ?: TooltipDefaults.SpacingBetweenTooltipAndAnchor.roundToPx()
                val fullBudget = if (constraints.hasBoundedWidth) {
                    (constraints.maxWidth - edgeMarginPx * 2).coerceAtLeast(constraints.minWidth)
                } else {
                    constraints.maxWidth
                }
                val sideBudget = if (
                    constraints.hasBoundedWidth &&
                    (preferredSide == TooltipAnchorPosition.Left || preferredSide == TooltipAnchorPosition.Right)
                ) {
                    val anchor = obtainAnchorBounds()?.takeIf { it.isAttached }
                    if (anchor != null) {
                        val anchorLeft = anchor.positionInWindow().x.roundToInt()
                        val anchorRight = anchorLeft + anchor.size.width
                        val leftRoom = anchorLeft - spacingPx - edgeMarginPx
                        val rightRoom = constraints.maxWidth - anchorRight - spacingPx - edgeMarginPx
                        val preferredRoom = if (preferredSide == TooltipAnchorPosition.Left) leftRoom else rightRoom
                        val oppositeRoom = if (preferredSide == TooltipAnchorPosition.Left) rightRoom else leftRoom
                        val minSidePx = min(maxWidth.roundToPx(), TooltipMinSideWidth.roundToPx())
                        when {
                            preferredRoom >= minSidePx -> preferredRoom
                            oppositeRoom >= minSidePx -> oppositeRoom
                            else -> null
                        }
                    } else {
                        null
                    }
                } else {
                    null
                }
                val boundedMaxWidth = if (constraints.hasBoundedWidth) {
                    val cap = if (sideBudget != null) min(fullBudget, sideBudget) else fullBudget
                    cap.coerceAtLeast(constraints.minWidth)
                } else {
                    constraints.maxWidth
                }
                val placeable = measurable.measure(
                    if (constraints.hasBoundedWidth) {
                        constraints.copy(maxWidth = boundedMaxWidth)
                    } else {
                        constraints
                    },
                )
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
            .widthIn(max = maxWidth)
            .onGloballyPositioned { selfBounds = it }
            .dropShadow(
                shape = bubbleShape,
                shadow = PopupShadow,
            )
            .background(
                color = containerColor,
                shape = bubbleShape,
            )
            .absolutePadding(
                left = if (caretSide == TooltipCaretSide.Left) caretPadding else 0.dp,
                top = if (caretSide == TooltipCaretSide.Top) caretPadding else 0.dp,
                right = if (caretSide == TooltipCaretSide.Right) caretPadding else 0.dp,
                bottom = if (caretSide == TooltipCaretSide.Bottom) caretPadding else 0.dp,
            )
            .padding(insideMargin),
    ) {
        content()
    }
}

/**
 * A plain tooltip convenience that wraps [TooltipBox] + [PlainTooltip] for a short text label.
 *
 * @param text the text label of the tooltip.
 * @param modifier the modifier applied to the anchor wrapper.
 * @param state the [TooltipState] controlling visibility.
 * @param enabled whether hover / long press on the anchor shows the tooltip.
 * @param positioning the preferred [TooltipAnchorPosition] of the tooltip.
 * @param containerColor the container color of the tooltip.
 * @param contentColor the content color of the tooltip.
 * @param content the anchor content.
 */
@Composable
fun TooltipBox(
    text: String,
    modifier: Modifier = Modifier,
    state: TooltipState = rememberTooltipState(isPersistent = false),
    enabled: Boolean = true,
    positioning: TooltipAnchorPosition = TooltipAnchorPosition.Below,
    containerColor: Color = TooltipDefaults.plainTooltipContainerColor,
    contentColor: Color = TooltipDefaults.plainTooltipContentColor,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning),
        tooltip = {
            PlainTooltip(containerColor = containerColor, contentColor = contentColor) {
                Text(
                    text = text,
                    color = contentColor,
                    // COUIDefaultBubbleStyleImpl sizes the label with
                    // `tool_tips_content_text_size` (13sp) over `couiTextBodyS`.
                    style = COUITheme.textStyles.footnote1,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        state = state,
        modifier = modifier,
        focusable = false,
        enableUserInput = enabled,
        content = content,
    )
}

/**
 * A rich tooltip convenience that wraps [TooltipBox] + [RichTooltip] with an optional title and a
 * single text action. The action invokes [onActionClick] then dismisses the tooltip.
 *
 * The tooltip is persistent: trigger it via long press / hover, or hoist [state] and call
 * [TooltipState.show] from the anchor's own click. It is dismissed by an outside tap, a back press,
 * or the action.
 *
 * @param text the body text of the tooltip.
 * @param modifier the modifier applied to the anchor wrapper.
 * @param state the [TooltipState] controlling visibility.
 * @param title the optional title shown above the body.
 * @param actionText the optional action label.
 * @param onActionClick called when the action is clicked, before the tooltip is dismissed.
 * @param enabled whether hover / long press on the anchor shows the tooltip.
 * @param positioning the preferred [TooltipAnchorPosition] of the tooltip.
 * @param colors the [RichTooltipColors] of the tooltip.
 * @param content the anchor content.
 */
@OptIn(ExperimentalCOUIApi::class)
@Composable
fun RichTooltipBox(
    text: String,
    modifier: Modifier = Modifier,
    state: TooltipState = rememberTooltipState(isPersistent = true),
    title: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    positioning: TooltipAnchorPosition = TooltipAnchorPosition.Below,
    colors: RichTooltipColors = TooltipDefaults.richTooltipColors(),
    content: @Composable () -> Unit,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(positioning),
        tooltip = {
            RichTooltip(
                title = title?.let {
                    {
                        Text(
                            text = it,
                            color = colors.titleContentColor,
                            // `tv_title` in coui_tool_tips_icon_style_layout is couiTextHeadlineXS.
                            style = COUITheme.textStyles.headline1,
                        )
                    }
                },
                action = actionText?.let {
                    {
                        TextButton(
                            text = it,
                            onClick = {
                                onActionClick?.invoke()
                                state.dismiss()
                            },
                            minWidth = 0.dp,
                            minHeight = 0.dp,
                            cornerRadius = TooltipDefaults.RichTooltipActionCornerRadius,
                            colors = ButtonDefaults.textButtonColors(
                                color = Color.Transparent,
                                textColor = colors.actionContentColor,
                            ),
                            insideMargin = TooltipDefaults.RichTooltipActionInsideMargin,
                        )
                    }
                },
                colors = colors,
            ) {
                Text(
                    text = text,
                    color = colors.contentColor,
                    style = COUITheme.textStyles.footnote1,
                )
            }
        },
        state = state,
        modifier = modifier,
        focusable = true,
        enableUserInput = enabled,
        content = content,
    )
}

/**
 * Colors for [RichTooltip].
 *
 * @param containerColor container color of the rich tooltip.
 * @param contentColor color of the rich tooltip body text.
 * @param titleContentColor color of the rich tooltip title.
 * @param actionContentColor color of the rich tooltip action label.
 */
@Immutable
data class RichTooltipColors(
    val containerColor: Color,
    val contentColor: Color,
    val titleContentColor: Color,
    val actionContentColor: Color,
)

/**
 * Positions a tooltip on the preferred side of its anchor, centered on the cross axis, flipping to
 * the opposite side when there is no room and clamped to the window bounds.
 */
@Stable
internal class TooltipPopupPositionProvider(
    val positioning: TooltipAnchorPosition,
    internal val spacing: Int,
    internal val edgeMargin: Int,
    internal val placementState: TooltipPlacementState = TooltipPlacementState(),
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val resolved = positioning.resolve(layoutDirection)
        val centerX = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        val centerY = anchorBounds.top + (anchorBounds.height - popupContentSize.height) / 2
        // COUIToolTips keeps the bubble mBubbleHorizontalPadding away from both side edges.
        val minX = min(edgeMargin, max(0, windowSize.width - popupContentSize.width) / 2)
        val maxX = max(minX, windowSize.width - popupContentSize.width - minX)

        var x = centerX
        var y = centerY
        val actualSide: TooltipAnchorPosition

        fun placeVertical(preferAbove: Boolean): TooltipAnchorPosition {
            val below = anchorBounds.bottom + spacing
            val above = anchorBounds.top - popupContentSize.height - spacing
            return if (preferAbove) {
                if (above >= 0 || below + popupContentSize.height > windowSize.height) {
                    y = above
                    TooltipAnchorPosition.Above
                } else {
                    y = below
                    TooltipAnchorPosition.Below
                }
            } else {
                if (below + popupContentSize.height <= windowSize.height || above < 0) {
                    y = below
                    TooltipAnchorPosition.Below
                } else {
                    y = above
                    TooltipAnchorPosition.Above
                }
            }
        }

        when (resolved) {
            TooltipAnchorPosition.Below -> {
                actualSide = placeVertical(preferAbove = false)
            }

            TooltipAnchorPosition.Above -> {
                actualSide = placeVertical(preferAbove = true)
            }

            TooltipAnchorPosition.Left -> {
                val left = anchorBounds.left - popupContentSize.width - spacing
                val right = anchorBounds.right + spacing
                when {
                    left >= minX -> {
                        x = left
                        actualSide = TooltipAnchorPosition.Left
                    }

                    right <= maxX -> {
                        x = right
                        actualSide = TooltipAnchorPosition.Right
                    }

                    else -> {
                        // Neither side fits within the viewport margins without overlapping the
                        // anchor; fall back vertically so the caret still points directly at it.
                        actualSide = placeVertical(preferAbove = false)
                    }
                }
            }

            TooltipAnchorPosition.Right -> {
                val right = anchorBounds.right + spacing
                val left = anchorBounds.left - popupContentSize.width - spacing
                when {
                    right <= maxX -> {
                        x = right
                        actualSide = TooltipAnchorPosition.Right
                    }

                    left >= minX -> {
                        x = left
                        actualSide = TooltipAnchorPosition.Left
                    }

                    else -> {
                        actualSide = placeVertical(preferAbove = false)
                    }
                }
            }

            else -> {
                actualSide = placeVertical(preferAbove = false)
            }
        }

        x = x.coerceIn(minX, maxX)
        y = y.coerceIn(0, max(0, windowSize.height - popupContentSize.height))

        val anchorCenterX = anchorBounds.left + anchorBounds.width / 2f
        val anchorCenterY = anchorBounds.top + anchorBounds.height / 2f
        val relativeAnchorX = anchorCenterX - x
        val relativeAnchorY = anchorCenterY - y
        val pivot = tooltipTransformOrigin(
            side = actualSide,
            relativeAnchorX = relativeAnchorX,
            relativeAnchorY = relativeAnchorY,
            popupSize = popupContentSize,
        )
        if (placementState.resolvedPosition != actualSide) {
            placementState.resolvedPosition = actualSide
        }
        if (placementState.transformOrigin != pivot) {
            placementState.transformOrigin = pivot
        }
        val anchorCenter = Offset(relativeAnchorX, relativeAnchorY)
        if (placementState.anchorCenterInPopup != anchorCenter) {
            placementState.anchorCenterInPopup = anchorCenter
        }
        return IntOffset(x, y)
    }
}

/**
 * Computes the scale pivot on the tooltip's facing edge pointing directly at the anchor center,
 * mirroring `COUIToolTips.calculatePivot` so enter and exit animations grow from and shrink back
 * into the anchor/caret tip instead of the screen or bubble center.
 */
internal fun tooltipTransformOrigin(
    side: TooltipAnchorPosition,
    relativeAnchorX: Float,
    relativeAnchorY: Float,
    popupSize: IntSize,
): TransformOrigin {
    val w = popupSize.width.toFloat()
    val h = popupSize.height.toFloat()
    return when (side) {
        TooltipAnchorPosition.Below -> TransformOrigin(
            pivotFractionX = if (w > 0f) (relativeAnchorX / w).coerceIn(0f, 1f) else 0.5f,
            pivotFractionY = 0f,
        )

        TooltipAnchorPosition.Above -> TransformOrigin(
            pivotFractionX = if (w > 0f) (relativeAnchorX / w).coerceIn(0f, 1f) else 0.5f,
            pivotFractionY = 1f,
        )

        TooltipAnchorPosition.Left -> TransformOrigin(
            pivotFractionX = 1f,
            pivotFractionY = if (h > 0f) (relativeAnchorY / h).coerceIn(0f, 1f) else 0.5f,
        )

        TooltipAnchorPosition.Right -> TransformOrigin(
            pivotFractionX = 0f,
            pivotFractionY = if (h > 0f) (relativeAnchorY / h).coerceIn(0f, 1f) else 0.5f,
        )

        else -> TransformOrigin(
            pivotFractionX = if (w > 0f) (relativeAnchorX / w).coerceIn(0f, 1f) else 0.5f,
            pivotFractionY = 0f,
        )
    }
}

private fun defaultTooltipTransformOrigin(side: TooltipAnchorPosition): TransformOrigin = when (side) {
    TooltipAnchorPosition.Below -> TransformOrigin(0.5f, 0f)
    TooltipAnchorPosition.Above -> TransformOrigin(0.5f, 1f)
    TooltipAnchorPosition.Left -> TransformOrigin(1f, 0.5f)
    TooltipAnchorPosition.Right -> TransformOrigin(0f, 0.5f)
    else -> TransformOrigin(0.5f, 0f)
}

private fun TooltipAnchorPosition.resolve(layoutDirection: LayoutDirection): TooltipAnchorPosition = when (this) {
    TooltipAnchorPosition.Start ->
        if (layoutDirection == LayoutDirection.Ltr) TooltipAnchorPosition.Left else TooltipAnchorPosition.Right

    TooltipAnchorPosition.End ->
        if (layoutDirection == LayoutDirection.Ltr) TooltipAnchorPosition.Right else TooltipAnchorPosition.Left

    else -> this
}

/**
 * Defaults for [TooltipBox], [PlainTooltip] and [RichTooltip].
 */
object TooltipDefaults {
    /** The default spacing between a tooltip and its anchor. */
    val SpacingBetweenTooltipAndAnchor = 8.dp

    /**
     * The minimum gap between a tooltip and the left and right window edges. COUI
     * `coui_tooltips_bubble_horizontal_padding` (`COUIToolTips.mBubbleHorizontalPadding`).
     */
    val HorizontalEdgeMargin = 16.dp

    /**
     * The size of the Miuix triangle caret. COUI's own bubble indicator is the `_sharp_` arrow
     * drawable described by [CaretWidth] / [CaretHeight], so this value is only kept for callers
     * that draw the legacy triangle themselves.
     */
    val caretSize = DpSize(16.dp, 8.dp)

    /**
     * The width of `coui_white_sharp_middle_arrow_up`, the centred bubble indicator.
     *
     * `COUIToolTips` inflates the drawable into an `ImageView` placed on the side of the bubble
     * facing the anchor, so the arrow is 20dp wide and the bubble's own edge is [CaretEmbedDistance]
     * inside it.
     */
    val CaretWidth = 20.dp

    /** The height of `coui_white_sharp_middle_arrow_up`. */
    val CaretHeight = 10.1.dp

    /**
     * The width of `coui_white_sharp_edges_arrow_up_right`, the indicator used when the anchor is
     * too close to the end of the bubble for the centred arrow to fit.
     */
    val CaretEdgeWidth = 16.27.dp

    /** The height of `coui_white_sharp_edges_arrow_up_right`. */
    val CaretEdgeHeight = 10.29.dp

    /**
     * How far the indicator is pushed back into the bubble, `coui_tooltips_indicator_embed_distance`.
     * The arrow is drawn on top of the bubble edge so the two silhouettes merge without a seam.
     */
    val CaretEmbedDistance = 0.33.dp

    /** How far the bubble is inset from the indicator, i.e. the tallest arrow minus the overlap. */
    val CaretPadding = maxOf(CaretHeight, CaretEdgeHeight) - CaretEmbedDistance

    /**
     * `coui_tooltips_edge_extra_distance`: when the edge drawable is pushed to the end of the
     * bubble it keeps this much clearance from the silhouette.
     */
    val CaretEdgeExtraDistance = 2.dp

    /** The default maximum width of a plain tooltip. */
    val PlainTooltipMaxWidth = 200.dp

    /**
     * The default corner radius of a plain tooltip.
     *
     * `COUIToolTips` draws the bubble with
     * `OplusOutlineAdapter.setSmoothRoundRect(rect, coui_tooltips_os_16_1_radius_17_dp, 3.0f)` on
     * ColorOS 17, i.e. 17dp on the Lp(3.0) smooth curve. The system has one bubble radius, which
     * [RichTooltipCornerRadius] shares.
     */
    val PlainTooltipCornerRadius = 17.dp

    /**
     * The default margin inside a plain tooltip.
     *
     * `COUIDefaultBubbleStyleImpl` insets the content `ScrollView` from the bubble by
     * `couiToolTipsContainerLayoutMarginStart` / `_MarginTop` / `_MarginBottom` (12dp each), so the
     * label sits 12dp from every edge the dismiss icon leaves free. The 13dp
     * `coui_tooltips_default_background_padding_*` values are the `<inset>` of
     * `drawable/coui_tool_tips_background`, i.e. the bubble's own outset from the panel, not content
     * padding.
     */
    val PlainTooltipInsideMargin = PaddingValues(all = 12.dp)

    /**
     * The default container color of a plain tooltip. `style/COUIToolTips` fills the bubble with
     * `?attr/couiColorTips`, which resolves to `color/coui_color_tips` (`#0080FF`, night `#1A8CFF`),
     * the same blue as the default [primary][io.github.suqi8.coui.kmp.theme.Colors.primary].
     */
    val plainTooltipContainerColor: Color
        @Composable get() = COUITheme.colorScheme.primary

    /** The default content color of a plain tooltip. `style/COUIToolTips` uses `coui_color_white`. */
    val plainTooltipContentColor: Color
        @Composable get() = COUITheme.colorScheme.onPrimary

    /**
     * The default maximum width of a rich tooltip, matching ColorOS 17's structured tooltip width
     * (`tool_tips_image_style_max_width` = 260dp) so the bubble stays compact instead of spanning
     * the full width of a 360dp phone screen.
     */
    val RichTooltipMaxWidth = 260.dp

    /**
     * The default corner radius of a rich tooltip. `COUIToolTips` has a single bubble radius, so
     * this is the same 17dp / weight 3.0 as [PlainTooltipCornerRadius].
     */
    val RichTooltipCornerRadius = 17.dp

    /** The default margin inside a rich tooltip. */
    val RichTooltipInsideMargin = PaddingValues(all = 16.dp)

    /** The default corner radius of a rich tooltip action button. */
    val RichTooltipActionCornerRadius = 8.dp

    /** The default margin inside a rich tooltip action button. */
    val RichTooltipActionInsideMargin = PaddingValues(horizontal = 12.dp, vertical = 6.dp)

    /**
     * Returns the bubble indicator shape. `COUIToolTips` always draws an arrow on the side facing
     * the anchor (`show` / `showWithDirection` default `hasIndicator` to true), so this is the
     * default for `caretShape`. Passing `null` keeps the caret-less Miuix bubble.
     */
    fun caretShape(): Shape = TooltipCaretShape

    @Composable
    fun richTooltipColors(
        containerColor: Color = COUITheme.colorScheme.surfaceContainer,
        contentColor: Color = COUITheme.colorScheme.onSurfaceContainerVariant,
        titleContentColor: Color = COUITheme.colorScheme.onSurfaceContainer,
        actionContentColor: Color = COUITheme.colorScheme.primary,
    ): RichTooltipColors = remember(containerColor, contentColor, titleContentColor, actionContentColor) {
        RichTooltipColors(
            containerColor = containerColor,
            contentColor = contentColor,
            titleContentColor = titleContentColor,
            actionContentColor = actionContentColor,
        )
    }

    /**
     * Returns a [PopupPositionProvider] that positions a tooltip on the [positioning] side of its
     * anchor.
     *
     * @param positioning the preferred [TooltipAnchorPosition].
     * @param spacingBetweenTooltipAndAnchor the spacing between the tooltip and the anchor.
     */
    @Composable
    fun rememberTooltipPositionProvider(
        positioning: TooltipAnchorPosition = TooltipAnchorPosition.Below,
        spacingBetweenTooltipAndAnchor: Dp = SpacingBetweenTooltipAndAnchor,
    ): PopupPositionProvider {
        val density = LocalDensity.current
        val spacingPx = with(density) { spacingBetweenTooltipAndAnchor.roundToPx() }
        val edgeMarginPx = with(density) { HorizontalEdgeMargin.roundToPx() }
        return remember(positioning, spacingPx, edgeMarginPx) {
            TooltipPopupPositionProvider(positioning, spacingPx, edgeMarginPx)
        }
    }
}

private object TooltipCaretShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = Path()
        val points = TooltipCaretGeometry.outline(edge = false, mirror = false)
        val sx = size.width / TooltipCaretGeometry.MIDDLE_WIDTH
        val sy = size.height / TooltipCaretGeometry.MIDDLE_HEIGHT
        points.forEachIndexed { index, point ->
            val x = point.x * sx
            val y = point.y * sy
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/** Which side of the bubble the indicator protrudes from. */
private enum class TooltipCaretSide { Top, Right, Bottom, Left }

private val TooltipAnchorPosition.caretSide: TooltipCaretSide
    get() = when (this) {
        TooltipAnchorPosition.Above -> TooltipCaretSide.Bottom
        TooltipAnchorPosition.Below -> TooltipCaretSide.Top
        TooltipAnchorPosition.Left -> TooltipCaretSide.Right
        else -> TooltipCaretSide.Left
    }

/**
 * Outline of the `COUIToolTips` bubble indicator, in dp, pointing up with its base on
 * `y = thickness`.
 *
 * The two system drawables are `coui_*_sharp_middle_arrow_up` (20 × 10.1dp) and
 * `coui_*_sharp_edges_arrow_up_right` (16.27 × 10.29dp). Both start with a shallow arc that lifts
 * the foot of the arrow off the bubble edge so it follows the 17dp bubble corner instead of
 * crossing it, then run a straight flank into a 2dp rounded tip. Mirroring is applied to `x` only;
 * the arcs are traced in the same parameter order, which reproduces the reflected sweep.
 */
internal object TooltipCaretGeometry {
    const val MIDDLE_WIDTH = 20f
    const val MIDDLE_HEIGHT = 10.1f
    const val EDGE_WIDTH = 16.27f
    const val EDGE_HEIGHT = 10.29f

    /** The apex of the centred arrow, measured from its left edge. */
    const val MIDDLE_APEX = 10f

    /** The apex of the edge arrow, measured from its left (walled) edge. */
    const val EDGE_APEX = 14.27f

    fun outline(edge: Boolean, mirror: Boolean): List<Offset> {
        val width = if (edge) EDGE_WIDTH else MIDDLE_WIDTH
        fun x(value: Float) = if (mirror) width - value else value
        val points = ArrayList<Offset>(80)
        fun arc(cx: Float, cy: Float, radius: Float, start: Float, sweep: Float, steps: Int) {
            for (step in 0..steps) {
                val degrees = start + sweep * step / steps
                val radians = degrees * PI / 180.0
                points += Offset(
                    x(cx + radius * cos(radians).toFloat()),
                    cy + radius * sin(radians).toFloat(),
                )
            }
        }
        if (edge) {
            arc(-1.775f, 2.570f, 7.92f, 77.05f, -27.37f, 10)
            points += Offset(x(13f), 0.48f)
            arc(14.270f, 2.024f, 2f, 230.56f, 128.75f, 12)
            points += Offset(x(EDGE_WIDTH), EDGE_HEIGHT)
        } else {
            arc(0.003f, 4.910f, 5.19f, 90.033f, -60.09f, 12)
            points += Offset(x(8.27f), 1f)
            arc(10f, 2f, 2f, 210f, 120f, 12)
            points += Offset(x(15.5f), 7.5f)
            arc(19.997f, 4.910f, 5.19f, 150.06f, -60.09f, 12)
        }
        return points
    }
}

/**
 * Unified bubble-plus-caret outline: combines the smooth-corner squircle bubble body and the
 * `COUIToolTips` arrow indicator via [PathOperation.Union] so fill and drop shadow form one
 * seamless silhouette with zero gap across all 4 directions and bubble sizes.
 */
@Immutable
private class TooltipBubbleShape(
    private val caretSide: TooltipCaretSide?,
    private val caretPadding: Dp,
    private val cornerRadius: Dp,
    private val anchorCenter: Offset?,
    private val squircleEnabled: Boolean,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val caretPaddingPx = if (caretSide != null) with(density) { caretPadding.toPx() } else 0f
        val bubbleRect = Rect(
            left = if (caretSide == TooltipCaretSide.Left) caretPaddingPx else 0f,
            top = if (caretSide == TooltipCaretSide.Top) caretPaddingPx else 0f,
            right = size.width - if (caretSide == TooltipCaretSide.Right) caretPaddingPx else 0f,
            bottom = size.height - if (caretSide == TooltipCaretSide.Bottom) caretPaddingPx else 0f,
        )
        val cornerRadiusPx = with(density) { cornerRadius.toPx() }
        val bodyPath = Path()
        if (bubbleRect.width > 0f && bubbleRect.height > 0f) {
            bodyPath.addSquircleRoundRect(
                rect = bubbleRect,
                topLeft = cornerRadiusPx,
                topRight = cornerRadiusPx,
                bottomRight = cornerRadiusPx,
                bottomLeft = cornerRadiusPx,
                squircleEnabled = squircleEnabled,
                weight = SquircleDefaults.WeightLarge,
            )
        }
        if (caretSide == null || bubbleRect.width <= 0f || bubbleRect.height <= 0f) {
            return Outline.Generic(bodyPath)
        }
        val anchor = anchorCenter ?: bubbleRect.center
        val caretPath = buildTooltipCaretPath(
            side = caretSide,
            size = size,
            bubbleRect = bubbleRect,
            caretPaddingPx = caretPaddingPx,
            anchorX = anchor.x,
            anchorY = anchor.y,
            cornerRadiusPx = cornerRadiusPx,
            density = density,
        )
        return Outline.Generic(Path.combine(PathOperation.Union, bodyPath, caretPath))
    }

    override fun equals(other: Any?): Boolean = other is TooltipBubbleShape &&
        other.caretSide == caretSide &&
        other.caretPadding == caretPadding &&
        other.cornerRadius == cornerRadius &&
        other.anchorCenter == anchorCenter &&
        other.squircleEnabled == squircleEnabled

    override fun hashCode(): Int {
        var result = caretSide?.hashCode() ?: 0
        result = 31 * result + caretPadding.hashCode()
        result = 31 * result + cornerRadius.hashCode()
        result = 31 * result + (anchorCenter?.hashCode() ?: 0)
        result = 31 * result + squircleEnabled.hashCode()
        return result
    }
}

/**
 * COUI `couiRoundCornerMRadius` (9dp): corner clearance kept before the edge arrow takes over,
 * ensuring the arrow base never floats over the steep outer portion of the 17dp bubble corner.
 */
private val TooltipArrowCornerThreshold = 9.dp

/**
 * Minimum horizontal space beside an anchor required before [TooltipSurface] constrains a wide
 * Start/End tooltip into that side slot; narrower slots fall back vertically to Above/Below.
 */
private val TooltipMinSideWidth = 120.dp

/**
 * Builds the `COUIToolTips` bubble indicator path fused into [bubbleRect].
 *
 * Narrow bubbles (e.g., short plain tooltips or compact left/right tooltips) keep the centred
 * arrow rather than forcing an edge arrow onto the rounded corner; wide bubbles switch to the edge
 * arrow near the ends while keeping `TooltipArrowCornerThreshold + CaretEdgeExtraDistance` clear
 * of the bubble end. Any residual corner curvature under the arrow's feet is absorbed into the
 * embed depth plus a short internal base skirt so the union with the squircle body never leaves a
 * gap.
 */
private fun buildTooltipCaretPath(
    side: TooltipCaretSide,
    size: Size,
    bubbleRect: Rect,
    caretPaddingPx: Float,
    anchorX: Float,
    anchorY: Float,
    cornerRadiusPx: Float,
    density: Density,
): Path {
    val vertical = side == TooltipCaretSide.Top || side == TooltipCaretSide.Bottom
    val span = if (vertical) bubbleRect.width else bubbleRect.height
    val crossSpan = if (vertical) bubbleRect.height else bubbleRect.width
    val anchor = if (vertical) anchorX - bubbleRect.left else anchorY - bubbleRect.top

    val effectiveRadius = cornerRadiusPx.coerceAtMost(min(bubbleRect.width, bubbleRect.height) * 0.5f)
    val middleWidth = with(density) { TooltipDefaults.CaretWidth.toPx() }
    val edgeWidth = with(density) { TooltipDefaults.CaretEdgeWidth.toPx() }
    val middleApex = TooltipCaretGeometry.MIDDLE_APEX * density.density

    val startLimit = effectiveRadius + middleWidth / 2f
    val endLimit = span - effectiveRadius - middleWidth / 2f
    val canFitFlatMiddle = startLimit <= endLimit
    val edge = canFitFlatMiddle && (anchor < startLimit || anchor > endLimit)
    val mirror = edge && anchor < span / 2f
    val length = if (edge) edgeWidth else middleWidth
    val arrowHeight = (if (edge) TooltipCaretGeometry.EDGE_HEIGHT else TooltipCaretGeometry.MIDDLE_HEIGHT) * density.density

    val apex = if (edge) {
        if (mirror) {
            (TooltipCaretGeometry.EDGE_WIDTH - TooltipCaretGeometry.EDGE_APEX) * density.density
        } else {
            TooltipCaretGeometry.EDGE_APEX * density.density
        }
    } else {
        middleApex
    }

    val edgeExtra = with(density) { TooltipDefaults.CaretEdgeExtraDistance.toPx() }
    val edgeCornerInset = with(density) { TooltipArrowCornerThreshold.toPx() } + edgeExtra
    val origin = when {
        edge -> {
            val minInset = edgeCornerInset.coerceAtMost(((span - length) / 2f).coerceAtLeast(0f))
            (anchor - apex).coerceIn(minInset, (span - length - minInset).coerceAtLeast(minInset))
        }

        canFitFlatMiddle -> {
            (anchor.coerceIn(startLimit, endLimit) - apex).coerceIn(
                effectiveRadius,
                (span - effectiveRadius - length).coerceAtLeast(effectiveRadius),
            )
        }

        else -> {
            // Short bubble whose flat edge is narrower than CaretWidth: keep the centred arrow
            // inside the gentle upper curve rather than pushing an edge arrow onto the steep corner.
            val maxSafeInset = ((span - length) / 2f).coerceAtLeast(0f)
            val inset = min(effectiveRadius * 0.65f, maxSafeInset)
            (anchor - apex).coerceIn(inset, (span - inset - length).coerceAtLeast(inset))
        }
    }

    // Upper bound on how far the rounded bubble corner recedes from the flat edge at a given
    // coordinate along [0..span]. Embedding the arrow base past this depth eliminates any gap.
    fun cornerDropAt(pos: Float): Float {
        val distFromEnd = min(pos, span - pos).coerceAtLeast(0f)
        if (distFromEnd >= effectiveRadius || effectiveRadius <= 0f) return 0f
        val dx = effectiveRadius - distFromEnd
        return effectiveRadius - sqrt((effectiveRadius * effectiveRadius - dx * dx).coerceAtLeast(0f))
    }

    val maxCornerDrop = max(cornerDropAt(origin), cornerDropAt(origin + length))
    val baseEmbed = with(density) { TooltipDefaults.CaretEmbedDistance.toPx() }
    val minOverlap = with(density) { 0.5.dp.toPx() }
    val effectiveEmbed = max(baseEmbed, maxCornerDrop + minOverlap).coerceAtMost(crossSpan * 0.25f)
    val yOffset = (caretPaddingPx + effectiveEmbed) - arrowHeight
    val skirtDepth = with(density) { 2.dp.toPx() }.coerceAtMost(crossSpan * 0.2f)

    fun map(localX: Float, localY: Float): Offset = when (side) {
        TooltipCaretSide.Top -> Offset(bubbleRect.left + origin + localX, yOffset + localY)
        TooltipCaretSide.Bottom -> Offset(bubbleRect.left + origin + length - localX, size.height - (yOffset + localY))
        TooltipCaretSide.Right -> Offset(size.width - (yOffset + localY), bubbleRect.top + origin + localX)
        TooltipCaretSide.Left -> Offset(yOffset + localY, bubbleRect.top + origin + length - localX)
    }

    val reversedAxis = side == TooltipCaretSide.Bottom || side == TooltipCaretSide.Left
    val outlineMirror = if (edge && reversedAxis) !mirror else mirror
    val outline = TooltipCaretGeometry.outline(edge = edge, mirror = outlineMirror)
    val path = Path()
    outline.forEachIndexed { index, point ->
        val mapped = map(point.x * density.density, point.y * density.density)
        if (index == 0) path.moveTo(mapped.x, mapped.y) else path.lineTo(mapped.x, mapped.y)
    }
    if (outline.isNotEmpty() && skirtDepth > 0f) {
        val last = outline.last()
        val first = outline.first()
        val lastSkirt = map(last.x * density.density, last.y * density.density + skirtDepth)
        val firstSkirt = map(first.x * density.density, first.y * density.density + skirtDepth)
        path.lineTo(lastSkirt.x, lastSkirt.y)
        path.lineTo(firstSkirt.x, firstSkirt.y)
    }
    path.close()
    return path
}
