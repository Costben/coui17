# CardInstructionPreference

`CardInstructionPreference` is the COUI card instruction block: a card that walks through a settings feature with an illustration, a title and a description, mirroring the ColorOS 17 `com.coui.appcompat.card.COUICardInstructionPreference` (a `COUIPreference` inflating `coui_component_card_instruction_preference.xml`). The device switches its pager adapter on `cardType`:

- **Description** — a `ViewPager2` of illustration / title / summary pages, with a `COUIPageIndicator` 24dp below.
- **Selector** — a row of selectable illustration / title / radio choices, split evenly and separated by a 24dp divider.

The animation views (`com.oplus.anim.EffectiveAnimationView`) are replaced by the caller-supplied `illustration` slot.

## Import

```kotlin
import io.github.suqi8.coui.kmp.preference.CardInstructionItem
import io.github.suqi8.coui.kmp.preference.CardInstructionPreference
import io.github.suqi8.coui.kmp.preference.CardInstructionPreferenceDefaults
import io.github.suqi8.coui.kmp.preference.CardInstructionType
```

## Basic Usage

```kotlin
CardInstructionPreference(
    items = listOf(
        CardInstructionItem("Smart sorting", "Groups photos by people, place and time"),
        CardInstructionItem("Cloud sync", "Continue browsing on your other devices"),
    ),
    cardType = CardInstructionType.Description,
    illustration = { index -> Illustration(index) },
    onSelectedIndexChange = { /* Track the active page */ },
)
```

## Properties

### CardInstructionPreference Properties

| Property              | Type                              | Description                                                          | Default Value                                              | Required |
| --------------------- | --------------------------------- | -------------------------------------------------------------------- | ---------------------------------------------------------- | -------- |
| items                 | `List<CardInstructionItem>`         | The pages (Description) or selector choices (Selector)               | -                                                          | Yes      |
| modifier              | Modifier                          | Modifier applied to the card                                         | Modifier                                                   | No       |
| cardType              | CardInstructionType               | Which of the two device adapters to use; see [CardInstructionType](#cardinstructiontype) | CardInstructionType.Description       | No       |
| selectedIndex         | Int                               | The active page or selected choice                                   | 0                                                          | No       |
| onSelectedIndexChange | (Int) -> Unit                     | Callback when the active page changes                                | {}                                                         | No       |
| colors                | CardInstructionColors             | The card colors                                                      | CardInstructionPreferenceDefaults.cardInstructionColors()  | No       |
| illustration          | (@Composable (index: Int) -> Unit)? | The illustration of the item at the given index; null leaves it out | null                                                       | No       |

### CardInstructionItem

| Property | Type    | Description                                          | Required |
| -------- | ------- | ---------------------------------------------------- | -------- |
| title    | String  | The title of the page (`?couiTextAppearanceHeadline6`) | Yes   |
| summary  | String? | The summary of the page; null leaves the line out    | No       |

### CardInstructionType

| Value       | Device constant                     | Layout                                                                |
| ----------- | ----------------------------------- | --------------------------------------------------------------------- |
| Description | `CARD_INSTRUCTION_TYPE_DESCRIPTION` (1) | A pager of illustration / title / summary pages (`HorizontalPager` + `PageIndicator`) |
| Selector    | `CARD_INSTRUCTION_TYPE_SELECTOR` (2)    | A row of selectable illustration / title / radio choices              |

### CardInstructionPreferenceDefaults Object

| Constant                | Type          | Value                                       | Device source                                                     |
| ----------------------- | ------------- | ------------------------------------------- | ----------------------------------------------------------------- |
| CornerRadius            | Dp            | 16.dp                                       | `coui_component_card_instruction_bg` `<corners android:radius>`   |
| ContentPadding          | PaddingValues | horizontal 16.dp, vertical 24.dp            | `coui_component_card_instruction_preference.xml` root padding     |
| ContentHeightComplete   | Dp            | 102.dp                                      | `coui_component_card_instruction_content_height_complete`         |
| ContentHeightPart       | Dp            | 64.dp                                       | `coui_component_card_instruction_content_height_part`             |
| ContentFadingEdgeLength | Dp            | 40.dp                                       | `coui_component_card_instruction_content_fading_edge_length` — not ported |
| DividerWidth            | Dp            | 24.dp                                       | `coui_component_card_instruction_divider_width`                   |
| AnimMarginEnd           | Dp            | 16.dp                                       | `coui_component_card_instruction_anim_margin_end`                 |
| SummaryMarginTopSmall   | Dp            | 12.dp                                       | `coui_component_card_instruction_summary_margin_top_small`        |
| SummaryMarginTopLarge   | Dp            | 24.dp                                       | `coui_component_card_instruction_summary_margin_top_large`        |
| IndicatorMarginTop      | Dp            | 24.dp                                       | the page indicator's `layout_marginTop`                           |
| AnimMarginTop           | Dp            | 24.dp                                       | description content container margin, used as the illustration-to-text gap |
| SelectorTitleMarginTop  | Dp            | 24.dp                                       | selector item title `layout_marginTop`                            |
| SelectorRadioMarginTop  | Dp            | 4.dp                                        | selector item radio `layout_marginTop`                            |
| SelectorRadioMarginBottom | Dp          | 24.dp                                       | selector item radio `layout_marginBottom`                         |
| SummaryTextSize         | TextUnit      | 12.sp                                       | `couiTextAppearanceDescription`                                   |

`ContentHeightPart` is the height the device caps the description block at when a page has no title; a `CardInstructionItem` always carries a title here, so this port only ever reaches `ContentHeightComplete`.

### `cardInstructionColors()` factory

| Parameter              | Type  | Default                                    | COUI role                    |
| ---------------------- | ----- | ------------------------------------------ | ---------------------------- |
| containerColor         | Color | COUITheme.colorScheme.surfaceContainer     | `?couiColorCardBackground`   |
| titleColor             | Color | COUITheme.colorScheme.onBackground         | `?couiColorPrimaryNeutral`   |
| summaryColor           | Color | COUITheme.colorScheme.onSurfaceSecondary   | `?couiColorSecondNeutral`    |
| indicatorActiveColor   | Color | COUITheme.colorScheme.onSurface            | `coui_color_label_primary`   |
| indicatorInactiveColor | Color | COUITheme.colorScheme.disabledOnSurface    | `coui_color_hint_neutral`    |

### CardInstructionColors Class

| Property Name          | Type  | Description                          |
| ---------------------- | ----- | ------------------------------------ |
| containerColor         | Color | Card background                      |
| titleColor             | Color | Title color                          |
| summaryColor           | Color | Summary / selector title color       |
| indicatorActiveColor   | Color | Active dot color                     |
| indicatorInactiveColor | Color | Inactive dot color                   |

## Behavior

- **Description** renders a `HorizontalPager`; the page indicator is shown only when there is more than one item. `selectedIndex` drives the pager, and the pager reports changes back through `onSelectedIndexChange`.
- Each description page is centred: the title uses the COUI `headline2` text style (centred, at most two lines) and the summary `footnote1` at 12sp, 12dp below the title. The text block is capped at 102dp and scrolls when it overflows.
- **Selector** lays its items out in a `Row`, each with `weight = 1f`, separated by a 24dp gap. Each item is a clickable `Column` (role `RadioButton`) with the illustration, the title 24dp below (`button` text style) and a 24dp `RadioButton` (4dp above, 24dp below).
- When `items` is empty the composable renders nothing.

## Not ported

Because the device class is Android View infrastructure, the `ViewPager2`, the `COUIPageIndicator` view (the repository's `basic/PageIndicator.kt` stands in), the `RecyclerView.Adapter` hierarchy (`BaseCardInstructionAdapter`, `CardInstructionDescriptionAdapter`, `CardInstructionSelectorAdapter`) and the `COUIMutableSizeScrollView` are not ported. The 40dp content fading edge is not ported either — the description block is simply capped at `ContentHeightComplete`.

## Advanced Usage

### Selector card

```kotlin
CardInstructionPreference(
    items = listOf(
        CardInstructionItem("Daily"),
        CardInstructionItem("Weekly"),
        CardInstructionItem("Monthly"),
    ),
    cardType = CardInstructionType.Selector,
    selectedIndex = period,
    onSelectedIndexChange = { period = it },
    illustration = { index -> PeriodIcon(index) },
)
```

### Custom colors

```kotlin
CardInstructionPreference(
    items = instructions,
    colors = CardInstructionPreferenceDefaults.cardInstructionColors(
        containerColor = COUITheme.colorScheme.surfaceContainerHigh,
    ),
)
```
