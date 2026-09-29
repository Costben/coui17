---
title: MaterialShadowEdge
requiresScaffoldHost: false
prerequisites:
  - Can be used anywhere, does not require `Scaffold` or `COUIPopupHost`
  - Paints over the node it is applied to
hostComponent: None
popupHost: None
---

# MaterialShadowEdge

`Modifier.materialShadowEdge` draws the ColorOS 17 "material" edge — a gradient stroke plus an inner shadow — over the node's full bounds. It is the portable port of `com.coui.appcompat.shadowedge.COUIShadowEdgeDrawable` and its `ShadowEdgeShader`, which wrap a single runtime shader (`ShadowStroke`) and feed it the surface's own geometry, so the edge is computed per pixel from a signed distance field rather than stroked by the canvas.

Every material surface in ColorOS — the floating toolbars, the search bar, the bottom sheet panel, the popup frames — uses this one shader with a different `GradientStrokeStyle`, `InnerShadowStyle` and stroke width. `MaterialStrokeParams` holds the per-component stroke values; `GradientStrokeStyle` and `InnerShadowStyle` hold the shared rim and shadow shapes.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.GradientStrokeStyle
import io.github.suqi8.coui.kmp.basic.InnerShadowStyle
import io.github.suqi8.coui.kmp.basic.MaterialShadowEdgeDefaults
import io.github.suqi8.coui.kmp.basic.MaterialStrokeParams
import io.github.suqi8.coui.kmp.basic.materialShadowEdge
```

## Basic Usage

```kotlin
val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
val shape = RoundedCornerShape(50)

Box(
    modifier = Modifier
        .height(48.dp)
        .width(180.dp)
        .clip(shape)
        .background(surfaceColor)
        .materialShadowEdge(
            stroke = MaterialStrokeParams.frameworkCapsule13(isDark),
            fallback = Modifier.background(color = surfaceColor, shape = shape),
            edgeStyle = GradientStrokeStyle.EDGE_STYLE_0,
            shadowStyle = InnerShadowStyle.SHADOW_STYLE_0,
        ),
)
```

The modifier is drawn after whatever the node has already painted, so put it after `background` / `clip` to lay the rim over the surface. `fallback` is returned instead of the shader when the platform cannot run runtime shaders, which is why it is worth passing the node's own background there — without it an unsupported platform silently loses the surface.

The corners are a capsule by default: the modifier writes the same radius into all four slots of the shader's `u_commonArray`, derived from `height / 2`, because every consumer of `GradientStrokeStyle.EDGE_STYLE_0` here is a capsule. Pass `cornerRadius` for a surface whose corners are not a capsule.

## Properties

### Modifier.materialShadowEdge

| Property      | Type                 | Description                                                                 | Default Value                                  | Required |
| ------------- | -------------------- | --------------------------------------------------------------------------- | ---------------------------------------------- | -------- |
| stroke        | MaterialStrokeParams | Rim alpha / width / angle and the inner-shadow scales                        | -                                              | Yes      |
| fallback      | Modifier             | Used when the platform cannot run runtime shaders                            | Modifier                                       | No       |
| cornerWeight  | Float                | `u_commonArray[10]`, the corner weight                                       | MaterialShadowEdgeDefaults.CornerWeight        | No       |
| cornerRadius  | Dp?                  | Corner radius, or `null` for the capsule rule `height / 2`                    | null                                           | No       |
| edgeStyle     | GradientStrokeStyle  | The rim style                                                                 | GradientStrokeStyle.EDGE_STYLE_0               | No       |
| shadowStyle   | InnerShadowStyle     | The inner-shadow style                                                       | InnerShadowStyle.SHADOW_STYLE_0                | No       |
| antiAliasing  | Float                | `u_commonArray[11]`, the anti-aliasing band                                   | MaterialShadowEdgeDefaults.AntiAliasing        | No       |

### MaterialStrokeParams

| Property        | Type  | Description                                                                       |
| --------------- | ----- | --------------------------------------------------------------------------------- |
| edgeAlpha       | Float | `StrokeEffectParams.edgeAlpha`, the rim alpha                                     |
| edgeWidth       | Dp    | `StrokeEffectParams.edgeWidthDp`, the rim width; the native converts it with `dp2px` |
| edgeAngle       | Float | `StrokeEffectParams.edgeAngle`, 0 for every preset                                |
| shadowFadeIn    | Float | Mapped onto the shader's `setFadeAlpha(in, out)`                                   |
| shadowFadeOut   | Float | Mapped onto the shader's `setFadeAlpha(in, out)`                                   |
| shadowFadeScale | Float | Mapped onto the shader's `setFadeWidthScale`                                       |

Presets: `FrameworkCapsule5Light` / `FrameworkCapsule5Dark` (`FRAMEWORK_CAPSULE_PARAMS_5`, the framework edge-effect path) and `FrameworkCapsule13Light` / `FrameworkCapsule13Dark` (`FRAMEWORK_CAPSULE_PARAMS_13`, the runtime-shader `COUIShadowEdgeDrawable` path), with the resolvers `frameworkCapsule5(isDark)` and `frameworkCapsule13(isDark)`.

| Preset                 | edgeAlpha | edgeWidth | shadowFadeIn | shadowFadeOut | shadowFadeScale |
| ---------------------- | --------- | --------- | ------------ | ------------- | --------------- |
| FrameworkCapsule5Light | 0.2       | 2.3.dp    | 0.1          | 1.0           | 1.0             |
| FrameworkCapsule5Dark  | 0.8       | 2.5.dp    | 0.03         | 1.0           | 1.0             |
| FrameworkCapsule13Light| 0.8       | 2.3.dp    | 0.1          | 0.0           | 1.0             |
| FrameworkCapsule13Dark | 0.15      | 2.3.dp    | 0.01         | 0.0           | 1.0             |

### GradientStrokeStyle

`GradientStrokeStylePresets`: `EDGE_STYLE_0` (the capsule rim, `400 x 100`), `EDGE_STYLE_1` (the compact icon-button rim, `160 x 160`) and `EDGE_STYLE_2` (the large panel rim, `600 x 200`). The dark presets are byte-identical to the light ones in ColorOS, so there is no night variant here either.

| Style        | rectLineSize | rectLineColor | near  | far   | alpha   | fade[4]              | angle | p1 | p2  |
| ------------ | ------------ | ------------- | ----- | ----- | ------- | -------------------- | ----- | -- | --- |
| EDGE_STYLE_0 | 400, 100     | 1, 1, 1       | 2, 6  | 2, 3  | 0.7, 1.0 | 0.2, 0.4, 0.15, 0.35 | 0     | 4  | 0.8 |
| EDGE_STYLE_1 | 160, 160     | 1, 1, 1       | 2, 6  | 2, 3  | 0.7, 1.0 | 0.06, 0.4, 0.01, 0.28 | 0     | 4  | 0.8 |
| EDGE_STYLE_2 | 600, 200     | 1, 1, 1       | 2, 8  | 2, 2  | 0.7, 0.6 | 0.18, 0.35, 0.25, 0.25 | 0     | 4  | 0.8 |

### InnerShadowStyle

`InnerShadowStylePresets`: `SHADOW_STYLE_0` (the capsule inner shadow, `400 x 100`), `SHADOW_STYLE_1` (the compact icon button, `160 x 160`) and `SHADOW_STYLE_2` (the large panel, `600 x 600`). Dark presets are identical to light ones.

| Style          | fadeSize | fadeColor | fadeAlpha   | fadeOffset | fadeScale | fadeIn | inP1 | inP2 | fadeOut | outP1 | outP2 |
| -------------- | -------- | --------- | ----------- | ---------- | --------- | ------ | ---- | ---- | ------- | ----- | ----- |
| SHADOW_STYLE_0 | 400, 100 | 1, 1, 1   | 1.0, 0, 0   | 0, 20      | 1.3, 1.15 | 120    | 4    | 0.9  | 20      | 4     | 0.7   |
| SHADOW_STYLE_1 | 160, 160 | 1, 1, 1   | 0.4, 0, 0   | 0, 20      | 1.4, 1.35 | 140    | 4    | 0.9  | 20      | 4     | 0.7   |
| SHADOW_STYLE_2 | 600, 600 | 1, 1, 1   | 0.6, 0, 0   | 0, 10      | 1.1, 1.07 | 86     | 4    | 0.7  | 1       | 1     | 0     |

### MaterialShadowEdgeDefaults

| Constant               | Type  | Value | COUI Source                                                                                  |
| ---------------------- | ----- | ----- | -------------------------------------------------------------------------------------------- |
| AntiAliasing           | Float | 1f    | `ShadowEdgeShader.mCommonUniform[11]`, no consumer calls `setAntiAliasing`                    |
| CornerWeight           | Float | 1f    | `u_commonArray[10]` of a surface with more than one item                                      |
| SingleItemCornerWeight | Float | 2f    | `COUIBottomFloatingToolbarMenuView.SINGLE_ITEM_CORNER_WEIGHT`                                 |

## Shader Uniforms

The shader is `com.coui.appcompat.shadowedge.agsl.ShadowEdge` (`NAME = "ShadowStroke"`), built for the `NEW_G2` corner geometry that every consumer selects with `setCornerType(2)`.

| Uniform        | Layout                                                                                                              |
| -------------- | ------------------------------------------------------------------------------------------------------------------- |
| `u_commonArray`| `[resW, resH, centreX, centreY, sizeW, sizeH, radius(lb, rb, rt, lt), weight, antiAliasing]`                          |
| `u_shadow`     | `1` while the inner shadow is enabled                                                                                |
| `u_shadowArray`| `[fadeColor rgb, fadeAlpha in1, in2, out, fadeIn, fadeOffset xy, fadeScale xy, fadeInPara1/2, fadeOut, fadeOutPara1/2]` |
| `u_edge`       | `1` while the rim is enabled                                                                                         |
| `u_edgeArray`  | `[rectLineColor rgb, rectLineNear xy, rectLineFar xy, rectLineAlpha near/far, rectLineFade 4, angle, para1, para2]`  |

Both mappers reproduce the native accumulation steps exactly — `[4] += [3]`, `[6] += [5]`, `[10] += [9]`, `[12] += [11]` — and the `(lineWidth > 0 ? lineWidth : 7) / 7` line factor, where 7 is `coerceAtLeast(max(near.x + near.y, far.x + far.y), 1e-4f)`. The inner shadow is scaled by the native density factor `fMax = max(1, density) * 0.34f`. Uniforms are pushed as raw 0..1 floats with the plain float-array setters, never `setColorUniform`: AGSL reserves the colour setter for `layout(color)` uniforms, which SkSL rejects.

## Not Ported / Deviations

- The `COMMON`, `BEZIER` and `OLD_G2` corner geometries of `ShadowEdge.CornerType`. `setCornerType` is called with `2` everywhere in ColorOS, so only `NEW_G2` and its `smoothSDF_D` capsule distance are ported.
- `InnerShadowStyleMapper.toShadowArray16Direct` / `GradientStrokeStyleMapper.toEdgeArray16Direct`, the branch used when `setBusinessShadowEnable(false)` / `setBusinessEdgeEnable(false)`. It is identical except that it drops the `fadeWidthScale` and `fadeAlpha{In,Out}Scale` factors, and this port exposes no way to disable the business path.
- The `COUIShadowEdgeDrawable` wrapper: its `SRC_OVER` paint blend mode, its `Paint(true)` anti-aliasing flag and its `Drawable` state handling. This port is a `Modifier` and always composites the same way.
- The drawable only builds the shader when `Build.VERSION.SDK_INT > 33`, while this port uses the library's `isRuntimeShaderSupported()` gate (`SDK_INT >= 33`).
- The two mappers are `internal`; only the styles, the stroke params and the defaults are public API.
- The port is source-faithful to the AGSL fragments but has not been verified at runtime: only that it compiles (the Android target) has been checked, and the shader has not been run on a device. A platform that rejects the source falls back to the `fallback` modifier rather than throwing.
