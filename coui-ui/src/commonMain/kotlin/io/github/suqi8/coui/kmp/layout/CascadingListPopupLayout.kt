// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.layout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import io.github.suqi8.coui.kmp.anim.folmeSpring
import io.github.suqi8.coui.kmp.basic.DropdownColors
import io.github.suqi8.coui.kmp.basic.DropdownDefaults
import io.github.suqi8.coui.kmp.basic.DropdownEntry
import io.github.suqi8.coui.kmp.basic.DropdownItem
import io.github.suqi8.coui.kmp.basic.ListPopupDefaults
import io.github.suqi8.coui.kmp.basic.ListPopupLayoutInfo
import io.github.suqi8.coui.kmp.basic.LocalPopupInteractionEnabled
import io.github.suqi8.coui.kmp.basic.PopupCornerRadius
import io.github.suqi8.coui.kmp.basic.PopupPositionProvider
import io.github.suqi8.coui.kmp.basic.popupSurfaceColor
import io.github.suqi8.coui.kmp.basic.rememberListPopupLayoutInfo
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.LocalDismissState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.time.TimeSource

/** Corner radius of the cascading popup container, shared with [PopupCornerRadius] (28dp squircle). */
internal val CascadingPopupCornerRadius = PopupCornerRadius

/**
 * Vertical overlap between the anchor row's top edge and the secondary menu's top edge.
 * COUI `coui_popup_list_window_vertical_overlap_between_main_and_sub_menu`.
 */
internal val MainSubMenuVerticalOverlap = 14.dp

/**
 * Minimum gap reserved above the secondary menu inside the available area.
 * COUI `coui_popup_list_window_min_gap_to_top`.
 */
internal val SubMenuMinGapToTop = 12.dp

/**
 * Vertical padding the popup list keeps above its first and below its last row
 * (COUI `coui_popup_list_padding_vertical`). The secondary menu's band clip starts inside it.
 */
internal val SubMenuInnerPadding = 8.dp

/**
 * Minimum width of a menu that owns submenus (COUI `coui_popup_list_window_width_with_icon`).
 * A plain menu floors at `ListPopupDefaults.MinWidth` (168dp) instead.
 */
private val PopupSubMenuMinWidth = 196.dp

/**
 * COUI `SpringProgressUnit`: `startAnimation(bounce, response, …)` with bounce 0 / response 0.35
 * for both submenu enter and exit (SmallScreenAnimationController).
 */
private const val SUBMENU_SPRING_DAMPING = 1f
private const val SUBMENU_SPRING_RESPONSE = 0.35f

/**
 * Rubber-band travel of the panel (`TouchMotionTierParams.iolMaxStretchDistance`, 28dp) — the
 * asymptote of the curve below, i.e. how far the menu can be pulled before the drag stops moving it.
 */
private val PanelDragMaxStretch = 28.dp

/**
 * Rubber-curve ratio of the panel (`TouchMotionTierParams.iolRubberCurveRatio`, 0.05). Feeds
 * [panelDragRubber], which saturates at [PanelDragMaxStretch] instead of following the finger.
 */
private const val PANEL_DRAG_CURVE_RATIO = 0.05f

/** Cap on the release velocity fed back into the hand-up spring (`iolHandUpMaxInitialVelocity`). */
private const val PANEL_DRAG_MAX_VELOCITY = 1000f

/**
 * `COUIVelocityTrackerHelper.mVelocityScale` — COUI scales the tracker's px/s by this before the
 * hand-up path sees it.
 */
private const val PANEL_DRAG_VELOCITY_SCALE = 0.3f

/**
 * A release whose last movement sample is older than this counts as a stopped finger: COUI reads its
 * velocity tracker at ACTION_UP, and a finger already resting before it lifted contributes none.
 */
private const val PANEL_DRAG_VELOCITY_STALE_MS = 100L

/**
 * Samples [PanelVelocityTracker] keeps. The horizon above is the binding limit at 60Hz; this only
 * stops a panic-driven event burst from growing the window without bound.
 */
private const val PANEL_DRAG_VELOCITY_SAMPLES = 8

/**
 * Spring the panel follows the finger with (`TouchMotionTierParams.iolDragSpring`, response 0.15 /
 * bounce 0.15). `COUISpringForce.setBounce(b)` sets `dampingRatio = 1 - b` and `setResponse(r)`
 * sets `stiffness = (2π / r)²`, which is exactly [folmeSpring]'s conversion.
 */
private val PanelDragFollowSpring =
    folmeSpring<Float>(damping = 0.85f, response = 0.15f, visibilityThreshold = 0.5f)

/** `TouchMotionTierParams.iolHandUpSpring` — response 0.65, bounce 0.42 (dampingRatio 0.58). */
private const val PANEL_HAND_UP_RESPONSE = 0.65f
private const val PANEL_HAND_UP_BOUNCE = 0.42f

/** `TouchMotionTierParams.iolHandUpSpringLargeArea` — response 0.65, bounce 0.45. */
private const val PANEL_HAND_UP_BOUNCE_LARGE_AREA = 0.45f

/** `iolHandUpSmallAreaRefWidthDp` / `iolHandUpLargeAreaRefWidthDp`, the area ramp's endpoints. */
private val PanelHandUpSmallAreaRefWidth = 56.dp
private val PanelHandUpLargeAreaRefWidth = 100.dp

/** `TouchMotionTierParams.iolHandUpAreaBezier`, a cubic-bezier easing `(x1, y1, x2, y2)`. */
private val PanelHandUpAreaBezier = CubicBezierEasing(0.55026454f, 0f, 0.8915344f, 0.18783069f)

/**
 * `TouchMotionHandUpSpringResolver.resolve` — the hand-up bounce attenuates with the popup's area
 * between the 56dp and 100dp reference squares through [PanelHandUpAreaBezier]; the response is
 * 0.65 at both ends of the ramp.
 */
internal fun panelHandUpAreaBounce(areaPx: Float, density: Density): Float {
    val smallArea = with(density) { PanelHandUpSmallAreaRefWidth.toPx().let { it * it } }
    val largeArea = with(density) { PanelHandUpLargeAreaRefWidth.toPx().let { it * it } }
    val ramp = if (largeArea <= smallArea) {
        1f
    } else {
        PanelHandUpAreaBezier.transform(
            ((areaPx - smallArea) / (largeArea - smallArea)).coerceIn(0f, 1f),
        )
    }
    return PANEL_HAND_UP_BOUNCE + (PANEL_HAND_UP_BOUNCE_LARGE_AREA - PANEL_HAND_UP_BOUNCE) * ramp
}

/** The hand-up spring for a given area bounce and release velocity, in [folmeSpring]'s terms. */
private fun panelHandUpSpring(areaBounce: Float, initialVelocity: Float): SpringSpec<Float> = folmeSpring<Float>(
    damping = 1f - panelHandUpBounce(initialVelocity, areaBounce),
    response = PANEL_HAND_UP_RESPONSE,
    visibilityThreshold = 0.5f,
)

/**
 * `COUIITouchMotionUnit.computeHandUpBounce` under `useNormalizedHandUpVelocity` (the shipping
 * default): the spring's bounce is the larger of the area-resolved value and the release velocity
 * normalised by `iolHandUpMaxInitialVelocity`, so a flick springs back harder than a slow release.
 */
internal fun panelHandUpBounce(initialVelocity: Float, areaBounce: Float): Float = maxOf(abs(initialVelocity) / PANEL_DRAG_MAX_VELOCITY, areaBounce).coerceAtMost(1f)

/** COUI SmallScreenAnimationController.mMainMenuScalePercent. */
private const val PRIMARY_SHRUNK_SCALE = 0.9f

/** COUI SmallScreenAnimationController.DEFAULT_MAIN_MENU_ALPHA_PERCENT (small screen: 0.3). */
private const val PRIMARY_SHRUNK_ALPHA = 0.3f
internal const val ENTER_SCALE_FROM = 0.15f
internal const val ENTER_SCALE_RANGE = 1f - ENTER_SCALE_FROM

/**
 * Popup content bounds in the dismiss layer's local coordinates.
 *
 * A plain holder rather than a state: it is written from the sub-layout's measure/layout pass and
 * read inside the dismiss gesture, so making it observable would only add a needless invalidation
 * (and risk a measure loop).
 */
private class PopupContentBounds {
    var rect: IntRect = IntRect.Zero
}

/**
 * Upward shift the primary menu takes when the secondary would otherwise leave less than
 * [SubMenuMinGapToTop] above itself (COUI `SmallScreenAnimationController.startSubMenuEnter`).
 * Written from the measure pass, read by the primary's `graphicsLayer`, hence not a state.
 */
private class PopupShift {
    var y: Float = 0f
}

/**
 * Internal shared layout for cascading list popups. Cascading depth is limited to 2.
 *
 * ColorOS system apps — the Gallery among them — lay their popups out with the panel's *physical*
 * density, so a display-size override only changes third-party apps. Rendering this popup at the
 * app density would make every row, font and padding about 10% larger than the popup it mimics on a
 * phone whose density was raised above the panel's. [popupPhysicalDensity] restores the native
 * geometry; on a device without an override it is the identity.
 *
 * [popupHost] receives the mounted state, a `passTouches` probe and the content. `passTouches` turns
 * true the moment the close starts: the panel keeps shrinking into its anchor, but a host that blocks
 * touches outside the panel has to stop doing so right away, so the page beneath can already be
 * dragged or swiped while the exit plays (see [ListPopupLayout]).
 */
@Composable
internal fun CascadingListPopupLayout(
    show: Boolean,
    popupHost: @Composable (visible: Boolean, passTouches: () -> Boolean, content: @Composable () -> Unit) -> Unit,
    entries: List<DropdownEntry>,
    onDismissRequest: () -> Unit,
    dropdownColors: DropdownColors,
    popupModifier: Modifier = Modifier,
    onDismissFinished: (() -> Unit)? = null,
    popupPositionProvider: PopupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
    alignment: PopupPositionProvider.Align = PopupPositionProvider.Align.End,
    enableWindowDim: Boolean = false,
    maxHeight: Dp? = null,
    minWidth: Dp = ListPopupDefaults.MinWidth,
    collapseOnSelection: Boolean = true,
    enableGlass: Boolean = false,
) {
    val density = LocalDensity.current
    val popupDensity = remember(density) { popupPhysicalDensity(density) }
    CompositionLocalProvider(LocalDensity provides popupDensity) {
        CascadingListPopupLayoutImpl(
            show = show,
            popupHost = popupHost,
            entries = entries,
            onDismissRequest = onDismissRequest,
            dropdownColors = dropdownColors,
            popupModifier = popupModifier,
            onDismissFinished = onDismissFinished,
            popupPositionProvider = popupPositionProvider,
            alignment = alignment,
            enableWindowDim = enableWindowDim,
            maxHeight = maxHeight,
            minWidth = minWidth,
            collapseOnSelection = collapseOnSelection,
            enableGlass = enableGlass,
        )
    }
}

/**
 * `ro.sf.lcd_density` — the panel's density, which ColorOS system apps use for their popups while
 * third-party apps follow the display-size override. Falls back to [base] when the property is
 * missing or implausible.
 */
internal fun popupPhysicalDensity(base: Density): Density {
    val physicalDpi = platformPhysicalDensityDpi() ?: return base
    val currentDpi = (base.density * 160f).roundToInt()
    if (physicalDpi <= 0 || currentDpi <= 0 || physicalDpi == currentDpi) return base
    val ratio = physicalDpi.toFloat() / currentDpi
    // A broken property must not make the menu unusable.
    if (ratio < 0.75f || ratio > 1.25f) return base
    return Density(base.density * ratio, base.fontScale)
}

@Composable
private fun CascadingListPopupLayoutImpl(
    show: Boolean,
    popupHost: @Composable (visible: Boolean, passTouches: () -> Boolean, content: @Composable () -> Unit) -> Unit,
    entries: List<DropdownEntry>,
    onDismissRequest: () -> Unit,
    dropdownColors: DropdownColors,
    popupModifier: Modifier = Modifier,
    onDismissFinished: (() -> Unit)? = null,
    popupPositionProvider: PopupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
    alignment: PopupPositionProvider.Align = PopupPositionProvider.Align.End,
    enableWindowDim: Boolean = true,
    maxHeight: Dp? = null,
    minWidth: Dp = ListPopupDefaults.MinWidth,
    collapseOnSelection: Boolean = true,
    enableGlass: Boolean = false,
) {
    val internalVisible = remember { mutableStateOf(false) }
    val enterFraction = remember { Animatable(0f) }
    val enterAlpha = remember { Animatable(0f) }
    val dimProgress = remember { Animatable(0f) }
    // Two tracks: a depth-0 gesture may start while the depth-1 collapse is still settling.
    val backEnterProgress = remember { Animatable(0f) }
    val backExpandProgress = remember { Animatable(0f) }

    var expandedItem by remember { mutableStateOf<DropdownItem?>(null) }
    // Outlives [expandedItem] until the collapse spring settles so the secondary stays subcomposed.
    var displayedItem by remember { mutableStateOf<DropdownItem?>(null) }
    val popupContentBounds = remember { PopupContentBounds() }
    val expandFraction = remember { Animatable(0f) }
    val primaryScale = remember { Animatable(1f) }
    val primaryAlpha = remember { Animatable(1f) }

    val layoutDirection = LocalLayoutDirection.current

    val currentOnDismiss by rememberUpdatedState(onDismissRequest)
    val currentShow by rememberUpdatedState(show)
    // Hand touches back the moment the close starts, as ListPopupLayout does: the exit springs run
    // for a third of a second, and a popup that is merely shrinking into its anchor must not hold
    // every drag on the page beneath until they settle. Derived, so only the flip recomposes.
    val passTouches by remember { derivedStateOf { !currentShow } }
    val currentOnDismissFinished by rememberUpdatedState(onDismissFinished)
    val coroutineScope = rememberCoroutineScope()
    // Defer enter until both are measured — otherwise transformOrigin and offset jump on frame 1.
    var primarySize by remember { mutableStateOf(IntSize.Zero) }
    var hostPositionInWindow by remember { mutableStateOf(IntOffset.Zero) }
    var hostMeasured by remember { mutableStateOf(false) }

    LaunchedEffect(show) {
        if (show) {
            internalVisible.value = true
            backEnterProgress.snapTo(0f)
            backExpandProgress.snapTo(0f)
            launch { dimProgress.animateTo(1f, ListPopupDefaults.DimEnterAnimationSpec) }
            snapshotFlow { primarySize to hostMeasured }
                .first { (size, ready) -> size != IntSize.Zero && ready }
            launch { enterFraction.animateTo(1f, ListPopupDefaults.FractionAnimationSpec) }
            launch { enterAlpha.animateTo(1f, ListPopupDefaults.AlphaEnterAnimationSpec) }
        } else {
            if (!internalVisible.value) return@LaunchedEffect
            expandedItem = null
            launch { enterFraction.animateTo(0f, ListPopupDefaults.FractionExitAnimationSpec) }
            launch { dimProgress.animateTo(0f, ListPopupDefaults.DimExitAnimationSpec) }
            enterAlpha.animateTo(0f, ListPopupDefaults.AlphaExitAnimationSpec)
            enterFraction.stop()
            dimProgress.stop()
            backEnterProgress.snapTo(0f)
            backExpandProgress.snapTo(0f)
            internalVisible.value = false
            currentOnDismissFinished?.invoke()
        }
    }

    LaunchedEffect(expandedItem) {
        val item = expandedItem
        if (item != null) {
            displayedItem = item
            backExpandProgress.snapTo(0f)
        }
        val target = if (item != null) 1f else 0f
        // COUI SmallScreenAnimationController drives both menus with bounce 0 / response 0.35.
        val mainSpec = folmeSpring<Float>(SUBMENU_SPRING_DAMPING, SUBMENU_SPRING_RESPONSE)
        launch {
            expandFraction.animateTo(target, mainSpec)
            if (target == 0f) {
                displayedItem = null
                // Only after the collapse settles — clearing earlier jumps the effective values.
                backExpandProgress.snapTo(0f)
            }
        }
        launch {
            primaryScale.animateTo(
                if (target == 1f) PRIMARY_SHRUNK_SCALE else 1f,
                mainSpec,
            )
        }
        // COUI SmallScreenAnimationController.startSubMenuEnter animates the main menu content to
        // mMainMenuAlphaPercent; the card material itself stays opaque.
        launch {
            primaryAlpha.animateTo(
                if (target == 1f) PRIMARY_SHRUNK_ALPHA else 1f,
                mainSpec,
            )
        }
    }

    // COUISpringDragHelper's model: the panel chases the finger on the drag spring and, on release,
    // travels back to the anchored offset — 0 — on the hand-up spring, carrying the release velocity
    // the native pipeline normalises and clamps at iolHandUpMaxInitialVelocity. Driving the target
    // through snapshotFlow rather than snapping keeps the follow lag the native spring produces.
    val panelDensity = LocalDensity.current
    val panelMaxStretchPx = with(panelDensity) { PanelDragMaxStretch.toPx() }
    val panelAreaHandUpBounce = remember(primarySize, panelDensity) {
        panelHandUpAreaBounce(primarySize.width.toFloat() * primarySize.height, panelDensity)
    }
    val panelDragOffset = remember { Animatable(0f) }
    var panelDragTarget by remember { mutableFloatStateOf(0f) }
    var panelDragging by remember { mutableStateOf(false) }
    // Finger travel accumulated over the gesture and the speed of the latest sample, so the hand-up
    // spring can start from the velocity the finger actually let go at.
    var panelTravel by remember { mutableFloatStateOf(0f) }
    var panelFingerVelocity by remember { mutableFloatStateOf(0f) }
    var panelReleaseVelocity by remember { mutableFloatStateOf(0f) }
    var panelLastMoveAt by remember { mutableStateOf(TimeSource.Monotonic.markNow()) }
    var panelDragStartedAt by remember { mutableStateOf(TimeSource.Monotonic.markNow()) }
    val panelVelocity = remember { PanelVelocityTracker() }
    val endPanelDrag = remember(panelMaxStretchPx, panelAreaHandUpBounce) {
        {
            panelReleaseVelocity =
                if (panelLastMoveAt.elapsedNow().inWholeMilliseconds > PANEL_DRAG_VELOCITY_STALE_MS) {
                    0f
                } else {
                    panelHandUpInitialVelocity(panelFingerVelocity, panelDragOffset.value, panelMaxStretchPx)
                }
            panelTravel = 0f
            panelFingerVelocity = 0f
            panelVelocity.reset()
            panelDragging = false
            panelDragTarget = 0f
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { panelDragTarget to panelDragging }
            .collectLatest { (target, dragging) ->
                panelDragOffset.animateTo(
                    target,
                    if (dragging) {
                        PanelDragFollowSpring
                    } else {
                        panelHandUpSpring(panelAreaHandUpBounce, panelReleaseVelocity)
                    },
                    initialVelocity = if (dragging) 0f else panelReleaseVelocity,
                )
            }
    }
    // A dismiss that lands mid-drag must not leave the exit animation running on an offset panel.
    LaunchedEffect(show) {
        if (!show) endPanelDrag()
    }

    if (!show && !internalVisible.value) return

    var parentBounds by remember { mutableStateOf(IntRect.Zero) }
    Spacer(
        modifier = Modifier.onGloballyPositioned { childCoordinates ->
            childCoordinates.parentLayoutCoordinates?.let { parent ->
                val pos = parent.positionInWindow()
                parentBounds = IntRect(
                    left = pos.x.toInt(),
                    top = pos.y.toInt(),
                    right = pos.x.toInt() + parent.size.width,
                    bottom = pos.y.toInt() + parent.size.height,
                )
            }
        },
    )
    if (parentBounds == IntRect.Zero) return

    val layoutInfo = rememberListPopupLayoutInfo(
        alignment = alignment,
        popupPositionProvider = popupPositionProvider,
        parentBounds = parentBounds,
        popupContentSize = primarySize,
    )

    val primaryShift = remember { PopupShift() }

    val anchorBoundsByItem = remember { mutableStateMapOf<DropdownItem, IntRect>() }

    // Native: the popup list is a COUIRecyclerView whose over-scroll moves the whole menu content —
    // COUITouchRecyclerView.updateOverScrollMode() gates that on canScrollVertically(), so the list
    // keeps the drag while it still has a scroll range and the panel rubber-bands on what is left
    // over. Compose states the same split as nested scroll: the list consumes what it can and hands
    // the remainder to this connection.
    val panelNestedScroll = remember(panelMaxStretchPx) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput || available.y == 0f) return Offset.Zero
                val now = TimeSource.Monotonic.markNow()
                panelLastMoveAt = now
                if (!panelDragging) {
                    panelDragging = true
                    panelTravel = 0f
                    panelFingerVelocity = 0f
                    panelVelocity.reset()
                    panelDragStartedAt = now
                }
                panelTravel += available.y
                panelVelocity.add(panelDragStartedAt.elapsedNow().inWholeMilliseconds, panelTravel)
                panelFingerVelocity = panelVelocity.velocityPxPerSecond()
                panelDragTarget = panelDragRubber(panelTravel, panelMaxStretchPx)
                return available
            }
        }
    }

    // COUI `getMainMenuMinWidth`: a menu whose rows own submenus floors at
    // `coui_popup_list_window_width_with_icon` (196dp) instead of the plain 168dp.
    val hasSubMenuRows = entries.any { entry -> entry.items.any { !it.children.isNullOrEmpty() } }
    val resolvedMinWidth = if (hasSubMenuRows) maxOf(minWidth, PopupSubMenuMinWidth) else minWidth

    popupHost(internalVisible.value, { passTouches }) {
        val backState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
        NavigationBackHandler(
            state = backState,
            isBackEnabled = show,
            onBackCancelled = {
                // Reset whichever track the gesture drove; depth cannot change mid-gesture.
                coroutineScope.launch {
                    if (expandedItem != null) {
                        backExpandProgress.animateTo(
                            0f,
                            folmeSpring(SUBMENU_SPRING_DAMPING, SUBMENU_SPRING_RESPONSE),
                        )
                    } else {
                        backEnterProgress.animateTo(0f, ListPopupDefaults.ResetAnimationSpec)
                    }
                }
            },
            // Mirror outside-tap: depth > 0 collapses the secondary, depth 0 dismisses the popup.
            onBackCompleted = {
                if (expandedItem != null) {
                    expandedItem = null
                } else {
                    currentOnDismiss()
                }
            },
        )

        LaunchedEffect(Unit) {
            // Single-coroutine collector so per-frame ticks do not relaunch this effect.
            snapshotFlow { backState.transitionState }
                .collect { transitionState ->
                    if (
                        transitionState is NavigationEventTransitionState.InProgress &&
                        transitionState.direction == NavigationEventTransitionState.TRANSITIONING_BACK
                    ) {
                        val progress = transitionState.latestEvent.progress
                        if (expandedItem != null) {
                            // Preview secondary → primary collapse along the expand-tree.
                            backExpandProgress.snapTo(progress)
                        } else {
                            // Preview popup → spawn corner retreat along the entry tree.
                            backEnterProgress.snapTo(progress)
                        }
                    }
                }
        }

        CompositionLocalProvider(LocalDismissState provides { currentOnDismiss() }) {
            val dimColor = COUITheme.colorScheme.windowDimming
            // COUI popup surface (couiColorBackgroundTop), shared with ListPopupContent.
            val surfaceColor = popupSurfaceColor()

            Box(modifier = Modifier.fillMaxSize()) {
                if (enableWindowDim) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = dimProgress.value * (1f - backEnterProgress.value) }
                            .background(dimColor),
                    )
                }
                Box(
                    modifier = popupModifier
                        .fillMaxSize()
                        .onGloballyPositioned { coords ->
                            val pos = coords.positionInWindow()
                            hostPositionInWindow = IntOffset(pos.x.toInt(), pos.y.toInt())
                            hostMeasured = true
                        }
                        // Without a pointer input this layer is no hit target at all, so once the
                        // close starts both handlers below drop out and touches reach the page.
                        .then(
                            if (passTouches) {
                                Modifier
                            } else {
                                Modifier.pointerInput(Unit) {
                                    detectTapGestures(onTap = { position ->
                                        // COUI hosts the popup in its own window, so a touch on a row
                                        // never reaches the window-level dismiss handler. Here both
                                        // share one hierarchy, and this layer sits above the list in
                                        // the hit path: without the bounds check the row's own gesture
                                        // runs first and this handler then resets the very item that
                                        // gesture just expanded.
                                        val bounds = popupContentBounds.rect
                                        val inside = bounds != IntRect.Zero &&
                                            position.x >= bounds.left && position.x <= bounds.right &&
                                            position.y >= bounds.top && position.y <= bounds.bottom
                                        if (inside) return@detectTapGestures
                                        if (expandedItem != null) {
                                            expandedItem = null
                                        } else {
                                            currentOnDismiss()
                                        }
                                    })
                                }
                            },
                        )
                        // The drag itself arrives through panelNestedScroll; this layer only has to
                        // notice the gesture ending, which the scrollable reports to nobody once it
                        // has consumed the last move. Observing in the Final pass keeps this out of
                        // every other handler's way, and it consumes nothing.
                        .nestedScroll(panelNestedScroll)
                        .then(
                            if (passTouches) {
                                Modifier
                            } else {
                                Modifier.pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            awaitFirstDown(requireUnconsumed = false)
                                            var lifted = false
                                            while (!lifted) {
                                                val event = awaitPointerEvent(PointerEventPass.Final)
                                                lifted = event.changes.none { it.pressed }
                                            }
                                            endPanelDrag()
                                        }
                                    }
                                }
                            },
                        ),
                ) {
                    // The host invokes this content in its own composition (COUIPopupUtils
                    // `popupState.content()`), so the density provided around this function never
                    // reaches it. Re-provide it here, inside the lambda, to render the menu at the
                    // panel density.
                    // A closing panel stops taking row input at once, like ListPopup's: the rows
                    // drop their gesture handlers, so a press on the shrinking card reaches the
                    // page as well instead of re-triggering a row.
                    CompositionLocalProvider(
                        LocalDensity provides panelDensity,
                        LocalPopupInteractionEnabled provides !passTouches,
                    ) {
                        CascadingMorphSubLayout(
                            modifier = Modifier.graphicsLayer { translationY = panelDragOffset.value },
                            entries = entries,
                            expandedItem = expandedItem,
                            displayedItem = displayedItem,
                            onExpand = { expandedItem = it },
                            onCollapseSecondary = { expandedItem = null },
                            onLeafSelected = { item ->
                                item.onClick?.invoke()
                                if (collapseOnSelection) {
                                    expandedItem = null
                                    currentOnDismiss()
                                }
                            },
                            getAnchorBounds = { anchorBoundsByItem[it] },
                            setAnchorBounds = { item, bounds ->
                                // Freeze the anchor once secondary is shown so primary's scale
                                // animation can't drift it via onGloballyPositioned.
                                if (displayedItem == null) {
                                    anchorBoundsByItem[item] = bounds
                                }
                            },
                            onPrimarySizeChange = { primarySize = it },
                            onContentBoundsChange = { popupContentBounds.rect = it },
                            windowBounds = layoutInfo.windowBounds,
                            popupMargin = layoutInfo.popupMargin,
                            parentBounds = parentBounds,
                            popupPositionProvider = popupPositionProvider,
                            alignment = alignment,
                            hostOriginInWindow = { hostPositionInWindow },
                            maxHeight = maxHeight,
                            minWidth = resolvedMinWidth,
                            dropdownColors = dropdownColors,
                            enterFraction = { enterFraction.value * (1f - backEnterProgress.value) },
                            enterAlpha = { enterAlpha.value * (1f - backEnterProgress.value) },
                            primaryScale = { 1f + (primaryScale.value - 1f) * (1f - backExpandProgress.value) },
                            primaryAlpha = { 1f + (primaryAlpha.value - 1f) * (1f - backExpandProgress.value) },
                            expandFraction = { expandFraction.value * (1f - backExpandProgress.value) },
                            primaryShift = primaryShift,
                            layoutInfo = layoutInfo,
                            layoutDirection = layoutDirection,
                            surfaceColor = surfaceColor,
                            enableGlass = enableGlass,
                        )
                    }
                }
            }
        }
    }
}

private const val SLOT_PRIMARY = "primary"
private const val SLOT_SECONDARY = "secondary"
private const val SLOT_SECONDARY_PROBE = "secondary_probe"

// Hoisted so each measure pass reuses the same instances.
private val ProbeOnHeaderClick: () -> Unit = {}
private val ProbeOnLeafSelected: (DropdownItem) -> Unit = {}

@Suppress("LongParameterList")
@Composable
private fun CascadingMorphSubLayout(
    entries: List<DropdownEntry>,
    expandedItem: DropdownItem?,
    displayedItem: DropdownItem?,
    onExpand: (DropdownItem) -> Unit,
    onCollapseSecondary: () -> Unit,
    onLeafSelected: (DropdownItem) -> Unit,
    getAnchorBounds: (DropdownItem) -> IntRect?,
    setAnchorBounds: (DropdownItem, IntRect) -> Unit,
    onPrimarySizeChange: (IntSize) -> Unit,
    onContentBoundsChange: (IntRect) -> Unit,
    windowBounds: IntRect,
    popupMargin: IntRect,
    parentBounds: IntRect,
    popupPositionProvider: PopupPositionProvider,
    alignment: PopupPositionProvider.Align,
    hostOriginInWindow: () -> IntOffset,
    maxHeight: Dp?,
    minWidth: Dp,
    dropdownColors: DropdownColors,
    enterFraction: () -> Float,
    enterAlpha: () -> Float,
    primaryScale: () -> Float,
    primaryAlpha: () -> Float,
    expandFraction: () -> Float,
    primaryShift: PopupShift,
    layoutInfo: ListPopupLayoutInfo,
    layoutDirection: LayoutDirection,
    surfaceColor: Color,
    enableGlass: Boolean,
    modifier: Modifier = Modifier,
) {
    SubcomposeLayout(modifier = modifier.fillMaxSize()) { constraints ->
        val resolvedMaxHeightPx = maxHeight?.roundToPx()?.coerceAtLeast(50.dp.roundToPx())
            ?: (windowBounds.height - popupMargin.top - popupMargin.bottom)
                .coerceAtLeast(50.dp.roundToPx())
        val minWidthPx = minWidth.roundToPx().coerceAtMost(constraints.maxWidth)
        val primaryConstraints = Constraints(
            minWidth = minWidthPx,
            maxWidth = constraints.maxWidth,
            minHeight = 0,
            maxHeight = resolvedMaxHeightPx.coerceAtMost(constraints.maxHeight),
        )

        // Secondary subcomposition tracks [displayedItem] so the clip animation survives the
        // collapse spring; primary's mask/indicator use [expandedItem] for instant input feedback.
        val activeExpanded = displayedItem
        val anchorRect = activeExpanded?.let { getAnchorBounds(it) }

        // COUI's small-screen shrink pivots on the menu's trailing edge and its top edge; with the
        // primary right-aligned that is exactly the right edge of the card. `Align.End` is logical,
        // so in RTL the trailing edge is the card's left one.
        val alignedToTrailingEdge = when (alignment) {
            PopupPositionProvider.Align.End,
            PopupPositionProvider.Align.TopEnd,
            PopupPositionProvider.Align.BottomEnd,
            -> true

            else -> false
        }
        val pivotAtRightEdge = if (layoutDirection == LayoutDirection.Ltr) {
            alignedToTrailingEdge
        } else {
            !alignedToTrailingEdge
        }
        val primaryPivotX = if (pivotAtRightEdge) 1f else 0f

        val primaryPlaceables = subcompose(SLOT_PRIMARY) {
            CascadingPrimaryContent(
                entries = entries,
                expandedItem = expandedItem,
                onExpand = onExpand,
                onLeafSelected = onLeafSelected,
                onAnchorBounds = setAnchorBounds,
                enterFraction = enterFraction,
                enterAlpha = enterAlpha,
                popupLayoutPosition = layoutInfo.popupLayoutPosition,
                transformOrigin = layoutInfo.localTransformOrigin,
                shrinkTransformOrigin = TransformOrigin(primaryPivotX, 0f),
                surfaceColor = surfaceColor,
                dropdownColors = dropdownColors,
                primaryScale = primaryScale,
                primaryAlpha = primaryAlpha,
                primaryTranslationY = { primaryShift.y * expandFraction() },
                onCollapseSecondary = onCollapseSecondary,
                enableGlass = enableGlass,
            )
        }.map { it.measure(primaryConstraints) }

        val primaryWidth = primaryPlaceables.maxOfOrZero { it.width }
        val primaryHeight = primaryPlaceables.sumOfOrZero { it.height }
        val primaryMeasuredSize = IntSize(primaryWidth, primaryHeight)
        onPrimarySizeChange(primaryMeasuredSize)

        val primaryOffsetWindow = if (primaryMeasuredSize == IntSize.Zero) {
            IntOffset.Zero
        } else {
            popupPositionProvider.calculatePosition(
                parentBounds,
                windowBounds,
                layoutDirection,
                primaryMeasuredSize,
                popupMargin,
                alignment,
            )
        }

        // COUI caps the secondary menu so a gap to the window top remains even for tall submenus.
        val secondaryMaxHeightPx = (resolvedMaxHeightPx - SubMenuMinGapToTop.roundToPx())
            .coerceAtLeast(0)

        // Probe the secondary's natural size first. COUI measures the sub menu from its own list
        // (the trigger row is part of that list), not from the main menu's width.
        val probeConstraints = primaryConstraints.copy(
            maxHeight = secondaryMaxHeightPx.coerceAtMost(constraints.maxHeight),
        )

        data class SecondaryLayout(
            val placeables: List<Placeable>,
            val rect: IntRect,
        )

        val secondaryLayout: SecondaryLayout? =
            if (activeExpanded != null && anchorRect != null) {
                val measured = subcompose(SLOT_SECONDARY_PROBE) {
                    Box(propagateMinConstraints = true) {
                        CascadingSecondaryColumn(
                            triggerItem = activeExpanded,
                            dropdownColors = dropdownColors,
                            onHeaderClick = ProbeOnHeaderClick,
                            onLeafSelected = ProbeOnLeafSelected,
                        )
                    }
                }.firstOrNull()?.measure(probeConstraints)
                val secondarySize = if (measured != null) {
                    IntSize(measured.width, measured.height)
                } else {
                    IntSize.Zero
                }
                if (secondarySize.width > 0 && secondarySize.height > 0 && primaryMeasuredSize != IntSize.Zero) {
                    val primaryRect = IntRect(
                        left = primaryOffsetWindow.x,
                        top = primaryOffsetWindow.y,
                        right = primaryOffsetWindow.x + primaryMeasuredSize.width,
                        bottom = primaryOffsetWindow.y + primaryMeasuredSize.height,
                    )
                    val secondaryRect = computeSecondaryRect(
                        primary = primaryRect,
                        anchor = anchorRect,
                        secondarySize = secondarySize,
                        available = IntRect(
                            left = windowBounds.left + popupMargin.left,
                            top = windowBounds.top + popupMargin.top,
                            right = windowBounds.right - popupMargin.right,
                            bottom = windowBounds.bottom - popupMargin.bottom,
                        ),
                        layoutDirection = layoutDirection,
                        verticalOverlapPx = MainSubMenuVerticalOverlap.roundToPx(),
                    )
                    // COUI keeps at least SubMenuMinGapToTop between the window top and the sub menu;
                    // when the sub menu crosses that line the primary menu steps up by the difference
                    // so the two cards stay visually attached.
                    val gapTop = primaryRect.top + SubMenuMinGapToTop.roundToPx()
                    primaryShift.y = if (secondaryRect.top < gapTop) {
                        (secondaryRect.top - gapTop).toFloat()
                    } else {
                        0f
                    }
                    val secondaryConstraints = Constraints.fixed(secondaryRect.width, secondaryRect.height)
                    val placeables = subcompose(SLOT_SECONDARY) {
                        CascadingSecondaryContent(
                            triggerItem = activeExpanded,
                            size = secondarySize,
                            anchorTopInWindow = anchorRect.top,
                            anchorHeightPx = anchorRect.height,
                            primaryBottomInWindow = primaryRect.bottom,
                            secondaryTopInWindow = secondaryRect.top,
                            progress = expandFraction,
                            onCollapseSecondary = onCollapseSecondary,
                            onLeafSelected = onLeafSelected,
                            dropdownColors = dropdownColors,
                            surfaceColor = surfaceColor,
                            enableGlass = enableGlass,
                        )
                    }.map { it.measure(secondaryConstraints) }
                    SecondaryLayout(placeables, secondaryRect)
                } else {
                    null
                }
            } else {
                null
            }

        val host = hostOriginInWindow()
        layout(constraints.maxWidth, constraints.maxHeight) {
            val primaryDxLocal = primaryOffsetWindow.x - host.x
            val primaryDyLocal = primaryOffsetWindow.y - host.y
            var py = primaryDyLocal
            primaryPlaceables.forEach { placeable ->
                placeable.place(primaryDxLocal, py)
                py += placeable.height
            }
            // Reported in the dismiss layer's local coordinates so it can tell a tap on the popup
            // from a tap outside it. See [PopupContentBounds].
            var bounds = IntRect(
                left = primaryDxLocal,
                top = primaryDyLocal,
                right = primaryDxLocal + primaryWidth,
                bottom = primaryDyLocal + primaryHeight,
            )
            secondaryLayout?.let { (placeables, rect) ->
                val sx = rect.left - host.x
                val sy = rect.top - host.y
                placeables.forEach { it.place(sx, sy) }
                bounds = unionOf(
                    bounds,
                    IntRect(sx, sy, sx + rect.width, sy + rect.height),
                )
            }
            onContentBoundsChange(bounds)
        }
    }
}

/**
 * `TouchMotionRubberBand.apply(offset, curveRatio, maxDistancePx)`:
 * `signum(offset) * (1 - 1 / (abs(offset) * curveRatio / maxDistancePx + 1)) * maxDistancePx`, with
 * `curveRatio` = [PANEL_DRAG_CURVE_RATIO] and `maxDistancePx` = [PanelDragMaxStretch], the popup
 * tier of `TouchMotionTierParams`.
 *
 * COUI feeds the whole finger displacement through this curve rather than only the part past an edge,
 * because the popup's list has no scroll range for the drag to live in — so the panel is compressed
 * from the first pixel and 28dp is the asymptote no drag can pass. Measured on the device, the native
 * Gallery ⋮ menu travels 37px for a 1000px drag and 41px for 1160px, which is this curve.
 *
 * The magnitude and the sign are separated exactly as the native does: `abs` keeps the denominator
 * away from zero for upward drags (a signed numerator would hit `1 + ratio * d / max == 0` at
 * `d == -max / ratio`, i.e. -2170px at 620dpi, and diverge past it), and `sign` gives the odd
 * symmetry the two drag directions need.
 */
internal fun panelDragRubber(distance: Float, maxStretchPx: Float): Float {
    if (maxStretchPx <= 0f) return 0f
    val magnitude = abs(distance) * PANEL_DRAG_CURVE_RATIO / maxStretchPx
    return sign(distance) * (1f - 1f / (magnitude + 1f)) * maxStretchPx
}

/**
 * Finger speed over the drag, in raw pointer px/s.
 *
 * COUI reads `VelocityTracker.computeCurrentVelocity()` at ACTION_UP, which fits the samples of the
 * last `VelocityTracker.HORIZON` (100ms) rather than reading one frame — the same 100ms this
 * horizon uses, and the same one [PANEL_DRAG_VELOCITY_STALE_MS] treats as a stopped finger. Across
 * the horizon it takes the secant, which is what the fit converges to for a steady drag and stays
 * bounded for an accelerating one, where a single-frame difference spikes. It is an approximation of
 * the fit, not the fit itself: a finger that reverses inside the horizon reports the net travel.
 */
internal class PanelVelocityTracker(
    private val horizonMs: Long = PANEL_DRAG_VELOCITY_STALE_MS,
    private val capacity: Int = PANEL_DRAG_VELOCITY_SAMPLES,
) {
    private val times = ArrayDeque<Long>()
    private val travels = ArrayDeque<Float>()

    fun reset() {
        times.clear()
        travels.clear()
    }

    fun add(elapsedMs: Long, travel: Float) {
        times.addLast(elapsedMs)
        travels.addLast(travel)
        while (times.size > 1 && elapsedMs - times.first() > horizonMs) {
            times.removeFirst()
            travels.removeFirst()
        }
        while (times.size > capacity) {
            times.removeFirst()
            travels.removeFirst()
        }
    }

    fun velocityPxPerSecond(): Float {
        if (times.size < 2) return 0f
        val window = (times.last() - times.first()).toFloat()
        if (window <= 0f) return 0f
        return (travels.last() - travels.first()) * 1000f / window
    }
}

/**
 * `TouchMotionHandUpHelper.computeInitialVelocity`: the velocity the hand-up spring starts from is
 * `clamp(velocity / totalDistancePx * |startValue|, ±maxVelocity)`, where `velocity` is the finger's
 * px/s already scaled by `COUIVelocityTrackerHelper`'s `velocityScale` (0.3) and `totalDistancePx`
 * is the rubber asymptote. The result is a PANEL-space velocity — the offset it is paired with is
 * the rubber-compressed one, not the finger travel — so it must not be fed the raw finger speed.
 */
internal fun panelHandUpInitialVelocity(
    fingerVelocityPx: Float,
    panelOffsetPx: Float,
    maxStretchPx: Float,
): Float {
    if (maxStretchPx <= 0f) return 0f
    val scaled = fingerVelocityPx * PANEL_DRAG_VELOCITY_SCALE
    return (scaled / maxStretchPx * abs(panelOffsetPx))
        .coerceIn(-PANEL_DRAG_MAX_VELOCITY, PANEL_DRAG_MAX_VELOCITY)
}

private fun unionOf(a: IntRect, b: IntRect): IntRect = IntRect(
    left = minOf(a.left, b.left),
    top = minOf(a.top, b.top),
    right = maxOf(a.right, b.right),
    bottom = maxOf(a.bottom, b.bottom),
)

/**
 * COUI `PopupMenuLocateHelper` sub-menu placement for a compact window:
 * - `getOffsetX` returns the *main menu's* leading edge, so the sub menu keeps that edge while the
 *   main menu scales towards its trailing pivot and visually retreats to that side. The sub menu is
 *   clamped into [available] horizontally, so a sub menu wider than the main menu never leaves the
 *   window when the main menu sits flush against the trailing edge.
 * - `getOffsetY` maps the tapped row through the main menu's shrink ratio
 *   (`relocated.height / mainMenu.height`, i.e. [PRIMARY_SHRUNK_SCALE]) and then subtracts the
 *   14dp vertical overlap, pinning the bottom on overflow.
 */
internal fun computeSecondaryRect(
    primary: IntRect,
    anchor: IntRect,
    secondarySize: IntSize,
    available: IntRect,
    layoutDirection: LayoutDirection,
    verticalOverlapPx: Int,
): IntRect {
    // The sub menu shares the main menu's leading edge: left in LTR, right in RTL.
    val preferredLeft = if (layoutDirection == LayoutDirection.Ltr) {
        primary.left
    } else {
        primary.right - secondarySize.width
    }
    val maxLeft = (available.right - secondarySize.width).coerceAtLeast(available.left)
    val left = preferredLeft.coerceIn(available.left, maxLeft)
    val scaledAnchorTop = primary.top + PRIMARY_SHRUNK_SCALE * (anchor.top - primary.top)
    val preferredTop = scaledAnchorTop.toInt() - verticalOverlapPx
    val top = if (preferredTop + secondarySize.height <= available.bottom) {
        preferredTop
    } else {
        available.bottom - secondarySize.height
    }.coerceAtLeast(available.top)
    return IntRect(
        left = left,
        top = top,
        right = left + secondarySize.width,
        bottom = top + secondarySize.height,
    )
}

private inline fun <T> List<T>.maxOfOrZero(selector: (T) -> Int): Int = if (isEmpty()) 0 else maxOf(selector)

private inline fun <T> List<T>.sumOfOrZero(selector: (T) -> Int): Int = if (isEmpty()) 0 else sumOf(selector)
