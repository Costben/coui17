// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * The press spotlight of a `COUIButton`, the second layer of the native button stack.
 *
 * `COUIButton.isSpotlightEffectEnabled()` clips a `COUISpotLightEffectDrawable` to the button's
 * path, and the light follows the finger while the button is held: `COUISpotLightEffect.animate`
 * springs the intensity to the preset's `endIntensity` and the radius from `startRadius` to
 * `endRadius`, `move` follows the touch on the move spring, and `extinguish` drops both back on the
 * release spring, overshooting the radius to `extraRadius` on the way out. This modifier reproduces
 * that driver with [SpotLightDefaults]' springs and paints the result through [spotLight].
 *
 * Only the light is this modifier's business: the capsule fill, the rim and the press scale stay
 * with the button, and the caller places the returned chain after the fill and before the label,
 * which is where `COUIButton.draw` puts the drawable.
 *
 * @param params The preset of the light, normally `CAPSULE_PARAMS_1` — the default of
 *   `COUISpotLightEffect` — resolved for the current night mode by [SpotLightDefaults.capsuleParams1].
 * @param shape The silhouette the light is clipped to, i.e. the button's own capsule.
 * @param enabled Whether the light is driven and painted at all.
 */
@Composable
internal fun Modifier.pressSpotLight(
    params: SpotLightParams,
    shape: Shape,
    enabled: Boolean = true,
): Modifier {
    val density = LocalDensity.current
    val widthPx = remember { mutableFloatStateOf(0f) }
    val heightPx = remember { mutableFloatStateOf(0f) }
    // `COUISpotLightEffect` starts the light at the centre of its bounds.
    val lightX = remember { Animatable(0.5f) }
    val lightY = remember { Animatable(0.5f) }
    val progress = remember { Animatable(0f) }
    var extinguishing by remember { mutableStateOf(false) }

    val radius = with(density) {
        couiSpotlightRadius(
            start = params.startRadius,
            end = params.endRadius,
            extinguishExtra = SpotLightDefaults.ExtinguishExtraRadius,
            progress = progress.value,
            extinguishing = extinguishing,
        ).toDp()
    }
    val lightHeight = with(density) { params.lightHeight.toDp() }

    return this
        .onSizeChanged {
            widthPx.floatValue = it.width.toFloat()
            heightPx.floatValue = it.height.toFloat()
        }
        // Initial pass: the button's own clickable consumes the same events later in the chain, so
        // the light has to read them before that without consuming anything itself.
        .pointerInput(params, enabled) {
            if (!enabled) return@pointerInput
            coroutineScope {
                val scope = this
                var moveJob: Job? = null
                fun moveTo(position: Offset) {
                    val x = position.x / widthPx.floatValue.coerceAtLeast(1f)
                    val y = position.y / heightPx.floatValue.coerceAtLeast(1f)
                    moveJob?.cancel()
                    if (progress.value == 0f) {
                        // The light starts exactly under the finger, like `COUISpotLightEffect.glow`.
                        // Undispatched so the snap lands before a following move can cancel it.
                        moveJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                            lightX.snapTo(x)
                            lightY.snapTo(y)
                        }
                    } else {
                        moveJob = scope.launch {
                            coroutineScope {
                                launch { lightX.animateTo(x, SpotLightDefaults.MoveSpring) }
                                launch { lightY.animateTo(y, SpotLightDefaults.MoveSpring) }
                            }
                        }
                    }
                }
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        moveTo(down.position)
                        extinguishing = false
                        launch { progress.animateTo(1f, SpotLightDefaults.GlowSpring) }
                        val pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            moveTo(change.position)
                            if (change.changedToUpIgnoreConsumed()) break
                        }
                        extinguishing = true
                        launch { progress.animateTo(0f, SpotLightDefaults.ExtinguishSpring) }
                    }
                }
            }
        }
        .spotLight(
            position = Offset(lightX.value, lightY.value),
            radius = radius,
            intensity = params.endIntensity * progress.value,
            lightHeight = lightHeight,
            ambientStrength = params.ambientStrength,
            diffuseStrength = params.diffuseStrength,
            noise = params.noise,
            dither = params.ditherN,
            blendMode = params.blendMode,
            // Nothing is painted at rest, so an idle button costs no shader work.
            enabled = enabled && progress.value > 0f,
            clipShape = shape,
        )
}
