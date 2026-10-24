# TopAppBar

`TopAppBar` 是 COUI 中的顶部应用栏组件，用于在界面顶部提供导航、标题和操作按钮。支持大标题模式和普通模式，以及滚动时的动态效果。

此组件通常与 `Scaffold` 组件结合使用，以便在应用程序的不同页面中保持一致的布局和行为。

<div style="position: relative; height: 300px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../../compose/index.html?id=topAppBar" title="Demo" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin"></iframe>
</div>

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.TopAppBar
import io.github.suqi8.coui.kmp.basic.SmallTopAppBar
import io.github.suqi8.coui.kmp.basic.COUIScrollBehavior
import io.github.suqi8.coui.kmp.basic.rememberTopAppBarState
```

## 基本用法

### 小标题顶部栏

```kotlin
Scaffold(
    topBar = {
        SmallTopAppBar(
            title = "标题",
            navigationIcon = {
                IconButton(onClick = { /* 处理点击事件 */ }) {
                    Icon(COUIIcons.Back, contentDescription = "返回")
                }
            },
            actions = {
                IconButton(onClick = { /* 处理点击事件 */ }) {
                    Icon(COUIIcons.More, contentDescription = "更多")
                }
            }
        )
    }
)
```

### 大标题顶部栏

```kotlin
Scaffold(
    topBar = {
        TopAppBar(
            title = "标题",
            largeTitle = "大标题", // 如果不指定，将使用 title 的值
            navigationIcon = {
                IconButton(onClick = { /* 处理点击事件 */ }) {
                    Icon(COUIIcons.Back, contentDescription = "返回")
                }
            },
            actions = {
                IconButton(onClick = { /* 处理点击事件 */ }) {
                    Icon(COUIIcons.More, contentDescription = "更多")
                }
            }
        )
    }
)
```

### 带副标题

两种顶部栏都可以在标题下方显示副标题。可折叠的 `TopAppBar` 中副标题默认随折叠渐隐；设置 `hideSubtitleOnCollapse = false` 可让其保持可见并滑入折叠栏标题下方：

```kotlin
TopAppBar(
    title = "标题",
    largeTitle = "大标题",
    subtitle = "副标题",
    hideSubtitleOnCollapse = false
)
```

## 大标题顶部栏滚动行为（使用脚手架）

TopAppBar 支持随内容滚动时改变其显示状态：

```kotlin
val scrollBehavior = COUIScrollBehavior()

Scaffold(
    topBar = {
        TopAppBar(
            title = "标题",
            largeTitle = "大标题", // 如果不指定，将使用 title 的值
            scrollBehavior = scrollBehavior
        )
    }
) { paddingValues ->
    // 内容区域需要考虑 padding
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            // 如需添加越界回弹效果，则应在绑定滚动行为之前添加
            .overScrollVertical()
            // 绑定 TopAppBar 滚动事件
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(top = paddingValues.calculateTopPadding())
    ) {
        // 列表内容
    }
}
```

## 自定义样式

### 自定义颜色

```kotlin
TopAppBar(
    title = "标题",
    color = COUITheme.colorScheme.primary,
    titleColor = COUITheme.colorScheme.onPrimary,
    largeTitleColor = COUITheme.colorScheme.onPrimary
)
```

### 自定义内容边距

```kotlin
TopAppBar(
    title = "标题",
    titlePadding = 32.dp
)
```

### 自定义图标边距

```kotlin
TopAppBar(
    title = "标题",
    navigationIconPadding = 12.dp,
    actionIconPadding = 12.dp
)
```

## 居中标题与嵌入分段槽位

`SmallTopAppBar` 还覆盖了 COUI 工具栏两个与折叠标题无关的形态：居中标题与嵌入分段槽位。
两者都只在 `SmallTopAppBar` 上提供——COUI 里它们属于普通 `COUIToolbar`，折叠栏没有对应证据。

### 居中标题

`centerTitle = true` 会让标题（以及副标题）在导航图标与操作区之间居中，对应 COUI 的
`setIsTitleCenterStyle`。该形态下栏两端保留的是 `centerTitleMargin`，取代常规的
`titlePadding` / `navigationIconPadding` / `actionIconPadding`；这个留白跟随 COUI 的宽度档位：
低于 600dp 为 16dp，600dp 到 840dp 为 24dp，840dp 及以上为 40dp。

```kotlin
SmallTopAppBar(
    title = "标题",
    centerTitle = true,
    navigationIcon = { /* ... */ },
    actions = { /* ... */ }
)
```

显式传入 `centerTitleMargin` 可以固定留白，不再跟随窗口宽度。

### 分段槽位

`segmentButton` 会在导航图标与标题之间嵌入一个分段控件，对应 COUI 的 `setSegmentButtons(View)`。
该槽位高 32dp，宽至少 108dp、最多 207dp，标题需要空间时会收缩；`centerTitle` 为 true 时
完全不参与布局——两种形态互斥。

```kotlin
SmallTopAppBar(
    title = "标题",
    segmentButton = {
        Row(Modifier.padding(4.dp)) { /* 分段按钮 */ }
    }
)
```

### 渐变玻璃

`gradientGlass = true` 会把顶栏材质从均匀模糊换成 COUI 的渐变形态。材质并不画在顶栏本身上：
它是一层材质，测量为顶栏加上 `AppBarBlurOverlayDefaults.ExtraHeight`（24dp），画在顶栏内容之后；
布局只报告顶栏自身的高度，因此材质会越过顶栏的下边缘，却不改变顶栏占用的空间。背景在顶边模糊最强，
顶栏的下边缘仍保留一些模糊与表面 alpha，它们在额外的那段带子上淡出。材质开启时会隐藏分割线，
避免分割线切过淡出区域。

配合 `scrollBehavior` 使用时，材质在静止时不可见，内容滚过顶栏 30dp 后达到满强度；不配
`scrollBehavior` 时材质始终开启。

它和均匀材质一样需要 `LocalBackdrop`；没有 backdrop 或不支持 runtime shader 时，只保留渐变颜色填充。

```kotlin
CompositionLocalProvider(LocalBackdrop provides backdrop) {
    SmallTopAppBar(title = "标题", gradientGlass = true)
}
```

### 需要 edge-to-edge

材质靠采样顶栏**底下**的内容成像。页面若把内容排在顶栏下方（`Scaffold` 的默认布局，内容被
`Modifier.padding` 顶下去），顶栏底下就没有任何像素可采：采样会钳到最近的一行，也就是内容的
第一行，于是顶栏被糊成一条平色横带，下边缘还留着一道硬切边。

所以 COUI 只在 `LocalEdgeToEdge` 为 `true` 时才画材质。给 `Scaffold` 传 `edgeToEdge = true`
（或自己 `CompositionLocalProvider(LocalEdgeToEdge provides true)`），并把顶栏高度从
`PaddingValues` 挪进列表自己的 `contentPadding`，让内容真的滚到顶栏底下：

```kotlin
Scaffold(edgeToEdge = true, topBar = { SmallTopAppBar(title = "标题", gradientGlass = true) }) { padding ->
    LazyColumn(contentPadding = PaddingValues(top = padding.calculateTopPadding())) { /* … */ }
}
```

没开 `edgeToEdge` 时顶栏保留自己的 `color` 纯色表面（分割线照常），不会出现那条色带；
`gradientGlass` 与均匀模糊一并降级。

### 渐变玻璃叠加层

`gradientGlass` 把材质布局在顶栏本身的后面。若页面把材质放在自己布局的独立节点上——ColorOS 17
主页把 `AppBarBlurOverlayView` inflate 成 app bar 的同级视图，并把导航栏掏空——请改用
[`AppBarBlurOverlay`](../components/appbarbluroverlay)。两者共用相同的测量行为，以及 `AppBarGlassDefaults`
的模糊半径与混合曲线；区别在于由谁布局节点，以及 alpha 从哪里来：`AppBarBlurOverlay` 由 `scrollFraction`
提供，而 `gradientGlass` 从顶栏的 `scrollBehavior` 推导。

## 属性

### TopAppBar 属性

| 属性名                     | 类型                            | 说明                           | 默认值                            | 是否必须 |
| -------------------------- | ------------------------------- | ------------------------------ | --------------------------------- | -------- |
| title                      | String                          | 顶部栏标题                     | -                                 | 是       |
| modifier                   | Modifier                        | 应用于顶部栏的修饰符           | Modifier                          | 否       |
| color                      | Color                           | 顶部栏背景颜色                 | COUITheme.colorScheme.surface    | 否       |
| titleColor                 | Color                           | 折叠时小标题文字颜色           | COUITheme.colorScheme.onSurface  | 否       |
| largeTitle                 | String                          | 大标题文本                     | title                             | 否       |
| largeTitleColor            | Color                           | 展开时大标题文字颜色           | COUITheme.colorScheme.onSurface  | 否       |
| subtitle                   | String                          | 显示在标题栏下方的副标题文本   | ""                                | 否       |
| subtitleColor              | Color                           | 副标题文字颜色                 | COUITheme.colorScheme.onSurfaceVariantSummary  | 否       |
| dividerColor               | Color                           | 折叠时显现的底部细分割线颜色   | COUITheme.colorScheme.dividerLine | 否       |
| navigationIcon             | @Composable () -> Unit          | 导航图标区域的可组合函数       | {}                                | 否       |
| actions                    | @Composable RowScope.() -> Unit | 操作按钮区域的可组合函数（建议使用 24dp 图标） | {}                                | 否       |
| scrollBehavior             | ScrollBehavior?                 | 控制顶部栏滚动行为             | null                              | 否       |
| defaultWindowInsetsPadding | Boolean                         | 是否应用默认窗口边距           | true                              | 否       |
| showDivider                | Boolean                         | 是否绘制随折叠渐显的底部细分割线 | true                              | 否       |
| hideSubtitleOnCollapse     | Boolean                         | 副标题是否随折叠渐隐；为 false 时保持不透明并滑入折叠栏标题下方 | true                              | 否       |
| titlePadding          | Dp                              | 水平内容边距                   | TopAppBarDefaults.TitlePadding | 否       |
| navigationIconPadding      | Dp                              | 导航图标的起始边距             | TopAppBarDefaults.NavigationIconPadding | 否       |
| actionIconPadding          | Dp                              | 操作图标的末尾边距             | TopAppBarDefaults.ActionIconPadding | 否       |
| gradientGlass              | Boolean                         | 是否改用 ColorOS 渐变模糊顶栏材质而非均匀模糊；材质越过顶栏下边缘 `AppBarBlurOverlayDefaults.ExtraHeight`（24dp），顶栏高度不变，并隐藏分割线；配合 `scrollBehavior` 时在滚动 30dp 后出现（见「渐变玻璃」）；需要 `Scaffold(edgeToEdge = true)`，否则降级为纯色 | false | 否 |
| bottomContent              | @Composable () -> Unit          | 显示在标题栏下方的可组合内容 | {}                                | 否       |

### SmallTopAppBar 属性

| 属性名                     | 类型                            | 说明                     | 默认值                            | 是否必须 |
| -------------------------- | ------------------------------- | ------------------------ | --------------------------------- | -------- |
| title                      | String                          | 顶部栏标题               | -                                 | 是       |
| modifier                   | Modifier                        | 应用于顶部栏的修饰符     | Modifier                          | 否       |
| color                      | Color                           | 顶部栏背景颜色           | COUITheme.colorScheme.surface    | 否       |
| titleColor                 | Color                           | 标题文字颜色             | COUITheme.colorScheme.onSurface  | 否       |
| subtitle                   | String                          | 显示在标题栏下方的副标题文本 | ""                                | 否       |
| subtitleColor              | Color                           | 副标题文字颜色           | COUITheme.colorScheme.onSurfaceVariantSummary  | 否       |
| dividerColor               | Color                           | 滚动时显现的底部细分割线颜色 | COUITheme.colorScheme.dividerLine | 否       |
| navigationIcon             | @Composable () -> Unit          | 导航图标区域的可组合函数 | {}                                | 否       |
| actions                    | @Composable RowScope.() -> Unit | 操作按钮区域的可组合函数（建议使用 24dp 图标） | {}                                | 否       |
| scrollBehavior             | ScrollBehavior?                 | 控制顶部栏滚动行为       | null                              | 否       |
| defaultWindowInsetsPadding | Boolean                         | 是否应用默认窗口边距     | true                              | 否       |
| showDivider                | Boolean                         | 内容在栏下滚动时是否绘制底部细分割线（需要 scrollBehavior） | true    | 否       |
| centerTitle                | Boolean                         | 是否让标题在两个图标槽之间居中（对应 COUI `setIsTitleCenterStyle`）；该形态下下方三个边距会被 `centerTitleMargin` 取代 | false | 否 |
| centerTitleMargin          | Dp                              | 居中标题两端保留的水平留白     | 按当前窗口宽度取档位值 | 否 |
| segmentButton              | (@Composable () -> Unit)?       | 嵌入在导航图标与标题之间的分段控件槽位；`centerTitle` 为 true 时不参与布局 | null | 否 |
| gradientGlass              | Boolean                         | 是否改用 ColorOS 渐变模糊顶栏材质而非均匀模糊；材质越过顶栏下边缘 `AppBarBlurOverlayDefaults.ExtraHeight`（24dp），顶栏高度不变，并隐藏分割线；配合 `scrollBehavior` 时在滚动 30dp 后出现（见「渐变玻璃」）；需要 `Scaffold(edgeToEdge = true)`，否则降级为纯色 | false | 否 |
| titlePadding          | Dp                              | 水平内容边距             | TopAppBarDefaults.TitlePadding | 否       |
| navigationIconPadding      | Dp                              | 导航图标的起始边距       | TopAppBarDefaults.NavigationIconPadding | 否       |
| actionIconPadding          | Dp                              | 操作图标的末尾边距       | TopAppBarDefaults.ActionIconPadding | 否       |
| bottomContent              | @Composable () -> Unit          | 显示在标题栏下方的可组合内容 | {}                                | 否       |

### TopAppBarDefaults 对象

TopAppBarDefaults 对象提供了 TopAppBar 和 SmallTopAppBar 组件的默认值。

#### 常量

| 常量名                    | 类型 | 说明                              | 默认值 |
| ------------------------- | ---- | --------------------------------- | ------ |
| TitlePadding    | Dp   | 标题和大标题的水平内边距          | 16.dp  |
| LargeTitleTopPadding | Dp | 展开态大标题的顶部内边距          | 54.dp  |
| NavigationIconPadding      | Dp   | 导航图标的起始边距               | 16.dp  |
| ActionIconPadding          | Dp   | 操作图标的末尾边距               | 16.dp  |
| CollapsedHeight            | Dp   | TopAppBar 折叠时的高度           | 52.dp  |
| ExpandedHeight             | Dp   | 无副标题时 TopAppBar 展开态的最小高度 | 107.dp |
| SmallTopAppBarCenterHeight | Dp   | SmallTopAppBar 布局的垂直中心高度 | 52.dp  |
| LargeTitleBottomPadding    | Dp   | 展开态大标题区块下方的底部边距（折叠时渐变至 0.dp） | 12.dp   |
| SubtitleBottomPadding      | Dp   | 副标题溢出折叠栏时其下方的底部边距 | 8.dp   |
| SubtitleMarginTop          | Dp   | 标题与副标题之间的垂直间距        | 3.5.dp |
| NavigationIconGap          | Dp   | 导航图标与折叠态标题之间的水平间距 | 4.dp   |
| ActionIconGap              | Dp   | 标题与操作图标之间的水平间距      | 8.dp   |
| ActionMenuItemSpacing      | Dp   | 操作菜单项之间的间距              | 10.dp  |
| CenterTitleMarginCompat    | Dp   | 居中标题的水平留白，compat 档（低于 600dp） | 16.dp |
| CenterTitleMarginMedium    | Dp   | 居中标题的水平留白，medium 档（600–840dp） | 24.dp |
| CenterTitleMarginExpanded  | Dp   | 居中标题的水平留白，expanded 档（840dp 及以上） | 40.dp |
| CenterTitlePadding         | Dp   | 窗口宽度未知时居中标题的回退内边距 | 4.dp   |
| SectionGap                 | Dp   | 分段槽位两侧保留的间距，medium / expanded 档 | 40.dp |
| SectionGapSmall            | Dp   | compat 档的分段间距               | 0.dp   |
| ActionMenuInnerPadding     | Dp   | 操作菜单容器内部的内边距，计入居中标题的槽位 | 0.dp |
| SegmentButtonHeight        | Dp   | 分段槽位的高度                    | 32.dp  |
| SegmentButtonMinWidth      | Dp   | 低于该宽度时分段槽位不再被视为能放在标题旁 | 108.dp |
| SegmentButtonMaxWidth      | Dp   | compat 档分段槽位的宽度上限       | 207.dp |

### ScrollBehavior

COUIScrollBehavior 是用于控制顶部栏滚动行为的配置对象。

#### rememberTopAppBarState

用于创建和记住 TopAppBarState：

```kotlin
val scrollBehavior = COUIScrollBehavior(
    state = rememberTopAppBarState(),
    canScroll = { true }
)
```

| 参数名             | 类型                        | 默认值                     | 说明                       |
| ------------------ | --------------------------- | -------------------------- | -------------------------- |
| state              | TopAppBarState              | rememberTopAppBarState()   | 控制滚动状态的状态对象     |
| canScroll          | () -> Boolean               | { true }                   | 控制是否允许滚动的回调     |
| snapAnimationSpec  | AnimationSpec\<Float>?      | 180ms 减速 tween           | 定义展开/折叠两态之间的过渡动画，以及旧的连续模式滚动停在中间态时的吸附动画 |
| flingAnimationSpec | DecayAnimationSpec\<Float>? | rememberSplineBasedDecay() | 定义顶部栏滑动的衰减动画   |
| expandOnContentScroll | Boolean                  | false                      | 列表在任意位置向下拖动时连续展开顶栏，而不是把标题吸附到工具栏行、只在顶部展开 |

默认情况下顶栏是吸附的：列表滚动时标题固定在 52dp 工具栏行内，只有列表回到顶部后大标题才展开到第二行；列表中间的小幅滚动不会把头部停在半折叠状态。需要 ColorOS 17 首页那种连续行为时设置 `expandOnContentScroll = true`，顶栏会在列表任意位置跟随内容的实际向下位移：

```kotlin
val scrollBehavior = COUIScrollBehavior(expandOnContentScroll = true)
```

顶栏跟随内容的实际向下位移，与列表同步移动，而不是按收起比例一次性弹开。这样，由同一段位移驱动 alpha 的材质——例如 [`AppBarBlurOverlay`](./appbarbluroverlay)——就不会在顶栏展开时变淡。该参数不会额外消费滚动：列表已经消费的位移仍归列表。

## 进阶用法

### 处理窗口边距

```kotlin
TopAppBar(
    title = "标题",
    largeTitle = "大标题",
    defaultWindowInsetsPadding = false // 自行处理窗口嵌入边距
)
```

### 自定义滚动行为动画

```kotlin
var isScrollingEnabled by remember { mutableStateOf(true) }
val scrollBehavior = COUIScrollBehavior(
    snapAnimationSpec = tween(durationMillis = 100),
    flingAnimationSpec = rememberSplineBasedDecay(),
    canScroll = { isScrollingEnabled } // 可以动态控制是否允许滚动
)

TopAppBar(
    title = "标题",
    largeTitle = "大标题",
    scrollBehavior = scrollBehavior
)
```

### 大标题和小标题结合使用

```kotlin
var useSmallTopBar by remember { mutableStateOf(false) }

Box(modifier = Modifier.fillMaxSize()) {
    if (useSmallTopBar) {
        SmallTopAppBar(
            title = "精简模式",
            navigationIcon = {
                IconButton(onClick = { useSmallTopBar = false }) {
                    Icon(
                        imageVector = COUIIcons.Back,
                        contentDescription = "切换到大标题",
                        tint = COUITheme.colorScheme.onBackground
                    )
                }
            }
        )
    } else {
        TopAppBar(
            title = "标题",
            largeTitle = "展开模式",
            navigationIcon = {
                IconButton(onClick = { useSmallTopBar = true }) {
                    Icon(
                        imageVector = COUIIcons.Back,
                        contentDescription = "切换到小标题",
                        tint = COUITheme.colorScheme.onBackground
                    )
                }
            }
        )
    }
}
```
