# PinnedTopAppBar

`PinnedTopAppBar` 是 ColorOS 17 相册风格的顶部应用栏：大标题位于**第一行**，与操作按钮并排，并且始终不动。[`TopAppBar`](./topappbar) 把大标题放在第二行，并随内容滚动折叠；本组件在状态栏下方只保留一行 52dp 的高度，滚动时只有材质发生变化。

材质使用 [`AppBarBlurOverlay`](./appbarbluroverlay)：静止时不可见，页面背景直接透出；内容滚到栏下方 30dp 之内逐渐淡入。alpha 在绘制阶段读取，因此滚动不会触发重组。

它由 `PinnedScrollBehavior` 驱动：这是一个不会折叠导航栏的 `ScrollBehavior`，只负责记录内容已在栏下方滚过的距离。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.PinnedTopAppBar
import io.github.suqi8.coui.kmp.basic.PinnedTopAppBarDefaults
import io.github.suqi8.coui.kmp.basic.PinnedScrollBehavior
import io.github.suqi8.coui.kmp.basic.couiTopAppBarActionItem
```

## 基本用法

```kotlin
val scrollBehavior = PinnedScrollBehavior()

Box(Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // 列表在导航栏下方滚动，顶部预留导航栏的高度。
        Spacer(Modifier.height(PinnedTopAppBarDefaults.Height))
        Content()
    }
    PinnedTopAppBar(
        title = "照片",
        scrollBehavior = scrollBehavior,
        actions = {
            IconButton(onClick = { /* 处理点击事件 */ }, modifier = Modifier.couiTopAppBarActionItem()) {
                Icon(COUIIcons.Basic.Search, contentDescription = "搜索")
            }
            IconButton(onClick = { /* 处理点击事件 */ }, modifier = Modifier.couiTopAppBarActionItem()) {
                Icon(COUIIcons.More, contentDescription = "更多")
            }
        },
    )
}
```

材质会采样 `LocalBackdrop`；提供 backdrop 才有模糊，否则只绘制纯 alpha 渐变。未传 `scrollBehavior` 时材质保持静止 alpha，即 `0`。

### 导航图标

`navigationIcon` 可选。只有传入时，标题前才会插入 `PinnedTopAppBarDefaults.NavigationIconGap` 的间距。

```kotlin
PinnedTopAppBar(
    title = "图集",
    scrollBehavior = scrollBehavior,
    navigationIcon = {
        IconButton(onClick = { /* 处理点击事件 */ }, modifier = Modifier.couiTopAppBarActionItem()) {
            Icon(COUIIcons.Back, contentDescription = "返回")
        }
    },
)
```

### 标题样式

标题使用 `COUITheme.textStyles.title1`（32sp）。标题字重在设备上没有量过，因此不强加字重；需要时通过 `titleStyle` 覆盖：

```kotlin
PinnedTopAppBar(
    title = "照片",
    titleStyle = PinnedTopAppBarDefaults.titleStyle().copy(fontWeight = FontWeight.Medium),
)
```

## PinnedScrollBehavior

```kotlin
val scrollBehavior = PinnedScrollBehavior(state = rememberTopAppBarState())
```

| 参数名 | 类型 | 默认值 | 说明 |
| ------ | ---- | ------ | ---- |
| state | TopAppBarState | rememberTopAppBarState() | 行为写入 `contentOffset` 的状态对象 |
| canScroll | () -> Boolean | { true } | 返回 `false` 时不再更新内容偏移 |

与 `COUIScrollBehavior` 不同，它从不写入 `heightOffset`，没有吸附和 fling 动画（`isPinned = true`），也不拦截预滚动阶段，内容始终获得完整的手势。`onPostScroll` 中它累加 `state.contentOffset += consumed.y` 并限制在 `<= 0`：`-state.contentOffset` 就是内容在导航栏下方滚过的距离。

把它交给 `TopAppBar` 时，导航栏保持完全展开，其分割线按内容与导航栏重叠的比例淡入，而不是按折叠比例；交给 `SmallTopAppBar` 时与其他行为一样驱动分割线。

## 参数

### PinnedTopAppBar 属性

| 属性名 | 类型 | 说明 | 默认值 | 必填 |
| ------ | ---- | ---- | ------ | ---- |
| title | String | 标题，位于行首的单行省略文本 | - | 是 |
| modifier | Modifier | 应用于导航栏；上报状态栏 inset 加 `Height`，材质带不占布局空间 | Modifier | 否 |
| scrollBehavior | ScrollBehavior? | 由 `-state.contentOffset` 驱动材质 alpha；`null` 保持静止 alpha，即 `0` | null | 否 |
| defaultWindowInsetsPadding | Boolean | 是否在水平方向避开刘海与导航栏；状态栏 inset 始终生效，与 `TopAppBar` 一致 | true | 否 |
| containerColor | Color | 材质底色 | AppBarBlurOverlayDefaults.containerColor() | 否 |
| titleColor | Color | 标题颜色 | COUITheme.colorScheme.onSurface | 否 |
| titleStyle | TextStyle | 标题文本样式 | PinnedTopAppBarDefaults.titleStyle() | 否 |
| backdrop | Backdrop? | 材质采样的内容 | LocalBackdrop.current | 否 |
| extraHeight | Dp | 材质越过导航栏下边缘的高度 | AppBarBlurOverlayDefaults.ExtraHeight | 否 |
| startPadding | Dp | 起始边到第一个子项的间距 | PinnedTopAppBarDefaults.StartPadding | 否 |
| endPadding | Dp | 末尾边到最后一个操作的间距 | PinnedTopAppBarDefaults.EndPadding | 否 |
| navigationIcon | (@Composable () -> Unit)? | 可选的起始图标 | null | 否 |
| actions | @Composable RowScope.() -> Unit | 末尾的操作，垂直居中 | {} | 否 |

### PinnedTopAppBarDefaults 对象

| 常量 / 函数 | 类型 | 值 | 来源 |
| ----------- | ---- | -- | ---- |
| Height | Dp | 52.dp | 相册 `toolbar` 节点 184px ÷ 3.53125 px/dp = 52.1dp；复用 `TopAppBarDefaults.CollapsedHeight` |
| StartPadding | Dp | 16.dp | 标题 `TextView` 起点 x=57px = 16.1dp |
| EndPadding | Dp | 16.dp | 最后一个操作距边缘 56px = 15.9dp |
| ActionSpacing | Dp | 10.dp | 两个操作相距 35px = 9.9dp；即 `TopAppBarDefaults.ActionMenuItemSpacing` |
| ActionItemSize | Dp | 40.dp | 操作为 141px = 39.9dp 方形；即 `TopAppBarDefaults.ActionItemMinWidth` |
| NavigationIconGap | Dp | 10.dp | 相册未实测；取设置工具栏的 `TopAppBarDefaults.NavigationIconGap` |
| TitleActionGap | Dp | 4.dp | `TopAppBarDefaults.ActionIconGap` |
| titleStyle() | TextStyle | `title1`（32sp） | 「照片」墨迹高 29.2dp、两字宽 59dp；字重未实测 |

## 实测数据

取自 ColorOS 17 相册「照片」页，设备为 OnePlus PLK110（ColorOS V17.0.0，1272x2772），深色主题下通过 `uiautomator dump` 与像素取样得到。`com.coloros.gallery3d` 以 565 dpi 渲染，即 3.53125 px/dp（短边 360dp）。

- 状态栏 141px；`toolbar` 节点 y 141..325，184px = 52.1dp。
- 标题起点 x=57px（16dp）；墨迹范围 x 63..271、y 181..283，与 32dp 标题字号一致。
- 两个操作（`action_main_tab_search` 与更多选项）均为 141px = 40dp 方形，相距 35px = 10dp，距末尾边缘 56px = 16dp，在行内垂直居中。
- 时间线滚动约 1000px 后，标题与操作的边界保持不变。
- 深色页面背景实测 48 / 48 / 48 = `AppBarBlurOverlayDefaults.DarkContainerColor`。

## 相册密度

相册不跟随「显示大小」设置：`BaseActivity.adjustDensity` 在加载任何视图之前就把资源钉在一个固定 dpi 上，所以无论用户选哪档密度，它的顶栏按钮、底栏和两侧配件球的像素尺寸都不变。`rememberGalleryDensity()` 返回的正是这个密度，并保留平台的 `fontScale`：

- 支持切换屏幕分辨率的机型（`oplus.software.display.resolution_switch_support` 特性）把屏幕短边钉为 360dp，即 `(shortEdgePx / 360 * 160).toInt()`：1272px 的 PLK110 上为 565 dpi = 3.53125 px/dp。
- 其余机型直接使用屏幕自身的物理密度：1240px 的 PKT110 上为 560 dpi = 3.5 px/dp，而系统其余部分为 620 dpi。此时 56dp 的配件球实测 196px、40dp 的顶栏按钮 140px，与真实相册一致。

```kotlin
import io.github.suqi8.coui.kmp.utils.rememberGalleryDensity

CompositionLocalProvider(LocalDensity provides rememberGalleryDensity()) {
    BottomFloatingNavigationBar { /* 条目 */ }
}
```

这些控件外围的布局（对齐、系统栏内边距、外边距）保持系统密度。锚定的弹出菜单会自行换算到屏幕物理密度，放在作用域内外都可以。桌面、Apple 与 Web 平台没有显示大小覆盖，返回平台密度。该计算以 `GalleryDensityDefaults.densityDpi(physicalDpi, shortEdgePx, resolutionSwitch)` 公开。
