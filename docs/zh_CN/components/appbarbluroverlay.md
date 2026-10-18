# AppBarBlurOverlay

`AppBarBlurOverlay` 是 ColorOS 17 的顶栏材质叠加层：覆盖在顶部导航栏之上、随滚动淡入的模糊表面。它是 `AppBarBlurOverlayView` + `AppBarBlurOverlayBehavior` + `AppBarBlurHelper` 的 Compose 移植。

设备上由 Behavior 改写尺寸的正是这个 overlay，而 app bar 本身被「掏空」——材质落在 overlay 上，导航栏自身不再有背景、也没有高度阴影。overlay 自己不画任何像素：Helper 给它一层渐变透明度剖面以及一层渐进模糊，Behavior 再按滚动偏移驱动整块视图的 alpha。

有两个锚点，且不是同一个。**节点由调用方定尺寸**：原生视图以 `layout_height=0dp` inflate，再由 Behavior 改写成 `appBar.top .. appBar.bottom + extraHeightPx`，因此请传 `Modifier.height(appBarHeight + extraHeight)`。而 alpha 剖面锚定在节点内**导航栏的下边缘**上，渐进模糊则在节点的上边缘最强。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.AppBarBlurOverlay
import io.github.suqi8.coui.kmp.basic.AppBarBlurOverlayDefaults
import io.github.suqi8.coui.kmp.basic.couiGlassHeader
```

## 设备实现

`AppBarBlurOverlayView` 以 `layout_height=0dp` inflate，并声明布局行为 `AppBarBlurOverlayBehavior`，由后者测量为 `appBar.top .. appBar.bottom + extraHeightPx` —— 即 app bar 自身的边界加上 `homepage_appbar_blur_extra_height`（14dp），也就是 `AppBarBlurOverlayDefaults.ExtraHeight`。该视图没有 `onDraw`、也没有 `onSizeChanged`：它只是被 Helper 装扮的画布。

`AppBarBlurHelper` 把材质的背景解析成一个 `GradientBackgroundDrawable`、把模糊解析成一个 `RenderEffect`；`blurViewForAppBar()` 优先返回同级的 overlay 而不是 app bar 自身，这正是宿主把导航栏掏空的原因。材质的渐变画在 overlay 自己的背景 drawable 里而不是 `RenderEffect` 里，因此即使无法运行模糊，剖面依然会被绘制。

`GradientBackgroundDrawable` 构造的是自下而上的 `LinearGradient`，也就是说剖面的位置是**从导航栏下边缘向上**量起的，`p = 1` 对应导航栏上边缘。因此导航栏下方那条额外带子是透明的，材质读起来像导航栏自己的表面，而不是一条更高的渐变。

框架的均匀路径 —— `setUpNormalBlurBackground` 配 `normalBlurRadiusPx` 与 `normalBackgroundAlphaFraction` —— 是死代码：在本移植所依据的 ColorOS 17 产物里没有任何调用点，因此有意不实现。

## 基本用法

```kotlin
val density = LocalDensity.current
var scrollPx by remember { mutableStateOf(0f) }
val barHeight = TopAppBarDefaults.CollapsedHeight

Box(Modifier.fillMaxSize()) {
    ScrollingContent(onScroll = { scrollPx = it })

    AppBarBlurOverlay(
        // 节点承载导航栏本体加上它下方那 14dp 的带子。
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight + AppBarBlurOverlayDefaults.ExtraHeight),
        scrollFraction = AppBarBlurOverlayDefaults.scrollAlpha(scrollPx, density),
    ) {
        // 导航栏自身的内容，已被掏空：材质在它下方淡入。
        SmallTopAppBar(title = "标题")
    }
}
```

overlay 通过 `LocalBackdrop` 采样它后方的画面；请放在提供了背景层的位置，或传 `backdrop = null` 只保留剖面。平台无法运行 runtime shader 时（Android 13 以下）模糊同样会被跳过，只绘制剖面。

### 静止下限

ColorOS 17 首页不会把材质完全撤掉：顶栏完全展开时仍保留一层表面色。下游项目在 PLK110 / ColorOS 17 上实测该下限为 `0.55`，`minAlpha` 重载让调用方可以直接在 `scrollFraction` 的 producer 里表达这个下限：

```kotlin
val scrollPx = remember { mutableFloatStateOf(0f) }
val scrollFraction = remember(density) {
    {
        AppBarBlurOverlayDefaults.scrollAlpha(
            scrollPx = scrollPx.floatValue,
            density = density,
            minAlpha = 0.55f,
        )
    }
}
```

`minAlpha` 会被钳制到 `0f..1f`。静止时结果停在下限，随后在与两参数重载相同的 30dp 阈值内从 `minAlpha` 升到 `1`。

### 跟随内容位移，不要绑定顶栏收起比例

请用内容实际位移（`scrollPx`）驱动 `scrollFraction`，不要绑定顶栏的收起比例。两者只在列表位于顶部时才一致。

下游项目最初把材质 alpha 绑在收起比例上，结果列表在中间稍微下拉时材质肉眼可见地变淡：下拉 240px、头像行重新露出的过程中，alpha 从 `1.0` 掉到 `0.55`，而顶栏当时正在展开。改用位移驱动后，同样下拉 240px 的过程中实测状态栏带亮度恒定在 222–225（0–255），与收起态基准 223–235 一致，下陷消失——数值与内容同向、同步移动。

可折叠顶栏可以用 `COUIScrollBehavior(expandOnContentScroll = true)` 跟随同一段位移：列表在任意位置向下拖动时顶栏都会展开，而不只在列表顶部（见 [TopAppBar](./topappbar)）。

## 玻璃顶栏

长页面顶栏——参照 ColorOS 17 的「我的」页——使用 `Modifier.couiGlassHeader`。它绘制顶部加权的 progressive blur，并让底色一直保持到底部的渐隐带，使顶栏融入内容而不是以一条硬边结束。下游项目实测该渐隐带约 60–80px（560dpi 下约 18dp），边缘平滑、无可见接缝。

```kotlin
val backdrop = rememberLayerBackdrop {
    drawRect(Color.Black)
    drawContent()
}

Box(Modifier.fillMaxSize()) {
    // backdrop 是顶栏的兄弟节点，绝不能是它的祖先。
    Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
        Content()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .couiGlassHeader(
                backdrop = backdrop,
                containerColor = COUITheme.colorScheme.surface,
            ),
    ) {
        // 顶栏内容。
    }
}
```

`fadeHeight` 默认取 `GlassDefaults.HeaderFadeHeight`（28dp）；要与下游页面一致可传 `fadeHeight = 18.dp`。`backdrop` 为 `null` 或平台无法运行 runtime shader 时，该修饰符回退为不透明白底 `containerColor`。

## 参数

### AppBarBlurOverlay 属性

| 属性名         | 类型                       | 说明                                                                     | 默认值                                | 是否必须 |
| -------------- | -------------------------- | ------------------------------------------------------------------------ | ------------------------------------- | -------- |
| modifier       | Modifier                   | 应用于 overlay；调用方需把节点尺寸定为导航栏高度加 `extraHeight`         | Modifier                              | 否       |
| containerColor | Color                      | alpha 剖面的基色；浅色 `#f0f1f2`、深色 `#333333`（见「取值溯源」）        | AppBarBlurOverlayDefaults.containerColor() | 否       |
| backdrop       | Backdrop?                  | 待采样的内容；`null` 时跳过模糊，只绘制剖面                              | LocalBackdrop.current                 | 否       |
| extraHeight    | Dp                         | overlay 越过导航栏下边缘的距离                                           | AppBarBlurOverlayDefaults.ExtraHeight | 否       |
| scrollFraction | Float                      | 滚动驱动的 alpha，`min(1, scrollPx / 30dp)`；为 `0` 时 overlay 不绘制任何内容 | 1f                                | 否       |
| content        | @Composable BoxScope.() -> Unit | 调用方掏空的导航栏内容；它不属于材质                                  | {}                                    | 否       |

`content` 绘制在材质之上，且不受 `scrollFraction` 影响：静止时标题与操作区依然可读，只有材质随滚动的淡入。

`containerColor` 只是剖面被上色的基色；用哪条剖面——浅色三档还是深色两档——由主题决定，不由这个颜色决定（见「取值溯源」）。

### AppBarBlurOverlayDefaults 对象

#### 常量

| 常量 / 函数           | 类型  | 说明                                                 | 值 / 来源 |
| --------------------- | ----- | ---------------------------------------------------- | --------- |
| ExtraHeight           | Dp    | overlay 越过导航栏下边缘的距离                       | 14.dp（`homepage_appbar_blur_extra_height`） |
| ScrollAlphaThresholdDp | Float | overlay 的 alpha 从 0 升到 1 所跨过的滚动距离        | 30f（`AppBarBlurOverlayBehavior.ALPHA_SCROLL_THRESHOLD_DP`） |
| DarkContainerColor    | Color | 深色主题的材质基色，取最近的 ColorOS 夜间 token 而非框架属性 | `#333333`（`coui_color_background_top` / `coui_color_surface_top`） |
| containerColor()      | Color | 当前主题的基色：浅色取 `surface`、深色取 `DarkContainerColor` | `#f0f1f2` / `#333333` |
| scrollAlpha(scrollPx, density) | Float | `min(1, scrollPx / ScrollAlphaThresholdDp)`，钳制到 `0f..1f` | — |
| scrollAlpha(scrollPx, density, minAlpha) | Float | 同一剖面重映射到 `minAlpha..1`，`minAlpha` 钳制到 `0f..1f` | — |

把 `scrollAlpha` 的结果交给 overlay 的 `scrollFraction`：静止时 overlay 不可见，内容滚过 `ScrollAlphaThresholdDp`（30dp）后达到满强度。

## 取值溯源

下表每个值都取自 ColorOS 17 材质默认值或其资源表，无一处编造。

| Token | 值 | 来源 |
| :--- | :--- | :--- |
| `ExtraHeight` | 14dp | `homepage_appbar_blur_extra_height` |
| `ScrollAlphaThresholdDp` | 30 | `AppBarBlurOverlayBehavior.ALPHA_SCROLL_THRESHOLD_DP` |
| `startBlurRadius` | 30px | `AppBarBlurParams.startBlurRadius` |
| `endBlurRadius` | 0px | `AppBarBlurParams.endBlurRadius` |
| 混合曲线 | `pow(1 - t, 3.0)` | `AppBarBlurParams.gradientBlurSpeed` = 3.0f |
| 浅色档位位置 | `[0.0, 0.4, 1.0]` | `AppBarBlurParams.gradientPos0/1/2` |
| 浅色档位 alpha | `[0.0, 0.7, 1.0]` | `AppBarBlurParams.gradientAlpha0/1/2` |
| 深色档位位置 | `[0.0, 1.0]` | `gradientPos0` / `gradientPos2` |
| 深色档位 alpha | `[0.0, 0.4]` | `gradientAlpha0` / `AppBarBlurHelper.GRADIENT_DARK_ALPHA` |

深色只有两档：`0.4` 这个上边缘 alpha 是硬编码的 `GRADIENT_DARK_ALPHA`，不是第三个 `gradientAlpha`。模糊半径与混合指数由 `TopAppBar.kt` 的 `AppBarGlassDefaults` 承载（`GradientBlurRadiusPx`、`GradientEndBlurRadiusPx`、`GradientBlurSpeed`），导航栏自身的 `gradientGlass` 材质也从同一处取值；`endBlurRadius` 为 0，是因为渐变形状按 `max(start, end)` 模糊、由混合曲线负责渐变。

走哪条剖面是**主题属性、不是基色亮度**：本移植在 `COUITheme.colorScheme.background.luminance() < 0.5f`（即框架的 `isCOUIDarkThemeOrNightMode`）时选深色两档，因此浅色主题下调暗 `containerColor` 也不会把剖面切到另一分支。

深色基色无法从同一条链里还原。ColorOS 从 `?couiColorBackgroundWithCard` 解析它，而该属性在 night 配置下是 `#ffffff`——这个颜色在数学上无法产生实测的剖面：材质顶边压在 `#454545` 卡片上得 60，配合深色剖面硬编码的顶边 alpha，`0.4 * B + 0.6 * 69 = 60` 解出 `B ≈ 46.5`。最近的 ColorOS 夜间 token 是 `coui_color_background_top` / `coui_color_surface_top` = `#333333`，`DarkContainerColor` 取该值。缺口不可能被某个显式颜色盖住：渐变模糊背景色在 ColorOS 17 里从未被赋值，静态字段恒为 `null`，最终解析的是页面兜底；深色路径上还叠着一层本仓无法复现的压暗。

## 未移植

- **均匀（`setUpNormalBlurBackground`）路径** —— `normalBlurRadiusPx`（50px）、`normalBackgroundAlphaFraction`（0.8）与 `NORMAL_BLUR_DARK_ALPHA`（0.6）在已研究的产物中没有任何调用点，故本移植只实现渐变形态。
- **`scrollDrivenAlphaEnabled` 属性** —— 设备视图在该属性置位时按滚动偏移读取 alpha；本移植改由调用方把已解析的 alpha 交给 `scrollFraction`。
- **基色解析链** —— `resolveGradientBlurBaseColorForAppBar` 会依次尝试静态 `gradientBlurBgColor`、app bar 自身背景、恢复快照与页面兜底；本移植只接受一个 `containerColor`。那条静态覆盖是失效的：ColorOS 17 中从未给它赋值，因此它恒为 `null`、始终走页面兜底；本移植够不到的那部分基色正是深色主题的。
- **`setAppBarLayoutTransparent()`** —— 宿主侧那个清掉 app bar 背景与高度阴影的调用；本移植由调用方在 `content` 里传入已掏空的导航栏来完成。
