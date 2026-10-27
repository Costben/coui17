// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.LineArrow
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * The expand/collapse arrow ColorOS 17 puts on a collapsible row.
 *
 * It is the Compose form of `com.coui.appcompat.rotateview.COUIRotateView` with
 * `coui:supportRotateType="couiRotateAloneZ"`: a 24x24dp box holding the 16x24dp
 * `coui_line_arrow` chevron, rotated 180 degrees about its centre when the row expands. The
 * native widget keeps one drawable on screen for the whole transition — it never swaps in a
 * mirrored vector — and drives the rotation with a 400ms `PathInterpolator(0.133, 0, 0.3, 1)`.
 * Settings uses it for every expandable list row, e.g.
 * `settings_preference_widget_rotateview.xml` and `settings_professional_mode_preference_layout.xml`
 * (`@id/expand_list_item_indicator`, `layout_marginEnd="-4dp"`).
 *
 * @param expanded Whether the row is expanded; the arrow points down when `false` and up when `true`.
 * @param modifier The modifier to be applied to the 24x24dp box.
 * @param tint The arrow colour. COUI paints the vector with `?couiColorLabelSecondary`, the
 *   theme's [io.github.suqi8.coui.kmp.theme.ColorScheme.onSurfaceSecondary].
 * @param contentDescription Accessibility label; `null` keeps the arrow decorative, which is what
 *   the native widget does (`android:focusable="false"`, `android:clickable="false"`).
 */
@Composable
fun ExpandArrow(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = COUITheme.colorScheme.onSurfaceSecondary,
    contentDescription: String? = null,
) {
    val rotation = animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(
            durationMillis = ExpandArrowDefaults.DurationMillis,
            easing = ExpandArrowDefaults.Easing,
        ),
        label = "ExpandArrowRotation",
    )
    Box(
        modifier = modifier
            .size(ExpandArrowDefaults.Size)
            .graphicsLayer { rotationZ = rotation.value },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = COUIIcons.Basic.LineArrow,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(ExpandArrowDefaults.GlyphWidth, ExpandArrowDefaults.GlyphHeight),
        )
    }
}

/** Defaults for [ExpandArrow]. */
object ExpandArrowDefaults {
    /** The 24x24dp touch box. COUI `expandable_list_item_size`. */
    val Size = 24.dp

    /** The vector's width. COUI `coui_line_arrow` is a 16x24dp drawable. */
    val GlyphWidth = 16.dp

    /** The vector's height. */
    val GlyphHeight = 24.dp

    /**
     * The rotation duration. `COUIRotateView.mDuration = 0x190` (400ms), used by both
     * `animate().rotation(180f)` and `setRotation(0f|180f)`.
     */
    val DurationMillis = 400

    /**
     * The rotation easing. `COUIRotateView.mInterpolator = PathInterpolatorCompat.create(0.133f,
     * 0f, 0.3f, 1f)`.
     */
    val Easing = CubicBezierEasing(0.133f, 0f, 0.3f, 1f)
}
