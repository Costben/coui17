// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.input

import androidx.compose.ui.input.pointer.PointerEvent

internal actual fun PointerEvent.isCouiTouchCancelled(): Boolean = nativeEvent == null && hasCouiSyntheticCancelChanges()
