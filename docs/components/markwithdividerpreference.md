# MarkWithDividerPreference

`MarkWithDividerPreference` is a `MarkPreference` whose mark is separated from the text block by a vertical hairline, mirroring ColorOS's `COUIMarkWithDividerPreference`. The device subclass swaps the row for `coui_mark_with_divider_preference.xml`: the same text block and tail mark as the plain mark row, with a 1dp x 22dp `?couiColorDivider` line before the mark. The line's inset on both sides works out to the 16dp the device keeps (`preference_divider_margin_end` on the widget side, plus the 8dp spacer `BasicComponent` already reserves).

## Import

```kotlin
import io.github.suqi8.coui.kmp.preference.MarkWithDividerPreference
```

## Basic Usage

Use it where a group of mutually exclusive rows sits together and the marks should read as a separate column — for example the "show notification content" / "hide notification content" pair:

```kotlin
MarkWithDividerPreference(
    title = "Show notification content",
    checked = notifyDetail,
    onClick = { notifyDetail = true }
)
MarkWithDividerPreference(
    title = "Hide notification content",
    checked = !notifyDetail,
    onClick = { notifyDetail = false }
)
```

## Component States

### Unchecked

```kotlin
MarkWithDividerPreference(
    title = "Option B",
    checked = false,
    onClick = {}
)
```

### Disabled State

```kotlin
MarkWithDividerPreference(
    title = "Disabled option",
    checked = true,
    onClick = {},
    enabled = false
)
```

## Properties

### MarkWithDividerPreference Properties

| Property Name    | Type                            | Description                                                       | Default Value                         | Required |
| ---------------- | ------------------------------- | ----------------------------------------------------------------- | ------------------------------------- | -------- |
| title            | String                          | Preference title                                                  | -                                     | Yes      |
| checked          | Boolean                         | Whether this row currently carries the mark                       | -                                     | Yes      |
| onClick          | (() -> Unit)?                   | Callback when the row is clicked                                  | -                                     | Yes      |
| modifier         | Modifier                        | Component modifier                                                | Modifier                              | No       |
| titleColor       | BasicComponentColors            | Title text color configuration                                    | BasicComponentDefaults.titleColor()   | No       |
| summary          | String?                         | Preference summary                                                | null                                  | No       |
| summaryColor     | BasicComponentColors            | Summary text color configuration                                  | BasicComponentDefaults.summaryColor() | No       |
| markColors       | RadioButtonColors               | Mark control color configuration                                  | RadioButtonDefaults.radioButtonColors() | No     |
| startAction      | @Composable (() -> Unit)?       | Custom start side content                                         | null                                  | No       |
| endActions       | @Composable (RowScope.() -> Unit)? | Custom end side content, placed between the divider and the mark | null                                 | No       |
| bottomAction     | @Composable (() -> Unit)?       | Custom bottom side content                                        | null                                  | No       |
| insideMargin     | PaddingValues                   | Component internal content padding                                | BasicComponentDefaults.InsideMargin   | No       |
| cardListPosition | CardListPosition                | Row position inside its card group; rounded outer edges gain extra padding | CardListPosition.None           | No       |
| holdDownState    | Boolean                         | Whether the component is held down                                | false                                 | No       |
| enabled          | Boolean                         | Component interactive state                                       | true                                  | No       |

## Notes

### Deliberate Omissions

- **The split click target.** `COUIMarkWithDividerPreference` divides the row into two hit areas — `main_layout` fires `OnMainLayoutClickListener` and `radio_layout` toggles the mark. Neither the repo's `MarkPreference` nor this wrapper models that split; the whole row keeps a single click target.
