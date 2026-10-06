// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.COUIResponsiveUtils
import io.github.suqi8.coui.kmp.utils.COUIWindowSizeClass
import kotlinx.coroutines.channels.Channel
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The drawer mode of a [SideNavigationBar].
 *
 * Mirrors the design's temporary (0) and fixed (1) drawer modes. The unspecified (-1) sentinel is
 * not observable once the window size is known, so it has no counterpart here.
 *
 * @property deviceValue The raw drawer-mode constant.
 */
@Immutable
enum class SideNavigationBarMode(val deviceValue: Int) {
    /**
     * `DRAWER_MODE_TEMPORARY` — the drawer floats above the content and dims it with
     * `coui_color_mask`, the mask tap closes it, and the width is `coui_side_navigation_bar_min_width`
     * (300dp).
     */
    Temporary(0),

    /**
     * `DRAWER_MODE_FIXED` — the drawer is pinned next to the content, adds no scrim
     * (`setDrawerMode(1, ...)` calls `setScrimColor(0)`), leaves the content interactive, and uses
     * `min(coui_side_navigation_bar_max_width, windowWidth * 0.382)`.
     */
    Fixed(1),
}

/**
 * The callbacks of a [SideNavigationBar], mirroring
 * `COUISideNavigationBar.COOUISideNavigationBarListener`.
 *
 * The device signatures carry the drawer `View` and raw pixel widths; both are dropped here because
 * the drawer and the content are slots of a composable rather than children of a view group. The
 * device argument order of `onDrawerSlide(View, float, int, int)` is preserved as
 * (offset, width, mode).
 */
@Stable
interface SideNavigationBarListener {

    /** The drawer moved. `slideOffset` runs from 0 (closed) to 1 (fully open). */
    fun onDrawerSlide(slideOffset: Float, slideWidth: Dp, mode: SideNavigationBarMode) = Unit

    /** The drawer finished opening. */
    fun onDrawerOpened() = Unit

    /** The drawer finished closing. */
    fun onDrawerClosed() = Unit

    /** The drawer started or stopped moving; mirrors `DrawerLayout#onDrawerStateChanged`. */
    fun onDrawerStateChanged() = Unit

    /** The [SideNavigationBarMode] was recomputed for the current window. */
    fun onDrawerModeChanged(mode: SideNavigationBarMode) = Unit

    /** The drawer width was recomputed; mirrors `onDrawerWidthChanged(int, int, int)`. */
    fun onDrawerWidthChanged(
        previousWidth: Dp,
        width: Dp,
        mode: SideNavigationBarMode,
    ) = Unit
}

/**
 * Internal target of an open/close command, together with the fling velocity that produced it.
 *
 * [id] makes every command unique so that a release that does not change the open flag — e.g. a
 * short drag that snaps back — still restarts the settle animation. [cancelAnimator] is the
 * "a gesture took over" command: it stops the running settle without starting a new one.
 */
@Immutable
internal data class SideNavigationBarSettleCommand(
    val open: Boolean,
    val velocity: Float,
    val cancelAnimator: Boolean,
    val id: Int,
)

/**
 * State of a [SideNavigationBar].
 *
 * The drawer position is held as a fraction of the drawer width, so it survives a window resize.
 * What is persisted mirrors `COUISideNavigationBar.COOUISideNavigationBarSavedState`, which stores
 * the open/closed bookkeeping (`mExplicitDrawerState` / `mImplicitDrawerState`) plus the edit flag.
 */
@Stable
class SideNavigationBarState internal constructor(initiallyOpen: Boolean) {

    /** Horizontal position of the drawer: 0 offset closed to 1 offset fully open. */
    internal val offset = Animatable(if (initiallyOpen) 1f else 0f)

    /** Registered [SideNavigationBarListener]s, iterated in reverse like the device list. */
    internal val listeners = mutableStateListOf<SideNavigationBarListener>()

    internal var command by mutableStateOf(SideNavigationBarSettleCommand(initiallyOpen, 0f, false, 0))
        private set

    private var commandCounter = 0

    /** The drawer width the layout resolved, mirroring `getDrawerViewWidth()`. */
    var drawerWidth: Dp by mutableStateOf(0.dp)
        internal set

    /** The mode the layout resolved, mirroring `getDrawerMode()`. */
    var mode: SideNavigationBarMode by mutableStateOf(SideNavigationBarMode.Temporary)
        internal set

    /** Whether the drawer is asked to be open. */
    val isOpen: Boolean get() = command.open

    /** The current drawer position, 0f closed to 1f fully open. */
    val slideOffset: Float get() = offset.value

    /** Asks the drawer to open; mirrors `openDrawer(View, boolean)`. */
    fun open() {
        settle(open = true, velocity = 0f)
    }

    /** Asks the drawer to close; mirrors `closeDrawer(View, boolean)`. */
    fun close() {
        settle(open = false, velocity = 0f)
    }

    /** Flips [isOpen]. */
    fun toggle() {
        settle(open = !command.open, velocity = 0f)
    }

    internal fun settle(open: Boolean, velocity: Float) {
        command = SideNavigationBarSettleCommand(open, velocity, cancelAnimator = false, id = ++commandCounter)
    }

    /** Stops the running settle because a gesture took over the drawer. */
    internal fun hold() {
        command = SideNavigationBarSettleCommand(command.open, 0f, cancelAnimator = true, id = ++commandCounter)
    }

    /** Registers [listener]; mirrors `addSideNavigationBarListener`. */
    fun addListener(listener: SideNavigationBarListener) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    /** Unregisters [listener]; mirrors `removeSideNavigationBarListener`. */
    fun removeListener(listener: SideNavigationBarListener) {
        listeners.remove(listener)
    }

    companion object {
        /** The [Saver] of [SideNavigationBarState]. */
        val Saver: Saver<SideNavigationBarState, *> =
            listSaver(
                save = { listOf(it.isOpen) },
                restore = { SideNavigationBarState(it[0]) },
            )
    }
}

/** Remembers a [SideNavigationBarState] across recomposition and configuration changes. */
@Composable
fun rememberSideNavigationBarState(initiallyOpen: Boolean = false): SideNavigationBarState = rememberSaveable(saver = SideNavigationBarState.Saver) {
    SideNavigationBarState(initiallyOpen)
}

/**
 * The ColorOS side navigation drawer.
 *
 * `drawerContent` sits at the start edge and slides in and out above `content`. The drawer is
 * *temporary* on compact windows and on windows whose content structure is a parent/child
 * hierarchy; it becomes *fixed* — no scrim, no tap-to-close, content still interactive — once the
 * COUI window content weight exceeds the content structure weight. That is exactly the rule
 * `COUISideNavigationBar#updateDrawerState` applies, with `WINDOW_CONTENT_WEIGHT_COMPAT/MEDIUM/EXPANDED`
 * of 1/2/3 against a `CONTENT_STRUCTURE_WEIGHT_NORMAL/PARENT_CHILD` of 1/2.
 *
 * While [editState] is set on a fixed drawer the mask fades in.
 * `COUISideNavigationBar#startMaskFadeAnimation` was built with no duration, so it uses the
 * `ValueAnimator` default of 300 ms and the `ValueAnimator` default
 * `AccelerateDecelerateInterpolator`.
 *
 * The drawer's own settle animation is inherited by the device class from `DrawerLayout`, which
 * routes it through `androidx.customview.widget.ViewDragHelper#settleCapturedViewAt`; COUI declares
 * no drawer duration or curve token. [SideNavigationBarDefaults.settleDurationMillis] therefore
 * reproduces `ViewDragHelper#computeAxisDuration` (256 ms base, 600 ms cap).
 *
 * @param drawerContent The drawer body, laid out at the start edge.
 * @param modifier The modifier applied to the whole navigation bar.
 * @param state The [SideNavigationBarState] of the drawer.
 * @param parentChildHierarchy Whether the content is a parent/child hierarchy, which raises the
 *   content structure weight from 1 to 2 (`COUISideNavigationBar#setParentChildHierarchy`) and can
 *   demote a medium window back to a temporary drawer.
 * @param editState Whether the host is in an edit state (`setIsInEditState`). On a fixed drawer this
 *   fades the mask in; on a temporary one it is inert, as in the device view.
 * @param handleEditModeMask The device `getHandlerEditModeMask()` flag; when false the edit-state
 *   mask is never faded in. Defaults to the device value `true`.
 * @param pushContent When true the content is padded by the drawer's current trailing edge instead
 *   of being overlaid. The device base class always overlays (it is a `DrawerLayout`); the push
 *   behaviour is what the Gallery's `SidePaneLayout` subclass adds in its own `t()` by setting the
 *   content view's relative start padding to the drawer's right edge.
 * @param scrimColor The color painted over the content in [SideNavigationBarMode.Temporary] mode
 *   (COUI `coui_color_mask`, `#33000000`).
 * @param dividerColor The color of the hairline on the drawer's trailing edge (COUI
 *   `coui_side_navigation_drawer_divider_width` wide, `attr/couiColorDivider` colored).
 * @param content The page content behind the drawer.
 */
@Composable
fun SideNavigationBar(
    drawerContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    state: SideNavigationBarState = rememberSideNavigationBarState(),
    parentChildHierarchy: Boolean = false,
    editState: Boolean = false,
    handleEditModeMask: Boolean = true,
    pushContent: Boolean = false,
    scrimColor: Color = SideNavigationBarDefaults.scrimColor(),
    dividerColor: Color = SideNavigationBarDefaults.dividerColor(),
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val windowDpSize = LocalWindowInfo.current.containerDpSize
    val sizeClass = COUIResponsiveUtils.windowSizeClass(windowDpSize.width)
    val mode = SideNavigationBarDefaults.drawerMode(sizeClass, parentChildHierarchy)
    val drawerWidth = SideNavigationBarDefaults.drawerWidth(mode, windowDpSize.width)
    val drawerWidthPx = with(density) { drawerWidth.toPx() }
    val dividerWidthPx = with(density) { SideNavigationBarDefaults.DividerWidth.toPx() }
    val minFlingVelocityPx = with(density) { SideNavigationBarDefaults.MinFlingVelocity.toPx() }
    val containerWidthPx = with(density) { windowDpSize.width.toPx() }
    val dragChannel = remember { Channel<Float>(Channel.CONFLATED) }

    // setDrawerMode(0) paints coui_color_mask (mMaskColor); setDrawerMode(1) clears the scrim, so
    // only the edit-state animator can bring the mask back on a fixed drawer.
    val maskFraction = remember { Animatable(if (editState) 1f else 0f) }
    var previousMode by remember { mutableStateOf(mode) }

    LaunchedEffect(mode, editState, handleEditModeMask) {
        val fadeIn = mode == SideNavigationBarMode.Fixed && editState && handleEditModeMask
        if (mode == SideNavigationBarMode.Fixed && handleEditModeMask) {
            maskFraction.animateTo(
                targetValue = if (fadeIn) 1f else 0f,
                animationSpec = tween(
                    durationMillis = SideNavigationBarDefaults.EDIT_MASK_DURATION_MILLIS,
                    easing = AccelerateDecelerateEasing,
                ),
            )
        } else {
            maskFraction.snapTo(if (fadeIn) 1f else 0f)
        }
    }

    LaunchedEffect(mode) {
        state.mode = mode
        state.listeners.reversed().forEach { it.onDrawerModeChanged(mode) }
        // updateDrawerState(): dropping out of a fixed mode closes the drawer that no longer fits.
        if (previousMode == SideNavigationBarMode.Fixed && mode == SideNavigationBarMode.Temporary) {
            state.close()
        }
        previousMode = mode
    }

    LaunchedEffect(drawerWidth, mode) {
        val previous = state.drawerWidth
        state.drawerWidth = drawerWidth
        state.listeners.reversed().forEach { it.onDrawerWidthChanged(previous, drawerWidth, mode) }
    }

    LaunchedEffect(state, drawerWidthPx, containerWidthPx) {
        for (fraction in dragChannel) state.offset.snapTo(fraction)
    }

    LaunchedEffect(state.command, drawerWidthPx, containerWidthPx) {
        if (drawerWidthPx <= 0f) return@LaunchedEffect
        val command = state.command
        if (command.cancelAnimator) return@LaunchedEffect
        val target = if (command.open) 1f else 0f
        val dx = ((target - state.offset.value) * drawerWidthPx).roundToInt()
        state.offset.animateTo(
            targetValue = target,
            animationSpec = tween(
                durationMillis = SideNavigationBarDefaults.settleDurationMillis(
                    dx = dx,
                    velocity = command.velocity,
                    motionRange = drawerWidthPx.toInt(),
                    parentWidth = containerWidthPx.toInt(),
                ),
                easing = LinearEasing,
            ),
        )
    }

    LaunchedEffect(state, drawerWidth) {
        snapshotFlow { state.slideOffset }.collect { fraction ->
            state.listeners.reversed().forEach {
                it.onDrawerSlide(fraction, drawerWidth * fraction, state.mode)
            }
        }
    }

    LaunchedEffect(state, drawerWidth) {
        snapshotFlow { state.offset.isRunning }.collect { running ->
            state.listeners.reversed().forEach { it.onDrawerStateChanged() }
            if (running) return@collect
            when {
                state.offset.value >= 1f -> state.listeners.reversed().forEach { it.onDrawerOpened() }
                state.offset.value <= 0f -> state.listeners.reversed().forEach { it.onDrawerClosed() }
            }
        }
    }

    // DrawerLayout paints the scrim with its own mScrimOpacity, which follows the drawer's slide, so
    // a closed temporary drawer leaves the content fully interactive. setDrawerMode(0) only picks
    // the mask colour; it is the slide that fades the mask in. A fixed drawer has no scrim at all
    // (setDrawerMode(1) calls setScrimColor(0)) — only the edit-state animator brings it back.
    val scrimAlpha = when (mode) {
        SideNavigationBarMode.Temporary -> state.slideOffset
        SideNavigationBarMode.Fixed -> maskFraction.value
    }
    val scrimVisible = scrimAlpha > 0f
    // shouldIgnoreAccessibilityInContent(): the edit state, or a temporary drawer being opened.
    val contentBlocked = when (mode) {
        SideNavigationBarMode.Temporary -> state.isOpen || scrimVisible
        SideNavigationBarMode.Fixed -> scrimVisible
    }
    val drawerOffsetPx = (state.slideOffset * drawerWidthPx) - drawerWidthPx - dividerWidthPx

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (pushContent) Modifier.padding(start = drawerWidth * state.slideOffset) else Modifier,
                )
                // While the page behind the mask is not reachable, its semantics subtree is dropped.
                .then(if (contentBlocked) Modifier.clearAndSetSemantics {} else Modifier),
        ) {
            content()
        }

        if (scrimVisible) {
            // onTouchEvent(): an up inside the touch slop closes the drawer, so a tap on the mask
            // closes it and a drag on the mask drives the drawer like a drag on the drawer itself.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimColor.copy(alpha = scrimColor.alpha * scrimAlpha))
                    .then(
                        if (mode == SideNavigationBarMode.Temporary) {
                            Modifier
                                .pointerInput(state) {
                                    detectTapGestures { state.close() }
                                }
                                .draggable(
                                    orientation = Orientation.Horizontal,
                                    state = rememberDraggableState { delta ->
                                        val fraction = dragFraction(state.slideOffset, delta, drawerWidthPx)
                                        if (fraction != null) dragChannel.trySend(fraction)
                                    },
                                    onDragStarted = { state.hold() },
                                    onDragStopped = { velocity ->
                                        settleFromGesture(state, velocity, drawerWidthPx, minFlingVelocityPx)
                                    },
                                )
                        } else {
                            Modifier
                        },
                    ),
            )
        }

        if (mode == SideNavigationBarMode.Temporary && state.slideOffset <= 0f) {
            // DrawerLayout enables ViewDragHelper edge tracking on this side; ViewDragHelper's
            // default edge strip is DEFAULT_EDGE_SIZE (20dp).
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(SideNavigationBarDefaults.EdgeSize)
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            val fraction = dragFraction(state.slideOffset, delta, drawerWidthPx)
                            if (fraction != null) dragChannel.trySend(fraction)
                        },
                        onDragStarted = { state.hold() },
                        onDragStopped = { velocity ->
                            settleFromGesture(state, velocity, drawerWidthPx, minFlingVelocityPx)
                        },
                    ),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(drawerWidth + SideNavigationBarDefaults.DividerWidth)
                .offset { IntOffset(drawerOffsetPx.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val fraction = dragFraction(state.slideOffset, delta, drawerWidthPx)
                        if (fraction != null) dragChannel.trySend(fraction)
                    },
                    onDragStarted = { state.hold() },
                    onDragStopped = { velocity ->
                        settleFromGesture(state, velocity, drawerWidthPx, minFlingVelocityPx)
                    },
                ),
        ) {
            Box(modifier = Modifier.width(drawerWidth).fillMaxHeight()) {
                drawerContent()
            }
            // The device adds the hairline as a sibling with translationZ 1 and paints it after the
            // drawer, at the drawer's right edge (drawChild sets its translationX to drawerView.right).
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(SideNavigationBarDefaults.DividerWidth)
                    .fillMaxHeight()
                    .background(dividerColor),
            )
        }
    }
}

/**
 * The drag position of a drawer, damped to one tenth of the movement outside the 0..1 range so a
 * dragged drawer rubber-bands instead of clipping.
 */
private fun dragFraction(current: Float, delta: Float, drawerWidthPx: Float): Float? {
    if (drawerWidthPx <= 0f) return null
    val raw = current * drawerWidthPx + delta
    val damped = when {
        raw < 0f -> raw * 0.1f
        raw > drawerWidthPx -> drawerWidthPx + (raw - drawerWidthPx) * 0.1f
        else -> raw
    }
    return damped / drawerWidthPx
}

/**
 * Settles a drawer that was released mid-gesture.
 *
 * `DrawerLayout#onViewReleased` opens when the fling exceeds the `ViewDragHelper` minimum velocity
 * (400 dp/s) in the opening direction and otherwise keeps whichever half of the range the drawer
 * passed, so the same rule is applied here.
 */
private fun settleFromGesture(
    state: SideNavigationBarState,
    velocity: Float,
    drawerWidthPx: Float,
    minFlingVelocityPx: Float,
) {
    if (drawerWidthPx <= 0f) return
    val open = if (abs(velocity) > minFlingVelocityPx) velocity > 0f else state.slideOffset > 0.5f
    state.settle(open = open, velocity = velocity)
}

/**
 * `android.animation.ValueAnimator`'s default interpolator, used by the edit-state mask fade because
 * `COUISideNavigationBar#startMaskFadeAnimation` builds its `ValueAnimator` without setting one.
 */
private val AccelerateDecelerateEasing = Easing { input ->
    (cos((input + 1f) * PI) / 2.0 + 0.5).toFloat()
}

/** Contains default values used by [SideNavigationBar]. */
object SideNavigationBarDefaults {

    /**
     * Fraction of the window width a fixed drawer takes; the device caps the result with
     * [MaxWidth]. COUI `COUISideNavigationBar.DRAWER_WIDTH_PROPORTION`.
     */
    const val WIDTH_PROPORTION = 0.382f

    /**
     * Drawer width in temporary mode. COUI `COUISideNavigationBar.mDrawerWidthInTemporaryMode`,
     * read from `dimen/coui_side_navigation_bar_min_width` — 300dp.
     *
     * The resource name is a misnomer in the device resources: the drawer is never narrower than
     * [MaxWidth] in practice, because 300dp is also the temporary-mode width for compact windows,
     * where the 0.382 proportion would give less.
     */
    val MinWidth = 300.dp

    /**
     * Upper bound of the fixed drawer width. COUI `COUISideNavigationBar.mMaxDrawerWidth`, read
     * from `dimen/coui_side_navigation_bar_max_width` — 280dp, which is *below* [MinWidth].
     */
    val MaxWidth = 280.dp

    /**
     * Smallest gap the device leaves between the drawer and the window edge when it measures the
     * drawer child. COUI `COUISideNavigationBar.MIN_DRAWER_MARGIN` — 64dp; only used to build the
     * device's child measure spec, so it has no direct Compose counterpart.
     */
    val MinDrawerMargin = 64.dp

    /**
     * Width of the hairline on the drawer's trailing edge. COUI
     * `coui_side_navigation_drawer_divider_width` — 0.33000004dp, i.e. one physical pixel at
     * ~3x density. `COUISideNavigationBar` draws it as a sibling view with `translationZ` 1.
     */
    val DividerWidth = 0.33.dp

    /**
     * `ViewDragHelper`'s default edge strip, from which the drawer can be pulled out when it is
     * closed. `COUISideNavigationBar` never changes the edge size, so the helper default of 20dp
     * applies.
     */
    val EdgeSize = 20.dp

    /**
     * Minimum fling velocity that snaps a released drawer. `ViewDragHelper`'s `MIN_VELOCITY`, which
     * `DrawerLayout` never overrides — 400 dp/s. The device constant
     * `COUISidePaneLayout.MIN_FLING_VELOCITY = 400` of the sibling side pane is the same value.
     */
    val MinFlingVelocity = 400.dp

    /**
     * `ViewDragHelper#computeAxisDuration`'s `BASE_SETTLE_DURATION` — 256 ms. Inherited by the
     * device drawer through `DrawerLayout`; no `coui_*` token covers it.
     */
    const val BASE_SETTLE_DURATION_MILLIS = 256

    /**
     * `ViewDragHelper#computeAxisDuration`'s `MAX_SETTLE_DURATION` — 600 ms. Inherited by the
     * device drawer through `DrawerLayout`; no `coui_*` token covers it.
     */
    const val MAX_SETTLE_DURATION_MILLIS = 600

    /**
     * Duration of the edit-state mask fade. `COUISideNavigationBar#startMaskFadeAnimation` creates
     * its `ValueAnimator` without a duration, so the `ValueAnimator` default of 300 ms applies; no
     * `coui_*` token covers it.
     */
    const val EDIT_MASK_DURATION_MILLIS = 300

    /** The default scrim color: COUI `coui_color_mask`, `#33000000`. */
    @Composable
    fun scrimColor(): Color = COUITheme.colorScheme.windowDimming

    /** The default divider color: `attr/couiColorDivider`, falling back to `color/coui_color_divider`. */
    @Composable
    fun dividerColor(): Color = COUITheme.colorScheme.dividerLine

    /**
     * Resolves the drawer mode the way `COUISideNavigationBar#updateDrawerState` does: the window
     * content weight (1 compact / 2 medium / 3 expanded) minus the content structure weight
     * (1 normal / 2 parent-child). A positive remainder is a fixed drawer, otherwise temporary.
     */
    fun drawerMode(
        windowSizeClass: COUIWindowSizeClass,
        parentChildHierarchy: Boolean,
    ): SideNavigationBarMode {
        val windowContentWeight = when (windowSizeClass) {
            COUIWindowSizeClass.Compat -> 1
            COUIWindowSizeClass.Medium -> 2
            COUIWindowSizeClass.Expanded -> 3
        }
        val contentStructureWeight = if (parentChildHierarchy) 2 else 1
        return if (windowContentWeight - contentStructureWeight > 0) {
            SideNavigationBarMode.Fixed
        } else {
            SideNavigationBarMode.Temporary
        }
    }

    /**
     * Resolves the drawer width the way `COUISideNavigationBar#calculateDrawerWidth` does: a
     * temporary drawer keeps [MinWidth], a fixed drawer takes [WIDTH_PROPORTION] of the window width
     * capped by [MaxWidth].
     */
    fun drawerWidth(mode: SideNavigationBarMode, windowWidth: Dp): Dp = when (mode) {
        SideNavigationBarMode.Temporary -> MinWidth
        SideNavigationBarMode.Fixed -> minOf(MaxWidth, windowWidth * WIDTH_PROPORTION)
    }

    /**
     * Reproduces `androidx.customview.widget.ViewDragHelper#computeAxisDuration`, which
     * `DrawerLayout` uses for its drawer settle duration.
     *
     * The base duration is scaled by the travelled range; when the gesture carried velocity the
     * duration is instead `4 * distance / velocity`, and the result is capped at
     * [MAX_SETTLE_DURATION_MILLIS]. `distance` is halfway across the parent plus the sine-weighted
     * distance influence of the drag.
     */
    fun settleDurationMillis(
        dx: Int,
        velocity: Float,
        motionRange: Int,
        parentWidth: Int,
    ): Int {
        if (dx == 0) return 0
        val range = if (motionRange > 0) motionRange else 1
        val halfParent = parentWidth / 2
        val distanceRatio = min(1f, abs(dx).toFloat() / parentWidth.coerceAtLeast(1))
        val influence = (distanceRatio - 0.5f) * 0.3f * (PI.toFloat() / 2f)
        val distance = halfParent + halfParent * sin(influence)
        val duration = if (velocity != 0f) {
            4 * (1000 * abs(distance / velocity)).roundToInt()
        } else {
            ((abs(dx).toFloat() / range + 1f) * BASE_SETTLE_DURATION_MILLIS).toInt()
        }
        return min(duration, MAX_SETTLE_DURATION_MILLIS)
    }
}
