// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.theme

import androidx.compose.ui.text.PlatformTextStyle

/** Returns the default platform text style for COUI typography on the current platform. */
internal expect fun couiDefaultPlatformTextStyle(): PlatformTextStyle?
