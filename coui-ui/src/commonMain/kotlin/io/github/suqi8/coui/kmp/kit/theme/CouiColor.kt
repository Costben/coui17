// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.theme

import androidx.compose.ui.graphics.Color

/** Fixed day/night COUI colors grouped by semantic role for components and icons. */
internal object CouiColor {
    /** Day palette values. */
    object Day {
        val BLUE = Color(0xFF0080FF)
        val GREEN = Color(0xFF00BD13)
        val YELLOW = Color(0xFFFFB200)
        val RED = Color(0xFFEB3B2F)
        val ORANGE = Color(0xFFFF7700)
        val GRAY = Color(0xFF474747)
        val DARK_GRAY = Color(0xFF191919)

        /** Extracted from the original fan icon; reused on the night side as an unconfirmed COUI theme color. */
        val CYAN_BLUE = Color(0xFF00A1FF)

        val HOME_BLUE = BLUE
        val HOME_GREEN = GREEN
        val HOME_YELLOW = YELLOW
    }

    /** Night palette values. */
    object Night {
        val BLUE = Color(0xFF1A8CFF)
        val GREEN = Color(0xFF24B232)
        val YELLOW = Color(0xFFE5A100)
        val RED = Color(0xFFEB493D)
        val ORANGE = Color(0xFFF08222)
        val GRAY = Day.GRAY
        val DARK_GRAY = Day.DARK_GRAY
        val CYAN_BLUE = Day.CYAN_BLUE

        val HOME_BLUE = BLUE
        val HOME_GREEN = GREEN
        val HOME_YELLOW = YELLOW
    }
}
