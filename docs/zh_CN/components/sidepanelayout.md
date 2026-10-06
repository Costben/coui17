# SidePaneLayout

`SidePaneLayout` 是贴在页面起始边的可拖拽侧面板。面板里放导航列表，内容在它背后，面板左上角的圆形折角按钮负责开合。设置 `coverStyle` 时面板盖在内容之上并用 `coui_color_mask` 遮罩压暗内容；否则内容被面板后缘推开。开合走 483ms 的移动缓动，拖拽松手时也可按抛掷速度归位。

它复刻了 ColorOS 的侧栏，后者继承 `android.widget.RelativeLayout`。

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.SidePaneLayout
import io.github.suqi8.coui.kmp.basic.SidePaneDefaults
import io.github.suqi8.coui.kmp.basic.SidePaneFoldIconColors
import io.github.suqi8.coui.kmp.basic.SidePaneLayoutState
import io.github.suqi8.coui.kmp.basic.SidePaneMaskListener
import io.github.suqi8.coui.kmp.basic.SidePanePanelSlideListener
import io.github.suqi8.coui.kmp.basic.SidePaneState
import io.github.suqi8.coui.kmp.basic.SidePaneUtils
import io.github.suqi8.coui.kmp.basic.rememberSidePaneLayoutState
```

## 设备实现

设备 View 布局三个子 View：下标 0 是可滑动面板，下标 1 是内容，下标 2 是 `layout/coui_sliding_icon_layout.xml` 膨胀出的折角按钮——一个以 `drawable/coui_pane_icon_bg` 为背景、带 `coui_side_pane_layout_icon_padding`（12dp）内边距、`src` 为 `drawable/coui_pane_fold_normal` 的 `ImageButton`。其中只有下标 0、1 在 Compose 里有对应槽位；下标 2 由本组件画在面板左上角，偏移取 `coui_side_pane_layout_icon_margin_start`（12dp）与 `coui_side_pane_layout_icon_margin_top`（45dp）。

`ViewDragHelper` 以 0.5 的灵敏度捕获拖拽，最小抛掷速度为 `MIN_FLING_VELOCITY`（400 dp/s）。开合是一个 `DURATION_OF_SLIDING_ANIMATOR`（483ms）的 `ValueAnimator`，由 `COUIMoveEaseInterpolator` 驱动，即 `PathInterpolator(0.3f, 0.0f, 0.1f, 1.0f)`。设备对松手后的拖拽走 `settleCapturedViewAt`，未声明独立时长，因此本组件对「手势松手」与「程序化开合」统一使用同一个 483ms 移动缓动。

遮罩为 `coui_color_mask`（`#33000000`），以自身 alpha 乘以滑动比例绘制，且只在 `coverStyle` 或 `alwaysShowMask` 成立时出现。`setAlwaysShowMask` 让遮罩在可点击关闭时保留；`setTouchContentEnable`（默认 true）决定点击面板外是否关闭。

`SidePaneUtils` 承载响应式辅助：`isLargeScreenStyle` 要求宽 600dp、高 900dp；`isMediumScreenStyle` 要求宽 600dp 且高至少 480dp，或处于多窗口；`makeFirstEditIconAnim` 在滑动最后十分之一里把折角图标 alpha 拉起；`makeSecToolbarAnim` 把次级工具栏的起始边距从 `coui_side_pane_layout_toolbar_margin_start`（72dp）降到 0。

> **调用点说明。** 被研究的两个系统应用都没有直接引用这个侧栏类；ColorOS 的图库侧栏是构建在侧边导航栏之上的另一个组件（见 [SideNavigationBar](../components/sidenavigationbar)）。因此本组件的演示位置是依据设计语言重建的，并非照搬某个应用页面。

## 基本用法

```kotlin
val paneState = rememberSidePaneLayoutState()

SidePaneLayout(
    paneContent = { NavigationList() },
    state = paneState,
    coverStyle = true,
    alwaysShowMask = true
) {
    PhotoGrid()
}
```

## 参数

| 参数 | 类型 | 说明 |
| :--- | :--- | :--- |
| `paneContent` | `@Composable () -> Unit` | 面板内容，布局在起始边 |
| `modifier` | `Modifier` | 施加在整个布局上的修饰符 |
| `state` | `SidePaneLayoutState` | 面板状态，默认 `rememberSidePaneLayoutState()` |
| `paneWidth` | `Dp` | 面板宽度，即 `firstPaneWidth` / `expandPaneWidth` 属性默认值 `coui_sliding_pane_width`（224dp） |
| `coverStyle` | `Boolean` | `coverStyle` 属性（默认 false）。为 true 时内容保持满宽、面板覆盖其上；为 false 时内容被面板后缘推开 |
| `alwaysShowMask` | `Boolean` | `setAlwaysShowMask`（默认 false）。与 `coverStyle` 一起决定是否启用 `coui_color_mask` 遮罩 |
| `touchContentEnabled` | `Boolean` | `setTouchContentEnable`（设备默认 true）。只有它、`coverStyle` 与打开状态同时成立，点击面板外才关闭 |
| `showFoldIcon` | `Boolean` | `setCreateIcon`（设备默认 true）。为 false 时不放置折角按钮 |
| `foldIcon` | `ImageVector` | 折角按钮内的图形，默认 `COUIIcons.Basic.FoldPane` |
| `slideListener` | `SidePanePanelSlideListener?` | 对应 `setPanelSlideListener` |
| `maskListener` | `SidePaneMaskListener?` | 对应 `setOnMaskClickListener` |
| `scrimColor` | `Color` | 遮罩颜色，`coui_color_mask`（`#33000000`） |
| `paneColor` | `Color` | 面板背景；设备未绘制，移植默认取 `surface` |
| `foldIconColors` | `SidePaneFoldIconColors` | 折角按钮的颜色 |
| `content` | `@Composable () -> Unit` | 面板滑过的页面内容 |

### SidePaneLayoutState

| 成员 | 类型 | 说明 |
| :--- | :--- | :--- |
| `state` | `SidePaneState` | 静止状态，`Open`（0）或 `Closed`（1），对应 `isOpen()` 取反 |
| `isOpen` | `Boolean` | 面板是否被要求打开 |
| `slideOffset` | `Float` | 当前位置，0f 隐藏、1f 完全展开；对应 `mSlideOffset` |
| `isSliding` | `Boolean` | 开合动画是否在运行；对应 `isSliding()` |
| `open()` / `close()` / `toggle()` | — | `openPane()` / `closePane()` 以及折角按钮的翻转 |

### SidePaneUtils

| 常量 | 类型 | 值 | COUI 来源 |
| :--- | :--- | :--- | :--- |
| `LARGE_SCREEN_WIDTH_THRESHOLD` | `Int` | 600 | `COUISidePaneUtils.LARGE_SCREEN_WIDTH_DP_THRESHOLD` |
| `LARGE_SCREEN_HEIGHT_THRESHOLD` | `Int` | 900 | `COUISidePaneUtils.LARGE_SCREEN_HEIGHT_DP_THRESHOLD` |
| `SMALLEST_SCREEN_HEIGHT_THRESHOLD` | `Int` | 480 | `COUISidePaneUtils.SMALLEST_SCREEN_HEIGHT_DP_THRESHOLD` |
| `START_EDIT_ICON_ALPHA_OFFSET` | `Float` | 0.9f | `COUISidePaneUtils.START_EDIT_ICON_ALPHA_OFFSET` |

`isLargeScreenStyle(windowSize)`、`isMediumScreenStyle(windowSize, inMultiWindowMode)`、`editIconAlpha(progress)` 与 `secondaryToolbarMarginStart(progress, margin)` 是四个设备方法的纯函数版本，入参即调用方原本写进 View 的值。

### SidePaneDefaults

| 常量 | 类型 | 值 | COUI 来源 |
| :--- | :--- | :--- | :--- |
| `PaneWidth` | `Dp` | 224.dp | `dimen/coui_sliding_pane_width` |
| `OverhangSize` | `Dp` | 32.dp | `COUISidePaneLayout.DEFAULT_OVERHANG_SIZE` |
| `DURATION_MILLIS` | `Int` | 483 | `COUISidePaneLayout.DURATION_OF_SLIDING_ANIMATOR` |
| `MinFlingVelocity` | `Dp` | 400.dp | `COUISidePaneLayout.MIN_FLING_VELOCITY` |
| `COVER_STYLE` | `Boolean` | false | `coverStyle` 属性默认值 |
| `ALWAYS_SHOW_MASK` | `Boolean` | false | `setAlwaysShowMask` 默认值 |
| `TOUCH_CONTENT_ENABLED` | `Boolean` | true | `setTouchContentEnable` 默认值 |
| `CREATE_ICON` | `Boolean` | true | `setCreateIcon` 默认值 |
| `FoldIconSize` | `Dp` | 48.dp | `dimen/coui_side_pane_layout_icon_size` |
| `FoldIconHeight` | `Dp` | 52.dp | `coui_pane_fold_normal` 视口 |
| `FoldIconPadding` | `Dp` | 12.dp | `dimen/coui_side_pane_layout_icon_padding` |
| `FoldIconTouchSize` | `Dp` | 72.dp | `FoldIconSize + FoldIconPadding * 2` |
| `FoldIconMarginTop` | `Dp` | 45.dp | `dimen/coui_side_pane_layout_icon_margin_top` |
| `FoldIconMarginStart` | `Dp` | 12.dp | `dimen/coui_side_pane_layout_icon_margin_start` |
| `ToolbarMarginStart` | `Dp` | 72.dp | `dimen/coui_side_pane_layout_toolbar_margin_start` |
| `FOLD_ICON_CONTENT_DESCRIPTION` | `String` | "Shrink and expand button" | `string/coui_side_pane_icon_content_description` |

`scrimColor()` 解析为 `coui_color_mask`，`paneColor()` 解析为 `surface`，`foldIconColors()` 解析为 `coui_pane_icon_bg_color`（浅色 `#0d000000`、深色 `#1affffff`）配 `couiColorLabelPrimary` 着色。

## 取值溯源

下表每个值均对标 ColorOS 17 实现，无一处编造。

| Token | 值 | 来源 |
| :--- | :--- | :--- |
| 面板宽度 | 224dp | `dimen/coui_sliding_pane_width` |
| 滑动时长 | 483ms | `COUISidePaneLayout.DURATION_OF_SLIDING_ANIMATOR` |
| 移动缓动 | `cubic-bezier(0.3, 0, 0.1, 1)` | `COUIMoveEaseInterpolator`，`PathInterpolator(0.3f, 0f, 0.1f, 1f)` |
| 最小抛掷速度 | 400dp/s | `COUISidePaneLayout.MIN_FLING_VELOCITY` |
| 越界尺寸 | 32dp | `COUISidePaneLayout.DEFAULT_OVERHANG_SIZE` |
| 开 / 关状态 | 0 / 1 | `COUISidePaneLayout.OPEN_STATE` / `CLOSE_STATE` |
| 遮罩 | `#33000000` | `color/coui_color_mask` |
| 折角图标尺寸 | 48dp | `dimen/coui_side_pane_layout_icon_size` |
| 折角图标内边距 | 12dp | `dimen/coui_side_pane_layout_icon_padding` |
| 折角图标边距 | 12dp / 45dp | `coui_side_pane_layout_icon_margin_start` / `_margin_top` |
| 工具栏边距 | 72dp | `dimen/coui_side_pane_layout_toolbar_margin_start` |
| 折角图标背景 | 浅色 `#0d000000`、深色 `#1affffff` | `color/coui_pane_icon_bg_color` |
| 折角图形 | 48x52dp、1.4 描边 | `drawable/coui_pane_fold_normal` |
| 大屏 | 宽 600dp、高 900dp | `COUISidePaneUtils.LARGE_SCREEN_WIDTH_DP_THRESHOLD` / `_HEIGHT_` |
| 中屏 | 宽 600dp、高 480dp | `COUISidePaneUtils.SMALLEST_SCREEN_HEIGHT_DP_THRESHOLD` |
| 编辑图标 alpha 起点 | 0.9f | `COUISidePaneUtils.START_EDIT_ICON_ALPHA_OFFSET` |
| 折角图标描述 | "Shrink and expand button" | `string/coui_side_pane_icon_content_description` |

## 未移植

- **带 `firstPaneWidth` / `expandPaneWidth` 的子 View `LayoutParams`** — 面板是槽位，宽度由单一 `paneWidth` 参数给出。
- **拖拽与遮罩点击之外的 `onInterceptTouchEvent` / `onTouchEvent`** — 移植改用 `Modifier.draggable` 与遮罩点击，不再自建 `ViewDragHelper` 回调。
- **`AccessibilityDelegate` 与 `ExploreByTouchHelper`** — 面板与内容都是普通 composable，只有折角按钮带 `contentDescription`。
- **`COUISidePaneLifeCycleObserver`** — 配置变化后的响应式重新测量交给 Compose 自身的重组。
- **`SavedState` 的 `isDefalutOpen`（设备拼写错误）与 `state` 字段** — 持久化改用 `SidePaneLayoutState.Saver`，只保存打开标志。
- **`getDrawerViewPaddingTop` / `reMeasureContentView`** — 不建模宿主自身的内容内边距，由调用方通过 `modifier` 设置。
