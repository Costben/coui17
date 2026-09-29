# NavigationPopupMenu

`NavigationPopupMenu` is the COUI bottom-navigation overflow menu (`COUINavigationPopupMenu`). It shows the navigation items that do not fit in the bar, as a window-level popup anchored above the anchor and aligned to its end edge.

The menu opens **above** the bar with a 16dp horizontal margin and an 8dp vertical margin, and draws the same 28dp smooth-corner glass surface the library's other popups use. Rows sit flush against each other with no divider band, and each row's press state is the COUI popup list selector.

Compose it inside the composable it should anchor to — the anchor is the bounds of the popup's parent layout node. `BottomFloatingNavigationBarItem` uses it for its `overflowMenu` slot: the slot's rows are shown in a `NavigationPopupMenu` anchored to that item on a long press.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.NavigationPopupMenu
import io.github.suqi8.coui.kmp.basic.NavigationPopupMenuItem
import io.github.suqi8.coui.kmp.basic.NavigationPopupMenuDefaults
```

## Basic Usage

```kotlin
var menuExpanded by remember { mutableStateOf(false) }

Box {
    TextButton(
        text = "Show menu",
        onClick = { menuExpanded = true },
    )
    NavigationPopupMenu(
        expanded = menuExpanded,
        onDismissRequest = { menuExpanded = false },
    ) {
        NavigationPopupMenuItem(
            label = "Search",
            icon = COUIIcons.Search,
            onClick = { menuExpanded = false },
        )
        NavigationPopupMenuItem(
            label = "Settings",
            icon = COUIIcons.Settings,
            onClick = { menuExpanded = false },
        )
        NavigationPopupMenuItem(
            label = "About",
            icon = COUIIcons.More,
            enabled = false,
            onClick = {},
        )
    }
}
```

## Using It as a Navigation Bar Overflow Menu

`BottomFloatingNavigationBarItem` takes an `overflowMenu` slot; filling it enables the long-press menu, and `null` disables the long press:

```kotlin
BottomFloatingNavigationBarItem(
    selected = selectedIndex == 0,
    onClick = { selectedIndex = 0 },
    icon = COUIIcons.Home,
    label = "Home",
    overflowMenu = {
        NavigationPopupMenuItem(
            label = "Search",
            icon = COUIIcons.Search,
            onClick = { /* ... */ },
        )
    },
)
```

## Properties

### NavigationPopupMenu Properties

| Property Name    | Type                   | Description                                              | Default Value | Required |
| ---------------- | ---------------------- | -------------------------------------------------------- | ------------- | -------- |
| expanded         | Boolean                | Whether the menu is shown                                | -             | Yes      |
| onDismissRequest | () -> Unit             | Called when the user dismisses the menu (outside tap or back) | -        | Yes      |
| modifier         | Modifier               | Modifier applied to the popup                            | Modifier      | No       |
| content          | @Composable () -> Unit  | The rows, usually `NavigationPopupMenuItem`s             | -             | Yes      |

### NavigationPopupMenuItem Properties

| Property Name | Type        | Description                    | Default Value | Required |
| ------------- | ----------- | ------------------------------ | ------------- | -------- |
| label         | String      | The row title                  | -             | Yes      |
| icon          | ImageVector | The row icon                   | -             | Yes      |
| onClick       | () -> Unit  | Called when the row is activated | -           | Yes      |
| modifier      | Modifier    | Modifier applied to the row    | Modifier      | No       |
| enabled       | Boolean     | Whether the row can be activated | true        | No       |

### NavigationPopupMenuDefaults Object

| Constant Name    | Type                  | Description                                                                        | Value      |
| ---------------- | --------------------- | ---------------------------------------------------------------------------------- | ---------- |
| MinWidth         | Dp                    | The minimum row width                                                              | 208.dp     |
| MaxHeight        | Dp                    | The maximum menu height                                                            | 324.dp     |
| HorizontalMargin | Dp                    | The margin between the menu and the bar's end edge, and the inset of a row's icon and label | 16.dp |
| VerticalMargin   | Dp                    | The margin between the menu and the bar                                            | 8.dp       |
| TextSize         | TextUnit              | The row title size                                                                 | 16.sp      |
| ItemPaddingVertical | Dp                 | The row's top and bottom padding                                                   | 12.dp      |
| IconSize         | Dp                    | The row icon size                                                                  | 24.dp      |
| ItemUsedSpace    | Dp                    | The space reserved for a row's icon and margins when the menu measures its width    | 72.dp      |
| PositionProvider | PopupPositionProvider | Places the menu above the anchor, aligned to the anchor's end edge                 | -          |

## Placement

`NavigationPopupMenuDefaults.PositionProvider` reproduces `COUINavigationPopupMenu.tryShow()`: the menu opens above the anchor, aligns to the anchor's end edge, keeps the horizontal and vertical margins, and is clamped to the window so it never runs past the screen edges. Because the menu is anchored to the popup's parent layout node, the composable you wrap the menu in determines where it appears.
