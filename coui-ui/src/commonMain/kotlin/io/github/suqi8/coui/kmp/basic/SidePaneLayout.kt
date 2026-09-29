// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.FoldPane
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Which end of its travel a [SidePaneLayout] pane is resting at.
 *
 * Mirrors `COUISidePaneLayout.OPEN_STATE` (0) and `COUISidePaneLayout.CLOSE_STATE` (1). The device
 * names them by the *state constant*, so `OPEN_STATE` is 0 and `isOpen()` is `mState == 0`.
 *
 * @property deviceValue The `*_STATE` constant of the device class.
 */
@Immutable
enum class SidePaneState(val deviceValue: Int) {
    /** `OPEN_STATE` — the pane is shown at the start edge. */
    Open(0),

    /** `CLOSE_STATE` — the pane is slid out of the layout. */
    Closed(1),
}

/**
 * The callbacks of a [SidePaneLayout], mirroring `COUISidePaneLayout.PanelSlideListener`.
 *
 * The device passes the owner `View` to `onPanelSlide` and an `int` state constant to the three
 * animation callbacks; the view is dropped because the pane is a slot here, and the constant is
 * mapped onto [SidePaneState].
 */
@Stable
interface SidePanePanelSlideListener {

    /** The pane moved. `offset` runs from 0 (closed) to 1 (fully open), matching `mSlideOffset`. */
    fun onPanelSlide(offset: Float) = Unit

    /** `onPanelAnimStart(int)` — an open/close animation started. */
    fun onPanelAnimationStart(state: SidePaneState) = Unit

    /** `onPanelAnimEnd(int)` — an open/close animation finished. */
    fun onPanelAnimationEnd(state: SidePaneState) = Unit

    /** `onPanelAnimCancel(int)` — an open/close animation was cancelled. */
    fun onPanelAnimationCancel(state: SidePaneState) = Unit
}

/**
 * The callback of a [SidePaneLayout] mask, mirroring `COUISidePaneLayout.PanelMaskListener`.
 */
fun interface SidePaneMaskListener {

    /** `PanelMaskListener#clickMask` — the mask was pressed while [SidePaneLayout.alwaysShowMask]. */
    fun clickMask()
}

/**
 * State of a [SidePaneLayout].
 *
 * The pane position is a fraction of the pane width, so it survives a container resize. What is
 * persisted mirrors `COUISidePaneLayout.SavedState`, which stores `isOpen`, `isDefalutOpen` (the
 * device's typo) and `state`.
 */
@Stable
class SidePaneLayoutState internal constructor(initiallyOpen: Boolean) {

    /** Horizontal position of the pane: 0 offset hidden to 1 offset fully shown. */
    internal val offset = Animatable(if (initiallyOpen) 1f else 0f)

    internal var command by mutableStateOf(SidePaneSettleCommand(initiallyOpen, 0f, false, 0))
        private set

    private var commandCounter = 0

    /** The state the pane is resting at, mirroring `COUISidePaneLayout#isOpen()` inverted. */
    val state: SidePaneState get() = if (command.open) SidePaneState.Open else SidePaneState.Closed

    /** Whether the pane is asked to be open. */
    val isOpen: Boolean get() = command.open

    /** The current pane position, 0f hidden to 1f fully shown; mirrors `mSlideOffset`. */
    val slideOffset: Float get() = offset.value

    /** Whether the open/close animator is running; mirrors `isSliding()`. */
    val isSliding: Boolean get() = offset.isRunning

    /** Opens the pane; mirrors `openPane()`. */
    fun open() {
        setOpen(true)
    }

    /** Closes the pane; mirrors `closePane()`. */
    fun close() {
        setOpen(false)
    }

    /** Flips [isOpen] — what the fold icon on `coui_sliding_icon_layout` does. */
    fun toggle() {
        setOpen(!command.open)
    }

    /** Settles the pane onto [open] after a gesture released with [velocity] (px/s). */
    internal fun release(open: Boolean, velocity: Float) {
        setOpen(open, velocity)
    }

    /** Stops the running animator because a gesture took over the pane. */
    internal fun hold() {
        command = SidePaneSettleCommand(command.open, 0f, cancelAnimator = true, id = ++commandCounter)
    }

    private fun setOpen(open: Boolean, velocity: Float = 0f) {
        command = SidePaneSettleCommand(open, velocity, cancelAnimator = false, id = ++commandCounter)
    }

    companion object {
        /** The [Saver] of [SidePaneLayoutState]. */
        val Saver: Saver<SidePaneLayoutState, *> =
            listSaver(
                save = { listOf(it.isOpen) },
                restore = { SidePaneLayoutState(it[0]) },
            )
    }
}

/**
 * Internal target of an open/close command with the gesture velocity that asked for it.
 *
 * [id] keeps otherwise identical commands distinct so a release that does not change the open flag
 * still restarts the settle animation. [cancelAnimator] is the "a gesture took over" command: it
 * stops the running animator without starting a new one.
 */
@Immutable
internal data class SidePaneSettleCommand(
    val open: Boolean,
    val velocity: Float,
    val cancelAnimator: Boolean,
    val id: Int,
)

/** Remembers a [SidePaneLayoutState] across recomposition and configuration changes. */
@Composable
fun rememberSidePaneLayoutState(initiallyOpen: Boolean = true): SidePaneLayoutState = rememberSaveable(saver = SidePaneLayoutState.Saver) {
    SidePaneLayoutState(initiallyOpen)
}

/**
 * The ColorOS draggable side pane, mirroring
 * `com.coui.appcompat.sidepane.COUISidePaneLayout`
 * (`extends android.widget.RelativeLayout`).
 *
 * The device view lays out three children: index 0 is the slideable pane, index 1 the content and
 * index 2 the fold button inflated from `layout/coui_sliding_icon_layout`. Only indices 0 and 1
 * have Compose counterparts as slots; index 2 is the [foldIcon] control this composable places at
 * the pane's top start, at `coui_side_pane_layout_icon_margin_start` / `_margin_top` from the
 * layout's top-start corner.
 *
 * Opening and closing is a `ValueAnimator` of `DURATION_OF_SLIDING_ANIMATOR` (483 ms) driven by
 * `COUIMoveEaseInterpolator` — `PathInterpolator(0.3f, 0.0f, 0.1f, 1.0f)`. The drag itself runs
 * through a `ViewDragHelper` created with sensitivity 0.5 whose minimum fling velocity is
 * `MIN_FLING_VELOCITY` (400 dp/s); the device settles a released drag through `settleCapturedViewAt`
 * and COUI declares no separate duration for that path, so this composable settles both a gesture
 * release and a programmatic open/close with the same 483 ms move-ease tween.
 *
 * @param paneContent The pane body, laid out at the start edge.
 * @param modifier The modifier applied to the whole layout.
 * @param state The [SidePaneLayoutState] of the pane.
 * @param paneWidth The pane width, i.e. the `firstPaneWidth` attribute, whose resource default is
 *   `coui_sliding_pane_width` (224dp).
 * @param coverStyle The `coverStyle` attribute (`mIsCover`, default false): when true the content
 *   keeps the full width and the pane covers it; when false the content is pushed by the pane's
 *   current trailing edge, which is what the Gallery's `SidePaneLayout#t()` does with relative start
 *   padding on the content view.
 * @param alwaysShowMask `setAlwaysShowMask` (`mAlwaysShow`, default false). Together with
 *   [coverStyle] it is what enables the `coui_color_mask` scrim in `drawChild`.
 * @param touchContentEnabled `setTouchContentEnable` (`mIsTouchEnabled`, device default true). A tap
 *   outside the pane closes it only when this, [coverStyle] and the open state all hold, which is
 *   `onInterceptTouchEvent`'s rule.
 * @param showFoldIcon `setCreateIcon` (`mCreateIcon`, device default true). When false no fold
 *   button is placed.
 * @param foldIcon The glyph inside the fold button. Defaults to
 *   [io.github.suqi8.coui.kmp.icon.basic.FoldPane], the 1:1 port of `drawable/coui_pane_fold_normal`
 *   that `coui_sliding_icon_layout` inflates.
 * @param slideListener The [SidePanePanelSlideListener]; mirrors `setPanelSlideListener`.
 * @param maskListener The [SidePaneMaskListener]; mirrors `setOnMaskClickListener`.
 * @param scrimColor The scrim color, COUI `coui_color_mask` (`#33000000`), painted at its own alpha
 *   multiplied by the slide offset.
 * @param paneColor The pane background. `COUISidePaneLayout` paints none on the pane child, so the
 *   port defaults to [COUITheme.colorScheme.surface] to read as a card over the page.
 * @param foldIconColors The colors of the fold button; see [SidePaneFoldIconColors].
 * @param content The page content the pane slides over.
 */
@Composable
fun SidePaneLayout(
    paneContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    state: SidePaneLayoutState = rememberSidePaneLayoutState(),
    paneWidth: Dp = SidePaneDefaults.PaneWidth,
    coverStyle: Boolean = SidePaneDefaults.COVER_STYLE,
    alwaysShowMask: Boolean = SidePaneDefaults.ALWAYS_SHOW_MASK,
    touchContentEnabled: Boolean = SidePaneDefaults.TOUCH_CONTENT_ENABLED,
    showFoldIcon: Boolean = SidePaneDefaults.CREATE_ICON,
    foldIcon: ImageVector = SidePaneDefaults.foldIcon,
    slideListener: SidePanePanelSlideListener? = null,
    maskListener: SidePaneMaskListener? = null,
    scrimColor: Color = SidePaneDefaults.scrimColor(),
    paneColor: Color = SidePaneDefaults.paneColor(),
    foldIconColors: SidePaneFoldIconColors = SidePaneDefaults.foldIconColors(),
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val paneWidthPx = with(density) { paneWidth.toPx() }
    val minFlingVelocityPx = with(density) { SidePaneDefaults.MinFlingVelocity.toPx() }
    val offset = state.slideOffset
    val maskVisible = (coverStyle || alwaysShowMask) && offset > 0f
    val dragChannel = remember { Channel<Float>(Channel.CONFLATED) }

    LaunchedEffect(state, paneWidthPx) {
        for (fraction in dragChannel) state.offset.snapTo(fraction)
    }

    LaunchedEffect(state.command, paneWidthPx) {
        val command = state.command
        if (command.cancelAnimator) return@LaunchedEffect
        val target = if (command.open) 1f else 0f
        slideListener?.onPanelAnimationStart(state.state)
        try {
            state.offset.animateTo(
                targetValue = target,
                animationSpec = tween(
                    durationMillis = SidePaneDefaults.DURATION_MILLIS,
                    easing = MoveEaseEasing,
                ),
            )
        } catch (cancellation: CancellationException) {
            slideListener?.onPanelAnimationCancel(state.state)
            throw cancellation
        }
        slideListener?.onPanelAnimationEnd(state.state)
    }

    LaunchedEffect(state) {
        snapshotFlow { state.slideOffset }.collect { value -> slideListener?.onPanelSlide(value) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (coverStyle) Modifier else Modifier.padding(start = paneWidth * offset),
                ),
        ) {
            content()
        }

        if (maskVisible) {
            // drawChild paints coui_color_mask at its own alpha times mSlideOffset, from the pane's
            // trailing edge to the end of the layout.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val left = paneWidthPx * offset
                        drawRect(
                            color = scrimColor.copy(alpha = scrimColor.alpha * offset),
                            topLeft = Offset(left, 0f),
                            size = Size((size.width - left).coerceAtLeast(0f), size.height),
                        )
                    }
                    .pointerInput(coverStyle, touchContentEnabled, alwaysShowMask) {
                        detectTapGestures {
                            if (alwaysShowMask) maskListener?.clickMask()
                            if (coverStyle && touchContentEnabled) state.close()
                        }
                    },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(paneWidth)
                .offset { IntOffset((-paneWidthPx * (1f - state.slideOffset)).roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val fraction = paneDragFraction(state.slideOffset, delta, paneWidthPx)
                        if (fraction != null) dragChannel.trySend(fraction)
                    },
                    onDragStarted = { state.hold() },
                    onDragStopped = { velocity ->
                        // onViewReleased: open on a rightward fling, or when past the halfway point.
                        val open = if (abs(velocity) > minFlingVelocityPx) {
                            velocity > 0f
                        } else {
                            state.slideOffset > 0.5f
                        }
                        state.release(open = open, velocity = velocity)
                    },
                )
                .background(paneColor),
        ) {
            paneContent()
        }

        if (showFoldIcon) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(
                        start = SidePaneDefaults.FoldIconMarginStart,
                        top = SidePaneDefaults.FoldIconMarginTop,
                    )
                    .size(SidePaneDefaults.FoldIconTouchSize)
                    .clip(CircleShape)
                    .background(foldIconColors.containerColor)
                    .clickable(role = Role.Button) { state.toggle() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = foldIcon,
                    contentDescription = SidePaneDefaults.FOLD_ICON_CONTENT_DESCRIPTION,
                    modifier = Modifier
                        .width(SidePaneDefaults.FoldIconSize)
                        .height(SidePaneDefaults.FoldIconHeight),
                    tint = foldIconColors.iconColor,
                )
            }
        }
    }
}

/**
 * The drag position of a pane, damped to one tenth of the movement outside the 0..1 range so the
 * pane rubber-bands instead of clipping. Returns null when the pane has no measurable width yet.
 */
private fun paneDragFraction(current: Float, delta: Float, paneWidthPx: Float): Float? {
    if (paneWidthPx <= 0f) return null
    val raw = current * paneWidthPx + delta
    val damped = when {
        raw < 0f -> raw * 0.1f
        raw > paneWidthPx -> paneWidthPx + (raw - paneWidthPx) * 0.1f
        else -> raw
    }
    return damped / paneWidthPx
}

/** `COUIMoveEaseInterpolator`, a `PathInterpolator(0.3f, 0.0f, 0.1f, 1.0f)`. */
private val MoveEaseEasing = CubicBezierEasing(0.3f, 0.0f, 0.1f, 1.0f)

/**
 * Helpers of [SidePaneLayout], mirroring `com.coui.appcompat.sidepane.COUISidePaneUtils`.
 *
 * The device methods take a `View` and a `Context`; here they are pure functions over the value the
 * caller would have written into that view, so the layout constants stay on the Compose side.
 */
object SidePaneUtils {

    /** `LARGE_SCREEN_WIDTH_DP_THRESHOLD` — 600. */
    const val LARGE_SCREEN_WIDTH_THRESHOLD = 600

    /** `LARGE_SCREEN_HEIGHT_DP_THRESHOLD` — 900. */
    const val LARGE_SCREEN_HEIGHT_THRESHOLD = 900

    /** `SMALLEST_SCREEN_HEIGHT_DP_THRESHOLD` — 480. */
    const val SMALLEST_SCREEN_HEIGHT_THRESHOLD = 480

    /**
     * `START_EDIT_ICON_ALPHA_OFFSET` — 0.9f. The device literal is `0.100000024f` for the divisor
     * below the offset, float noise for 0.1f.
     */
    const val START_EDIT_ICON_ALPHA_OFFSET = 0.9f

    /** `isLargeScreenStyle(Context)` — 600dp wide and 900dp tall. */
    fun isLargeScreenStyle(windowSize: DpSize): Boolean = windowSize.width.value >= LARGE_SCREEN_WIDTH_THRESHOLD &&
        windowSize.height.value >= LARGE_SCREEN_HEIGHT_THRESHOLD

    /** `isMediumScreenStyle(Activity)` — 600dp wide and at least 480dp tall, or multi-window. */
    fun isMediumScreenStyle(windowSize: DpSize, inMultiWindowMode: Boolean = false): Boolean = windowSize.width.value >= LARGE_SCREEN_WIDTH_THRESHOLD &&
        (windowSize.height.value >= SMALLEST_SCREEN_HEIGHT_THRESHOLD || inMultiWindowMode)

    /**
     * `makeFirstEditIconAnim(float, View, Context)` — the icon is invisible below the offset and
     * ramps to full alpha across the last tenth of the slide.
     */
    fun editIconAlpha(progress: Float): Float = when {
        progress > 1f || progress < 0f -> 0f

        progress >= START_EDIT_ICON_ALPHA_OFFSET ->
            (progress - START_EDIT_ICON_ALPHA_OFFSET) / (1f - START_EDIT_ICON_ALPHA_OFFSET)

        else -> 0f
    }

    /**
     * `makeSecToolbarAnim(float, View, Context)` — the second toolbar's start margin ramps down from
     * `coui_side_pane_layout_toolbar_margin_start` to 0 as the pane opens.
     */
    fun secondaryToolbarMarginStart(
        progress: Float,
        margin: Dp = SidePaneDefaults.ToolbarMarginStart,
    ): Dp = margin * (1f - progress)
}

/** Contains default values used by [SidePaneLayout]. */
object SidePaneDefaults {

    /**
     * The pane width, i.e. the `firstPaneWidth` / `expandPaneWidth` attribute defaults. COUI
     * `dimen/coui_sliding_pane_width` — 224dp.
     */
    val PaneWidth = 224.dp

    /**
     * `COUISidePaneLayout.DEFAULT_OVERHANG_SIZE` — 32dp of the layout the slideable pane may travel
     * into, converted by the device to `(32 * density) + 0.5` pixels.
     */
    val OverhangSize = 32.dp

    /**
     * `COUISidePaneLayout.DURATION_OF_SLIDING_ANIMATOR` — 483 ms, used by both `mTranlateAnimator`
     * and `mSlideAnimator`.
     */
    const val DURATION_MILLIS = 483

    /**
     * `COUISidePaneLayout.MIN_FLING_VELOCITY` — 400 dp/s, handed to
     * `ViewDragHelper#setMinVelocity`. The device passes `density * 400.0f` pixels per second.
     */
    val MinFlingVelocity = 400.dp

    /**
     * `coverStyle` attribute default in `COUISidePaneLayout`'s constructor — false, i.e. the pane
     * pushes the content rather than covering it. `layout/app_activity_support_side_pane.xml` leaves
     * it at the default.
     */
    const val COVER_STYLE = false

    /** `setAlwaysShowMask` default — false. */
    const val ALWAYS_SHOW_MASK = false

    /** `setTouchContentEnable` default — true. */
    const val TOUCH_CONTENT_ENABLED = true

    /** `setCreateIcon` default — true. */
    const val CREATE_ICON = true

    /** The fold glyph that `layout/coui_sliding_icon_layout` inflates (`drawable/coui_pane_fold_normal`). */
    val foldIcon: ImageVector get() = COUIIcons.Basic.FoldPane

    /**
     * Icon size, COUI `coui_side_pane_layout_icon_size` — 48dp. `coui_sliding_icon_layout` sets no
     * explicit size, so the vector's own 48x52dp intrinsic size is what the ImageButton measures.
     */
    val FoldIconSize = 48.dp

    /** Height of `coui_pane_fold_normal`'s viewport — 52dp; no separate `coui_*` dimen token. */
    val FoldIconHeight = 52.dp

    /** Padding of the fold button, COUI `coui_side_pane_layout_icon_padding` — 12dp. */
    val FoldIconPadding = 12.dp

    /**
     * Touch target of the fold button: [FoldIconSize] plus twice [FoldIconPadding], the box the
     * device's `wrap_content` ImageButton ends up with.
     */
    val FoldIconTouchSize = FoldIconSize + FoldIconPadding * 2

    /** Top margin of the fold button, COUI `coui_side_pane_layout_icon_margin_top` — 45dp. */
    val FoldIconMarginTop = 45.dp

    /** Start margin of the fold button, COUI `coui_side_pane_layout_icon_margin_start` — 12dp. */
    val FoldIconMarginStart = 12.dp

    /**
     * Start margin of the pane's secondary toolbar, COUI
     * `coui_side_pane_layout_toolbar_margin_start` — 72dp; consumed by
     * [SidePaneUtils.secondaryToolbarMarginStart].
     */
    val ToolbarMarginStart = 72.dp

    /** `coui_sliding_icon_layout`'s `android:contentDescription` — "Shrink and expand button". */
    const val FOLD_ICON_CONTENT_DESCRIPTION = "Shrink and expand button"

    /** The default scrim color: COUI `coui_color_mask`, `#33000000`. */
    @Composable
    fun scrimColor(): Color = COUITheme.colorScheme.windowDimming

    /** The default pane background: [COUITheme.colorScheme.surface], a port stand-in for a host card. */
    @Composable
    fun paneColor(): Color = COUITheme.colorScheme.surface

    /**
     * The default fold button colors: `drawable/coui_pane_icon_bg` ripples with
     * `color/coui_pane_icon_bg_color` (`#0d000000` light, `#1affffff` night) and
     * `drawable/coui_pane_fold_normal` is tinted with `attr/couiColorLabelPrimary`.
     */
    @Composable
    fun foldIconColors(
        containerColor: Color = COUITheme.colorScheme.onSurface.copy(alpha = 0.05f),
        iconColor: Color = COUITheme.colorScheme.onSurface,
    ): SidePaneFoldIconColors = remember(containerColor, iconColor) {
        SidePaneFoldIconColors(containerColor = containerColor, iconColor = iconColor)
    }
}

/**
 * Colors used by the fold button of a [SidePaneLayout].
 *
 * @param containerColor The circular press background, COUI `coui_pane_icon_bg_color`.
 * @param iconColor The glyph tint, COUI `couiColorLabelPrimary`.
 */
@Immutable
data class SidePaneFoldIconColors(
    val containerColor: Color,
    val iconColor: Color,
)
