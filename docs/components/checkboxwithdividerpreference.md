# CheckBoxWithDividerPreference

`CheckBoxWithDividerPreference` is a `CheckboxPreference` whose checkbox is separated from the text block by a vertical hairline, mirroring ColorOS's `COUICheckBoxWithDividerPreference`. The device subclass swaps the row for `coui_check_box_divider_preference.xml`: the same text block and checkbox as the plain checkbox row, with the checkbox moved to the tail and a 1dp x 22dp `?couiColorDivider` line before it. The line's inset on both sides works out to the 16dp the device keeps (`preference_divider_margin_end` on the widget side, plus the 8dp spacer `BasicComponent` already reserves).

## Import

```kotlin
import io.github.suqi8.coui.kmp.preference.CheckBoxWithDividerPreference
```

## Basic Usage

Use it where checkboxes form a column beside the row text, for example a list of independent notifications toggles:

```kotlin
CheckBoxWithDividerPreference(
    title = "Show on lock screen",
    checked = lockScreenNotify,
    onCheckedChange = { lockScreenNotify = it }
)
```

## Component States

### Unchecked

```kotlin
CheckBoxWithDividerPreference(
    title = "Unchecked row",
    checked = false,
    onCheckedChange = {}
)
```

### Disabled State

```kotlin
CheckBoxWithDividerPreference(
    title = "Disabled row",
    checked = true,
    onCheckedChange = {},
    enabled = false
)
```

## Properties

### CheckBoxWithDividerPreference Properties

| Property Name    | Type                            | Description                                                       | Default Value                         | Required |
| ---------------- | ------------------------------- | ----------------------------------------------------------------- | ------------------------------------- | -------- |
| title            | String                          | Preference title                                                  | -                                     | Yes      |
| checked          | Boolean                         | Checked state of the checkbox                                     | -                                     | Yes      |
| onCheckedChange  | ((Boolean) -> Unit)?            | Callback when the checked state is changed                        | -                                     | Yes      |
| modifier         | Modifier                        | Component modifier                                                | Modifier                              | No       |
| titleColor       | BasicComponentColors            | Title text color configuration                                    | BasicComponentDefaults.titleColor()   | No       |
| summary          | String?                         | Preference summary                                                | null                                  | No       |
| summaryColor     | BasicComponentColors            | Summary text color configuration                                  | BasicComponentDefaults.summaryColor() | No       |
| checkboxColors   | CheckboxColors                  | Checkbox control color configuration                              | CheckboxDefaults.checkboxColors()     | No       |
| startAction      | @Composable (() -> Unit)?       | Custom start side content                                         | null                                  | No       |
| endActions       | @Composable (RowScope.() -> Unit)? | Custom end side content, placed between the divider and the checkbox | null                              | No       |
| bottomAction     | @Composable (() -> Unit)?       | Custom bottom side content                                        | null                                  | No       |
| insideMargin     | PaddingValues                   | Component internal content padding                                | BasicComponentDefaults.InsideMargin   | No       |
| cardListPosition | CardListPosition                | Row position inside its card group; rounded outer edges gain extra padding | CardListPosition.None           | No       |
| holdDownState    | Boolean                         | Whether the component is held down                                | false                                 | No       |
| enabled          | Boolean                         | Component interactive state                                       | true                                  | No       |
