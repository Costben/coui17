# SearchViewAnimate

`SearchViewAnimate` 是 ColorOS 的「磁吸式」搜索栏。它在两种状态之间切换：收起时只显示一个搜索图标，展开时输入框和尾部取消按钮滑入。搜索栏的高度、垂直位移、内容透明度和背景色全部由宿主传入的一个折叠进度值推导出来，这正是它看起来「吸附」在滚动内容上的原因。

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
| 内容透明度 | `(f - 0.5) * 2` |
| 背景色 | `ArgbEvaluator(clamp(f / 0.3, 0, 1))`，从 `couiColorDivider` 过渡到 `coui_search_view_selector_color_normal` |

## 参数

| 参数 | 类型 | 说明 |
| :--- | :--- | :--- |
| `query` | `String` | 输入框中的文本 |
| `onQueryChange` | `(String) -> Unit` | 文本变化时回调 |
| `expanded` | `Boolean` | 是否处于 `SearchViewState.Edit` |
| `onExpandedChange` | `(Boolean) -> Unit` | 搜索栏请求进入或退出编辑态时回调 |
| `modifier` | `Modifier` | 应用于搜索栏的修饰符 |
| `hint` | `String` | `query` 为空时显示的占位文本 |
| `type` | `SearchViewAnimateType` | `InstantSearch` 或 `NonInstantSearch` |
| `collapsePercent` | `Float` | 折叠进度，0..1 |
| `onSearch` | `(String) -> Unit` | 输入法搜索键触发时回调当前文本 |
| `cancelText` | `String` | 尾部按钮文案（`string/coui_search_view_cancel`） |
| `cancelButton` | `(@Composable () -> Unit)?` | 完全替换尾部按钮 |
| `leadingIcon` | `ImageVector` | 收起时显示的图标 |
| `colors` | `SearchViewAnimateColors` | 搜索栏配色 |

## 动画

进入与退出动画由 `initSmoothEnterAnimatorSet` / `initSmoothExitAnimatorSet` 构成，全部使用 `COUIMoveEaseInterpolator`（`pathInterpolator(0.3, 0, 0.1, 1)`）：

| 轨道 | 时长 | 常量 |
| :--- | :--- | :--- |
| 位移 | 450ms | `DEFAULT_SEARCH_VIEW_OFFSET_CHANGE_DURATION` |
| 缩放 | 450ms | `DEFAULT_SEARCH_VIEW_SCALE_CHANGE_DURATION` |
| 按钮透明度（即时搜索） | 350ms | `DEFAULT_BUTTON_ALPHA_CHANGE_DURATION` |
| 按钮透明度（非即时搜索） | 100ms | `NON_INSTANT_SEARCH_BUTTON_ALPHA_CHANGE_DURATION` |

按压反馈（`playPressAnimation` / `playReleaseAnimation`）使用 150ms 的 `COUIEaseInterpolator`（`pathInterpolator(0.33, 0, 0.67, 1)`）在常态与按压态选择器之间过渡。

## 复原的令牌

以下数值均对标 ColorOS 17 实现，没有一个是臆造的。

| 令牌 | 值 | 来源资源 |
| :--- | :--- | :--- |
| `WrapperHeight` | 40dp | `dimen/coui_search_view_wrapper_height` |
| `AnimateHeight` | 50dp | `dimen/coui_search_view_animate_height` |
| `CollapsedMinHeight` | 1dp | `dimen/coui_search_view_collapsed_min_height`（声明为 `1px`） |
| `IconSize` | 24dp | `dimen/coui_search_view_icon_size` |
| `SearchIconMargin` | 12dp | `dimen/coui_search_view_search_icon_margin` |
| `CornerRadius` | 18dp | `dimen/coui_search_view_corner` |
| `InputTextSize` | 15sp | `dimen/coui_search_view_input_text_size` |
| `CancelTextSize` | 14sp | `dimen/coui_search_view_cancel_text_size` |
| `CancelButtonBackgroundRadius` | 24dp | `dimen/coui_searchview_cancel_button_bg_radius` |
| `CancelButtonMinHeight` | 36dp | `dimen/coui_search_bar_inner_search_icon_size` |
| `CancelButtonPaddingHorizontal` | 8dp | `dimen/coui_search_cancel_button_bg_padding_horizontal` |
| `CancelButtonPaddingVertical` | 4dp | `dimen/coui_search_cancel_button_bg_padding_vertical` |
| `CancelMarginSmall` | 4dp | `dimen/coui_search_view_cancel_margin_small` |
| `CancelMarginLarge` | 8dp | `dimen/coui_search_view_cancel_margin_large` |
| `ButtonDividerMarginStart` | 0dp | `dimen/coui_search_view_button_divider_margin_start` |
| `DividerWidth` | 1dp | `dimen/coui_search_linear_divider_width` |
| `DividerHeight` | 16dp | `drawable/search_linear_divider` 的 `android:height` |
| 背景常态 | `#14000000` 浅色 / `#1affffff` 深色 | `color/coui_search_view_selector_color_normal` |
| 背景按压 | `#1e000000` 浅色 / `#33ffffff` 深色 | `color/coui_search_view_selector_color_pressed` |
| 占位文本 | `#23000000` 浅色 / `#33ffffff` 深色 | `color/coui_search_view_hint_color` |
| 输入文本 | `#ff000000` 浅色 / `#ffffffff` 深色 | `color/coui_search_view_input_text_color` |
| 取消文案 | `#ff000000` 浅色 / `#ffffffff` 深色 | `color/coui_searchview_cancel_text_color` |
| 分割线 | `#1a000000` 浅色 / `#33ffffff` 深色 | `color/coui_search_view_linear_divider_color` |
| 取消按钮文案 | `"Cancel"` | `string/coui_search_view_cancel` |
| `ClampAnimationPercent` | 0.3 | `COUISearchViewAnimate.CLAMP_ANIMATION_PERCENT` |
| `MoveEase` | `cubic-bezier(0.3, 0, 0.1, 1)` | `COUIMoveEaseInterpolator` |
| `Ease` | `cubic-bezier(0.33, 0, 0.67, 1)` | `COUIEaseInterpolator` |

布局中复原的间距：`coui_search_view_background_start_gap`（40dp）与 `coui_search_view_background_end_gap`（4dp）决定非即时布局中输入框的首尾留白，`coui_search_view_auto_complete_padding_end`（16dp）是输入框的尾部内边距。

## 未移植

- **动画辅助类**——其精确调度未知，本移植重现了进入/退出动画组组合出的轨道与时长，但不包含该辅助类可能追加的中间关键帧。
- **`COUIHintAnimationLayout`**——用于在收起与展开之间旋转提示文案的布局。它是独立的类，不属于本次移植范围；这里把提示文案当作普通占位文本处理。
- **工具栏吸附偏移**——原生会依据工具栏高度与 insets 对搜索栏做偏移，这部分属于宿主侧计算，在本移植中交给调用方。
- **`SearchViewAnimateDefaults.CornerRadius`**——暴露了原生的 18dp 值供调用方使用，但搜索栏自身用 `CircleShape` 裁剪，因为复原出的高度（40dp）对应的胶囊半径为 20dp，而原生半径会被约束到 `height / 2`。
