// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.text

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
internal actual fun rememberCouiTextFontScale(): Float = LocalConfiguration.current.fontScale

internal actual fun couiLineSpacing(kind: CouiTextKind): Float = when (kind) {
    CouiTextKind.Category -> 1f
    CouiTextKind.GroupTitle -> if (Build.VERSION.SDK_INT >= 35) 1.143f else 1f
    CouiTextKind.Description -> if (Build.VERSION.SDK_INT >= 35) 1.143f else 1f
    CouiTextKind.Stepper -> if (Build.VERSION.SDK_INT >= 35) 1.1429f else 1.15f
    CouiTextKind.Progress -> if (Build.VERSION.SDK_INT >= 35) 1.2245f else 1.1f
    CouiTextKind.PreferenceTitle -> if (Build.VERSION.SDK_INT >= 35) 1.158f else 1.05f
    CouiTextKind.Toolbar -> if (Build.VERSION.SDK_INT >= 35) 1.1429f else 1.15f
    CouiTextKind.PreferenceSummary -> if (Build.VERSION.SDK_INT >= 35) 1.1739f else 1.1f
    CouiTextKind.PageTitle -> 1f
    CouiTextKind.SectionTitle -> if (Build.VERSION.SDK_INT >= 35) 1.1429f else 1.15f
    CouiTextKind.Body -> 1.1321f
    CouiTextKind.HomeTitle -> 1f
    CouiTextKind.Search -> if (Build.VERSION.SDK_INT >= 35) 1.1321f else 1.1f
    CouiTextKind.SearchCancel -> if (Build.VERSION.SDK_INT >= 35) 1.2245f else 1.1f
    CouiTextKind.PreferenceValue -> if (Build.VERSION.SDK_INT >= 35) 1.2245f else 1.1f
}
