// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.preference

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * The COUI card entrance: a tappable card block that opens another page, mirroring
 * `com.coui.appcompat.card.COUICardEntrancePreference` — which itself extends
 * `COUIPressFeedbackJumpPreference` (`COUIJumpPreference` with a press-feedback touch listener).
 *
 * The device switches between two layouts on `cardType`
 * (`COUICardEntrancePreference#getLayoutResByCardType`):
 * - [CardEntranceType.Small] → `res/layout/coui_component_card_entrance_preference_type_small.xml`,
 *   a vertical `LinearLayout` (16dp padding) with the round icon on top, the title 12dp below it and
 *   the summary 4dp below the title, on
 *   `res/drawable/coui_component_card_entrance_small_bg.xml` (corners 12dp,
 *   `?couiColorCardBackground`);
 * - [CardEntranceType.Large] → `res/layout/coui_component_card_entrance_preference_type_large.xml`,
 *   a `RelativeLayout` (start 16dp / end 12dp / vertical 20dp) with the round icon vertically
 *   centred at the start and the title + summary 16dp to its end (summary 3dp below the title), on
 *   `res/drawable/coui_component_card_entrance_large_bg.xml` (corners 16dp).
 *
 * Both layouts colour the title `?couiColorPrimaryNeutral` and the summary `?couiColorSecondNeutral`;
 * `setSummaryStatus(true)` repaints the summary with `?couiColorPrimaryText`. `showSummary = false`
 * keeps the summary out of the card and puts it on the device's separate status line, which this
 * port does not model — pass a summary with [statusOn] instead.
 *
 * The press feedback reuses the repository's [Card] press tint, which is the COUI card press
 * behaviour (`COUICardListSelectedItemLayout`); the device's `COUIPressFeedbackHelper` itself is
 * Android touch infrastructure and is not ported.
 *
 * @param title The title of the card (`?couiColorPrimaryNeutral`).
 * @param modifier The modifier to be applied to the [CardEntrancePreference].
 * @param cardType Which of the two device layouts to use; see [CardEntranceType].
 * @param icon The card icon (COUI `android:id/icon`, a `couiType="round"` `COUIRoundImageView`).
 * @param summary The summary of the card. Null leaves the line out.
 * @param statusOn Whether the summary is in its "on" state
 *   (`COUICardEntrancePreference#setStatusOn`), which repaints it with `?couiColorPrimaryText`.
 * @param tintIcon How the icon is tinted; see [CardEntranceTintIcon].
 * @param onClick The callback when the card is clicked. Null leaves the card inert.
 * @param colors The [CardEntranceColors] of the [CardEntrancePreference].
 */
@Composable
@NonRestartableComposable
fun CardEntrancePreference(
    title: String,
    modifier: Modifier = Modifier,
    cardType: CardEntranceType = CardEntranceType.Small,
    icon: Painter? = null,
    summary: String? = null,
    statusOn: Boolean = false,
    tintIcon: CardEntranceTintIcon = CardEntranceTintIcon.None,
    onClick: (() -> Unit)? = null,
    colors: CardEntranceColors = CardEntrancePreferenceDefaults.cardEntranceColors(),
) {
    val iconTint = when (tintIcon) {
        CardEntranceTintIcon.None -> Color.Unspecified
        CardEntranceTintIcon.ByGlobalTheme, CardEntranceTintIcon.Always -> colors.iconTint
    }
    val summaryColor = if (statusOn) colors.activeSummaryColor else colors.summaryColor

    Card(
        modifier = modifier,
        cornerRadius = cardType.cornerRadius,
        colors = CardDefaults.defaultColors(
            color = colors.containerColor,
            pressedColor = colors.pressedColor,
        ),
        onClick = onClick,
    ) {
        when (cardType) {
            CardEntranceType.Small -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(CardEntrancePreferenceDefaults.SmallContentPadding),
            ) {
                if (icon != null) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        modifier = Modifier.size(CardEntrancePreferenceDefaults.IconSize),
                        tint = iconTint,
                    )
                }
                Text(
                    text = title,
                    modifier = Modifier.padding(top = CardEntrancePreferenceDefaults.SmallTitleMarginTop),
                    style = COUITheme.textStyles.headline2,
                    color = colors.titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (summary != null) {
                    Text(
                        text = summary,
                        modifier = Modifier.padding(top = CardEntrancePreferenceDefaults.SmallSummaryMarginTop),
                        style = COUITheme.textStyles.footnote1.copy(
                            fontSize = CardEntrancePreferenceDefaults.SummaryTextSize,
                        ),
                        color = summaryColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            CardEntranceType.Large -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(CardEntrancePreferenceDefaults.LargeContentPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        modifier = Modifier.size(CardEntrancePreferenceDefaults.IconSize),
                        tint = iconTint,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = CardEntrancePreferenceDefaults.LargeIconMarginEnd),
                ) {
                    Text(
                        text = title,
                        style = COUITheme.textStyles.headline2,
                        color = colors.titleColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (summary != null) {
                        Text(
                            text = summary,
                            modifier = Modifier.padding(
                                top = CardEntrancePreferenceDefaults.LargeSummaryMarginTop,
                            ),
                            style = COUITheme.textStyles.footnote1.copy(
                                fontSize = CardEntrancePreferenceDefaults.SummaryTextSize,
                            ),
                            color = summaryColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The two device entrance layouts, `COUICardEntrancePreference.CARD_TYPE_SMALL` (1) and
 * `CARD_TYPE_LARGE` (2).
 */
enum class CardEntranceType(
    val cornerRadius: Dp,
) {
    /** `CARD_TYPE_SMALL` (1): icon above the text, 12dp corners. */
    Small(CardEntrancePreferenceDefaults.SmallCornerRadius),

    /** `CARD_TYPE_LARGE` (2): icon beside the text, 16dp corners. */
    Large(CardEntrancePreferenceDefaults.LargeCornerRadius),
}

/**
 * How [CardEntrancePreference] tints its icon, `COUICardEntrancePreference.TINT_ICON_*`.
 *
 * The device routes both non-`None` modes through `COUIThemeOverlay.applyCOUITintIcon`; this port
 * has a single theme tint, so [ByGlobalTheme] and [Always] resolve to the same colour.
 */
enum class CardEntranceTintIcon {
    /** `TINT_ICON_NONE` (0): draw the icon untinted. */
    None,

    /** `TINT_ICON_BY_GLOBAL_THEME` (1): tint with the global COUI theme. */
    ByGlobalTheme,

    /** `TINT_ICON_ANYWAY` (2): tint regardless of the global theme flag. */
    Always,
}

object CardEntrancePreferenceDefaults {

    /** `coui_component_card_entrance_small_bg`'s `<corners android:radius>` (12dp). */
    val SmallCornerRadius = 12.dp

    /** `coui_component_card_entrance_large_bg`'s `<corners android:radius>` (16dp). */
    val LargeCornerRadius = 16.dp

    /** The small layout's root `android:padding` (16dp). */
    val SmallContentPadding = PaddingValues(16.dp)

    /** The small layout's title `android:layout_marginTop` (12dp). */
    val SmallTitleMarginTop = 12.dp

    /** The small layout's summary `android:layout_marginTop` (4dp). */
    val SmallSummaryMarginTop = 4.dp

    /**
     * The large layout's root padding: `paddingStart` 16dp, `paddingEnd` 12dp,
     * `paddingVertical` 20dp.
     */
    val LargeContentPadding = PaddingValues(start = 16.dp, end = 12.dp, top = 20.dp, bottom = 20.dp)

    /** The large layout's text block `android:layout_marginStart` (16dp). */
    val LargeIconMarginEnd = 16.dp

    /** The large layout's summary `android:layout_marginTop` (3dp). */
    val LargeSummaryMarginTop = 3.dp

    /**
     * The icon's `layout_width` / `layout_height`. The device leaves both `wrap_content` and sizes the
     * `COUIRoundImageView` from the drawable it is given, so there is no `coui_*` dimen for it; this
     * port picks 32dp as the card-entry glyph size rather than guessing a device value.
     */
    val IconSize = 32.dp

    /** `couiTextAppearanceDescription` (12sp), the summary text size. */
    val SummaryTextSize = 12.sp

    /**
     * The default [CardEntranceColors]. [containerColor] follows `?couiColorCardBackground`
     * ([COUITheme.colorScheme.surfaceContainer]); [titleColor] `?couiColorPrimaryNeutral`,
     * [summaryColor] `?couiColorSecondNeutral` and [activeSummaryColor] `?couiColorPrimaryText`.
     */
    @Composable
    fun cardEntranceColors(
        containerColor: Color = COUITheme.colorScheme.surfaceContainer,
        titleColor: Color = COUITheme.colorScheme.onBackground,
        summaryColor: Color = COUITheme.colorScheme.onSurfaceSecondary,
        activeSummaryColor: Color = COUITheme.colorScheme.onBackground,
        iconTint: Color = COUITheme.colorScheme.onBackground,
        pressedColor: Color = COUITheme.colorScheme.surfaceContainerHigh,
    ): CardEntranceColors = remember(
        containerColor,
        titleColor,
        summaryColor,
        activeSummaryColor,
        iconTint,
        pressedColor,
    ) {
        CardEntranceColors(
            containerColor = containerColor,
            titleColor = titleColor,
            summaryColor = summaryColor,
            activeSummaryColor = activeSummaryColor,
            iconTint = iconTint,
            pressedColor = pressedColor,
        )
    }
}

/**
 * Colors used by a [CardEntrancePreference].
 *
 * @param containerColor The card background (`?couiColorCardBackground`).
 * @param titleColor The title color (`?couiColorPrimaryNeutral`).
 * @param summaryColor The summary color (`?couiColorSecondNeutral`).
 * @param activeSummaryColor The summary color when the status is on (`?couiColorPrimaryText`).
 * @param iconTint The icon tint for [CardEntranceTintIcon.ByGlobalTheme] / [CardEntranceTintIcon.Always].
 * @param pressedColor The fill the card animates towards while pressed (`couiColorCardPressed`).
 */
@Immutable
data class CardEntranceColors(
    val containerColor: Color,
    val titleColor: Color,
    val summaryColor: Color,
    val activeSummaryColor: Color,
    val iconTint: Color,
    val pressedColor: Color,
)
