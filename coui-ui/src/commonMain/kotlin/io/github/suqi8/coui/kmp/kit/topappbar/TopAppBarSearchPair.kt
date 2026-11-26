// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.topappbar

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.kit.effect.CouiFloatingRenderer
import io.github.suqi8.coui.kmp.kit.effect.CouiFloatingSurface
import io.github.suqi8.coui.kmp.kit.effect.SpotlightPreset
import io.github.suqi8.coui.kmp.kit.effect.couiFloatingPair
import io.github.suqi8.coui.kmp.kit.effect.createBlurRenderEffect
import io.github.suqi8.coui.kmp.kit.effect.isKitRenderEffectSupported
import io.github.suqi8.coui.kmp.kit.effect.preferFastAnimatingFloatingSurfaces
import io.github.suqi8.coui.kmp.kit.effect.rememberCouiSpotlight
import io.github.suqi8.coui.kmp.kit.input.couiMovingActionClick
import io.github.suqi8.coui.kmp.kit.motion.couiPressMotion
import io.github.suqi8.coui.kmp.kit.motion.couiTouchMotion
import io.github.suqi8.coui.kmp.kit.motion.rememberCouiPressMotion
import io.github.suqi8.coui.kmp.kit.motion.rememberCouiTouchMotion
import io.github.suqi8.coui.kmp.kit.theme.ProvideCouiContent
import kotlin.math.min
import kotlin.math.roundToInt

private enum class SearchPairSlot { Input, Action }

/** Dual-body metaball material shared between the search field and action; slots supply content while clicks, geometry, and feedback stay here. */
@Composable
internal fun TopAppBarSearchPair(
    modifier: Modifier = Modifier,
    renderer: CouiFloatingRenderer = remember { CouiFloatingRenderer.pair() },
    fullWidth: Float,
    progressProvider: () -> Float,
    enabled: Boolean,
    active: Boolean,
    deform: Boolean,
    animating: Boolean,
    capsule: Boolean,
    contentRenderEffect: RenderEffect?,
    action: @Composable () -> Unit,
    onActionClick: (() -> Unit)?,
    input: @Composable (Float) -> Unit,
) {
    val backdrop = checkNotNull(LocalCouiToolbarBackdrop.current)
    val style = LocalCouiToolbarStyle.current
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val inputLayer = rememberGraphicsLayer()
    val actionLayer = rememberGraphicsLayer()
    SideEffect { inputLayer.renderEffect = contentRenderEffect }
    val shape = style.capsuleShape
    val inputTouch = rememberCouiTouchMotion(deform)
    val inputPress = rememberCouiPressMotion()
    val actionEnabled = enabled && active && onActionClick != null
    val actionTouch = rememberCouiTouchMotion(deform && onActionClick != null)
    val actionPress = rememberCouiPressMotion()
    val actionSource = remember { MutableInteractionSource() }
    val (inputLightInput, inputLight) = rememberCouiSpotlight(enabled && active, SpotlightPreset.OpaqueMedium2, style.dark)
    val (actionLightInput, actionLight) = rememberCouiSpotlight(actionEnabled, SpotlightPreset.OpaqueMedium2, style.dark)
    // Non-snapshot holder avoids mutating Compose state inside SubcomposeLayout's measure pass
    // (which previously triggered a redundant second layout/draw pass on every animation frame).
    val geometryRef = remember { arrayOfNulls<CouiTopAppBarSearchPairGeometry>(1) }
    val actionBlurCache = remember { HashMap<Float, RenderEffect>(10) }
    val actionBlur = if (preferFastAnimatingFloatingSurfaces) {
        null
    } else {
        val fraction = progressProvider().coerceIn(0f, 1f)
        val rawActionBlur = (1f - fraction) * 30f
        val actionBlurRadius = if (rawActionBlur < 2f) 0f else (rawActionBlur / 4f).roundToInt() * 4f
        remember(actionBlurRadius) {
            if (actionBlurRadius > 0f && isKitRenderEffectSupported()) {
                actionBlurCache.getOrPut(actionBlurRadius) {
                    createBlurRenderEffect(actionBlurRadius, actionBlurRadius, TileMode.Decal)
                }
            } else {
                null
            }
        }
    }
    fun inputSurface(): CouiFloatingSurface {
        val frame = geometryRef[0] ?: return CouiFloatingSurface(androidx.compose.ui.geometry.Rect.Zero, visible = false)
        val bounds = frame.inputBounds.translate(Offset(inputTouch.translationX.value, inputTouch.translationY.value))
        return CouiFloatingSurface(
            bounds,
            scaleX = inputTouch.scaleX.value + inputPress.scale.value - 1f,
            scaleY = inputTouch.scaleY.value + inputPress.scale.value - 1f,
            cornerWeight = 1f,
        )
    }
    fun actionSurface(): CouiFloatingSurface {
        val frame = geometryRef[0] ?: return CouiFloatingSurface(androidx.compose.ui.geometry.Rect.Zero, visible = false)
        return CouiFloatingSurface(
            frame.actionBounds.translate(Offset(actionTouch.translationX.value, actionTouch.translationY.value)),
            scaleX = frame.actionScale * (actionTouch.scaleX.value + actionPress.scale.value - 1f),
            scaleY = frame.actionScale * (actionTouch.scaleY.value + actionPress.scale.value - 1f),
            cornerWeight = 1f,
            alpha = frame.actionAlpha,
            visible = frame.actionAlpha > 0f,
        )
    }
    val extension = with(density) { 4.dp.roundToPx() }
    val materials = remember(style, capsule, extension) {
        listOf(
            style.material(capsule).copy(index = 1, extension = extension),
            style.material(true).copy(index = 2, extension = extension),
        )
    }
    val shadows = remember(style) {
        val s = style.shadow(true)
        listOf(s, s)
    }
    val shapes = remember(shape) { listOf(shape, shape) }
    val layers = remember(inputLayer, actionLayer) { listOf(inputLayer, actionLayer) }
    val drawing = Modifier.couiFloatingPair(
        backdrop,
        renderer,
        materials,
        shadows,
        shapes,
        layers,
        style.contourGlowEnabled,
        drawOrder = listOf(1, 0),
        foregroundComposition = true,
        animating = animating,
        beforeContent = { index, path ->
            val surface = if (index == 0) inputSurface() else actionSurface()
            val light = if (index == 0) inputLight else actionLight
            if (light != null && surface.visible) {
                inset(
                    surface.bounds.left,
                    surface.bounds.top,
                    size.width - surface.bounds.right,
                    size.height - surface.bounds.bottom,
                ) {
                    withTransform({ scale(surface.scaleX, surface.scaleY, center) }) { clipPath(path) { light() } }
                }
            }
        },
        surfaces = { listOf(inputSurface(), actionSurface()) },
    )
    val finalInputWidthState = remember { mutableFloatStateOf(fullWidth) }
    val actionSubcomposeContent = remember(
        style,
        enabled,
        active,
        onActionClick,
        deform,
        actionEnabled,
        animating,
        shape,
        actionBlur,
        action,
        progressProvider,
        actionLayer,
    ) {
        @Composable {
            ProvideCouiContent(
                color = style.contentColor.copy(alpha = style.contentColor.alpha * if (enabled && onActionClick != null) 1f else .38f),
                textStyle = style.textStyle,
            ) {
                Box(
                    Modifier.height(40.dp)
                        .widthIn(min = CouiTopAppBarDefaults.searchActionMinWidth, max = CouiTopAppBarDefaults.searchActionMaxWidth)
                        .graphicsLayer {
                            // Semantics evaluation at attach time runs before first measurement; keep unmeasured nodes invisible and non-hittable.
                            val frame = geometryRef[0]
                            if (frame == null) {
                                alpha = 0f
                            } else {
                                val surface = actionSurface()
                                translationX = (surface.bounds.left - frame.actionBounds.left).roundToInt().toFloat()
                                translationY = surface.bounds.top
                                scaleX = surface.scaleX
                                scaleY = surface.scaleY
                                alpha = surface.alpha
                            }
                        }.couiTouchMotion(actionTouch, deform && onActionClick != null)
                        .couiPressMotion(actionPress, actionEnabled).then(actionLightInput)
                        .drawWithContent { actionLayer.record { this@drawWithContent.drawContent() } }
                        .clip(shape)
                        .then(if (!active) Modifier.clearAndSetSemantics {} else Modifier)
                        .couiMovingActionClick(actionSource, actionEnabled, animating) { onActionClick?.invoke() }
                        .padding(horizontal = CouiTopAppBarDefaults.searchActionPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.graphicsLayer {
                            alpha = progressProvider().coerceIn(0f, 1f)
                            renderEffect = actionBlur
                        },
                    ) { action() }
                }
            }
        }
    }
    val inputSubcomposeContent = remember(
        style,
        enabled,
        active,
        deform,
        shape,
        inputLayer,
        input,
    ) {
        @Composable {
            ProvideCouiContent(
                color = style.contentColor.copy(alpha = style.contentColor.alpha * if (enabled) 1f else .38f),
                textStyle = style.textStyle,
            ) {
                Box(
                    Modifier.graphicsLayer {
                        if (geometryRef[0] == null) {
                            alpha = 0f
                        } else {
                            val surface = inputSurface()
                            translationX = inputTouch.translationX.value
                            translationY = inputTouch.translationY.value
                            scaleX = surface.scaleX
                            scaleY = surface.scaleY
                            alpha = 1f
                        }
                    }.couiTouchMotion(inputTouch, deform).couiPressMotion(inputPress, enabled && active)
                        .then(inputLightInput)
                        .drawWithContent { inputLayer.record { this@drawWithContent.drawContent() } }
                        .clip(shape),
                ) { input(finalInputWidthState.floatValue) }
            }
        }
    }
    SubcomposeLayout(modifier.then(drawing)) { constraints ->
        val height = constraints.maxHeight
        val progress = progressProvider()
        // Measure custom action content first to determine final input width without a one-frame onSizeChanged mismatch.
        val actionWidthLimit = min(
            CouiTopAppBarDefaults.searchActionMaxWidth.roundToPx(),
            (fullWidth * .4f).roundToInt(),
        ).coerceAtLeast(1)
        val button = subcompose(SearchPairSlot.Action, actionSubcomposeContent)
            .single()
            .measure(Constraints(maxWidth = actionWidthLimit, minHeight = height, maxHeight = height))
        val gap = min(CouiTopAppBarDefaults.searchActionGap.toPx(), (fullWidth - button.width - 1f).coerceAtLeast(0f))
        val finalInputWidth = (fullWidth - button.width - gap).roundToInt().coerceAtLeast(1).toFloat()
        if (finalInputWidthState.floatValue != finalInputWidth) {
            finalInputWidthState.floatValue = finalInputWidth
        }
        val frame = topAppBarSearchPairGeometry(
            constraints.maxWidth.toFloat(),
            height.toFloat(),
            button.width.toFloat(),
            gap,
            progress,
            rtl,
        )
        geometryRef[0] = frame
        val body = subcompose(SearchPairSlot.Input, inputSubcomposeContent)
            .single()
            .measure(Constraints.fixed(frame.inputBounds.width.roundToInt(), height))
        layout(constraints.maxWidth, height) {
            // Draw order is managed by the dual-body renderer; revealed action receives touches first.
            if (frame.actionAlpha > 0f) button.place(frame.actionBounds.left.roundToInt(), 0, zIndex = 1f)
            body.place(frame.inputBounds.left.roundToInt(), 0)
        }
    }
}
