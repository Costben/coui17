# HorizontalPopupMenu

`HorizontalPopupMenu` is the ColorOS 17 horizontal icon menu block of a popup list. It lays its items out as a grid of equal columns across the popup width: 3 columns when at least one item carries a label, otherwise between 3 and 5 columns depending on how many items there are.

Each item is a square button — icon on top, optional label underneath — with a smooth-cornered press mask over the whole item. Icon-only items use a 24dp icon and a 40dp square; items with a label use a 20dp icon over a 12sp, two-line centred title. Compose the block inside the popup content next to the vertical rows.

Two deliberate differences from the native adapter: COUI drops horizontal items beyond the column limit, while this component wraps the excess onto a second grid row; and the divider band COUI inserts after the block is owned by `ListPopupColumn`'s measure policy, which only bands adjacent registered rows.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.HorizontalPopupMenu
import io.github.suqi8.coui.kmp.basic.HorizontalPopupMenuItem
import io.github.suqi8.coui.kmp.basic.HorizontalPopupMenuDefaults
import io.github.suqi8.coui.kmp.basic.ListPopupColumn
import io.github.suqi8.coui.kmp.window.WindowListPopup
```

## Basic Usage

```kotlin
var showPopup by remember { mutableStateOf(false) }

Box {
    TextButton(
        text = "Show menu",
        onClick = { showPopup = true },
    )
    WindowListPopup(
        show = showPopup,
        onDismissRequest = { showPopup = false },
    ) {
        val dismiss = LocalDismissState.current
        ListPopupColumn {
            HorizontalPopupMenu {
                HorizontalPopupMenuItem(
                    icon = COUIIcons.Copy,
                    label = "Copy",
                    onClick = { dismiss?.invoke() },
                )
                HorizontalPopupMenuItem(
                    icon = COUIIcons.Share,
                    label = "Share",
                    onClick = { dismiss?.invoke() },
                )
                HorizontalPopupMenuItem(
                    icon = COUIIcons.Delete,
                    label = "Delete",
                    onClick = { dismiss?.invoke() },
                )
            }
        }
    }
}
```

## Icon-only Items

Leave `label` as `null` (or empty) to select the icon-only shape. The item then takes the 40dp square, the 24dp icon and the icon-only padding, and `contentDescription` names the button for accessibility:

```kotlin
HorizontalPopupMenu {
    HorizontalPopupMenuItem(
        icon = COUIIcons.Copy,
        contentDescription = "Copy",
        onClick = { /* ... */ },
    )
    HorizontalPopupMenuItem(
        icon = COUIIcons.Delete,
        contentDescription = "Delete",
        onClick = { /* ... */ },
    )
}
```

The item is measured with an exact width by `HorizontalPopupMenu`; the caller does not size it.

## Properties

### HorizontalPopupMenu Properties

| Property Name | Type                   | Description                                                              | Default Value | Required |
| ------------- | ---------------------- | ------------------------------------------------------------------------ | ------------- | -------- |
| modifier      | Modifier               | Modifier applied to the block                                            | Modifier      | No       |
| content       | @Composable () -> Unit  | The `HorizontalPopupMenuItem`s; they are laid out in composition order   | -             | Yes      |

### HorizontalPopupMenuItem Properties

| Property Name      | Type        | Description                                                                 | Default Value | Required |
| ------------------ | ----------- | --------------------------------------------------------------------------- | ------------- | -------- |
| icon               | ImageVector | The icon drawn above the label                                              | -             | Yes      |
| onClick            | () -> Unit  | Invoked when the item is activated                                          | -             | Yes      |
| modifier           | Modifier    | Modifier applied to the item                                                | Modifier      | No       |
| label              | String?     | The label drawn under the icon; `null` or empty selects the icon-only shape | null          | No       |
| enabled            | Boolean     | Whether the item reacts to input                                            | true          | No       |
| contentDescription | String?     | Accessible name for an icon-only item; the label names the other shape      | null          | No       |

### HorizontalPopupMenuDefaults Object

| Constant Name               | Type     | Description                                                                     | Value   |
| --------------------------- | -------- | ------------------------------------------------------------------------------- | ------- |
| ColumnsWithText             | Int      | Columns when at least one item carries a label                                  | 3       |
| MaxIconOnlyColumns          | Int      | Columns of an icon-only block at most                                           | 5       |
| MinColumns                  | Int      | Columns of an icon-only block at least                                          | 3       |
| MinColumnWidth              | Dp       | Minimum width of one column                                                     | 52.dp   |
| IconOnlySize                | Dp       | Height of an icon-only item, which is also its width floor                      | 40.dp   |
| TextMinHeight               | Dp       | Minimum height of an item with a label                                          | 40.dp   |
| TextPaddingHorizontal       | Dp       | Horizontal padding of an item with a label                                      | 6.dp    |
| TextPaddingVertical         | Dp       | Vertical padding of an item with a label                                        | 6.dp    |
| IconOnlyPaddingHorizontal   | Dp       | Horizontal padding of an icon-only item                                         | 8.dp    |
| IconSizeWithText            | Dp       | Icon size of an item with a label                                               | 20.dp   |
| IconSizeIconOnly            | Dp       | Icon size of an icon-only item                                                  | 24.dp   |
| IconTitleGap                | Dp       | Gap between the icon and the label                                              | 4.dp    |
| TextSpacing                 | Dp       | Gap between two labelled items                                                  | 4.dp    |
| IconOnlySpacing             | Dp       | Gap between two icon-only items                                                 | 8.dp    |
| SpacingMin                  | Dp       | Floor the container-derived gap may not fall below                              | 4.dp    |
| TextEdgeMargin              | Dp       | Leading and trailing margin of a labelled block                                 | 8.dp    |
| IconOnlyEdgeMargin          | Dp       | Leading and trailing margin of an icon-only block                               | 12.dp   |
| MaskRadiusIconOnly          | Dp       | Press mask radius of an icon-only item, `OS <= 37` arc                          | 12.dp   |
| MaskRadiusIconOnlySmooth    | Dp       | Press mask radius of an icon-only item on the smooth-corner generation          | 17.dp   |
| MaskRadiusWithText          | Dp       | Press mask radius of a labelled item, `OS <= 37` arc                            | 16.dp   |
| MaskRadiusWithTextSmooth    | Dp       | Press mask radius of a labelled item on the smooth-corner generation            | 23.dp   |
| LabelFontSize               | TextUnit | The label size                                                                  | 12.sp   |

## Sizing

The block is the popup's widest child, so it sizes itself within the popup list's minimum and maximum width band (`ListPopupDefaults.MinWidth` 168dp, `ListPopupDefaults.MaxWidth` 256dp). The leading, trailing and inter-item margins are derived from the block's width and item count: the default edge margin and gap are capped to what the container can afford, and any remainder is split between an item's two sides, so every icon-only column lands on its 40dp square.
