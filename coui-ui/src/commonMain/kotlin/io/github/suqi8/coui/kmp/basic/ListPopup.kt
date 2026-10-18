// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.anim.BezierGravityPathGeometry
import io.github.suqi8.coui.kmp.anim.BezierGravityPathMath
import io.github.suqi8.coui.kmp.anim.PopupGravityMotion
import io.github.suqi8.coui.kmp.anim.SinOutEasing
import io.github.suqi8.coui.kmp.anim.folmeSpring
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.BlendColorEntry
import io.github.suqi8.coui.kmp.blur.BlurBlendMode
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.squircle.SquircleShape
import io.github.suqi8.coui.kmp.squircle.addSquircleRect
import io.github.suqi8.coui.kmp.squircle.isSquircleEnabled
import io.github.suqi8.coui.kmp.squircle.squircleSurface
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.CouiHapticEffect
import io.github.suqi8.coui.kmp.utils.rememberCouiHaptic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.jvm.JvmName
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min

private fun PopupPositionProvider.Align.resolve(layoutDirection: LayoutDirection): PopupPositionProvider.Align {
    if (layoutDirection == LayoutDirection.Ltr) return this
    return when (this) {
        PopupPositionProvider.Align.Start -> PopupPositionProvider.Align.End
        PopupPositionProvider.Align.End -> PopupPositionProvider.Align.Start
        PopupPositionProvider.Align.TopStart -> PopupPositionProvider.Align.TopEnd
        PopupPositionProvider.Align.TopEnd -> PopupPositionProvider.Align.TopStart
        PopupPositionProvider.Align.BottomStart -> PopupPositionProvider.Align.BottomEnd
        PopupPositionProvider.Align.BottomEnd -> PopupPositionProvider.Align.BottomStart
    }
}

private const val MAX_ITEMS_FOR_WIDTH = 8

/** Thickness of the divider hairline between adjacent popup rows (COUI coui_popup_list_divider_height). */
private val PopupItemDividerThickness = 0.5.dp

/** Horizontal inset of the divider hairline (COUI coui_popup_list_default_divider_margin_horizontal). */
private val PopupItemDividerMargin = 20.dp

/**
 * Total height of the divider band reserved between adjacent popup rows, hairline included
 * (COUI coui_popup_list_group_divider_total_height). ColorOS 17 renders every separator as the real
 * layout space of MixedPopupListAdapter.DividerViewHolder and centres the hairline inside it; the
 * retired ListView path instead drew a bare hairline and a separate full-bleed 4dp group band.
 */
private val PopupItemDividerBandHeight = 12.dp

/** Vertical padding the popup list keeps above its first and below its last row (COUI coui_popup_list_padding_vertical). */
private val PopupListVerticalPadding = 8.dp

/**
 * Alpha spring of the hairlines adjacent to the pressed row
 * (COUI COUITouchListView.DividerAnimationController: COUISpringForce response 0.25 / bounce 0).
 */
private val PopupItemDividerAlphaSpring = spring<Float>(dampingRatio = 1f, stiffness = 631.65f, visibilityThreshold = 0.002f)

/** Press mask spring of a popup row (COUI COUISpringForce response 0.3 / bounce 0). */
internal val PopupItemPressSpring = spring<Float>(dampingRatio = 1f, stiffness = 438.65f)

/**
 * Minimum press progress a row mask reaches before a release may fade it out, so quick taps still
 * flash (COUI COUIMaskEffectDrawable DEFAULT_MIN_PROGRESS_FOR_TOUCH_ENTER_ANIMATION).
 */
internal val PopupItemPressMinVisibleProgress = 0.7f

/** couiColorPress in light themes (COUI coui_color_press #1F000000). */
internal val PopupItemPressLight = Color(0x1F000000)

/** couiColorPress in dark themes (COUI coui_color_press_dark #33FFFFFF). */
internal val PopupItemPressDark = Color(0x33FFFFFF)

/** Horizontal inset of the press mask, per side (COUI coui_popup_list_window_mask_margin_horizontal). */
private val PopupItemPressMaskMargin = 8.dp

/**
 * Press mask corner radius of a row exactly one line tall (COUI
 * coui_popup_list_window_vertical_mask_radius). Such a row is as tall as the radius is wide, so the
 * mask closes into a stadium — COUI passes weight 1.0 to keep that arc exact.
 */
private val PopupItemPressMaskRadius = 20.dp

/**
 * Press mask corner radius of any other row (COUI coui_popup_list_window_vertical_mask_radius_16_1,
 * selected when `COUIVersionUtil.getOSVersionCode() > 37`). COUI passes weight 3.0 here, i.e. the
 * smoothed silhouette rather than a plain arc.
 */
private val PopupItemPressMaskRadiusTall = 28.dp

/** Travel direction of a drag crossing into a new row (COUI DividerAnimationController MOVE_FLAG_*). */
private const val MOVE_FLAG_OUT = 0
private const val MOVE_FLAG_UP = -1
private const val MOVE_FLAG_DOWN = 1

/**
 * A single popup row registered with a [PopupListGestureHost].
 *
 * Bounds are held as plain fields rather than snapshot state: they are written by
 * [ListPopupColumn]'s measure policy, which knows the exact placement order and offsets, and are
 * only read back during the pointer and draw phases, both of which run after layout on every frame.
 */
@Stable
internal class PopupListItemHandle {
    var top: Float = 0f
    var bottom: Float = 0f
    var enabled: Boolean = true
    var onClick: () -> Unit = {}

    /** Press mask progress (0 → 1), driven exclusively by the owning [PopupListGestureHost]. */
    val pressProgress: Animatable<Float, AnimationVector1D> = Animatable(0f)

    private var pressJob: Job? = null

    fun contains(y: Float): Boolean = y >= top && y < bottom

    /** Runs the press-in mask animation. */
    fun enterPress(scope: CoroutineScope) {
        pressJob?.cancel()
        pressJob = scope.launch { pressProgress.animateTo(1f, PopupItemPressSpring) }
    }

    /**
     * Releases the press mask, first holding the press-in animation until it passes
     * [PopupItemPressMinVisibleProgress] so a fast tap still flashes.
     */
    fun exitPress(scope: CoroutineScope) {
        pressJob?.cancel()
        pressJob = scope.launch {
            if (pressProgress.value > 0f && pressProgress.value < PopupItemPressMinVisibleProgress) {
                try {
                    pressProgress.animateTo(1f, PopupItemPressSpring) {
                        if (value >= PopupItemPressMinVisibleProgress) throw PressMaskThresholdReached()
                    }
                } catch (_: PressMaskThresholdReached) {
                    // Minimum visible progress reached; fall through to the fade-out.
                }
            }
            pressProgress.animateTo(0f, PopupItemPressSpring)
        }
    }
}

/** Thrown to stop a deferred press-in mask once [PopupItemPressMinVisibleProgress] is reached. */
private class PressMaskThresholdReached : CancellationException("Press mask reached the minimum visible progress")

/**
 * Owns the drag-to-move-highlight gesture and the hairline dividers of one popup list, mirroring
 * COUI COUITouchListView. Rows register through [Modifier.popupListItem] and never track their own
 * press state: the host resolves the row under the pointer from the registered bounds, so dragging
 * moves the highlight and a release selects whichever row the finger currently rests on.
 */
@Stable
internal class PopupListGestureHost {
    val items = mutableStateListOf<PopupListItemHandle>()

    /**
     * Rows in placement order, written by [ListPopupColumn]'s measure policy. Registration order is
     * composition order but is not guaranteed, so the measure pass — which walks the measurables in
     * their true order — is the authority.
     */
    private var ordered: List<PopupListItemHandle> = emptyList()

    /**
     * Y offset of each drawn hairline, keyed by the index of the row above it in [ordered]. Only
     * boundaries that actually carry a hairline appear here: a group divider band replaces it
     * (COUI DefaultAdapter.getDividerView item view type 2).
     */
    private val dividerOffsets = mutableMapOf<Int, Float>()

    /** The row currently holding the highlight, or `null` while no row is pressed. */
    private var activeItem: PopupListItemHandle? = null

    /** Divider alpha tracks, indexed by the boundary between [ordered] `i` and `i + 1`. */
    private val dividerAlphas = mutableMapOf<Int, Animatable<Float, AnimationVector1D>>()

    /** True while the list has no scroll range, i.e. COUI's mIsDynamicSelection. */
    var isDynamicSelection: Boolean = true

    /** Suppresses divider fades while the list is settling a scroll (COUI onScrollStateChanged). */
    var isScrolling: Boolean = false

    /**
     * Publishes the placement produced by one measure pass: the rows in order and the exact Y of
     * every hairline. Called from the measure policy, so both are already expressed in the list's
     * own coordinate space and need no cross-node resolution.
     */
    fun onPlaced(rows: List<PopupListItemHandle>, hairlines: Map<Int, Float>) {
        ordered = rows
        dividerOffsets.clear()
        dividerOffsets.putAll(hairlines)
    }

    fun register(handle: PopupListItemHandle) {
        if (!items.contains(handle)) items.add(handle)
    }

    fun unregister(handle: PopupListItemHandle) {
        items.remove(handle)
        ordered = ordered - handle
    }

    /** Rows top to bottom, as placed by the last measure pass. */
    fun sortedItems(): List<PopupListItemHandle> = ordered

    /** The hairlines to paint, as `boundary index` to `Y offset` pairs. */
    fun hairlines(): Map<Int, Float> = dividerOffsets

    fun dividerAlpha(boundary: Int): Float = dividerAlphas[boundary]?.value ?: 1f

    private fun animateDivider(boundary: Int, target: Float, scope: CoroutineScope) {
        if (isScrolling) return
        val track = dividerAlphas.getOrPut(boundary) { Animatable(1f) }
        if (track.value == target && !track.isRunning) return
        scope.launch { track.animateTo(target, PopupItemDividerAlphaSpring) }
    }

    /**
     * Fades the two hairlines around [item] out ([visible] `false`) or back in, suppressing the
     * boundary the finger just crossed so the highlight does not flicker over it
     * (COUI DividerAnimationController.dividerAnim MOVE_FLAG_UP/DOWN).
     */
    private fun animateAdjacentDividers(
        item: PopupListItemHandle?,
        visible: Boolean,
        moveFlag: Int,
        scope: CoroutineScope,
    ) {
        if (item == null || !item.enabled) return
        val ordered = sortedItems()
        val index = ordered.indexOf(item)
        if (index < 0) return
        val target = if (visible) 1f else 0f
        if (index > 0 && !(visible && moveFlag == MOVE_FLAG_UP)) {
            animateDivider(index - 1, target, scope)
        }
        if (index < ordered.lastIndex && !(visible && moveFlag == MOVE_FLAG_DOWN)) {
            animateDivider(index, target, scope)
        }
    }

    /** Enters the row under [y], if any. Returns the row that took the highlight. */
    fun onDown(y: Float, scope: CoroutineScope): PopupListItemHandle? {
        val target = sortedItems().firstOrNull { it.contains(y) }
        activeItem = target
        if (target != null && target.enabled) {
            target.enterPress(scope)
            animateAdjacentDividers(target, visible = false, moveFlag = MOVE_FLAG_OUT, scope = scope)
        }
        return target
    }

    /** Whether a move to [y] would hand the highlight to a different, enabled row. */
    fun willChangeRow(y: Float): Boolean {
        val target = sortedItems().firstOrNull { it.contains(y) } ?: return false
        return target !== activeItem && target.enabled
    }

    /**
     * Moves the highlight to the row under [y]. Only meaningful in dynamic selection mode; the
     * scrollable case releases the highlight through [cancel] once the drag passes the touch slop.
     */
    fun onMoveTo(y: Float, scope: CoroutineScope) {
        val previous = activeItem
        val ordered = sortedItems()
        val target = ordered.firstOrNull { it.contains(y) }
        if (target == null) {
            cancel(scope)
            return
        }
        if (target === previous) return

        val moveFlag = when {
            previous == null -> MOVE_FLAG_OUT
            !target.enabled -> MOVE_FLAG_OUT
            ordered.indexOf(target) > ordered.indexOf(previous) -> MOVE_FLAG_DOWN
            else -> MOVE_FLAG_UP
        }
        previous?.exitPress(scope)
        animateAdjacentDividers(previous, visible = true, moveFlag = moveFlag, scope = scope)

        activeItem = target
        if (target.enabled) {
            target.enterPress(scope)
            animateAdjacentDividers(target, visible = false, moveFlag = MOVE_FLAG_OUT, scope = scope)
        }
    }

    /** Releases the highlight without selecting, e.g. when the finger leaves every row. */
    fun cancel(scope: CoroutineScope) {
        val previous = activeItem ?: return
        activeItem = null
        previous.exitPress(scope)
        animateAdjacentDividers(previous, visible = true, moveFlag = MOVE_FLAG_OUT, scope = scope)
    }

    /** Selects the row the finger currently rests on (COUI resolves the target at UP, not at DOWN). */
    fun onUp(scope: CoroutineScope) {
        val target = activeItem
        cancel(scope)
        if (target != null && target.enabled) target.onClick()
    }
}

internal val LocalPopupListGestureHost = staticCompositionLocalOf<PopupListGestureHost?> { null }

/** Exit keeps the host alive, but native RoundFrameLayout stops dispatching row input immediately. */
internal val LocalPopupInteractionEnabled = staticCompositionLocalOf { true }

/**
 * Records where an anchor was last touched, so a popup can open at the finger rather than centred on
 * the anchor — COUI `PreciseClickHelper` / `PreciseLongPressHelper`, which stash the DOWN `(x, y)`
 * and pass it to the click callback, after which `PopupMenuLocateHelper.setAnchor` collapses the
 * anchor bounds to that single point.
 *
 * The point is kept in **window** coordinates, because the composable that positions the popup is
 * usually not the node that receives the touch (a preference hosts its popup inside its trailing
 * action slot, while the whole row is what gets tapped), so an anchor-local offset would be measured
 * against the wrong origin.
 *
 * [Offset.Unspecified] means "no point recorded": the popup then falls back to centring on the
 * anchor, which is also COUI's TalkBack path (`PreciseClickHelper` substitutes the view centre when
 * TalkBack is enabled or the recorded point is still the initial `(0, 0)`).
 */
@Stable
class PreciseClickState {
    /** The last recorded touch point in window coordinates, or [Offset.Unspecified] if none. */
    var touchPoint: Offset by mutableStateOf(Offset.Unspecified)
        internal set

    /**
     * Coordinates of the anchor the point was recorded against, used to convert the touch offset
     * into window space. Held here rather than in a `remember` so [preciseClickAnchor] stays a plain
     * modifier factory instead of a `composed` one.
     */
    internal var anchorCoordinates: LayoutCoordinates? = null

    /** Discards the recorded point, restoring anchor-centred positioning. */
    fun clear() {
        touchPoint = Offset.Unspecified
    }
}

/** Creates and remembers a [PreciseClickState] for a point-anchored popup. */
@Composable
fun rememberPreciseClickState(): PreciseClickState = remember { PreciseClickState() }

/**
 * Records the DOWN position of each touch into [state], mirroring COUI `PreciseClickHelper`'s
 * `OnTouchListener`. Attach to the anchor of a popup that was given the same [PreciseClickState] so
 * the popup opens at the finger.
 *
 * The handler observes in [PointerEventPass.Initial] and consumes nothing, so the anchor's own
 * `clickable` still sees the whole gesture; accessibility clicks produce no pointer event at all and
 * therefore leave the point unrecorded, which is exactly COUI's TalkBack fallback.
 */
fun Modifier.preciseClickAnchor(state: PreciseClickState): Modifier = this
    .onGloballyPositioned { state.anchorCoordinates = it }
    .pointerInput(state) {
        awaitPointerEventScope {
            while (true) {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val anchor = state.anchorCoordinates
                state.touchPoint = if (anchor != null && anchor.isAttached) {
                    anchor.localToWindow(down.position)
                } else {
                    Offset.Unspecified
                }
            }
        }
    }

/**
 * Parent data a registered row exposes to [ListPopupColumn]'s measure policy, so the policy can tell
 * selectable rows apart from group divider bands and headers without inspecting the composition.
 */
private class PopupListItemParentData(
    val handle: PopupListItemHandle,
    val groupKey: Any?,
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any = this@PopupListItemParentData
}

private val IntrinsicMeasurable.popupListItemHandle: PopupListItemHandle?
    get() = (parentData as? PopupListItemParentData)?.handle

/**
 * Group identity a registered row exposes to [ListPopupColumn]'s measure policy, or `null` when the
 * row declares none. COUI only inserts a group divider where `PopupListItem.getGroupId()` changes
 * (`COUIPopupListWindow` collects the boundary indices into `mGroupSets`), so two adjacent rows
 * sharing a key are contiguous and the 12dp band is skipped between them.
 */
private val IntrinsicMeasurable.popupListItemGroupKey: Any?
    get() = (parentData as? PopupListItemParentData)?.groupKey

/**
 * Registers a popup row with the enclosing [PopupListGestureHost] so the list can own its press
 * highlight, and draws the row's press mask: a `couiColorPress` path inset [PopupItemPressMaskMargin]
 * on each side and rounded by [PopupItemPressMaskRadius] or [PopupItemPressMaskRadiusTall]
 * (COUI MixedPopupListAdapter.configureMaskPath).
 *
 * The row keeps its own click semantics: the accessibility action below is registered independently
 * of the pointer tracking, matching COUI's TalkBack path, which bypasses the drag gesture entirely
 * and calls `performItemClick`.
 *
 * @param selected Whether the row is the selected option.
 * @param enabled Whether the row reacts to input.
 * @param role The accessibility [Role] exposed for the row.
 * @param onClick Invoked when the row is selected, by pointer or by accessibility action.
 * @param groupKey Group identity shared with the adjacent rows that belong to the same popup group.
 *   [ListPopupColumn] skips the 12dp divider band between two rows carrying an equal non-null key,
 *   matching `COUIPopupListWindow`'s `getGroupId()` boundary rule. `null` keeps the band between
 *   every adjacent pair.
 */
@Composable
internal fun Modifier.popupListItem(
    selected: Boolean,
    enabled: Boolean,
    role: Role,
    onClick: () -> Unit,
    groupKey: Any? = null,
): Modifier {
    val interactionEnabled = enabled && LocalPopupInteractionEnabled.current
    val host = LocalPopupListGestureHost.current ?: return this.selectable(
        selected = selected,
        enabled = interactionEnabled,
        role = role,
        onClick = onClick,
    )

    val handle = remember { PopupListItemHandle() }
    val currentOnClick by rememberUpdatedState(onClick)
    handle.enabled = interactionEnabled
    handle.onClick = remember { { currentOnClick() } }

    DisposableEffect(host, handle) {
        host.register(handle)
        onDispose { host.unregister(handle) }
    }

    val parentData = remember(handle, groupKey) { PopupListItemParentData(handle, groupKey) }
    val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
    val maskColor = if (isDark) PopupItemPressDark else PopupItemPressLight
    val squircleEnabled = isSquircleEnabled()

    return this
        .then(parentData)
        .drawWithCache {
            val inset = PopupItemPressMaskMargin.toPx()
            val singleLine = size.height == PopupItemPressMaskRadius.toPx() * 2f
            val radius = if (singleLine) PopupItemPressMaskRadius else PopupItemPressMaskRadiusTall
            val path = Path()
            onDrawBehind {
                val progress = handle.pressProgress.value
                if (progress <= 0f) return@onDrawBehind
                path.rewind()
                path.addSquircleRect(
                    width = (size.width - inset * 2f).coerceAtLeast(0f),
                    height = size.height,
                    cornerRadius = radius.toPx(),
                    // COUI asks for weight 1.0 on a stadium row, which is a plain arc; the taller rows
                    // take weight 3.0 and follow the squircle silhouette.
                    squircleEnabled = squircleEnabled && !singleLine,
                )
                translate(left = inset) {
                    drawPath(path, color = maskColor, alpha = progress.coerceIn(0f, 1f))
                }
            }
        }
        .semantics(mergeDescendants = true) {
            this.role = role
            this.selected = selected
            if (interactionEnabled) {
                onClick {
                    currentOnClick()
                    true
                }
            } else {
                disabled()
            }
        }
}

/**
 * A column that automatically aligns the width to the widest item.
 *
 * The column owns the COUI popup gesture: a single pointer handler resolves the row under the
 * finger, so dragging moves the press highlight between rows and a release selects the row the
 * finger currently rests on.
 *
 * It also reserves real layout space for the divider band between every adjacent pair of selectable
 * rows, matching COUI, whose adapter interleaves an actual divider `View` of
 * `coui_popup_list_group_divider_total_height` between items
 * (MixedPopupListAdapter.DividerViewHolder). Because the bands are placed by this measure policy,
 * the exact row bounds and divider offsets are known during layout and are published to the host
 * directly — no cross-node coordinate resolution, which is what previously left the bounds at zero
 * (a child is positioned before its parent).
 *
 * @param content The items
 */
@Composable
fun ListPopupColumn(
    content: @Composable () -> Unit,
) {
    val scrollState = rememberScrollState()
    val host = remember { PopupListGestureHost() }
    val coroutineScope = rememberCoroutineScope()
    val performHaptic = rememberCouiHaptic()
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val dividerColor = COUITheme.colorScheme.dividerLine
    val interactionEnabled = LocalPopupInteractionEnabled.current

    host.isDynamicSelection = !scrollState.canScrollForward && !scrollState.canScrollBackward
    host.isScrolling = scrollState.isScrollInProgress

    val measurePolicy = remember(host) {
        object : MeasurePolicy {
            /** Width clamp shared by the measure and intrinsic passes (COUI min/max window width). */
            private fun Density.resolveIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int {
                val widthCount = min(MAX_ITEMS_FOR_WIDTH, measurables.size)
                var maxIntrinsic = 0
                for (i in 0 until widthCount) {
                    val w = measurables[i].maxIntrinsicWidth(height)
                    if (w > maxIntrinsic) maxIntrinsic = w
                }
                return maxIntrinsic
            }

            override fun MeasureScope.measure(
                measurables: List<Measurable>,
                constraints: Constraints,
            ): MeasureResult {
                val minPx = ListPopupDefaults.MinWidth.roundToPx()
                val maxPx = ListPopupDefaults.MaxWidth.roundToPx()
                val maxIntrinsic = resolveIntrinsicWidth(measurables, constraints.maxHeight)
                val parentMin = constraints.minWidth
                val parentMax = constraints.maxWidth
                val upper = maxOf(maxPx, parentMin).coerceAtMost(parentMax)
                val lower = maxOf(minPx, parentMin).coerceAtMost(upper)
                val listWidth = maxIntrinsic.coerceIn(lower, upper)

                // COUI's DividerViewHolder is a real coui_popup_list_group_divider_total_height
                // View, so the whole band is reserved and the hairline is centred inside it.
                val dividerBand = PopupItemDividerBandHeight.roundToPx()
                val childConstraints = constraints.copy(minWidth = listWidth, maxWidth = listWidth, minHeight = 0)

                // COUI COUIPopupListWindow.measurePopupWindow stops accumulating once the next row
                // would exceed getMaxMainMenuHeight(), and backs off the divider it already added.
                val maxHeight = constraints.maxHeight

                val placeables = ArrayList<Placeable>(measurables.size)
                val offsets = ArrayList<Int>(measurables.size)
                val rows = ArrayList<PopupListItemHandle>(measurables.size)
                val hairlines = mutableMapOf<Int, Float>()

                var listHeight = 0
                var pendingDividerBand = 0
                var previousWasRow = false
                var previousGroupKey: Any? = null

                for (i in measurables.indices) {
                    val measurable = measurables[i]
                    val handle = measurable.popupListItemHandle
                    val isRow = handle != null
                    val groupKey = measurable.popupListItemGroupKey
                    // A divider band only separates two adjacent selectable rows; an explicitly
                    // composed divider or a header occupies its own slot (COUI item view types 2 and 5).
                    // COUIPopupListWindow collects mGroupSets from where PopupListItem.getGroupId()
                    // changes, so two rows declaring an equal group key are one group and stay
                    // contiguous — a uniform single-group list gets no band at all.
                    val sameGroup =
                        isRow && previousWasRow && groupKey != null && groupKey == previousGroupKey
                    val gap = if (isRow && previousWasRow && !sameGroup) dividerBand else 0

                    val placeable = measurable.measure(childConstraints)
                    if (listHeight + gap + placeable.height > maxHeight && placeables.isNotEmpty()) {
                        listHeight -= pendingDividerBand
                        break
                    }

                    if (gap > 0) hairlines[rows.lastIndex] = (listHeight).toFloat()
                    listHeight += gap
                    offsets.add(listHeight)
                    placeables.add(placeable)

                    if (handle != null) {
                        handle.top = listHeight.toFloat()
                        handle.bottom = (listHeight + placeable.height).toFloat()
                        rows.add(handle)
                    }
                    listHeight += placeable.height
                    pendingDividerBand = gap
                    previousWasRow = isRow
                    previousGroupKey = groupKey
                }

                host.onPlaced(rows, hairlines)

                return layout(listWidth, listHeight) {
                    for (i in placeables.indices) {
                        placeables[i].placeRelative(0, offsets[i])
                    }
                }
            }

            override fun IntrinsicMeasureScope.minIntrinsicHeight(
                measurables: List<IntrinsicMeasurable>,
                width: Int,
            ): Int {
                val minPx = ListPopupDefaults.MinWidth.roundToPx()
                val maxPx = ListPopupDefaults.MaxWidth.roundToPx()
                val listWidth = resolveIntrinsicWidth(measurables, Int.MAX_VALUE).coerceIn(minPx, maxPx)
                val dividerBand = PopupItemDividerBandHeight.roundToPx()

                var height = 0
                var previousWasRow = false
                var previousGroupKey: Any? = null
                for (measurable in measurables) {
                    val isRow = measurable.popupListItemHandle != null
                    val groupKey = measurable.popupListItemGroupKey
                    val sameGroup =
                        isRow && previousWasRow && groupKey != null && groupKey == previousGroupKey
                    if (isRow && previousWasRow && !sameGroup) height += dividerBand
                    height += measurable.minIntrinsicHeight(listWidth)
                    previousWasRow = isRow
                    previousGroupKey = groupKey
                }
                return height
            }
        }
    }

    Layout(
        content = {
            CompositionLocalProvider(LocalPopupListGestureHost provides host) {
                content()
            }
        },
        modifier = Modifier
            .focusGroup()
            .height(IntrinsicSize.Min)
            // COUI pads the popup RecyclerView itself (coui_popup_list_padding_vertical) on top of
            // the per-row padding. The band sits outside the scroller, i.e. clipToPadding stays on.
            .padding(vertical = PopupListVerticalPadding)
            .verticalScroll(state = scrollState)
            .popupListGestures(host, coroutineScope, touchSlop, performHaptic, interactionEnabled)
            .drawWithContent {
                drawContent()
                drawPopupItemDividers(host, dividerColor)
            },
        measurePolicy = measurePolicy,
    )
}

/**
 * The COUI popup drag-to-select gesture (COUI COUITouchListView.dispatchTouchEvent). A single
 * handler on the list owns DOWN/MOVE/UP: in dynamic selection mode (a list with no scroll range)
 * dragging moves the highlight across rows with a haptic per crossing; in the scrollable case the
 * highlight is released once the drag passes the touch slop so scrolling wins.
 */
private fun Modifier.popupListGestures(
    host: PopupListGestureHost,
    scope: CoroutineScope,
    touchSlop: Float,
    performHaptic: (CouiHapticEffect) -> Unit,
    enabled: Boolean,
): Modifier = pointerInput(host, enabled) {
    if (!enabled) return@pointerInput
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val downY = down.position.y
        var pressed = host.onDown(downY, scope) != null
        var released = false

        try {
            while (pressed && !released) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change: PointerInputChange = event.changes.firstOrNull { it.id == down.id } ?: break

                if (event.changes.size > 1) {
                    // A second pointer cancels the selection, as COUI does on ACTION_POINTER_DOWN.
                    host.cancel(scope)
                    pressed = false
                    break
                }

                // Resolve both axes and the final UP position. Leaving the menu cancels the row,
                // while returning during the same gesture restores dynamic selection.
                val inside = change.position.x >= 0f && change.position.x < size.width
                if (!inside) {
                    host.cancel(scope)
                } else if (host.isDynamicSelection) {
                    val crossing = host.willChangeRow(change.position.y)
                    host.onMoveTo(change.position.y, scope)
                    if (crossing) performHaptic(CouiHapticEffect.Switch)
                }

                if (change.changedToUpIgnoreConsumed()) {
                    // Compose represents ACTION_CANCEL as an already-consumed synthetic UP.
                    // It must release the mask without activating the item under the pointer.
                    if (change.isConsumed) host.cancel(scope) else host.onUp(scope)
                    released = true
                    break
                }

                val y = change.position.y
                if (!host.isDynamicSelection && abs(y - downY) > touchSlop) {
                    host.cancel(scope)
                    pressed = false
                }
            }
        } finally {
            host.cancel(scope)
        }
    }
}

/**
 * Draws the divider hairline centred in each band the measure policy reserved between two adjacent
 * rows. Only the inset `coui_popup_list_divider_height` line is painted: COUI's divider `View`
 * spans `match_parent` over a transparent `coui_popup_list_group_divider_total_height` band and
 * carries an `InsetDrawable` background (MixedPopupListAdapter.DividerViewHolder).
 */
private fun DrawScope.drawPopupItemDividers(
    host: PopupListGestureHost,
    color: Color,
) {
    val dividers = host.hairlines()
    if (dividers.isEmpty()) return

    val thickness = PopupItemDividerThickness.toPx().coerceAtLeast(1f)
    val bandHeight = PopupItemDividerBandHeight.roundToPx()
    val margin = PopupItemDividerMargin.roundToPx()
    val width = (size.width - margin * 2).coerceAtLeast(0f)
    if (width <= 0f) return

    for ((boundary, y) in dividers) {
        val alpha = host.dividerAlpha(boundary)
        if (alpha <= 0f) continue
        drawRect(
            color = color,
            alpha = alpha.coerceIn(0f, 1f),
            topLeft = Offset(margin.toFloat(), y + (bandHeight - thickness) / 2f),
            size = Size(width = width, height = thickness),
        )
    }
}

/**
 * The divider COUI stacks between two adjacent popup rows and between two dropdown groups: a
 * coui_popup_list_group_divider_total_height band whose coui_popup_list_divider_height hairline is
 * vertically centred and inset by coui_popup_list_default_divider_margin_horizontal on both sides
 * (COUI MixedPopupListAdapter.DividerViewHolder).
 *
 * Rows laid out by [ListPopupColumn] reserve this band themselves; compose this only where the
 * divider is an explicit child of the list, such as a dropdown group boundary.
 *
 * @param modifier The modifier applied to the band.
 * @param color The hairline color (COUI ?couiColorDivider).
 */
@Composable
@NonRestartableComposable
fun PopupListDivider(
    modifier: Modifier = Modifier,
    color: Color = COUITheme.colorScheme.dividerLine,
) = HorizontalDivider(
    modifier = modifier.padding(
        horizontal = PopupItemDividerMargin,
        vertical = (PopupItemDividerBandHeight - PopupItemDividerThickness) / 2,
    ),
    thickness = PopupItemDividerThickness,
    color = color,
)

@Stable
interface PopupPositionProvider {
    /**
     * Calculate the position (offset) of Popup
     *
     * @param anchorBounds Bounds of the anchored (parent) component
     * @param windowBounds Bounds of the safe area of window (excluding the [WindowInsets.Companion.statusBars],
     *   [WindowInsets.Companion.navigationBars] and [WindowInsets.Companion.captionBar])
     * @param layoutDirection [LayoutDirection]
     * @param popupContentSize Actual size of the popup content
     * @param popupMargin (Extra) Margins for the popup content. See [PopupPositionProvider.getMargins]
     * @param alignment Alignment of the popup (relative to the window). See [PopupPositionProvider.Align]
     */
    fun calculatePosition(
        anchorBounds: IntRect,
        windowBounds: IntRect,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
        popupMargin: IntRect,
        alignment: Align,
    ): IntOffset

    /**
     * (Extra) Margins for the popup content.
     */
    fun getMargins(): PaddingValues

    /**
     * Position relative to the window, not relative to the anchor!
     */
    enum class Align {
        Start,
        End,
        TopStart,
        TopEnd,
        BottomStart,
        BottomEnd,
    }
}

object ListPopupDefaults {
    /**
     * COUI ColorOS 17 replaced the single main-menu `COUISpringForce` with three independent springs
     * per direction — one for the Bezier path parameter, one for scale, one for alpha — all carrying
     * COUI's `SpringData` response/bounce pair converted by [folmeSpring] (`dampingRatio` comes from
     * the bounce, `stiffness` from `39.47 / response²`, which is `(2π / response)²`).
     *
     * Geometry and the six spring pairs come from `PopupGravityMotionParams`; the trajectory they
     * feed is built by [BezierGravityPathMath].
     */
    val GravityMotion: PopupGravityMotion = PopupGravityMotion.Default

    /**
     * Path-parameter enter spring, driving the Bezier trajectory's `t` from 0 to 1
     * (COUI `PopupGravityMotionParams.openPathSpring`, response 0.29 / bounce 0.13).
     */
    val PathEnterAnimationSpec =
        folmeSpring<Float>(damping = 0.935f, response = 0.29f, visibilityThreshold = 0.001f)

    /**
     * Path-parameter exit spring, driving the Bezier trajectory's `t` back to 0
     * (COUI `PopupGravityMotionParams.closePathSpring`, response 0.26 / bounce 0).
     */
    val PathExitAnimationSpec =
        folmeSpring<Float>(damping = 1f, response = 0.26f, visibilityThreshold = 0.001f)

    /**
     * Scale enter spring (COUI `PopupGravityMotionParams.openScaleSpring`, response 0.40 /
     * bounce 0.20).
     */
    val FractionAnimationSpec =
        folmeSpring<Float>(damping = 0.9f, response = 0.4f, visibilityThreshold = 0.001f)

    /**
     * Scale exit spring (COUI `PopupGravityMotionParams.closeScaleSpring`, response 0.21 /
     * bounce 0). COUI's exit is asymmetric: the scale settles on its own spring while
     * [AlphaExitAnimationSpec] drives the fade and the unmount timing.
     */
    val FractionExitAnimationSpec =
        folmeSpring<Float>(damping = 1f, response = 0.21f, visibilityThreshold = 0.001f)

    /**
     * Content alpha enter spring (COUI `PopupGravityMotionParams.openOpacitySpring`, response 0.27 /
     * bounce 0).
     */
    val AlphaEnterAnimationSpec =
        folmeSpring<Float>(damping = 1f, response = 0.27f, visibilityThreshold = 0.001f)

    /**
     * Content alpha exit spring; also the master timing for unmounting the popup
     * (COUI `PopupGravityMotionParams.closeOpacitySpring`, response 0.23 / bounce 0).
     */
    val AlphaExitAnimationSpec =
        folmeSpring<Float>(damping = 1f, response = 0.23f, visibilityThreshold = 0.001f)

    /**
     * Window-level exit fade, applied by COUI to the popup window itself rather than to its content
     * (`Animation.COUI.PopupListWindow.Dismiss` → `coui_popup_list_window_exit.xml`: alpha 200ms,
     * `pathInterpolator(0.3, 0, 0.8, 0.15)`). The content is already invisible by the time it runs —
     * the three springs own the visible exit — so it only governs how long the window lingers.
     */
    val WindowExitAnimationSpec =
        tween<Float>(durationMillis = 200, easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f))

    /**
     * Content alpha below which a closing popup stops holding touches. The exit spring takes
     * ~340ms to settle and the window fade another 200ms; holding the page underneath for all of
     * it reads as the page being stuck. At this alpha (~140ms into the close) the panel is all but
     * gone, so a scroll or tap started from here goes to the page.
     */
    const val ExitTouchPassThroughAlpha = 0.1f

    /**
     * Extra clearance reserved below the status bar before the popup may be placed
     * (COUI coui_popup_list_window_top_status_bar_margin).
     */
    val TopBarrier = 16.dp

    /**
     * Extra clearance reserved above the navigation bar before the popup may be placed
     * (COUI coui_popup_list_window_bottom_navigation_bar_margin).
     */
    val BottomBarrier = 32.dp

    /** Default animation spec driving the background dim while entering. */
    val DimEnterAnimationSpec = tween<Float>(durationMillis = 300, easing = SinOutEasing)

    /** Default animation spec driving the background dim while exiting. */
    val DimExitAnimationSpec = tween<Float>(durationMillis = 150, easing = SinOutEasing)

    /**
     * Re-enter spring used when a cancelled back gesture restores the popup. COUI has no dedicated
     * restore spring — `COUIPopupTouchMotionAnimator.startPopupAnim` re-enters through
     * `startOpenPopupMotion`, i.e. the open springs — so this reuses the slowest of them.
     */
    val ResetAnimationSpec = PathEnterAnimationSpec

    /**
     * Default minimum width of the popup (COUI coui_popup_list_window_min_width).
     * Also matches the lower clamp inside [ListPopupColumn]'s width measurement.
     */
    val MinWidth = 168.dp

    /**
     * Default maximum width of the popup window (COUI coui_popup_list_window_max_width).
     * Also matches the upper clamp inside [ListPopupColumn]'s width measurement.
     */
    val MaxWidth = 256.dp

    /**
     * Default minimum height the popup will occupy when measured. Used as the floor for the
     * effective `maxHeight` and `minHeight` constraints during placement.
     */
    val MinPopupHeight = 50.dp

    /**
     * The horizontal margin a dropdown keeps from the window edges, so the menu never sits flush to
     * a screen edge. Stands in for COUI's responsive-grid barrier, whose margin lives in the vendor
     * `responsiveui` component; measured at 16dp on ColorOS 16.
     */
    val HorizontalBarrier = 16.dp

    /**
     * Creates a [PopupPositionProvider] that anchors the popup directly below (or above when there
     * is no room) the anchor, used by dropdown-style list popups.
     *
     * When the anchor bounds arrive collapsed to a single point — which is what
     * [Modifier.preciseClickAnchor] produces from the recorded touch point — the popup is centred on
     * that point and the vertical gap to the anchor is dropped, matching COUI
     * `PopupMenuLocateHelper.setAnchor`: it collapses `mAnchorBounds` to `(left + x, top + y)` and
     * pairs it with the context rule's EMPTY_OUTSETS instead of DEFAULT_ANCHOR_OUTSETS.
     *
     * @param verticalMargin The extra vertical margin between the popup and the anchor.
     * @param horizontalMargin The extra horizontal margin applied to the popup.
     */
    fun dropdownPositionProvider(
        verticalMargin: Dp = 8.dp,
        horizontalMargin: Dp = HorizontalBarrier,
    ): PopupPositionProvider = object : PopupPositionProvider {
        private val margins = PaddingValues(horizontal = horizontalMargin, vertical = verticalMargin)

        override fun calculatePosition(
            anchorBounds: IntRect,
            windowBounds: IntRect,
            layoutDirection: LayoutDirection,
            popupContentSize: IntSize,
            popupMargin: IntRect,
            alignment: PopupPositionProvider.Align,
        ): IntOffset {
            // COUI PopupMenuLocateHelper centres the menu on the anchor and then clamps it into
            // the available bounds; the requested Align only matters for context menus. The
            // available width is inset on BOTH sides, so the menu never sits flush to a screen edge.
            val availableLeft = windowBounds.left + popupMargin.left
            val availableRight = windowBounds.right - popupMargin.right
            val available = availableRight - availableLeft

            // A zero-area anchor is a recorded touch point; COUI then uses EMPTY_OUTSETS, so the
            // 8dp gap collapses and the below/above test runs against the point itself.
            val isPointAnchored = anchorBounds.width == 0 && anchorBounds.height == 0
            val verticalGap = if (isPointAnchored) 0 else popupMargin.bottom
            val topGap = if (isPointAnchored) 0 else popupMargin.top

            val centeredX = anchorBounds.left + anchorBounds.width / 2 - popupContentSize.width / 2
            val offsetX = if (popupContentSize.width > available) {
                availableLeft + (available - popupContentSize.width) / 2
            } else {
                centeredX
            }
            val offsetY = if (windowBounds.bottom - anchorBounds.bottom > popupContentSize.height) {
                // Show below
                anchorBounds.bottom + verticalGap
            } else if (anchorBounds.top - windowBounds.top > popupContentSize.height) {
                // Show above
                anchorBounds.top - popupContentSize.height - topGap
            } else {
                // Neither side fits: mMainMenuLocateYRule leaves y at mAvailableBounds.top rather
                // than centring the menu over its anchor.
                windowBounds.top + popupMargin.top
            }
            return IntOffset(
                x = offsetX.coerceIn(
                    availableLeft,
                    (availableRight - popupContentSize.width).coerceAtLeast(availableLeft),
                ),
                y = offsetY.coerceIn(
                    (windowBounds.top + popupMargin.top).coerceAtMost(windowBounds.bottom - popupContentSize.height - popupMargin.bottom),
                    windowBounds.bottom - popupContentSize.height - popupMargin.bottom,
                ),
            )
        }

        override fun getMargins(): PaddingValues = margins
    }

    /** Default dropdown [PopupPositionProvider] created by [dropdownPositionProvider]. */
    val DropdownPositionProvider: PopupPositionProvider = dropdownPositionProvider()

    /** A [PopupPositionProvider] that anchors the popup to a corner of the anchor for context menus. */
    val ContextMenuPositionProvider = object : PopupPositionProvider {
        override fun calculatePosition(
            anchorBounds: IntRect,
            windowBounds: IntRect,
            layoutDirection: LayoutDirection,
            popupContentSize: IntSize,
            popupMargin: IntRect,
            alignment: PopupPositionProvider.Align,
        ): IntOffset {
            val offsetX: Int
            val offsetY: Int
            when (alignment.resolve(layoutDirection)) {
                PopupPositionProvider.Align.TopStart -> {
                    offsetX = anchorBounds.left + popupMargin.left
                    offsetY = anchorBounds.bottom + popupMargin.top
                }

                PopupPositionProvider.Align.TopEnd -> {
                    offsetX = anchorBounds.right - popupContentSize.width - popupMargin.right
                    offsetY = anchorBounds.bottom + popupMargin.top
                }

                PopupPositionProvider.Align.BottomStart -> {
                    offsetX = anchorBounds.left + popupMargin.left
                    offsetY = anchorBounds.top - popupContentSize.height - popupMargin.bottom
                }

                PopupPositionProvider.Align.BottomEnd -> {
                    offsetX = anchorBounds.right - popupContentSize.width - popupMargin.right
                    offsetY = anchorBounds.top - popupContentSize.height - popupMargin.bottom
                }

                else -> {
                    // Fallback
                    offsetX = if (alignment.resolve(layoutDirection) == PopupPositionProvider.Align.End) {
                        anchorBounds.right - popupContentSize.width - popupMargin.right
                    } else {
                        anchorBounds.left + popupMargin.left
                    }
                    offsetY = if (windowBounds.bottom - anchorBounds.bottom > popupContentSize.height) {
                        // Show below
                        anchorBounds.bottom + popupMargin.bottom
                    } else if (anchorBounds.top - windowBounds.top > popupContentSize.height) {
                        // Show above
                        anchorBounds.top - popupContentSize.height - popupMargin.top
                    } else {
                        // Middle
                        anchorBounds.top + anchorBounds.height / 2 - popupContentSize.height / 2
                    }
                }
            }
            return IntOffset(
                x = offsetX.coerceIn(
                    windowBounds.left,
                    (windowBounds.right - popupContentSize.width - popupMargin.right).coerceAtLeast(windowBounds.left),
                ),
                y = offsetY.coerceIn(
                    (windowBounds.top + popupMargin.top).coerceAtMost(windowBounds.bottom - popupContentSize.height - popupMargin.bottom),
                    windowBounds.bottom - popupContentSize.height - popupMargin.bottom,
                ),
            )
        }

        override fun getMargins(): PaddingValues = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
    }
}

/**
 * Corner radius of the popup container (COUI coui_popup_round_frame_layout_corner_radius, applied by
 * RoundFrameLayout.getSmoothRadiusAndWeight once the OPlus SDK version is above 37). The 12dp
 * `couiRoundCornerM` / `coui_round_corner_m` radius the layout declares is overridden on ColorOS 17.
 */
internal val PopupCornerRadius = 28.dp

/** RoundFrameLayout.initMaterialEffects: SHADOW_STYLE_2 / EDGE_STYLE_2 with per-theme overrides. */
private val PopupStrokeLight = MaterialStrokeParams(edgeAlpha = 0.9f, edgeWidth = 4.dp, shadowFadeIn = 0.9f)
private val PopupStrokeDark = MaterialStrokeParams(edgeAlpha = 0.4f, edgeWidth = 2.dp, shadowFadeIn = 0.1f)

/** RoundFrameLayout.DEFAULT_BLUR_RADIUS_PX, authored in physical pixels. */
private const val POPUP_BLUR_RADIUS_PX = 160f

/** Gallery's weight_16_1_normal, shared by the popup silhouette and its NEW_G2 material rim. */
private const val POPUP_CORNER_WEIGHT = 2.5f

/** COUI popup surface in light themes (coui_popup_window_background -> ?couiColorBackgroundTop, coui_color_background_top #FFFFFF). */
private val PopupSurfaceLight = Color.White

/** COUI popup surface in dark themes (coui_color_background_top #333333, values-night). */
private val PopupSurfaceDark = Color(0xFF333333)

/**
 * Resolves the COUI popup container color for the current theme: the popup floats above
 * arbitrary content, so COUI uses the opaque couiColorBackgroundTop pair (#FFFFFF / #333333)
 * instead of the translucent dark card color (coui_popup_window_background).
 */
@Composable
internal fun popupSurfaceColor(): Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) PopupSurfaceDark else PopupSurfaceLight

/** COUI `coui_popup_list_mix_blur_light` / `coui_popup_list_mix_blur_dark`. */
private val PopupMixBlurLight = Color(0xFF99CDCDCD)
private val PopupMixBlurDark = Color(0xFF99262626)

/** COUI `coui_popup_list_blend_blur_light` / `coui_popup_list_blend_blur_dark`. */
private val PopupBlendBlurLight = Color(0xFFBFEEEEEE)
private val PopupBlendBlurDark = Color(0xFF99262626)

/**
 * The tint pair COUI feeds its popup blur builder: `RoundFrameLayout.onAttachedToWindow()` sets a
 * mix color and a blend color per theme, then `setMode(4)` selects
 * `OplusBlurParam.BLUR_BLEND_MODE_GLOW_OVERLAY` (2 = OVERLAY, 3 = COLORDODGE, 4 = GLOW_OVERLAY).
 * libhwui resolves the pair as colour adjustments, not as opaque layers: the mix color is applied
 * with a luminosity blend and the blend color with an overlay blend, each weighted by the color's
 * alpha (`libhwui.so`: `inColor.rgb = mix(inColor.rgb, luminosity(uMixColor.rgb, inColor.rgb),
 * uMixColor.w * ratio)` followed by the same for `overlay(uBlendColor.rgb, inColor.rgb)`). That is
 * why the native panel stays see-through. The HDR pair (`_hdr`, picked when the display is in an
 * HDR state) is not modelled.
 */
@Composable
internal fun popupGlassBlendColors(): List<BlendColorEntry> = if (COUITheme.colorScheme.background.luminance() < 0.5f) {
    listOf(
        BlendColorEntry(color = PopupMixBlurDark, mode = BlurBlendMode.Luminosity),
        BlendColorEntry(color = PopupBlendBlurDark, mode = BlurBlendMode.Overlay),
    )
} else {
    listOf(
        BlendColorEntry(color = PopupMixBlurLight, mode = BlurBlendMode.Luminosity),
        BlendColorEntry(color = PopupBlendBlurLight, mode = BlurBlendMode.Overlay),
    )
}

/**
 * Drop shadow of the popup container, derived from COUI SHADOW_LV4's Oplus light-source geometry.
 *
 * `RoundFrameLayout.setClipMode(1)` calls `ShadowUtils.setElevationToView(this, 3)`, which on an
 * OPlus-view-elevation SDK (`checkOPlusViewSubSDK(34, 2)`) resolves SHADOW_LV4 and hands five
 * values to `OplusView.setOverrideLightSourceGeometry`:
 *
 * | source parameter | value | COUI token |
 * | :-- | :-- | :-- |
 * | occluder height (elevation) | 180dp | `coui_shadow_elevation_level4` |
 * | spot + ambient colour | `argb(48, 0, 0, 0)` | `coui_shadow_color_lv4` = 48 |
 * | lightX / lightY | -1 / -200dp | `coui_shadow_light_y_level4` |
 * | lightZ | 10000dp | `coui_shadow_light_z_level4` |
 * | light radius | 2000dp | `coui_shadow_light_r_level4` |
 * | extra blur radius | 0dp | `coui_shadow_blur_r_level4` |
 *
 * The call lands on `RenderNode.nSetOverrideLightSourceGeometry` (native), and hwui renders the
 * caster through Skia's spot-shadow metrics — `SkDrawShadowMetrics::GetSpotParams` in
 * `src/core/SkDrawShadowInfo.h`, the same model `ReorderBarrierDrawables::drawShadow` feeds into
 * `SkShadowUtils::DrawShadow`. There is therefore no closed-form
 * `Shadow(radius, color, offset, spread)`; the values below are the engineering approximation that
 * model produces:
 *
 * ```
 * zRatio     = clamp(elevation / (lightZ - elevation), 0, 0.95) = 180 / 9820     = 0.01833
 * offset     = (-zRatio * lightX, -zRatio * lightY)                             = (0.02, +3.67) dp
 * blurRadius = lightRadius * zRatio                                             = 36.66 dp
 * scale      = clamp(lightZ / (lightZ - elevation), 1, 1.95)                    = 1.018
 * ```
 *
 * Direction: lightY is negative, i.e. the light sits above the panel, so the cast shadow runs
 * downward; lightX is the literal -1 passed by COUI, so it is horizontally centred.
 *
 * `radius` transfers 1:1 because both sides convert a radius to a Gaussian sigma with the same
 * rule: Skia's `SkBlurMask::ConvertRadiusToSigma` (`0.57735r + 0.5`) for the spot shadow, and
 * `BlurEffect.convertRadiusToSigma` in Compose's shadow painter.
 *
 * Two parts of the native result a single Compose [Shadow] cannot carry, documented rather than
 * guessed:
 *  - hwui draws an additional omni-directional **ambient** shadow from the same colour: outset
 *    `min(0.5 * elevation, 150dp)` = 90dp at `alpha / AmbientRecipAlpha(180dp)` ≈ 0.078.
 *    `Modifier.dropShadow` renders one shadow only, so this softer halo is dropped.
 *  - the spot silhouette is uniformly scaled by 1.018, a ~1.5dp outset on a 168dp panel that is
 *    well below the blur radius and not a constant, so `spread` stays 0.
 *
 * Shared with the cascading popup surfaces so main and cascading menus cast identical shadows.
 */
internal val PopupShadow: Shadow = Shadow(
    radius = 36.66.dp,
    color = Color.Black.copy(alpha = 48f / 255f),
    spread = 0.dp,
    offset = DpOffset(0.dp, 3.67.dp),
)

/** Group opacity must include the exterior shadow, not just the menu's rectangular bounds. */
private fun Modifier.popupOpacity(progress: () -> Float): Modifier = drawWithCache {
    val paint = Paint()
    // DropShadowPainter's bitmap extends by radius + spread, then moves by the shadow offset.
    val xOutset = ceil((PopupShadow.radius.toPx() + PopupShadow.spread.toPx() + abs(PopupShadow.offset.x.toPx())).coerceAtLeast(0f))
    val yOutset = ceil((PopupShadow.radius.toPx() + PopupShadow.spread.toPx() + abs(PopupShadow.offset.y.toPx())).coerceAtLeast(0f))
    val bounds = Rect(-xOutset, -yOutset, size.width + xOutset, size.height + yOutset)
    onDrawWithContent {
        val opacity = progress().coerceIn(0f, 1f)
        if (opacity == 1f) {
            drawContent()
        } else if (opacity > 0f) {
            paint.alpha = opacity
            // graphicsLayer(alpha < 1) implicitly clips to the node's width/height, even with
            // clip=false. Keep the original coordinates and blend the whole expanded group once.
            drawContext.canvas.saveLayer(bounds, paint)
            try {
                drawContent()
            } finally {
                drawContext.canvas.restore()
            }
        }
    }
}

internal fun safeTransformOrigin(x: Float, y: Float): TransformOrigin {
    val safeX = if (x.isNaN() || x < 0f) 0f else x
    val safeY = if (y.isNaN() || y < 0f) 0f else y
    return TransformOrigin(safeX, safeY)
}

/**
 * Describes how the popup is placed relative to its anchor, used to drive the directional reveal
 * and transform origin.
 *
 * @property showBelow Whether the popup is shown below the anchor.
 * @property showAbove Whether the popup is shown above the anchor.
 * @property isRightAligned Whether the popup is aligned to the right edge of the anchor.
 */
@Immutable
data class PopupLayoutPosition(
    val showBelow: Boolean,
    val showAbove: Boolean,
    val isRightAligned: Boolean,
)

/**
 * The resolved layout information for a list popup, produced by [rememberListPopupLayoutInfo].
 *
 * @property windowBounds Bounds of the safe area of the window the popup is placed within.
 * @property popupMargin The (extra) margins applied around the popup content.
 * @property effectiveTransformOrigin The transform origin in window coordinates used to scale the popup from its anchor corner.
 * @property localTransformOrigin The transform origin local to the popup content used by its [graphicsLayer] scaling.
 * @property popupLayoutPosition The resolved [PopupLayoutPosition] describing how the popup is placed relative to its anchor.
 * @property calculatedOffset The popup content's resolved position in window coordinates. Declared last so
 *   the destructuring `component1`..`component5` keep the meanings they had before the property existed.
 */
@Immutable
data class ListPopupLayoutInfo(
    val windowBounds: IntRect,
    val popupMargin: IntRect,
    val effectiveTransformOrigin: TransformOrigin,
    val localTransformOrigin: TransformOrigin,
    val popupLayoutPosition: PopupLayoutPosition,
    val calculatedOffset: IntOffset,
) {
    /**
     * The five-argument constructor, kept so callers compiled before [calculatedOffset] existed still
     * link; the offset defaults to [IntOffset.Zero]. [calculatedOffset] deliberately carries no default
     * on the primary constructor: a defaulted sixth parameter would make a five-argument call ambiguous
     * between this constructor and the primary one.
     */
    constructor(
        windowBounds: IntRect,
        popupMargin: IntRect,
        effectiveTransformOrigin: TransformOrigin,
        localTransformOrigin: TransformOrigin,
        popupLayoutPosition: PopupLayoutPosition,
    ) : this(
        windowBounds = windowBounds,
        popupMargin = popupMargin,
        effectiveTransformOrigin = effectiveTransformOrigin,
        localTransformOrigin = localTransformOrigin,
        popupLayoutPosition = popupLayoutPosition,
        calculatedOffset = IntOffset.Zero,
    )

    /** The five-property [copy], kept for binary compatibility with callers built before 1.2.0. */
    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Migrated to the copy that also takes calculatedOffset.",
        level = DeprecationLevel.HIDDEN,
    )
    fun copy(
        windowBounds: IntRect = this.windowBounds,
        popupMargin: IntRect = this.popupMargin,
        effectiveTransformOrigin: TransformOrigin = this.effectiveTransformOrigin,
        localTransformOrigin: TransformOrigin = this.localTransformOrigin,
        popupLayoutPosition: PopupLayoutPosition = this.popupLayoutPosition,
    ): ListPopupLayoutInfo = copy(
        windowBounds = windowBounds,
        popupMargin = popupMargin,
        effectiveTransformOrigin = effectiveTransformOrigin,
        localTransformOrigin = localTransformOrigin,
        popupLayoutPosition = popupLayoutPosition,
        calculatedOffset = calculatedOffset,
    )
}

/**
 * Computes and remembers the [ListPopupLayoutInfo] for a list popup from its anchor and content size.
 *
 * @param alignment The [PopupPositionProvider.Align] of the popup relative to the window.
 * @param popupPositionProvider The [PopupPositionProvider] that computes the popup offset and margins.
 * @param parentBounds The bounds of the anchor (parent) component in window coordinates.
 * @param popupContentSize The measured size of the popup content; [IntSize.Zero] before it is measured.
 */
@Composable
fun rememberListPopupLayoutInfo(
    alignment: PopupPositionProvider.Align,
    popupPositionProvider: PopupPositionProvider,
    parentBounds: IntRect,
    popupContentSize: IntSize,
): ListPopupLayoutInfo {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val layoutDirection = LocalLayoutDirection.current
    val displayCutout = WindowInsets.displayCutout
    val statusBars = WindowInsets.statusBars
    val navigationBars = WindowInsets.navigationBars
    val captionBar = WindowInsets.captionBar

    val margins = popupPositionProvider.getMargins()
    val popupMargin = remember(layoutDirection, density, margins) {
        with(density) {
            IntRect(
                left = margins.calculateLeftPadding(layoutDirection).roundToPx(),
                top = margins.calculateTopPadding().roundToPx(),
                right = margins.calculateRightPadding(layoutDirection).roundToPx(),
                bottom = margins.calculateBottomPadding().roundToPx(),
            )
        }
    }

    val containerSize = windowInfo.containerSize
    val bottomBarrierTop = LocalPopupBarriers.current?.bottomBarrierTop

    val windowBounds = remember(
        layoutDirection,
        density,
        displayCutout,
        statusBars,
        navigationBars,
        captionBar,
        containerSize,
        bottomBarrierTop,
    ) {
        with(density) {
            // COUI reserves an extra barrier inside the raw insets before the menu may be placed.
            val windowBottom = containerSize.height - navigationBars.getBottom(this) - captionBar.getBottom(this) -
                ListPopupDefaults.BottomBarrier.roundToPx()
            // A bottom navigation view raises the barrier further, but only in a window tall enough.
            val tallEnough = containerSize.height > PopupBarrierDefaults.MinWindowHeightToApply.roundToPx()
            IntRect(
                left = displayCutout.getLeft(this, layoutDirection),
                top = statusBars.getTop(this) + ListPopupDefaults.TopBarrier.roundToPx(),
                right = containerSize.width - displayCutout.getRight(this, layoutDirection),
                bottom = if (bottomBarrierTop != null && tallEnough) min(windowBottom, bottomBarrierTop) else windowBottom,
            )
        }
    }

    val predictedTransformOrigin = remember(alignment, popupMargin, parentBounds, layoutDirection, containerSize) {
        val xInWindow = when (alignment.resolve(layoutDirection)) {
            PopupPositionProvider.Align.End,
            PopupPositionProvider.Align.TopEnd,
            PopupPositionProvider.Align.BottomEnd,
            -> parentBounds.right - popupMargin.right

            else -> parentBounds.left + popupMargin.left
        }
        val yInWindow = when (alignment.resolve(layoutDirection)) {
            PopupPositionProvider.Align.BottomEnd, PopupPositionProvider.Align.BottomStart ->
                parentBounds.top - popupMargin.bottom

            else ->
                parentBounds.bottom + popupMargin.bottom
        }
        safeTransformOrigin(
            xInWindow / containerSize.width.toFloat(),
            yInWindow / containerSize.height.toFloat(),
        )
    }

    val calculatedOffset = remember(
        popupContentSize,
        windowBounds,
        parentBounds,
        alignment,
        layoutDirection,
        popupMargin,
        popupPositionProvider,
    ) {
        if (popupContentSize == IntSize.Zero) {
            IntOffset.Zero
        } else {
            popupPositionProvider.calculatePosition(
                parentBounds,
                windowBounds,
                layoutDirection,
                popupContentSize,
                popupMargin,
                alignment,
            )
        }
    }

    val popupLayoutPosition = remember(
        popupContentSize,
        windowBounds,
        parentBounds,
        alignment,
        calculatedOffset,
        layoutDirection,
    ) {
        if (popupContentSize == IntSize.Zero) {
            val isRightAligned = when (alignment.resolve(layoutDirection)) {
                PopupPositionProvider.Align.End,
                PopupPositionProvider.Align.TopEnd,
                PopupPositionProvider.Align.BottomEnd,
                -> true

                else -> false
            }
            PopupLayoutPosition(showBelow = true, showAbove = false, isRightAligned = isRightAligned)
        } else {
            val popupCenterY = calculatedOffset.y + popupContentSize.height / 2
            val anchorCenterY = parentBounds.top + parentBounds.height / 2
            val showBelow = popupCenterY > anchorCenterY
            val showAbove = popupCenterY < anchorCenterY

            val distLeft = abs(calculatedOffset.x - parentBounds.left)
            val distRight = abs((calculatedOffset.x + popupContentSize.width) - parentBounds.right)
            val isRightAligned = distRight < distLeft

            PopupLayoutPosition(showBelow = showBelow, showAbove = showAbove, isRightAligned = isRightAligned)
        }
    }

    val effectiveTransformOrigin = remember(
        popupContentSize,
        calculatedOffset,
        popupLayoutPosition,
        containerSize,
        predictedTransformOrigin,
    ) {
        if (popupContentSize == IntSize.Zero) {
            predictedTransformOrigin
        } else {
            val (showBelow, showAbove, isRightAligned) = popupLayoutPosition
            val cornerX = if (isRightAligned) {
                (calculatedOffset.x + popupContentSize.width).toFloat()
            } else {
                calculatedOffset.x.toFloat()
            }

            val showMiddle = !showBelow && !showAbove
            val topLeftY = calculatedOffset.y
            val cornerY = when {
                showMiddle -> (topLeftY + popupContentSize.height / 2f)
                showBelow -> topLeftY.toFloat()
                showAbove -> (topLeftY + popupContentSize.height).toFloat()
                else -> topLeftY.toFloat()
            }

            safeTransformOrigin(
                cornerX / containerSize.width.toFloat(),
                cornerY / containerSize.height.toFloat(),
            )
        }
    }

    val localTransformOrigin = remember(popupLayoutPosition, popupContentSize, calculatedOffset, parentBounds) {
        val (showBelow, _, isRightAligned) = popupLayoutPosition

        // COUI PopupMenuDomain.getMainMenuEnterPivotX: the scale pivot X is the anchor's
        // horizontal center clamped into the menu bounds — not the menu corner. Fall back to
        // the near corner until the popup has been measured.
        val pivotFractionX = if (popupContentSize == IntSize.Zero) {
            if (isRightAligned) 1f else 0f
        } else {
            ((parentBounds.left + parentBounds.width / 2f - calculatedOffset.x) / popupContentSize.width)
                .coerceIn(0f, 1f)
        }

        TransformOrigin(
            pivotFractionX = pivotFractionX,
            // COUI PopupMenuDomain.getMainMenuEnterPivotY is binary: the menu top edge when the
            // menu sits below the anchor, the bottom edge otherwise.
            pivotFractionY = if (showBelow) 0f else 1f,
        )
    }

    return ListPopupLayoutInfo(
        windowBounds = windowBounds,
        popupMargin = popupMargin,
        effectiveTransformOrigin = effectiveTransformOrigin,
        localTransformOrigin = localTransformOrigin,
        calculatedOffset = calculatedOffset,
        popupLayoutPosition = popupLayoutPosition,
    )
}

/**
 * The popup's Bezier gravity trajectory: COUI `PopupMenuDomain.computeMainMenuPopupPathAnchorOffset`
 * produces the path anchor, `PopupMotionTrajectoryUtil.snapshot` turns it into the offset from the
 * menu centre, and `BezierGravityPathMath.controlPoints` fits the arc between it and the menu centre.
 *
 * With the default `pathAnchorMode = PATH_ANCHOR_MODE_CENTER` the path anchor is the anchor's
 * horizontal centre on the menu-facing edge — the anchor bottom when the menu opens below it, the
 * anchor top when it opens above — offset along that axis by
 * [PopupGravityMotion.pathAnchorEdgeOffsetY]. The auto-corner anchor mode is not implemented.
 *
 * @param anchorBounds The anchor's bounds in window coordinates.
 * @param layoutInfo The resolved popup layout, supplying the placement direction and menu position.
 * @param popupContentSize The measured popup size; [IntSize.Zero] until the popup is measured, in
 *   which case the trajectory collapses to a degenerate straight line.
 */
fun popupGravityPath(
    anchorBounds: IntRect,
    layoutInfo: ListPopupLayoutInfo,
    popupContentSize: IntSize,
    gravityMotion: PopupGravityMotion = ListPopupDefaults.GravityMotion,
): BezierGravityPathGeometry {
    val menuCenter = Offset(
        layoutInfo.calculatedOffset.x + popupContentSize.width / 2f,
        layoutInfo.calculatedOffset.y + popupContentSize.height / 2f,
    )
    val anchorY = if (layoutInfo.popupLayoutPosition.showBelow) {
        anchorBounds.bottom + gravityMotion.pathAnchorEdgeOffsetY
    } else {
        anchorBounds.top - gravityMotion.pathAnchorEdgeOffsetY
    }
    val pathAnchor = Offset(anchorBounds.left + anchorBounds.width / 2f, anchorY)
    return BezierGravityPathMath.controlPoints(
        start = pathAnchor - menuCenter,
        end = Offset.Zero,
        config = gravityMotion.gravityConfig,
        p1T = gravityMotion.p1T,
        p2T = gravityMotion.p2T,
    )
}

/**
 * The scaling and fading container that hosts a list popup's content.
 *
 * @param popupContentSize The last reported size of the content, compared against the latest
 *   measurement to avoid redundant [onPopupContentSizeChange] callbacks.
 * @param onPopupContentSizeChange Called when the measured content size changes.
 * @param fractionProgress Provides the current scale fraction (0 → 1) of the popup.
 * @param alphaProgress Provides the current alpha (0 → 1) of the popup content.
 * @param pathProgress Provides the current Bezier trajectory parameter (0 → 1) of the popup.
 * @param gravityPath The trajectory built by [popupGravityPath]; the popup is translated along it.
 * @param popupLayoutPosition The [PopupLayoutPosition] describing the popup's spawn direction.
 * @param localTransformOrigin The transform origin local to the content used while scaling.
 * @param modifier The modifier to be applied to the popup container.
 * @param enableGlass Whether to draw the ColorOS 17 popup glass behind the content. Only the
 *   in-window overlay host enables it; the `Dialog` host has no coordinates in the host window for
 *   a backdrop to sample.
 * @param interactionEnabled Whether the mounted surface accepts touches and row actions. Hosts
 *   disable it as soon as dismissal starts, while retaining the surface for its exit animation.
 * @param content The content of the popup.
 */
@Composable
fun ListPopupContent(
    popupContentSize: IntSize,
    onPopupContentSizeChange: (IntSize) -> Unit,
    fractionProgress: () -> Float,
    alphaProgress: () -> Float,
    pathProgress: () -> Float,
    gravityPath: BezierGravityPathGeometry,
    popupLayoutPosition: PopupLayoutPosition,
    localTransformOrigin: TransformOrigin,
    modifier: Modifier = Modifier,
    enableGlass: Boolean = false,
    interactionEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val backgroundColor = popupSurfaceColor()
    val backdrop = if (enableGlass) LocalBackdrop.current else null
    val squircleEnabled = isSquircleEnabled()
    val glassShape = remember(PopupCornerRadius, squircleEnabled) {
        SquircleShape(cornerRadius = PopupCornerRadius, squircleEnabled = squircleEnabled)
    }
    val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
    val blurRadius = with(LocalDensity.current) { POPUP_BLUR_RADIUS_PX.toDp().value }
    val materialEdge = if (backdrop != null) {
        Modifier.materialShadowEdge(
            stroke = if (isDark) PopupStrokeDark else PopupStrokeLight,
            cornerRadius = PopupCornerRadius,
            cornerWeight = if (squircleEnabled) POPUP_CORNER_WEIGHT else 1f,
            edgeStyle = GradientStrokeStyle.EDGE_STYLE_2,
            shadowStyle = InnerShadowStyle.SHADOW_STYLE_2,
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val size = coordinates.size
                if (popupContentSize != size) onPopupContentSizeChange(size)
            }
            .couiTouchMotion(
                enabled = interactionEnabled,
                pressScaleEnabled = false,
                consumeDrag = false,
                lightEnabled = backdrop != null,
                shape = glassShape,
                lightParams = SpotLightDefaults.translucentLarge1(isDark),
                surface = Modifier
                    .popupOpacity(alphaProgress)
                    .dropShadow(shape = glassShape, shadow = PopupShadow)
                    .couiGlassSurface(
                        backdrop = backdrop,
                        shape = glassShape,
                        containerColor = backgroundColor,
                        fallback = Modifier.squircleSurface(color = backgroundColor, cornerRadius = PopupCornerRadius),
                        blurRadius = blurRadius,
                        blendColors = popupGlassBlendColors(),
                    )
                    .then(materialEdge),
            ) {
                // COUI's main menu scales a true 0 -> 1; only the small-screen sub-menu clip-reveals.
                val scale = fractionProgress()
                scaleX *= scale
                scaleY *= scale
                transformOrigin = localTransformOrigin
                // COUI COUIPopupTouchMotionAnimator.collect() leaves the scale pivot where
                // PopupMenuDomain.getMainMenuEnterPivotX/Y put it and folds the pivot compensation
                // into TRANSLATE_X/Y instead of moving it, so the two below always travel together:
                // size * (0.5 - pivotRatio) * (1 - scale) cancels the pivot and lets the trajectory
                // start at the path anchor rather than at the pivot point.
                val pathPoint = gravityPath.pointAt(pathProgress())
                translationX += pathPoint.x + size.width * (0.5f - localTransformOrigin.pivotFractionX) * (1f - scale)
                translationY += pathPoint.y + size.height * (0.5f - localTransformOrigin.pivotFractionY) * (1f - scale)
            },
    ) {
        CompositionLocalProvider(LocalPopupInteractionEnabled provides interactionEnabled) {
            content()
        }
    }
}

@Composable
fun ListPopupContent(
    popupContentSize: IntSize,
    onPopupContentSizeChange: (IntSize) -> Unit,
    fractionProgress: () -> Float,
    alphaProgress: () -> Float,
    popupLayoutPosition: PopupLayoutPosition,
    localTransformOrigin: TransformOrigin,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // The legacy overload had no gravity trajectory; a degenerate path keeps the content in place.
    ListPopupContent(
        popupContentSize = popupContentSize,
        onPopupContentSizeChange = onPopupContentSizeChange,
        fractionProgress = fractionProgress,
        alphaProgress = alphaProgress,
        pathProgress = { 0f },
        gravityPath = BezierGravityPathGeometry(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        popupLayoutPosition = popupLayoutPosition,
        localTransformOrigin = localTransformOrigin,
        modifier = modifier,
        content = content,
    )
}

/**
 * Directional clip-reveal used during popup enter/exit. The visible band grows along the popup's
 * spawn direction encoded by [popupLayoutPosition] as [fractionProgress] moves 0 → 1: from the top
 * when shown below the anchor, from the bottom when shown above, and outwards from the center
 * otherwise. The band itself is shaped as a squircle (via [addSquircleRect]) so the four corners
 * stay aligned with the surrounding [squircleSurface] / `squircleClip` during reveal — when
 * [squircleEnabled] is `false`, [addSquircleRect] falls back to a plain rounded rectangle to
 * match the squircle modifiers' fallback rendering.
 */
internal fun Modifier.popupClipReveal(
    fractionProgress: () -> Float,
    popupLayoutPosition: PopupLayoutPosition,
    cornerRadius: Dp,
    squircleEnabled: Boolean,
): Modifier = drawWithCache {
    val path = Path()
    val showBelow = popupLayoutPosition.showBelow
    val showAbove = popupLayoutPosition.showAbove
    onDrawWithContent {
        // Clamp — source spring overshoots; an oversized reveal path would cut downstream content.
        val progress = fractionProgress().coerceIn(0f, 1f)
        if (progress <= 0f) return@onDrawWithContent

        val height = size.height
        val visibleHeight = height * progress
        if (visibleHeight <= 0f) return@onDrawWithContent

        val clipStart = when {
            showBelow -> 0f
            showAbove -> height * (1f - progress)
            else -> height * (0.5f - 0.5f * progress)
        }

        path.rewind()
        path.addSquircleRect(
            width = size.width,
            height = visibleHeight,
            cornerRadius = cornerRadius.toPx(),
            squircleEnabled = squircleEnabled,
        )
        if (clipStart == 0f) {
            clipPath(path) {
                this@onDrawWithContent.drawContent()
            }
        } else {
            translate(top = clipStart) {
                clipPath(path) {
                    translate(top = -clipStart) {
                        this@onDrawWithContent.drawContent()
                    }
                }
            }
        }
    }
}
