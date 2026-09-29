# SwitchWithDividerPreference

`SwitchWithDividerPreference` is a `SwitchPreference` whose switch is separated from the text block by a vertical hairline, mirroring ColorOS's `COUISwitchWithDividerPreference`. The device subclass swaps the row for `coui_switch_with_divider_preference.xml`: the same text block and tail switch as the plain switch row, with a 1dp x 22dp `?couiColorDivider` line before it. The line's inset on both sides works out to the 16dp the device keeps (`android_preference_switch_margin_left` on the widget side, plus the 8dp spacer `BasicComponent` already reserves).

## Import

```kotlin
import io.github.suqi8.coui.kmp.preference.SwitchWithDividerPreference
```

## Basic Usage

Use it where switches form a column beside the row text, for example a group of notification switches:

```kotlin
SwitchWithDividerPreference(
    title = "Allow notifications",
    checked = notifyEnabled,
    onCheckedChange = { notifyEnabled = it }
)
```

## Component States

### Off

```kotlin
SwitchWithDividerPreference(
    title = "Off row",
    checked = false,
    onCheckedChange = {}
)
```

### Disabled State

```kotlin
SwitchWithDividerPreference(
    title = "Disabled row",
    checked = true,
    onCheckedChange = {},
    enabled = false
)
```

## Properties

### SwitchWithDividerPreference Properties

| Property Name    | Type                            | Description                                                       | Default Value                         | Required |
| ---------------- | ------------------------------- | ----------------------------------------------------------------- | ------------------------------------- | -------- |
| checked          | Boolean                         | Switch checked state                                              | -                                     | Yes      |
| onCheckedChange  | (Boolean) -> Unit               | Switch state change callback                                      | -                                     | Yes      |
| title            | String                          | Preference title                                                  | -                                     | Yes      |
| modifier         | Modifier                        | Component modifier                                                | Modifier                              | No       |
| titleColor       | BasicComponentColors            | Title text color configuration                                    | BasicComponentDefaults.titleColor()   | No       |
| summary          | String?                         | Preference summary                                                | null                                  | No       |
| summaryColor     | BasicComponentColors            | Summary text color configuration                                  | BasicComponentDefaults.summaryColor() | No       |
| startAction      | @Composable (() -> Unit)?       | Custom start side content                                         | null                                  | No       |
| endActions       | @Composable (RowScope.() -> Unit)? | Custom end side content, placed between the divider and the switch | null                               | No       |
| bottomAction     | @Composable (() -> Unit)?       | Custom bottom side content                                        | null                                  | No       |
| switchColors     | SwitchColors                    | Switch control color configuration                                | SwitchDefaults.switchColors()         | No       |
| insideMargin     | PaddingValues                   | Component internal content padding                                | BasicComponentDefaults.InsideMargin   | No       |
| cardListPosition | CardListPosition                | Row position inside its card group; rounded outer edges gain extra padding | CardListPosition.None           | No       |
| holdDownState    | Boolean                         | Whether the component is held down                                | false                                 | No       |
| enabled          | Boolean                         | Component interactive state                                       | true                                  | No       |
