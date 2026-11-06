# SearchBar

`SearchBar` 是 COUI 中用于用户输入搜索内容的组件。它提供了一个直观且易用的搜索界面，支持展开/收起状态切换以及搜索建议展示。

它有两种形态。低层形态把输入框作为 `inputField` 插槽传入，并可选 `outsideEndAction`，适合自己拼装输入框的调用方。另一种形态直接接收 `query` / `onQueryChange` / `cancelText`，自己构建输入框与编辑态取消胶囊，两者会像 `COUISearchBar` 那样融合成一个轮廓 —— 见[取消胶囊与融合球](#取消胶囊与融合球)。

<div style="position: relative; height: 250px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../../compose/index.html?id=searchBar" title="Demo" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin"></iframe>
</div>

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.SearchBar
import io.github.suqi8.coui.kmp.basic.InputField
```

## 基本用法

```kotlin
var searchText by remember { mutableStateOf("") }
var expanded by remember { mutableStateOf(false) }

SearchBar(
    inputField = {
        InputField(
            query = searchText,
            onQueryChange = { searchText = it },
            onSearch = { /* 处理搜索操作 */ },
            expanded = expanded,
            onExpandedChange = { expanded = it }
        )
    },
    expanded = expanded,
    onExpandedChange = { expanded = it }
) {
    // 搜索结果内容
    Column {
        // 在这里添加搜索建议或结果
    }
}
```

## 属性

### SearchBar 属性

| 属性名               | 类型                                 | 说明            | 默认值                 | 是否必须 |
|-------------------|------------------------------------|---------------|---------------------|------|
| inputField        | @Composable () -> Unit             | 搜索输入框组件       | -                   | 是    |
| onExpandedChange  | (Boolean) -> Unit                  | 展开状态变化的回调     | -                   | 是    |
| modifier          | Modifier                           | 应用于搜索栏的修饰符    | Modifier            | 否    |
| insideMargin      | DpSize                             | 内部边距          | SearchBarDefaults.InsideMargin | 否    |
| expanded          | Boolean                            | 是否展开显示搜索结果    | false               | 否    |
| outsideEndAction  | @Composable (() -> Unit)?          | 展开时显示在右侧的操作组件 | null                | 否    |
| content           | @Composable ColumnScope.() -> Unit | 展开时显示的内容      | -                   | 是    |

### InputField 属性

| 属性名               | 类型                        | 说明            | 默认值      | 是否必须 |
|-------------------|---------------------------|---------------|----------|------|
| query             | String                    | 搜索框中的文本内容     | -        | 是    |
| onQueryChange     | (String) -> Unit          | 文本内容变化时的回调    | -        | 是    |
| onSearch          | (String) -> Unit          | 执行搜索操作时的回调    | -        | 是    |
| expanded          | Boolean                   | 是否处于展开状态      | -        | 是    |
| onExpandedChange  | (Boolean) -> Unit         | 展开状态变化的回调     | -        | 是    |
| modifier          | Modifier                  | 应用于输入框的修饰符    | Modifier | 否    |
| label             | String                    | 搜索框为空时显示的提示文本 | ""       | 否    |
| enabled           | Boolean                   | 是否启用搜索框       | true     | 否    |
| textStyle         | TextStyle?                | 搜索框中文本的样式     | null     | 否    |
| colors            | SearchBarColors           | 输入框的颜色配置      | SearchBarDefaults.searchBarColors() | 否    |
| stroke            | MaterialStrokeParams?     | 胶囊上的材质描边与内阴影  | SearchBarDefaults.searchBarStroke() | 否    |
| glowColor         | Color?                    | 胶囊下方的焦散光，只画在胶囊外侧。默认不画：系统把它挂在两台参考机都关着的材质特性上，需要时传 `SearchBarDefaults.causticGlowColor()` | null | 否    |
| leadingIcon       | @Composable (() -> Unit)? | 前置图标          | 默认放大镜    | 否    |
| trailingIcon      | @Composable (() -> Unit)? | 后置图标          | 默认清除按钮   | 否    |
| interactionSource | MutableInteractionSource? | 交互源           | null     | 否    |
| clearContentDescription | String              | 默认清除按钮的无障碍文案（系统取 `R.string.coui_search_clear_button_description`） | "Search Cleanup" | 否 |

### SearchBarDefaults 对象

SearchBarDefaults 对象提供了 SearchBar 和 InputField 组件的默认值。

#### 常量

| 常量名                      | 类型     | 说明                             | 默认值              |
| --------------------------- | -------- | -------------------------------- | ------------------- |
| InsideMargin                | DpSize   | SearchBar 的内部边距             | DpSize(16.dp, 6.dp) |
| NATIVE_BLUR_SIGMA_PX        | Float    | 模糊预设（`FRAMEWORK_TOP_BAR_BLUR_PARAMS`）的模糊 sigma | 20f |
| NATIVE_TOP_BAR_BLUR_SIGMA_PX | Float   | 默认预设（`FRAMEWORK_TOP_BAR_PARAMS`）的模糊 sigma：不模糊，胶囊只叠两层色 | 0f |
| InputFieldMinHeight         | Dp       | InputField 背景的最小高度        | 40.dp               |
| InputFieldFontSize          | TextUnit | InputField 文本与标签字号        | 15.sp               |
| LeadingIconStartPadding     | Dp       | 默认前置图标的起始内边距         | 12.dp               |
| LeadingIconEndPadding       | Dp       | 默认前置图标的结束内边距         | 8.dp                |
| TrailingIconStartPadding    | Dp       | 默认清除按钮的起始内边距         | 4.dp                |
| TrailingIconEndPadding      | Dp       | 默认清除按钮的结束内边距         | 4.dp                |
| TrailingIconTouchTargetSize | Dp       | 默认清除按钮的圆形触摸目标尺寸   | 36.dp               |
| TrailingIconVisualSize      | Dp       | 默认清除按钮的可见圆形尺寸       | 18.dp               |
| MetaBallSpringResponse      | Float    | 编辑态弹簧响应，`StateChangedToEditParamsHigh.getResponse()` | 0.5f |
| MetaBallSpringBounce        | Float    | 编辑态弹簧回弹，`COUISpringForce.setBounce` | 0.25f |
| CancelInitialScale          | Float    | 取消胶囊的起始缩放，`getFunctionalButtonInitScale()` | 0.5f |
| CancelSlidePx               | Float    | `mFunctionalButtonOffsetX`：按钮起始位置比静止位置多出 20 个原始像素 | 20f |

#### 方法

| 方法名            | 类型            | 说明                     |
| ----------------- | --------------- | ------------------------ |
| searchBarColors() | SearchBarColors | 创建输入框的默认颜色配置 |
| searchBarStroke() | MaterialStrokeParams | 解析胶囊描边与内阴影参数 |
| causticGlowColor() | Color | 解析胶囊下方的光：浅色 `#2EFFFFFF`，深色 `#33FFFFFF`。需要自行选用：`isCausticShadowEnable()` 依赖默认关闭的 `mSystemCausticShadowState` |
| metaBallFusion() | Boolean | 取消胶囊是否可与输入框融合：即 `setBallChain` 的材质条件，在本库对应 `isBlurEnabled()` |

### SearchBarActionButton

`COUISearchBar` 的功能按钮——展开态输入框右侧那颗 40dp 胶囊，承载「取消」文案。

| 属性名          | 类型                        | 说明                    | 默认值          | 是否必须 |
|-----------------|-----------------------------|-------------------------|-----------------|----------|
| text            | String                      | 按钮文案                | -               | 是       |
| onClick         | () -> Unit                  | 点击回调                | -               | 是       |
| modifier        | Modifier                    | 应用于按钮的修饰符      | Modifier        | 否       |
| enabled         | Boolean                     | 是否响应输入            | true            | 否       |
| interactionSource | MutableInteractionSource? | 可外部托管的交互源      | null            | 否       |
| containerColor  | Color                       | 胶囊填充色              | 搜索框同一材质色 | 否       |
| contentColor    | Color                       | 文案颜色                | onSurface 90%   | 否       |
| glowColor       | Color?                      | 胶囊下方的焦散光，只画在胶囊外侧；与输入框一样默认关闭 | null | 否 |

`SearchBarActionButtonDefaults` 保存原生几何：`Height`（40dp，
`coui_search_bar_normal_background_height`）、`MaxWidth`（152dp，
`coui_search_bar_functional_button_max_width`：`measureFunctionalButton` 按该宽度 AT_MOST 测量，
超长文案改为省略号）、`HorizontalPadding`（16dp，
`coui_search_bar_functional_button_text_padding_horizontal`）、`StartGap`（12dp，
`coui_search_bar_functional_button_start_gap`）。文案为 14sp Medium（`couiTextAppearanceButton`）。

### SearchBarColors 类

| 属性名          | 类型  | 说明                                                              |
| --------------- | ----- | ----------------------------------------------------------------- |
| backgroundColor | Color | 胶囊填充色（工具栏材质层：深色 80% #262626，浅色 60% 白）          |
| labelColor      | Color | 提示（hint）文字颜色（onSurfaceSecondary，54% 黑 / 54% 白）       |
| iconColor       | Color | 默认前置放大镜图标着色（onSurfaceSecondary）                      |
| clearIconColor  | Color | 默认清除按钮圆形填充色（onSurfaceContainerHigh）                  |

## 取消胶囊与融合球

`COUISearchBar.changeStateWithAnimation` 会在开始编辑态动画前调用 `setBallChain()`：输入框与功能按钮进入同一条融合球链（输入框用 `OplusMaterialMetaBallParams(1, 0, 4dp, 1.5f, 2.0f)`，按钮用索引 2），因此「取消」胶囊是从输入框尾端「长出来」的，而不是在旁边滑入。自己持有输入框的那个重载实现了这一点。

```kotlin
var query by remember { mutableStateOf("") }
var expanded by remember { mutableStateOf(false) }

SearchBar(
    query = query,
    onQueryChange = { query = it },
    onSearch = { expanded = false },
    onExpandedChange = { expanded = it },
    expanded = expanded,
    label = "搜索设置项",
    cancelText = "取消",
    onCancel = { expanded = false },
) {
    // 搜索结果内容
}
```

融合链只在材质开启时建立 —— `setBallChain` 的条件是 `mUseMaterialEffectAttr != 0 && isSupportBlur() && isBlurEnable()` —— 所以 `metaBallFusion` 默认取 `isBlurEnabled()`。模糊关闭时回落到低层形态：两个独立胶囊，走 `outsideEndAction` 的进出动画。

过渡参数按系统原样：`measureBackground` 把输入框缩到 `宽度 - 按钮宽 - 12dp`，`doWidthChangedAnimate` 再随进度插值回全宽；按钮从 0.5 缩放，`translationX` 从 `mFunctionalButtonOffsetX` 回到 0；三条轨道共用一条弹簧，响应 0.5s、回弹 0.25（`StateChangedToEditParamsHigh` 与 `StateChangedToNormalParamsHigh` 是同一对值）。静止时两个胶囊正好相隔一个 `coui_search_bar_functional_button_start_gap`（12dp），是 6dp 融合半径的两倍，所以只是相切而不搭桥；桥只出现在按钮还在往里走的阶段。

融合后的轮廓用**视图级**描边，而不是融合球自己的材质边：`COUISearchBar` 给字段和按钮设的都是 `COUIMaterialStrokeEffect` 的 `TYPE_FRAMEWORK_CAPSULE_8`（亮色 alpha 0.8、深色 0.15，1.8dp），融合链只负责把两个轮廓并成一个。两者在深色材质上差别很大 —— 融合球的 `blendEdge` 把描边映射成 `2 × improvedColor`，深色填充下只能到填充亮度的两倍；视图描边直接合成线色本身。设备实测：视图描边在 39 的填充上画出 78 的顶部亮线，融合球混合同条件下只有 47，边缘读起来像被压暗的一圈。融合路径因此走 `plainRim`（`Modifier.nativeMaterialEdge` 的内部参数），底栏仍用融合球混合。

胶囊填充取材质本体：`FRAMEWORK_TOP_BAR_PARAMS_DARK` 的第一层 `#CC262626`（第二层 `#B37F7F7F` + `OVERLAY` 在中性色上不改变结果），亮色取单层 60% 白，复现该预设 `COLOR_DODGE` + `LUMINOSITY` 两层的合成结果（页面 235 上得 247，40% 白会低 5 级）。深色下它比页面略暗（`#2E2E2E` 页面上得到 39，与系统实测一致），而不是 `coui_search_bar_no_blur_solid` 的 `#28767676` 灰。

焦散光随编辑进度淡入（`updateDropShadow(progress)`，`mNoShadowInNormalState` 让静止胶囊完全不发光），所以收起态的融合胶囊不投影。

| 属性名          | 类型                        | 说明                                                | 默认值                               | 必填 |
|-----------------|-----------------------------|-----------------------------------------------------|--------------------------------------|------|
| query           | String                      | 搜索框内的文本                                      | -                                    | 是   |
| onQueryChange   | (String) -> Unit            | 文本变化回调                                        | -                                    | 是   |
| onSearch        | (String) -> Unit            | 键盘搜索键回调                                      | -                                    | 是   |
| onExpandedChange | (Boolean) -> Unit          | 展开状态变化回调                                    | -                                    | 是   |
| modifier        | Modifier                    | 应用到搜索栏的 Modifier                             | Modifier                             | 否   |
| expanded        | Boolean                     | 是否展开                                            | false                                | 否   |
| label           | String                      | 为空时的占位文本                                    | ""                                   | 否   |
| enabled         | Boolean                     | 输入框是否可用                                      | true                                 | 否   |
| textStyle       | TextStyle?                  | 输入文字样式                                        | null                                 | 否   |
| colors          | SearchBarColors             | 输入框与取消胶囊的颜色                              | SearchBarDefaults.searchBarColors()  | 否   |
| stroke          | MaterialStrokeParams?       | 材质描边；融合时沿合并后的轮廓按该预设自身的 1.8dp 宽度绘制          | SearchBarDefaults.searchBarStroke()  | 否   |
| leadingIcon     | @Composable (() -> Unit)?   | 前置图标                                            | 默认放大镜                           | 否   |
| trailingIcon    | @Composable (() -> Unit)?   | 后置图标                                            | 默认清除按钮                         | 否   |
| interactionSource | MutableInteractionSource? | 交互源                                              | null                                 | 否   |
| glowColor       | Color?                      | 胶囊下方的光，裁在轮廓之外；默认关闭（参考机的材质特性为关） | null | 否   |
| backdrop        | Backdrop?                   | 胶囊下方取样的背景                                  | null                                 | 否   |
| insideMargin    | DpSize                      | 搜索栏内容四周的边距                                | SearchBarDefaults.InsideMargin       | 否   |
| cancelText      | String?                     | 取消胶囊文字，`null` 表示没有取消胶囊               | null                                 | 否   |
| onCancel        | () -> Unit                  | 点击取消胶囊的回调                                  | 收起搜索栏                           | 否   |
| metaBallFusion  | Boolean                     | 两个胶囊是否允许融合                                | SearchBarDefaults.metaBallFusion()   | 否   |
| clearContentDescription | String              | 默认清除按钮的无障碍文案                            | "Search Cleanup"                     | 否   |
| content         | @Composable ColumnScope.() -> Unit | 展开时显示的内容                              | {}                                   | 否   |

## 进阶用法

### 带图标的搜索栏

```kotlin
var searchText by remember { mutableStateOf("") }
var expanded by remember { mutableStateOf(false) }

SearchBar(
    inputField = {
        InputField(
            query = searchText,
            onQueryChange = { searchText = it },
            onSearch = { /* 处理搜索操作 */ },
            expanded = expanded,
            onExpandedChange = { expanded = it },
            leadingIcon = {
                Icon(
                    modifier = Modifier.padding(start = 12.dp, end = 8.dp),
                    imageVector = COUIIcons.Basic.Search,
                    contentDescription = "搜索"
                )
            }
        )
    },
    expanded = expanded,
    onExpandedChange = { expanded = it }
) {
    // 搜索结果内容
}
```

### 带搜索建议的搜索栏

```kotlin
var searchText by remember { mutableStateOf("") }
var expanded by remember { mutableStateOf(false) }
val suggestions = listOf("建议 1", "建议 2", "建议 3")

SearchBar(
    inputField = {
        InputField(
            query = searchText,
            onQueryChange = { searchText = it },
            onSearch = { /* 处理搜索操作 */ },
            expanded = expanded,
            onExpandedChange = { expanded = it }
        )
    },
    expanded = expanded,
    onExpandedChange = { expanded = it }
) {
    Column {
        suggestions.forEach { suggestion ->
            BasicComponent(
                title = suggestion,
                onClick = {
                    searchText = suggestion
                    expanded = false
                }
            )
        }
    }
}
```

### 带取消按钮的搜索栏

```kotlin
var searchText by remember { mutableStateOf("") }
var expanded by remember { mutableStateOf(false) }

SearchBar(
    modifier = Modifier.padding(horizontal = 12.dp),
    inputField = {
        InputField(
            query = searchText,
            onQueryChange = { searchText = it },
            onSearch = { expanded = false },
            expanded = expanded,
            onExpandedChange = { expanded = it },
            label = "搜索"
        )
    },
    expanded = expanded,
    onExpandedChange = { expanded = it },
    outsideEndAction = {
        // SearchBar 已按 COUI 规格提供该插槽两侧的间距
        Text(
            modifier = Modifier
                .clickable(
                    interactionSource = null,
                    indication = null
                ) {
                    expanded = false
                    searchText = ""
                },
            text = "取消",
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
            color = COUITheme.colorScheme.primary
        )
    }
) {
    // 搜索结果内容
}
```