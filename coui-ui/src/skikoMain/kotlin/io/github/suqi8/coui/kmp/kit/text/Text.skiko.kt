// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

@Composable
internal actual fun rememberCouiTextFontScale(): Float = LocalDensity.current.fontScale

internal actual fun couiLineSpacing(kind: CouiTextKind): Float = when (kind) {
    CouiTextKind.Category -> 1f
    CouiTextKind.GroupTitle -> 1.143f
    CouiTextKind.Description -> 1.143f
    CouiTextKind.Stepper -> 1.1429f
    CouiTextKind.Progress -> 1.2245f
    CouiTextKind.PreferenceTitle -> 1.158f
    CouiTextKind.Toolbar -> 1.1429f
    CouiTextKind.PreferenceSummary -> 1.1739f
    CouiTextKind.PageTitle -> 1f
    CouiTextKind.SectionTitle -> 1.1429f
    CouiTextKind.Body -> 1.1321f
    CouiTextKind.HomeTitle -> 1f
    CouiTextKind.Search -> 1.1321f
    CouiTextKind.SearchCancel -> 1.2245f
    CouiTextKind.PreferenceValue -> 1.2245f
}
