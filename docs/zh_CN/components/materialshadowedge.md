---
title: MaterialShadowEdge
requiresScaffoldHost: false
prerequisites:
  - 可以在任何地方使用，不需要 `Scaffold` 或 `COUIPopupHost`
  - 绘制在应用它的节点之上
hostComponent: None
popupHost: None
---

# MaterialShadowEdge

`Modifier.materialShadowEdge` 在节点完整范围内绘制 ColorOS 17 的「材质」边缘——渐变描边加内阴影。它是该边缘的可移植实现：包裹同一个运行时 shader（`ShadowStroke`），并把表面自身的几何喂给它，因此边缘是由有符号距离场逐像素算出来的，而不是用画布描边画出来的。

ColorOS 中所有材质表面——悬浮工具栏、搜索栏、底部面板、弹窗边框——都用这一个 shader，只是 `GradientStrokeStyle`、`InnerShadowStyle` 与描边宽度不同。`MaterialStrokeParams` 承载按组件区分的描边数值；`GradientStrokeStyle` 与 `InnerShadowStyle` 承载共用的边缘与阴影形状。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.GradientStrokeStyle
import io.github.suqi8.coui.kmp.basic.InnerShadowStyle
import io.github.suqi8.coui.kmp.basic.MaterialShadowEdgeDefaults
import io.github.suqi8.coui.kmp.basic.MaterialStrokeParams
import io.github.suqi8.coui.kmp.basic.materialShadowEdge
```

## 基本用法

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

该修饰符绘制在节点已经绘制的内容之后，因此把它放在 `background` / `clip` 之后，边缘才会覆盖在表面之上。平台无法运行运行时 shader 时，返回的是 `fallback` 而非 shader，所以值得把节点自己的背景传进去——否则在不支持的平台上会静默丢掉表面。

默认情况下四角为胶囊：修饰符把 `height / 2` 得到的同一半径写入 shader `u_commonArray` 的四个槽位，因为本库中 `GradientStrokeStyle.EDGE_STYLE_0` 的消费者都是胶囊。若表面四角不是胶囊，请传入 `cornerRadius`。

## 属性

### Modifier.materialShadowEdge

| 属性         | 类型                 | 说明                                              | 默认值                                        | 必需 |
| ------------ | -------------------- | ------------------------------------------------- | --------------------------------------------- | ---- |
| stroke       | MaterialStrokeParams | 边缘 alpha / 宽度 / 角度与内阴影缩放              | -                                             | 是   |
| fallback     | Modifier             | 平台无法运行运行时 shader 时使用                  | Modifier                                      | 否   |
| cornerWeight | Float                | `u_commonArray[10]`，圆角权重                     | MaterialShadowEdgeDefaults.CornerWeight       | 否   |
| cornerRadius | Dp?                  | 圆角半径，`null` 表示使用胶囊规则 `height / 2`     | null                                          | 否   |
| edgeStyle    | GradientStrokeStyle  | 边缘样式                                          | GradientStrokeStyle.EDGE_STYLE_0              | 否   |
| shadowStyle  | InnerShadowStyle     | 内阴影样式                                        | InnerShadowStyle.SHADOW_STYLE_0               | 否   |
| antiAliasing | Float                | `u_commonArray[11]`，抗锯齿带宽度                 | MaterialShadowEdgeDefaults.AntiAliasing       | 否   |

### MaterialStrokeParams

| 属性            | 类型  | 说明                                                                     |
| --------------- | ----- | ------------------------------------------------------------------------ |
| edgeAlpha       | Float | `StrokeEffectParams.edgeAlpha`，边缘 alpha                               |
| edgeWidth       | Dp    | `StrokeEffectParams.edgeWidthDp`，边缘宽度；原生用 `dp2px` 换算           |
| edgeAngle       | Float | `StrokeEffectParams.edgeAngle`，所有预设均为 0                            |
| shadowFadeIn    | Float | 映射到 shader 的 `setFadeAlpha(in, out)`                                  |
| shadowFadeOut   | Float | 映射到 shader 的 `setFadeAlpha(in, out)`                                  |
| shadowFadeScale | Float | 映射到 shader 的 `setFadeWidthScale`                                      |

预设：`FrameworkCapsule5Light` / `FrameworkCapsule5Dark`（`FRAMEWORK_CAPSULE_PARAMS_5`，框架边缘特效路径）与 `FrameworkCapsule13Light` / `FrameworkCapsule13Dark`（`FRAMEWORK_CAPSULE_PARAMS_13`，运行时 shader 的 `COUIShadowEdgeDrawable` 路径），并配有 `frameworkCapsule5(isDark)` 与 `frameworkCapsule13(isDark)` 解析方法。

| 预设                    | edgeAlpha | edgeWidth | shadowFadeIn | shadowFadeOut | shadowFadeScale |
| ----------------------- | --------- | --------- | ------------ | ------------- | --------------- |
| FrameworkCapsule5Light  | 0.2       | 2.3.dp    | 0.1          | 1.0           | 1.0             |
| FrameworkCapsule5Dark   | 0.8       | 2.5.dp    | 0.03         | 1.0           | 1.0             |
| FrameworkCapsule13Light | 0.8       | 2.3.dp    | 0.1          | 0.0           | 1.0             |
| FrameworkCapsule13Dark  | 0.15      | 2.3.dp    | 0.01         | 0.0           | 1.0             |

### GradientStrokeStyle

`GradientStrokeStylePresets`：`EDGE_STYLE_0`（胶囊边缘，`400 x 100`）、`EDGE_STYLE_1`（紧凑图标按钮边缘，`160 x 160`）、`EDGE_STYLE_2`（大面板边缘，`600 x 200`）。ColorOS 中深色预设与浅色逐字节相同，因此这里也没有夜间变体。

| 样式         | rectLineSize | rectLineColor | near | far  | alpha    | fade[4]                | angle | p1 | p2  |
| ------------ | ------------ | ------------- | ---- | ---- | -------- | ---------------------- | ----- | -- | --- |
| EDGE_STYLE_0 | 400, 100     | 1, 1, 1       | 2, 6 | 2, 3 | 0.7, 1.0 | 0.2, 0.4, 0.15, 0.35   | 0     | 4  | 0.8 |
| EDGE_STYLE_1 | 160, 160     | 1, 1, 1       | 2, 6 | 2, 3 | 0.7, 1.0 | 0.06, 0.4, 0.01, 0.28  | 0     | 4  | 0.8 |
| EDGE_STYLE_2 | 600, 200     | 1, 1, 1       | 2, 8 | 2, 2 | 0.7, 0.6 | 0.18, 0.35, 0.25, 0.25 | 0     | 4  | 0.8 |

### InnerShadowStyle

`InnerShadowStylePresets`：`SHADOW_STYLE_0`（胶囊内阴影，`400 x 100`）、`SHADOW_STYLE_1`（紧凑图标按钮，`160 x 160`）、`SHADOW_STYLE_2`（大面板，`600 x 600`）。深色预设与浅色相同。

| 样式           | fadeSize | fadeColor | fadeAlpha | fadeOffset | fadeScale | fadeIn | inP1 | inP2 | fadeOut | outP1 | outP2 |
| -------------- | -------- | --------- | --------- | ---------- | --------- | ------ | ---- | ---- | ------- | ----- | ----- |
| SHADOW_STYLE_0 | 400, 100 | 1, 1, 1   | 1.0, 0, 0 | 0, 20      | 1.3, 1.15 | 120    | 4    | 0.9  | 20      | 4     | 0.7   |
| SHADOW_STYLE_1 | 160, 160 | 1, 1, 1   | 0.4, 0, 0 | 0, 20      | 1.4, 1.35 | 140    | 4    | 0.9  | 20      | 4     | 0.7   |
| SHADOW_STYLE_2 | 600, 600 | 1, 1, 1   | 0.6, 0, 0 | 0, 10      | 1.1, 1.07 | 86     | 4    | 0.7  | 1       | 1     | 0     |

### MaterialShadowEdgeDefaults

| 常量                   | 类型  | 取值 | COUI 来源                                                                  |
| ---------------------- | ----- | ---- | -------------------------------------------------------------------------- |
| AntiAliasing           | Float | 1f   | `ShadowEdgeShader.mCommonUniform[11]`，没有消费者调用 `setAntiAliasing`     |
| CornerWeight           | Float | 1f   | 多于一个元素时表面的 `u_commonArray[10]`                                     |
| SingleItemCornerWeight | Float | 2f   | `COUIBottomFloatingToolbarMenuView.SINGLE_ITEM_CORNER_WEIGHT`               |

## Shader Uniforms

shader 为 AGSL 运行时 shader（`NAME = "ShadowStroke"`），按所有消费者选用的 `NEW_G2` 圆角几何构建。

| Uniform         | 布局                                                                                                            |
| --------------- | --------------------------------------------------------------------------------------------------------------- |
| `u_commonArray` | `[resW, resH, centreX, centreY, sizeW, sizeH, radius(lb, rb, rt, lt), weight, antiAliasing]`                     |
| `u_shadow`      | 内阴影启用时为 `1`                                                                                              |
| `u_shadowArray` | `[fadeColor rgb, fadeAlpha in1, in2, out, fadeIn, fadeOffset xy, fadeScale xy, fadeInPara1/2, fadeOut, fadeOutPara1/2]` |
| `u_edge`        | 边缘启用时为 `1`                                                                                                |
| `u_edgeArray`   | `[rectLineColor rgb, rectLineNear xy, rectLineFar xy, rectLineAlpha near/far, rectLineFade 4, angle, para1, para2]` |

两个 mapper 逐字复现原生的累加步骤——`[4] += [3]`、`[6] += [5]`、`[10] += [9]`、`[12] += [11]`——以及 `(lineWidth > 0 ? lineWidth : 7) / 7` 的线宽系数，其中 7 为 `coerceAtLeast(max(near.x + near.y, far.x + far.y), 1e-4f)`。内阴影按原生密度因子 `fMax = max(1, density) * 0.34f` 缩放。所有 uniform 都以 0..1 的原始浮点数经 float 数组 setter 写入，从不使用 `setColorUniform`：AGSL 把颜色 setter 保留给 `layout(color)` uniform，而 SkSL 不接受该限定符。

## 未移植 / 差异

- `ShadowEdge.CornerType` 的 `COMMON`、`BEZIER` 与 `OLD_G2` 圆角几何。ColorOS 中 `setCornerType` 处处以 `2` 调用，因此只移植 `NEW_G2` 及其 `smoothSDF_D` 胶囊距离。
- `InnerShadowStyleMapper.toShadowArray16Direct` / `GradientStrokeStyleMapper.toEdgeArray16Direct`，即 `setBusinessShadowEnable(false)` / `setBusinessEdgeEnable(false)` 时使用的分支。它与常规分支唯一的差别是去掉 `fadeWidthScale` 与 `fadeAlpha{In,Out}Scale` 因子，而本移植没有提供关闭该路径的入口。
- `COUIShadowEdgeDrawable` 包装：其 `SRC_OVER` 画笔混合模式、`Paint(true)` 抗锯齿标志与 `Drawable` 状态处理。本移植是 `Modifier`，合成方式固定。
- drawable 仅在 `Build.VERSION.SDK_INT > 33` 时构建 shader，而本移植使用本库的 `isRuntimeShaderSupported()` 门限（`SDK_INT >= 33`）。
- 两个 mapper 为 `internal`；公开 API 只有样式、描边参数与默认值。
- 本移植忠于 AGSL 片段，但未在运行时验证：只确认了编译（Android 目标），shader 未在设备上跑过。平台拒绝该源串时会回退到 `fallback` 修饰符，而不是抛异常。
