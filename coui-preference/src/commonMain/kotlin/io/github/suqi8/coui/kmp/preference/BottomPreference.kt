// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.preference

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The invisible bottom spacer a COUI settings screen ends with, mirroring COUIBottomPreference.
 *
 * `COUIBottomPreference` is the one preference that draws nothing: it inflates
 * `res/layout/coui_preference_bottom.xml`, an invisible `FrameLayout` whose only content is
 * `android:layout_height="@dimen/support_preference_foot_preference_padding_bottom"`, and
 * `onBindViewHolder` copies `mPlaceholderHeight` onto the item's layout params. The attribute
 * `couiPlaceholderHeight` overrides it; its default is
 * `dimen/support_preference_foot_preference_padding_bottom`, which aliases
 * `dimen/coui_list_to_ex_bottom_padding` (32dp).
 *
 * @param modifier The modifier to be applied to the [BottomPreference].
 * @param height The placeholder height (COUI `couiPlaceholderHeight`).
 */
@Composable
@NonRestartableComposable
fun BottomPreference(
    modifier: Modifier = Modifier,
    height: Dp = BottomPreferenceDefaults.Height,
) {
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    )
}

object BottomPreferenceDefaults {

    /**
     * `dimen/support_preference_foot_preference_padding_bottom`, aliasing
     * `dimen/coui_list_to_ex_bottom_padding` (32dp).
     */
    val Height = 32.dp
}
