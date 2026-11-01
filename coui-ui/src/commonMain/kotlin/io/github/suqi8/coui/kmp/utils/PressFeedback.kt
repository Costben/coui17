// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import io.github.suqi8.coui.kmp.interfaces.HoldDownInteraction
import kotlinx.coroutines.launch

/**
 * The press half of `PressParams`'s default: bounce 0 (critically damped, no overshoot) and
 * response 0.15s, i.e. `stiffness = (2 * PI / 0.15)^2 = 1754.6`.
 */
private val CardPressSpec: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 1754.6f)

/**
 * The hand-up half of `PressParams`'s default: bounce 0.5 (zeta 0.5) and response 0.4s, i.e.
 * `stiffness = (2 * PI / 0.4)^2 = 246.74`, so a released card springs back with one overshoot.
 */
private val CardReleaseSpec: SpringSpec<Float> = spring(dampingRatio = 0.5f, stiffness = 246.74f)

/**
 * Sinks a card while it is pressed and springs it back on release, as `COUICardFeedbackHelper` does
 * with `CARD_SCALE_PRESS_DEFAULT` (0.98) and `PressParams`'s two spring sets.
 *
 * The press runs on [animationSpec]; the hand-up runs on [releaseAnimationSpec]. The release half sits
 * outside the primary constructor so the data-class constructor, [copy] and `componentN` keep the
 * shapes binary compatibility expects.
 */
@Stable
data class SinkFeedback(
    val sinkAmount: Float = 0.98f,
    val animationSpec: AnimationSpec<Float> = CardPressSpec,
) : IndicationNodeFactory {
    /** The spring of the hand-up; not part of [equals], [hashCode] or [copy]. */
    var releaseAnimationSpec: AnimationSpec<Float> = CardReleaseSpec

    /** Builds a sink feedback whose press and hand-up use different springs. */
    constructor(
        sinkAmount: Float,
        animationSpec: AnimationSpec<Float>,
        releaseAnimationSpec: AnimationSpec<Float>,
    ) : this(sinkAmount, animationSpec) {
        this.releaseAnimationSpec = releaseAnimationSpec
    }

    override fun create(interactionSource: InteractionSource): DelegatableNode = SinkFeedbackNode(interactionSource, sinkAmount, animationSpec, releaseAnimationSpec)

    private class SinkFeedbackNode(
        var interactionSource: InteractionSource,
        var sinkAmount: Float,
        var animationSpec: AnimationSpec<Float>,
        var releaseAnimationSpec: AnimationSpec<Float>,
    ) : Modifier.Node(),
        LayoutModifierNode {

        private val animatedScale = Animatable(1f)
        private var isPressed = false
        private var isHoldDown = false

        private fun updateState() {
            val pressed = isPressed || isHoldDown
            coroutineScope.launch {
                animatedScale.animateTo(
                    targetValue = if (pressed) sinkAmount else 1f,
                    animationSpec = if (pressed) animationSpec else releaseAnimationSpec,
                )
            }
        }

        override fun onAttach() {
            coroutineScope.launch {
                interactionSource.interactions.collect { interaction: Interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> isPressed = true
                        is PressInteraction.Release, is PressInteraction.Cancel -> isPressed = false
                        is HoldDownInteraction.HoldDown -> isHoldDown = true
                        is HoldDownInteraction.Release -> isHoldDown = false
                        else -> return@collect
                    }
                    updateState()
                }
            }
        }

        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints,
        ): MeasureResult {
            val placeable = measurable.measure(constraints)
            return layout(placeable.width, placeable.height) {
                placeable.placeWithLayer(0, 0) {
                    scaleX = animatedScale.value
                    scaleY = animatedScale.value
                }
            }
        }
    }
}

@Stable
data class TiltFeedback(
    val tiltAmount: Float = 8f,
    val animationSpec: AnimationSpec<Float> = spring(0.6f, 400f),
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = TiltFeedbackNode(interactionSource, tiltAmount, animationSpec)

    private class TiltFeedbackNode(
        var interactionSource: InteractionSource,
        var tiltAmount: Float,
        var animationSpec: AnimationSpec<Float>,
    ) : Modifier.Node(),
        LayoutModifierNode,
        PointerInputModifierNode {

        private var transformOrigin: TransformOrigin = TransformOrigin.Center
        private var targetX = 0f
        private var targetY = 0f
        private val animatedTiltX = Animatable(0f)
        private val animatedTiltY = Animatable(0f)
        private var isPressed = false
        private var isHoldDown = false

        private fun updateState() {
            if (isPressed || isHoldDown) {
                coroutineScope.launch { animatedTiltX.animateTo(targetX, animationSpec) }
                coroutineScope.launch { animatedTiltY.animateTo(targetY, animationSpec) }
            } else {
                coroutineScope.launch { animatedTiltX.animateTo(0f, animationSpec) }
                coroutineScope.launch { animatedTiltY.animateTo(0f, animationSpec) }
            }
        }

        override fun onAttach() {
            coroutineScope.launch {
                interactionSource.interactions.collect { interaction: Interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> isPressed = true
                        is PressInteraction.Release, is PressInteraction.Cancel -> isPressed = false
                        is HoldDownInteraction.HoldDown -> isHoldDown = true
                        is HoldDownInteraction.Release -> isHoldDown = false
                        else -> return@collect
                    }
                    updateState()
                }
            }
        }

        override fun onPointerEvent(
            pointerEvent: PointerEvent,
            pass: PointerEventPass,
            bounds: IntSize,
        ) {
            if (pass != PointerEventPass.Main) return
            if (pointerEvent.type == PointerEventType.Press) {
                val offset = pointerEvent.changes.first().position

                transformOrigin = TransformOrigin(
                    pivotFractionX = if (offset.x < bounds.width / 2f) 1f else 0f,
                    pivotFractionY = if (offset.y < bounds.height / 2f) 1f else 0f,
                )

                targetX = if (offset.y < bounds.height / 2f) tiltAmount else -tiltAmount
                targetY = if (offset.x < bounds.width / 2f) -tiltAmount else tiltAmount
            }
        }

        override fun onCancelPointerInput() {
            transformOrigin = TransformOrigin.Center
            targetX = 0f
            targetY = 0f
        }

        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints,
        ): MeasureResult {
            val placeable = measurable.measure(constraints)
            return layout(placeable.width, placeable.height) {
                placeable.placeWithLayer(0, 0) {
                    rotationX = animatedTiltX.value
                    rotationY = animatedTiltY.value
                    cameraDistance = 12 * density
                    this.transformOrigin = this@TiltFeedbackNode.transformOrigin
                }
            }
        }
    }
}

/**
 * The type of visual feedback to apply when the component is pressed.
 */
enum class PressFeedbackType {
    /** No feedback effect. */
    None,

    /** Animates the fill towards the pressed color while pressed (COUI: COUICardListSelectedItemLayout). */
    Tint,

    /** Sinks slightly when pressed. */
    Sink,

    /** Tilts based on touch position when pressed. */
    Tilt,
}
