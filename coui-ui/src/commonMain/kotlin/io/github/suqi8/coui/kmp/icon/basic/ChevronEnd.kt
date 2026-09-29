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
 * COUI next chevron, ported 1:1 from ColorOS coui_ic_chevron_end.xml
 * (24x24dp viewport, 1.4dp round-cap stroke, autoMirrored, tinted `?attr/couiColorPrimaryNeutral`).
 * This is the `src` of the calendar header's trailing button.
 */
val COUIIcons.Basic.ChevronEnd: ImageVector
    get() {
        if (_chevronEnd != null) return _chevronEnd!!
        _chevronEnd = ImageVector.Builder("ChevronEnd", 24.dp, 24.dp, 24f, 24f, autoMirror = true).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.4f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(10.16f, 5.636f)
                lineTo(16.171f, 11.646f)
                curveTo(16.366f, 11.842f, 16.366f, 12.158f, 16.171f, 12.354f)
                lineTo(10.16f, 18.364f)
            }
        }.build()
        return _chevronEnd!!
    }

private var _chevronEnd: ImageVector? = null
