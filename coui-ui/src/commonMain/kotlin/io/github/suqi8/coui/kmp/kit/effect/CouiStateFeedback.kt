// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.effect

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import io.github.suqi8.coui.kmp.kit.input.isCouiTouchCancelled
import io.github.suqi8.coui.kmp.kit.theme.CouiTheme

/** Navigation items filter out state_pressed; COUIMaskRippleDrawable only draws hover -> focus. */
@Composable
internal fun Modifier.couiNavigationFeedback(
    dark: Boolean,
    interactionSource: MutableInteractionSource,
): Modifier {
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val hover = rememberMaskProgress(hovered)
    val focus = rememberMaskProgress(focused)
    val inset = with(LocalDensity.current) { 8.dp.toPx() }
    return hoverable(interactionSource).drawBehind {
        val radius = size.height * 0.5f
        val maskSize = Size(size.width + inset * 2f, size.height)
        drawRoundRect(maskColor(hover.value, Color(if (dark) 0x26FFFFFF else 0x14000000)), Offset(-inset, 0f), maskSize, CornerRadius(radius))
        drawRoundRect(maskColor(focus.value, Color(if (dark) 0x331A8CFF else 0x1F0080FF)), Offset(-inset, 0f), maskSize, CornerRadius(radius))
    }
}

/** COUIFloatingButton disables the press mask when material blur is active while keeping hover; press scale is handled separately. */
@Composable
internal fun Modifier.couiFabFeedback(dark: Boolean, interactionSource: MutableInteractionSource): Modifier {
    val hovered by interactionSource.collectIsHoveredAsState()
    val hover = rememberMaskProgress(hovered)
    return hoverable(interactionSource).drawBehind {
        val radius = CornerRadius(size.minDimension * 0.5f)
        drawRoundRect(maskColor(hover.value, Color(if (dark) 0x26FFFFFF else 0x14000000)), cornerRadius = radius)
    }
}

/** Vendor toolbar material filters pressed notifications and keeps only hover/focus masks; press scale is handled separately. */
@Composable
internal fun Modifier.couiToolbarFeedback(
    dark: Boolean,
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
): Modifier {
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val hover = rememberMaskProgress(enabled && hovered)
    val focus = rememberMaskProgress(enabled && focused)
    return hoverable(interactionSource, enabled).drawBehind {
        val radius = CornerRadius(size.minDimension * 0.5f)
        drawRoundRect(maskColor(hover.value, Color(if (dark) 0x26FFFFFF else 0x14000000)), cornerRadius = radius)
        drawRoundRect(maskColor(focus.value, Color(if (dark) 0x331A8CFF else 0x1F0080FF)), cornerRadius = radius)
    }
}

@Composable
private fun rememberMaskProgress(active: Boolean): State<Float> {
    val effect = remember { CouiStateProgress() }
    SideEffect { effect.setActive(active) }
    DisposableEffect(effect) { onDispose { effect.dispose() } }
    return effect.progress
}

/**
 * Hover, focus, and press mask for regular buttons or list items; does not trigger clicks or business actions.
 * [list] selects the vendor list minimum press progress and cancellation rules; [hoverLocked] keeps row hover during Popup expansion.
 * [pressFeedback] controls both regular and list press masks; disabling it releases any active press animation without affecting hover/focus.
 * The host shares the same [interactionSource] with the sole clickable/toggleable owner and handles outer clipping.
 */
@Composable
internal fun Modifier.couiButtonFeedback(
    dark: Boolean = CouiTheme.isDark,
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    pressFeedback: Boolean = true,
    list: Boolean = false,
    hoverLocked: Boolean = false,
): Modifier {
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val hover = rememberMaskProgress(enabled && (hovered || hoverLocked))
    val focus = rememberMaskProgress(enabled && focused)
    val listPress = if (list && pressFeedback) remember { CouiListPressMask() } else null
    if (listPress != null) {
        SideEffect { listPress.updateState(enabled, pressed) }
        DisposableEffect(listPress) {
            onDispose { listPress.dispose() }
        }
        ObserveWindowFocusLost(listPress) { listPress.release() }
    }
    val press = if (listPress == null) rememberMaskProgress(enabled && pressed && pressFeedback) else null
    val observer = if (listPress != null && enabled) {
        Modifier.pointerInput(listPress) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                listPress.touch(true)
                try {
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val consumedMove = change.pressed && change.isConsumed && change.position != change.previousPosition
                        if (event.isCouiTouchCancelled() || consumedMove || !size.toSize().toRect().contains(change.position)) break
                        if (!change.pressed) break
                    } while (true)
                } finally {
                    listPress.touch(false)
                }
            }
        }
    } else {
        Modifier
    }
    return hoverable(interactionSource, enabled).then(observer).drawBehind {
        drawRect(maskColor(hover.value, Color(if (dark) 0x26FFFFFF else 0x14000000)))
        drawRect(maskColor(focus.value, Color(if (dark) 0x331A8CFF else 0x1F0080FF)))
        drawRect(maskColor(listPress?.progress ?: checkNotNull(press).value, Color(if (dark) 0x33FFFFFF else 0x1F000000)))
    }
}

/** Hover and press mask for COUISwitch; touch session and click recognition cancellation settle separately. */
@Composable
internal fun Modifier.couiSwitchFeedback(
    dark: Boolean,
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
    touchPressed: Boolean,
): Modifier {
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val hover = rememberMaskProgress(enabled && hovered)
    val press = rememberMaskProgress(enabled && (pressed || touchPressed))
    return hoverable(interactionSource, enabled).drawBehind {
        if (enabled) {
            drawRect(maskColor(hover.value, Color(if (dark) 0x26FFFFFF else 0x14000000)))
            drawRect(maskColor(press.value, Color(if (dark) 0x33FFFFFF else 0x1F000000)))
        }
    }
}

/** TYPE_OUTER of COUIStrokeDrawable: strokes double width along the item shape, then clips out the track interior. */
@Composable
internal fun Modifier.couiSwitchFocusOutline(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
    color: Color,
    shape: Shape,
): Modifier {
    val focused by interactionSource.collectIsFocusedAsState()
    val focus = rememberMaskProgress(enabled && focused)
    return drawWithCache {
        val path = when (val outline = shape.createOutline(size, layoutDirection, this)) {
            is Outline.Generic -> outline.path
            is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
            is Outline.Rectangle -> Path().apply { addRect(outline.rect) }
        }
        // getDimensionPixelOffset(default_focus_stroke_radius) truncates to int first; outer half stroke width is 3dp.
        val stroke = Stroke(3.dp.toPx().toInt() * 2f)
        onDrawBehind {
            if (enabled) {
                clipPath(path, ClipOp.Difference) {
                    drawPath(path, maskColor(focus.value, color), style = stroke)
                }
            }
        }
    }
}

/** Observes window focus loss on the current platform and invokes [onFocusLost]. */
@Composable
internal expect fun ObserveWindowFocusLost(
    key: Any?,
    onFocusLost: () -> Unit,
)

/** Container mask for ListSelectedItemLayout: starts on DOWN, reaches .7 on short tap/CANCEL before exiting. */
private class CouiListPressMask {
    private val effect = CouiStateProgress()
    val progress: Float get() = effect.value
    private var touched = false
    private var semanticPressed = false
    private var awaitingSemanticRelease = false
    private var enabled = true
    private var disposed = false
    fun touch(value: Boolean) {
        touched = value
        awaitingSemanticRelease = !value
        update()
    }
    fun updateState(active: Boolean, pressed: Boolean) {
        enabled = active
        if (!pressed) awaitingSemanticRelease = false
        semanticPressed = pressed && !awaitingSemanticRelease
        update()
    }
    private fun update() {
        if (disposed) return
        if (!enabled) {
            clear()
            return
        }
        effect.setActive(touched || (semanticPressed && !awaitingSemanticRelease), minimumPress = true)
    }
    fun clear() {
        touched = false
        semanticPressed = false
        awaitingSemanticRelease = true
        effect.reset()
    }

    // Activity transitions drop window focus while still drawing. Complete minimum press and touch exit instead of snapping to zero.
    fun release() {
        touched = false
        semanticPressed = false
        awaitingSemanticRelease = true
        update()
    }
    fun dispose() {
        disposed = true
        clear()
        effect.dispose()
    }
}

private fun maskColor(progress: Float, target: Color): Color = lerp(Color.Transparent, target, progress.coerceIn(0f, 1f))
