// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.slider

import androidx.compose.ui.graphics.Color

internal object CouiSliderDefaults {
    fun colors(primary: Color, dark: Boolean, enabled: Boolean = true): CouiSliderColors {
        val inactive = Color(if (dark) 0x33ffffff else 0x1f000000)
        return CouiSliderColors(
            activeTrack = if (enabled) primary else inactive,
            inactiveTrack = inactive,
            thumb = if (enabled) Color.White else Color(if (dark) 0x29ffffff else 0x8affffff),
            activeTick = if (enabled) Color.White else Color(if (dark) 0x29ffffff else 0x8affffff),
            inactiveTick = Color(
                if (dark && !enabled) {
                    0x29ffffff
                } else if (dark) {
                    0x4dffffff
                } else {
                    0x42000000
                },
            ),
            thumbShadow = Color(if (dark) 0x40000000 else 0x1a000000),
        )
    }
}
