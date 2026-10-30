// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Indication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.anim.COUIInEase
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.BlendColorEntry
import io.github.suqi8.coui.kmp.blur.BlurBlendMode
import io.github.suqi8.coui.kmp.shader.asBrush
import io.github.suqi8.coui.kmp.shader.isRuntimeShaderSupported
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.CouiHapticEffect
import io.github.suqi8.coui.kmp.utils.rememberCouiHaptic
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A [TabRow] with COUI style, without the container capsule.
 *
 * Renders the COUI segment button (see [TabRowWithContour]) without the container fill: only the
 * sliding capsule indicator with its drop shadow, on a transparent background by default.
 *
 * @param tabs The text to be displayed in the [TabRow].
 * @param selectedTabIndex The selected tab index of the [TabRow].
 * @param onTabSelected The callback when a tab is selected.
 * @param modifier The modifier to be applied to the [TabRow].
 * @param colors The colors of the [TabRow].
 * @param minWidth The minimum width of the tab in [TabRow].
 * @param maxWidth The maximum width of the tab in [TabRow].
 * @param height The height of the [TabRow].
 * @param cornerRadius The corner radius of the sliding indicator in [TabRow].
 * @param itemSpacing The spacing between tabs in [TabRow].
 * @param contentAlignment The content alignment of the tab in [TabRow].
 * @param listState The [LazyListState] to be used for the [TabRow].
 * @param interactionSource The [MutableInteractionSource] to be used for the [TabRow].
 * @param indication The [Indication] to be used for the [TabRow].
 */
@Composable
@NonRestartableComposable
fun TabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    colors: TabRowColors = TabRowDefaults.tabRowColors(backgroundColor = Color.Transparent),
    minWidth: Dp = TabRowDefaults.TabRowMinWidth,
    maxWidth: Dp = TabRowDefaults.TabRowMaxWidth,
    height: Dp = TabRowDefaults.TabRowHeight,
    cornerRadius: Dp = TabRowDefaults.TabRowCornerRadius,
    itemSpacing: Dp = 0.dp,
    contentAlignment: Alignment = Alignment.Center,
    listState: LazyListState? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    dragSelectionEnabled: Boolean = true,
    commitOnRelease: Boolean = false,
) {
    SegmentButtonRow(
        tabs = tabs,
        selectedTabIndex = selectedTabIndex,
        onTabSelected = onTabSelected,
        modifier = modifier,
        colors = colors,
        minWidth = minWidth,
        maxWidth = maxWidth,
        height = height,
        cornerRadius = cornerRadius,
        contourPadding = 0.dp,
        segmentPadding = SegmentPaddingHorizontal,
        itemSpacing = itemSpacing,
        contentAlignment = contentAlignment,
        listState = listState,
        interactionSource = interactionSource,
        indication = indication,
        indicatorMaterial = null,
        backdrop = null,
        dragSelectionEnabled = dragSelectionEnabled,
        commitOnRelease = commitOnRelease,
        touchMotionEnabled = false,
    )
}

/** Binary-compatibility bridge for [TabRow] callers compiled before drag-selection parameters existed. */
@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload with dragSelectionEnabled and commitOnRelease.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
@NonRestartableComposable
fun TabRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    colors: TabRowColors = TabRowDefaults.tabRowColors(backgroundColor = Color.Transparent),
    minWidth: Dp = TabRowDefaults.TabRowMinWidth,
    maxWidth: Dp = TabRowDefaults.TabRowMaxWidth,
    height: Dp = TabRowDefaults.TabRowHeight,
    cornerRadius: Dp = TabRowDefaults.TabRowCornerRadius,
    itemSpacing: Dp = 0.dp,
    contentAlignment: Alignment = Alignment.Center,
    listState: LazyListState? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
) {
    TabRow(
        tabs = tabs,
        selectedTabIndex = selectedTabIndex,
        onTabSelected = onTabSelected,
        modifier = modifier,
        colors = colors,
        minWidth = minWidth,
        maxWidth = maxWidth,
        height = height,
        cornerRadius = cornerRadius,
        itemSpacing = itemSpacing,
        contentAlignment = contentAlignment,
        listState = listState,
        interactionSource = interactionSource,
        indication = indication,
        dragSelectionEnabled = true,
        commitOnRelease = false,
    )
}

/**
 * A [TabRowWithContour] with COUI style.
 *
 * A port of the ColorOS 17 segmented control (`COUISegmentButtonLayout`, default `SegmentButton`
 * style), supporting press-and-drag slide selection across segments, optional release-commit
 * navigation, backdrop glass blur, and ColorOS 17 rubber-band touch deformation with spotlight.
 * For the `SegmentButton.Tiny` style, pass [TabRowDefaults.TabRowWithContourTinyHeight],
 * [TabRowDefaults.TabRowWithContourTinyCornerRadius] and
 * [TabRowDefaults.TabRowWithContourTinyPadding].
 *
 * @param tabs The text to be displayed in the [TabRow].
 * @param selectedTabIndex The selected tab index of the [TabRow].
 * @param onTabSelected The callback when a tab is selected.
 * @param modifier The modifier to be applied to the [TabRow].
 * @param colors The colors of the [TabRow].
 * @param minWidth The minimum width of the tab in [TabRow].
 * @param maxWidth The maximum width of the tab in [TabRow].
 * @param height The height of the [TabRow].
 * @param cornerRadius The corner radius of the sliding indicator in [TabRow].
 * @param contourPadding The inset between the container capsule and the sliding indicator.
 * @param segmentPadding The horizontal padding inside each segment, around its label. Lowering it
 *   lets more labels fit the capsule without narrowing the container.
 * @param itemSpacing The spacing between tabs in [TabRow].
 * @param contentAlignment The content alignment of the tab in [TabRow].
 * @param listState The [LazyListState] to be used for the [TabRow].
 * @param interactionSource The [MutableInteractionSource] to be used for the [TabRow].
 * @param indication The [Indication] to be used for the [TabRow].
 * @param indicatorMaterial When non-null, the sliding indicator also gets the material rim and inner
 *   shadow described by these params, the same edge the search capsule uses. `null` keeps the plain
 *   indicator fill.
 * @param backdrop Optional [Backdrop] blurred behind the segment container capsule when non-null.
 * @param dragSelectionEnabled Whether pressing and dragging horizontally slides the active indicator
 *   across segments with haptic feedback, matching `COUISegmentButtonLayout.dispatchTouchEvent`.
 * @param commitOnRelease When `true`, dragging moves the indicator live while [onTabSelected] is
 *   dispatched once on finger release (and rolled back on gesture cancel), avoiding mid-drag page
 *   switches from interrupting the indicator spring.
 * @param touchMotionEnabled Whether the outer capsule applies ColorOS 17 rubber-band deformation and
 *   `CAPSULE_PARAMS_2` spotlight feedback.
 */
@Composable
@NonRestartableComposable
fun TabRowWithContour(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    colors: TabRowColors = TabRowDefaults.tabRowColors(),
    minWidth: Dp = TabRowDefaults.TabRowWithContourMinWidth,
    maxWidth: Dp = TabRowDefaults.TabRowWithContourMaxWidth,
    height: Dp = TabRowDefaults.TabRowWithContourHeight,
    cornerRadius: Dp = TabRowDefaults.TabRowWithContourCornerRadius,
    contourPadding: Dp = TabRowDefaults.TabRowWithContourPadding,
    segmentPadding: Dp = SegmentPaddingHorizontal,
    itemSpacing: Dp = 0.dp,
    contentAlignment: Alignment = Alignment.Center,
    listState: LazyListState? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    indicatorMaterial: MaterialStrokeParams? = null,
    backdrop: Backdrop? = null,
    dragSelectionEnabled: Boolean = true,
    commitOnRelease: Boolean = false,
    touchMotionEnabled: Boolean = true,
) {
    SegmentButtonRow(
        tabs = tabs,
        selectedTabIndex = selectedTabIndex,
        onTabSelected = onTabSelected,
        modifier = modifier,
        colors = colors,
        minWidth = minWidth,
        maxWidth = maxWidth,
        height = height,
        cornerRadius = cornerRadius,
        contourPadding = contourPadding,
        segmentPadding = segmentPadding,
        itemSpacing = itemSpacing,
        contentAlignment = contentAlignment,
        listState = listState,
        interactionSource = interactionSource,
        indication = indication,
        indicatorMaterial = indicatorMaterial,
        backdrop = backdrop,
        dragSelectionEnabled = dragSelectionEnabled,
        commitOnRelease = commitOnRelease,
        touchMotionEnabled = touchMotionEnabled,
    )
}

/** Binary-compatibility bridge for [TabRowWithContour] callers compiled before ColorOS 17 material/drag args. */
@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload with backdrop, dragSelectionEnabled, commitOnRelease and touchMotionEnabled.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
@NonRestartableComposable
fun TabRowWithContour(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    colors: TabRowColors = TabRowDefaults.tabRowColors(),
    minWidth: Dp = TabRowDefaults.TabRowWithContourMinWidth,
    maxWidth: Dp = TabRowDefaults.TabRowWithContourMaxWidth,
    height: Dp = TabRowDefaults.TabRowWithContourHeight,
    cornerRadius: Dp = TabRowDefaults.TabRowWithContourCornerRadius,
    contourPadding: Dp = TabRowDefaults.TabRowWithContourPadding,
    segmentPadding: Dp = SegmentPaddingHorizontal,
    itemSpacing: Dp = 0.dp,
    contentAlignment: Alignment = Alignment.Center,
    listState: LazyListState? = null,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = null,
    indicatorMaterial: MaterialStrokeParams? = null,
) {
    TabRowWithContour(
        tabs = tabs,
        selectedTabIndex = selectedTabIndex,
        onTabSelected = onTabSelected,
        modifier = modifier,
        colors = colors,
        minWidth = minWidth,
        maxWidth = maxWidth,
        height = height,
        cornerRadius = cornerRadius,
        contourPadding = contourPadding,
        segmentPadding = segmentPadding,
        itemSpacing = itemSpacing,
        contentAlignment = contentAlignment,
        listState = listState,
        interactionSource = interactionSource,
        indication = indication,
        indicatorMaterial = indicatorMaterial,
        backdrop = null,
        dragSelectionEnabled = true,
        commitOnRelease = false,
        touchMotionEnabled = true,
    )
}

/**
 * Shared implementation of the COUI segment button, following COUISegmentButtonLayout.onDraw
 * and COUISegmentButtonLayout.dispatchTouchEvent.
 */
@Composable
private fun SegmentButtonRow(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    colors: TabRowColors,
    minWidth: Dp,
    maxWidth: Dp,
    height: Dp,
    cornerRadius: Dp,
    contourPadding: Dp,
    segmentPadding: Dp,
    itemSpacing: Dp,
    contentAlignment: Alignment,
    listState: LazyListState?,
    interactionSource: MutableInteractionSource?,
    indication: Indication?,
    indicatorMaterial: MaterialStrokeParams?,
    backdrop: Backdrop?,
    dragSelectionEnabled: Boolean,
    commitOnRelease: Boolean,
    touchMotionEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val currentOnTabSelected by rememberUpdatedState(onTabSelected)
    val couiHaptic = rememberCouiHaptic()
    val currentHaptic by rememberUpdatedState(couiHaptic)
    val isDarkTheme = COUITheme.colorScheme.background.luminance() < 0.5f
    val shadowColor = colors.indicatorShadowColor().takeOrElse { if (isDarkTheme) IndicatorShadowDark else IndicatorShadowLight }
    // coui_segment_button_shadow_end_color is 3/13 of coui_segment_button_shadow_color.
    val shadowEndColor = remember(shadowColor) { shadowColor.copy(alpha = shadowColor.alpha * INDICATOR_SHADOW_END_RATIO) }
    val containerColor = colors.backgroundColor(false)
    val indicatorColor = colors.backgroundColor(true)
    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    val textMeasurer = rememberTextMeasurer()
    val labelFontSize = COUITheme.textStyles.body2.fontSize
    // COUI measures segments with the selected appearance (couiTextButtonM: 14sp, medium).
    val measureStyle = remember(labelFontSize) { TextStyle(fontSize = labelFontSize, fontWeight = FontWeight.Medium) }
    val resolvedListState = listState ?: rememberLazyListState()
    val indicatorShader = rememberMaterialShadowEdgeShader(
        enabled = indicatorMaterial != null && isRuntimeShaderSupported(),
    )
    val containerRadiusDp = cornerRadius + contourPadding
    val containerShape = remember(containerRadiusDp, height) {
        if (containerRadiusDp * 2 >= height) SegmentCapsuleShape else RoundedCornerShape(containerRadiusDp)
    }
    val hasContainerSurface = contourPadding > 0.dp && containerColor.alpha > 0f
    val blurScale = rememberToolbarAttenuatedBlurScale()
    val glassActive = hasContainerSurface && backdrop != null && isRuntimeShaderSupported()
    val containerGlassModifier = if (hasContainerSurface && backdrop != null) {
        Modifier.couiGlassSurface(
            backdrop = backdrop,
            shape = containerShape,
            containerColor = containerColor,
            fallback = Modifier,
            blurRadius = with(density) { (SEGMENT_NATIVE_BLUR_SIGMA_PX / TEXTURE_BLUR_SIGMA_SCALE).toDp().value } * blurScale(),
            blendColors = TabRowDefaults.segmentBlurColors(isDarkTheme),
        )
    } else {
        Modifier
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
            .height(height)
            .couiTouchMotion(
                surface = containerGlassModifier,
                enabled = touchMotionEnabled && hasContainerSurface,
                // COUISegmentButtonLayout.initTouchMotionAnimator: deformation, rubber band and
                // press scale are all on.
                pressScaleEnabled = true,
                consumeDrag = false,
                // COUISegmentButtonLayout only lights up under setMaterialEffectEnabled.
                lightEnabled = hasContainerSurface && (backdrop != null || indicatorMaterial != null),
                shape = containerShape.takeIf { hasContainerSurface },
                lightParams = SpotLightDefaults.capsuleParams2(isDarkTheme),
            ),
    ) {
        // Whole pixels, like the view's padding and the LazyRow's own, so labels and indicator line up.
        val contourPaddingPx = with(density) { contourPadding.roundToPx().toFloat() }
        val viewportPx = with(density) { this@BoxWithConstraints.maxWidth.toPx() } - contourPaddingPx * 2f
        val minWidthPx = with(density) { minWidth.roundToPx() }
        val maxWidthPx = if (maxWidth == Dp.Infinity) Int.MAX_VALUE else with(density) { maxWidth.roundToPx() }
        val spacingPx = with(density) { itemSpacing.roundToPx() }
        val segmentPaddingPx = with(density) { segmentPadding.roundToPx() }

        // COUISegmentButtonLayout.calculateChildrenWidths: natural label widths distributed to fill the row.
        val segmentWidthsPx = remember(
            tabs,
            viewportPx,
            minWidthPx,
            maxWidthPx,
            spacingPx,
            segmentPaddingPx,
            textMeasurer,
            measureStyle,
        ) {
            val naturalWidths = tabs.map { label ->
                max(
                    textMeasurer.measure(text = label, style = measureStyle, maxLines = 1).size.width +
                        2 * segmentPaddingPx,
                    minWidthPx,
                )
            }
            val availablePx = viewportPx - spacingPx * max(0, tabs.size - 1)
            computeSegmentWidths(naturalWidths, availablePx, minWidthPx, maxWidthPx)
        }
        val segmentLeftsPx = remember(segmentWidthsPx, spacingPx) {
            buildList {
                var left = 0
                for (widthPx in segmentWidthsPx) {
                    add(left)
                    left += widthPx + spacingPx
                }
            }
        }

        // Pending index during press-and-drag slide selection (COUISegmentButtonLayout.dispatchTouchEvent).
        var dragPreviewIndex by remember { mutableStateOf<Int?>(null) }
        LaunchedEffect(selectedTabIndex) {
            if (dragPreviewIndex == selectedTabIndex) {
                dragPreviewIndex = null
            }
        }
        val activeSelectedTabIndex = (dragPreviewIndex ?: selectedTabIndex).coerceIn(0, max(0, tabs.lastIndex))

        val contentHeightPx = with(density) { height.toPx() } - contourPaddingPx * 2f
        val compact = contourPadding < SegmentCompactPaddingThreshold
        val maxExtraWidthPx = with(density) { (if (compact) SegmentSmallExtraWidth else SegmentRegularExtraWidth).toPx() }
        val switchDistancePx = with(density) { SegmentSwitchDistance.toPx() }
        // Initialized from the current selection so the first frame shows the indicator at rest.
        val indicator = remember {
            val index = activeSelectedTabIndex.coerceIn(0, max(0, segmentWidthsPx.lastIndex))
            val width = if (segmentWidthsPx.isEmpty()) 0f else segmentWidthsPx[index].toFloat()
            SegmentIndicatorMotion(
                center = if (segmentWidthsPx.isEmpty()) 0f else segmentLeftsPx[index] + width / 2f,
                width = width,
                height = contentHeightPx,
            )
        }
        val indicatorScale = remember { Animatable(1f) }
        var selectedSegmentPressed by remember { mutableStateOf(false) }
        var selectedTabPressed by remember { mutableStateOf(false) }
        var dragPressedIndex by remember { mutableIntStateOf(-1) }
        var lastSettledSelectedTabIndex by remember(resolvedListState) { mutableIntStateOf(-1) }

        LaunchedEffect(indicator) { indicator.animate() }

        // COUISegmentButtonLayout.animateIndicatorToPosition.
        LaunchedEffect(activeSelectedTabIndex, segmentWidthsPx, segmentLeftsPx, contentHeightPx) {
            if (segmentWidthsPx.isEmpty()) return@LaunchedEffect
            val index = activeSelectedTabIndex.coerceIn(0, segmentWidthsPx.lastIndex)
            val targetWidth = segmentWidthsPx[index].toFloat()
            val targetCenter = segmentLeftsPx[index] + targetWidth / 2f
            if (lastSettledSelectedTabIndex < 0 || lastSettledSelectedTabIndex == activeSelectedTabIndex) {
                indicator.snapTo(targetCenter, targetWidth, contentHeightPx)
            } else {
                indicator.select(
                    targetCenter = targetCenter,
                    targetWidth = targetWidth,
                    restHeight = contentHeightPx,
                    firstCenter = segmentLeftsPx.first() + segmentWidthsPx.first() / 2f,
                    lastCenter = segmentLeftsPx.last() + segmentWidthsPx.last() / 2f,
                    maxExtraWidth = maxExtraWidthPx,
                    compact = compact,
                )
            }
        }

        // Keep the selected tab visible when the row overflows and scrolls.
        LaunchedEffect(activeSelectedTabIndex, viewportPx, resolvedListState, segmentWidthsPx) {
            if (segmentWidthsPx.isNotEmpty()) {
                val index = activeSelectedTabIndex.coerceIn(0, segmentWidthsPx.lastIndex)
                val offsetPx = -((viewportPx - segmentWidthsPx[index]) / 2f).roundToInt()
                if (lastSettledSelectedTabIndex < 0 || lastSettledSelectedTabIndex == activeSelectedTabIndex) {
                    resolvedListState.scrollToItem(index, offsetPx)
                } else {
                    resolvedListState.animateScrollToItem(index, offsetPx)
                }
            }
            lastSettledSelectedTabIndex = activeSelectedTabIndex
        }

        // COUIPressFeedbackHelper: pressing the selected segment scales the indicator around its center.
        val indicatorPressed = selectedSegmentPressed || selectedTabPressed
        LaunchedEffect(indicatorPressed, activeSelectedTabIndex, segmentWidthsPx, height, contourPadding) {
            val target = if (indicatorPressed && segmentWidthsPx.isNotEmpty()) {
                val index = activeSelectedTabIndex.coerceIn(0, segmentWidthsPx.lastIndex)
                with(density) {
                    pressedScaleEndRatio(
                        Size(segmentWidthsPx[index].toFloat(), (height - contourPadding * 2).toPx()),
                    )
                }
            } else {
                1f
            }
            indicatorScale.animateTo(target, SegmentButtonSpring)
        }

        val scrollOffset by remember(segmentLeftsPx, resolvedListState) {
            derivedStateOf {
                val firstIndex = resolvedListState.firstVisibleItemIndex
                (segmentLeftsPx.getOrElse(firstIndex) { 0 } + resolvedListState.firstVisibleItemScrollOffset).toFloat()
            }
        }
        val currentSelectedTabIndex by rememberUpdatedState(selectedTabIndex)
        val currentCommitOnRelease by rememberUpdatedState(commitOnRelease)
        val currentActiveTabIndex by rememberUpdatedState(activeSelectedTabIndex)
        val onSelectedPressedChange: (Boolean) -> Unit = remember { { pressed -> selectedTabPressed = pressed } }
        val containerPath = remember { Path() }
        val indicatorPath = remember { Path() }

        val slideSelectModifier = if (dragSelectionEnabled && segmentWidthsPx.size > 1) {
            Modifier.pointerInput(segmentWidthsPx, segmentLeftsPx, contourPaddingPx, switchDistancePx) {
                val touchSlop = viewConfiguration.touchSlop
                val lastSegment = segmentWidthsPx.lastIndex
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val pointerId = down.id
                        val downContentX = down.position.x - contourPaddingPx + scrollOffset
                        val pressedSegment = resolveSegmentAtX(downContentX, segmentLeftsPx, segmentWidthsPx)
                        val startIndex = currentActiveTabIndex
                        val reachesIndicator = abs(downContentX - indicator.centerX) <= indicator.width / 2f + SEGMENT_INDICATOR_TOUCH_SLOP_PX
                        // COUISegmentButtonLayout only drags from the indicator; any other press is
                        // a tap handled by the segment itself.
                        if (pressedSegment != startIndex && !reachesIndicator) continue
                        var dragging = false
                        var cancelled = false
                        var lastIndex = startIndex
                        selectedSegmentPressed = true
                        dragPressedIndex = pressedSegment
                        indicator.beginDrag(downContentX)
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == pointerId }
                            if (change == null) {
                                cancelled = true
                                break
                            }
                            if (change.changedToUpIgnoreConsumed()) {
                                if (dragging) change.consume()
                                break
                            }
                            val dx = change.position.x - down.position.x
                            val dy = change.position.y - down.position.y
                            if (!dragging) {
                                if (abs(dy) > touchSlop && abs(dy) > abs(dx)) {
                                    cancelled = true
                                    break
                                }
                                dragging = abs(dx) > touchSlop
                            }
                            if (dragging) change.consume()
                            val contentX = change.position.x - contourPaddingPx + scrollOffset
                            // Past the first 5dp of a neighbouring segment the selection hops over;
                            // until then the indicator only leans toward the finger.
                            val hovered = segmentUnderFinger(contentX, segmentLeftsPx, segmentWidthsPx, switchDistancePx)
                            if (hovered >= 0 && hovered != lastIndex) {
                                lastIndex = hovered
                                dragPressedIndex = hovered
                                dragPreviewIndex = hovered
                                currentHaptic(CouiHapticEffect.Switch)
                                if (!currentCommitOnRelease) currentOnTabSelected(hovered)
                            } else if (!indicator.isPositionRunning) {
                                val scale = indicatorScale.value
                                val atEnd = lastIndex == 0 || lastIndex == lastSegment
                                val overDistance = (1f - scale) * indicator.width / 2f + if (atEnd) 0f else switchDistancePx
                                indicator.dragTo(contentX, overDistance)
                            }
                        }
                        selectedSegmentPressed = false
                        dragPressedIndex = -1
                        if (!indicator.isPositionRunning) {
                            indicator.settle(segmentLeftsPx[lastIndex] + segmentWidthsPx[lastIndex] / 2f)
                        }
                        if (lastIndex != startIndex || dragPreviewIndex != null) {
                            if (cancelled) {
                                dragPreviewIndex = null
                            } else {
                                val finalSelection = dragPreviewIndex
                                if (currentCommitOnRelease && finalSelection != null && finalSelection != currentSelectedTabIndex) {
                                    currentOnTabSelected(finalSelection)
                                }
                                dragPreviewIndex = null
                            }
                        }
                    }
                }
            }
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(slideSelectModifier)
                .drawBehind {
                    // 1. Container capsule (COUISegmentButtonLayout.drawButtonBackground).
                    val containerRadius = min((cornerRadius + contourPadding).toPx(), size.height / 2f)
                    containerPath.reset()
                    if (containerRadius >= size.height / 2f) {
                        containerPath.addSegmentCapsule(Rect(Offset.Zero, size))
                    } else {
                        containerPath.addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(containerRadius)))
                    }
                    if (containerColor.alpha > 0f && !glassActive) {
                        drawPath(containerPath, color = containerColor)
                    }
                    if (segmentWidthsPx.isEmpty()) return@drawBehind
                    val pad = contourPaddingPx
                    val scale = indicatorScale.value
                    val indicatorW = indicator.width * scale
                    val indicatorH = indicator.height * scale
                    if (indicatorW <= 0f || indicatorH <= 0f) return@drawBehind
                    val centerX = pad + indicator.centerX - scrollOffset
                    val centerY = size.height / 2f
                    // COUISegmentButtonLayout.clampIndicatorBounds keeps an overshooting indicator
                    // inside the contour, flattening it against the end of the row.
                    val scrolls = segmentLeftsPx.last() + segmentWidthsPx.last() > viewportPx + 1f
                    val rect = Rect(
                        left = (centerX - indicatorW / 2f).let { if (scrolls) it else max(it, pad) },
                        top = centerY - indicatorH / 2f,
                        right = (centerX + indicatorW / 2f).let { if (scrolls) it else min(it, size.width - pad) },
                        bottom = centerY + indicatorH / 2f,
                    )
                    val radius = min(cornerRadius.toPx(), indicatorH / 2f)
                    indicatorPath.reset()
                    if (radius >= indicatorH / 2f) {
                        indicatorPath.addSegmentCapsule(rect)
                    } else {
                        indicatorPath.addRoundRect(RoundRect(rect, CornerRadius(radius)))
                    }
                    val drawSegment: DrawScope.() -> Unit = {
                        // 2. Indicator shadow, clipped outside the indicator
                        // (COUISegmentButtonLayout.drawShadow): it reaches the bottom of the
                        // container and fades radially from the indicator's centre in light themes.
                        clipPath(indicatorPath, clipOp = ClipOp.Difference) {
                            val shadowDy = IndicatorShadowOffsetY.toPx()
                            val shadowTop = rect.top + shadowDy
                            val shadowBottom = if (pad > 0f) size.height else rect.bottom + shadowDy
                            val shadowBrush = if (isDarkTheme) {
                                SolidColor(shadowColor)
                            } else {
                                Brush.radialGradient(
                                    colors = listOf(shadowColor, shadowEndColor),
                                    center = rect.center,
                                    radius = rect.width / 2f,
                                )
                            }
                            drawRoundRect(
                                brush = shadowBrush,
                                topLeft = Offset(rect.left, shadowTop),
                                size = Size(rect.width, shadowBottom - shadowTop),
                                cornerRadius = CornerRadius(indicatorH / 2f),
                            )
                        }
                        // 3. Indicator capsule fill.
                        drawPath(indicatorPath, color = indicatorColor)
                        // 4. Optional indicator material: rim and inner shadow on the indicator's own bounds.
                        val material = indicatorMaterial
                        val shader = indicatorShader
                        if (material != null && shader != null) {
                            setMaterialShadowEdgeUniforms(
                                shader = shader,
                                width = rect.width,
                                height = rect.height,
                                radius = radius,
                                stroke = material,
                                cornerWeight = MaterialShadowEdgeDefaults.CornerWeight,
                                antiAliasing = MaterialShadowEdgeDefaults.AntiAliasing,
                                edgeStyle = GradientStrokeStyle.EDGE_STYLE_0,
                                shadowStyle = InnerShadowStyle.SHADOW_STYLE_0,
                                density = density.density,
                            )
                            // The edge shader measures fragCoord in canvas space, not in the
                            // drawRect's local space, so a rect drawn off-origin would keep its
                            // rim anchored at the canvas origin. Translate the canvas so the
                            // shader's own (0,0) is the indicator's top-left.
                            translate(left = rect.left, top = rect.top) {
                                drawRect(brush = shader.asBrush(), size = rect.size)
                            }
                        }
                    }
                    // COUISegmentButtonLayout never clips the indicator: it stays inside the contour
                    // on its own. A row that scrolls has to, and a frameless TabRow must not, or
                    // the shadow past its bottom edge would shear off.
                    if (scrolls && containerColor.alpha > 0f) {
                        clipPath(containerPath) { drawSegment() }
                    } else {
                        drawSegment()
                    }
                },
        ) {
            // 4. Labels, composed on top of the canvas.
            LazyRow(
                state = resolvedListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contourPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(itemSpacing),
                overscrollEffect = null,
            ) {
                itemsIndexed(tabs) { index, label ->
                    SegmentTab(
                        text = label,
                        isSelected = activeSelectedTabIndex == index,
                        onClick = { currentOnTabSelected.invoke(index) },
                        dragPressed = dragPressedIndex == index,
                        width = with(density) { segmentWidthsPx[index].toDp() },
                        color = colors.contentColor(activeSelectedTabIndex == index),
                        fontSize = labelFontSize,
                        segmentPadding = segmentPadding,
                        contentAlignment = contentAlignment,
                        interactionSource = interactionSource,
                        indication = indication,
                        onSelectedPressedChange = onSelectedPressedChange,
                    )
                }
            }
        }
    }
}

/** `COUISegmentButtonLayout.isTouchInIndicator`: a press this close to the indicator still grabs it. */
private const val SEGMENT_INDICATOR_TOUCH_SLOP_PX = 10f

/** `coui_segment_switch_distance`: how far into a segment the finger must reach to select it. */
private val SegmentSwitchDistance = 5.dp

/** `coui_segment_regular_extra_width`: how far a growing indicator may overshoot its segment. */
private val SegmentRegularExtraWidth = 7.dp

/** `coui_segment_small_extra_width`: [SegmentRegularExtraWidth] for the `SegmentButton.Tiny` style. */
private val SegmentSmallExtraWidth = 5.dp

/** Halfway between the `coui_segment_padding` (4dp) and `coui_segment_padding_tiny` (2dp) styles. */
private val SegmentCompactPaddingThreshold = 3.dp

/** `CONTENT_SEGMENT_BUTTON_PARAMS` blur sigma, in pixels. */
private const val SEGMENT_NATIVE_BLUR_SIGMA_PX = 25f

private fun resolveSegmentAtX(contentX: Float, lefts: List<Int>, widths: List<Int>): Int {
    if (widths.isEmpty()) return 0
    if (contentX <= lefts.first()) return 0
    for (i in widths.indices) {
        val right = lefts[i] + widths[i]
        if (contentX <= right) return i
        if (i + 1 < widths.size) {
            val midGap = (right + lefts[i + 1]) * 0.5f
            if (contentX < midGap) return i
        }
    }
    return widths.lastIndex
}

/**
 * `COUISegmentButtonLayout.getTouchedChildIndex` while dragging: the segment [contentX] sits in, at
 * least [inset] from its edges, or -1 in the dead band around a boundary.
 */
private fun segmentUnderFinger(contentX: Float, lefts: List<Int>, widths: List<Int>, inset: Float): Int {
    for (i in widths.indices) {
        if (contentX >= lefts[i] + inset && contentX <= lefts[i] + widths[i] - inset) return i
    }
    return -1
}

@Composable
private fun SegmentTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    dragPressed: Boolean,
    width: Dp,
    color: Color,
    fontSize: TextUnit,
    segmentPadding: Dp,
    contentAlignment: Alignment,
    interactionSource: MutableInteractionSource?,
    indication: Indication?,
    onSelectedPressedChange: (Boolean) -> Unit,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val currentOnSelectedPressedChange by rememberUpdatedState(onSelectedPressedChange)
    LaunchedEffect(isPressed, isSelected) {
        currentOnSelectedPressedChange(isPressed && isSelected)
    }
    // COUIPressFeedbackHelper on the segment label.
    val scaleProgress by animateFloatAsState(
        targetValue = if (isPressed || dragPressed) 1f else 0f,
        animationSpec = SegmentButtonSpring,
        label = "segmentPressScale",
    )
    // COUISegmentButtonLayout.setSelectedAt: color crossfades over 300ms, weight switches instantly.
    val textColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(TextColorAnimationDuration, easing = TextColorEasing),
        label = "segmentTextColor",
    )
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(width)
            .graphicsLayer {
                val scale = 1f - (1f - pressedScaleEndRatio(size)) * scaleProgress
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = indication,
                onClick = onClick,
            )
            .semantics {
                role = Role.Tab
                selected = isSelected
            }
            .padding(horizontal = segmentPadding),
        contentAlignment = contentAlignment,
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            fontSize = fontSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Resolve the widths of all segments (COUISegmentButtonLayout.onMeasure).
 *
 * When the row cannot give every segment [minWidthPx] the segments keep their natural widths and
 * the row scrolls instead.
 */
private fun computeSegmentWidths(
    naturalWidths: List<Int>,
    availablePx: Float,
    minWidthPx: Int,
    maxWidthPx: Int,
): List<Int> {
    val count = naturalWidths.size
    if (count == 0) return emptyList()
    if (availablePx <= 0f || availablePx / count < minWidthPx) {
        return naturalWidths.map { min(it, maxWidthPx) }
    }
    return distributeSegmentWidths(naturalWidths, availablePx).map { min(it, maxWidthPx) }
}

/**
 * Distribute the available width among segments (COUISegmentButtonLayout.calculateChildrenWidths):
 * narrow segments keep their natural width and the remaining space is split among the wider ones.
 */
private fun distributeSegmentWidths(
    naturalWidths: List<Int>,
    availablePx: Float,
): List<Int> {
    val count = naturalWidths.size
    val sortedIndices = naturalWidths.indices.sortedBy { naturalWidths[it] }
    val fairShare = availablePx / count
    val widths = FloatArray(count)
    var remaining = availablePx
    var share = fairShare
    var lockedIndex = -1
    var i = 0
    while (i < count) {
        val natural = naturalWidths[sortedIndices[i]].toFloat()
        if (natural < share) {
            widths[sortedIndices[i]] = natural
            remaining -= natural
            i++
        } else if (lockedIndex == i) {
            while (i < count) {
                widths[sortedIndices[i]] = share
                i++
            }
            remaining = 0f
        } else {
            share = remaining / (count - i)
            lockedIndex = i
        }
    }
    if (lockedIndex == -1) {
        return List(count) { fairShare.roundToInt() }
    }
    if (remaining > 0f) {
        for (j in 0 until count - 1) {
            widths[sortedIndices[j]] += remaining / (count - 1)
        }
    }
    return List(count) { widths[it].roundToInt() }
}

object TabRowDefaults {
    /**
     * The colour layers of `CONTENT_SEGMENT_BUTTON_PARAMS`, which `COUISegmentButtonLayout` lays
     * over its blurred backdrop: `#7F333333` in `ColorDodge` then `#99EFEFEF` in `Luminosity` (light),
     * `#7F7F7F7F` in `Overlay` then `#99262626` in `Luminosity` (dark).
     */
    fun segmentBlurColors(isDark: Boolean): List<BlendColorEntry> = if (isDark) SegmentBlurColorsDark else SegmentBlurColorsLight

    private val SegmentBlurColorsLight = listOf(
        BlendColorEntry(color = Color(0x7F333333), mode = BlurBlendMode.ColorDodge),
        BlendColorEntry(color = Color(0x99EFEFEF), mode = BlurBlendMode.Luminosity),
    )
    private val SegmentBlurColorsDark = listOf(
        BlendColorEntry(color = Color(0x7F7F7F7F), mode = BlurBlendMode.Overlay),
        BlendColorEntry(color = Color(0x99262626), mode = BlurBlendMode.Luminosity),
    )

    /**
     * The default height of the [TabRow]. COUI `SegmentButton` style height (40dp).
     */
    val TabRowHeight = 40.dp

    /**
     * The default height of the [TabRowWithContour]. COUI `SegmentButton` style height (40dp).
     */
    val TabRowWithContourHeight = 40.dp

    /**
     * The height of the COUI `SegmentButton.Tiny` style (32dp), for use as [TabRowWithContour]'s
     * `height` together with [TabRowWithContourTinyPadding] and [TabRowWithContourTinyCornerRadius].
     */
    val TabRowWithContourTinyHeight = 32.dp

    /**
     * The default corner radius of the sliding indicator in [TabRow]. COUI draws capsules
     * (radius = height / 2), hence 20dp for the full 40dp height.
     */
    val TabRowCornerRadius = 20.dp

    /**
     * The default corner radius of the sliding indicator in [TabRowWithContour]. Half the 32dp
     * indicator height (40dp container minus the 4dp inset on both sides).
     */
    val TabRowWithContourCornerRadius = 16.dp

    /**
     * The indicator corner radius of the COUI `SegmentButton.Tiny` style: (32dp - 2 * 2dp) / 2.
     */
    val TabRowWithContourTinyCornerRadius = 14.dp

    /**
     * The default inset between the container and the indicator in [TabRowWithContour].
     * COUI `SegmentButton` style `android:padding` (4dp).
     */
    val TabRowWithContourPadding = 4.dp

    /**
     * The contour inset of the COUI `SegmentButton.Tiny` style (`android:padding` 2dp).
     */
    val TabRowWithContourTinyPadding = 2.dp

    /**
     * The default minimum width of the tab in [TabRow]. COUI `coui_segment_min_width` (52dp).
     */
    val TabRowMinWidth = 52.dp

    /**
     * The default minimum width of the tab in [TabRowWithContour].
     * COUI `coui_segment_min_width` (52dp).
     */
    val TabRowWithContourMinWidth = 52.dp

    /**
     * The default maximum width of the tab in [TabRow]. COUI segments have no maximum width.
     */
    val TabRowMaxWidth = Dp.Infinity

    /**
     * The default maximum width of the tab in [TabRowWithContour]. COUI segments have no maximum width.
     */
    val TabRowWithContourMaxWidth = Dp.Infinity

    /**
     * The default colors for the [TabRow] and [TabRowWithContour], following
     * COUISegmentButtonLayout: both label states default to `couiColorLabelPrimary` — the
     * selected state is distinguished by the indicator and the medium font weight.
     *
     * @param indicatorShadowColor The drop shadow under the sliding indicator.
     *   `coui_segment_button_shadow_color`; the light and dark themes carry different alphas.
     */
    @Composable
    fun tabRowColors(
        backgroundColor: Color = if (isDarkTheme()) SegmentContainerDark else SegmentContainerLight,
        contentColor: Color = COUITheme.colorScheme.onSurface,
        selectedBackgroundColor: Color = if (isDarkTheme()) SegmentIndicatorDark else SegmentIndicatorLight,
        selectedContentColor: Color = COUITheme.colorScheme.onSurface,
        indicatorShadowColor: Color = if (isDarkTheme()) IndicatorShadowDark else IndicatorShadowLight,
    ): TabRowColors = remember(backgroundColor, contentColor, selectedBackgroundColor, selectedContentColor, indicatorShadowColor) {
        TabRowColors(
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            selectedBackgroundColor = selectedBackgroundColor,
            selectedContentColor = selectedContentColor,
            indicatorShadowColor = indicatorShadowColor,
        )
    }

    @Composable
    private fun isDarkTheme(): Boolean = COUITheme.colorScheme.background.luminance() < 0.5f

    /**
     * The opaque segment capsule ColorOS Settings draws on its Privacy & security page: the
     * container is the app's opaque `color_segment_button_background` (`#E2E3E4` light,
     * `#353535` dark), while the indicator keeps COUI's translucent
     * `coui_color_segment_button_indicator`, so it renders as `#F6F7F7` / `#5D5D5D` over the
     * container. Unlike [tabRowColors], content behind the row does not tint the container.
     *
     * Sampled from the ColorOS 17 Privacy & security dashboard (the Privacy / Security capsule
     * above the pager). The label, indicator shadow and other theme-driven roles stay the same
     * as the default pair.
     */
    @Composable
    fun opaqueTabRowColors(
        backgroundColor: Color = if (isDarkTheme()) OpaqueContainerDark else OpaqueContainerLight,
        contentColor: Color = COUITheme.colorScheme.onSurface,
        selectedBackgroundColor: Color = if (isDarkTheme()) SegmentIndicatorDark else SegmentIndicatorLight,
        selectedContentColor: Color = COUITheme.colorScheme.onSurface,
        indicatorShadowColor: Color = if (isDarkTheme()) IndicatorShadowDark else IndicatorShadowLight,
    ): TabRowColors = tabRowColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        selectedBackgroundColor = selectedBackgroundColor,
        selectedContentColor = selectedContentColor,
        indicatorShadowColor = indicatorShadowColor,
    )

    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Migrated to the overload that also takes indicatorShadowColor.",
        level = DeprecationLevel.HIDDEN,
    )
    @Composable
    fun tabRowColors(
        backgroundColor: Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) SegmentContainerDark else SegmentContainerLight,
        contentColor: Color = COUITheme.colorScheme.onSurface,
        selectedBackgroundColor: Color = if (COUITheme.colorScheme.background.luminance() < 0.5f) SegmentIndicatorDark else SegmentIndicatorLight,
        selectedContentColor: Color = COUITheme.colorScheme.onSurface,
    ): TabRowColors = tabRowColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        selectedBackgroundColor = selectedBackgroundColor,
        selectedContentColor = selectedContentColor,
    )
}

@Immutable
data class TabRowColors(
    private val backgroundColor: Color,
    private val contentColor: Color,
    private val selectedBackgroundColor: Color,
    private val selectedContentColor: Color,
    private val indicatorShadowColor: Color = Color.Unspecified,
) {
    /**
     * The four-color form, kept so callers written before [indicatorShadowColor] existed still
     * compile; the shadow then falls back to the theme's own segment shadow.
     */
    constructor(
        backgroundColor: Color,
        contentColor: Color,
        selectedBackgroundColor: Color,
        selectedContentColor: Color,
    ) : this(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        selectedBackgroundColor = selectedBackgroundColor,
        selectedContentColor = selectedContentColor,
        indicatorShadowColor = Color.Unspecified,
    )

    @Stable
    internal fun backgroundColor(selected: Boolean): Color = if (selected) selectedBackgroundColor else backgroundColor

    @Stable
    internal fun contentColor(selected: Boolean): Color = if (selected) selectedContentColor else contentColor

    /** [Color.Unspecified] when the caller built this instance without the factory. */
    @Stable
    internal fun indicatorShadowColor(): Color = indicatorShadowColor

    /** The four-color [copy], kept for binary compatibility with callers built before 1.3.0. */
    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Migrated to the copy that also takes indicatorShadowColor.",
        level = DeprecationLevel.HIDDEN,
    )
    fun copy(
        backgroundColor: Color = this.backgroundColor,
        contentColor: Color = this.contentColor,
        selectedBackgroundColor: Color = this.selectedBackgroundColor,
        selectedContentColor: Color = this.selectedContentColor,
    ): TabRowColors = copy(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        selectedBackgroundColor = selectedBackgroundColor,
        selectedContentColor = selectedContentColor,
        indicatorShadowColor = indicatorShadowColor,
    )
}

/**
 * COUISpringForce(response = 0.3f, bounce = 0f): a critically damped spring with stiffness
 * (2 * PI / 0.3)^2 ~= 438.65, used for the indicator springs and the press scale.
 */
private val SegmentButtonSpring = couiSegmentSpring(response = 0.3f, bounce = 0f)

/** COUISpringForce conversion used by the native segment indicator. */
private fun couiSegmentSpring(response: Float, bounce: Float): androidx.compose.animation.core.SpringSpec<Float> = spring(dampingRatio = 1f - bounce, stiffness = ((2f * kotlin.math.PI.toFloat() / response) * (2f * kotlin.math.PI.toFloat() / response)))

/** COUIMoveEaseInterpolator: PathInterpolator(0.3f, 0f, 0.1f, 1f). */
private val TextColorEasing = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)

/** COUISegmentButtonLayout text color animator duration (COLOR_ANIMATOR_TIME = 300ms). */
private val TextColorAnimationDuration = 300

/** COUI `coui_segment_btn_padding_horizontal` (12dp) — horizontal text padding of a segment. */
private val SegmentPaddingHorizontal = 12.dp

/** COUI `coui_segment_shadow_offset` (2dp) — the vertical offset of the indicator shadow. */
private val IndicatorShadowOffsetY = 2.dp

/** COUI `coui_segment_button_shadow_end_color` (#03000000) over `coui_segment_button_shadow_color` (#0D000000). */
private const val INDICATOR_SHADOW_END_RATIO = 3f / 13f

/** COUI `coui_segment_button_shadow_color` in light themes (#0D000000). */
private val IndicatorShadowLight = Color(0x0D000000)

/** COUI `coui_segment_button_shadow_color` in dark themes (#14000000). */
private val IndicatorShadowDark = Color(0x14000000)

/** COUI `coui_color_segment_button_background` in light themes (#0F000000). */
private val SegmentContainerLight = Color(0x0F000000)

/** COUI `coui_color_segment_button_background` in dark themes (#1FFFFFFF). */
private val SegmentContainerDark = Color(0x1FFFFFFF)

/** COUI `coui_color_segment_button_indicator` in light themes (#B2FFFFFF). */
private val SegmentIndicatorLight = Color(0xB2FFFFFF)

/** COUI `coui_color_segment_button_indicator` in dark themes (#33FFFFFF). */
private val SegmentIndicatorDark = Color(0x33FFFFFF)

/** Privacy & security dashboard `color_segment_button_background` in light themes (#E2E3E4). */
private val OpaqueContainerLight = Color(0xFFE2E3E4)

/** Privacy & security dashboard `color_segment_button_background` in dark themes (#353535). */
private val OpaqueContainerDark = Color(0xFF353535)

/** COUI `COUIInEaseInterpolator`; shares [COUIInEase]. Maps the segment area to the pressed end scale. */
private val PressedScaleEasing = COUIInEase

/** COUIPressFeedbackHelper area bounds: coui_min_end_value_size (48dp) squared. */
private val PressedScaleMinAreaDp2 = 48f * 48f

/** COUIPressFeedbackHelper area bounds: coui_max_end_value_width x height (328dp x 220dp). */
private val PressedScaleMaxAreaDp2 = 328f * 220f

/** The pressed end scale of the smallest surfaces (COUIPressFeedbackHelper DEFAULT_SCALE_END_RATIO). */
private val PressedScaleMin = 0.92f

/** The pressed end scale of the largest surfaces (COUIPressFeedbackHelper MAX_SCALE_END_RATIO). */
private val PressedScaleMax = 0.98f

/**
 * The scale a segment of [size] shrinks to when fully pressed: [PressedScaleMin] for areas up to
 * 48 x 48dp, easing towards [PressedScaleMax] at 328 x 220dp
 * (COUIPressFeedbackHelper.getCardScaleRatio).
 */
private fun Density.pressedScaleEndRatio(size: Size): Float {
    val area = size.width.toDp().value * size.height.toDp().value
    return when {
        area <= PressedScaleMinAreaDp2 -> PressedScaleMin

        area >= PressedScaleMaxAreaDp2 -> PressedScaleMax

        else -> PressedScaleMin + (PressedScaleMax - PressedScaleMin) *
            PressedScaleEasing.transform((area - PressedScaleMinAreaDp2) / (PressedScaleMaxAreaDp2 - PressedScaleMinAreaDp2))
    }
}
