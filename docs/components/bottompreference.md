# BottomPreference

`BottomPreference` is the invisible bottom spacer a COUI settings screen ends with, mirroring ColorOS's `COUIBottomPreference`. It is the one preference that draws nothing: the device inflates `coui_preference_bottom.xml`, an invisible `FrameLayout` whose only content is `android:layout_height="@dimen/support_preference_foot_preference_padding_bottom"`, and `onBindViewHolder` copies `mPlaceholderHeight` onto the item's layout params. The `couiPlaceholderHeight` attribute overrides the height.

## Import

```kotlin
import io.github.suqi8.coui.kmp.preference.BottomPreference
```

## Basic Usage

Place it as the last item of the settings list so the final card does not sit flush against the bottom edge:

```kotlin
LazyColumn {
    // ... cards ...
    item { BottomPreference() }
}
```

## Component States

### Custom Height

```kotlin
BottomPreference(height = 48.dp)
```

## Properties

### BottomPreference Properties

| Property Name | Type     | Description                                            | Default Value              | Required |
| ------------- | -------- | ------------------------------------------------------ | -------------------------- | -------- |
| modifier      | Modifier | Component modifier                                     | Modifier                   | No       |
| height        | Dp       | Placeholder height (COUI `couiPlaceholderHeight`)      | BottomPreferenceDefaults.Height | No  |

### BottomPreferenceDefaults

| Property | Type | Description                                                                        | Value |
| -------- | ---- | ---------------------------------------------------------------------------------- | ----- |
| Height   | Dp   | `support_preference_foot_preference_padding_bottom`, aliasing `coui_list_to_ex_bottom_padding` | 32dp |
