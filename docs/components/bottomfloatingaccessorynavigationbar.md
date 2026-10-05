# BottomFloatingAccessoryNavigationBar

`BottomFloatingAccessoryNavigationBar` is the ColorOS 17 bottom floating navigation bar with an accessory view on each side (`COUIBottomFloatingAccessoryNavigationBar`). It is `BottomFloatingNavigationBar` plus two round views that sit at the bar's start and end edges.

Both accessory views are 56dp circles separated from the capsule by a 12dp gap. A configured accessory always reserves its space, but only expands into view when its `*Visible` flag is set: it scales up from `0.5` and fades in, and slides out of the capsule, on the native springs. While either accessory is out, all three silhouettes — start accessory, capsule, end accessory — are drawn through one `MetaBallSurface`, so the accessory melts out of the capsule. With neither slot configured the bar is a plain glass capsule, exactly like `BottomFloatingNavigationBar`.

The bar samples the content behind it through `LocalBackdrop`; when no backdrop is available it keeps an opaque fill.

On compatible ColorOS devices, the merged silhouette uses the system material renderer. Other platforms use the portable shader. Dragging preserves spring velocity when the target changes, and release springs use the control area and release velocity. During collapse, an accessory stays opaque until half of its transformed area overlaps the capsule, then fades as the overlap grows. Stretching transforms the original circle into an ellipse. The navigation capsule owns item press motion, so its items do not apply a second scale.

Press lighting belongs to the region where the gesture starts: the start circle, menu capsule, or end circle. Its spring follows the finger with unclamped coordinates while drawing stays clipped to that region. A nearby exit leaves part of the halo visible, a distant exit leaves none, and returning restores it without transferring the light to another region. All navigation items share the menu capsule's light.

The lower light uses native caustic shadows: level-4 for the bar (bottom offset 4.899994dp, SDF width 10.399994dp) and level-5 for accessories (11.199982dp, 20dp). Both use the same vertical power falloff and coui_caustic_color_2, without a 75° angular mask. ColorOS calls the system API; other platforms run the recovered formula.

Upper and lower rims use TYPE_FRAMEWORK_CAPSULE_3 / EDGE_STYLE_0: width 2.5dp, alpha 0.8 in dark mode and 0.25 in light mode. The upper far line is narrower; the lower near line is broader and weaker. Caustic light and contour highlights are separate, and both rims remain present with backdrop blur disabled.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.BottomFloatingAccessoryNavigationBar
import io.github.suqi8.coui.kmp.basic.BottomFloatingAccessoryNavigationBarDefaults
import io.github.suqi8.coui.kmp.basic.BottomFloatingNavigationBarItem
import io.github.suqi8.coui.kmp.basic.BottomFloatingRedDot
```

## Basic Usage

```kotlin
var selectedIndex by remember { mutableIntStateOf(0) }

BottomFloatingAccessoryNavigationBar(
    startAccessory = {
        // The merged blob already carries the accessory's circle, so the slot draws no background.
        Icon(imageVector = COUIIcons.Search, contentDescription = "Search")
    },
    endAccessory = {
        Icon(imageVector = COUIIcons.Add, contentDescription = "Add")
    },
) {
    BottomFloatingNavigationBarItem(
        selected = selectedIndex == 0,
        onClick = { selectedIndex = 0 },
        icon = COUIIcons.Home,
        label = "Home",
    )
    BottomFloatingNavigationBarItem(
        selected = selectedIndex == 1,
        onClick = { selectedIndex = 1 },
        icon = COUIIcons.Messages,
        label = "Messages",
    )
}
```

## Collapsing an Accessory

`startAccessoryVisible` and `endAccessoryVisible` toggle each accessory between its expanded and collapsed state. The slot stays configured either way, so its space is still reserved and only the view animates:

```kotlin
BottomFloatingAccessoryNavigationBar(
    startAccessoryVisible = showSearch,
    startAccessory = {
        Icon(imageVector = COUIIcons.Search, contentDescription = "Search")
    },
) {
    // ... items ...
}
```

## Properties

### BottomFloatingAccessoryNavigationBar Properties

| Property Name         | Type                                | Description                                                                       | Default Value             | Required |
| --------------------- | ----------------------------------- | --------------------------------------------------------------------------------- | ------------------------- | -------- |
| modifier              | Modifier                            | Modifier applied to the bar                                                        | Modifier                  | No       |
| containerColor        | Color                               | Background color of the capsule and of the merged blob                             | COUITheme.colorScheme.bar | No       |
| backdrop              | Backdrop?                           | The content blurred behind the bar; `null` keeps it opaque                         | LocalBackdrop.current     | No       |
| startAccessoryVisible | Boolean                             | Whether the `startAccessory` view is expanded                                      | true                      | No       |
| endAccessoryVisible   | Boolean                             | Whether the `endAccessory` view is expanded                                        | true                      | No       |
| startAccessory        | (@Composable BoxScope.() -> Unit)?  | The view at the bar's start edge; `null` is not configured and reserves no space   | null                      | No       |
| endAccessory          | (@Composable BoxScope.() -> Unit)?  | The view at the bar's end edge; `null` is not configured and reserves no space     | null                      | No       |
| content               | @Composable RowScope.() -> Unit     | The content of the bar, usually `BottomFloatingNavigationBarItem`s                 | -                         | Yes      |

### BottomFloatingAccessoryNavigationBarDefaults Object

| Constant Name            | Type              | Description                                                        | Value                       |
| ------------------------ | ----------------- | ------------------------------------------------------------------ | --------------------------- |
| AccessorySize            | Dp                | The accessory view's diameter                                      | 56.dp                       |
| SideSpacing              | Dp                | The gap between the capsule and an accessory view                  | 12.dp                       |
| PaddingHorizontal        | Dp                | The outside padding kept on a small screen                         | 16.dp                       |
| MinScale                 | Float             | The scale the accessory view starts from                           | 0.5f                        |
| AlphaSpring              | `SpringSpec<Float>` | The accessory's alpha spring                                       | response 0.35 / bounce 0    |
| ScaleSpring              | `SpringSpec<Float>` | The accessory's scale spring                                       | response 0.55 / bounce 0.35 |
| TranslationXSpring       | `SpringSpec<Float>` | The spring that slides the accessory out of the capsule            | response 0.55 / bounce 0.35 |
| LowEndAlphaSpring        | `SpringSpec<Float>` | The alpha spring used when the device reports a low animation level | response 0.2 / bounce 0     |
| LowEndScaleSpring        | `SpringSpec<Float>` | The low-end scale spring                                           | response 0.5 / bounce 0.3   |
| LowEndTranslationXSpring | `SpringSpec<Float>` | The low-end translation spring                                     | response 0.5 / bounce 0.3   |

## Responsive Padding

Like the plain floating bar, the 16dp outside padding is kept on a small screen only and collapses to zero on the medium and large tiers. The capsule is centred in the space the accessory views leave over.

The capsule itself is measured the way `BaseBottomFloatingMenuView.onMeasure` measures the native menu view — as wide as its items at `BottomFloatingNavigationBarDefaults.ItemMaxWidth` (70dp) plus the 12dp inner padding and the 8dp gaps while they all fit, and squeezed into the width the accessories leave over when they do not. Vertically it sits `BottomFloatingNavigationBarDefaults.MenuToScreenNaviDistance` (8dp, `coui_bottom_floating_navigation_bar_menu_to_screen_navi_distance`) above the bottom edge of the bar's box, i.e. 24dp above the screen edge once the system's 16dp `navigationBars` inset is added.

`onLayout` picks between `layoutPositionsSmallScreen` and `layoutPositionsMediumScreen` on that same tier, but the choice cannot be observed here. Only the small-screen method survives decompilation, and it pins each accessory to its own outer edge of the bar and centres the menu between them — which is what this bar already does, because the native component measures itself to its content plus padding and this port likewise wraps its content.

## Relationship to BottomFloatingNavigationBar

Both bars share their items, red dots and defaults: `BottomFloatingNavigationBarItem` and `BottomFloatingRedDot` are used here unchanged, and the capsule height, corner radius, item metrics and touch highlight come from `BottomFloatingNavigationBarDefaults`. Use `BottomFloatingNavigationBar` when no side accessory is needed, and this component when the bar carries one or two round actions next to the capsule.

The upper/lower rim now samples the actual blurred backdrop before tinting and follows libhwui `blendEdge` brightness correction and RGB mixing. Alpha 0.8/0.25 controls that material blend, rather than an opaque white overlay. The same formula is used by toolbar circles, with their own native circle parameters. Exterior caustic profiles remain unchanged.
