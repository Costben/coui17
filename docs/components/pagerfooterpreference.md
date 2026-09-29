# PagerFooterPreference

`PagerFooterPreference` is the closing note a COUI settings page ends with, mirroring ColorOS's `COUIPagerFooterPreference`. The device class shares `COUIPagerHeaderPreference`'s layout shape — icon, title over summary, empty `widget_frame` — with three differences: `coui_pager_footer_preference.xml` pins the summary to `coui_preference_header_footer_textsize` (12sp), `onBindViewHolder` writes a 24dp bottom margin onto the item unless `withExtraMarginBottom` is false, and it honours the `COUIPreference_couiEnalbeClickSpan` flag by running the summary through `COUIPreferenceUtils.setSummaryView`, which makes its spans tappable.

The click-span flag is why `summary` is an `AnnotatedString`: the repo's `Text` renders one with link styles already, so there is no separate boolean to carry — pass a plain `AnnotatedString(text)` when no span should react.

## Import

```kotlin
import io.github.suqi8.coui.kmp.preference.PagerFooterPreference
```

## Basic Usage

```kotlin
PagerFooterPreference(
    summary = AnnotatedString("This page is for component demonstration only.")
)
```

## Component States

### With a Clickable Span

```kotlin
PagerFooterPreference(
    summary = buildAnnotatedString {
        append("More information in the ")
        withLink(
            LinkAnnotation.Clickable(tag = "legal") { openLegalPage() }
        ) {
            append("legal notice")
        }
        append(".")
    }
)
```

### Without the Extra Bottom Margin

```kotlin
PagerFooterPreference(
    summary = AnnotatedString("Last line of the list"),
    withExtraMarginBottom = false
)
```

## Properties

### PagerFooterPreference Properties

| Property Name        | Type                            | Description                                                       | Default Value                             | Required |
| -------------------- | ------------------------------- | ----------------------------------------------------------------- | ----------------------------------------- | -------- |
| summary              | AnnotatedString                 | Note text, with its clickable spans                               | -                                         | Yes      |
| modifier             | Modifier                        | Component modifier                                                | Modifier                                  | No       |
| title                | String?                         | Optional title                                                    | null                                      | No       |
| startAction          | @Composable (() -> Unit)?       | Custom start side content, in the layout's `@android:id/icon`      | null                                      | No       |
| endActions           | @Composable RowScope.() -> Unit | Custom end side content, in the layout's `widget_frame`            | {}                                        | No       |
| titleColor           | Color                           | Title color (`coui_preference_title_color`, `?couiColorLabelPrimary`) | COUITheme.colorScheme.onBackground     | No       |
| summaryColor         | Color                           | Summary color (`coui_preference_secondary_text_color`, `?couiColorSecondNeutral`) | COUITheme.colorScheme.onBackgroundVariant | No |
| withExtraMarginBottom | Boolean                        | Whether the 24dp bottom margin is applied (COUI `withExtraMarginBottom`) | true                                | No       |

### PagerFooterPreferenceDefaults

| Property     | Type          | Description                                                                       | Value |
| ------------ | ------------- | --------------------------------------------------------------------------------- | ----- |
| InsideMargin | PaddingValues | The same insets the header row uses: 32dp horizontal, 8dp vertical                 | 32dp horizontal, 8dp vertical |
| MarginBottom | Dp            | `support_preference_footer_preference_margin_bottom`                              | 24dp  |
| TextSize     | TextUnit      | `coui_preference_header_footer_textsize`                                          | 12sp  |
