# AppInfoPreference

`AppInfoPreference` is the application header a COUI about page opens with, mirroring the `aboutpage` `COUIAppInfoPreference`. The device class extends `COUIPreference`, and its layout `coui_component_preference_application_info.xml` is a centred `ConstraintLayout`: a 60dp `COUIRoundImageView` with `couiBorderRadius` 16dp and `couiType="round"`, a 42dp top margin above it, then the 20sp application name (`?couiColorPrimaryNeutral`, 8dp below the icon) and the 12sp version (`?couiColorSecondNeutral`, 4dp below the name). The root carries an 8dp bottom margin.

`couiType="round"` routes the icon through `COUIRoundRectUtil.getPath`, which draws `COUIShapePath`'s smooth corner rather than an arc, so this port clips with `SquircleShape` — the repo's port of that same path — and forwards `isSquircleEnabled` through it like every other COUI surface does.

## Import

```kotlin
import io.github.suqi8.coui.kmp.preference.AppInfoPreference
```

## Basic Usage

```kotlin
AppInfoPreference(
    appName = "COUI 17",
    appVersion = "V17.0.0",
    icon = rememberAppIcon()
)
```

## Component States

### Without an Icon

The icon is optional; without it the block starts at the application name, and the 42dp top margin is skipped with it:

```kotlin
AppInfoPreference(
    appName = "COUI 17",
    appVersion = "V17.0.0"
)
```

### Version Long Press

On the device a long press on the version opens a `COUIPopupWindow` holding a copy action that puts the version on the clipboard and toasts a confirmation. That popup is app copy, not preference geometry, so the port stops at a callback:

```kotlin
val clipboard = LocalClipboardManager.current

AppInfoPreference(
    appName = "COUI 17",
    appVersion = "V17.0.0",
    icon = appIcon,
    onVersionLongClick = { clipboard.setText(AnnotatedString("V17.0.0")) }
)
```

## Properties

### AppInfoPreference Properties

| Property Name      | Type              | Description                                                       | Default Value                             | Required |
| ------------------ | ----------------- | ----------------------------------------------------------------- | ----------------------------------------- | -------- |
| appName            | String            | Application name (COUI `appName`)                                 | -                                         | Yes      |
| appVersion         | String            | Version string (COUI `appVersion`)                                | -                                         | Yes      |
| modifier           | Modifier          | Component modifier                                                | Modifier                                  | No       |
| icon               | Painter?          | Application icon (COUI `appIcon`), drawn untinted                 | null                                      | No       |
| onVersionLongClick | (() -> Unit)?     | Callback when the version line is long-pressed; null leaves it inert | null                                   | No       |
| appNameColor       | Color             | Color of the application name (`?couiColorPrimaryNeutral`)        | COUITheme.colorScheme.onBackground        | No       |
| appVersionColor    | Color             | Color of the version (`?couiColorSecondNeutral`)                  | COUITheme.colorScheme.onBackgroundVariant | No       |

### AppInfoPreferenceDefaults

| Property          | Type     | Description                                                                     | Value |
| ----------------- | -------- | ------------------------------------------------------------------------------- | ----- |
| IconSize          | Dp       | `about_app_icon`'s `layout_width` / `layout_height`                             | 60dp  |
| IconCornerRadius  | Dp       | `about_app_icon`'s `couiBorderRadius`                                           | 16dp  |
| IconMarginTop     | Dp       | `about_app_icon`'s `layout_marginTop`                                           | 42dp  |
| NameMarginTop     | Dp       | `about_app_name`'s `layout_marginTop`                                           | 8dp   |
| VersionMarginTop  | Dp       | `about_app_version`'s `layout_marginTop`                                        | 4dp   |
| MarginBottom      | Dp       | The root's `layout_marginBottom`                                                | 8dp   |
| AppNameTextSize   | TextUnit | `about_app_name`'s explicit `android:textSize`, overriding its 16sp title style | 20sp  |
| AppVersionTextSize | TextUnit | `about_app_version`'s explicit `android:textSize`                               | 12sp  |

## Notes

### Deliberate Omissions

- **The copy popup.** The device builds a `COUIPopupWindow` from `coui_component_popup_window_layout.xml`, positions it above the version, copies `appVersion` to the clipboard on tap and toasts `copyFinishText`. None of that is preference geometry or theming information — it is app behaviour the caller already owns, so the port exposes `onVersionLongClick` instead of hard-coding a clipboard write.
- **`copyText` / `copyFinishText` as parameters.** They only exist to fill that popup, so they are omitted along with it.
