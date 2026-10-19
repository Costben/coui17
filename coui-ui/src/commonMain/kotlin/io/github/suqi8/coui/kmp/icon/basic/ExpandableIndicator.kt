// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.suqi8.coui.kmp.icon.basic

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.icon.COUIIcons

/**
 * COUI expandable-list indicator, ported 1:1 from ColorOS `coui_list_expandable_indicator.xml`
 * (24x24dp viewport, two 1.4dp round-cap strokes; the glyph sits in the trailing half of the box,
 * as the native vector does). Used as the trailing icon of a popup row that opens a submenu.
 * Render at its native 24x24dp size and tint with couiColorLabelTertiary.
 */
val COUIIcons.Basic.ExpandableIndicator: ImageVector
    get() {
        if (_expandableIndicator != null) return _expandableIndicator!!
        _expandableIndicator = ImageVector.Builder("ExpandableIndicator", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.4f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(14.2f, 10f)
                lineTo(18.329f, 5.413f)
                curveTo(18.527f, 5.192f, 18.873f, 5.192f, 19.072f, 5.413f)
                lineTo(23.2f, 10f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.4f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(23.2f, 14f)
                lineTo(19.072f, 18.587f)
                curveTo(18.873f, 18.808f, 18.527f, 18.808f, 18.329f, 18.587f)
                lineTo(14.2f, 14f)
            }
        }.build()
        return _expandableIndicator!!
    }

private var _expandableIndicator: ImageVector? = null
