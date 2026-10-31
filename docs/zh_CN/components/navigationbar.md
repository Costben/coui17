# NavigationBar

`NavigationBar` 是 COUI 中的底部导航栏组件，用于在应用底部创建导航菜单，支持 2 到 5 个导航项，提供不同的显示模式（仅图标、仅文本、图标和文本、仅选中项显示文本）。

`FloatingNavigationBar` 是一个悬浮样式的底部导航栏组件，同样支持 2 到 5 个导航项，仅显示图标。

这些组件通常与 `Scaffold` 组件结合使用，以便在应用程序的不同页面中保持一致的布局和行为。

<div style="position: relative; height: 300px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../../compose/index.html?id=navigationBar" title="Demo" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin"></iframe>
</div>

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.NavigationBar
import io.github.suqi8.coui.kmp.basic.NavigationBarItem
import io.github.suqi8.coui.kmp.basic.FloatingNavigationBar
import io.github.suqi8.coui.kmp.basic.FloatingNavigationBarItem
import io.github.suqi8.coui.kmp.basic.BottomFloatingNavigationBar
import io.github.suqi8.coui.kmp.basic.BottomFloatingNavigationBarItem
import io.github.suqi8.coui.kmp.basic.BottomFloatingNavigationBarDefaults
import io.github.suqi8.coui.kmp.basic.BottomFloatingRedDot
import io.github.suqi8.coui.kmp.basic.NavigationBarDisplayMode
import io.github.suqi8.coui.kmp.basic.NavigationItem
```

## 基本用法

### NavigationBar

NavigationBar 组件可用于创建固定在底部的导航菜单：

```kotlin
var selectedIndex by remember { mutableStateOf(0) }
val items = listOf("Home", "Profile", "Settings")
val icons = listOf(COUIIcons.VerticalSplit, COUIIcons.Contacts, COUIIcons.Settings)

Scaffold(
    bottomBar = {
        NavigationBar {
            items.forEachIndexed { index, label ->
                NavigationBarItem(
                    selected = selectedIndex == index,
                    onClick = { selectedIndex = index },
                    icon = icons[index],
                    label = label
                )
            }
        }
    }
)
```

### FloatingNavigationBar

FloatingNavigationBar 组件可用于创建悬浮在底部的导航菜单：

```kotlin
var selectedIndex by remember { mutableStateOf(0) }
val items = listOf("Home", "Profile", "Settings")
val icons = listOf(COUIIcons.VerticalSplit, COUIIcons.Contacts, COUIIcons.Settings)

Scaffold(
    bottomBar = {
        FloatingNavigationBar {
            items.forEachIndexed { index, label ->
                FloatingNavigationBarItem(
                    selected = selectedIndex == index,
                    onClick = { selectedIndex = index },
                    icon = icons[index],
                    label = label
                )
            }
        }
    }
)
```

### BottomFloatingNavigationBar

`BottomFloatingNavigationBar` 是 ColorOS 17 的悬浮胶囊导航栏（`COUIBottomFloatingNavigationBar`）。它与 `FloatingNavigationBar` 是两个不同的组件：其导航项同时带有图标与标签，并且可以承载红点、长按溢出菜单，以及从胶囊中「融出」的圆形展开视图。

```kotlin
var selectedIndex by remember { mutableStateOf(0) }
val items = listOf("Home", "Messages", "Settings")
val icons = listOf(COUIIcons.Home, COUIIcons.Messages, COUIIcons.Settings)

BottomFloatingNavigationBar {
    items.forEachIndexed { index, label ->
        BottomFloatingNavigationBarItem(
            selected = selectedIndex == index,
            onClick = { selectedIndex = index },
            icon = icons[index],
            label = label
        )
    }
}
```

导航栏通过 `LocalBackdrop` 采样其后方的画面；提供背景层即可获得 ColorOS 17 的光感玻璃效果，传入 `backdrop = null` 则保持胶囊不透明。

## 组件状态

### 选中状态

`NavigationBarItem` 遵循 COUI tab 导航的状态模型：选中项的图标与标签以完整标签色显示，未选中项呈暗化色；图标在两种状态间以 180ms 交叉淡变过渡，标签颜色则瞬时切换。标签始终使用 Medium 字重；按压未选中项时图标会预览选中色。`FloatingNavigationBarItem` 在选中时高亮图标。

## 属性

### NavigationBar 属性

| 属性名                     | 类型                    | 说明                       | 默认值                                | 是否必须 |
| -------------------------- | ----------------------- | -------------------------- | ------------------------------------- | -------- |
| modifier                   | Modifier                | 应用于导航栏的修饰符       | Modifier                              | 否       |
| color                      | Color                   | 导航栏背景颜色             | COUITheme.colorScheme.background     | 否       |
| showDivider                | Boolean                 | 是否显示顶部分割线         | true                                  | 否       |
| defaultWindowInsetsPadding | Boolean                 | 是否应用默认窗口嵌入边距   | true                                  | 否       |
| mode                       | NavigationBarDisplayMode   | 导航项的显示模式           | NavigationBarDisplayMode.IconAndText     | 否       |
| content                    | @Composable RowScope.() | 导航栏的内容               | -                                     | 是       |

### NavigationBarItem 属性

| 属性名 | 类型        | 说明             | 默认值 | 是否必须 |
| ------ | ----------- | ---------------- | ------ | -------- |
| selected | Boolean     | 是否选中         | -      | 是       |
| onClick  | () -> Unit  | 点击时的回调     | -      | 是       |
| icon     | ImageVector | 图标             | -      | 是       |
| label    | String      | 文本标签         | -      | 是       |
| modifier | Modifier    | 应用于导航项的修饰符 | Modifier | 否       |
| enabled  | Boolean     | 是否启用         | true     | 否       |

### FloatingNavigationBar 属性

| 属性名                     | 类型                      | 说明                               | 默认值                                  | 是否必须 |
| -------------------------- | ------------------------- | ---------------------------------- | --------------------------------------- | -------- |
| modifier                   | Modifier                  | 应用于导航栏的修饰符               | Modifier                                | 否       |
| color                      | Color                     | 导航栏背景颜色                     | COUITheme.colorScheme.surfaceContainer | 否       |
| cornerRadius               | Dp                        | 导航栏的圆角半径                   | FloatingToolbarDefaults.CornerRadius    | 否       |
| horizontalAlignment        | Alignment.Horizontal      | 导航栏在其父容器中的水平对齐方式   | CenterHorizontally                      | 否       |
| horizontalOutSidePadding   | Dp                        | 导航栏外部的水平内边距             | FloatingNavigationBarDefaults.HorizontalOutSidePadding | 否       |
| shadowElevation            | Dp                        | 导航栏的阴影高度                   | FloatingNavigationBarDefaults.ShadowElevation          | 否       |
| showDivider                | Boolean                   | 是否显示导航栏周围的分割线         | false                                   | 否       |
| defaultWindowInsetsPadding | Boolean                   | 是否应用默认窗口嵌入边距           | true                                    | 否       |
| content                    | @Composable () -> Unit    | 导航栏的内容                       | -                                       | 是       |

### FloatingNavigationBarItem 属性

| 属性名 | 类型        | 说明             | 默认值 | 是否必须 |
| ------ | ----------- | ---------------- | ------ | -------- |
| selected | Boolean     | 是否选中         | -      | 是       |
| onClick  | () -> Unit  | 点击时的回调     | -      | 是       |
| icon     | ImageVector | 图标             | -      | 是       |
| label    | String      | 文本标签         | -      | 是       |
| modifier | Modifier    | 应用于导航项的修饰符 | Modifier | 否       |
| enabled  | Boolean     | 是否启用         | true     | 否       |

### BottomFloatingNavigationBar 属性

| 属性名         | 类型                                | 说明                                                     | 默认值                    | 是否必须 |
| -------------- | ----------------------------------- | -------------------------------------------------------- | ------------------------- | -------- |
| modifier       | Modifier                            | 应用于导航栏的修饰符                                     | Modifier                  | 否       |
| containerColor | Color                               | 没有可模糊的背景层时胶囊的不透明填充                     | COUITheme.colorScheme.bar | 否       |
| backdrop       | Backdrop?                           | 胶囊后方被模糊的内容；`null` 时保持不透明                | LocalBackdrop.current     | 否       |
| expanded       | Boolean                             | 是否显示展开视图；`expansion` 为 `null` 时忽略           | false                     | 否       |
| expansion      | (@Composable BoxScope.() -> Unit)?  | 从胶囊中「融出」的展开视图                               | null                      | 否       |
| shadow         | Shadow?                             | 胶囊与展开视图各自投下的阴影；`null` 时不绘制。系统焦散光已投下原生阴影时不再绘制默认值 | BottomFloatingNavigationBarDefaults.BarShadow | 否 |
| content        | @Composable RowScope.() -> Unit     | 导航栏内容，通常是 `BottomFloatingNavigationBarItem`     | -                         | 是       |

### BottomFloatingNavigationBarItem 属性

| 属性名       | 类型                       | 说明                                                        | 默认值   | 是否必须 |
| ------------ | -------------------------- | ----------------------------------------------------------- | -------- | -------- |
| selected     | Boolean                    | 是否选中                                                    | -        | 是       |
| onClick      | () -> Unit                 | 点击时的回调                                                | -        | 是       |
| icon         | ImageVector                | 图标                                                        | -        | 是       |
| label        | String                     | 文本标签                                                    | -        | 是       |
| modifier     | Modifier                   | 应用于导航项的修饰符                                        | Modifier | 否       |
| enabled      | Boolean                    | 是否启用                                                    | true     | 否       |
| largeIcon    | Boolean                    | 是否绘制溢出导航项的 40dp 大图标形态                        | false    | 否       |
| redDot       | BottomFloatingRedDot?      | 固定在图标顶部结束角的红点                                  | null     | 否       |
| overflowMenu | (@Composable () -> Unit)?  | 长按溢出菜单的各行，显示在 `NavigationPopupMenu` 中         | null     | 否       |

### BottomFloatingRedDot

`BottomFloatingRedDot` 是固定在底部悬浮导航项图标上的红点。纯圆点为 `Dot`；数字胶囊为 `Count(number)`。

| 值            | 说明                  |
| ------------- | --------------------- |
| Dot           | 纯圆点                |
| Count(number) | 显示 `number` 的胶囊  |

### BottomFloatingNavigationBarDefaults 对象

`BottomFloatingNavigationBarDefaults` 对象提供 `BottomFloatingNavigationBar` 与 `BottomFloatingNavigationBarItem` 共用的度量值。

#### 常量

| 常量名                          | 类型     | 说明                                             | 默认值 |
| ------------------------------- | -------- | ------------------------------------------------ | ------ |
| BarHeight                       | Dp       | 胶囊高度                                         | 56.dp  |
| MinHeightWithoutPaddingVertical | Dp       | 导航栏的最小外部高度                             | 88.dp  |
| PaddingHorizontal               | Dp       | 小屏下的外部内边距                               | 16.dp  |
| MenuToScreenNaviDistance        | Dp       | 手势导航下宿主给导航栏的底部留白                 | 8.dp   |
| ButtonNavigationDistance        | Dp       | 三键导航下宿主给导航栏的底部留白                 | 36.dp  |
| navigationBarPadding()          | Dp       | 按当前导航方式取宿主的底部留白                   | 8.dp / 36.dp / 0.dp |
| ExpansionSize                   | Dp       | 展开视图的占位尺寸                               | 56.dp  |
| SideSpacing                     | Dp       | 胶囊与展开视图之间的间距                         | 12.dp  |
| BarShadow                       | Shadow   | 胶囊与展开视图投下的阴影（COUI `SHADOW_LV_B`）   | 半径 6.dp、黑色 12%、下移 2.dp |
| NATIVE_BLUR_RADIUS_PX           | Float    | COUI `TYPE_FRAMEWORK_BOTTOM_BAR` 材质的模糊半径（像素） | 25f |
| BlurColorsLight                 | List&lt;BlendColorEntry&gt; | 浅色主题下材质的着色层，按施加顺序排列 | `#B3666666` ColorDodge、`#99F5F5F5` Luminosity |
| BlurColorsDark                  | List&lt;BlendColorEntry&gt; | 深色主题下的同一组着色层               | `#80666666` Overlay、`#B3262626` Luminosity |
| RIM_ALPHA_LIGHT                 | Float    | 浅色主题下边缘光的不透明度（`TYPE_FRAMEWORK_CAPSULE_3`） | 0.25f |
| RIM_ALPHA_DARK                  | Float    | 深色主题下边缘光的不透明度                       | 0.8f   |
| CausticColor                    | Color    | 胶囊下方投出的焦散光（`coui_caustic_color_2`）   | `#42FFFFFF` |
| ItemHeight                      | Dp       | 导航项单元高度                                   | 48.dp  |
| ItemMaxWidth                    | Dp       | 导航项单元的最大宽度                             | 70.dp  |
| ItemSpacing                     | Dp       | 导航项之间的间距                                 | 8.dp   |
| ItemInnerPadding                | Dp       | 导航项单元内部的水平内边距                       | 12.dp  |
| ItemBackgroundHorizontalPadding | Dp       | 导航项背景的水平内边距                           | 8.dp   |
| IconSize                        | Dp       | 图标尺寸                                         | 24.dp  |
| IconTopMargin                   | Dp       | 图标的上边距                                     | 4.dp   |
| IconTitleGapVertical            | Dp       | 图标与标题之间的垂直间距                         | 2.dp   |
| IconTitleGapHorizontal          | Dp       | 图标与标题之间的水平间距                         | 4.dp   |
| LargeIconSize                   | Dp       | 大图标形态的图标尺寸                             | 40.dp  |
| LargeIconOverflow               | Dp       | 大图标形态超出导航项上边缘的距离                 | 14.dp  |
| RedDotDiameter                  | Dp       | 纯圆点红点的直径                                 | 6.dp   |
| RedDotRightOverflow1Digit       | Dp       | 一位数字时红点超出结束边的距离                   | 6.dp   |
| RedDotRightOverflow2Digit       | Dp       | 两位数字时红点超出结束边的距离                   | 9.dp   |
| RedDotRightOverflow3Digit       | Dp       | 三位数字时红点超出结束边的距离                   | 10.dp  |
| RedDotTopOverflow               | Dp       | 数字红点向上溢出的距离                           | 4.dp   |
| TouchEffectRadius               | Dp       | 径向触摸高亮的半径                               | 40.dp  |
| MinimumSpacingWithSlideView     | Dp       | 与滑动视图之间保留的最小间距                     | 12.dp  |
| LabelFontSize                   | TextUnit | 标签字号，按 dp 绘制、不随字体缩放（原生 `dp2px(10f)`） | 10.sp |
| CornerRadius                    | Dp       | 胶囊圆角半径                                     | 28.dp  |

#### 函数

| 函数名             | 返回类型                    | 说明                                                                    |
| ------------------ | --------------------------- | ----------------------------------------------------------------------- |
| blurColors(isDark) | List&lt;BlendColorEntry&gt; | 返回 `BlurColorsDark` 或 `BlurColorsLight`                              |
| blurRadius()       | Float                       | 把 `NATIVE_BLUR_RADIUS_PX` 换算成 `textureBlur` 在当前密度下得到同一高斯所需的 dp 半径 |

### NavigationBarDefaults 对象

NavigationBarDefaults 对象提供了 NavigationBar 和 NavigationBarItem 组件的默认值。

#### 常量

| 常量名                 | 类型     | 说明                       | 默认值 |
| ---------------------- | -------- | -------------------------- | ------ |
| ItemHeight             | Dp       | 项目单元高度               | 56.dp  |
| HorizontalPadding      | Dp       | 栏两侧的水平内边距         | 12.dp  |
| ItemHorizontalPadding  | Dp       | 项目单元内部水平内边距     | 2.dp   |
| IconSize               | Dp       | 图标尺寸                   | 24.dp  |
| IconTopPadding         | Dp       | 图标顶部内边距             | 9.dp   |
| LabelBottomPadding     | Dp       | 标签底部内边距             | 7.dp   |
| LabelFontSize          | TextUnit | 标签字号                   | 10.sp  |
| TextFontSize           | TextUnit | 字号（TextOnly 模式）      | 14.sp  |
| IconFadeDurationMillis | Int      | 图标状态间交叉淡变时长     | 180    |

### FloatingNavigationBarDefaults 对象

FloatingNavigationBarDefaults 对象提供了 FloatingNavigationBar 和 FloatingNavigationBarItem 组件的默认值。

#### 常量

| 常量名                   | 类型  | 说明                       | 默认值 |
| ------------------------ | ----- | -------------------------- | ------ |
| HorizontalOutSidePadding | Dp    | 外部水平内边距             | 36.dp  |
| ShadowElevation          | Dp    | 阴影高度                   | 1.dp   |
| HorizontalPadding        | Dp    | 内部水平内边距             | 12.dp  |
| ItemSpacing              | Dp    | 项目间距                   | 12.dp  |
| IconSize                 | Dp    | 图标尺寸                   | 28.dp  |
| IconPadding              | Dp    | 图标周围的内边距           | 10.dp  |
| SelectedPressedAlpha     | Float | 选中项按压时的透明度       | 0.5f   |
| UnselectedPressedAlpha   | Float | 未选中项按压时的透明度     | 0.6f   |
| UnselectedAlpha          | Float | 未选中项的透明度           | 0.4f   |

### NavigationBarDisplayMode 枚举

| 值                    | 说明                   |
| --------------------- | ---------------------- |
| IconAndText           | 显示图标和文本         |
| IconOnly              | 仅显示图标             |
| TextOnly              | 仅显示文本             |
| IconWithSelectedLabel | 始终显示图标，仅选中时显示文本 |

设置在 `NavigationBar` 上的显示模式会通过 `LocalNavigationBarDisplayMode` 组合局部值传递给各导航项。`FloatingNavigationBar` 没有 `mode` 参数——其导航项始终仅显示图标。

### NavigationItem 属性

`NavigationItem` 是一个用于承载导航项标签与图标的便捷数据类。

| 属性名 | 类型        | 说明       | 默认值 | 是否必须 |
| ------ | ----------- | ---------- | ------ | -------- |
| label  | String      | 该项的标签 | -      | 是       |
| icon   | ImageVector | 该项的图标 | -      | 是       |

## 进阶用法

### NavigationBar

#### 自定义颜色

```kotlin
NavigationBar(
    color = Color.Red.copy(alpha = 0.3f)
) {
    // ... items ...
}
```

#### 无分割线

```kotlin
NavigationBar(
    showDivider = false
) {
    // ... items ...
}
```

#### 处理窗口边距

```kotlin
NavigationBar(
    defaultWindowInsetsPadding = false // Handle window insets padding manually
) {
    // ... items ...
}
```

#### 显示模式

```kotlin
NavigationBar(
    // IconAndText (default) / IconOnly / TextOnly / IconWithSelectedLabel
    mode = NavigationBarDisplayMode.IconWithSelectedLabel
) {
    // ... items ...
}
```

### FloatingNavigationBar

#### 自定义颜色和圆角

```kotlin
FloatingNavigationBar(
    color = COUITheme.colorScheme.primaryContainer,
    cornerRadius = 28.dp
) {
    // ... items ...
}
```

#### 自定义对齐和边距

```kotlin
FloatingNavigationBar(
    horizontalAlignment = Alignment.Start, // Align to start
    horizontalOutSidePadding = 16.dp // Set outside padding
) {
    // ... items ...
}
```

#### 分割线与阴影

```kotlin
FloatingNavigationBar(
    showDivider = true, // Draw a thin divider ring around the bar
    shadowElevation = 0.dp // Disable the drop shadow
) {
    // ... items ...
}
```

### BottomFloatingNavigationBar

#### 玻璃表面

`BottomFloatingNavigationBar` 使用 ColorOS 17 的光感玻璃绘制胶囊。`backdrop` 是它采样的内容，默认为 `LocalBackdrop.current`，因此在提供了背景层的位置，导航栏会自动取用。玻璃采用原生胶囊请求的 `TYPE_FRAMEWORK_BOTTOM_BAR` 材质：背景以原生的 25px 半径模糊，再用保留色相与饱和度、只重映射明度的混合模式着色层（`BlurColorsLight` / `BlurColorsDark`）着色，因此下方滚动内容的颜色仍会透出。边缘带 `TYPE_FRAMEWORK_CAPSULE_3` 边缘光，胶囊下方投出原生焦散光。`backdrop` 为 `null` 时，胶囊回退为 `containerColor` 的不透明填充。有背景层时不绘制 `containerColor`，与设备一致。

```kotlin
CompositionLocalProvider(LocalBackdrop provides backdrop) {
    BottomFloatingNavigationBar(containerColor = COUITheme.colorScheme.bar) {
        // ... 导航项 ...
    }
}
```

#### 胶囊几何

胶囊按 `BaseBottomFloatingMenuView.onMeasure` 测量原生菜单视图的方式测量，因此每个导航项拿到的是**精确**宽度，而不是 wrap-content：

- `innerPadding` 12dp（`BottomFloatingNavigationBarDefaults.ItemInnerPadding`，`coui_bottom_floating_navigation_bar_item_padding`）、`itemSpace` 8dp（`ItemSpacing`，`..._item_space`）、`itemMaxWidth` 70dp（`ItemMaxWidth`，`..._item_max_width`）；胶囊高 56dp（`BarHeight`，`..._height`）；
- `totalItemMax = n * 70 + 2 * 12 + (n - 1) * 8`；
- `totalItemMax` 能放进导航栏让出的宽度时，胶囊正好宽 `totalItemMax`，每项 70dp；放不下时胶囊取满可用宽度，每项拿到 `(available - 24 - (n - 1) * 8) / n`，截断到整数像素；
- 每个子项都以精确宽度测量，并放置在 `x = 12 + i * (itemWidth + 8)`，在胶囊内垂直居中。

胶囊的填充、模糊、阴影与触摸高亮共用一个 `CapsuleOutlineShape`。在 56dp 胶囊的 28dp 半径下，它走系统胶囊曲线（`addSegmentCapsule`，即无 weight 的 `OplusPathAdapter.addSmoothRoundRect`），而不是正圆跑道形；显式传入更小半径时保持 Lp 超椭圆。展开视图的圆形轮廓仍为正圆。

12dp 的内边距是胶囊自身宽度的一部分，而不是一个 `padding` 修饰符，因此玻璃表面与径向触摸高亮都覆盖整条胶囊。`RowScope.weight` 与 `RowScope.align` 在本栏内不起作用：这一行自行测量每个子项，与原生菜单视图一致——后者没有 weight 概念，而是把每个子项放到自己算出的 x 上。

纵向上，胶囊在导航栏 88dp 的盒子里居中，与 `COUIBottomFloatingNavigationBar.onLayout` 让菜单视图居中一致。盒子下方的留白由宿主决定：用 `BottomFloatingNavigationBarDefaults.navigationBarPadding()` 代替 `navigationBarsPadding()`——手势导航 8dp（`MenuToScreenNaviDistance`）、三键导航 36dp（`ButtonNavigationDistance`）、没有导航栏时为 0。手势导航下胶囊底边即距屏幕边缘 24dp，与 ColorOS 17 相册一致。

#### 展开视图（融球）

传入 `expansion` 视图并用 `expanded` 控制它。导航栏会在胶囊旁边为它预留空间，并把两个轮廓通过同一个融合表面绘制，因此展开视图是从胶囊中「融出」再融回的：

```kotlin
var expanded by remember { mutableStateOf(false) }

BottomFloatingNavigationBar(
    expanded = expanded,
    expansion = {
        Box(
            modifier = Modifier.fillMaxSize().clip(CircleShape).background(COUITheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = COUIIcons.Add, contentDescription = "展开")
        }
    },
) {
    // ... 导航项 ...
}
```

未传入 `expansion` 时 `expanded` 不产生任何效果，导航栏就是普通胶囊。展开视图显示期间，胶囊会按它让出的宽度进行测量——即展开视图的占位加上 `BottomFloatingNavigationBarDefaults.SideSpacing`。

#### 大图标

在导航项上设置 `largeIcon = true`，即可绘制 40dp 的大图标形态（`BottomFloatingNavigationBarDefaults.LargeIconSize`），并溢出导航项 `BottomFloatingNavigationBarDefaults.LargeIconOverflow`，取代默认的 24dp 图标。标签仍在图标槽位下方保持原位。

```kotlin
BottomFloatingNavigationBarItem(
    selected = selectedIndex == 0,
    onClick = { selectedIndex = 0 },
    icon = COUIIcons.Home,
    label = "首页",
    largeIcon = true,
)
```

#### 红点

`redDot` 会把红点固定在图标顶部的结束角：`BottomFloatingRedDot.Dot` 为纯圆点，`BottomFloatingRedDot.Count(number)` 为数字胶囊。`null` 表示不显示。

```kotlin
BottomFloatingNavigationBarItem(
    selected = false,
    onClick = {},
    icon = COUIIcons.Messages,
    label = "消息",
    redDot = BottomFloatingRedDot.Count(12),
)
```

#### 溢出菜单

`overflowMenu` 用于填充导航项的长按菜单，其中的各行会显示在锚定到该导航项的 `NavigationPopupMenu` 中。传入 `null` 则禁用长按。

```kotlin
BottomFloatingNavigationBarItem(
    selected = false,
    onClick = {},
    icon = COUIIcons.Settings,
    label = "设置",
    overflowMenu = {
        NavigationPopupMenuItem(
            label = "搜索",
            icon = COUIIcons.Search,
            onClick = { /* ... */ },
        )
    },
)
```

#### 径向触摸高亮

手指按下期间，导航栏会在导航项下方绘制一层径向高亮，并裁剪到胶囊轮廓内。它从中心的白色 38/255 透明度渐变到 `BottomFloatingNavigationBarDefaults.TouchEffectRadius`（40dp）处的完全透明，并以自己的弹簧淡入。指针只被追踪而不会被消费，因此导航项自身的点击与长按手势仍能拿到完整的手势。

#### 响应式边距

16dp 的外部内边距（`BottomFloatingNavigationBarDefaults.PaddingHorizontal`）只在小屏下保留；中屏与大屏下收缩为 0，胶囊可以使用整个宽度。

这也是本栏唯一的宽度档位，并且没有横屏分支。`COUIBottomFloatingNavigationBarMenuView` 两者都没有，而 `onConfigurationChanged` 覆写只是跳过下一次布局动画，重组已经取代了它。`couiMenuViewCentered` 属性会选中第二套排列：小屏下且恰好两个可见项时，导航栏会在胶囊两侧各留出展开视图的占位加 `BottomFloatingNavigationBarDefaults.SideSpacing`，再把胶囊居中于剩余空间，展开视图则贴住导航栏的尾部边缘。这里不对外暴露该属性。原生栏把自身测量为「内容 + 内边距」，本移植同样包裹内容并交给父级居中，因此在这些尺寸下两套排列无法区分；该属性默认值也是 `false`。

### 结合页面切换使用（使用脚手架）

#### 使用 NavigationBar

```kotlin
val pages = listOf("Home", "Profile", "Settings")
val icons = listOf(COUIIcons.VerticalSplit, COUIIcons.Contacts, COUIIcons.Settings)
var selectedIndex by remember { mutableStateOf(0) }

Scaffold(
    bottomBar = {
        NavigationBar {
            pages.forEachIndexed { index, label ->
                NavigationBarItem(
                    selected = selectedIndex == index,
                    onClick = { selectedIndex = index },
                    icon = icons[index],
                    label = label
                )
            }
        }
    }
) { paddingValues ->
    // Content area needs to consider padding
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Current Page: ${pages[selectedIndex]}",
            style = COUITheme.textStyles.title1
        )
    }
}
```

#### 使用 FloatingNavigationBar

```kotlin
val pages = listOf("Home", "Profile", "Settings")
val icons = listOf(COUIIcons.VerticalSplit, COUIIcons.Contacts, COUIIcons.Settings)
var selectedIndex by remember { mutableStateOf(0) }

Scaffold(
    bottomBar = {
        FloatingNavigationBar {
            pages.forEachIndexed { index, label ->
                FloatingNavigationBarItem(
                    selected = selectedIndex == index,
                    onClick = { selectedIndex = index },
                    icon = icons[index],
                    label = label
                )
            }
        }
    }
) { paddingValues ->
    // Content area needs to consider padding
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Current Page: ${pages[selectedIndex]}",
            style = COUITheme.textStyles.title1
        )
    }
}
```
