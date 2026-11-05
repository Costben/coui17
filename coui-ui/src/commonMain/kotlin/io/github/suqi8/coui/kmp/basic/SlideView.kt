// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.anim.COUIMoveEase
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * An action revealed behind a [SlideView] row, `COUISlideMenuItem`.
 *
 * @param text the label, drawn centred in the action.
 * @param icon an icon drawn centred in the action at its intrinsic size. ColorOS's own delete
 *   action is the 24dp `coui_slide_view_delete` icon with no label.
 * @param contentDescription what accessibility services read for the action; defaults to [text].
 * @param color the action's fill; [Color.Unspecified] takes [SlideViewColors.menuColor].
 * @param width the minimum width; a longer label widens the action to its text plus
 *   [SlideViewDefaults.TextPadding] on both sides, as `COUISlideView.addItem` does.
 */
@Immutable
@ExperimentalCOUIApi
data class SlideMenuItem(
    val text: String?,
    val icon: ImageVector? = null,
    val contentDescription: String? = text,
    val color: Color = Color.Unspecified,
    val width: Dp = SlideViewDefaults.MenuItemWidth,
)

/** The colours of a [SlideView]. */
@Immutable
@ExperimentalCOUIApi
data class SlideViewColors(
    val menuColor: Color,
    val textColor: Color,
    val iconColor: Color,
)

/**
 * The position of a [SlideView]: how far its content has slid aside, and whether it is being
 * deleted.
 */
@Stable
@ExperimentalCOUIApi
class SlideViewState(initiallyOpen: Boolean = false) {
    /** `getSlideViewScrollX()` in layout-direction terms: 0 when closed, positive towards the menu. */
    internal val scroll = Animatable(0f)

    /** `mHolderWidth`, the total width of the actions, published by [SlideView]. */
    internal var holderWidthPx by mutableFloatStateOf(0f)

    /** The row width, published by [SlideView]. */
    internal var rowWidthPx by mutableFloatStateOf(0f)

    /** The share of the row height still shown while a delete collapses the row. */
    internal var heightFraction by mutableFloatStateOf(1f)

    /** `mSlideDelete`: the delete was started by sliding past the menu, not by [delete]. */
    internal var slideDelete by mutableStateOf(false)

    /** `mUpScrollX`: the offset the finger left the row at when it started a slide delete. */
    internal var upScrollPx by mutableFloatStateOf(0f)

    /** Whether a delete animation is running or has finished. */
    var isDeleting by mutableStateOf(false)
        internal set

    internal var openTarget by mutableStateOf(initiallyOpen)

    /** Whether the menu is open or opening (`SLIDE_STATUS_ON`). */
    val isOpen: Boolean get() = openTarget

    /** Opens the menu with the spring `COUISlideView.animationScrollTo` runs on release. */
    suspend fun open() {
        openTarget = true
        scroll.animateTo(holderWidthPx, SlideViewDefaults.ReleaseSpring)
    }

    /** `COUISlideView.shrink`: closes the menu with the short `Scroller` glide. */
    suspend fun close() {
        openTarget = false
        smoothScrollTo(0f)
    }

    /**
     * `COUISlideView.startDeleteAnimation`: the content slides out across the row while the row
     * collapses to nothing. It returns once the row has collapsed; remove the item from its list
     * then. The state stays deleting, so a reused state must be replaced.
     */
    suspend fun delete() {
        if (isDeleting) return
        isDeleting = true
        runDelete(from = holderWidthPx)
    }

    internal suspend fun slideDelete() {
        isDeleting = true
        slideDelete = true
        upScrollPx = scroll.value
        runDelete(from = scroll.value)
    }

    private suspend fun runDelete(from: Float) = coroutineScope {
        // COUISlideDeleteAnimation: the scroll and the height animators start together; the height
        // waits ANIMATION_DELAY first and runs longer.
        launch {
            scroll.snapTo(from)
            scroll.animateTo(rowWidthPx, tween(SlideViewDefaults.DeleteSlideDurationMillis, easing = COUIMoveEase))
        }
        launch {
            delay(SlideViewDefaults.DeleteCollapseDelayMillis.toLong())
            Animatable(1f).animateTo(0f, tween(SlideViewDefaults.DeleteCollapseDurationMillis, easing = COUIMoveEase)) {
                heightFraction = value
            }
        }
    }

    /**
     * `COUISlideView.smoothScrollTo`: a `Scroller` run of three milliseconds per pixel, capped at
     * 200ms, on the row's `PathInterpolator(0.133, 0, 0.3, 1)`.
     */
    internal suspend fun smoothScrollTo(target: Float) {
        val duration = min(abs(target - scroll.value).roundToInt() * 3, SlideViewDefaults.SmoothScrollMaxDurationMillis)
        scroll.animateTo(target, tween(duration, easing = SlideViewDefaults.SmoothScrollEasing))
    }

    companion object {
        /** Saves whether the menu is open, not an unfinished drag. */
        val Saver = Saver<SlideViewState, Boolean>(
            save = { it.isOpen && !it.isDeleting },
            restore = { SlideViewState(it) },
        )
    }
}

/** Remembers a [SlideViewState] across recreation. */
@Composable
@ExperimentalCOUIApi
fun rememberSlideViewState(initiallyOpen: Boolean = false): SlideViewState = rememberSaveable(saver = SlideViewState.Saver) { SlideViewState(initiallyOpen) }

/**
 * A list row that slides aside to reveal actions behind it, ColorOS `COUISlideView` in its
 * `MENU_ITEM_NORMAL_RECT_STYLE`, the style ColorOS 17 Calendar uses for custom reminders.
 *
 * Swiping towards the start reveals [menuItems], the first one at the end edge; the fill runs the
 * full row height with square corners. Dragging past the menu is damped, and the actions spread to
 * keep their labels centred in the growing fill. On release the row springs open or closed; a tap
 * on the content of an open row closes it, and a tap on an action calls [onMenuItemClick].
 *
 * Call [SlideViewState.delete] to play the delete animation, then remove the item. With
 * [fastDeleteEnabled], sliding far enough past the menu deletes the row by itself and then calls
 * [onFastDelete].
 *
 * @param menuItems the actions, from the end edge inwards.
 * @param onMenuItemClick called with the index of the tapped action.
 * @param modifier the modifier for the row.
 * @param state the row position.
 * @param enabled whether the row can be slid.
 * @param fastDeleteEnabled `enableFastDelete`: whether sliding past the menu deletes the row.
 * @param onFastDelete called once a fast delete has collapsed the row.
 * @param colors the action fill and label colours.
 * @param textStyle the action label style.
 * @param content the row content. Give it its own background; the actions are drawn behind it.
 */
@Composable
@ExperimentalCOUIApi
fun SlideView(
    menuItems: List<SlideMenuItem>,
    onMenuItemClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    state: SlideViewState = rememberSlideViewState(),
    enabled: Boolean = true,
    fastDeleteEnabled: Boolean = false,
    onFastDelete: () -> Unit = {},
    colors: SlideViewColors = SlideViewDefaults.slideViewColors(),
    textStyle: TextStyle = SlideViewDefaults.textStyle(),
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val textMeasurer = rememberTextMeasurer()
    val scope = rememberCoroutineScope()
    val currentOnMenuItemClick by rememberUpdatedState(onMenuItemClick)
    val currentOnFastDelete by rememberUpdatedState(onFastDelete)
    val currentFastDelete by rememberUpdatedState(fastDeleteEnabled)

    val labelStyle = textStyle.merge(TextStyle(color = colors.textColor))
    val layouts = remember(menuItems, labelStyle, textMeasurer) {
        menuItems.map { item -> item.text?.let { textMeasurer.measure(it, labelStyle) } }
    }
    val itemWidths = with(density) {
        val padding = SlideViewDefaults.TextPadding.toPx()
        menuItems.mapIndexed { i, item -> max(item.width.toPx(), (layouts[i]?.size?.width ?: 0) + 2 * padding) }
    }
    state.holderWidthPx = itemWidths.sum()
    val painters = menuItems.map { item -> item.icon?.let { rememberVectorPainter(it) } }
    val itemColors = menuItems.map { if (it.color.isSpecified()) it.color else colors.menuColor }
    val singleFill = itemColors.distinct().size == 1
    val currentItemWidths by rememberUpdatedState(itemWidths)

    Box(
        modifier = modifier
            // The clip sits outside the collapse, so it cuts the full-height row and its actions to
            // the shrinking height, the way the native row shrinks its content view's height.
            .clipToBounds()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                state.rowWidthPx = placeable.width.toFloat()
                val height = (placeable.height * state.heightFraction).roundToInt()
                layout(placeable.width, height) { placeable.place(0, 0) }
            }
            .semantics {
                customActions = menuItems.mapIndexed { i, item ->
                    CustomAccessibilityAction(item.contentDescription ?: item.text ?: "") {
                        currentOnMenuItemClick(i)
                        true
                    }
                }
            }
            .drawBehind {
                val s = state.scroll.value
                if (s <= 0f || menuItems.isEmpty()) return@drawBehind
                val w = size.width
                val h = state.holderWidthPx
                val n = menuItems.size

                // Positions are worked out start-to-end in LTR terms and mirrored for RTL.
                fun mirror(x: Float, width: Float) = if (rtl) w - x - width else x
                // The overscroll the actions share out. A delete started by [SlideViewState.delete]
                // keeps sharing it, which keeps a lone label centred in the fill as it widens.
                val extra = if (s > h) s - h else 0f
                if (singleFill) {
                    drawRect(itemColors[0], Offset(mirror(w - s, s), 0f), Size(s, size.height))
                }
                for (i in 0 until n) {
                    val left = slideMenuItemStart(i, s, w, h, itemWidths, state.slideDelete, state.upScrollPx)
                    val itemWidth = itemWidths[i]
                    if (!singleFill) {
                        // COUISlideView.drawItemBackground: each fill also takes half of its share of
                        // the overscroll on either side, so neighbouring fills still meet.
                        val share = (extra / (n + 1f)).roundToInt().toFloat()
                        val (start, end) = when (i) {
                            0 -> left - share / 2f to w
                            n - 1 -> left - share to left + itemWidth + share / 2f
                            else -> left - share / 2f to left + itemWidth + share / 2f
                        }
                        drawRect(itemColors[i], Offset(mirror(start, end - start), 0f), Size(end - start, size.height))
                    }
                    val centre = left + itemWidth / 2f
                    val painter = painters[i]
                    if (painter != null) {
                        val iconSize = painter.intrinsicSize
                        val x = mirror(centre - iconSize.width / 2f, iconSize.width)
                        translate(x, (size.height - iconSize.height) / 2f) {
                            with(painter) { draw(iconSize, colorFilter = ColorFilter.tint(colors.iconColor)) }
                        }
                    }
                    val layout = layouts[i]
                    if (layout != null) {
                        val x = mirror(centre - layout.size.width / 2f, layout.size.width.toFloat())
                        drawText(layout, topLeft = Offset(x, (size.height - layout.size.height) / 2f))
                    }
                }
            }
            .pointerInput(state, enabled, rtl) {
                if (!enabled) return@pointerInput
                val slop = viewConfiguration.touchSlop
                val quickDeletePx = SlideViewDefaults.QuickDeleteDistance.toPx()
                val overSlideDeletePx = SlideViewDefaults.OverSlideDeleteDistance.toPx()
                val dir = if (rtl) -1f else 1f
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    if (state.isDeleting) {
                        down.consume()
                        return@awaitEachGesture
                    }
                    val itemWidths = currentItemWidths
                    // The gesture tracks the offset itself: the snaps it launches land a frame later.
                    var current = state.scroll.value
                    scope.launch { state.scroll.stop() }
                    val w = size.width.toFloat()
                    val h = state.holderWidthPx
                    val open = current >= h - 0.5f && h > 0f
                    // An open row keeps the touch: its content must not see a tap meant to close it.
                    val startX = if (rtl) w - down.position.x else down.position.x
                    val onMenu = open && startX > w - h
                    if (open) down.consume()
                    val tracker = VelocityTracker()
                    tracker.addPosition(down.uptimeMillis, down.position)
                    var dragging = false
                    var lastX = down.position.x
                    var total = Offset.Zero
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        tracker.addPosition(change.uptimeMillis, change.position)
                        if (!change.pressed) {
                            if (dragging) {
                                change.consume()
                                release(state, current, tracker.calculateVelocity().x * dir, currentFastDelete, quickDeletePx, overSlideDeletePx, scope) {
                                    currentOnFastDelete()
                                }
                            } else if (open) {
                                change.consume()
                                val upX = if (rtl) w - change.position.x else change.position.x
                                val index = if (onMenu && upX > w - h) menuIndexAt(upX - (w - h), itemWidths) else -1
                                if (index >= 0) currentOnMenuItemClick(index) else scope.launch { state.close() }
                            }
                            break
                        }
                        total += change.positionChange()
                        if (!dragging) {
                            if (abs(total.x) > slop && abs(total.x) * 0.5f > abs(total.y)) {
                                dragging = true
                                // onInterceptTouchEvent: the row jumps by 3/7 (4/7 while closed with
                                // several actions) of the travel so far, then follows the finger
                                // from the touch slop on.
                                val k = if (current >= h || itemWidths.size == 1) 3f / 7f else 4f / 7f
                                current = (current - total.x * dir * k).coerceIn(0f, h)
                                val jumped = current
                                scope.launch { state.scroll.snapTo(jumped) }
                                lastX = down.position.x + if (total.x > 0) slop else -slop
                            } else if (abs(total.y) > slop) {
                                break
                            }
                        }
                        if (dragging) {
                            change.consume()
                            var delta = (change.position.x - lastX) * dir
                            lastX = change.position.x
                            val s = current
                            if (s >= h) {
                                // COUIPhysicalAnimationUtil.calcRealOverScrollDist over the rest of
                                // the row: 40% of the travel, falling to nothing at the far edge.
                                delta = delta * (1f - min(abs(s - h) / (w - h), 1f)) / 5f * 2f
                            }
                            current = max(s - delta, 0f)
                            val next = current
                            scope.launch { state.scroll.snapTo(next) }
                        }
                    }
                }
            },
    ) {
        Box(
            Modifier.graphicsLayer {
                translationX = -state.scroll.value * if (rtl) -1f else 1f
            },
        ) {
            content()
        }
    }
}

/**
 * Where action [index] starts, in LTR px from the row's start edge, while the content has slid by
 * [scroll]: `COUISlideView.drawItemBackground`. The first action sits at the end edge; past the menu
 * each action takes its share of the overscroll. During a slide delete the shares are frozen at the
 * offset the finger left, [upScroll], and grow towards the end edge as the content slides out.
 */
internal fun slideMenuItemStart(
    index: Int,
    scroll: Float,
    rowWidth: Float,
    holderWidth: Float,
    itemWidths: List<Float>,
    slideDelete: Boolean = false,
    upScroll: Float = 0f,
): Float {
    val n = itemWidths.size
    val base = if (slideDelete) {
        val share = (upScroll - holderWidth) * (n - index) / (n + 1)
        val travel = rowWidth - upScroll
        travel + share + if (travel != 0f) (scroll - upScroll) * share / travel else 0f
    } else {
        val extra = if (scroll > holderWidth) scroll - holderWidth else 0f
        (rowWidth - scroll) + (n - index) * extra / (n + 1)
    }
    return base + itemWidths.subList(index + 1, n).sum()
}

/** What a released [SlideView] does next. */
internal enum class SlideRelease { Close, Open, Delete }

/** `COUISlideView.onTouchEvent` `ACTION_UP`, with [velocityX] in px/s, positive towards the end. */
internal fun slideReleaseTarget(
    scroll: Float,
    holderWidth: Float,
    velocityX: Float,
    fastDeleteEnabled: Boolean,
    quickDeletePx: Float,
    overSlideDeletePx: Float,
): SlideRelease {
    val fling = SlideViewDefaults.FlingVelocity
    if (fastDeleteEnabled) {
        // The fast-delete branch ignores the fling for opening and closing; only the distance counts.
        if ((velocityX < -fling && scroll > holderWidth + quickDeletePx) || scroll > holderWidth + overSlideDeletePx) {
            return SlideRelease.Delete
        }
        return if (scroll > holderWidth / 2f) SlideRelease.Open else SlideRelease.Close
    }
    val open = when {
        velocityX < -fling -> true
        velocityX > fling -> false
        else -> scroll > holderWidth / 2f
    }
    return if (open) SlideRelease.Open else SlideRelease.Close
}

/** The index of the action under [offset] px into the menu, start-to-end, or -1. */
internal fun menuIndexAt(offset: Float, widths: List<Float>): Int {
    // The menu runs from the last action at its start to the first at the end edge.
    var x = 0f
    for (i in widths.indices.reversed()) {
        if (offset < x + widths[i]) return i
        x += widths[i]
    }
    return -1
}

/** Settles a released drag on [slideReleaseTarget]. */
private fun release(
    state: SlideViewState,
    s: Float,
    velocityX: Float,
    fastDeleteEnabled: Boolean,
    quickDeletePx: Float,
    overSlideDeletePx: Float,
    scope: CoroutineScope,
    onFastDelete: () -> Unit,
) {
    when (slideReleaseTarget(s, state.holderWidthPx, velocityX, fastDeleteEnabled, quickDeletePx, overSlideDeletePx)) {
        SlideRelease.Delete -> scope.launch {
            state.slideDelete()
            onFastDelete()
        }

        SlideRelease.Open -> state.settleTo(true, scope)

        SlideRelease.Close -> state.settleTo(false, scope)
    }
}

private fun SlideViewState.settleTo(open: Boolean, scope: CoroutineScope) {
    openTarget = open
    scope.launch { scroll.animateTo(if (open) holderWidthPx else 0f, SlideViewDefaults.ReleaseSpring) }
}

private fun Color.isSpecified() = this != Color.Unspecified

/** Values from the ColorOS 17 `COUISlideView` resources and animators. */
@ExperimentalCOUIApi
object SlideViewDefaults {
    /** `coui_slideview_menuitem_width`. */
    val MenuItemWidth = 80.dp

    /** `coui_slide_view_text_padding`, on either side of a label that outgrows [MenuItemWidth]. */
    val TextPadding = 12.dp

    /** `coui_slideview_over_slide_delete`: how far past the menu a slow release deletes. */
    val OverSlideDeleteDistance = 48.dp

    /** `coui_slideview_quick_delete`: how far past the menu a fling deletes. */
    val QuickDeleteDistance = 30.dp

    /** The `VelocityTracker` speed, in px/s, past which a release counts as a fling. */
    val FlingVelocity = 1000f

    /** `animationScrollTo`: a `SpringForce` with damping ratio 1 and stiffness 200. */
    val ReleaseSpring = spring<Float>(dampingRatio = 1f, stiffness = 200f, visibilityThreshold = 0.75f)

    /** The row `Scroller`'s `PathInterpolator(0.133, 0, 0.3, 1)`. */
    val SmoothScrollEasing: Easing = CubicBezierEasing(0.133f, 0f, 0.3f, 1f)

    /** `smoothScrollTo` caps its `Scroller` run at 200ms. */
    val SmoothScrollMaxDurationMillis = 200

    /** `COUISlideDeleteAnimation.SLIDE_VIEW_DURATION`. */
    val DeleteSlideDurationMillis = 330

    /** `COUISlideDeleteAnimation.ITEM_VIEW_DURATION`. */
    val DeleteCollapseDurationMillis = 400

    /** `COUISlideDeleteAnimation.ANIMATION_DELAY`. */
    val DeleteCollapseDelayMillis = 10

    /**
     * The action colours: `couiColorError` for the fill, the style's default `itemBackgroundColor`;
     * `coui_slideview_textcolor` (#F9F9F9 in both themes) for the labels; and white for the icons,
     * which `COUISlideView` draws untinted and `coui_slide_view_delete` fills with #FFFFFF.
     */
    @Composable
    fun slideViewColors(
        menuColor: Color = COUITheme.colorScheme.error,
        textColor: Color = Color(0xFFF9F9F9),
        iconColor: Color = Color.White,
    ): SlideViewColors = remember(menuColor, textColor, iconColor) { SlideViewColors(menuColor, textColor, iconColor) }

    /**
     * `coui_slide_view_text_size`, 15sp, with the 1.15x font-scale cap of
     * `COUIChangeTextUtil.getSuitableFontSize(size, fontScale, 2)`.
     */
    @Composable
    fun textStyle(): TextStyle {
        val fontScale = LocalDensity.current.fontScale
        return COUITheme.textStyles.main.copy(fontSize = cappedLabelFontSizeSp(15f, fontScale).sp)
    }
}
