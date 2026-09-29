// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.preference

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.basic.CircularProgressIndicator
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.preference.internal.CategoryHeader
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * A category header whose trailing widget is a loading indicator, mirroring
 * COUILoadingPreferenceCategory.
 *
 * The device class extends `COUIPreferenceCategory` and swaps the category's `widget_frame` content
 * through its `LoadingType`:
 *
 * | `LoadingType` | widget |
 * | :--- | :--- |
 * | `LOADING` (0) | `coui_preference_category_widget_layout_loading.xml`, indicator playing |
 * | `PAUSE` (1) | the same layout, indicator paused |
 * | `INVISIBLE` (2) | the same layout, indicator `gone` |
 * | `AFTER_LOADING` (3) | the `coui_loading_after_layout` resource |
 * | `BEFORE_LOADING` (4) | the `coui_loading_before_layout` resource |
 *
 * `PAUSE` and `INVISIBLE` are intentionally not modelled. Both keep whatever widget layout the
 * category already had and only start or stop the `COUICompProgressIndicator`'s animation, so a
 * Compose caller can express them by not switching [loading] at all — there is no state to carry.
 * The two resource-swapping types collapse into [widget], which is what the caller would hand to
 * either one.
 *
 * @param title The title of the [LoadingPreferenceCategory].
 * @param modifier The modifier to be applied to the [LoadingPreferenceCategory].
 * @param loading Whether the loading widget is shown (COUI `LoadingType.LOADING`). When false the
 *   [widget] is shown instead (COUI `LoadingType.AFTER_LOADING`).
 * @param loadingText The text before the indicator (COUI `text_in_loading`). Null hides the label,
 *   leaving the bare 16dp indicator, exactly as `TextUtils.isEmpty(mLoadingText)` does on the device.
 * @param widget The trailing widget shown while [loading] is false (COUI
 *   `coui_loading_after_layout` / `coui_loading_before_layout`).
 * @param titleColor The color of the title.
 */
@Composable
@NonRestartableComposable
fun LoadingPreferenceCategory(
    title: String,
    modifier: Modifier = Modifier,
    loading: Boolean = true,
    loadingText: String? = null,
    widget: (@Composable () -> Unit)? = null,
    titleColor: Color = COUITheme.colorScheme.onBackgroundVariant,
) {
    CategoryHeader(
        title = title,
        modifier = modifier,
        titleColor = titleColor,
        trailing = if (loading) {
            {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (loadingText != null) {
                        Text(
                            text = loadingText,
                            modifier = Modifier.padding(
                                end = LoadingPreferenceCategoryDefaults.TextMarginEnd,
                            ),
                            style = COUITheme.textStyles.subtitle.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = COUITheme.colorScheme.onBackground,
                            maxLines = 2,
                        )
                    }
                    CircularProgressIndicator(
                        size = LoadingPreferenceCategoryDefaults.LoadingSize,
                    )
                }
            }
        } else {
            widget
        },
    )
}

object LoadingPreferenceCategoryDefaults {

    /**
     * `couiSmallLottieLoadingViewWidth` / `couiSmallLottieLoadingViewHeight` (16dp), the size the
     * `coui_preference_category_widget_layout_loading.xml` indicator is given.
     */
    val LoadingSize = 16.dp

    /**
     * The `layout_marginEnd` of `text_in_loading` in
     * `coui_preference_category_widget_layout_loading.xml` (4dp).
     */
    val TextMarginEnd = 4.dp
}
