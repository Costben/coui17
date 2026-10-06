# SideNavigationBar

`SideNavigationBar` 是 ColorOS 的侧边导航抽屉：导航列表从起始边滑出，覆盖在页面之上。窗口较窄时它浮在内容上方，用 `coui_color_mask` 遮罩压暗内容；窗口足够宽后变成固定在内容旁边的窄栏，遮罩撤掉、内容仍可交互。20dp 的屏幕边缘条与抽屉本身的拖拽都能移动它，松手后按越过的半程归位。

对应 ColorOS 的侧边导航抽屉，后者继承 `androidx.drawerlayout.widget.DrawerLayout`。

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.SideNavigationBar
import io.github.suqi8.coui.kmp.basic.SideNavigationBarDefaults
import io.github.suqi8.coui.kmp.basic.SideNavigationBarListener
import io.github.suqi8.coui.kmp.basic.SideNavigationBarMode
import io.github.suqi8.coui.kmp.basic.SideNavigationBarState
import io.github.suqi8.coui.kmp.basic.rememberSideNavigationBarState
```

## 设备实现

设备类是带起始边抽屉子 View 和内容子 View 的 `DrawerLayout`。`updateDrawerState` 用两个权重决定 `DRAWER_MODE_*`：窗口尺寸档（compat / medium / expanded = 1 / 2 / 3）减去内容结构权重（normal / parent-child = 1 / 2），差为正取 `DRAWER_MODE_FIXED`（1），否则取 `DRAWER_MODE_TEMPORARY`（0）。`calculateDrawerWidth` 再给临时抽屉 `coui_side_navigation_bar_min_width`（300dp），给固定抽屉 `min(coui_side_navigation_bar_max_width, windowWidth * 0.382)`。`setDrawerMode` 另外为临时模式涂上 `coui_color_mask`（`mMaskColor`）、为固定模式清掉遮罩，因此固定抽屉上只有编辑态动画能把遮罩重新淡入。

`startMaskFadeAnimation` 构造 `ValueAnimator` 时没有设置时长与插值器，所以走平台默认的 300ms 与 `AccelerateDecelerateInterpolator`。抽屉自身的归位动画则继承自 `DrawerLayout`，由 `androidx.customview.widget.ViewDragHelper#settleCapturedViewAt` 承担；COUI 没有为此声明时长或曲线 token，故 `SideNavigationBarDefaults.settleDurationMillis` 复刻 `ViewDragHelper#computeAxisDuration`（基准 256ms、上限 600ms）。

## 基本用法

```kotlin
val drawerState = rememberSideNavigationBarState()

SideNavigationBar(
    drawerContent = { NavigationList() },
    state = drawerState
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = "相册",
                navigationIcon = {
                    IconButton(onClick = { drawerState.open() }) {
                        Icon(COUIIcons.Basic.FoldPane, contentDescription = "打开侧边导航")
                    }
                }
            )
        }
    ) { padding ->
        PhotoGrid(Modifier.padding(padding))
    }
}
```

## 参数

| 参数 | 类型 | 说明 |
| :--- | :--- | :--- |
| `drawerContent` | `@Composable () -> Unit` | 抽屉内容，布局在起始边 |
| `modifier` | `Modifier` | 施加在整个导航栏上的修饰符 |
| `state` | `SideNavigationBarState` | 抽屉状态，默认 `rememberSideNavigationBarState()` |
| `parentChildHierarchy` | `Boolean` | 内容结构权重由 1 提到 2（`setParentChildHierarchy`），可能把 medium 窗口降回临时抽屉 |
| `editState` | `Boolean` | 宿主处于编辑态（`setIsInEditState`）。固定抽屉上会淡入遮罩；临时抽屉上无效，与设备一致 |
| `handleEditModeMask` | `Boolean` | 设备 `getHandlerEditModeMask()` 标志；为 false 时编辑态遮罩永不淡入。默认为设备值 `true` |
| `pushContent` | `Boolean` | 内容按抽屉当前后缘让出空间而不是被覆盖。设备基类始终覆盖；这是 Gallery 的 `SidePaneLayout` 子类在自身 `t()` 里加的行为 |
| `scrimColor` | `Color` | 临时模式下覆盖内容的颜色，`coui_color_mask`（`#33000000`） |
| `dividerColor` | `Color` | 抽屉后缘细分隔线的颜色 |
| `content` | `@Composable () -> Unit` | 抽屉背后的页面内容 |

### SideNavigationBarState

| 成员 | 类型 | 说明 |
| :--- | :--- | :--- |
| `drawerWidth` | `Dp` | 布局解析出的抽屉宽度，对应 `getDrawerViewWidth()` |
| `mode` | `SideNavigationBarMode` | 布局解析出的模式，对应 `getDrawerMode()` |
| `isOpen` | `Boolean` | 抽屉是否被要求打开 |
| `slideOffset` | `Float` | 当前位置，0f 关闭、1f 完全打开 |
| `open()` / `close()` / `toggle()` | — | `openDrawer(View, boolean)` / `closeDrawer(View, boolean)` 以及折角按钮的翻转 |
| `addListener()` / `removeListener()` | — | `addSideNavigationBarListener` / `removeSideNavigationBarListener` |

### SideNavigationBarDefaults

| 常量 | 类型 | 值 | COUI 来源 |
| :--- | :--- | :--- | :--- |
| `WIDTH_PROPORTION` | `Float` | 0.382f | `COUISideNavigationBar.DRAWER_WIDTH_PROPORTION` |
| `MinWidth` | `Dp` | 300.dp | `dimen/coui_side_navigation_bar_min_width` |
| `MaxWidth` | `Dp` | 280.dp | `dimen/coui_side_navigation_bar_max_width` |
| `MinDrawerMargin` | `Dp` | 64.dp | `COUISideNavigationBar.MIN_DRAWER_MARGIN` |
| `DividerWidth` | `Dp` | 0.33.dp | `dimen/coui_side_navigation_drawer_divider_width` |
| `EdgeSize` | `Dp` | 20.dp | `ViewDragHelper.DEFAULT_EDGE_SIZE` |
| `MinFlingVelocity` | `Dp` | 400.dp | `ViewDragHelper.MIN_VELOCITY` |
| `BASE_SETTLE_DURATION_MILLIS` | `Int` | 256 | `ViewDragHelper.BASE_SETTLE_DURATION` |
| `MAX_SETTLE_DURATION_MILLIS` | `Int` | 600 | `ViewDragHelper.MAX_SETTLE_DURATION` |
| `EDIT_MASK_DURATION_MILLIS` | `Int` | 300 | `ValueAnimator` 默认值，经 `startMaskFadeAnimation` |

`scrimColor()` 解析为 `coui_color_mask`（`#33000000`），`dividerColor()` 解析为 `attr/couiColorDivider`（回退 `color/coui_color_divider`）。`drawerMode(sizeClass, parentChildHierarchy)`、`drawerWidth(mode, windowWidth)` 与 `settleDurationMillis(dx, velocity, motionRange, parentWidth)` 分别承载上面三处设备计算。

注意 `coui_side_navigation_bar_min_width`（300dp）比 `coui_side_navigation_bar_max_width`（280dp）更大。设备代码里 300dp 是临时模式宽度、280dp 是固定模式比例的上限，两个名字描述的是不同模式，并非一对 min/max。

## 取值溯源

下表每个值都对标 ColorOS 17 的实现，无一处编造。

| Token | 值 | 来源 |
| :--- | :--- | :--- |
| `WidthProportion` | 0.382 | `COUISideNavigationBar.DRAWER_WIDTH_PROPORTION` |
| 临时模式宽度 | 300dp | `dimen/coui_side_navigation_bar_min_width` |
| 固定模式宽度上限 | 280dp | `dimen/coui_side_navigation_bar_max_width` |
| 最小抽屉边距 | 64dp | `COUISideNavigationBar.MIN_DRAWER_MARGIN` |
| 分隔线宽度 | 0.33000004dp | `dimen/coui_side_navigation_drawer_divider_width` |
| 遮罩 | `#33000000` | `color/coui_color_mask` |
| 分隔线颜色 | `#1f000000` | `color/coui_color_divider` |
| 边缘条 | 20dp | `ViewDragHelper.DEFAULT_EDGE_SIZE` |
| 最小抛掷速度 | 400dp/s | `ViewDragHelper.MIN_VELOCITY` |
| 归位基准 / 上限 | 256ms / 600ms | `ViewDragHelper.BASE_SETTLE_DURATION` / `MAX_SETTLE_DURATION` |
| 编辑态遮罩淡入 | 300ms、`AccelerateDecelerateInterpolator` | `ValueAnimator` 默认值，`startMaskFadeAnimation` |

## 未移植

- **`DrawerLayout` 的 `DrawerListener` / `setDrawerLockMode` / `openDrawer(gravity)`** — 抽屉固定是起始边子 View，gravity 与锁定模式没有对应物；固定抽屉由解析出的 `SideNavigationBarMode` 表达。
- **`onDrawerSlide(View, float)` 里的抽屉 View** — `SideNavigationBarListener.onDrawerSlide` 只报 `(slideOffset, slideWidth, mode)`，丢掉 View，因为抽屉是槽位而不是子 ViewGroup。
- **超出 `coui_color_mask` 的 `setScrimColor(int)` / `getScrimColor()`** — `scrimColor` 只承载 COUI 遮罩色。
- **`COUISideNavigationBarSavedState` 的 Parcel** — 持久化改用 `SideNavigationBarState.Saver`，只保存打开标志。
- **`mExplicitDrawerState` / `mImplicitDrawerState` 的拆分** — 设备区分「用户最后移动」与「框架最后移动」，移植只保留单一 `isOpen` 命令。
