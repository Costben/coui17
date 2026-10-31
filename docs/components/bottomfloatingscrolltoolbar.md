# BottomFloatingScrollToolbar

`BottomFloatingScrollToolbar` is the horizontally scrolling main menu pill of the ColorOS 17 Photos editor (`picture3d_editor_preview_toolbar`). Each `BottomFloatingScrollToolbarItem` stacks a 24dp icon over a compact 8dp one-line label; when the items exceed the pill width, the row scrolls horizontally with overscroll and fades out over 50dp on whichever edge still has items beyond it.

It is the labelled, scrolling counterpart to `BottomFloatingToolbar`: the outer pill responds to touch with `couiTouchMotion` (deformation, rubber band, press scale, and `TYPE_CAPSULE_1` white spotlight) while leaving horizontal drags to the scrolling list inside.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.BottomFloatingScrollToolbar
import io.github.suqi8.coui.kmp.basic.BottomFloatingScrollToolbarItem
import io.github.suqi8.coui.kmp.basic.BottomFloatingScrollToolbarColors
import io.github.suqi8.coui.kmp.basic.BottomFloatingScrollToolbarDefaults
```

## Basic Usage

```kotlin
var selected by remember { mutableIntStateOf(0) }

BottomFloatingScrollToolbar {
    tools.forEachIndexed { index, tool ->
        BottomFloatingScrollToolbarItem(
            onClick = { selected = index },
            icon = tool.icon,
            label = tool.label,
            selected = index == selected,
        )
    }
}
```

## Scrolling, Edge Fade and Neighbour Reveal

- **Pill sizing**: The pill wraps its items and centres itself within the available width, capped at `460.dp` with `16.dp` horizontal margins on either side.
- **Edge fade**: When `scrollState` can scroll further toward an edge, the clipped items fade to transparent over `50.dp` (`base_editor_fading_edge_width`), clipped inside the 28dp stadium outline.
- **Neighbour reveal**: Tapping an item near either edge smoothly scrolls the adjacent item on that side into view (`298ms`, cubic deceleration), matching `EditorLinearListView` so the next tool is reachable with a single tap.

## Pill Surface and Colors

Because the ColorOS Photos editor runs on a fixed dark canvas, `BottomFloatingScrollToolbarDefaults.colors()` defaults to the editor's dark palette (`#232323` fill, `0.67.dp` `#0DFFFFFF` hairline under `TYPE_FRAMEWORK_CAPSULE_13` material rim, `#73FFFFFF` idle items, and `#FFB200` selected items). Pass a custom `BottomFloatingScrollToolbarColors` when adapting the bar to another surface.

The pill's fill, border, clip and touch highlight share one `CapsuleOutlineShape` and follow the system capsule curve (`addSegmentCapsule`); the material rim uses the capsule corner weight (1.0 for a wide pill, 2.0 for a square one).

## Properties

### BottomFloatingScrollToolbar Properties

| Property Name | Type                               | Description                                      | Default Value                                  | Required |
| ------------- | ---------------------------------- | ------------------------------------------------ | ---------------------------------------------- | -------- |
| modifier      | Modifier                           | Modifier applied to the toolbar                  | Modifier                                       | No       |
| colors        | BottomFloatingScrollToolbarColors  | Colors of the pill and its items                 | BottomFloatingScrollToolbarDefaults.colors()   | No       |
| scrollState   | ScrollState                        | Horizontal scroll state of the item row          | rememberScrollState()                          | No       |
| content       | @Composable () -> Unit             | Content of the toolbar, usually items            | -                                              | Yes      |

### BottomFloatingScrollToolbarItem Properties

| Property Name | Type        | Description                              | Default Value | Required |
| ------------- | ----------- | ---------------------------------------- | ------------- | -------- |
| onClick       | () -> Unit  | Called when the item is activated        | -             | Yes      |
| icon          | ImageVector | The icon of the item                     | -             | Yes      |
| label         | String      | The one-line label below the icon        | -             | Yes      |
| modifier      | Modifier    | Modifier applied to the item             | Modifier      | No       |
| selected      | Boolean     | Whether this item is the active tool     | false         | No       |
| enabled       | Boolean     | Whether the item can be activated        | true          | No       |

### BottomFloatingScrollToolbarColors

| Property Name        | Type  | Description                                       | Default Value   |
| -------------------- | ----- | ------------------------------------------------- | --------------- |
| containerColor       | Color | Fill color of the pill                            | `#FF232323`     |
| strokeColor          | Color | Hairline color under the material rim             | `#0DFFFFFF`     |
| contentColor         | Color | Icon and label color of an idle item              | `#73FFFFFF`     |
| selectedContentColor | Color | Icon and label color of the selected item         | `#FFFFB200`     |
| disabledContentColor | Color | Icon and label color of a disabled item           | `#33FFFFFF`     |

### BottomFloatingScrollToolbarDefaults Object

| Constant Name        | Type                | Description                                              | Value                         |
| -------------------- | ------------------- | -------------------------------------------------------- | ----------------------------- |
| Height               | Dp                  | Height of the pill and of each item                      | 56.dp                         |
| CornerRadius         | Dp                  | Pill corner radius; at 28dp on the 56dp pill it draws the system capsule curve | 28.dp                         |
| MaxWidth             | Dp                  | Maximum width of the pill                                | 460.dp                        |
| MarginHorizontal     | Dp                  | Outer horizontal margin kept on either side              | 16.dp                         |
| ContentPadding       | Dp                  | Horizontal padding inside the scrolling row              | 16.dp                         |
| FadingEdgeWidth      | Dp                  | Width of the fade on a scrollable edge                   | 50.dp                         |
| ItemWidth            | Dp                  | Width of each item cell                                  | 51.dp                         |
| ItemSpacing          | Dp                  | Spacing between adjacent items                           | 0.dp                          |
| IconSize             | Dp                  | Size of an item's icon                                   | 24.dp                         |
| IconLabelGap         | Dp                  | Vertical gap between icon and label                      | 1.dp                          |
| LabelWidth           | Dp                  | Width bound of an item's label                           | 49.dp                         |
| LabelSize            | Dp                  | Fixed font size of the label (independent of font scale) | 8.dp                          |
| LabelLineHeight      | Dp                  | Fixed line height of the label                           | 14.dp                         |
| ContainerColor       | Color               | Default pill fill color                                  | `Color(0xFF232323)`           |
| StrokeColor          | Color               | Default pill hairline color                              | `Color(0x0DFFFFFF)`           |
| StrokeWidth          | Dp                  | Width of the pill hairline                               | 0.67.dp                       |
| ContentColor         | Color               | Default idle item color                                  | `Color(0x73FFFFFF)`           |
| SelectedContentColor | Color               | Default selected item color                              | `Color(0xFFFFB200)`           |
| DisabledContentColor | Color               | Default disabled item color                              | `Color(0x33FFFFFF)`           |
| LabelShadow          | Shadow              | Drop shadow behind the label text                        | `#4C000000`, blurRadius `12f` |
| RevealSpec           | TweenSpec&lt;Float&gt; | Animation spec for neighbour reveal scrolling         | 298ms, cubic decelerate       |

`BottomFloatingScrollToolbarDefaults` also exposes two `@Composable` functions:

| Function Name | Signature                                                                 | Description                                       |
| ------------- | ------------------------------------------------------------------------- | ------------------------------------------------- |
| colors        | @Composable (...): BottomFloatingScrollToolbarColors                      | Creates and remembers the toolbar's color set     |
| labelStyle    | @Composable (): TextStyle                                                 | Medium-weight style converted from `LabelSize` dp |

## Native Tokens

| Default              | ColorOS Photos resource                                |
| -------------------- | ------------------------------------------------------ |
| Height               | `photoeditorpage_main_menu_portrait_height`            |
| CornerRadius         | `photo_editor_main_menu_pill_corner_radius`            |
| MaxWidth             | `photo_editor_main_menu_pill_max_size`                 |
| MarginHorizontal     | `photo_editor_main_menu_pill_margin_horizontal`        |
| FadingEdgeWidth      | `base_editor_fading_edge_width`                        |
| ItemWidth            | `photoeditorpage_main_menu_item_width`                 |
| IconSize             | `base_editor_main_menu_item_icon_height_width`         |
| LabelWidth           | `photoeditorpage_main_menu_item_text_width`            |
| ContainerColor       | `photo_editor_main_menu_pill_fill_color`               |
| StrokeColor          | `photo_editor_main_menu_pill_stroke_color`             |
| StrokeWidth          | `base_editor_menu_item_stroke_width`                   |
| ContentColor         | `picture3d_preview_menu_no_select_normal_color`        |
| SelectedContentColor | `base_editor_theme_color`                              |
| DisabledContentColor | `picture3d_preview_menu_no_select_gray_color`          |
| LabelShadow          | `base_editor_menu_item_text_shadow_color`              |
