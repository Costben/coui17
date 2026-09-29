# StatusSwitchingPreference

`StatusSwitchingPreference` is a preference row whose trailing widget switches between a status text and an icon, mirroring ColorOS's `COUIStatusSwitchingPreference`. The device inflates `coui_preference_widget_status_switching.xml` into the standard row's `widget_frame`: a `FrameLayout` holding a `COUITextView` (`couiTextAppearanceBody`, `?couiColorLabelSecondary`), an `ImageView`, and an `EffectiveAnimationView`, of which `showText` / `showIcon` / `showAnim` make exactly one visible.

The animation branch (`STATUS_TYPE_ANIM`, a Lottie `raw` resource played on a loop) is intentionally not modelled: the class stores a raw resource id rather than a drawable, so there is nothing to hand a Compose caller, and the repo carries no animation runtime. Only `StatusSwitchingContent.Text` and `StatusSwitchingContent.Icon` exist, mirroring `STATUS_TYPE_TEXT` and `STATUS_TYPE_IMAGE`.

## Import

```kotlin
import io.github.suqi8.coui.kmp.preference.StatusSwitchingPreference
import io.github.suqi8.coui.kmp.preference.StatusSwitchingContent
```

## Basic Usage

```kotlin
StatusSwitchingPreference(
    title = "Battery level",
    status = StatusSwitchingContent.Text("About 9 h 12 min left"),
    onClick = { /* open battery details */ }
)
```

## Component States

### Text Status

```kotlin
StatusSwitchingPreference(
    title = "Bluetooth",
    status = StatusSwitchingContent.Text("Connected")
)
```

### Icon Status

```kotlin
StatusSwitchingPreference(
    title = "Location",
    status = StatusSwitchingContent.Icon(
        imageVector = COUIIcons.Info,
        contentDescription = "Location is on",
        tint = COUITheme.colorScheme.primary
    )
)
```

### Disabled State

```kotlin
StatusSwitchingPreference(
    title = "Disabled row",
    status = StatusSwitchingContent.Text("Unavailable"),
    enabled = false
)
```

## Properties

### StatusSwitchingPreference Properties

| Property Name    | Type                            | Description                                                       | Default Value                         | Required |
| ---------------- | ------------------------------- | ----------------------------------------------------------------- | ------------------------------------- | -------- |
| title            | String                          | Preference title                                                  | -                                     | Yes      |
| status           | StatusSwitchingContent          | Trailing widget content: `Text` or `Icon`                         | -                                     | Yes      |
| modifier         | Modifier                        | Component modifier                                                | Modifier                              | No       |
| titleColor       | BasicComponentColors            | Title text color configuration                                    | BasicComponentDefaults.titleColor()   | No       |
| summary          | String?                         | Preference summary                                                | null                                  | No       |
| summaryColor     | BasicComponentColors            | Summary text color configuration                                  | BasicComponentDefaults.summaryColor() | No       |
| startAction      | @Composable (() -> Unit)?       | Custom start side content                                         | null                                  | No       |
| statusColor      | Color                           | Color of the status text (COUI `?couiColorLabelSecondary`)        | COUITheme.colorScheme.onSurfaceSecondary | No    |
| bottomAction     | @Composable (() -> Unit)?       | Custom bottom side content                                        | null                                  | No       |
| insideMargin     | PaddingValues                   | Component internal content padding                                | BasicComponentDefaults.InsideMargin   | No       |
| cardListPosition | CardListPosition                | Row position inside its card group; rounded outer edges gain extra padding | CardListPosition.None           | No       |
| holdDownState    | Boolean                         | Whether the component is held down                                | false                                 | No       |
| enabled          | Boolean                         | Component interactive state                                       | true                                  | No       |
| onClick          | (() -> Unit)?                   | Callback when the row is clicked                                  | null                                  | No       |

### StatusSwitchingContent

| Variant | Properties                                                   | Device branch   |
| ------- | ------------------------------------------------------------ | --------------- |
| Text    | text: String                                                 | `STATUS_TYPE_TEXT` (2)  |
| Icon    | imageVector: ImageVector, contentDescription: String?, tint: Color | `STATUS_TYPE_IMAGE` (3) |

### StatusSwitchingPreferenceDefaults

| Property         | Type | Description                                                                  | Value |
| ---------------- | ---- | ---------------------------------------------------------------------------- | ----- |
| WidgetMarginStart | Dp  | Start margin of the widget frame, from `coui_status_switching_preference_widget_margin_start` | 16dp |

## Notes

### Deliberate Omissions

- **`STATUS_TYPE_ANIM`.** The device's `EffectiveAnimationView` plays a Lottie animation from a `raw` resource. The repo has no animation runtime, and the class stores only the resource id, so there is no drawable to expose. Documented here rather than approximated with a substitute spinner.
