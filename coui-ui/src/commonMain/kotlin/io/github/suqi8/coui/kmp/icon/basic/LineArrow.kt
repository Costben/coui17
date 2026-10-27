// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.suqi8.coui.kmp.icon.basic

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.icon.COUIIcons

/**
 * COUI line arrow, ported 1:1 from ColorOS `coui_line_arrow_collapsed.xml`: a 16x24dp filled
 * chevron pointing down, painted with `?couiColorLabelSecondary`.
 *
 * This is the glyph `COUIRotateView` shows for an expandable row. The native widget
 * (`com.coui.appcompat.rotateview.COUIRotateView` with `coui:supportRotateType="couiRotateAloneZ"`)
 * keeps this single collapsed vector on screen and rotates the view 180 degrees when the row
 * expands, so callers must render it inside a 24x24dp box (`expandable_list_item_size`) and rotate
 * the box instead of swapping in the mirrored `coui_line_arrow_expanded` vector. Use
 * `ExpandArrow` for the ready-made behaviour.
 *
 * Render at its native 16x24dp size.
 */
val COUIIcons.Basic.LineArrow: ImageVector
    get() {
        if (_lineArrow != null) return _lineArrow!!
        _lineArrow = ImageVector.Builder("LineArrow", 16.dp, 24.dp, 16f, 24f).apply {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                moveTo(8.595f, 13.743f)
                curveTo(8.751f, 13.901f, 8.829f, 13.981f, 8.921f, 14.003f)
                curveTo(8.973f, 14.016f, 9.027f, 14.016f, 9.079f, 14.003f)
                curveTo(9.171f, 13.981f, 9.249f, 13.901f, 9.405f, 13.743f)
                lineTo(15.035f, 8f)
                lineTo(16f, 8.975f)
                lineTo(9.795f, 15.202f)
                curveTo(9.489f, 15.509f, 9.336f, 15.663f, 9.154f, 15.707f)
                curveTo(9.053f, 15.731f, 8.947f, 15.731f, 8.846f, 15.707f)
                curveTo(8.664f, 15.663f, 8.511f, 15.509f, 8.205f, 15.202f)
                lineTo(2f, 8.975f)
                lineTo(2.966f, 8f)
                lineTo(8.595f, 13.743f)
                close()
            }
        }.build()
        return _lineArrow!!
    }

private var _lineArrow: ImageVector? = null
