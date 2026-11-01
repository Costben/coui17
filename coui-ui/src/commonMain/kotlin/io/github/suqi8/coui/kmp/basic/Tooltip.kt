// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BasicTooltipDefaults
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import io.github.suqi8.coui.kmp.squircle.SquircleShape
import io.github.suqi8.coui.kmp.squircle.isSquircleEnabled
import io.github.suqi8.coui.kmp.squircle.squircleBackground
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
import kotlin.math.sin
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

private class TooltipScopeImpl(
    override val positioning: TooltipAnchorPosition,
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
        if (isPersistent) {
            job?.cancel()
        }
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
    val positioning = (positionProvider as? TooltipPopupPositionProvider)?.positioning ?: TooltipAnchorPosition.Below
    val scope = remember(positioning) { TooltipScopeImpl(positioning) { anchorBounds } }

    TooltipBoxImpl(
        positionProvider = positionProvider,
        tooltip = {
            AnimatedVisibility(
                visibleState = state.transition,
                enter = fadeIn(ListPopupDefaults.AlphaEnterAnimationSpec) +
                    scaleIn(initialScale = 0.9f, animationSpec = ListPopupDefaults.FractionAnimationSpec),
                exit = fadeOut(ListPopupDefaults.AlphaExitAnimationSpec) +
                    scaleOut(targetScale = 0.9f, animationSpec = TooltipExitScaleSpec),
            ) {
                scope.tooltip()
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
 * Miuix's tooltip primitive. Forked from the experimental foundation `BasicTooltipBox` so the mouse
 * hover gesture keeps a persistent tooltip open: when the cursor leaves the anchor the tooltip is
 * dismissed only if it is not persistent. This lets the cursor travel onto a persistent [RichTooltip]
 * to reach its action, matching the Material 3 tooltip behavior.
 */
@Composable
private fun TooltipBoxImpl(
    positionProvider: PopupPositionProvider,
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
    content: @Composable () -> Unit,
) {
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = { if (state.isVisible) state.dismiss() },
        properties = PopupProperties(focusable = focusable),
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
                            // Long press detected: show the tooltip and consume the following up
                            // event so the anchor's own click handling does not also fire.
                            launch { state.show(MutatePriority.UserInput) }
                            val changes = awaitPointerEvent(pass = pass).changes
                            for (i in changes.indices) {
                                changes[i].consume()
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
    val caretSide = if (caretShape != null) {
        positioning.resolve(layoutDirection).caretSide
    } else {
        null
    }
    val caretPadding = TooltipDefaults.CaretPadding
    var selfBounds by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(
        modifier = modifier
            .layout { measurable, constraints ->
                // COUIToolTips never lets the bubble reach mBubbleHorizontalPadding from the
                // viewport, so a narrow window caps it before maxWidth does.
                val inset = TooltipDefaults.HorizontalEdgeMargin.roundToPx() * 2
                val placeable = measurable.measure(
                    if (constraints.hasBoundedWidth) {
                        constraints.copy(maxWidth = (constraints.maxWidth - inset).coerceAtLeast(constraints.minWidth))
                    } else {
                        constraints
                    },
                )
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
            .widthIn(max = maxWidth)
            .onGloballyPositioned { selfBounds = it }
            .then(
                if (caretSide != null) {
                    // Painted over the content: under it, the bubble's drop shadow greys the arrow
                    // out until it no longer reads against the page.
                    Modifier.drawWithContent {
                        drawContent()
                        val anchor = obtainAnchorBounds() ?: return@drawWithContent
                        val self = selfBounds ?: return@drawWithContent
                        if (!anchor.isAttached || !self.isAttached) return@drawWithContent
                        val anchorOnWindow = anchor.positionInWindow()
                        val selfOnWindow = self.positionInWindow()
                        drawTooltipCaret(
                            side = caretSide,
                            anchorX = anchorOnWindow.x - selfOnWindow.x + anchor.size.width / 2f,
                            anchorY = anchorOnWindow.y - selfOnWindow.y + anchor.size.height / 2f,
                            cornerRadiusPx = cornerRadius.toPx(),
                            color = containerColor,
                        )
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .absolutePadding(
                    left = if (caretSide == TooltipCaretSide.Left) caretPadding else 0.dp,
                    top = if (caretSide == TooltipCaretSide.Top) caretPadding else 0.dp,
                    right = if (caretSide == TooltipCaretSide.Right) caretPadding else 0.dp,
                    bottom = if (caretSide == TooltipCaretSide.Bottom) caretPadding else 0.dp,
                )
                .dropShadow(
                    shape = SquircleShape(
                        cornerRadius = cornerRadius,
                        squircleEnabled = isSquircleEnabled(),
                        weight = SquircleDefaults.WeightLarge,
                    ),
                    shadow = PopupShadow,
                )
                .squircleBackground(
                    color = containerColor,
                    cornerRadius = cornerRadius,
                    weight = SquircleDefaults.WeightLarge,
                )
                .padding(insideMargin),
        ) {
            content()
        }
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
@Immutable
private class TooltipPopupPositionProvider(
    val positioning: TooltipAnchorPosition,
    private val spacing: Int,
    private val edgeMargin: Int,
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

        var x = centerX
        var y = centerY
        when (resolved) {
            TooltipAnchorPosition.Below -> {
                val below = anchorBounds.bottom + spacing
                val above = anchorBounds.top - popupContentSize.height - spacing
                y = if (below + popupContentSize.height <= windowSize.height || above < 0) below else above
            }

            TooltipAnchorPosition.Above -> {
                val above = anchorBounds.top - popupContentSize.height - spacing
                val below = anchorBounds.bottom + spacing
                y = if (above >= 0 || below + popupContentSize.height > windowSize.height) above else below
            }

            TooltipAnchorPosition.Left -> {
                val left = anchorBounds.left - popupContentSize.width - spacing
                val right = anchorBounds.right + spacing
                x = if (left >= 0 || right + popupContentSize.width > windowSize.width) left else right
            }

            TooltipAnchorPosition.Right -> {
                val right = anchorBounds.right + spacing
                val left = anchorBounds.left - popupContentSize.width - spacing
                x = if (right + popupContentSize.width <= windowSize.width || left < 0) right else left
            }

            else -> {
                y = anchorBounds.bottom + spacing
            }
        }

        // COUIToolTips keeps the bubble mBubbleHorizontalPadding away from both side edges.
        val minX = min(edgeMargin, max(0, windowSize.width - popupContentSize.width) / 2)
        x = x.coerceIn(minX, max(minX, windowSize.width - popupContentSize.width - minX))
        y = y.coerceIn(0, max(0, windowSize.height - popupContentSize.height))
        return IntOffset(x, y)
    }
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

    /** The default maximum width of a rich tooltip. */
    val RichTooltipMaxWidth = 320.dp

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
 * Draws the `COUIToolTips` bubble indicator.
 *
 * `COUIToolTips.addIndicator` always keeps the indicator on the bubble, projecting the anchor on
 * the facing edge and pushing it clear of the rounded corner; when the centred drawable no longer
 * fits it swaps in the `_sharp_edges_` drawable, whose flat side sits against the bubble edge.
 */
private fun DrawScope.drawTooltipCaret(
    side: TooltipCaretSide,
    anchorX: Float,
    anchorY: Float,
    cornerRadiusPx: Float,
    color: Color,
) {
    val vertical = side == TooltipCaretSide.Top || side == TooltipCaretSide.Bottom
    val span = if (vertical) size.width else size.height
    val anchor = if (vertical) anchorX else anchorY

    val middleWidth = TooltipDefaults.CaretWidth.toPx()
    val edgeWidth = TooltipDefaults.CaretEdgeWidth.toPx()

    val startLimit = cornerRadiusPx + middleWidth / 2f
    val endLimit = span - cornerRadiusPx - middleWidth / 2f
    val edge = startLimit > endLimit || anchor < startLimit || anchor > endLimit
    val mirror = edge && anchor < span / 2f
    val length = if (edge) edgeWidth else middleWidth

    val apex = if (edge) {
        if (mirror) {
            (TooltipCaretGeometry.EDGE_WIDTH - TooltipCaretGeometry.EDGE_APEX) * density
        } else {
            TooltipCaretGeometry.EDGE_APEX * density
        }
    } else {
        TooltipCaretGeometry.MIDDLE_APEX * density
    }
    val edgeExtra = TooltipDefaults.CaretEdgeExtraDistance.toPx()
    val origin = if (edge) {
        // `coui_tooltips_edge_extra_distance`: the edge drawable keeps 2dp from the bubble end.
        val min = edgeExtra.coerceAtMost((span - length).coerceAtLeast(0f))
        (anchor - apex).coerceIn(min, (span - length - edgeExtra).coerceAtLeast(min))
    } else {
        (anchor.coerceIn(startLimit, endLimit) - apex).coerceIn(
            cornerRadiusPx,
            (span - cornerRadiusPx - length).coerceAtLeast(cornerRadiusPx),
        )
    }

    fun map(localX: Float, localY: Float): Offset = when (side) {
        TooltipCaretSide.Top -> Offset(origin + localX, localY)
        TooltipCaretSide.Bottom -> Offset(origin + length - localX, size.height - localY)
        TooltipCaretSide.Right -> Offset(size.width - localY, origin + localX)
        TooltipCaretSide.Left -> Offset(localY, origin + length - localX)
    }

    val path = Path()
    TooltipCaretGeometry.outline(edge = edge, mirror = mirror).forEachIndexed { index, point ->
        val mapped = map(point.x * density, point.y * density)
        if (index == 0) path.moveTo(mapped.x, mapped.y) else path.lineTo(mapped.x, mapped.y)
    }
    path.close()
    drawPath(path, color = color)
}
