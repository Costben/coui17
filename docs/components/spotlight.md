---
title: SpotLight
requiresScaffoldHost: false
prerequisites:
  - Can be used anywhere, does not require `Scaffold` or `COUIPopupHost`
  - Paints over the node it is applied to
hostComponent: None
popupHost: None
---

# SpotLight

`Modifier.spotLight` is the ColorOS 17 spotlight, the portable port of `COUISpotLightEffect` and its `SpotLightShader`. It paints a real light source over the node's bounds: a position normalised inside the node, a radius at which the falloff dies, an intensity, and the preset fields of a `SpotLightParams`. Unlike a radial gradient, the light sits at a height above the surface and shades with `(ambient + diffuse * lambert) * lightColor`, so the edges of the lit area darken as the light moves away from them.

ColorOS drives the light from touch gestures and spring animations inside `COUISpotLightEffect`. This port keeps only the layer: the caller animates `position`, `radius` and `intensity`, normally on the springs exported by `SpotLightDefaults`.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.SpotLightDefaults
import io.github.suqi8.coui.kmp.basic.SpotLightParams
import io.github.suqi8.coui.kmp.basic.spotLight
```

## Basic Usage

```kotlin
val params = SpotLightDefaults.CapsuleParams1Light

Box(
    modifier = Modifier
        .size(200.dp)
        .clip(RoundedCornerShape(20.dp))
        .background(Color(0xFF3A3A3F))
        .spotLight(
            position = Offset(0.5f, 0.5f),
            radius = 120.dp,
            intensity = params.endIntensity,
            lightHeight = 40.dp,
            ambientStrength = params.ambientStrength,
            diffuseStrength = params.diffuseStrength,
            blendMode = params.blendMode,
        ),
)
```

`position` is normalised 0..1 inside the node, exactly like `COUISpotLightEffectDrawable.setLightPositionByPointerIndex`, which divides the touch point by `mBounds.width()` / `mBounds.height()`. The light is drawn after whatever the node has already painted, so put it after `background` / `clip` in the chain to brighten the surface rather than replace it.

## Picking a Preset

The 12 presets of `COUISpotLightEffect` are exposed as `SpotLightParams` values, together with the night-mode resolvers that `getParamsByType` uses:

```kotlin
val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
val params = SpotLightDefaults.capsuleParams1(isDark)
```

The radii and the light height of a preset are raw fragment-space values: the native sets the shader resolution from the drawable bounds in pixels and folds the radius into `u_lightPosAndHeight.z` unchanged, so those fields are compared against distances measured in those pixels. `Modifier.spotLight` takes `radius` and `lightHeight` in `Dp`, so convert a preset's raw value with the density:

```kotlin
val density = LocalDensity.current
val radius = with(density) { params.endRadius.toDp() }
val lightHeight = with(density) { params.lightHeight.toDp() }
```

## Animated Light

`COUISpotLightEffect` lerps `mStartIntensity` → `mEndIntensity` and `mStartRadius` → `mEndRadius` as its progress moves, and moves the position with a follow-the-finger spring. The same animation is a few lines on top of this modifier:

```kotlin
val transition = rememberInfiniteTransition()
val sweep by transition.animateFloat(
    initialValue = 0.2f,
    targetValue = 0.8f,
    animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
)
val glow by transition.animateFloat(
    initialValue = params.startIntensity,
    targetValue = params.endIntensity,
    animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
)

Box(
    modifier = Modifier
        .fillMaxWidth()
        .height(120.dp)
        .background(surface)
        .spotLight(
            position = Offset(sweep, 0.5f),
            radius = radius,
            intensity = glow,
            blendMode = params.blendMode,
        ),
)
```

For a finger-driven light, drive `position` from `Modifier.pointerInput` and settle on `SpotLightDefaults.MoveSpring`.

## Properties

### Modifier.spotLight

| Property          | Type           | Description                                          | Default Value                     | Required |
| ----------------- | -------------- | ---------------------------------------------------- | --------------------------------- | -------- |
| position          | Offset         | Light position, normalised 0..1 inside the node       | -                                 | Yes      |
| radius            | Dp             | Radius at which the light dies out                    | -                                 | Yes      |
| intensity         | Float          | Intensity multiplier                                  | -                                 | Yes      |
| modifier          | Modifier       | Modifiers applied outside the light layer             | Modifier                          | No       |
| lightColor        | Color          | Light colour                                          | Color.White                       | No       |
| lightHeight       | Dp             | Height of the light above the surface                 | SpotLightDefaults.LightHeight.dp  | No       |
| ambientStrength   | Float          | Ambient term                                          | SpotLightDefaults.AmbientStrength | No       |
| diffuseStrength   | Float          | Diffuse term                                          | SpotLightDefaults.DiffuseStrength | No       |
| noise             | Float          | Noise amount (`noise_exp`); 0 disables the noise pass | 0f                                | No       |
| dither            | Float          | Dither amount; 0 disables the dither pass             | 0f                                | No       |
| blendMode         | BlendMode      | How the light composites onto the surface             | BlendMode.SrcOver                 | No       |
| enabled           | Boolean        | Whether the light is painted at all                   | true                              | No       |

### SpotLightParams

| Property        | Type      | Description                                             |
| --------------- | --------- | ------------------------------------------------------- |
| blendMode       | BlendMode | `mBlendMode`, the native `OVERLAY` / `PLUS` / `SRC_OVER` |
| startIntensity  | Float     | `mStartIntensity`                                       |
| endIntensity    | Float     | `mEndIntensity`                                         |
| startRadius     | Float     | `mStartRadius`, a raw fragment-space value              |
| endRadius       | Float     | `mEndRadius`, a raw fragment-space value                |
| lightHeight     | Float     | `mLightHeight`, a raw fragment-space value              |
| ambientStrength | Float     | `mAmbientStrength`                                      |
| diffuseStrength | Float     | `mDiffuseStrength`                                      |
| ditherN         | Float     | `mDitherN`                                              |
| noise           | Float     | `mNoise`, which is the shader's `noise_exp`             |

### SpotLightDefaults

| Constant               | Type                | Value                              | COUI Source                                                |
| ---------------------- | ------------------- | ---------------------------------- | ---------------------------------------------------------- |
| LightHeight            | Float               | 200f                               | `SpotLightShader.mLightHeight`                             |
| AmbientStrength        | Float               | 0.1f                               | `SpotLightShader.mAmbientStrength`                         |
| DiffuseStrength        | Float               | 0.5f                               | `SpotLightShader.DEFAULT_DIFFUSE_STRENGTH`                 |
| LightRadius            | Float               | 200f                               | `SpotLightShader.mLightRadius`                             |
| Level                  | Float               | 1f                                 | `SpotLightShader.mLevel`                                   |
| ExtinguishExtraRadius  | Float               | 800f                               | `COUISpotLightEffect.mDiffuseExtinguishExtraRadius` (400*2) |
| GlowSpring             | SpringSpec&lt;Float&gt; | response 0.15 / bounce 0       | `LIGHT_GLOW_RESPONSE` / `LIGHT_GLOW_BOUNCE`                 |
| ExtinguishSpring       | SpringSpec&lt;Float&gt; | response 0.4 / bounce 0        | `LIGHT_EXTINGUISH_RESPONSE` / `LIGHT_EXTINGUISH_BOUNCE`     |
| MoveSpring             | SpringSpec&lt;Float&gt; | response 0.35 / bounce 0.15    | `LIGHT_MOVE_RESPONSE` / `LIGHT_MOVE_BOUNCE`                 |

`MoveSpring` is cut at `LIGHT_MOVE_MIN_VISIBLE_CHANGE` (`1.0E-4f`); the glow and extinguish springs have no recorded threshold, so they use the framework default. The springs follow the library's `folmeSpring` convention, `damping = 1 - bounce`.

## Presets

`COUISpotLightEffect.SpotLightParams`: blend mode, start / end intensity, start / end radius, light height, ambient, diffuse, `ditherN`, `noise`.

| Constant              | Blend    | Start int. | End int. | Start r. | End r. | Light height | Ambient | Diffuse | DitherN | Noise |
| --------------------- | -------- | ---------- | -------- | -------- | ------ | ------------ | ------- | ------- | ------- | ----- |
| CapsuleParams1Light   | OVERLAY  | 0          | 1.0      | 100      | 400    | 80           | 0.1     | 0.5     | 0       | 0     |
| CapsuleParams1Dark    | OVERLAY  | 0          | 1.0      | 100      | 400    | 80           | 0.1     | 0.5     | 0       | 0     |
| CapsuleParams2Light   | OVERLAY  | 0          | 1.0      | 100      | 500    | 200          | 0.4     | 0.4     | 1.0     | 0.3   |
| CapsuleParams2Dark    | PLUS     | 0          | 0.15     | 100      | 500    | 200          | 0.4     | 0.4     | 0       | 0.1   |
| CircleParams1Light    | PLUS     | 0          | 0.2      | 100      | 600    | 200          | 0.1     | 0.5     | 0       | 0     |
| CircleParams1Dark     | PLUS     | 0          | 0.2      | 100      | 600    | 200          | 0.1     | 0.5     | 0       | 0     |
| RoundRectParams1Light | PLUS     | 0          | 1.3      | 100      | 600    | 120          | 0.2     | 0.2     | 1.0     | 0.08  |
| RoundRectParams1Dark  | PLUS     | 0          | 0.4      | 0        | 600    | 100          | 0.1     | 0.5     | 1.0     | 0.02  |
| SwitchParamsLight     | OVERLAY  | 0          | 0.8      | 100      | 200    | 100          | 0.1     | 0.5     | 0       | 0     |
| SwitchParamsDark      | OVERLAY  | 0          | 0.8      | 100      | 200    | 100          | 0.1     | 0.5     | 0       | 0     |
| ChipTabParamsLight    | PLUS     | 0          | 0.2      | 100      | 600    | 200          | 0.1     | 0.5     | 0       | 0     |
| ChipTabParamsDark     | SRC_OVER | 0          | 0.3      | 100      | 600    | 200          | 0.1     | 0.5     | 0       | 0     |

Night-mode resolvers: `capsuleParams1(isDark)`, `capsuleParams2(isDark)`, `circleParams1(isDark)`, `roundRectParams1(isDark)`, `switchParams(isDark)`, `chipTabParams(isDark)`.

## Shader Uniforms

The shader is `com.coui.appcompat.spotlight.agsl.SpotLight` (`AGSL_SOURCE`, `NAME = "SpotLight"`), created with `RuntimeShader`.

| Uniform                 | Packing                                                      | Written by                                          |
| ----------------------- | ------------------------------------------------------------ | --------------------------------------------------- |
| `u_lightPosAndHeight`   | `(pos.x * resW, pos.y * resH, radius * level, intensity * level)` | `setLightPosition`, `setLightRadius`, `setIntensity` |
| `u_lightColorAndAmbient`| `(r, g, b, ambientStrength)`                                 | `setLightColor`, `setAmbientStrength`               |
| `u_lightingParams`      | `(diffuseStrength, lightHeight, noise_exp, ditherN)`         | `setDiffuseStrength`, `setLightHeight`, `setNoiseExp`, `setDitherN` |
| `resolution`            | `(w, h)`                                                     | `setResolution`                                     |

`mLevel` scales both the radius and the intensity, and the fixed `lightIntensity = 1.5` inside `main` scales the lighting again. Compose's `BlendMode.Overlay` / `BlendMode.Plus` / `BlendMode.SrcOver` stand in for the native `OVERLAY` / `PLUS` / `SRC_OVER` paint blend modes.

## Not Ported / Deviations

- The 28-value `SpotLightType` enum and `getParamsByType`. The table is recorded in the reverse-engineering report, but no consumer in this library selects a spotlight by type, so only the 12 presets and their night-mode resolvers are exposed. Note that the shipped `getParamsByType` is off by one for cases 19–24 (`TYPE_SEGMENT_BUTTON` receives the switch preset, `TYPE_CAPSULE_1` receives `CAPSULE_PARAMS_2`, …); that behaviour is intentionally not reproduced, because reproducing it would only matter through the enum this port does not have.
- The `@Deprecated("Deprecated since 17.0.14")` no-op setters: `setEnableHDR`, `setFalloffExponent`, `setFalloffType`, `setShininess`, `setSpecularStrength` and `setLightIntensity`. They write nothing in ColorOS 17, so there is nothing to port.
- `COUISpotLightEffectDrawable` and `SpotLightEffectRenderer`: this port is a `Modifier`, so the `Drawable` wrapper, its press-state handling and the seekbar renderer are out of scope.
- The touch-gesture driver — `glow` / `extinguish` / `animateToPosition` and the `COUISpringAnimation` wiring. The springs are exported by `SpotLightDefaults` and the caller animations `position`, `radius` and `intensity`; the extinguish overshoot constant is documented but not applied for you.
- `LIGHT_ANIMATION_RANGE` (`10000.0f`) and `normalizedProgressScope`, the seekbar progress mapping; no consumer in this library reports a normalised progress.
- The port is source-faithful to the AGSL string but has not been verified at runtime: only that it compiles (the Android target) has been checked, and the shader has not been run on a device. The platform gate is `isRuntimeShaderSupported()`, i.e. `SDK_INT >= 33`, and a platform that rejects the source falls back to no light rather than throwing.
