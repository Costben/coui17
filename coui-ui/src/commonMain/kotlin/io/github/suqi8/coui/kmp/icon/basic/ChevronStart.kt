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
 * COUI previous chevron, ported 1:1 from ColorOS coui_ic_chevron_start.xml
 * (24x24dp viewport, 1.4dp round-cap stroke, autoMirrored, tinted `?attr/couiColorPrimaryNeutral`).
 * This is the `src` of the calendar header's leading button.
 */
val COUIIcons.Basic.ChevronStart: ImageVector
    get() {
        if (_chevronStart != null) return _chevronStart!!
        _chevronStart = ImageVector.Builder("ChevronStart", 24.dp, 24.dp, 24f, 24f, autoMirror = true).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.4f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(13.84f, 5.636f)
                lineTo(7.829f, 11.646f)
                curveTo(7.634f, 11.842f, 7.634f, 12.158f, 7.829f, 12.354f)
                lineTo(13.84f, 18.364f)
            }
        }.build()
        return _chevronStart!!
    }

private var _chevronStart: ImageVector? = null
