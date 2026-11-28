// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.segmentbutton

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import io.github.suqi8.coui.kmp.kit.backdrop.CouiBackdrop
import io.github.suqi8.coui.kmp.kit.effect.CouiContour
import io.github.suqi8.coui.kmp.kit.effect.CouiContourStyle
import io.github.suqi8.coui.kmp.kit.effect.CouiFloatingRenderer
import io.github.suqi8.coui.kmp.kit.effect.CouiFloatingShadow
import io.github.suqi8.coui.kmp.kit.effect.CouiFloatingSurface
import io.github.suqi8.coui.kmp.kit.effect.LocalCouiContourGlowEnabled
import io.github.suqi8.coui.kmp.kit.effect.SpotlightPreset
import io.github.suqi8.coui.kmp.kit.effect.couiButtonFeedback
import io.github.suqi8.coui.kmp.kit.effect.couiFloatingSurface
import io.github.suqi8.coui.kmp.kit.effect.isKitRuntimeShaderSupported
import io.github.suqi8.coui.kmp.kit.effect.isKitSurfaceSupported
import io.github.suqi8.coui.kmp.kit.effect.preferFastAnimatingFloatingSurfaces
import io.github.suqi8.coui.kmp.kit.effect.rememberCouiSpotlight
import io.github.suqi8.coui.kmp.kit.input.isCouiTouchCancelled
import io.github.suqi8.coui.kmp.kit.motion.CouiPressMotion
import io.github.suqi8.coui.kmp.kit.motion.CouiTouchMotion
import io.github.suqi8.coui.kmp.kit.motion.CouiVelocityTracker
import io.github.suqi8.coui.kmp.kit.motion.rememberCouiPressMotion
import io.github.suqi8.coui.kmp.kit.motion.rememberCouiTouchMotion
import io.github.suqi8.coui.kmp.kit.shape.CouiCapsuleShape
import io.github.suqi8.coui.kmp.kit.shape.CouiCornerWeight
import io.github.suqi8.coui.kmp.kit.shape.CouiShape
import io.github.suqi8.coui.kmp.kit.theme.CouiTheme
import io.github.suqi8.coui.kmp.utils.CouiHapticEffect
import io.github.suqi8.coui.kmp.utils.rememberCouiHaptic
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Controlled capsule segmented control migrated from COUI-Kit.
 *
 * Uses plain fill when [backdrop] is `null`, and ColorOS 17 blurred glass material when [backdrop]
 * is non-null on supported platforms (Android API 34+ or Skiko). The edge follows the system
 * `TYPE_FRAMEWORK_CAPSULE_6` recipe (`useMenuLightEdge = false`): the inner glow stacks additively
 * instead of blending by coverage, and the edge highlight skips the menu reference-color
 * calibration. In light glass mode, the indicator drop shadow uses the faint measured glass
 * falloff (`0.006f` -> transparent) when [shadowColor] is unspecified or the plain light default
 * (`0x0D000000`), and the runtime-shader indicator contour rim uses the native `.15f` `lineAlpha`.
 * Defaults to committing on drag crossing; [commitOnRelease] defers commit until valid UP.
 */
@Composable
internal fun SegmentedControl(
    modifier: Modifier = Modifier,
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    onReselected: ((Int) -> Unit)? = null,
    onTabClick: ((Int) -> Unit)? = null,
    backdrop: CouiBackdrop? = null,
    dark: Boolean = CouiTheme.isDark,
    enabled: Boolean = true,
    small: Boolean = false,
    dragSelectionEnabled: Boolean = true,
    commitOnRelease: Boolean = false,
    touchMotionEnabled: Boolean = true,
    minWidth: Dp = CouiSegmentedControlDefaults.minWidth,
    maxWidth: Dp = Dp.Infinity,
    height: Dp = if (small) CouiSegmentedControlDefaults.smallHeight else CouiSegmentedControlDefaults.height,
    cornerRadius: Dp = Dp.Unspecified,
    contourPadding: Dp = if (small) CouiSegmentedControlDefaults.smallPadding else CouiSegmentedControlDefaults.padding,
    segmentPadding: Dp = CouiSegmentedControlDefaults.labelPadding,
    contentAlignment: Alignment = Alignment.Center,
    interactionSource: MutableInteractionSource? = null,
    textStyle: TextStyle = CouiSegmentedControlDefaults.textStyle(),
    containerColor: Color = CouiSegmentedControlDefaults.containerColor(dark),
    indicatorColor: Color = CouiSegmentedControlDefaults.indicatorColor(dark),
    contentColor: Color = Color.Unspecified,
    selectedContentColor: Color = Color.Unspecified,
    shadowColor: Color = Color.Unspecified,
    tabModifier: ((index: Int) -> Modifier)? = null,
    tabContent: (@Composable (index: Int, label: String, selected: Boolean) -> Unit)? = null,
) {
    if (labels.isEmpty()) return
    val safeSelected = selectedIndex.coerceIn(0, labels.lastIndex)
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val current = rememberUpdatedState(safeSelected)
    val submit = rememberUpdatedState(onSelected)
    val reselect = rememberUpdatedState(onReselected)
    val tap = rememberUpdatedState(onTabClick)
    val haptic = rememberUpdatedState(rememberCouiHaptic())
    val session = remember(labels) { SegmentSelectionSession(safeSelected) }
    var lightCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var inputCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val motion = remember { CouiSegmentMotion() }
    val touch = rememberCouiTouchMotion(enabled && touchMotionEnabled)
    val press = rememberCouiPressMotion()
    val indicatorPress = rememberCouiPressMotion()
    val labelPresses = labels.indices.map { key(it) { rememberCouiPressMotion() } }

    val surfaceSupported = isKitSurfaceSupported()
    val runtimeShaderSupported = isKitRuntimeShaderSupported()
    val glassSupported = surfaceSupported && backdrop != null
    val renderer = remember(glassSupported) {
        if (glassSupported) runCatching { CouiFloatingRenderer.standalone() }.getOrNull() else null
    }
    val effectiveBackdrop = if (glassSupported && renderer != null) backdrop else null
    val contourEnabled = runtimeShaderSupported && effectiveBackdrop != null && LocalCouiContourGlowEnabled.current
    val contour = remember(contourEnabled) {
        if (contourEnabled) runCatching { CouiContour(style = CouiContourStyle.Button) }.getOrNull() else null
    }
    val edgePaint = remember { Paint().apply { isAntiAlias = true } }
    val indicatorShape: Shape = remember(cornerRadius) {
        if (cornerRadius.isSpecified) SegmentIndicatorShape(cornerRadius) else CouiCapsuleShape
    }
    val containerShape: Shape = CouiCapsuleShape
    val useGlassLightShadow = !dark && effectiveBackdrop != null &&
        (shadowColor == Color.Unspecified || shadowColor == Color(0x0D000000))
    val resolvedShadowColor = when {
        useGlassLightShadow -> Color.Black.copy(alpha = 0.006f)
        else -> shadowColor.takeOrElse { Color(if (dark) 0x14000000 else 0x0D000000) }
    }
    val resolvedShadowEndColor = when {
        useGlassLightShadow -> Color.Transparent
        else -> resolvedShadowColor.copy(alpha = resolvedShadowColor.alpha * (3f / 13f))
    }
    val indicatorDrawing = remember(density, rtl, resolvedShadowColor, resolvedShadowEndColor) {
        SegmentIndicatorDrawing(resolvedShadowColor, resolvedShadowEndColor)
    }
    val contentLayer = if (effectiveBackdrop != null) rememberGraphicsLayer() else null
    val defaultTextColor = textStyle.color.takeOrElse { CouiTheme.colorSchemeFor(dark).onSurface }
    val unselectedLabelColor = contentColor.takeOrElse { defaultTextColor }
    val selectedLabelColor = selectedContentColor.takeOrElse { defaultTextColor }

    val (lightInput, light) = if (effectiveBackdrop != null) {
        rememberCouiSpotlight(
            enabled,
            SpotlightPreset.OpaqueMedium2,
            dark,
            noise = CouiSegmentedControlDefaults.spotlightNoise(dark),
            dither = CouiSegmentedControlDefaults.spotlightDither(dark),
            inputTarget = { _, _ -> if (session.allowLight) lightCoordinates else null },
        )
    } else {
        Modifier to null
    }

    val layoutDirection = LocalLayoutDirection.current
    val measurer = rememberTextMeasurer()
    val weightAdjustment = io.github.suqi8.coui.kmp.theme.LocalPlatformFontWeightAdjustment.current
    val adjustedTextStyle = remember(textStyle, weightAdjustment) {
        textStyle.copy(
            fontWeight = io.github.suqi8.coui.kmp.theme.adjustPlatformFontWeight(
                textStyle.fontWeight,
                weightAdjustment,
            ),
        )
    }
    val whiteLabelStyle = remember(adjustedTextStyle) {
        adjustedTextStyle.copy(
            textAlign = TextAlign.Center,
            color = Color.White,
        )
    }
    // Re-measure natural widths if asynchronous font loading resolves after first composition.
    val resolvedTypeface = LocalFontFamilyResolver.current.resolve(
        adjustedTextStyle.fontFamily,
        adjustedTextStyle.fontWeight ?: FontWeight.Normal,
        adjustedTextStyle.fontStyle ?: FontStyle.Normal,
        adjustedTextStyle.fontSynthesis ?: FontSynthesis.All,
    ).value
    val minWidthPx = with(density) { minWidth.roundToPx() }
    val maxWidthPx = if (maxWidth == Dp.Infinity) Int.MAX_VALUE else with(density) { maxWidth.roundToPx().coerceAtLeast(minWidthPx) }
    val segmentPaddingPx = with(density) { segmentPadding.roundToPx() }
    val extraLabelSlackPx = if (preferFastAnimatingFloatingSurfaces || weightAdjustment != 0) 2 else 1
    val naturalWidths = remember(labels, adjustedTextStyle, density, measurer, resolvedTypeface, minWidthPx, maxWidthPx, segmentPaddingPx, extraLabelSlackPx) {
        labels.map { label ->
            val measured = ceil(measurer.measure(label, adjustedTextStyle, maxLines = 1).size.width.toFloat()).toInt() +
                extraLabelSlackPx + segmentPaddingPx * 2
            measured.coerceIn(minWidthPx, maxWidthPx).toFloat()
        }
    }

    LaunchedEffect(safeSelected, session.touching) { session.sync(safeSelected) }
    LaunchedEffect(enabled) {
        if (!enabled) {
            session.finish(true, current.value, commitOnRelease) { }
            indicatorPress.release()
            touch.release(Offset.Zero)
            press.release()
            labelPresses.forEach { it.release() }
            motion.release()
        }
    }
    DisposableEffect(motion) { onDispose { motion.dispose() } }

    BoxWithConstraints(modifier.height(height)) {
        val defaultPadding = with(density) { contourPadding.roundToPx() }
        val desired = (naturalWidths.sum() + defaultPadding * 2f).roundToInt()
        val actualWidth = if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            constraints.constrainWidth(desired)
        }
        val actualHeight = constraints.maxHeight
        val padding = defaultPadding.coerceAtMost(actualWidth / 2)
        // Labels that do not fit at their natural width scroll inside the capsule instead of being
        // squeezed, so a long row keeps the same glass container, rim and indicator as a short one.
        val naturalContent = ceil(naturalWidths.sum()).toInt()
        val scrolls = naturalContent > actualWidth - padding * 2
        val contentWidth = if (scrolls) naturalContent + padding * 2 else actualWidth
        val maxWidthLimitPx = if (maxWidth == Dp.Infinity) Float.MAX_VALUE else maxWidthPx.toFloat()
        val geometry = remember(contentWidth, actualHeight, padding, naturalWidths, rtl, small, maxWidthLimitPx) {
            SegmentGeometry(
                contentWidth.toFloat(),
                actualHeight.toFloat(),
                padding.toFloat(),
                segmentWidths(naturalWidths, contentWidth - padding * 2, maxWidthLimitPx),
                rtl,
                small,
            )
        }
        val scroll = rememberScrollState()
        // Where the scrolled content's left edge sits in the capsule; horizontalScroll places the
        // content relative to the layout direction, so RTL anchors it to the right edge.
        val currentContentWidth by rememberUpdatedState(contentWidth)
        val currentActualWidth by rememberUpdatedState(actualWidth)
        val contentOffsetX: () -> Float = remember(scroll, rtl) {
            {
                if (rtl) {
                    (currentActualWidth - currentContentWidth + scroll.value).toFloat()
                } else {
                    -scroll.value.toFloat()
                }
            }
        }
        LaunchedEffect(session.preview, geometry) { motion.select(session.preview, geometry, density.density) }
        // Keep the selected label centred in view when the row scrolls.
        val firstScroll = remember { booleanArrayOf(true) }
        LaunchedEffect(safeSelected, geometry, actualWidth, scrolls) {
            if (!scrolls) {
                if (scroll.value != 0) scroll.scrollTo(0)
                return@LaunchedEffect
            }
            val center = geometry.center(safeSelected)
            val logical = if (rtl) contentWidth - center else center
            val target = (logical - actualWidth / 2f).roundToInt().coerceIn(0, (contentWidth - actualWidth).coerceAtLeast(0))
            if (firstScroll[0]) {
                firstScroll[0] = false
                scroll.scrollTo(target)
            } else {
                scroll.animateScrollTo(target)
            }
        }
        var bounds by remember { mutableStateOf(IntSize.Zero) }

        val surface = if (surfaceSupported && effectiveBackdrop != null && renderer != null && contentLayer != null) {
            floatingGlassSurface(
                backdrop = effectiveBackdrop,
                renderer = renderer,
                dark = dark,
                contentLayer = contentLayer,
                contour = contour,
                edgePaint = edgePaint,
                indicatorShape = indicatorShape,
                cornerRadius = cornerRadius,
                indicatorDrawing = indicatorDrawing,
                indicatorColor = indicatorColor,
                geometry = geometry,
                motion = motion,
                touch = touch,
                press = press,
                indicatorPress = indicatorPress,
                touchMotionEnabled = touchMotionEnabled,
                bounds = bounds,
                light = light,
                contentOffsetX = contentOffsetX,
            )
        } else {
            Modifier
        }

        Box(
            Modifier.width(with(density) { actualWidth.toDp() }).fillMaxHeight()
                .onSizeChanged { bounds = it }.selectableGroup()
                .pointerInput(session) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.any { it.pressed && !it.previousPressed }) session.allowLight = true
                        }
                    }
                }.then(lightInput).then(surface),
        ) {
            val content = if (contentLayer != null) {
                Modifier.drawWithContent { contentLayer.record { this@drawWithContent.drawContent() } }
            } else {
                Modifier.drawWithCache {
                    val background = Path().apply {
                        addOutline(containerShape.createOutline(size, layoutDirection, this@drawWithCache))
                    }
                    onDrawWithContent {
                        clipPath(background) {
                            drawRect(containerColor)
                            drawSegmentIndicator(
                                motion.rect(geometry, indicatorPress.scale.value).translate(contentOffsetX(), 0f),
                                geometry.height,
                                cornerRadius,
                                indicatorShape,
                                indicatorDrawing,
                                null,
                                edgePaint,
                                dark,
                                indicatorColor,
                            )
                        }
                        drawContent()
                    }
                }
            }

            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    if (touchMotionEnabled) {
                        translationX = touch.translationX.value
                        translationY = touch.translationY.value
                        val scale = touch.scaleX.value + press.scale.value - 1f
                        scaleX = scale
                        scaleY = touch.scaleY.value + press.scale.value - 1f
                    }
                }.onGloballyPositioned { lightCoordinates = it }
                    .then(content).clip(containerShape),
            ) {
                Row(
                    Modifier.fillMaxSize()
                        .then(
                            if (scrolls) {
                                Modifier.horizontalScroll(scroll, overscrollEffect = null, enabled = enabled).fillMaxHeight()
                            } else {
                                Modifier
                            },
                        )
                        .padding(horizontal = with(density) { padding.toDp() })
                        .onGloballyPositioned { inputCoordinates = it }
                        .pointerInput(geometry, labels, enabled, dragSelectionEnabled, commitOnRelease, touchMotionEnabled) {
                            if (!enabled) return@pointerInput
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                val coordinates = inputCoordinates?.takeIf { it.isAttached } ?: return@awaitEachGesture
                                val start = coordinates.localToWindow(down.position)
                                if (touchMotionEnabled) {
                                    touch.begin(start, bounds)
                                    press.press(bounds)
                                }
                                session.begin(current.value)
                                val downPosition = down.position + Offset(padding.toFloat(), 0f)
                                val indicator = motion.rect(geometry, indicatorPress.scale.value).inflate(10f)
                                val dragging = dragSelectionEnabled &&
                                    (geometry.hit(downPosition.x) == current.value || indicator.contains(downPosition))
                                if (dragging) {
                                    motion.beginDrag()
                                    indicatorPress.press(IntSize(motion.indicatorWidth.roundToInt(), geometry.indicatorHeight.roundToInt()))
                                }
                                var pressedLabel = geometry.hit(downPosition.x)
                                pressedLabel?.let { labelPresses[it].press(IntSize(geometry.widths[it], geometry.indicatorHeight.roundToInt())) }
                                val switchDistance = CouiSegmentedControlDefaults.switchDistance.toPx().toInt().toFloat()
                                val hitInset = if (dragging) switchDistance else 0f
                                var rubberOrigin = down.position.x
                                var ownsDrag = false
                                var didDrag = false
                                var completed = false
                                val tracker = CouiVelocityTracker(down.uptimeMillis)
                                try {
                                    tracker.add(down, coordinates)
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        if (event.isCouiTouchCancelled() || event.changes.size != 1) break
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!coordinates.isAttached) break
                                        val windowPosition = coordinates.localToWindow(change.position)
                                        val delta = windowPosition - start
                                        if (change.isConsumed) break
                                        if (!ownsDrag) {
                                            if (abs(delta.y) > viewConfiguration.touchSlop && abs(delta.y) > abs(delta.x)) break
                                            ownsDrag = dragging && change.pressed && abs(delta.x) > viewConfiguration.touchSlop
                                            if (ownsDrag) didDrag = true
                                        }
                                        if (ownsDrag) {
                                            change.consume()
                                        } else {
                                            val pass = if (change.pressed) PointerEventPass.Final else PointerEventPass.Main
                                            if (awaitPointerEvent(pass).changes.any { it.id == down.id && it.isConsumed }) break
                                        }
                                        tracker.add(change, coordinates)
                                        if (change.pressed && touchMotionEnabled) touch.move(windowPosition)
                                        val target = geometry.hit(change.position.x + padding, hitInset)
                                        if (dragging && change.pressed) {
                                            if (target != null && target != session.preview) {
                                                didDrag = true
                                                session.select(target, commitOnRelease) { submit.value(it) }
                                                haptic.value(CouiHapticEffect.Switch)
                                            } else if (!motion.moving) {
                                                if (motion.resetRubberOrigin) {
                                                    rubberOrigin = change.position.x
                                                    motion.resetRubberOrigin = false
                                                }
                                                motion.dragRubber(
                                                    change.position.x - rubberOrigin,
                                                    indicatorPress.scale.value,
                                                    session.preview == 0 || session.preview == labels.lastIndex,
                                                    switchDistance,
                                                )
                                            }
                                        }
                                        if (target != pressedLabel && (!dragging || target != null)) {
                                            pressedLabel?.let { labelPresses[it].release() }
                                            if (dragging) {
                                                target?.let {
                                                    labelPresses[it].press(IntSize(geometry.widths[it], geometry.indicatorHeight.roundToInt()))
                                                }
                                            }
                                            pressedLabel = target
                                        }
                                        if (!change.pressed) {
                                            val wasSelected = current.value
                                            if (target != null) {
                                                session.select(target, commitOnRelease) { submit.value(it) }
                                            }
                                            completed = target != null
                                            if (touchMotionEnabled) touch.release(tracker.releaseVelocity())
                                            change.consume()
                                            if (target != null && !didDrag) {
                                                if (target == wasSelected) {
                                                    reselect.value?.invoke(target)
                                                }
                                                tap.value?.invoke(target)
                                            }
                                            break
                                        }
                                    }
                                } finally {
                                    tracker.recycle()
                                    if (!completed) {
                                        session.allowLight = false
                                        if (touchMotionEnabled) touch.release(Offset.Zero)
                                    }
                                    if (touchMotionEnabled) press.release()
                                    session.finish(!completed, current.value, commitOnRelease) { submit.value(it) }
                                    motion.release()
                                    indicatorPress.release()
                                    labelPresses.forEach { it.release() }
                                }
                            }
                        },
                ) {
                    labels.forEachIndexed { index, label ->
                        val itemInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
                        val isActive = session.preview == index
                        val targetTextColor = if (isActive) selectedLabelColor else unselectedLabelColor
                        val animatedTextColor by animateColorAsState(
                            targetValue = if (enabled) targetTextColor else targetTextColor.copy(alpha = targetTextColor.alpha * .38f),
                            animationSpec = tween(durationMillis = 300, easing = TextColorEasing),
                            label = "segmentedTextColor",
                        )
                        Box(
                            Modifier.width(with(density) { geometry.widths[index].toDp() }).fillMaxHeight()
                                .then(tabModifier?.invoke(index) ?: Modifier)
                                .graphicsLayer {
                                    scaleX = labelPresses[index].scale.value
                                    scaleY = labelPresses[index].scale.value
                                }
                                .clip(indicatorShape)
                                .couiButtonFeedback(dark, itemInteractionSource, enabled, pressFeedback = false)
                                .semantics(mergeDescendants = true) {
                                    role = Role.Tab
                                    selected = safeSelected == index
                                    if (!enabled) {
                                        disabled()
                                    } else {
                                        onClick {
                                            if (index != current.value) submit.value(index) else reselect.value?.invoke(index)
                                            tap.value?.invoke(index)
                                            true
                                        }
                                    }
                                }
                                .onKeyEvent {
                                    if (enabled && it.type == KeyEventType.KeyUp &&
                                        (it.key == Key.Enter || it.key == Key.Spacebar || it.key == Key.DirectionCenter)
                                    ) {
                                        if (index != current.value) submit.value(index) else reselect.value?.invoke(index)
                                        tap.value?.invoke(index)
                                        true
                                    } else {
                                        false
                                    }
                                }
                                .focusable(enabled, itemInteractionSource)
                                .then(if (tabContent == null) Modifier.padding(horizontal = segmentPadding) else Modifier),
                            contentAlignment = contentAlignment,
                        ) {
                            if (tabContent != null) {
                                tabContent(index, label, isActive)
                            } else if (preferFastAnimatingFloatingSurfaces) {
                                val rasterCache = remember(label, whiteLabelStyle, measurer, density, layoutDirection) {
                                    SegmentLabelRasterCache()
                                }
                                Layout(
                                    modifier = Modifier
                                        .semantics { text = AnnotatedString(label) }
                                        .drawWithContent {
                                            val fallbackMaxW = size.width.roundToInt().coerceAtLeast(1)
                                            val bitmap = rasterCache.bitmap ?: rasterCache.getOrRasterize(
                                                maxWidth = fallbackMaxW,
                                                label = label,
                                                style = whiteLabelStyle,
                                                textMeasurer = measurer,
                                                density = density,
                                                layoutDirection = layoutDirection,
                                            ) ?: return@drawWithContent
                                            val x = ((size.width - bitmap.width) * 0.5f).roundToInt()
                                            val y = ((size.height - bitmap.height) * 0.5f).roundToInt()
                                            drawImage(
                                                image = bitmap,
                                                dstOffset = IntOffset(x, y),
                                                dstSize = IntSize(bitmap.width, bitmap.height),
                                                colorFilter = ColorFilter.tint(animatedTextColor),
                                            )
                                        },
                                ) { _, constraints ->
                                    val maxW = if (constraints.hasBoundedWidth) {
                                        constraints.maxWidth.coerceAtLeast(1)
                                    } else {
                                        Constraints.Infinity
                                    }
                                    val bitmap = rasterCache.getOrRasterize(
                                        maxWidth = maxW,
                                        label = label,
                                        style = whiteLabelStyle,
                                        textMeasurer = measurer,
                                        density = density,
                                        layoutDirection = layoutDirection,
                                    )
                                    val w = (bitmap?.width ?: 0).coerceIn(constraints.minWidth, constraints.maxWidth)
                                    val h = (bitmap?.height ?: 0).coerceIn(constraints.minHeight, constraints.maxHeight)
                                    layout(w, h) {}
                                }
                            } else {
                                BasicText(
                                    label,
                                    style = adjustedTextStyle.copy(
                                        textAlign = TextAlign.Center,
                                        color = animatedTextColor,
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun floatingGlassSurface(
    backdrop: CouiBackdrop,
    renderer: CouiFloatingRenderer,
    dark: Boolean,
    contentLayer: GraphicsLayer,
    contour: CouiContour?,
    edgePaint: Paint,
    indicatorShape: Shape,
    cornerRadius: Dp,
    indicatorDrawing: SegmentIndicatorDrawing,
    indicatorColor: Color,
    geometry: SegmentGeometry,
    motion: CouiSegmentMotion,
    touch: CouiTouchMotion,
    press: CouiPressMotion,
    indicatorPress: CouiPressMotion,
    touchMotionEnabled: Boolean,
    bounds: IntSize,
    light: (DrawScope.() -> Unit)?,
    contentOffsetX: () -> Float,
): Modifier {
    val contourEnabled = contour != null && LocalCouiContourGlowEnabled.current
    val material = remember(dark) { CouiSegmentedControlDefaults.material(dark) }
    val shadow = remember { CouiFloatingShadow(0f, Color.Transparent, 0f, 0f, 0f, Color.Transparent, Rect.Zero, 0f) }
    return Modifier.couiFloatingSurface(
        backdrop = backdrop,
        renderer = renderer,
        material = material,
        shadow = shadow,
        shape = CouiCapsuleShape,
        contentLayer = contentLayer,
        contourGlowEnabled = contourEnabled,
        foregroundComposition = true,
        beforeContent = { clip ->
            val tx = if (touchMotionEnabled) touch.translationX.value else 0f
            val ty = if (touchMotionEnabled) touch.translationY.value else 0f
            val sx = if (touchMotionEnabled) touch.scaleX.value + press.scale.value - 1f else 1f
            val sy = if (touchMotionEnabled) touch.scaleY.value + press.scale.value - 1f else 1f
            withTransform({
                translate(tx, ty)
                scale(sx, sy, center)
            }) {
                clipPath(clip) {
                    drawSegmentIndicator(
                        motion.rect(geometry, indicatorPress.scale.value).translate(contentOffsetX(), 0f),
                        geometry.height,
                        cornerRadius,
                        indicatorShape,
                        indicatorDrawing,
                        contour,
                        edgePaint,
                        dark,
                        indicatorColor,
                    )
                    // Blends onto the rendered material directly without passing through the content layer.
                    light?.invoke(this)
                }
            }
        },
        useMenuLightEdge = false,
    ) {
        val tx = if (touchMotionEnabled) touch.translationX.value else 0f
        val ty = if (touchMotionEnabled) touch.translationY.value else 0f
        val sx = if (touchMotionEnabled) touch.scaleX.value + press.scale.value - 1f else 1f
        val sy = if (touchMotionEnabled) touch.scaleY.value + press.scale.value - 1f else 1f
        CouiFloatingSurface(
            Rect(Offset(tx, ty), Size(bounds.width.toFloat(), bounds.height.toFloat())),
            scaleX = sx,
            scaleY = sy,
            cornerWeight = 1f,
        )
    }
}

private val TextColorEasing = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)

private data class SegmentIndicatorShape(private val cornerRadius: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val radiusPx = with(density) { cornerRadius.toPx() }
        return if (radiusPx >= size.minDimension * 0.5f - 0.5f) {
            CouiCapsuleShape.createOutline(size, layoutDirection, density)
        } else {
            CouiShape(cornerRadius, CouiCornerWeight).createOutline(size, layoutDirection, density)
        }
    }
}

/** Caches path, shadow path, and radial gradient per indicator size; translation alone does not rebuild them. */
private class SegmentIndicatorDrawing(
    val shadowColor: Color,
    private val shadowEndColor: Color,
) {
    private var size = Size.Unspecified
    private var shadowSize = Size.Unspecified
    private var shadowDy = Float.NaN
    private var lastShape: Shape? = null
    var path = Path()
        private set
    var shadowPath = Path()
        private set
    var shadow: Brush? = null
        private set

    fun update(next: Size, nextShadow: Size, nextShadowDy: Float, shape: Shape, scope: DrawScope) {
        val shapeChanged = lastShape != shape
        lastShape = shape
        if (size != next || shapeChanged) {
            size = next
            path = Path().apply { addOutline(shape.createOutline(next, scope.layoutDirection, scope)) }
            shadow = Brush.radialGradient(
                listOf(shadowColor, shadowEndColor),
                Offset(next.width / 2f, next.height / 2f),
                (next.width / 2f).coerceAtLeast(1f),
            )
        }
        if (shadowSize != nextShadow || shadowDy != nextShadowDy || shapeChanged) {
            shadowSize = nextShadow
            shadowDy = nextShadowDy
            shadowPath = if (nextShadow.width > 0f && nextShadow.height > 0f) {
                Path().apply {
                    addOutline(shape.createOutline(nextShadow, scope.layoutDirection, scope))
                    translate(Offset(0f, nextShadowDy))
                }
            } else {
                Path()
            }
        }
    }
}

/** Draws the indicator radial drop shadow and fill, plus the optional runtime-shader contour rim. */
private fun DrawScope.drawSegmentIndicator(
    rect: Rect,
    containerHeight: Float,
    cornerRadius: Dp,
    shape: Shape,
    drawing: SegmentIndicatorDrawing,
    contour: CouiContour?,
    paint: Paint,
    dark: Boolean,
    color: Color,
) {
    if (rect.width <= 0f || rect.height <= 0f) return
    val shadowDy = 2.dp.toPx()
    val shadowSize = Size(rect.width, (containerHeight - rect.top - shadowDy).coerceAtLeast(0f))
    drawing.update(rect.size, shadowSize, shadowDy, shape, this)
    translate(rect.left, rect.top) {
        if (shadowSize.width > 0f && shadowSize.height > 0f) {
            clipPath(drawing.path, ClipOp.Difference) {
                if (dark) {
                    drawPath(drawing.shadowPath, drawing.shadowColor)
                } else {
                    drawPath(drawing.shadowPath, checkNotNull(drawing.shadow))
                }
            }
        }
        clipPath(drawing.path) {
            drawRect(color, size = rect.size)
            if (isKitRuntimeShaderSupported() && contour != null) {
                drawIndicatorContour(rect.size, cornerRadius, contour, paint)
            }
        }
    }
}

private fun DrawScope.drawIndicatorContour(
    rectSize: Size,
    cornerRadius: Dp,
    contour: CouiContour,
    paint: Paint,
) {
    val halfMin = rectSize.minDimension * 0.5f
    val isCapsule = !cornerRadius.isSpecified || cornerRadius.toPx() >= halfMin - 0.5f
    val resolvedRadius = if (isCapsule) halfMin else cornerRadius.toPx().coerceAtLeast(0f)
    val cornerWeight = if (isCapsule) 1f else CouiCornerWeight
    paint.shader = contour.shader(
        rectSize,
        density,
        resolvedRadius,
        cornerWeight,
        Color.Transparent,
        0f,
        .15f,
        1f,
        1.8f * density,
    )
    drawIntoCanvas { canvas -> canvas.drawRect(0f, 0f, rectSize.width, rectSize.height, paint) }
}

/**
 * Pre-rasterizes a segment label in white into a tiny CPU-backed [ImageBitmap] once per
 * `(label, style, maxWidth)` so Skiko/Metal draws it via [ColorFilter.tint] instead of live
 * `Paragraph.updateForegroundPaint` inside an offscreen `GraphicsLayer`, preventing glyph-atlas
 * corruption and disappearing/truncated tab labels after video playback on iOS.
 */
private class SegmentLabelRasterCache {
    var maxWidth: Int = -2
    var bitmap: ImageBitmap? = null

    fun getOrRasterize(
        maxWidth: Int,
        label: String,
        style: TextStyle,
        textMeasurer: TextMeasurer,
        density: Density,
        layoutDirection: LayoutDirection,
    ): ImageBitmap? {
        if (this.maxWidth == maxWidth) return bitmap
        this.maxWidth = maxWidth
        if (label.isEmpty()) {
            bitmap = null
            return null
        }
        val padX = 2
        val measured = textMeasurer.measure(
            text = label,
            style = style,
            overflow = TextOverflow.Ellipsis,
            softWrap = true,
            maxLines = 1,
            constraints = Constraints(maxWidth = maxWidth),
        )
        val w = (measured.size.width + padX * 2).coerceAtLeast(1)
        val h = measured.size.height.coerceAtLeast(1)
        val image = ImageBitmap(w, h, ImageBitmapConfig.Argb8888)
        val canvas = Canvas(image)
        CanvasDrawScope().draw(
            density = density,
            layoutDirection = layoutDirection,
            canvas = canvas,
            size = Size(w.toFloat(), h.toFloat()),
        ) {
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(padX.toFloat(), 0f),
            )
        }
        image.prepareToDraw()
        bitmap = image
        return image
    }
}
