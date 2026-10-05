// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.preference

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.PageIndicator
import io.github.suqi8.coui.kmp.basic.RadioButton
import io.github.suqi8.coui.kmp.basic.RadioButtonDefaults
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * The COUI card instruction block: a card that walks through a settings feature with an illustration,
 * a title and a description, mirroring `com.coui.appcompat.card.COUICardInstructionPreference`.
 *
 * The device class extends `COUIPreference` and inflates
 * `res/layout/coui_component_card_instruction_preference.xml` — a 16dp-corner card
 * (`res/drawable/coui_component_card_instruction_bg.xml`, `?couiColorCardBackground`,
 * 16dp horizontal / 24dp vertical padding) holding a `ViewPager2` and, 24dp below it, a
 * `COUIPageIndicator`. It switches the pager adapter on `cardType`
 * (`COUICardInstructionPreference#getPageAdapter`):
 * - [CardInstructionType.Description] (`CARD_INSTRUCTION_TYPE_DESCRIPTION`, 1) →
 *   `CardInstructionDescriptionAdapter` over
 *   `res/layout/coui_component_card_instruction_description_page.xml`: an animation/image row, then a
 *   scrollable block capped at `coui_component_card_instruction_content_height_complete` (102dp) with
 *   the title (`?couiTextAppearanceHeadline6`, `?couiColorPrimaryNeutral`, centred, at most two
 *   lines) and the summary (`?couiTextAppearanceDescription`, `?couiColorSecondNeutral`) 12dp below
 *   it;
 * - [CardInstructionType.Selector] (`CARD_INSTRUCTION_TYPE_SELECTOR`, 2) →
 *   `CardInstructionSelectorAdapter` over
 *   `res/layout/coui_component_card_instruction_selector_page.xml`: a horizontal row of selectable
 *   items, each `res/layout/coui_component_card_instruction_selector.xml` — an animation, the title
 *   (`?couiTextAppearanceButton`, `?couiColorSecondNeutral`) 24dp below it and a 24dp `RadioButton`
 *   (4dp above, 24dp below) — separated by the 24dp
 *   `res/drawable/coui_component_card_instruction_divider.xml` and split evenly with
 *   `layout_weight = 1`.
 *
 * Not ported, because the device class is Android View infrastructure: the `ViewPager2`, the
 * `COUIPageIndicator` view (`basic/PageIndicator.kt` stands in), the `RecyclerView.Adapter`
 * hierarchy (`BaseCardInstructionAdapter`, `CardInstructionDescriptionAdapter`,
 * `CardInstructionSelectorAdapter`) and the `COUIMutableSizeScrollView`. The animation views
 * (`com.oplus.anim.EffectiveAnimationView`) are replaced by the caller-supplied [illustration] slot.
 * The 40dp content fading edge
 * (`coui_component_card_instruction_content_fading_edge_length`) is not ported either: the content
 * block is simply capped at [CardInstructionPreferenceDefaults.ContentHeightComplete].
 *
 * @param items The pages (or selector choices) of the card.
 * @param modifier The modifier to be applied to the [CardInstructionPreference].
 * @param cardType Which of the two device adapters to use; see [CardInstructionType].
 * @param selectedIndex The active page (Description) or selected choice (Selector).
 * @param onSelectedIndexChange The callback when the active page changes. The device routes this
 *   through `ViewPager2.OnPageChangeCallback` (Description) or
 *   `COUICardInstructionPreference.OnItemSelectedListener` (Selector).
 * @param colors The [CardInstructionColors] of the [CardInstructionPreference].
 * @param illustration The illustration of the item at the given index. Null leaves it out.
 */
@Composable
fun CardInstructionPreference(
    items: List<CardInstructionItem>,
    modifier: Modifier = Modifier,
    cardType: CardInstructionType = CardInstructionType.Description,
    selectedIndex: Int = 0,
    onSelectedIndexChange: (Int) -> Unit = {},
    colors: CardInstructionColors = CardInstructionPreferenceDefaults.cardInstructionColors(),
    illustration: (@Composable (index: Int) -> Unit)? = null,
) {
    if (items.isEmpty()) return

    Card(
        modifier = modifier,
        cornerRadius = CardInstructionPreferenceDefaults.CornerRadius,
        colors = CardDefaults.defaultColors(
            color = colors.containerColor,
            pressedColor = colors.containerColor,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardInstructionPreferenceDefaults.ContentPadding),
        ) {
            when (cardType) {
                CardInstructionType.Description -> DescriptionContent(
                    items = items,
                    selectedIndex = selectedIndex,
                    onSelectedIndexChange = onSelectedIndexChange,
                    colors = colors,
                    illustration = illustration,
                )

                CardInstructionType.Selector -> SelectorContent(
                    items = items,
                    selectedIndex = selectedIndex,
                    onSelectedIndexChange = onSelectedIndexChange,
                    colors = colors,
                    illustration = illustration,
                )
            }
        }
    }
}

@Composable
private fun DescriptionContent(
    items: List<CardInstructionItem>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    colors: CardInstructionColors,
    illustration: (@Composable (index: Int) -> Unit)?,
) {
    val currentOnSelectedIndexChange by rememberUpdatedState(onSelectedIndexChange)
    val pagerState = rememberPagerState(
        initialPage = selectedIndex.coerceIn(0, items.lastIndex),
        pageCount = { items.size },
    )

    LaunchedEffect(pagerState, selectedIndex, items.size) {
        val target = selectedIndex.coerceIn(0, items.lastIndex)
        if (pagerState.currentPage != target) pagerState.animateScrollToPage(target)
    }
    LaunchedEffect(pagerState.currentPage) {
        currentOnSelectedIndexChange(pagerState.currentPage)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) { page ->
            val item = items[page]
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                illustration?.invoke(page)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = if (illustration != null) {
                                CardInstructionPreferenceDefaults.AnimMarginTop
                            } else {
                                0.dp
                            },
                        )
                        .heightIn(max = CardInstructionPreferenceDefaults.ContentHeightComplete)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = item.title,
                        style = COUITheme.textStyles.headline2,
                        color = colors.titleColor,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.summary != null) {
                        Text(
                            text = item.summary,
                            modifier = Modifier.padding(
                                top = CardInstructionPreferenceDefaults.SummaryMarginTopSmall,
                            ),
                            style = COUITheme.textStyles.footnote1.copy(
                                fontSize = CardInstructionPreferenceDefaults.SummaryTextSize,
                            ),
                            color = colors.summaryColor,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        if (items.size > 1) {
            PageIndicator(
                count = items.size,
                currentIndex = pagerState.currentPage,
                modifier = Modifier.padding(
                    top = CardInstructionPreferenceDefaults.IndicatorMarginTop,
                ),
                activeColor = colors.indicatorActiveColor,
                inactiveColor = colors.indicatorInactiveColor,
            )
        }
    }
}

@Composable
private fun SelectorContent(
    items: List<CardInstructionItem>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    colors: CardInstructionColors,
    illustration: (@Composable (index: Int) -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CardInstructionPreferenceDefaults.DividerWidth),
        verticalAlignment = Alignment.Top,
    ) {
        items.forEachIndexed { index, item ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(role = Role.RadioButton, onClick = { onSelectedIndexChange(index) }),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                illustration?.invoke(index)
                Text(
                    text = item.title,
                    modifier = Modifier.padding(
                        top = if (illustration != null) {
                            CardInstructionPreferenceDefaults.SelectorTitleMarginTop
                        } else {
                            0.dp
                        },
                    ),
                    style = COUITheme.textStyles.button,
                    color = colors.summaryColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                RadioButton(
                    selected = index == selectedIndex,
                    onClick = null,
                    modifier = Modifier.padding(
                        top = CardInstructionPreferenceDefaults.SelectorRadioMarginTop,
                        bottom = CardInstructionPreferenceDefaults.SelectorRadioMarginBottom,
                    ),
                    colors = RadioButtonDefaults.radioButtonColors(),
                )
            }
        }
    }
}

/**
 * One page of a [CardInstructionPreference], the Compose counterpart of
 * `com.coui.appcompat.card.BaseDisplayInfo` (title / summary) as bound by
 * `CardInstructionDescriptionAdapter`.
 *
 * @param title The title of the page (`?couiTextAppearanceHeadline6`).
 * @param summary The summary of the page. Null leaves the line out.
 */
@Stable
data class CardInstructionItem(
    val title: String,
    val summary: String? = null,
)

/** The two device card types, `COUICardInstructionPreference.CARD_INSTRUCTION_TYPE_*`. */
enum class CardInstructionType(
    val value: Int,
) {
    /** `CARD_INSTRUCTION_TYPE_DESCRIPTION` (1): a pager of illustration/title/summary pages. */
    Description(1),

    /** `CARD_INSTRUCTION_TYPE_SELECTOR` (2): a row of selectable illustration/title/radio choices. */
    Selector(2),
}

object CardInstructionPreferenceDefaults {

    /** `coui_component_card_instruction_bg`'s `<corners android:radius>` (16dp). */
    val CornerRadius = 16.dp

    /**
     * `coui_component_card_instruction_preference.xml`'s root padding: `paddingHorizontal` 16dp
     * (`coui_component_card_instruction_margin_horizontal`), `paddingVertical` 24dp.
     */
    val ContentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp)

    /** `coui_component_card_instruction_content_height_complete` (102dp). */
    val ContentHeightComplete = 102.dp

    /**
     * `coui_component_card_instruction_content_height_part` (64dp). The device caps the description
     * block at this height when the page has no title; a [CardInstructionItem] always carries a title
     * here, so this port only ever reaches [ContentHeightComplete].
     */
    val ContentHeightPart = 64.dp

    /**
     * `coui_component_card_instruction_content_fading_edge_length` (40dp), the fading edge the
     * device sets on the description scroll view. Not ported; see [CardInstructionPreference].
     */
    val ContentFadingEdgeLength = 40.dp

    /**
     * `coui_component_card_instruction_divider_width` (24dp): the gap between selector items on
     * `coui_component_card_instruction_selector_page.xml`, and the `setMarginStart` the description's
     * animation row puts before its second animation.
     */
    val DividerWidth = 24.dp

    /** `coui_component_card_instruction_anim_margin_end` (16dp), the animation's trailing margin. */
    val AnimMarginEnd = 16.dp

    /** `coui_component_card_instruction_summary_margin_top_small` (12dp). */
    val SummaryMarginTopSmall = 12.dp

    /** `coui_component_card_instruction_summary_margin_top_large` (24dp). */
    val SummaryMarginTopLarge = 24.dp

    /** The page indicator's `layout_marginTop` (24dp). */
    val IndicatorMarginTop = 24.dp

    /**
     * Spacing between an illustration in [CardInstructionPreference] and the text below it. The
     * device lays the animation out as a sibling in a `ConstraintLayout` with no explicit top margin,
     * and the description's content container carries the 24dp margin; this port uses it as the
     * illustration-to-text gap.
     */
    val AnimMarginTop = 24.dp

    /** The selector item's title `layout_marginTop` (24dp). */
    val SelectorTitleMarginTop = 24.dp

    /** The selector item's radio `layout_marginTop` (4dp). */
    val SelectorRadioMarginTop = 4.dp

    /** The selector item's radio `layout_marginBottom` (24dp). */
    val SelectorRadioMarginBottom = 24.dp

    /** `couiTextAppearanceDescription` (12sp), the description text size. */
    val SummaryTextSize = 12.sp

    /**
     * The default [CardInstructionColors]. [containerColor] follows `?couiColorCardBackground`
     * (`COUITheme.colorScheme.surfaceContainer`); [titleColor] `?couiColorPrimaryNeutral` and
     * [summaryColor] `?couiColorSecondNeutral`.
     */
    @Composable
    fun cardInstructionColors(
        containerColor: Color = COUITheme.colorScheme.surfaceContainer,
        titleColor: Color = COUITheme.colorScheme.onBackground,
        summaryColor: Color = COUITheme.colorScheme.onSurfaceSecondary,
        indicatorActiveColor: Color = COUITheme.colorScheme.onSurface,
        indicatorInactiveColor: Color = COUITheme.colorScheme.disabledOnSurface,
    ): CardInstructionColors = remember(
        containerColor,
        titleColor,
        summaryColor,
        indicatorActiveColor,
        indicatorInactiveColor,
    ) {
        CardInstructionColors(
            containerColor = containerColor,
            titleColor = titleColor,
            summaryColor = summaryColor,
            indicatorActiveColor = indicatorActiveColor,
            indicatorInactiveColor = indicatorInactiveColor,
        )
    }
}

/**
 * Colors used by a [CardInstructionPreference].
 *
 * @param containerColor The card background (`?couiColorCardBackground`).
 * @param titleColor The title color (`?couiColorPrimaryNeutral`).
 * @param summaryColor The summary / selector title color (`?couiColorSecondNeutral`).
 * @param indicatorActiveColor The active dot color (`coui_color_label_primary`).
 * @param indicatorInactiveColor The inactive dot color (`coui_color_hint_neutral`).
 */
@Immutable
data class CardInstructionColors(
    val containerColor: Color,
    val titleColor: Color,
    val summaryColor: Color,
    val indicatorActiveColor: Color,
    val indicatorInactiveColor: Color,
)
