# SearchViewAnimate

`SearchViewAnimate` 是 ColorOS 的「磁吸式」搜索栏。它在两种状态之间切换：收起时图标与提示文案一起留在胶囊里，展开时输入框和尾部取消按钮接管。搜索栏的高度、垂直位移、内容透明度和背景色全部由宿主传入的一个折叠进度值推导出来，这正是它看起来「吸附」在滚动内容上的原因。

本组件对标 ColorOS 17 磁吸搜索栏。

<div style="position: relative; height: 250px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../../compose/index.html?id=searchViewAnimate" title="Demo" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin"></iframe>
</div>

## 导入

```kotlin
import io.github.suqi8.coui.kmp.basic.SearchViewAnimate
import io.github.suqi8.coui.kmp.basic.SearchViewAnimateDefaults
import io.github.suqi8.coui.kmp.basic.SearchViewAnimateType
import io.github.suqi8.coui.kmp.basic.SearchViewState
```

## 原生来源

本组件移植自 `COUISearchViewAnimate`，它**不是** `COUISearchBar` 这个类——[SearchBar](../components/searchbar) 移植的是后者，那是一个普通的胶囊输入框，点击后打开结果面板。两者的状态模型完全不同，因此分开成两个文件，`SearchBar` 的公开 API 不受影响。

原生控件带有 `STATE_NORMAL`（0）和 `STATE_EDIT`（1）两种状态，这里对应 `SearchViewState`；并有两套布局：`TYPE_INSTANT_SEARCH`（0）让尾部按钮紧跟在输入框之后，`TYPE_NON_INSTANT_SEARCH`（1）则在两者之间插入 1dp 的 `search_linear_divider`。`mSearchViewType` 初始值为 1，因此默认是 `NonInstantSearch`。

## 两种状态

`changeStateWithOutAnimation` 在 `STATE_NORMAL` 下只隐藏功能按钮和分割线，搜索视图本身保持 `alpha = 1`，因此**收起态下提示文案依然可见**。这正是搜索栏挂在工具栏里时长成的样子，也是本移植不再在 `expanded` 为 false 时把输入框淡出的原因。

进入 `STATE_EDIT` 时只做两件事：淡入功能按钮，并把输入框让窄给它；图标不动。把图标绕起始边翻转 80° 的 `startSearchIconEnterAnimator` / `startSearchIconExitAnimator` 是原生类上的公开辅助方法，类内部从不调用，只有宿主主动驱动时图标才会翻转。

输入框非空时显示搜索视图的清除按钮（`search_close_btn`）：18dp 的 `couiColorHintNeutral` 圆底加一个白叉，占原生在输入框尾部预留的 36dp 槽位。点击会通过 `onQueryChange` 回报空串，无障碍文案是 `SearchViewAnimateDefaults.CLEAR_TEXT`。

## 基本用法

```kotlin
var query by remember { mutableStateOf("") }
var expanded by remember { mutableStateOf(false) }

SearchViewAnimate(
    query = query,
    onQueryChange = { query = it },
    expanded = expanded,
    onExpandedChange = { expanded = it },
    hint = "Search settings",
    modifier = Modifier.fillMaxWidth()
)
```

## 折叠进度

`collapsePercent` 即 `setSearchViewAnimateHeightPercent`：宿主传入搜索栏所依附内容的折叠进度（0 表示完全收起到图标，1 表示完全展开），搜索栏据此推导其余全部参数。

```kotlin
var collapsePercent by remember { mutableFloatStateOf(1f) }

Column {
    SearchViewAnimate(
        query = query,
        onQueryChange = { query = it },
        expanded = expanded,
        onExpandedChange = { expanded = it },
        hint = "Search",
        collapsePercent = collapsePercent,
        modifier = Modifier.fillMaxWidth()
    )
    Slider(value = collapsePercent, onValueChange = { collapsePercent = it })
}
```

推导关系如下：

| 输出 | 公式 |
| :--- | :--- |
| 容器高度 | `max(coui_search_view_collapsed_min_height, wrapperHeight * (f / 0.7 - 0.42857146))` |
| 垂直位移 | `(animateHeight / 2) * (1 - f)` |
| 图标与输入框透明度 | `(f - 0.5) * 2` |
| 背景色 | `ArgbEvaluator(clamp(f / 0.3, 0, 1))`，从 `couiColorDivider` 过渡到 `coui_search_view_selector_color_normal` |

透明度只作用于图标和输入框：背景由 `onDraw` 直接填充，不会变透明。混色是 sRGB 逐通道插值（`ArgbEvaluator` 的算法），不是 `Color.lerp` 走的 Oklab 混色。

## 参数

| 参数 | 类型 | 说明 |
| :--- | :--- | :--- |
| `query` | `String` | 输入框中的文本 |
| `onQueryChange` | `(String) -> Unit` | 文本变化时回调 |
| `expanded` | `Boolean` | 是否处于 `SearchViewState.Edit` |
| `onExpandedChange` | `(Boolean) -> Unit` | 搜索栏请求进入或退出编辑态时回调 |
| `modifier` | `Modifier` | 应用于搜索栏的修饰符 |
| `hint` | `String` | `query` 为空时显示的占位文本，收起态同样显示 |
| `type` | `SearchViewAnimateType` | `InstantSearch` 或 `NonInstantSearch` |
| `collapsePercent` | `Float` | 折叠进度，0..1 |
| `onSearch` | `(String) -> Unit` | 输入法搜索键触发时回调当前文本 |
| `cancelText` | `String` | 尾部按钮文案（`string/coui_search_view_cancel`） |
| `cancelButton` | `(@Composable () -> Unit)?` | 完全替换尾部按钮 |
| `leadingIcon` | `ImageVector` | 起始位置的图标 |
| `colors` | `SearchViewAnimateColors` | 搜索栏配色 |

## 动画

两种状态之间本移植只驱动两条轨道，全部使用 `COUIMoveEaseInterpolator`（`pathInterpolator(0.3, 0, 0.1, 1)`）：

| 轨道 | 时长 | 常量 |
| :--- | :--- | :--- |
| 按钮透明度（即时搜索） | 350ms，另加 100ms 起始延迟 | `DEFAULT_BUTTON_ALPHA_CHANGE_DURATION`、`setStartDelay(... ? 0L : 100L)` |
| 按钮透明度（非即时搜索） | 100ms | `NON_INSTANT_SEARCH_BUTTON_ALPHA_CHANGE_DURATION` |

分割线在收起态是 `GONE`，并跟着按钮的透明度轨道淡入，而不是横向滑开。

按压反馈（`playPressAnimation` / `playReleaseAnimation`）使用 150ms 的 `COUIEaseInterpolator`（`pathInterpolator(0.33, 0, 0.67, 1)`）在常态与按压态选择器之间过渡。

## 复原的令牌

以下数值均对标 ColorOS 17 实现，没有一个是臆造的。

| 令牌 | 值 | 来源资源 |
| :--- | :--- | :--- |
| `WrapperHeight` | 40dp | `dimen/coui_search_view_wrapper_height` |
| `AnimateHeight` | 50dp | `dimen/coui_search_view_animate_height` |
| `CollapsedMinHeight` | 1dp | `dimen/coui_search_view_collapsed_min_height`（声明为 `1px`；另有 `CollapsedMinHeightPx` = 1） |
| `ICON_ROTATION_Y` | 80° | `mSearchIcon.animate().rotationY(80.0f)`；控件自身不施加 |
| `ICON_ROTATION_DURATION_MILLIS` | 150 | 同一次调用的 `setDuration(150L)` |
| `IconSize` | 24dp | `dimen/coui_search_view_icon_size` |
| `SearchIconMargin` | 12dp | `dimen/coui_search_view_search_icon_margin` |
| `IconInputGap` | 8dp | `animated_search_view` 的 `android:layout_marginStart` |
| `CornerRadius` | 18dp | `dimen/coui_search_view_corner` |
| `InputTextSize` | 15sp | `dimen/coui_search_view_input_text_size` |
| `InputEndPadding` | 16dp | `dimen/coui_search_view_auto_complete_padding_end` |
| `CancelTextSize` | 14sp | `dimen/coui_search_view_cancel_text_size` |
| `CancelButtonBackgroundRadius` | 17dp | `drawable/coui_searchview_cancel_button_bg` 的 `android:radius`（`dimen/coui_searchview_cancel_button_bg_radius` 的 24dp 未被使用） |
| `CancelButtonMinHeight` | 0dp | 布局的 `android:minHeight` |
| `CancelButtonMaxWidth` | 152dp | `dimen/coui_search_function_button_max_width` |
| `CancelButtonPaddingHorizontal` | 8dp | `dimen/coui_search_cancel_button_bg_padding_horizontal` |
| `CancelButtonPaddingVertical` | 4dp | `dimen/coui_search_cancel_button_bg_padding_vertical` |
| `CancelMarginSmall` | 4dp | `dimen/coui_search_view_cancel_margin_small` |
| `CancelMarginLarge` | 8dp | `dimen/coui_search_view_cancel_margin_large` |
| `ButtonDividerMarginStart` | 0dp | `dimen/coui_search_view_button_divider_margin_start` |
| `DividerWidth` | 1dp | `dimen/coui_search_linear_divider_width` |
| `DividerHeight` | 16dp | `drawable/search_linear_divider` 的 `android:height` |
| `ClearButtonSize` / `DiscSize` / `GlyphSize` | 36dp / 18dp / 12dp | `ic_edit_text_delete_search_view`，36dp 视口内半径 9 的圆 |
| 背景常态 | `#14000000` 浅色 / `#1affffff` 深色 | `color/coui_search_view_selector_color_normal` |
| 背景按压 | `#1e000000` 浅色 / `#33ffffff` 深色 | `color/coui_search_view_selector_color_pressed` |
| 占位文本 | `#8a000000` 浅色 / `#33ffffff` 深色 | `normalHintColor`：浅色走 `couiColorLabelSecondary`，深色走 `coui_search_view_hint_color_dark` |
| 输入文本 | `#e6000000` 浅色 / `#ffffffff` 深色 | `inputTextColor`：浅色走 `couiColorPrimaryNeutral`，深色走 `coui_search_view_input_text_color_dark` |
| 取消文案 | `#0080ff` 浅色 / `#5c9dff` 深色 | `functionalButtonTextColor`：`couiColorPrimaryText` |
| 图标 | `#8a000000` 浅色 / `#8affffff` 深色 | `couiColorSecondNeutral` |
| 清除圆底 | `#42000000` 浅色 / `#4dffffff` 深色 | `couiColorHintNeutral` |
| 分割线 | `#1f000000` 浅色 / `#33ffffff` 深色 | `color/coui_color_divider` |
| 取消按钮文案 | `"Cancel"` | `string/coui_search_view_cancel` |
| `ClampAnimationPercent` | 0.3 | `COUISearchViewAnimate.CLAMP_ANIMATION_PERCENT` |
| `MoveEase` | `cubic-bezier(0.3, 0, 0.1, 1)` | `COUIMoveEaseInterpolator` |
| `Ease` | `cubic-bezier(0.33, 0, 0.67, 1)` | `COUIEaseInterpolator` |

布局中复原的间距：`coui_search_view_background_start_gap`（40dp）与 `coui_search_view_background_end_gap`（4dp）决定非即时布局中输入框的首尾留白。

## 未移植

- **整条视图的位移与输入框尾部外边距**——`initSmoothEnterAnimatorSet` / `initSmoothExitAnimatorSet` 让视图相对工具栏移动，并动画 SearchView 的 `marginEnd`；两者都依赖宿主的工具栏几何，留在调用方。本移植只保留它们的 450ms / `COUIMoveEaseInterpolator` 常量（`REVEAL_DURATION_MILLIS`）。
- **分裂背景**——非即时布局展开后，`mDivideBackground` 会把填充改画成在分割线处相接的两个半胶囊。与整块胶囊的差别只有那 1dp 缝隙，本移植保持一整块。
- **`COUIHintAnimationLayout`**——用于轮换多条提示文案的布局。它是独立的类，不属于本次移植范围；这里把提示文案当作普通占位文本处理。
- **工具栏吸附偏移**——原生会依据工具栏高度与 insets 对搜索栏做偏移，这部分属于宿主侧计算，在本移植中交给调用方。
- **`SearchViewAnimateDefaults.CornerRadius`**——暴露了原生的 18dp 值供调用方使用，但搜索栏自身用 `CircleShape` 裁剪，因为复原出的高度（40dp）对应的胶囊半径为 20dp，而原生半径会被约束到 `height / 2`。
