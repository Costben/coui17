// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Indication
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.interfaces.HoldDownInteraction
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// COUI: coui_color_hover #14000000 / #26FFFFFF, coui_color_press #1F000000 / #33FFFFFF.
private const val HOVER_ALPHA_LIGHT = 0x14 / 255f
private const val HOVER_ALPHA_DARK = 0x26 / 255f
private const val PRESS_ALPHA_LIGHT = 0x1F / 255f
private const val PRESS_ALPHA_DARK = 0x33 / 255f

// COUIStrokeDrawable strokes `default_focus_stroke_radius` (3dp) on both sides of the edge.
private val FocusStrokeWidth = 6.dp

// COUI: COUISpringForce response 0.3 / bounce 0 -> stiffness (2 * PI / 0.3)^2 = 438.65, damping ratio 1.
private val StateLayerSpring = spring<Float>(dampingRatio = 1f, stiffness = 438.65f)

/**
 * COUI default [Indication] that draws a state layer overlay when pressed or hovered, and the
 * focus ring `COUIStrokeDrawable` draws when focused.
 *
 * The dark theme variant is selected by the luminance of [color] — the content color
 * (e.g. `onBackground`), which is bright in dark theme.
 *
 * @param color The content color, which picks the light or dark state layer.
 * @param focusOutline The ring colour `couiColorFocusOutline`. No ring is drawn when it is
 *   [Color.Unspecified].
 * @param shape The outline the ring follows. `COUIStrokeDrawable` falls back to a stadium built
 *   from the bounds when the view sets no path or rect, so a capsule control and the default agree;
 *   a control with a different outline passes its own.
 */
@Immutable
class COUIIndication(
    private val color: Color = Color.Black,
    private val focusOutline: Color = Color.Unspecified,
    private val shape: Shape = RoundedCornerShape(percent = 50),
) : IndicationNodeFactory {
    /** The 1.1.0 form without a focus ring, kept for binary compatibility. */
    @Deprecated(
        message = "Migrated to the constructor that also takes the focus ring colour and shape.",
        level = DeprecationLevel.HIDDEN,
    )
    constructor(color: Color) : this(color = color, focusOutline = Color.Unspecified, shape = RoundedCornerShape(percent = 50))

    override fun create(interactionSource: InteractionSource): DelegatableNode = COUIIndicationInstance(
        interactionSource = interactionSource,
        isDarkTheme = color.luminance() > 0.5f,
        focusOutline = focusOutline,
        shape = shape,
    )

    override fun hashCode(): Int {
        var result = color.hashCode()
        result = 31 * result + focusOutline.hashCode()
        result = 31 * result + shape.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is COUIIndication) return false
        if (color != other.color) return false
        if (focusOutline != other.focusOutline) return false
        if (shape != other.shape) return false
        return true
    }

    private class COUIIndicationInstance(
        private val interactionSource: InteractionSource,
        isDarkTheme: Boolean,
        private val focusOutline: Color,
        private val shape: Shape,
    ) : Modifier.Node(),
        DrawModifierNode {
        private val overlayColor = if (isDarkTheme) Color.White else Color.Black
        private val hoverAlpha = if (isDarkTheme) HOVER_ALPHA_DARK else HOVER_ALPHA_LIGHT
        private val pressAlpha = if (isDarkTheme) PRESS_ALPHA_DARK else PRESS_ALPHA_LIGHT
        private var isPressed = false
        private var isHovered = false
        private var isFocused = false
        private var isHoldDown = false
        private val animatedAlpha = Animatable(0f)
        private val animatedFocusAlpha = Animatable(0f)
        private var pressedAnimation: Job? = null
        private var restingAnimation: Job? = null
        private var focusAnimation: Job? = null

        private fun updateStates() {
            var targetAlpha = 0.0f
            if (isHovered) targetAlpha += hoverAlpha
            // Hold-down keeps the press layer visible after the press is released.
            if (isPressed || isHoldDown) targetAlpha += pressAlpha

            if (targetAlpha == 0.0f) {
                restingAnimation?.cancel()
                restingAnimation =
                    coroutineScope.launch {
                        if (coroutineContext.isActive) {
                            // Cut the fade-in short instead of waiting for it: a quick tap would
                            // otherwise keep the layer at full strength long after the finger is
                            // up, and a picture taken of the page would freeze it there.
                            pressedAnimation?.cancel()
                            animatedAlpha.animateTo(
                                targetValue = targetAlpha,
                                animationSpec = StateLayerSpring,
                            )
                        }
                    }
            } else {
                pressedAnimation?.cancel()
                restingAnimation?.cancel()
                pressedAnimation =
                    coroutineScope.launch {
                        animatedAlpha.animateTo(
                            targetValue = targetAlpha,
                            animationSpec = StateLayerSpring,
                        )
                    }
            }
        }

        private fun updateFocus() {
            focusAnimation?.cancel()
            // StateEffectAnimator: the ring fades in and out on the same spring as the state layer.
            focusAnimation =
                coroutineScope.launch {
                    animatedFocusAlpha.animateTo(
                        targetValue = if (isFocused) 1f else 0f,
                        animationSpec = StateLayerSpring,
                    )
                }
        }

        override fun onAttach() {
            coroutineScope.launch {
                interactionSource.interactions.collect { interaction ->
                    val previousPressed = isPressed
                    val previousHovered = isHovered
                    val previousFocused = isFocused
                    val previousHoldDown = isHoldDown

                    when (interaction) {
                        is PressInteraction.Press -> isPressed = true
                        is PressInteraction.Release, is PressInteraction.Cancel -> isPressed = false
                        is HoverInteraction.Enter -> isHovered = true
                        is HoverInteraction.Exit -> isHovered = false
                        is FocusInteraction.Focus -> isFocused = true
                        is FocusInteraction.Unfocus -> isFocused = false
                        is HoldDownInteraction.HoldDown -> isHoldDown = true
                        is HoldDownInteraction.Release -> isHoldDown = false
                        else -> return@collect
                    }

                    if (previousPressed != isPressed ||
                        previousHovered != isHovered ||
                        previousHoldDown != isHoldDown
                    ) {
                        updateStates()
                    }
                    if (previousFocused != isFocused) {
                        updateFocus()
                    }
                }
            }
        }

        override fun ContentDrawScope.draw() {
            drawContent()
            val alpha = animatedAlpha.value
            if (alpha > 0f) {
                drawRect(color = overlayColor, alpha = alpha, size = size)
            }
            val ringAlpha = animatedFocusAlpha.value
            if (ringAlpha > 0f && focusOutline.isSpecified) {
                // Stroked on the edge itself: the inner half stays visible under a clip to the
                // component's own outline, which is exactly the band COUIStrokeDrawable.TYPE_INNER
                // shows.
                drawOutline(
                    outline = shape.createOutline(size, layoutDirection, this),
                    color = focusOutline,
                    alpha = ringAlpha,
                    style = Stroke(width = FocusStrokeWidth.toPx()),
                )
            }
        }
    }
}
