# HighlightedIconButton

`HighlightedIconButton` 是 ColorOS 17 带光晕的工具栏图标按钮，对应 ColorOS 给工具栏返回键与三点菜单键施加的那套光效。

ColorOS 给这两个键做的是同一套处理：一个**正圆 40dp**——不是 squircle——配一层「中心暗、边缘亮」的径向填充、一圈按角度分布的白描边，以及边缘外侧随主题不同的光晕。按下时整块控件弹到 `HighlightedIconButtonDefaults.PressedScale`，描边与外光晕消失，填充与图标变亮；两个主题表现一致。

光晕绘制在节点直径之外，因此调用方的修饰符链上不能有任何裁剪（不要 `clip`，也不要裁剪形状的父级）。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.HighlightedIconButton
import io.github.suqi8.coui.kmp.basic.HighlightedIconButtonDefaults
```

## 设备实现

该效果属于 COUI 工具栏库的 `coui_toolbar_back_view` / `toolbar_more_view`，而不是一个独立的组件类；**该视图的属性表在反编译中尚未定位**，因此下表的数值来自真机测量——UI dump 加真机像素采样——以及 `ShadowEdge` AGSL 着色器，标注为「实测所得」而非「反编译所得」。

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
| StrokeAlpha           | Float                  | 描边透明度                                                 | 0.15f |
| StrokeWidth           | Dp                     | 描边宽度                                                   | 1.dp |
| StrokeShadowFadeIn    | Float                  | 描边内阴影的淡入比例                                       | 0.01f |
| Stroke                | MaterialStrokeParams   | 描边参数：`StrokeAlpha` 白、`StrokeWidth` 宽、无内阴影      | — |
| DarkHaloColor         | Color                  | 深色主题外辉光的颜色                                       | 白 @ 8% |
| DarkHaloExtent        | Dp                     | 深色主题外辉光越过边缘的距离                               | 5.2.dp |
| DarkHaloStartAngle    | Float                  | 辉光弧的起始角；0° 为 3 点钟方向，故 90° 朝正下             | 60f |
| DarkHaloSweepAngle    | Float                  | 辉光弧的扫过角度                                           | 60f |
| LightHaloColor        | Color                  | 浅色主题光晕的颜色                                         | 黑 @ 5% |
| LightHaloRadius       | Dp                     | 浅色主题光晕越过边缘的距离                                 | 15.5.dp |
| LightHaloOffsetY      | Dp                     | 浅色主题光晕的垂直偏移                                     | 1.dp |

`fillCenter(isDark)` 与 `fillEdge(isDark)` 按当前主题解析这两个填充色。`PressedFillBrighten` 与 `PressedContentBrighten` 是名义值：测量只记录了按压时填充与图标会变亮，没给出幅度，这两个值并非实测所得。

## 取值溯源

下表每个值都在真机上测过（UI dump 加像素采样，1dp = 3.875px），或取自 `ShadowEdge` 着色器；**没有一项是从工具栏自身的属性表反编译所得**，因为那张表尚未定位。两层证据在此分开列出。

| Token | 值 | 证据层 |
| :--- | :--- | :--- |
| 直径 | 155px（1dp = 3.875px），即 40dp；正圆，非 squircle | 真机实测 |
| 描边 | ≈15% 白，按角度分布——上下强、左右近乎不可见 | 真机实测 |
| 描边厚度 | 圆顶部 ≈0.8dp、底部 ≈1.2dp | 真机实测 |
| 描边 shader | `GradientStrokeStyle.EDGE_STYLE_1` + `InnerShadowStyle.SHADOW_STYLE_1`（`160 × 160` 预设） | `ShadowEdge` AGSL 着色器 |
| 描边内阴影 | 无——shader 的淡入沿用 `TYPE_FRAMEWORK_CAPSULE_13` 深色档的不可见值 | 真机实测 |
| 填充 | 径向，中心暗边缘亮；透明度 ≈0.78（两点估算） | 真机实测 |
| 填充·深色 | r = 0 处 `#1e1e1e` → r = 70px 处 `#242424` | 真机实测 |
| 填充·浅色 | r = 0 处 249 → r = 68px 处 251 | 真机实测 |
| 外辉光·深色 | 只在 60°–120° 下半弧，到 R + 20px ≈ 5.2dp，贴边 1px 处 ≈8% 白 | 真机实测 |
| 光晕·浅色 | 近对称的柔和暗投影，半径 ≈60px ≈ 15.5dp，最深 −13 luma 且底部加权（底 −13 / 左右 −11 / 上 −8） | 真机实测 |
| 按压态 | 整块控件缩放到 ≈0.89（半径 77.5px → 69px）；描边与辉光淡出，填充与图标变亮 | 真机实测 |
| 按压弹簧 | `folmeSpring(damping = 1f, response = 0.3f, visibilityThreshold = 0.001f)` | `COUISpringForce`，本库的按压反馈弹簧 |

## 未移植 / 偏差

- **工具栏自身的属性表** —— 因为 `coui_toolbar_back_view` 的 `ShadowEdge` uniform（`u_edgeArray` / `u_commonArray` 各分量）尚未反编译，参数标注为实测所得；本移植只使用 `EDGE_STYLE_1` / `SHADOW_STYLE_1` 预设与上表的实测值。
- **`ShadowEdge` 着色器自带的光晕** —— shader 只覆盖节点自身边界，故填充与光晕由本组件绘制，描边仍走 shader。
- **工具栏自身的布局** —— 40dp 圆距工具栏起始边的距离与它的垂直居中属于工具栏的职责，不属于本按钮。
