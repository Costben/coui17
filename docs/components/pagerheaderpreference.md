# PagerHeaderPreference

`PagerHeaderPreference` is the explanatory note a COUI settings page opens with, mirroring ColorOS's `COUIPagerHeaderPreference`. The device class adds nothing to `androidx.preference.Preference` but its layout, `coui_pager_header_preference.xml`: a horizontal `LinearLayout` with an optional round icon, a title (`COUIPreferenceTitleStyle`) over a summary (`COUIPreferenceDescriptionStyle`), and an empty `widget_frame`. The row carries 8dp of vertical padding, a 32dp horizontal inset, and a 12dp top margin.

It deliberately does not reuse `BasicComponent`: that component enforces a 48dp minimum height and renders its summary in the 14sp body style, while this row is only as tall as its text and its summary is the 12sp description style.

## Import

```kotlin
import io.github.suqi8.coui.kmp.preference.PagerHeaderPreference
```

## Basic Usage

Place it above the first card of a page:

```kotlin
PagerHeaderPreference(
    summary = "These switches only affect the current device."
)
```

## Component States

### With a Title

```kotlin
PagerHeaderPreference(
    title = "Before you start",
    summary = "Changing these settings may restart the device."
)
```

### With a Start Icon

```kotlin
PagerHeaderPreference(
    summary = "Read-only information, no permission required.",
    startAction = {
        Icon(
            imageVector = COUIIcons.Info,
            contentDescription = null,
            tint = COUITheme.colorScheme.onSurfaceSecondary
        )
    }
)
```

## Properties

### PagerHeaderPreference Properties

| Property Name | Type                            | Description                                                       | Default Value                             | Required |
| ------------- | ------------------------------- | ----------------------------------------------------------------- | ----------------------------------------- | -------- |
| summary       | String                          | Note text                                                         | -                                         | Yes      |
| modifier      | Modifier                        | Component modifier                                                | Modifier                                  | No       |
| title         | String?                         | Optional title                                                    | null                                      | No       |
| startAction   | @Composable (() -> Unit)?       | Custom start side content, in the layout's `@android:id/icon`      | null                                      | No       |
| endActions    | @Composable RowScope.() -> Unit | Custom end side content, in the layout's `widget_frame`            | {}                                        | No       |
| titleColor    | Color                           | Title color (`coui_preference_title_color`, `?couiColorLabelPrimary`) | COUITheme.colorScheme.onBackground     | No       |
| summaryColor  | Color                           | Summary color (`coui_preference_secondary_text_color`, `?couiColorSecondNeutral`) | COUITheme.colorScheme.onBackgroundVariant | No |

### PagerHeaderPreferenceDefaults

| Property    | Type          | Description                                                                       | Value |
| ----------- | ------------- | --------------------------------------------------------------------------------- | ----- |
| InsideMargin | PaddingValues | `support_preference_header_preference_padding_vertical` (8dp) paired with `support_preference_title_padding_start` / `_end` (32dp) | 32dp horizontal, 8dp vertical |
| MarginTop   | Dp            | `support_preference_header_preference_margin_top`                                 | 12dp  |
| TextSize    | TextUnit      | `coui_preference_header_footer_textsize`, the size the summary resolves to         | 12sp  |
