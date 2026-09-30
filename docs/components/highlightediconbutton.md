# HighlightedIconButton

`HighlightedIconButton` is the ColorOS 17 toolbar icon button with its light halo, the port of the effect ColorOS puts on the toolbar's back key and overflow key.

ColorOS gives both keys the same treatment: a **true 40dp circle** — not a squircle — carrying a radial fill that is darker in the middle and brighter at the rim, an angle-distributed white perimeter stroke, and a theme-dependent halo outside the edge. While pressed the whole control springs to `HighlightedIconButtonDefaults.PressedScale`, the rim and the halo disappear, and the fill and the icon brighten. Both themes behave identically.

The halo is painted outside the node's diameter, so nothing in the caller's chain may clip it (no `clip`, no clip-shaped parent).

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.HighlightedIconButton
import io.github.suqi8.coui.kmp.basic.HighlightedIconButtonDefaults
```

## Native Origin

The effect belongs to the COUI toolbar library's `coui_toolbar_back_view` / `toolbar_more_view` rather than to a standalone component class, and **that view's attribute set has not been located in decompilation yet**, so the numbers below come from device measurement — UI dump plus pixel sampling of the real device — and from the `ShadowEdge` AGSL shader, and are marked as device-derived rather than decompiled.

The rim is `Modifier.materialShadowEdge`, the port of that `ShadowEdge` runtime shader, whose `u_edgeArray[13]` angle is what distributes the stroke alpha around the perimeter: strong at the top and bottom, nearly invisible at the left and right. The colours, the halo and the fill are not part of the shader — it only covers the node's own bounds — so they are painted in this component's own draw scope and the shader is layered on top.

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
| StrokeAlpha           | Float                  | The rim alpha                                                           | 0.15f |
| StrokeWidth           | Dp                     | The rim width                                                           | 1.dp |
| StrokeShadowFadeIn    | Float                  | The rim's inner-shadow fade-in scale                                    | 0.01f |
| Stroke                | MaterialStrokeParams   | The rim: `StrokeAlpha` white, `StrokeWidth` wide, no inner shadow       | — |
| DarkHaloColor         | Color                  | The dark theme's glow colour                                            | white @ 8% |
| DarkHaloExtent        | Dp                     | How far the dark theme's glow reaches past the edge                     | 5.2.dp |
| DarkHaloStartAngle    | Float                  | The glow arc's start angle; 0° is 3 o'clock, so 90° points down          | 60f |
| DarkHaloSweepAngle    | Float                  | The glow arc's sweep                                                    | 60f |
| LightHaloColor        | Color                  | The light theme's halo colour                                           | black @ 5% |
| LightHaloRadius       | Dp                     | How far the light theme's halo reaches past the edge                    | 15.5.dp |
| LightHaloOffsetY      | Dp                     | The light theme's halo vertical offset                                  | 1.dp |

`fillCenter(isDark)` and `fillEdge(isDark)` resolve the two fill colours for the current theme. `PressedFillBrighten` and `PressedContentBrighten` are nominal: the measurement records that the fill and the icon brighten while pressed but carries no magnitude, so these two values are not device-derived.

## Recovered Tokens

Every value below is measured on the device (UI dump plus pixel sampling, 1dp = 3.875px) or taken from the `ShadowEdge` shader; **none of it is decompiled from the toolbar's own attribute set**, which has not been located. The two layers are stated separately on purpose.

| Token | Value | Layer |
| :--- | :--- | :--- |
| Diameter | 155px at 1dp = 3.875px, i.e. 40dp; a true circle, not a squircle | device measurement |
| Rim | ≈15% white, angle-distributed — strong at the top and bottom, nearly invisible at the left and right | device measurement |
| Rim width | ≈0.8dp at the top, ≈1.2dp at the bottom | device measurement |
| Rim shader | `GradientStrokeStyle.EDGE_STYLE_1` + `InnerShadowStyle.SHADOW_STYLE_1` (`160 × 160` presets) | `ShadowEdge` AGSL shader |
| Rim inner shadow | none — the shader's fade is kept at the `TYPE_FRAMEWORK_CAPSULE_13` dark value, where it is invisible | device measurement |
| Fill | radial, darker centre / brighter edge; alpha ≈0.78 (two-point estimate) | device measurement |
| Fill, dark | `#1e1e1e` at r = 0 → `#242424` at r = 70px | device measurement |
| Fill, light | 249 at r = 0 → 251 at r = 68px | device measurement |
| Glow, dark | lower arc only (60°–120°), R + 20px ≈ 5.2dp, ≈8% white 1px outside the edge | device measurement |
| Halo, light | near-symmetric soft dark drop shadow, radius ≈60px ≈ 15.5dp, deepest −13 luma and bottom-weighted (bottom −13 / sides −11 / top −8) | device measurement |
| Press | the whole control scales to ≈0.89 (radius 77.5px → 69px); the rim and the glow fade out, the fill and the icon brighten | device measurement |
| Press spring | `folmeSpring(damping = 1f, response = 0.3f, visibilityThreshold = 0.001f)` | `COUISpringForce`, the library's press-feedback spring |

## Not Ported / Deviations

- **The toolbar's own attribute set** — the parameters are marked device-derived because `coui_toolbar_back_view`'s `ShadowEdge` uniforms (`u_edgeArray` / `u_commonArray` components) have not been decompiled; only the `EDGE_STYLE_1` / `SHADOW_STYLE_1` presets and the measured values above are used.
- **The `ShadowEdge` shader's own halo** — the shader only covers the node's own bounds, so the fill and the halo are painted by this component instead; the rim is still the shader.
- **The toolbar's layout** — the 40dp circle's distance from the toolbar's start edge and its vertical centring are the toolbar's business, not this button's.
