// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.preference

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.squircle.SquircleShape
import io.github.suqi8.coui.kmp.squircle.isSquircleEnabled
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * The application header a COUI about page opens with, mirroring the `aboutpage`
 * COUIAppInfoPreference.
 *
 * The device class extends `COUIPreference` and its layout
 * `res/layout/coui_component_preference_application_info.xml` is a centred `ConstraintLayout`: a
 * 60dp `COUIRoundImageView` with `couiBorderRadius` 16dp and `couiType="round"`, a 42dp top margin
 * above it, then the 20sp application name (`?couiColorPrimaryNeutral`, 8dp below the icon) and the
 * 12sp version (`?couiColorSecondNeutral`, 4dp below the name). The root carries an 8dp bottom
 * margin.
 *
 * `couiType="round"` routes the icon through `COUIRoundRectUtil.getPath`, which draws
 * `COUIShapePath`'s smooth corner rather than an arc, so this port clips with [SquircleShape] — the
 * repo's port of that same path — and forwards [isSquircleEnabled] through it like every other COUI
 * surface does.
 *
 * The device's long press on the version opens a `COUIPopupWindow` holding a copy action that puts
 * [appVersion] on the clipboard and toasts `copyFinishText`. That popup is app copy, not preference
 * geometry, so the port stops at [onVersionLongClick] and lets the caller decide what the gesture
 * does.
 *
 * @param appName The application name (COUI `appName`).
 * @param appVersion The version string (COUI `appVersion`).
 * @param modifier The modifier to be applied to the [AppInfoPreference].
 * @param icon The application icon (COUI `appIcon`). Drawn untinted, since it is not a glyph.
 * @param onVersionLongClick The callback when the version line is long-pressed (COUI's copy popup).
 *   Null leaves the line inert, as the device does when no `copyText` is set.
 * @param appNameColor The color of the application name (`?couiColorPrimaryNeutral`).
 * @param appVersionColor The color of the version (`?couiColorSecondNeutral`).
 */
@Composable
@NonRestartableComposable
fun AppInfoPreference(
    appName: String,
    appVersion: String,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    onVersionLongClick: (() -> Unit)? = null,
    appNameColor: Color = COUITheme.colorScheme.onBackground,
    appVersionColor: Color = COUITheme.colorScheme.onBackgroundVariant,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = AppInfoPreferenceDefaults.MarginBottom),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = appName,
                modifier = Modifier
                    .padding(top = AppInfoPreferenceDefaults.IconMarginTop)
                    .size(AppInfoPreferenceDefaults.IconSize)
                    .clip(
                        SquircleShape(
                            cornerRadius = AppInfoPreferenceDefaults.IconCornerRadius,
                            squircleEnabled = isSquircleEnabled(),
                        ),
                    ),
                tint = Color.Unspecified,
            )
        }
        Text(
            text = appName,
            modifier = Modifier.padding(top = AppInfoPreferenceDefaults.NameMarginTop),
            style = COUITheme.textStyles.headline1.copy(
                fontSize = AppInfoPreferenceDefaults.AppNameTextSize,
            ),
            color = appNameColor,
            textAlign = TextAlign.Center,
        )
        Text(
            text = appVersion,
            modifier = Modifier
                .padding(top = AppInfoPreferenceDefaults.VersionMarginTop)
                .then(
                    if (onVersionLongClick != null) {
                        Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = onVersionLongClick,
                        )
                    } else {
                        Modifier
                    },
                ),
            style = COUITheme.textStyles.footnote1.copy(
                fontSize = AppInfoPreferenceDefaults.AppVersionTextSize,
            ),
            color = appVersionColor,
            textAlign = TextAlign.Center,
        )
    }
}

object AppInfoPreferenceDefaults {

    /** `about_app_icon`'s `layout_width` / `layout_height` in the layout (60dp). */
    val IconSize = 60.dp

    /** `about_app_icon`'s `couiBorderRadius` (16dp). */
    val IconCornerRadius = 16.dp

    /** `about_app_icon`'s `layout_marginTop` (42dp). */
    val IconMarginTop = 42.dp

    /** `about_app_name`'s `layout_marginTop` (8dp). */
    val NameMarginTop = 8.dp

    /** `about_app_version`'s `layout_marginTop` (4dp). */
    val VersionMarginTop = 4.dp

    /** The root's `layout_marginBottom` (8dp). */
    val MarginBottom = 8.dp

    /**
     * `about_app_name`'s explicit `android:textSize` (20sp), which overrides the 16sp its
     * `COUIPreferenceTitleStyle` parent would give it.
     */
    val AppNameTextSize = 20.sp

    /**
     * `about_app_version`'s explicit `android:textSize` (12sp), the size of
     * `couiTextAppearanceDescription`.
     */
    val AppVersionTextSize = 12.sp
}
