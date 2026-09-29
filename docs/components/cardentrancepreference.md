# CardEntrancePreference

`CardEntrancePreference` is a tappable card that opens another page, mirroring the ColorOS 17 `com.coui.appcompat.card.COUICardEntrancePreference` (itself a `COUIPressFeedbackJumpPreference`). It has two device layouts, switched by `cardType`:

- **Small** — a vertical block: round icon on top, the title 12dp below it, the summary 4dp below the title, on a 12dp-corner background.
- **Large** — a row: round icon at the start, the title and summary 16dp to its end (summary 3dp below the title), on a 16dp-corner background.

The card press feedback reuses the repository's `Card` press tint, which is the COUI card press behaviour.

## Import

```kotlin
import io.github.suqi8.coui.kmp.preference.CardEntrancePreference
import io.github.suqi8.coui.kmp.preference.CardEntrancePreferenceDefaults
import io.github.suqi8.coui.kmp.preference.CardEntranceType
import io.github.suqi8.coui.kmp.preference.CardEntranceTintIcon
```

## Basic Usage

```kotlin
CardEntrancePreference(
    title = "Cloud album",
    cardType = CardEntranceType.Large,
    icon = rememberVectorPainter(COUIIcons.Basic.Cloud),
    summary = "Browse your photos on every device",
    onClick = { /* Open the album */ },
)
```

## Properties

### CardEntrancePreference Properties

| Property  | Type                     | Description                                                             | Default Value                                        | Required |
| --------- | ------------------------ | ----------------------------------------------------------------------- | ---------------------------------------------------- | -------- |
| title     | String                   | Card title (`?couiColorPrimaryNeutral`)                                 | -                                                    | Yes      |
| modifier  | Modifier                 | Modifier applied to the card                                            | Modifier                                             | No       |
| cardType  | CardEntranceType         | Which of the two device layouts to use; see [CardEntranceType](#cardentrancetype) | CardEntranceType.Small                       | No       |
| icon      | Painter?                 | Card icon; null leaves it out                                           | null                                                 | No       |
| summary   | String?                  | Card summary (`?couiColorSecondNeutral`); null leaves the line out      | null                                                 | No       |
| statusOn  | Boolean                  | Repaints the summary with `?couiColorPrimaryText`                       | false                                                | No       |
| tintIcon  | CardEntranceTintIcon     | How the icon is tinted; see [CardEntranceTintIcon](#cardentrancetinticon) | CardEntranceTintIcon.None                          | No       |
| onClick   | (() -> Unit)?            | Callback when the card is clicked; null leaves the card inert           | null                                                 | No       |
| colors    | CardEntranceColors       | The card colors                                                         | CardEntrancePreferenceDefaults.cardEntranceColors()  | No       |

### CardEntranceType

| Value | Device constant    | Layout                                                                 | Corner radius |
| ----- | ------------------ | ---------------------------------------------------------------------- | ------------- |
| Small | `CARD_TYPE_SMALL` (1) | `coui_component_card_entrance_preference_type_small.xml` (icon above text) | 12dp      |
| Large | `CARD_TYPE_LARGE` (2) | `coui_component_card_entrance_preference_type_large.xml` (icon beside text) | 16dp     |

### CardEntranceTintIcon

| Value         | Device constant            | Meaning                                             |
| ------------- | -------------------------- | --------------------------------------------------- |
| None          | `TINT_ICON_NONE` (0)       | Draw the icon untinted                              |
| ByGlobalTheme | `TINT_ICON_BY_GLOBAL_THEME` (1) | Tint with the global COUI theme                |
| Always        | `TINT_ICON_ANYWAY` (2)     | Tint regardless of the global theme flag            |

The device routes both non-`None` modes through `COUIThemeOverlay.applyCOUITintIcon`; this port has a single theme tint, so `ByGlobalTheme` and `Always` resolve to the same colour.

### CardEntrancePreferenceDefaults Object

| Constant            | Type          | Value                                          | Device source                                              |
| ------------------- | ------------- | ---------------------------------------------- | ---------------------------------------------------------- |
| SmallCornerRadius   | Dp            | 12.dp                                          | `coui_component_card_entrance_small_bg` `<corners android:radius>` |
| LargeCornerRadius   | Dp            | 16.dp                                          | `coui_component_card_entrance_large_bg` `<corners android:radius>` |
| SmallContentPadding | PaddingValues | 16.dp all around                               | small layout root `android:padding`                        |
| SmallTitleMarginTop | Dp            | 12.dp                                          | small layout title `android:layout_marginTop`              |
| SmallSummaryMarginTop | Dp          | 4.dp                                           | small layout summary `android:layout_marginTop`            |
| LargeContentPadding | PaddingValues | start 16.dp, end 12.dp, top 20.dp, bottom 20.dp | large layout root padding                                  |
| LargeIconMarginEnd  | Dp            | 16.dp                                          | large layout text block `android:layout_marginStart`       |
| LargeSummaryMarginTop | Dp          | 3.dp                                           | large layout summary `android:layout_marginTop`            |
| IconSize            | Dp            | 32.dp                                          | not a device dimen — see note below                        |
| SummaryTextSize     | TextUnit      | 12.sp                                          | `couiTextAppearanceDescription`                            |

`IconSize` is a port choice: the device leaves the icon `wrap_content` and sizes the `COUIRoundImageView` from its drawable, so there is no `coui_*` dimen for it.

### `cardEntranceColors()` factory

| Parameter          | Type  | Default                                   | COUI role               |
| ------------------ | ----- | ----------------------------------------- | ----------------------- |
| containerColor     | Color | COUITheme.colorScheme.surfaceContainer    | `?couiColorCardBackground` |
| titleColor         | Color | COUITheme.colorScheme.onBackground        | `?couiColorPrimaryNeutral` |
| summaryColor       | Color | COUITheme.colorScheme.onSurfaceSecondary  | `?couiColorSecondNeutral` |
| activeSummaryColor | Color | COUITheme.colorScheme.onBackground        | `?couiColorPrimaryText` |
| iconTint           | Color | COUITheme.colorScheme.onBackground        | theme icon tint         |
| pressedColor       | Color | COUITheme.colorScheme.surfaceContainerHigh | `couiColorCardPressed` |

### CardEntranceColors Class

| Property Name      | Type  | Description                                       |
| ------------------ | ----- | ------------------------------------------------- |
| containerColor     | Color | Card background                                   |
| titleColor         | Color | Title color                                       |
| summaryColor       | Color | Summary color                                     |
| activeSummaryColor | Color | Summary color when the status is on               |
| iconTint           | Color | Icon tint for `ByGlobalTheme` / `Always`          |
| pressedColor       | Color | Fill the card animates towards while pressed      |

## Behavior

- The **Small** layout stacks the icon, the title (12dp below) and the summary (4dp below the title) in a `Column` with 16dp padding all around.
- The **Large** layout places the icon at the start and the title/summary block 16dp to its end, vertically centred; the block takes the remaining width.
- The title uses the COUI `headline2` text style and is capped at two lines; the summary uses `footnote1` at 12sp, also capped at two lines.
- `statusOn = true` repaints the summary with `?couiColorPrimaryText`, mirroring `COUICardEntrancePreference#setStatusOn`.

## Not ported

The device's `COUIPressFeedbackHelper` (press-feedback touch infrastructure) and its separate status line for `showSummary = false` are Android View infrastructure and are not modelled — the press feedback comes from the repository's `Card` press tint, and the summary is simply a [summary](#cardentrancepreference-properties) parameter.

## Advanced Usage

### Small card

```kotlin
CardEntrancePreference(
    title = "Recent",
    cardType = CardEntranceType.Small,
    icon = rememberVectorPainter(COUIIcons.Basic.Recent),
    onClick = { /* Open */ },
)
```

### Tinted icon with an active summary

```kotlin
CardEntrancePreference(
    title = "Backup",
    cardType = CardEntranceType.Large,
    icon = rememberVectorPainter(COUIIcons.Basic.Cloud),
    summary = "Up to date",
    statusOn = true,
    tintIcon = CardEntranceTintIcon.ByGlobalTheme,
    onClick = { /* Open */ },
)
```
