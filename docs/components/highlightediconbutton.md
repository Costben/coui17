# HighlightedIconButton

`HighlightedIconButton` is the ColorOS 17 toolbar icon button with its light halo, the port of the effect ColorOS puts on the toolbar's back key and overflow key.

ColorOS gives both keys the same treatment: a **true 40dp circle** — not a squircle — carrying a radial fill that is darker in the middle and brighter at the rim, an angle-distributed white perimeter stroke, and a theme-dependent halo outside the edge. While pressed the whole control springs to `HighlightedIconButtonDefaults.PressedScale`, the rim and the halo disappear, and the fill and the icon brighten. Both themes behave identically.

The dark lower halo uses the ColorOS caustic-shadow API with the level-2 offset-rectangle SDF and vertical power falloff. Other platforms render the recovered shader formula without an angular mask. The halo shares the button transform and fades while pressed. Dragging adds the native TouchMotion rubber-band translation and area-preserving deformation, then springs back on release. Fill, icon, rim, and outer light share one transform.

The halo is painted outside the node's diameter, so nothing in the caller's chain may clip it (no `clip`, no clip-shaped parent).

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.HighlightedIconButton
import io.github.suqi8.coui.kmp.basic.HighlightedIconButtonDefaults
```

## Native Origin

The native chain is `ToolbarMaterialEffectDelegate.applyStroke` → `ToolbarMaterialEffectResolver.getCircleStrokeEffectType` → `TYPE_FRAMEWORK_CIRCLE_1`. The normal toolbar uses alpha 0.6 (dark) / 0.2 (light), width 1.9dp, and `EDGE_STYLE_1`. The window/popup circle preset is a separate path.

The perimeter uses the framework `blendEdge` RGB formula: brightness correction `0.1 × (1 − clamp(1.2 × luma))⁴`, followed by the native blend with the sampled surface. It preserves the fill alpha and leaves the icon and exterior caustic outside the effect. This is shared with the floating bottom bar; each component retains its native geometry and parameters.

The content colour is published through `LocalContentColor`, so a default `Icon` in `content` follows the fill as it brightens.

## Basic Usage

```kotlin
HighlightedIconButton(onClick = { /* Handle click event */ }) {
    Icon(imageVector = COUIIcons.Back, contentDescription = "Back")
}
```

A back key that mirrors itself in a right-to-left layout is the repository's
`example/shared` `BackNavigationIcon`, which draws the arrow inside this button through
`Modifier.graphicsLayer`:

```kotlin
val layoutDirection = LocalLayoutDirection.current
HighlightedIconButton(onClick = onClick) {
    Icon(
        modifier = Modifier.graphicsLayer {
            if (layoutDirection == LayoutDirection.Rtl) scaleX = -1f
        },
        imageVector = COUIIcons.Back,
        contentDescription = null,
    )
}
```

## Parameters

### HighlightedIconButton Properties

| Property Name | Type                       | Description                                              | Default Value                      | Required |
| ------------- | -------------------------- | -------------------------------------------------------- | ---------------------------------- | -------- |
| onClick       | () -> Unit                 | The callback when the button is clicked                  | -                                  | Yes      |
| modifier      | Modifier                   | Applied to the button                                    | Modifier                           | No       |
| size          | Dp                         | The diameter of the button                               | HighlightedIconButtonDefaults.Size | No       |
| enabled       | Boolean                    | Whether the button can be clicked                        | true                               | No       |
| content       | @Composable BoxScope.() -> Unit | The content of the button, typically an `Icon`, drawn centred in the circle | - | Yes |

The click carries no ripple: the highlight itself is the press feedback, exactly as on the device.

### HighlightedIconButtonDefaults Object

#### Constants

| Constant Name         | Type                   | Description                                                             | Value |
| --------------------- | ---------------------- | ----------------------------------------------------------------------- | ----- |
| Size                  | Dp                     | The diameter of the button                                              | 40.dp |
| PressedScale          | Float                  | The scale the whole control springs to while pressed                    | 0.89f |
| PressSpring           | SpringSpec\<Float\>    | The press spring                                                        | response 0.3 / damping 1.0 |
| FillAlpha             | Float                  | The alpha the radial fill is painted at                                 | 0.78f |
| FillEdgeStop          | Float                  | The radial-fill stop at which the edge colour is reached, as a fraction of the radius | 0.89f |
| FillCenterDark        | Color                  | The radial fill's centre colour, dark theme                             | `#1E1E1E` |
| FillEdgeDark          | Color                  | The radial fill's edge colour, dark theme                               | `#242424` |
| FillCenterLight       | Color                  | The radial fill's centre colour, light theme                            | `#F9F9F9` |
| FillEdgeLight         | Color                  | The radial fill's edge colour, light theme                              | `#FBFBFB` |
| PressedFillBrighten   | Float                  | How far the pressed fill is lerped toward white                         | 0.06f (nominal) |
| PressedContentBrighten| Float                  | How far the pressed content colour is lerped toward white                | 0.1f (nominal) |
| StrokeAlpha           | Float                  | The rim alpha                                                           | 0.6f (dark); `LightStrokeAlpha` 0.2f |
| StrokeWidth           | Dp                     | The rim width                                                           | 1.9.dp |
| StrokeShadowFadeIn    | Float                  | The rim's inner-shadow fade-in scale                                    | 0.1f |
| Stroke                | MaterialStrokeParams   | The rim: `StrokeAlpha` white, `StrokeWidth` wide, no inner shadow       | — |
| DarkHaloColor | Color | Native dark caustic color | white @ 20% |
| DarkHaloExtent | Dp | Level-2 bottom layout offset | 4.899994.dp |
| DarkHaloSdfWidth | Dp | Level-2 SDF width | 9.22998.dp |



| LightHaloColor        | Color                  | The light theme's halo colour                                           | black @ 5% |
| LightHaloRadius       | Dp                     | How far the light theme's halo reaches past the edge                    | 15.5.dp |
| LightHaloOffsetY      | Dp                     | The light theme's halo vertical offset                                  | 1.9.dp |

`fillCenter(isDark)` and `fillEdge(isDark)` resolve the two fill colours for the current theme. `PressedFillBrighten` and `PressedContentBrighten` are nominal: the measurement records that the fill and the icon brighten while pressed but carries no magnitude, so these two values are not device-derived.

## Recovered Tokens

Rim parameters and blending come from ToolbarMaterialEffectDelegate, ToolbarMaterialEffectResolver, COUIMaterialStrokeEffect, and libhwui. The radial fill and pressed size retain earlier device measurements.

| Token | Value | Layer |
| :--- | :--- | :--- |
| Diameter | 155px at 1dp = 3.875px, i.e. 40dp; a true circle, not a squircle | device measurement |
| Rim | circle-1 alpha 0.6 dark / 0.2 light, native material brightness blend | device measurement |
| Rim width | 1.9dp parameter, near/far SDF falloff | device measurement |
| Rim shader | `GradientStrokeStyle.EDGE_STYLE_1` + `InnerShadowStyle.SHADOW_STYLE_1` (`160 × 160` presets) | `ShadowEdge` AGSL shader |
| Rim inner shadow | the perimeter effect preserves sampled surface RGB/alpha; no white inner-shadow overlay | device measurement |
| Fill | radial, darker centre / brighter edge; alpha ≈0.78 (two-point estimate) | device measurement |
| Fill, dark | `#1e1e1e` at r = 0 → `#242424` at r = 70px | device measurement |
| Fill, light | 249 at r = 0 → 251 at r = 68px | device measurement |
| Glow, dark | level-2: bottom=4.899994dp, sdf=9.22998dp; factors=0.6/0.6, speed=18; color=0x33ffffff | ToolbarMaterialEffectDelegate + COUICausticShadowEffect + libhwui |
| Halo, light | near-symmetric soft dark drop shadow, radius ≈60px ≈ 15.5dp, deepest −13 luma and bottom-weighted (bottom −13 / sides −11 / top −8) | device measurement |
| Press | the whole control scales to ≈0.89 (radius 77.5px → 69px); the rim and the glow fade out, the fill and the icon brighten | device measurement |
| Press spring | `folmeSpring(damping = 1f, response = 0.3f, visibilityThreshold = 0.001f)` | `COUISpringForce`, the library's press-feedback spring |

## Not Ported / Deviations

- **The toolbar's own attribute set** — the parameters are marked device-derived because the toolbar view's `ShadowEdge` uniforms (`u_edgeArray` / `u_commonArray` components) are not known; only the `EDGE_STYLE_1` / `SHADOW_STYLE_1` presets and the measured values above are used.
- **The `ShadowEdge` shader's own halo** — the shader only covers the node's own bounds, so the fill and the halo are painted by this component instead; the rim is still the shader.
- **The toolbar's layout** — the 40dp circle's distance from the toolbar's start edge and its vertical centring are the toolbar's business, not this button's.
