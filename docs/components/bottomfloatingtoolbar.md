# BottomFloatingToolbar

`BottomFloatingToolbar` is the ColorOS 17 floating text-selection toolbar (`COUIBottomFloatingToolbar`). It is one or more capsules of `BottomFloatingToolbarItem`s that float over the content, sliding up from below the screen edge and back out again.

It is the text-selection sibling of `BottomFloatingEditToolbar` and `BottomFloatingNavigationBar`: the same slide motion, but the items are grouped — a selection toolbar shows *cut / copy / paste* in one capsule and *select all / translate* in another — and each capsule carries the ColorOS 17 material edge instead of the glass highlight the other two use.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.BottomFloatingToolbar
import io.github.suqi8.coui.kmp.basic.BottomFloatingToolbarGroup
import io.github.suqi8.coui.kmp.basic.BottomFloatingToolbarItem
import io.github.suqi8.coui.kmp.basic.BottomFloatingToolbarDefaults
```

## Basic Usage

```kotlin
var visible by remember { mutableStateOf(true) }

BottomFloatingToolbar(
    visible = visible,
) {
    BottomFloatingToolbarGroup {
        BottomFloatingToolbarItem(
            label = "Copy",
            icon = COUIIcons.Copy,
            onClick = { /* copy */ },
        )
        BottomFloatingToolbarItem(
            label = "Cut",
            icon = COUIIcons.Cut,
            onClick = { /* cut */ },
        )
        BottomFloatingToolbarItem(
            label = "Delete",
            icon = COUIIcons.Delete,
            onClick = { /* delete */ },
            enabled = false,
        )
    }
}
```

## Grouping

Each `BottomFloatingToolbarGroup` is drawn as its own capsule, so a group boundary is a visible gap. Groups are placed like the native `COUIBottomFloatingToolbar.onLayout`:

- one group is centred;
- two groups take the start and end edges, inset by the group margin;
- three or more keep the first and last groups on the edges and spread the rest evenly between them, never closer than 16dp to a neighbour.

The margin follows the width tier — 16dp below 600dp, 24dp up to 840dp, 40dp above that — exactly like the native `COUIBottomFloatingToolbar.onMeasure`.

```kotlin
BottomFloatingToolbar {
    BottomFloatingToolbarGroup {
        BottomFloatingToolbarItem(label = "Select all", icon = COUIIcons.SelectAll, onClick = {})
        BottomFloatingToolbarItem(label = "Translate", onClick = {})
    }
    BottomFloatingToolbarGroup {
        BottomFloatingToolbarItem(label = "Share", icon = COUIIcons.Share, onClick = {})
    }
}
```

## Showing and Hiding

`visible` drives the slide motion: the toolbar is translated down by the distance that puts the capsules just past the container's bottom edge and back to zero, on the show / hide spring. The toolbar keeps its 76dp minimum outer height while hidden, so the layout does not jump.

```kotlin
BottomFloatingToolbar(visible = isSelecting) {
    // ... groups ...
}
```

## Capsule Surface

The capsule samples the content behind it through `LocalBackdrop`. When a backdrop is present and the platform can run runtime shaders, the capsule is blurred and tinted with the toolbar colors; otherwise it falls back to an opaque `containerColor` fill. Either way the rim and inner shadow are drawn by `Modifier.materialShadowEdge` with the `TYPE_FRAMEWORK_CAPSULE_13` parameters, and a capsule holding a single item gets the single-item corner weight so its rim is a full capsule.

## Properties

### BottomFloatingToolbar Properties

| Property Name  | Type                  | Description                                                        | Default Value             | Required |
| -------------- | --------------------- | ------------------------------------------------------------------ | ------------------------- | -------- |
| modifier       | Modifier              | Modifier applied to the toolbar                                    | Modifier                  | No       |
| containerColor | Color                 | Background color of each capsule, and its tint over the blur      | COUITheme.colorScheme.bar | No       |
| backdrop       | Backdrop?             | The content blurred behind the capsules; `null` keeps them opaque  | LocalBackdrop.current     | No       |
| visible        | Boolean               | Whether the toolbar is on screen                                   | true                      | No       |
| content        | @Composable () -> Unit | The content of the toolbar, one or more `BottomFloatingToolbarGroup`s | -                      | Yes      |

### BottomFloatingToolbarGroup Properties

| Property Name | Type                  | Description                                   | Default Value | Required |
| ------------- | --------------------- | --------------------------------------------- | ------------- | -------- |
| modifier      | Modifier              | Modifier applied to the capsule               | Modifier      | No       |
| content       | @Composable () -> Unit | The content of the capsule, usually items     | -             | Yes      |

### BottomFloatingToolbarItem Properties

| Property Name      | Type         | Description                                                       | Default Value | Required |
| ------------------ | ------------ | ----------------------------------------------------------------- | ------------- | -------- |
| label              | String       | The label of the item; also the icon's content description        | -             | Yes      |
| onClick            | () -> Unit   | Called when the item is activated                                 | -             | Yes      |
| icon               | ImageVector? | The icon of the item, or `null` for a text-only item              | null          | No       |
| enabled            | Boolean      | Whether the item can be activated                                 | true          | No       |
| contentDescription | String?      | The icon's content description, or `null` for the label           | null          | No       |

### BottomFloatingToolbarDefaults Object

| Constant Name                   | Type                    | Description                                             | Value                        |
| ------------------------------- | ----------------------- | ------------------------------------------------------- | ---------------------------- |
| MenuViewHeight                  | Dp                      | The height of a group capsule                           | 44.dp                        |
| MinHeight                       | Dp                      | The minimum outer height of the toolbar                 | 76.dp                        |
| MenuViewPadding                 | Dp                      | The content padding at either end of a capsule          | 16.dp                        |
| ItemWidth                       | Dp                      | The icon cell width                                     | 44.dp                        |
| ItemHeight                      | Dp                      | The icon cell height                                    | 40.dp                        |
| ItemViewSpace                   | Dp                      | The spacing between items inside a capsule              | 4.dp                         |
| IconSize                        | Dp                      | The icon size                                           | 24.dp                        |
| LabelFontSize                   | TextUnit                | The label size                                          | 16.sp                        |
| ItemTextPaddingHorizontal       | Dp                      | The text cell's horizontal padding                      | 16.dp                        |
| ItemTextPaddingVertical         | Dp                      | The text cell's vertical padding                        | 10.dp                        |
| ItemTextMinWidth                | Dp                      | The text cell's minimum width                           | 52.dp                        |
| ItemTextMaxWidth                | Dp                      | The text cell's maximum width                           | 200.dp                       |
| ItemCircleRadius                | Dp                      | The radius of an item's circular touch mask             | 20.dp                        |
| MaskViewIconRadius              | Dp                      | The radius of the circular mask behind an icon item     | 20.dp                        |
| MenuTouchEffectRadius           | Dp                      | The radius of the radial press highlight                | 40.dp                        |
| ItemSingleMaskInsetHorizontal   | Dp                      | The single-item mask inset from the capsule's ends      | 2.dp                         |
| RedDotOffset                    | Dp                      | The horizontal offset of the red dot from the icon      | 12.dp                        |
| RedDotRadius                    | Dp                      | The red dot's diameter                                  | 6.dp                         |
| MinGroupSpace                   | Dp                      | The minimum gap kept between two capsules               | 16.dp                        |
| GroupMarginSmallScreen          | Dp                      | The edge inset of the outermost capsules on a small screen | 16.dp                     |
| GroupMarginMediumScreen         | Dp                      | The same inset on a medium screen                       | 24.dp                        |
| GroupMarginLargeScreen          | Dp                      | The same inset on a large screen                        | 40.dp                        |
| BlurRadius                      | Float                   | The blur radius in dp                                   | 25f                          |
| BlurColorsLight                 | List&lt;BlendColorEntry&gt; | The tint layers over the blurred backdrop, light theme | `#99F5F5F5` Luminosity, `#B3666666` ColorDodge |
| BlurColorsDark                  | List&lt;BlendColorEntry&gt; | The same layers in the dark theme                  | `#B3262626` Luminosity, `#80666666` Overlay |
| TranslationYSpring              | SpringSpec&lt;Float&gt; | The show / hide spring                                  | response 0.38 / bounce 0.75  |

`BottomFloatingToolbarDefaults` also exposes two functions:

| Function Name       | Signature                                                       | Description                              |
| ------------------- | --------------------------------------------------------------- | ---------------------------------------- |
| capsuleCornerRadius | (height: Dp, paddingTop: Dp = 0.dp, paddingBottom: Dp = 0.dp): Dp | The capsule radius, `(height - paddings) / 2` |
| blurColors          | @Composable (isDark: Boolean): BlurColors                        | The remembered tint layers for a theme   |

## Native Tokens

| Default                | COUI resource                                          |
| ---------------------- | ------------------------------------------------------ |
| MenuViewHeight         | `coui_floating_toolbar_menu_view_height`               |
| MinHeight              | `coui_floating_toolbar_min_height`                     |
| MenuViewPadding        | `coui_floating_toolbar_menu_view_padding`              |
| ItemWidth              | `coui_floating_toolbar_item_width`                     |
| ItemHeight             | `coui_floating_toolbar_item_height`                    |
| ItemViewSpace          | `coui_floating_toolbar_item_view_space`                |
| IconSize               | `coui_floating_toolbar_item_icon_width` / `..._icon_height` |
| LabelFontSize          | `coui_floating_toolbar_text_size`                      |
| ItemTextPadding*       | `coui_floating_toolbar_item_text_padding_horizontal` / `..._vertical` |
| ItemTextMinWidth       | `coui_floating_toolbar_item_text_min_width`            |
| ItemTextMaxWidth       | `coui_floating_toolbar_item_text_max_width`            |
| ItemCircleRadius       | `coui_floating_toolbar_item_circle_radius`             |
| MaskViewIconRadius     | `coui_floating_toolbar_mask_view_icon_radius`          |
| MenuTouchEffectRadius  | `coui_floating_toolbar_menu_touch_effect_radius`       |
| ItemSingleMaskInsetHorizontal | `coui_floating_toolbar_item_single_mask_inset_horizontal` |
| RedDotOffset           | `coui_floating_toolbar_red_dot_offset`                 |
| RedDotRadius           | `coui_floating_toolbar_red_dot_radius`                 |
| MinGroupSpace          | `coui_floating_toolbar_min_group_space`                |
| GroupMargin*           | `coui_floating_toolbar_group_margin_small_screen` / `..._medium_screen` / `..._large_screen` |

`menu_view_padding` is applied inside the capsule as its content padding. Natively it pads the menu view around the background, together with `menu_view_item_bg_inset_horizontal`; that inset is not part of this port, so the capsule hugs its items.

## Not Ported

- The framework-only `OplusMaterialEdgeParams` path (`TYPE_FRAMEWORK_CAPSULE_5`), used when the runtime shader is unavailable; the opaque `containerColor` stands in for it.
- The caustics shadow and the spot-light touch feedback (`COUISpotLightEffectDrawable`); the radial press highlight shared with `BottomFloatingNavigationBar` is drawn instead.
- The group insert / remove animation (`COUIBottomFloatingToolbarAction`) and XML menu inflation (`inflateMenu`).
