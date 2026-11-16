// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.effect

import androidx.compose.runtime.compositionLocalOf

/** 统一控制 COUI 描边与内光晕, 不控制模糊、材质、跟手光或外投影。 */
internal val LocalCouiContourGlowEnabled = compositionLocalOf { true }
