// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.suqi8.coui.kmp.icon.basic

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.icon.COUIIcons

/**
 * COUI shrink / expand side pane glyph, ported 1:1 from ColorOS coui_pane_fold_normal.xml
 * (48x52 viewport, 1.4dp round-cap round-join stroke: a rounded rectangle with a vertical divider).
 * This is the default icon of COUISidePaneLayout#createIconView, whose ImageButton inflates
 * layout/coui_sliding_icon_layout. Render at its native 48x52dp size and tint with
 * couiColorLabelPrimary. The vector's android:tint selector is dropped here so the caller
 * controls the colour.
 */
val COUIIcons.Basic.FoldPane: ImageVector
    get() {
        if (_foldPane != null) return _foldPane!!
        _foldPane = ImageVector.Builder("FoldPane", 48.dp, 52.dp, 48f, 52f).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.4f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(14f, 32.7f)
                lineTo(14f, 19.3f)
                arcToRelative(1.3f, 1.3f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 1.3f, dy1 = -1.3f)
                lineTo(32.7f, 18f)
                arcToRelative(1.3f, 1.3f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = 1.3f, dy1 = 1.3f)
                lineTo(34f, 32.7f)
                arcToRelative(1.3f, 1.3f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -1.3f, dy1 = 1.3f)
                lineTo(15.3f, 34f)
                arcToRelative(1.3f, 1.3f, 0f, isMoreThanHalf = false, isPositiveArc = true, dx1 = -1.3f, dy1 = -1.3f)
                close()
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.4f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(22f, 34f)
                lineTo(22f, 18f)
            }
        }.build()
        return _foldPane!!
    }

private var _foldPane: ImageVector? = null
