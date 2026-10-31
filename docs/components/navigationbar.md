# NavigationBar

`NavigationBar` is a bottom navigation bar component in COUI, used to create navigation menus fixed at the bottom of applications. It supports 2 to 5 navigation items, offering different display modes (icon only, text only, icon and text, icon with selected label).

`FloatingNavigationBar` is a floating-style bottom navigation bar component, also supporting 2 to 5 navigation items, showing icons only.

These components are typically used in conjunction with the `Scaffold` component to maintain consistent layout and behavior across different pages in the application.

<div style="position: relative; height: 300px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../compose/index.html?id=navigationBar" title="Demo" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin"></iframe>
</div>

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.NavigationBar
import io.github.suqi8.coui.kmp.basic.NavigationBarItem
import io.github.suqi8.coui.kmp.basic.FloatingNavigationBar
import io.github.suqi8.coui.kmp.basic.FloatingNavigationBarItem
import io.github.suqi8.coui.kmp.basic.BottomFloatingNavigationBar
import io.github.suqi8.coui.kmp.basic.BottomFloatingNavigationBarItem
import io.github.suqi8.coui.kmp.basic.BottomFloatingNavigationBarDefaults
import io.github.suqi8.coui.kmp.basic.BottomFloatingRedDot
import io.github.suqi8.coui.kmp.basic.NavigationBarDisplayMode
import io.github.suqi8.coui.kmp.basic.NavigationItem
```

## Basic Usage

### NavigationBar

The NavigationBar component can be used to create bottom navigation menus fixed to the bottom:

```kotlin
var selectedIndex by remember { mutableStateOf(0) }
val items = listOf("Home", "Profile", "Settings")
val icons = listOf(COUIIcons.VerticalSplit, COUIIcons.Contacts, COUIIcons.Settings)

Scaffold(
    bottomBar = {
        NavigationBar {
            items.forEachIndexed { index, label ->
                NavigationBarItem(
                    selected = selectedIndex == index,
                    onClick = { selectedIndex = index },
                    icon = icons[index],
                    label = label
                )
            }
        }
    }
)
```

### FloatingNavigationBar

The FloatingNavigationBar component can be used to create floating navigation menus at the bottom:

```kotlin
var selectedIndex by remember { mutableStateOf(0) }
val items = listOf("Home", "Profile", "Settings")
val icons = listOf(COUIIcons.VerticalSplit, COUIIcons.Contacts, COUIIcons.Settings)

Scaffold(
    bottomBar = {
        FloatingNavigationBar {
            items.forEachIndexed { index, label ->
                FloatingNavigationBarItem(
                    selected = selectedIndex == index,
                    onClick = { selectedIndex = index },
                    icon = icons[index],
                    label = label
                )
            }
        }
    }
)
```

### BottomFloatingNavigationBar

`BottomFloatingNavigationBar` is the ColorOS 17 floating capsule bar (`COUIBottomFloatingNavigationBar`). It is a separate component from `FloatingNavigationBar`: its items carry an icon and a label, and a bar can host a red dot, a long-press overflow menu and a round expansion view that melts out of the capsule.

```kotlin
var selectedIndex by remember { mutableStateOf(0) }
val items = listOf("Home", "Messages", "Settings")
val icons = listOf(COUIIcons.Home, COUIIcons.Messages, COUIIcons.Settings)

BottomFloatingNavigationBar {
    items.forEachIndexed { index, label ->
        BottomFloatingNavigationBarItem(
            selected = selectedIndex == index,
            onClick = { selectedIndex = index },
            icon = icons[index],
            label = label
        )
    }
}
```

The bar samples the content behind it from `LocalBackdrop`; provide a backdrop to get the ColorOS 17 light-sensing glass, or pass `backdrop = null` to keep the capsule opaque.

## Component States

### Selected State

`NavigationBarItem` follows the COUI tab navigation state model: the selected item shows the icon and label at full label color, unselected items are dimmed, and the icon cross-fades between the two states over 180ms while the label color switches instantly. Labels always use medium font weight; pressing an unselected item previews the selected icon color. `FloatingNavigationBarItem` highlights the icon when selected.

## Properties

### NavigationBar Properties

| Property Name              | Type                     | Description                         | Default Value                         | Required |
| -------------------------- | ------------------------ | ----------------------------------- | ------------------------------------- | -------- |
| modifier                   | Modifier                 | Modifier applied to the nav bar     | Modifier                              | No       |
| color                      | Color                    | Background color of the nav bar     | COUITheme.colorScheme.background     | No       |
| showDivider                | Boolean                  | Show top divider line or not        | true                                  | No       |
| defaultWindowInsetsPadding | Boolean                  | Apply default window insets padding | true                                  | No       |
| mode                       | NavigationBarDisplayMode    | Display mode for items              | NavigationBarDisplayMode.IconAndText     | No       |
| content                    | @Composable RowScope.()  | The content of the nav bar          | -                                     | Yes      |

### NavigationBarItem Properties

| Property Name | Type          | Description                      | Default Value | Required |
| ------------- | ------------- | -------------------------------- | ------------- | -------- |
| selected      | Boolean       | Whether the item is selected     | -             | Yes      |
| onClick       | () -> Unit    | Callback when the item is clicked| -             | Yes      |
| icon          | ImageVector   | Icon of the item                 | -             | Yes      |
| label         | String        | Label of the item                | -             | Yes      |
| modifier      | Modifier      | Modifier applied to the item     | Modifier      | No       |
| enabled       | Boolean       | Whether the item is enabled      | true          | No       |

### FloatingNavigationBar Properties

| Property Name              | Type                     | Description                             | Default Value                           | Required |
| -------------------------- | ------------------------ | --------------------------------------- | --------------------------------------- | -------- |
| modifier                   | Modifier                 | Modifier applied to the nav bar         | Modifier                                | No       |
| color                      | Color                    | Background color of the nav bar         | COUITheme.colorScheme.surfaceContainer | No       |
| cornerRadius               | Dp                       | Corner radius of the nav bar            | FloatingToolbarDefaults.CornerRadius    | No       |
| horizontalAlignment        | Alignment.Horizontal     | Horizontal alignment within its parent  | CenterHorizontally                      | No       |
| horizontalOutSidePadding   | Dp                       | Horizontal padding outside the nav bar  | FloatingNavigationBarDefaults.HorizontalOutSidePadding | No       |
| shadowElevation            | Dp                       | The shadow elevation of the nav bar     | FloatingNavigationBarDefaults.ShadowElevation          | No       |
| showDivider                | Boolean                  | Show divider line around the nav bar    | false                                   | No       |
| defaultWindowInsetsPadding | Boolean                  | Apply default window insets padding     | true                                    | No       |
| content                    | @Composable () -> Unit   | The content of the nav bar              | -                                       | Yes      |

### FloatingNavigationBarItem Properties

| Property Name | Type          | Description                      | Default Value | Required |
| ------------- | ------------- | -------------------------------- | ------------- | -------- |
| selected      | Boolean       | Whether the item is selected     | -             | Yes      |
| onClick       | () -> Unit    | Callback when the item is clicked| -             | Yes      |
| icon          | ImageVector   | Icon of the item                 | -             | Yes      |
| label         | String        | Label of the item                | -             | Yes      |
| modifier      | Modifier      | Modifier applied to the item     | Modifier      | No       |
| enabled       | Boolean       | Whether the item is enabled      | true          | No       |

### BottomFloatingNavigationBar Properties

| Property Name  | Type                                | Description                                                             | Default Value             | Required |
| -------------- | ----------------------------------- | ----------------------------------------------------------------------- | ------------------------- | -------- |
| modifier       | Modifier                            | Modifier applied to the bar                                             | Modifier                  | No       |
| containerColor | Color                               | Opaque fill of the capsule when there is no backdrop to blur            | COUITheme.colorScheme.bar | No       |
| backdrop       | Backdrop?                           | The content blurred behind the capsule; `null` keeps it opaque          | LocalBackdrop.current     | No       |
| expanded       | Boolean                             | Whether the expansion view is shown; ignored when `expansion` is `null` | false                     | No       |
| expansion      | (@Composable BoxScope.() -> Unit)?  | The view that melts out of the capsule                                  | null                      | No       |
| shadow         | Shadow?                             | The shadow the capsule and the expansion view each cast; `null` for none. The default is left out where the system caustic already casts the native shadow | BottomFloatingNavigationBarDefaults.BarShadow | No |
| content        | @Composable RowScope.() -> Unit     | The content of the bar, usually `BottomFloatingNavigationBarItem`s      | -                         | Yes      |

### BottomFloatingNavigationBarItem Properties

| Property Name | Type                       | Description                                                                | Default Value | Required |
| ------------- | -------------------------- | -------------------------------------------------------------------------- | ------------- | -------- |
| selected      | Boolean                    | Whether the item is selected                                               | -             | Yes      |
| onClick       | () -> Unit                 | Callback when the item is clicked                                          | -             | Yes      |
| icon          | ImageVector                | Icon of the item                                                           | -             | Yes      |
| label         | String                     | Label of the item                                                          | -             | Yes      |
| modifier      | Modifier                   | Modifier applied to the item                                               | Modifier      | No       |
| enabled       | Boolean                    | Whether the item is enabled                                                | true          | No       |
| largeIcon     | Boolean                    | Whether to draw the 40dp large-icon form overflowing the item              | false         | No       |
| redDot        | BottomFloatingRedDot?      | The red dot pinned to the icon's top end corner                            | null          | No       |
| overflowMenu  | (@Composable () -> Unit)?  | The rows of the long-press overflow menu, shown in a `NavigationPopupMenu` | null          | No       |

### BottomFloatingRedDot

`BottomFloatingRedDot` is the red dot pinned to a bottom floating item's icon. A plain dot is `Dot`; a number capsule is `Count(number)`.

| Value         | Description                |
| ------------- | -------------------------- |
| Dot           | A plain dot                |
| Count(number) | A capsule showing `number` |

### BottomFloatingNavigationBarDefaults Object

The `BottomFloatingNavigationBarDefaults` object provides the metrics shared by `BottomFloatingNavigationBar` and `BottomFloatingNavigationBarItem`.

#### Constants

| Constant Name                   | Type     | Description                                                    | Default Value |
| ------------------------------- | -------- | -------------------------------------------------------------- | ------------- |
| BarHeight                       | Dp       | The capsule height                                             | 56.dp         |
| MinHeightWithoutPaddingVertical | Dp       | The minimum outer height of the bar                            | 88.dp         |
| PaddingHorizontal               | Dp       | The outside padding on a small screen                          | 16.dp         |
| MenuToScreenNaviDistance        | Dp       | The host's bottom padding under gesture navigation              | 8.dp          |
| ButtonNavigationDistance        | Dp       | The host's bottom padding under three-button navigation         | 36.dp         |
| navigationBarPadding()          | Dp       | The host's bottom padding for the current navigation mode       | 8.dp / 36.dp / 0.dp |
| ExpansionSize                   | Dp       | The footprint of the expansion view                            | 56.dp         |
| SideSpacing                     | Dp       | The gap between the capsule and the expansion view             | 12.dp         |
| BarShadow                       | Shadow   | The spot shadow the capsule and the expansion view cast (COUI `SHADOW_LV_B`) | 6.dp radius, black 12%, 2.dp down |
| NATIVE_BLUR_RADIUS_PX           | Float    | The pixel blur radius of COUI's `TYPE_FRAMEWORK_BOTTOM_BAR` material | 25f |
| BlurColorsLight                 | List&lt;BlendColorEntry&gt; | The material's tint layers in the light theme, in the order applied | `#B3666666` ColorDodge, `#99F5F5F5` Luminosity |
| BlurColorsDark                  | List&lt;BlendColorEntry&gt; | The same layers in the dark theme                    | `#80666666` Overlay, `#B3262626` Luminosity |
| RIM_ALPHA_LIGHT                 | Float    | The rim light's alpha in the light theme (`TYPE_FRAMEWORK_CAPSULE_3`) | 0.25f |
| RIM_ALPHA_DARK                  | Float    | The rim light's alpha in the dark theme                        | 0.8f          |
| CausticColor                    | Color    | The caustic light cast under the capsule (`coui_caustic_color_2`) | `#42FFFFFF` |
| ItemHeight                      | Dp       | The item cell height                                           | 48.dp         |
| ItemMaxWidth                    | Dp       | The maximum width of an item cell                              | 70.dp         |
| ItemSpacing                     | Dp       | The spacing between items                                      | 8.dp          |
| ItemInnerPadding                | Dp       | The horizontal padding inside an item cell                     | 12.dp         |
| ItemBackgroundHorizontalPadding | Dp       | The horizontal padding of the item background                  | 8.dp          |
| IconSize                        | Dp       | The icon size                                                  | 24.dp         |
| IconTopMargin                   | Dp       | The top margin of the icon                                     | 4.dp          |
| IconTitleGapVertical            | Dp       | The vertical gap between icon and title                        | 2.dp          |
| IconTitleGapHorizontal          | Dp       | The horizontal gap between icon and title                      | 4.dp          |
| LargeIconSize                   | Dp       | The icon size of the large-icon form                           | 40.dp         |
| LargeIconOverflow               | Dp       | How far the large-icon form is drawn above the item's top edge | 14.dp         |
| RedDotDiameter                  | Dp       | The diameter of the plain red dot                              | 6.dp          |
| RedDotRightOverflow1Digit       | Dp       | The red dot's end-edge overflow for a one-digit count          | 6.dp          |
| RedDotRightOverflow2Digit       | Dp       | The red dot's end-edge overflow for a two-digit count          | 9.dp          |
| RedDotRightOverflow3Digit       | Dp       | The red dot's end-edge overflow for a three-digit count        | 10.dp         |
| RedDotTopOverflow               | Dp       | The red dot's top overflow for a number                        | 4.dp          |
| TouchEffectRadius               | Dp       | The radius of the radial touch highlight                       | 40.dp         |
| MinimumSpacingWithSlideView     | Dp       | The minimum spacing kept with a slide view                     | 12.dp         |
| LabelFontSize                   | TextUnit | The label size, drawn as dp so it ignores the font scale (native `dp2px(10f)`) | 10.sp |
| CornerRadius                    | Dp       | The capsule corner radius                                      | 28.dp         |

#### Functions

| Function Name     | Return Type                 | Description                                                                                     |
| ----------------- | --------------------------- | ----------------------------------------------------------------------------------------------- |
| blurColors(isDark) | List&lt;BlendColorEntry&gt; | `BlurColorsDark` or `BlurColorsLight`                                                          |
| blurRadius()      | Float                       | `NATIVE_BLUR_RADIUS_PX` as the dp radius `textureBlur` needs for the same Gaussian at the current density |

### NavigationBarDefaults Object

The NavigationBarDefaults object provides default values for NavigationBar and NavigationBarItem components.

#### Constants

| Constant Name          | Type     | Description                                  | Default Value |
| ---------------------- | -------- | -------------------------------------------- | ------------- |
| ItemHeight             | Dp       | Item cell height                             | 56.dp         |
| HorizontalPadding      | Dp       | Horizontal padding at both edges of the bar  | 12.dp         |
| ItemHorizontalPadding  | Dp       | Horizontal padding inside each item cell     | 2.dp          |
| IconSize               | Dp       | Icon size                                    | 24.dp         |
| IconTopPadding         | Dp       | Top padding for the icon                     | 9.dp          |
| LabelBottomPadding     | Dp       | Bottom padding for the label                 | 7.dp          |
| LabelFontSize          | TextUnit | Label font size                              | 10.sp         |
| TextFontSize           | TextUnit | Text font size (TextOnly mode)               | 14.sp         |
| IconFadeDurationMillis | Int      | Icon cross-fade duration between states      | 180           |

### FloatingNavigationBarDefaults Object

The FloatingNavigationBarDefaults object provides default values for FloatingNavigationBar and FloatingNavigationBarItem components.

#### Constants

| Constant Name            | Type  | Description                         | Default Value |
| ------------------------ | ----- | ----------------------------------- | ------------- |
| HorizontalOutSidePadding | Dp    | Horizontal outside padding          | 36.dp         |
| ShadowElevation          | Dp    | Shadow elevation                    | 1.dp          |
| HorizontalPadding        | Dp    | Horizontal padding inside the bar   | 12.dp         |
| ItemSpacing              | Dp    | Spacing between items               | 12.dp         |
| IconSize                 | Dp    | Icon size                           | 28.dp         |
| IconPadding              | Dp    | Padding around the icon             | 10.dp         |
| SelectedPressedAlpha     | Float | Alpha for selected pressed item     | 0.5f          |
| UnselectedPressedAlpha   | Float | Alpha for unselected pressed item   | 0.6f          |
| UnselectedAlpha          | Float | Alpha for unselected item           | 0.4f          |

### NavigationBarDisplayMode Enum

| Value                 | Description                                  |
| --------------------- | -------------------------------------------- |
| IconAndText           | Show both icon and text                      |
| IconOnly              | Show icon only                               |
| TextOnly              | Show text only                               |
| IconWithSelectedLabel | Show icon always, show text only when selected|

The display mode set on `NavigationBar` is delivered to its items through the `LocalNavigationBarDisplayMode` composition local. `FloatingNavigationBar` has no `mode` parameter — its items always show icons only.

### NavigationItem Properties

`NavigationItem` is a convenience data class for holding a navigation item's label and icon.

| Property Name | Type        | Description       | Default Value | Required |
| ------------- | ----------- | ----------------- | ------------- | -------- |
| label         | String      | Label of the item | -             | Yes      |
| icon          | ImageVector | Icon of the item  | -             | Yes      |

## Advanced Usage

### NavigationBar

#### Custom Colors

```kotlin
NavigationBar(
    color = Color.Red.copy(alpha = 0.3f)
) {
    // ... items ...
}
```

#### Without Divider

```kotlin
NavigationBar(
    showDivider = false
) {
    // ... items ...
}
```

#### Handling Window Insets

```kotlin
NavigationBar(
    defaultWindowInsetsPadding = false // Handle window insets padding manually
) {
    // ... items ...
}
```

#### Display Modes

```kotlin
NavigationBar(
    // IconAndText (default) / IconOnly / TextOnly / IconWithSelectedLabel
    mode = NavigationBarDisplayMode.IconWithSelectedLabel
) {
    // ... items ...
}
```

### FloatingNavigationBar

#### Custom Color and Corner Radius

```kotlin
FloatingNavigationBar(
    color = COUITheme.colorScheme.primaryContainer,
    cornerRadius = 28.dp
) {
    // ... items ...
}
```

#### Custom Alignment and Padding

```kotlin
FloatingNavigationBar(
    horizontalAlignment = Alignment.Start, // Align to start
    horizontalOutSidePadding = 16.dp // Set outside padding
) {
    // ... items ...
}
```

#### Divider and Shadow

```kotlin
FloatingNavigationBar(
    showDivider = true, // Draw a thin divider ring around the bar
    shadowElevation = 0.dp // Disable the drop shadow
) {
    // ... items ...
}
```

### BottomFloatingNavigationBar

#### Glass Surface

`BottomFloatingNavigationBar` draws its capsule with the ColorOS 17 light-sensing glass. `backdrop` is the content it samples; it defaults to `LocalBackdrop.current`, so a bar placed where a backdrop is provided picks it up automatically. The glass is the material the native capsule requests, `TYPE_FRAMEWORK_BOTTOM_BAR`: the backdrop is blurred at the native 25px radius and tinted with blend-mode layers (`BlurColorsLight` / `BlurColorsDark`) that keep its hue and saturation and only remap its lightness, so the colour of what scrolls underneath still shows through. The rim carries the `TYPE_FRAMEWORK_CAPSULE_3` light and the capsule casts the native caustic light below it. When `backdrop` is `null` the capsule falls back to an opaque fill of `containerColor`; with a backdrop `containerColor` is not drawn, as on the device.

```kotlin
CompositionLocalProvider(LocalBackdrop provides backdrop) {
    BottomFloatingNavigationBar(containerColor = COUITheme.colorScheme.bar) {
        // ... items ...
    }
}
```

#### Capsule Geometry

The capsule is measured the way `BaseBottomFloatingMenuView.onMeasure` measures the native menu view, so every item gets an **exact** width instead of a wrap-content one:

- `innerPadding` 12dp (`BottomFloatingNavigationBarDefaults.ItemInnerPadding`, `coui_bottom_floating_navigation_bar_item_padding`), `itemSpace` 8dp (`ItemSpacing`, `..._item_space`) and `itemMaxWidth` 70dp (`ItemMaxWidth`, `..._item_max_width`); the capsule is 56dp tall (`BarHeight`, `..._height`);
- `totalItemMax = n * 70 + 2 * 12 + (n - 1) * 8`;
- when `totalItemMax` fits the width the bar leaves over, the capsule is exactly `totalItemMax` wide and every item is 70dp; otherwise the capsule takes the available width and every item gets `(available - 24 - (n - 1) * 8) / n`, truncated to whole pixels;
- every child is measured with an exact width and placed at `x = 12 + i * (itemWidth + 8)`, centred vertically in the capsule.

The capsule's fill, blur, shadow and touch highlight share one `CapsuleOutlineShape`. At the 28dp radius of the 56dp bar it follows the system capsule curve (`addSegmentCapsule`, `OplusPathAdapter.addSmoothRoundRect` without a weight), not a circular stadium; a smaller explicit radius keeps the Lp squircle. The expansion view's round silhouette stays a circle.

The 12dp inner padding is part of the capsule's own width rather than a `padding` modifier, so the glass surface and the radial touch highlight span the whole capsule. `RowScope.weight` and `RowScope.align` do nothing inside this bar: the row measures every child itself, matching the native menu view, which has no weight concept and places each child at its own computed x.

Vertically, the capsule is centred in the bar's 88dp box, as `COUIBottomFloatingNavigationBar.onLayout` centres its menu view. The room under the box belongs to the host: pad it with `BottomFloatingNavigationBarDefaults.navigationBarPadding()` — 8dp under gesture navigation (`MenuToScreenNaviDistance`), 36dp under three-button navigation (`ButtonNavigationDistance`), nothing without a navigation bar — in place of `navigationBarsPadding()`. Under gesture navigation the capsule then ends 24dp above the screen edge, as in the ColorOS 17 Gallery.

#### Expansion View (Melt Ball)

Pass an `expansion` view and drive it with `expanded`. The bar reserves room for it next to the capsule and draws both silhouettes through one merged surface, so the expansion view melts out of the capsule and back in:

```kotlin
var expanded by remember { mutableStateOf(false) }

BottomFloatingNavigationBar(
    expanded = expanded,
    expansion = {
        Box(
            modifier = Modifier.fillMaxSize().clip(CircleShape).background(COUITheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = COUIIcons.Add, contentDescription = "Expand")
        }
    },
) {
    // ... items ...
}
```

Without `expansion` the `expanded` flag has no effect and the bar is a plain capsule. While the expansion view is shown the capsule is measured against the width it leaves over — the expansion footprint plus `BottomFloatingNavigationBarDefaults.SideSpacing`.

#### Large Icon

Set `largeIcon = true` on an item to draw the 40dp form (`BottomFloatingNavigationBarDefaults.LargeIconSize`) overflowing the item by `BottomFloatingNavigationBarDefaults.LargeIconOverflow` instead of the 24dp icon. The label keeps its place below the icon slot.

```kotlin
BottomFloatingNavigationBarItem(
    selected = selectedIndex == 0,
    onClick = { selectedIndex = 0 },
    icon = COUIIcons.Home,
    label = "Home",
    largeIcon = true,
)
```

#### Red Dot

`redDot` pins a dot to the icon's top end corner: `BottomFloatingRedDot.Dot` for a plain dot, `BottomFloatingRedDot.Count(number)` for a number capsule. `null` shows nothing.

```kotlin
BottomFloatingNavigationBarItem(
    selected = false,
    onClick = {},
    icon = COUIIcons.Messages,
    label = "Messages",
    redDot = BottomFloatingRedDot.Count(12),
)
```

#### Overflow Menu

`overflowMenu` fills the item's long-press menu; its rows are shown in a `NavigationPopupMenu` anchored to the item. Passing `null` disables the long press.

```kotlin
BottomFloatingNavigationBarItem(
    selected = false,
    onClick = {},
    icon = COUIIcons.Settings,
    label = "Settings",
    overflowMenu = {
        NavigationPopupMenuItem(
            label = "Search",
            icon = COUIIcons.Search,
            onClick = { /* ... */ },
        )
    },
)
```

#### Radial Touch Highlight

While a finger is down, the bar paints a radial highlight under the items, clipped to the capsule silhouette. It runs from white at 38/255 alpha in the centre to fully transparent at `BottomFloatingNavigationBarDefaults.TouchEffectRadius` (40dp), and fades in on its own spring. The pointer is tracked without consuming the press, so the items' own tap and long-press gestures still see the whole gesture.

#### Responsive Margins

The 16dp outside padding (`BottomFloatingNavigationBarDefaults.PaddingHorizontal`) is kept on a small screen only; on the medium and large tiers it collapses to zero, so the capsule can use the full width.

That is the bar's only width tier, and there is no landscape branch. `COUIBottomFloatingNavigationBarMenuView` carries neither, and the `onConfigurationChanged` override only skips the next layout animation, which recomposition replaces. The `couiMenuViewCentered` attribute selects a second arrangement: on a small screen with exactly two visible items the bar reserves the expansion view's footprint plus `BottomFloatingNavigationBarDefaults.SideSpacing` on both sides of the capsule and centres the capsule in what is left, with the expansion view pinned to the bar's trailing edge. It is not exposed here. The native bar measures itself to its content plus padding and this port likewise wraps its content and lets the parent centre it, so the two arrangements cannot be told apart at these sizes, and the attribute defaults to `false`.

### Using with Page Navigation (Using Scaffold)

#### Using NavigationBar

```kotlin
val pages = listOf("Home", "Profile", "Settings")
val icons = listOf(COUIIcons.VerticalSplit, COUIIcons.Contacts, COUIIcons.Settings)
var selectedIndex by remember { mutableStateOf(0) }

Scaffold(
    bottomBar = {
        NavigationBar {
            pages.forEachIndexed { index, label ->
                NavigationBarItem(
                    selected = selectedIndex == index,
                    onClick = { selectedIndex = index },
                    icon = icons[index],
                    label = label
                )
            }
        }
    }
) { paddingValues ->
    // Content area needs to consider padding
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Current Page: ${pages[selectedIndex]}",
            style = COUITheme.textStyles.title1
        )
    }
}
```

#### Using FloatingNavigationBar

```kotlin
val pages = listOf("Home", "Profile", "Settings")
val icons = listOf(COUIIcons.VerticalSplit, COUIIcons.Contacts, COUIIcons.Settings)
var selectedIndex by remember { mutableStateOf(0) }

Scaffold(
    bottomBar = {
        FloatingNavigationBar {
            pages.forEachIndexed { index, label ->
                FloatingNavigationBarItem(
                    selected = selectedIndex == index,
                    onClick = { selectedIndex = index },
                    icon = icons[index],
                    label = label
                )
            }
        }
    }
) { paddingValues ->
    // Content area needs to consider padding
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Current Page: ${pages[selectedIndex]}",
            style = COUITheme.textStyles.title1
        )
    }
}
```
