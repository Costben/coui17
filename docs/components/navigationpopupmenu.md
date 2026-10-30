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

## Glass Variant on the Popup Host

`OverlayNavigationPopupMenu` is the same menu drawn on the in-window popup host instead of a `PopupWindow`: it keeps `NavigationPopupMenuDefaults.PositionProvider`, `MinWidth` and `MaxHeight`, but the surface is the ColorOS 17 popup glass — the blurred backdrop behind the panel, the panel's drop shadow and the material rim — which is what the Gallery's "More" menu on the bottom bar renders. It takes grouped `DropdownEntry` rows rather than a content slot, so a row that owns `children` opens a submenu.

Use it when the menu must sit over live app content (the popup host can sample the backdrop); use `NavigationPopupMenu` when there is no page underneath to blur.

```kotlin
import io.github.suqi8.coui.kmp.overlay.OverlayNavigationPopupMenu

Box {
    IconButton(onClick = { menuExpanded = true }) {
        Icon(imageVector = COUIIcons.More, contentDescription = null)
    }
    OverlayNavigationPopupMenu(
        expanded = menuExpanded,
        entries = listOf(
            DropdownEntry(
                items = listOf(
                    DropdownItem(text = "Search", onClick = { menuExpanded = false }),
                    DropdownItem(text = "Settings", onClick = { menuExpanded = false }),
                ),
            ),
        ),
        onDismissRequest = { menuExpanded = false },
    )
}
```

`alignment` picks which edge of the anchor the menu hugs: the default `BottomEnd` keeps the bar's right-edge convention, and `BottomStart` mirrors it for an anchor on the leading edge. It also decides the corner the card grows out of.

The card opens and closes the way the Gallery's "More" menu does: it springs out of the anchor along a curved path while scaling up from nothing and fading in, each on its own spring, and retraces the same path when it closes.

Closing is not modal: the moment the menu is dismissed, the panel keeps shrinking back into its anchor, but touches already reach the page beneath, so the user can drag, scroll or swipe without waiting for the exit animation to finish. A press that lands on the shrinking panel goes to the page too, and selects no row.

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

### OverlayNavigationPopupMenu Properties

| Property Name    | Type                     | Description                                                    | Default Value                        | Required |
| ---------------- | ------------------------ | -------------------------------------------------------------- | ------------------------------------ | -------- |
| expanded         | Boolean                  | Whether the menu is shown                                      | -                                    | Yes      |
| entries          | `List<DropdownEntry>`    | The rows, grouped like any dropdown entry                       | -                                    | Yes      |
| onDismissRequest | () -> Unit               | Called when the user dismisses the menu                         | -                                    | Yes      |
| modifier         | Modifier                 | Modifier applied to the popup                                   | Modifier                             | No       |
| alignment        | PopupPositionProvider.Align | Which edge of the anchor the menu aligns to                  | `Align.BottomEnd`                    | No       |
| popupPositionProvider | PopupPositionProvider | Position strategy                                               | `NavigationPopupMenuDefaults.PositionProvider` | No |
| minWidth         | Dp                       | Minimum width of the menu                                       | `NavigationPopupMenuDefaults.MinWidth` | No     |
| maxHeight        | Dp?                      | Maximum height; null bounds it by the safe area                 | `NavigationPopupMenuDefaults.MaxHeight` | No     |
| onDismissFinished | (() -> Unit)?           | Called after the exit animation finishes                        | null                                 | No       |
| renderInRootScaffold | Boolean              | Whether to render in the outermost `Scaffold`'s popup host      | true                                 | No       |
| dropdownColors   | DropdownColors           | Colors used by every row                                        | `DropdownDefaults.dropdownColors()`  | No       |
| collapseOnSelection | Boolean               | Whether selecting any leaf dismisses the menu                    | true                                 | No       |

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
