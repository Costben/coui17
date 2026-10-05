# BottomFloatingEditToolbar

`BottomFloatingEditToolbar` is the ColorOS 17 floating edit toolbar (`COUIBottomFloatingEditToolbar`). It is a capsule of `BottomFloatingEditToolbarItem`s that slides in from below the screen edge and back out again, on the native show / hide spring.

It is the toolbar sibling of `BottomFloatingNavigationBar`: both are floating glass capsules, but the edit toolbar arranges its items differently and is shorter on a wide screen. The capsule samples the content behind it through `LocalBackdrop`; without a backdrop it keeps an opaque fill, and its corner radius is the menu height halved.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.BottomFloatingEditToolbar
import io.github.suqi8.coui.kmp.basic.BottomFloatingEditToolbarItem
import io.github.suqi8.coui.kmp.basic.BottomFloatingEditToolbarDefaults
import io.github.suqi8.coui.kmp.basic.BottomFloatingEditToolbarStyle
import io.github.suqi8.coui.kmp.basic.LocalBottomFloatingEditToolbarStyle
import io.github.suqi8.coui.kmp.basic.BottomFloatingRedDot
```

## Basic Usage

```kotlin
var visible by remember { mutableStateOf(true) }

BottomFloatingEditToolbar(
    visible = visible,
) {
    BottomFloatingEditToolbarItem(
        onClick = { /* copy */ },
        icon = COUIIcons.Copy,
        label = "Copy",
    )
    BottomFloatingEditToolbarItem(
        onClick = { /* delete */ },
        icon = COUIIcons.Delete,
        label = "Delete",
        redDot = BottomFloatingRedDot.Count(3),
    )
    BottomFloatingEditToolbarItem(
        onClick = { /* share */ },
        icon = COUIIcons.Share,
        label = "Share",
        enabled = false,
    )
}
```

## Showing and Hiding

`visible` drives the slide motion: the toolbar is translated down by the distance that puts the capsule just past the bottom edge and back to zero. The toolbar keeps its 88dp minimum outer height while hidden, so the layout does not jump:

```kotlin
BottomFloatingEditToolbar(visible = isEditing) {
    // ... items ...
}
```

## Item Layout

The item arrangement follows the window tier. On a small screen the toolbar uses the vertical layout — icon above a one-line 10sp title — while on a medium or large screen it switches to the horizontal layout, where a 14sp title sits next to the icon and may run to two lines. The toolbar publishes its choice to the items through `LocalBottomFloatingEditToolbarStyle`, which defaults to `BottomFloatingEditToolbarStyle.Vertical`; items read it themselves, so they take the right shape without extra wiring.

## Properties

### BottomFloatingEditToolbar Properties

| Property Name  | Type                   | Description                                                    | Default Value             | Required |
| -------------- | ---------------------- | -------------------------------------------------------------- | ------------------------- | -------- |
| modifier       | Modifier               | Modifier applied to the toolbar                                | Modifier                  | No       |
| containerColor | Color                  | Background color of the capsule                                | COUITheme.colorScheme.bar | No       |
| backdrop       | Backdrop?              | The content blurred behind the capsule; `null` keeps it opaque | LocalBackdrop.current     | No       |
| visible        | Boolean                | Whether the toolbar is on screen                               | true                      | No       |
| content        | @Composable () -> Unit  | The content of the toolbar, usually `BottomFloatingEditToolbarItem`s | -                   | Yes      |

### BottomFloatingEditToolbarItem Properties

| Property Name | Type                | Description                                        | Default Value | Required |
| ------------- | ------------------- | -------------------------------------------------- | ------------- | -------- |
| onClick       | () -> Unit          | Called when the item is activated                  | -             | Yes      |
| icon          | ImageVector         | The icon of the item                               | -             | Yes      |
| label         | String              | The title of the item                              | -             | Yes      |
| modifier      | Modifier            | Modifier applied to the item                       | Modifier      | No       |
| enabled       | Boolean             | Whether the item can be activated                  | true          | No       |
| redDot        | BottomFloatingRedDot? | The red dot pinned to the icon's top end corner  | null          | No       |

### BottomFloatingEditToolbarStyle Enum

| Value      | Description                                                        |
| ---------- | ------------------------------------------------------------------ |
| Horizontal | Wide layout: icon and a two-line title side by side                |
| Vertical   | Narrow layout: icon above a one-line title                         |

### BottomFloatingEditToolbarDefaults Object

| Constant Name                   | Type       | Description                                                              | Value                                   |
| ------------------------------- | ---------- | ------------------------------------------------------------------------ | --------------------------------------- |
| MenuHeightSmall                 | Dp         | The capsule height on a small screen                                     | 56.dp                                   |
| MenuHeightMediumLarge           | Dp         | The capsule height on a medium or large screen                           | 44.dp                                   |
| MinHeightWithoutPaddingVertical | Dp         | The minimum outer height                                                 | 88.dp                                   |
| PaddingHorizontalSmallScreen    | Dp         | The outside padding on a small screen                                    | 16.dp                                   |
| PaddingHorizontalMediumScreen   | Dp         | The outside padding on a medium screen                                   | 24.dp                                   |
| PaddingHorizontalLargeScreen    | Dp         | The outside padding on a large screen                                    | 40.dp                                   |
| InnerPaddingHorizontal          | Dp         | The padding between the capsule's end edges and the first and last item  | 10.dp                                   |
| InnerPaddingVertical            | Dp         | The same padding in the narrow layout                                    | 12.dp                                   |
| ItemHeightHorizontal            | Dp         | The item height in the wide layout                                       | 40.dp                                   |
| ItemHeightVertical              | Dp         | The item height in the narrow layout                                     | 48.dp                                   |
| ItemMaxWidthHorizontal          | Dp         | The per-item width bound in the wide layout                              | 98.dp                                   |
| ItemMaxWidthVertical            | Dp         | The per-item width bound in the narrow layout                            | 70.dp                                   |
| SingleItemWidthHorizontal       | Dp         | The width a lone item takes in the wide layout                           | 152.dp                                  |
| SingleItemWidthVertical         | Dp         | The width a lone item takes in the narrow layout                         | 148.dp                                  |
| ItemSpacing                     | Dp         | The spacing between items                                                | 8.dp                                    |
| IconSize                        | Dp         | The icon size                                                            | 24.dp                                   |
| IconTopMargin                   | Dp         | The top margin of the icon in the narrow layout                          | 4.dp                                    |
| IconTitleGapVertical            | Dp         | The vertical gap between icon and title in the narrow layout             | 2.dp                                    |
| IconTitleGapHorizontal          | Dp         | The horizontal gap between icon and title in the wide layout             | 4.dp                                    |
| LabelFontSizeHorizontal         | TextUnit   | The title size in the wide layout                                        | 14.sp                                   |
| LabelFontSizeVertical           | TextUnit   | The title size in the narrow layout                                      | 10.sp                                   |
| TranslationYSpring              | `SpringSpec<Float>` | The show / hide spring                                            | response 0.38 / bounce 0.25             |
