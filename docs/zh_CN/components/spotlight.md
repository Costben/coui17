---
title: SpotLight
requiresScaffoldHost: false
prerequisites:
  - 可以在任何地方使用，不需要 `Scaffold` 或 `COUIPopupHost`
  - 绘制在应用它的节点之上
hostComponent: None
popupHost: None
---

# SpotLight

`Modifier.spotLight` 即 ColorOS 17 高光，是 `COUISpotLightEffect` 及其 `SpotLightShader` 的可移植移植实现。它在节点范围内绘制一个真实光源：节点内归一化的位置、光衰减到零的半径、强度，以及 `SpotLightParams` 的各预设字段。与径向渐变不同，光源位于表面上方一定高度，按 `(ambient + diffuse * lambert) * lightColor` 着色，因此光离开某处后那里会随之变暗。

ColorOS 由 `COUISpotLightEffect` 内部的手势与弹簧动画驱动光源。本移植只保留绘制层：由调用方驱动 `position`、`radius` 与 `intensity`，通常使用 `SpotLightDefaults` 导出的弹簧。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.SpotLightDefaults
import io.github.suqi8.coui.kmp.basic.SpotLightParams
import io.github.suqi8.coui.kmp.basic.spotLight
```

## 基本用法

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

`position` 是节点内 0..1 的归一化坐标，与 `COUISpotLightEffectDrawable.setLightPositionByPointerIndex` 一致——后者把触点除以 `mBounds.width()` / `mBounds.height()`。光源绘制在节点已经绘制的内容之后，因此把它放在 `background` / `clip` 之后，是提亮表面而不是覆盖表面。

## 选择预设

`COUISpotLightEffect` 的 12 个预设以 `SpotLightParams` 暴露，同时提供 `getParamsByType` 使用的日夜模式解析方法：

```kotlin
val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
val params = SpotLightDefaults.capsuleParams1(isDark)
```

预设中的半径与光源高度是原始的分片空间数值：原生由 drawable 边界的像素值设置 shader 分辨率，并把半径原样折入 `u_lightPosAndHeight.z`，因此这些字段是与同一像素空间中的距离比较的。`Modifier.spotLight` 的 `radius` 与 `lightHeight` 以 `Dp` 计，需要按密度换算预设的原始值：

```kotlin
val density = LocalDensity.current
val radius = with(density) { params.endRadius.toDp() }
val lightHeight = with(density) { params.lightHeight.toDp() }
```

## 动画光源

`COUISpotLightEffect` 随着进度在 `mStartIntensity` → `mEndIntensity`、`mStartRadius` → `mEndRadius` 之间插值，并用跟随手指的弹簧移动位置。在本修饰符之上，同一套动画只需几行：

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

若要由手指驱动，可在 `Modifier.pointerInput` 中更新 `position`，并用 `SpotLightDefaults.MoveSpring` 收敛。

## 属性

### Modifier.spotLight

| 属性            | 类型      | 说明                                        | 默认值                            | 必需 |
| --------------- | --------- | ------------------------------------------- | --------------------------------- | ---- |
| position        | Offset    | 光源位置，节点内 0..1 归一化               | -                                 | 是   |
| radius          | Dp        | 光源衰减到零的半径                          | -                                 | 是   |
| intensity       | Float     | 强度倍率                                    | -                                 | 是   |
| modifier        | Modifier  | 应用于光源层之外的修饰符                    | Modifier                          | 否   |
| lightColor      | Color     | 光源颜色                                    | Color.White                       | 否   |
| lightHeight     | Dp        | 光源高出表面的高度                          | SpotLightDefaults.LightHeight.dp  | 否   |
| ambientStrength | Float     | 环境光项                                    | SpotLightDefaults.AmbientStrength | 否   |
| diffuseStrength | Float     | 漫反射项                                    | SpotLightDefaults.DiffuseStrength | 否   |
| noise           | Float     | 噪声量（`noise_exp`），0 关闭噪声处理       | 0f                                | 否   |
| dither          | Float     | 抖动量，0 关闭抖动处理                      | 0f                                | 否   |
| blendMode       | BlendMode | 光源与表面的混合方式                        | BlendMode.SrcOver                 | 否   |
| enabled         | Boolean   | 是否绘制光源                                | true                              | 否   |

### SpotLightParams

| 属性            | 类型      | 说明                                                       |
| --------------- | --------- | ---------------------------------------------------------- |
| blendMode       | BlendMode | `mBlendMode`，原生的 `OVERLAY` / `PLUS` / `SRC_OVER`       |
| startIntensity  | Float     | `mStartIntensity`                                          |
| endIntensity    | Float     | `mEndIntensity`                                            |
| startRadius     | Float     | `mStartRadius`，原始分片空间数值                           |
| endRadius       | Float     | `mEndRadius`，原始分片空间数值                             |
| lightHeight     | Float     | `mLightHeight`，原始分片空间数值                           |
| ambientStrength | Float     | `mAmbientStrength`                                         |
| diffuseStrength | Float     | `mDiffuseStrength`                                         |
| ditherN         | Float     | `mDitherN`                                                 |
| noise           | Float     | `mNoise`，即 shader 中的 `noise_exp`                       |

### SpotLightDefaults

| 常量                  | 类型                    | 取值                            | COUI 来源                                                   |
| --------------------- | ----------------------- | ------------------------------- | ----------------------------------------------------------- |
| LightHeight           | Float                   | 200f                            | `SpotLightShader.mLightHeight`                              |
| AmbientStrength       | Float                   | 0.1f                            | `SpotLightShader.mAmbientStrength`                          |
| DiffuseStrength       | Float                   | 0.5f                            | `SpotLightShader.DEFAULT_DIFFUSE_STRENGTH`                  |
| LightRadius           | Float                   | 200f                            | `SpotLightShader.mLightRadius`                              |
| Level                 | Float                   | 1f                              | `SpotLightShader.mLevel`                                    |
| ExtinguishExtraRadius | Float                   | 800f                            | `COUISpotLightEffect.mDiffuseExtinguishExtraRadius`（400*2） |
| GlowSpring            | SpringSpec&lt;Float&gt; | response 0.15 / bounce 0        | `LIGHT_GLOW_RESPONSE` / `LIGHT_GLOW_BOUNCE`                  |
| ExtinguishSpring      | SpringSpec&lt;Float&gt; | response 0.4 / bounce 0         | `LIGHT_EXTINGUISH_RESPONSE` / `LIGHT_EXTINGUISH_BOUNCE`      |
| MoveSpring            | SpringSpec&lt;Float&gt; | response 0.35 / bounce 0.15     | `LIGHT_MOVE_RESPONSE` / `LIGHT_MOVE_BOUNCE`                  |

`MoveSpring` 在 `LIGHT_MOVE_MIN_VISIBLE_CHANGE`（`1.0E-4f`）处截断；发光与熄灭弹簧没有记录阈值，使用框架默认值。各弹簧遵循本库 `folmeSpring` 的约定：`damping = 1 - bounce`。

## 预设

`COUISpotLightEffect.SpotLightParams` 字段顺序：混合模式、起始 / 结束强度、起始 / 结束半径、光源高度、环境光、漫反射、`ditherN`、`noise`。

| 常量                  | 混合     | 起始强度 | 结束强度 | 起始半径 | 结束半径 | 光源高度 | 环境光 | 漫反射 | DitherN | Noise |
| --------------------- | -------- | -------- | -------- | -------- | -------- | -------- | ------ | ------ | ------- | ----- |
| CapsuleParams1Light   | OVERLAY  | 0        | 1.0      | 100      | 400      | 80       | 0.1    | 0.5    | 0       | 0     |
| CapsuleParams1Dark    | OVERLAY  | 0        | 1.0      | 100      | 400      | 80       | 0.1    | 0.5    | 0       | 0     |
| CapsuleParams2Light   | OVERLAY  | 0        | 1.0      | 100      | 500      | 200      | 0.4    | 0.4    | 1.0     | 0.3   |
| CapsuleParams2Dark    | PLUS     | 0        | 0.15     | 100      | 500      | 200      | 0.4    | 0.4    | 0       | 0.1   |
| CircleParams1Light    | PLUS     | 0        | 0.2      | 100      | 600      | 200      | 0.1    | 0.5    | 0       | 0     |
| CircleParams1Dark     | PLUS     | 0        | 0.2      | 100      | 600      | 200      | 0.1    | 0.5    | 0       | 0     |
| RoundRectParams1Light | PLUS     | 0        | 1.3      | 100      | 600      | 120      | 0.2    | 0.2    | 1.0     | 0.08  |
| RoundRectParams1Dark  | PLUS     | 0        | 0.4      | 0        | 600      | 100      | 0.1    | 0.5    | 1.0     | 0.02  |
| SwitchParamsLight     | OVERLAY  | 0        | 0.8      | 100      | 200      | 100      | 0.1    | 0.5    | 0       | 0     |
| SwitchParamsDark      | OVERLAY  | 0        | 0.8      | 100      | 200      | 100      | 0.1    | 0.5    | 0       | 0     |
| ChipTabParamsLight    | PLUS     | 0        | 0.2      | 100      | 600      | 200      | 0.1    | 0.5    | 0       | 0     |
| ChipTabParamsDark     | SRC_OVER | 0        | 0.3      | 100      | 600      | 200      | 0.1    | 0.5    | 0       | 0     |

日夜模式解析方法：`capsuleParams1(isDark)`、`capsuleParams2(isDark)`、`circleParams1(isDark)`、`roundRectParams1(isDark)`、`switchParams(isDark)`、`chipTabParams(isDark)`。

## Shader Uniforms

shader 为 `com.coui.appcompat.spotlight.agsl.SpotLight`（`AGSL_SOURCE`，`NAME = "SpotLight"`），由 `RuntimeShader` 创建。

| Uniform                  | 打包方式                                                     | 写入方                                               |
| ------------------------ | ------------------------------------------------------------ | ---------------------------------------------------- |
| `u_lightPosAndHeight`    | `(pos.x * resW, pos.y * resH, radius * level, intensity * level)` | `setLightPosition`、`setLightRadius`、`setIntensity` |
| `u_lightColorAndAmbient` | `(r, g, b, ambientStrength)`                                | `setLightColor`、`setAmbientStrength`                |
| `u_lightingParams`       | `(diffuseStrength, lightHeight, noise_exp, ditherN)`        | `setDiffuseStrength`、`setLightHeight`、`setNoiseExp`、`setDitherN` |
| `resolution`             | `(w, h)`                                                     | `setResolution`                                      |

`mLevel` 同时缩放半径与强度，`main` 内固定的 `lightIntensity = 1.5` 再对光照缩放一次。Compose 的 `BlendMode.Overlay` / `BlendMode.Plus` / `BlendMode.SrcOver` 对应原生的 `OVERLAY` / `PLUS` / `SRC_OVER` 画笔混合模式。

## 未移植 / 差异

- 28 个取值的 `SpotLightType` 枚举与 `getParamsByType`。该表已记录在逆向报告中，但本库没有任何消费者按类型选择高光，因此只暴露 12 个预设及其日夜解析方法。另需说明：出厂的 `getParamsByType` 在 case 19–24 上错位一位（`TYPE_SEGMENT_BUTTON` 拿到 switch 预设，`TYPE_CAPSULE_1` 拿到 `CAPSULE_PARAMS_2` 等）；本移植有意不复现该行为，因为它只有通过本移植并不提供的枚举才会显现。
- `@Deprecated("Deprecated since 17.0.14")` 的空实现 setter：`setEnableHDR`、`setFalloffExponent`、`setFalloffType`、`setShininess`、`setSpecularStrength`、`setLightIntensity`。它们在 ColorOS 17 中不写入任何内容，无内容可移植。
- `COUISpotLightEffectDrawable` 与 `SpotLightEffectRenderer`：本移植是 `Modifier`，因此 `Drawable` 包装、其按压状态处理以及 seekbar 渲染器均不在范围内。
- 触摸手势驱动——`glow` / `extinguish` / `animateToPosition` 及其 `COUISpringAnimation` 接线。弹簧由 `SpotLightDefaults` 导出，由调用方驱动 `position`、`radius` 与 `intensity`；熄灭时的额外半径常量仅作记录，不会自动应用。
- `LIGHT_ANIMATION_RANGE`（`10000.0f`）与 `normalizedProgressScope`，即 seekbar 的进度映射；本库没有消费者上报归一化进度。
- 本移植忠于 AGSL 源串，但未在运行时验证：只确认了编译（Android 目标），shader 未在真机上跑过。平台门限为 `isRuntimeShaderSupported()`，即 `SDK_INT >= 33`；平台拒绝该源串时回退为不绘制光源，而不是抛异常。
