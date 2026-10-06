# HighlightedIconButton

`HighlightedIconButton` 是 ColorOS 17 带光晕的工具栏图标按钮，对应 ColorOS 给工具栏返回键与三点菜单键施加的那套光效。

ColorOS 给这两个键做的是同一套处理：一个**正圆 40dp**——不是 squircle——配一层「中心暗、边缘亮」的径向填充、一圈按角度分布的白描边，以及边缘外侧随主题不同的光晕。按下时整块控件弹到 `HighlightedIconButtonDefaults.PressedScale`，描边与外光晕消失，填充与图标变亮；两个主题表现一致。

深色主题的下光晕直接调用 ColorOS 焦散阴影接口，使用 level-2 的偏移矩形距离场与纵向幂次衰减。其他平台使用还原的着色器公式；不再使用角度蒙版。光晕与按钮共用形变，按压时淡出。拖动使用系统 TouchMotion 的橡皮筋位移和面积守恒弹性形变，松手回弹；填充、图标、描边和外光晕共用变换。

光晕绘制在节点直径之外，因此调用方的修饰符链上不能有任何裁剪（不要 `clip`，也不要裁剪形状的父级）。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.HighlightedIconButton
import io.github.suqi8.coui.kmp.basic.HighlightedIconButtonDefaults
```

## 设备实现

该效果属于 COUI 工具栏库的工具栏视图，而不是一个独立的组件类；**该视图的属性表尚未定位**，因此下表的数值来自实测——UI dump 加像素采样——以及 `ShadowEdge` AGSL 着色器，标注为「实测所得」。

描边走 `Modifier.materialShadowEdge`，即该 `ShadowEdge` runtime shader 的移植；shader 的 `u_edgeArray[13]` 角度正是让描边透明度沿周长分布的原因：上下强、左右近乎不可见。颜色、光晕与填充不属于 shader（shader 只覆盖节点自身边界），因此由本组件在自己的绘制域里画，再把 shader 叠在最上层。

内容颜色通过 `LocalContentColor` 发布，因此 `content` 里默认的 `Icon` 会随填充一起变亮。

## 基本用法

```kotlin
HighlightedIconButton(onClick = { /* 处理点击 */ }) {
    Icon(imageVector = COUIIcons.Back, contentDescription = "返回")
}
```

需要在 RTL 布局下自动镜像的返回键是仓库 `example/shared` 里的 `BackNavigationIcon`，它在这个按钮内部用 `Modifier.graphicsLayer` 绘制箭头：

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

## 参数

### HighlightedIconButton 属性

| 属性名 | 类型                       | 说明                                                  | 默认值                             | 是否必须 |
| ------ | -------------------------- | ----------------------------------------------------- | ---------------------------------- | -------- |
| onClick | () -> Unit                | 点击时的回调                                          | -                                  | 是       |
| modifier | Modifier                 | 应用于按钮的修饰符                                    | Modifier                           | 否       |
| size   | Dp                         | 按钮直径                                              | HighlightedIconButtonDefaults.Size | 否       |
| enabled | Boolean                   | 是否可点击                                            | true                               | 否       |
| content | @Composable BoxScope.() -> Unit | 按钮内容，通常是 `Icon`，居中绘制在圆内          | -                                  | 是       |

点击不附带涟漪：高亮本身就是按压反馈，与设备一致。

### HighlightedIconButtonDefaults 对象

#### 常量

| 常量名                | 类型                   | 说明                                                       | 值 |
| --------------------- | ---------------------- | ---------------------------------------------------------- | -- |
| Size                  | Dp                     | 按钮直径                                                   | 40.dp |
| PressedScale          | Float                  | 按下时整块控件弹到的缩放                                   | 0.89f |
| PressSpring           | SpringSpec\<Float\>    | 按压弹簧                                                   | 响应 0.3 / 阻尼 1.0 |
| FillAlpha             | Float                  | 径向填充的绘制透明度                                       | 0.78f |
| FillEdgeStop          | Float                  | 径向填充到达边缘色的位置（半径比例）                       | 0.89f |
| FillCenterDark        | Color                  | 深色主题径向填充的中心色                                   | `#1E1E1E` |
| FillEdgeDark          | Color                  | 深色主题径向填充的边缘色                                   | `#242424` |
| FillCenterLight       | Color                  | 浅色主题径向填充的中心色                                   | `#F9F9F9` |
| FillEdgeLight         | Color                  | 浅色主题径向填充的边缘色                                   | `#FBFBFB` |
| PressedFillBrighten   | Float                  | 按压时填充向白色插值的比例                                 | 0.06f（名义值） |
| PressedContentBrighten| Float                  | 按压时内容色向白色插值的比例                               | 0.1f（名义值） |
| StrokeAlpha           | Float                  | 描边透明度                                                 | 0.6f（暗色）；`LightStrokeAlpha` 0.2f |
| StrokeWidth           | Dp                     | 描边宽度                                                   | 1.9.dp |
| StrokeShadowFadeIn    | Float                  | 描边内阴影的淡入比例                                       | 0.1f |
| Stroke                | MaterialStrokeParams   | 描边参数：`StrokeAlpha` 白、`StrokeWidth` 宽、无内阴影      | — |
| DarkHaloColor | Color | 系统深色焦散颜色 | white @ 20% |
| DarkHaloExtent | Dp | level-2 底边偏移 | 4.899994.dp |
| DarkHaloSdfWidth | Dp | level-2 距离场宽度 | 9.22998.dp |



| LightHaloColor        | Color                  | 浅色主题光晕的颜色                                         | 黑 @ 5% |
| LightHaloRadius       | Dp                     | 浅色主题光晕越过边缘的距离                                 | 15.5.dp |
| LightHaloOffsetY      | Dp                     | 浅色主题光晕的垂直偏移                                     | 1.9.dp |

`fillCenter(isDark)` 与 `fillEdge(isDark)` 按当前主题解析这两个填充色。`PressedFillBrighten` 与 `PressedContentBrighten` 是名义值：测量只记录了按压时填充与图标会变亮，没给出幅度，这两个值并非实测所得。

## 取值溯源

描边沿 `ToolbarMaterialEffectDelegate.applyStroke` → `ToolbarMaterialEffectResolver.getCircleStrokeEffectType` 使用普通顶栏 `TYPE_FRAMEWORK_CIRCLE_1`：暗色 alpha 0.6、浅色 0.2、宽 1.9dp。与底栏共用框架 `blendEdge` 的材质亮度混合，保留表面透明度，图标和外焦散光不参与该混合。填充和按压尺寸仍沿用此前实测。

| Token | 值 | 证据层 |
| :--- | :--- | :--- |
| 直径 | 155px（1dp = 3.875px），即 40dp；正圆，非 squircle | 设备实测 |
| 描边 | ≈15% 白，按角度分布——上下强、左右近乎不可见 | 设备实测 |
| 描边厚度 | 圆顶部 ≈0.8dp、底部 ≈1.2dp | 设备实测 |
| 描边 shader | `GradientStrokeStyle.EDGE_STYLE_1` + `InnerShadowStyle.SHADOW_STYLE_1`（`160 × 160` 预设） | `ShadowEdge` AGSL 着色器 |
| 描边内阴影 | 无——shader 的淡入沿用 `TYPE_FRAMEWORK_CAPSULE_13` 深色档的不可见值 | 设备实测 |
| 填充 | 径向，中心暗边缘亮；透明度 ≈0.78（两点估算） | 设备实测 |
| 填充·深色 | r = 0 处 `#1e1e1e` → r = 70px 处 `#242424` | 设备实测 |
| 填充·浅色 | r = 0 处 249 → r = 68px 处 251 | 设备实测 |
| 外辉光·深色 | level-2: bottom=4.899994dp, sdf=9.22998dp; factors=0.6/0.6, speed=18; color=0x33ffffff | ToolbarMaterialEffectDelegate + COUICausticShadowEffect + libhwui |
| 光晕·浅色 | 近对称的柔和暗投影，半径 ≈60px ≈ 15.5dp，最深 −13 luma 且底部加权（底 −13 / 左右 −11 / 上 −8） | 设备实测 |
| 按压态 | 整块控件缩放到 ≈0.89（半径 77.5px → 69px）；描边与辉光淡出，填充与图标变亮 | 设备实测 |
| 按压弹簧 | `folmeSpring(damping = 1f, response = 0.3f, visibilityThreshold = 0.001f)` | `COUISpringForce`，本库的按压反馈弹簧 |

## 未移植 / 偏差

- **工具栏自身的属性表** —— 因为工具栏视图的 `ShadowEdge` uniform（`u_edgeArray` / `u_commonArray` 各分量）未知，参数标注为实测所得；本移植只使用 `EDGE_STYLE_1` / `SHADOW_STYLE_1` 预设与上表的实测值。
- 外光晕独立调用系统焦散接口；`ShadowEdge` 负责内部边缘。非 ColorOS 平台的背景亮度衰减以主题背景为输入。
- **工具栏自身的布局** —— 40dp 圆距工具栏起始边的距离与它的垂直居中属于工具栏的职责，不属于本按钮。

原生高光读取实际表面亮度，按 `0.1 × (1 − clamp(1.2 × luma))⁴` 修正后进行材质混合；不再把白色描边直接盖到填充上。普通顶栏圆形样式与窗口浮层圆形样式分别处理。
