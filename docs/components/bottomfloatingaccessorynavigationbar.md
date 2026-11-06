# BottomFloatingAccessoryNavigationBar

`BottomFloatingAccessoryNavigationBar` is the ColorOS 17 bottom floating navigation bar with an accessory view on each side. It is `BottomFloatingNavigationBar` plus two round views that sit at the bar's start and end edges.

Both accessory views are 56dp circles separated from the capsule by a 12dp gap. A configured accessory always reserves its space, but only expands into view when its `*Visible` flag is set: it scales up from `0.5` and fades in, and slides out of the capsule, on the native springs. While either accessory is out, all three silhouettes — start accessory, capsule, end accessory — merge into one blob, so the accessory melts out of the capsule: the system material chains them on ColorOS (`setMetaBallChain`), the runtime `MetaBallSurface` shader everywhere else. With neither slot configured the bar is a plain glass capsule, exactly like `BottomFloatingNavigationBar`.

The capsule's fill, blur, shadow and touch highlight share one `CapsuleOutlineShape` and follow the system capsule curve (`addSegmentCapsule`), not a circular stadium; the two accessory views stay circles. The `MetaBallSurface` capsule particle is still a rounded-rect SDF, so while the blob is merged its capsule end is that shader's circular approximation.

The bar samples the content behind it through `LocalBackdrop`; when no backdrop is available it keeps an opaque fill.

The merged silhouette is drawn by the system material on ColorOS and by the runtime shader on every other platform — see [Platform Material](#platform-material) for which one runs and when. The shader is the fallback: at rest it matches the renderer it replaces to within one level of brightness. Dragging preserves spring velocity when the target changes, and release springs use the control area and release velocity. During collapse, an accessory stays opaque until half of its transformed area overlaps the capsule, then fades as the overlap grows. Stretching transforms the original circle into an ellipse. The navigation capsule owns item press motion, so its items do not apply a second scale.

Press lighting belongs to the region where the gesture starts: the start circle, menu capsule, or end circle. Its spring follows the finger with unclamped coordinates while drawing stays clipped to that region. A nearby exit leaves part of the halo visible, a distant exit leaves none, and returning restores it without transferring the light to another region. All navigation items share the menu capsule's light.

The lower light uses native caustic shadows: level-4 for the bar (bottom offset 4.899994dp, SDF width 10.399994dp) and level-5 for accessories (11.199982dp, 20dp). Both use the same vertical power falloff and coui_caustic_color_2, without a 75° angular mask. ColorOS calls the system API; other platforms run the recovered formula.

Upper and lower rims use TYPE_FRAMEWORK_CAPSULE_3 / EDGE_STYLE_0: width 2.5dp, alpha 0.8 in dark mode and 0.25 in light mode. The upper far line is narrower, the lower near line broader. The device gives both edges the same `edgeAlpha`, so its lower line reads 0.89x the upper one in dark and 1.24x in light; `EDGE_STYLE_0` weights the two lines 0.7 / 1.0, and `BottomFloatingNavigationBarDefaults.rimBottomScale(isDark)` supplies the rest, which puts the Compose lower rim within a level of the device in both themes. Caustic light and contour highlights are separate, and both rims remain present with backdrop blur disabled.

The plain capsule form of `BottomFloatingNavigationBar` and of this component (no expansion view, no accessories) draws through the same silhouette renderer, so it carries the same rim and the same caustic light; without that it had the upper rim and nothing under its lower edge.

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

Configuring both accessories and keeping both collapsed is the ColorOS 17 Gallery's 创作 (Create) tab: a lone capsule, still centred because both slots reserve the same space. Configure both sides for this state; configuring only one leaves the capsule off centre. Measured on the PLK110 Gallery, the capsule wears the same translucent material whether its accessories are out or collapsed — its brightness follows the content behind it on every tab.

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

The capsule itself is measured the way ColorOS measures the native menu view — as wide as its items at `BottomFloatingNavigationBarDefaults.ItemMaxWidth` (70dp) plus the 12dp inner padding and the 8dp gaps while they all fit, and squeezed into the width the accessories leave over when they do not. Vertically it is centred in the bar's 88dp box, as `COUIBottomFloatingAccessoryNavigationBar.onLayout` centres its children. The room under the box belongs to the host: pad it with `BottomFloatingNavigationBarDefaults.navigationBarPadding()` (8dp under gesture navigation, 36dp under three-button navigation) instead of `navigationBarsPadding()`, and the capsule ends 24dp above the screen edge under gesture navigation, as in the ColorOS 17 Gallery.

The layout picks between a small-screen and a medium-screen placement on that same tier, but the choice cannot be observed here. The small-screen placement pins each accessory to its own outer edge of the bar and centres the menu between them — which is what this bar already does, because the native component measures itself to its content plus padding and this port likewise wraps its content.

## Platform Material

On ColorOS 17 the bar hands the whole material to the system instead of painting its own glass: `setBackgroundRenderEffect` blurs and tints the backdrop, `setCornerParams` / `setEdgeParams` / `setShadowParams` draw the `TYPE_FRAMEWORK_CAPSULE_3` continuous-corner stroke and its fade band, the outline caustic shadow — or the `level3b` spot shadow when the caustic is unavailable — casts the lift, and `setMetaBallChain` melts the capsule and the two accessory balls into one blob.

`LocalPlatformBottomBarMaterialEnabled` (default `true`) controls that, and `LocalBlurEnabled` (default `true`) gates it. The system material blurs the backdrop itself, so with blur switched off — by the user or by the system's power-saving state — the bar goes back to the Compose renderer instead of blurring anyway. On every other platform, and on a ColorOS device missing one of those APIs, the bar uses the Compose renderer regardless: `textureBlur` behind the capsule and a `MetaBallSurface` for the merged silhouettes.

The `setBaseParams` renderer this path replaced replayed the vendor's material animation whenever the overlay state changed — opening a dialog shrank the whole blob to 84% and sprang it back over ~160 ms — which is why the runtime shader was briefly forced on ColorOS. The background-render-effect renderer does not replay it: on the reference device, frame-by-frame capture with the overflow panel open shows the silhouette holding its resting geometry (best-fit scale 1.000 against the resting frame, correlation ≥ 0.99) with the switch on and off alike.

The switch defaults to on because the system path is what the platform itself draws, even though it is the more expensive one. On the same device and release build, with the same 500 px drags over the middle of the list (atrace `DrawFrame` slice, two interleaved sessions), the Compose renderer measured 5.82 ms p50 at 120 Hz and 8.97 ms at 90 Hz, the system backdrop blur alone 6.04 / 9.39 ms, and the full system material 6.56 / 10.46 ms. The gap is 0.7–1.5 ms of a frame — 13–16 % — and it does not cost a frame: both paths hold the display's refresh rate, and their dropped-frame rates differ by about a percentage point (3.1 % versus 3.5 % at 120 Hz, 8.8 % versus 10.0 % at 90 Hz). The cost is spread across the system backdrop pass and the extra display-list work behind it rather than concentrated in one parameter, so there is nothing to trim without leaving the system's own look. Set the switch to `false` when that headroom matters more than the pixel-exact system material.

The stroke and the caustic shadow are not painted unconditionally either. They follow the switches `COUIMaterialStateManager` reads: `system_material_stroke_enable`, the low-power and super power-saving modes, the app's `coui_blur_enable` / `is_immersive_theme` booleans and the `persist.sys.oplus.anim_level` animation level, all observed live. With the stroke switched off the bar applies `TYPE_NO_EFFECT` — no rim at all, not a dimmer one — and falls back from the caustic to the `level3b` spot shadow.

## Relationship to BottomFloatingNavigationBar

Both bars share their items, red dots and defaults: `BottomFloatingNavigationBarItem` and `BottomFloatingRedDot` are used here unchanged, and the capsule height, corner radius, item metrics and touch highlight come from `BottomFloatingNavigationBarDefaults`. Use `BottomFloatingNavigationBar` when no side accessory is needed, and this component when the bar carries one or two round actions next to the capsule.

The upper/lower rim now samples the actual blurred backdrop before tinting and follows libhwui `blendEdge` brightness correction and RGB mixing. Alpha 0.8/0.25 controls that material blend, rather than an opaque white overlay. The same formula is used by toolbar circles, with their own native circle parameters. Exterior caustic profiles remain unchanged.
