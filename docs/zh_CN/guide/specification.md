# 实现规范

本规范定义使用 COUI 构建 ColorOS 17 风格界面时的组件选型、密度作用域、玻璃材质接线、浮层宿主、表面色彩层级、交互细节及工程校验标准。所有界面实现均以 ColorOS 17 系统应用为基准（参照设备：OnePlus PLK110，ColorOS V17.0.0，1272×2772）。

---

## 一、系统界面对标与组件选型

构建界面时遵循以下标准流程：

1. **确定对标系统界面**：先明确目标界面在 ColorOS 17 系统应用中的对应形态（主 Tab 页对标系统「相册 · 照片」，二级配置页对标系统「设置」，确认弹窗对标系统确认对话框）。
2. **拆解系统控件映射**：通过 `uiautomator dump` 核对系统节点树的 `resource-id` 与控件层级，按下表映射为 COUI 组件。
3. **划分密度作用域**：按区域确定采用应用密度、相册密度或面板物理密度（见第二节）。
4. **接入玻璃材质**：凡对标界面含实时模糊材质的区域，完整接入录制、提供、采样三要素（见第三节）。
5. **同屏剖面校验**：在参照设备上使用统一采样脚本对比系统界面与实现界面的几何与光学剖面（见第八节）。

### 系统控件与库组件标准映射表

| 系统界面元素 | 系统节点标识 / 类型 | COUI 标准组件组合 | 适用密度 |
| :--- | :--- | :--- | :--- |
| 主 Tab 页标题栏与操作按钮 | `toolbar`、`action_main_tab_search` | `PinnedTopAppBar` 或 `TopAppBar(gradientGlass = true)` + `HighlightedIconButton` + `Modifier.couiTopAppBarActionItem()` | 相册密度 |
| 二级设置页返回键与更多键 | `coui_toolbar_back_view`、`coui_toolbar_more_view` | `TopAppBar` / `SmallTopAppBar` + `HighlightedIconButton` + `Modifier.couiTopAppBarActionItem()` | 应用密度 |
| 顶栏三点弹出菜单 | `coui_popup_wrapper`、`coui_popup_list_view` | `OverlayListPopup` + `ListPopupColumn` + `DropdownImpl(role = Role.Button)` | 面板物理密度（组件内部自动切换） |
| 底部悬浮导航胶囊 | `TYPE_FRAMEWORK_BOTTOM_BAR` | `BottomFloatingNavigationBar` | 相册密度 |
| 带两侧独立圆钮的底部悬浮栏 | `TYPE_FRAMEWORK_BOTTOM_BAR`（配件形态） | `BottomFloatingAccessoryNavigationBar` | 相册密度 |
| 确认与操作对话框 | 系统确认对话框 | `WindowAlertDialog` | 应用密度 |
| 自定义内容模态弹窗 | 系统模态面板 | `WindowDialog`（或需要玻璃材质时用 `OverlayDialog` / `OverlayBottomSheet`） | 应用密度 |
| 设置分组卡片与设置行 | 系统设置列表项 | `Card` + `coui-preference` 系列组件 | 应用密度 |
| 胶囊分段控件 | 系统分段切换条 | `TabRowWithContour` | 应用密度 |
| 搜索输入框 | 系统搜索胶囊 | `InputField` | 应用密度 |

---

## 二、密度体系规范

ColorOS 17 的同一屏幕中同时存在三种密度基准。不同区域必须绑定对应的密度作用域：

| 密度类型 | 参照机（PLK110）基准值 | 适用范围 | 接入方式 |
| :--- | :--- | :--- | :--- |
| **应用密度** | `620dpi = 3.875 px/dp`（随系统「显示大小」缩放） | 应用窗口默认布局、二级设置页、正文列表、常规弹窗 | Compose 默认 `LocalDensity`，无需额外包裹 |
| **相册密度** | `565dpi = 3.53125 px/dp`（短边固定为 `360dp`） | 主 Tab 页顶栏操作按钮、底部悬浮导航栏及其两侧配件圆钮 | 通过 [`rememberGalleryDensity()`](../components/pinnedtopappbar#相册密度) 显式提供 `LocalDensity` |
| **面板物理密度** | `560dpi = 3.5 px/dp`（面板物理 `ro.sf.lcd_density`） | 系统级弹出菜单（`OverlayListPopup`、`WindowListPopup`、级联菜单） | 组件内部通过 `popupPhysicalDensity` 自动从基准密度换算至物理密度，调用方无需手动切换 |

### 密度作用域约束

1. **主 Tab 页顶栏与底栏固定尺寸元素必须使用相册密度**：以 `40dp` 的顶栏图标按钮为例，在相册密度下渲染为 `141px`（与系统相册一致），在应用密度下则为 `155px`；`56dp` 的底部悬浮胶囊在应用密度下会偏大再约 10%。
2. **相册密度仅包裹固定尺寸子树**：外层布局定位修饰符（如 `align`、`statusBarsPadding`、`navigationBarsPadding`、外边距）应保留在应用密度下解析，避免因密度缩放引起控件整体偏移。
3. **主 Tab 页顶栏的两种标准密度作用域策略**：
   - **整条顶栏置入相册密度**：按钮尺寸、锚点位置与大标题字号均完全对齐系统相册（大标题字号较跟随显示大小时小约 9%），适用于严格 1:1 还原系统主 Tab 页的场景。
   - **仅将 `HighlightedIconButton` 置入相册密度**：按钮本体保持 `141px` 标准尺寸，标题字号与顶栏行高继续跟随系统「显示大小」缩放。
4. **弹出菜单不受外层相册密度影响**：将 `OverlayListPopup` 置于相册密度作用域内是安全的，组件内部始终按物理面板密度独立换算。
5. **像素与 dp 换算基准**：度量相册密度区域时按 `3.53125 px/dp` 换算，度量弹出菜单时按 `3.5 px/dp` 换算，度量常规应用区域时按 `3.875 px/dp` 换算。

---

## 三、玻璃材质与 Backdrop 接线规范

实时模糊与玻璃高光材质由三部分协同工作，缺一不可：

1. **创建背景图层**：调用 `rememberLayerBackdrop` 创建图层，并在绘制闭包中**先填充页面底色 `drawRect(pageColor)`，再调用 `drawContent()`**。这样即使列表为空或处于深色模式下，采样方也能读到真实的页面底色而非透明通道。
2. **标记采样源节点**：在被采样的内容容器上挂载 `Modifier.layerBackdrop(backdrop)`。
3. **注入上下文**：通过 `CompositionLocalProvider(LocalBackdrop provides backdrop)` 将图层提供给顶栏、底栏及弹出菜单。

### Backdrop 拓扑与隔离规则

- **单源录制约束**：每一个 `LayerBackdrop` 实例只能挂载到**唯一一个** `Modifier.layerBackdrop` 节点上。主框架底栏的 `barBackdrop` 应直接挂在整个 `HorizontalPager` 上，严禁在多个分页上重复挂载同一个 `LayerBackdrop`。
- **录制层与采样方严格分离**：采样组件（如 `TopAppBar`、`BottomFloatingNavigationBar`）必须处于其所采样的 `Modifier.layerBackdrop` 节点**之外**（作为兄弟节点叠放）。若采样组件位于自身的录制层内部，会采样到自身的上一帧画面并产生帧间闪烁。
- **主框架底栏与单页顶栏双 Backdrop 分离**：
  - `barBackdrop` 挂载在 `HorizontalPager` 上，供外层 `Scaffold` 的底部悬浮导航栏采样；
  - 每个 Tab 页内部单独创建 `topBarBackdrop`，挂载在该页的正文容器上，仅向该页的 `TopAppBar` 提供。
- **弹出菜单的 Backdrop 继承机制**：`OverlayListPopup` 在根 `Scaffold` 的浮层宿主中渲染时，会自动重新注入**调用处**的 `LocalBackdrop`。因此当菜单声明在顶栏 `actions` 内时，只要顶栏上下文中提供了采样页面正文的 `topBarBackdrop`，弹出的菜单面板即可实时透出背后的页面内容。
- **模糊半径换算公式**：系统原生模糊半径为像素级高斯 `sigma`，换算为 Compose `Dp` 的公式为 `sigmaPx / 0.45 / density`（不可直接将像素值写作 `dp`）。

---

## 四、浮层与弹窗宿主规范

### 1. `Overlay*` 系列（宿主内浮层，支持实时玻璃）

- **宿主依赖**：`OverlayListPopup`、`OverlayDialog`、`OverlayBottomSheet` 均在根 `Scaffold` 内部的 `COUIPopupHost` 中组合渲染。所有使用 `Overlay*` 的页面必须位于 COUI `Scaffold` 子树内。
- **独立路由页面的宿主包裹**：对于全屏图片查看器、WebView 页面或自定义外壳页面等不自带常规顶底栏的独立路由，必须在导航宿主入口处包裹一层透明 `Scaffold` 以提供浮层宿主：

```kotlin
entry<Route.ImageViewer> { route ->
    Scaffold(containerColor = Color.Transparent) {
        ImageViewer(route)
    }
}
```

- **保留默认 `popupHost`**：严禁向 `Scaffold` 传入空的 `popupHost = { }`，否则该子树内所有 `Overlay*` 浮层均不会挂载渲染。

### 2. `Window*` 系列（独立窗口对话框）

- **标准确认框**：使用 `WindowAlertDialog(title, summary, confirmText, cancelText)`，自带标准面板圆角、按钮横竖自适应排列、窗口遮罩（scrim）与进出场动效。
- **自定义内容模态框**：使用 `WindowDialog`，面板底色自动匹配主题背景色。不要使用裸 `Dialog` 搭配 `Card`（深色模式下 `surfaceContainer` 仅为 10% 白色半透明，直接作为无遮罩窗口底板会导致透底）。
- **材质边界**：`Window*` 运行在独立的平台 `Dialog` 窗口中，不共享宿主窗口图层坐标，因此不启用 `LocalBackdrop` 玻璃采样；凡需玻璃材质的菜单或面板均应选用 `Overlay*`。

---

## 五、页面色彩与表面层级规范

ColorOS 17 页面严格遵循**两层表面体系**：页面背景层（`background`）与卡片容器层（`surfaceContainer`）。

| 层级 | 颜色 Token | 浅色模式取值 | 深色模式取值 | 适用组件与区域 |
| :--- | :--- | :--- | :--- | :--- |
| **第一层：页面背景** | `COUITheme.colorScheme.background` | 页面浅灰 | 深灰（`#1E1E1E` 级） | `Scaffold(containerColor)`、`TopAppBar(color)`、`SmallTopAppBar(color)`、`layerBackdrop` 铺底色 |
| **第二层：卡片容器** | `COUITheme.colorScheme.surfaceContainer` | 纯白 `#FFFFFF` | 10% 白色叠加于页面背景 | 页面上的 `Card`、列表分组容器 |
| **卡片内嵌套块** | 浅色取 `background`，深色取 `surfaceContainer` | 页面浅灰（与纯白外卡形成区分） | 10% 白色二次叠加（在深灰外卡上进一步提亮） | 卡片内部的引用块、转发原文区、内嵌代码框 |

### 色彩使用准则

1. **显式指定 `Scaffold` 与 `TopAppBar` 背景色**：`Scaffold` 的 `containerColor` 与 `TopAppBar` / `SmallTopAppBar` 的 `color` 默认值为 `surface`（深色模式下为纯黑 `#111111`）。构建标准页面时应统一显式传入 `COUITheme.colorScheme.background`。
2. **避免使用 `surface` 绘制内嵌胶囊或轨道**：`surface` 在常规主题下与页面背景同色，用其绘制滑块轨道或胶囊底板会导致控件与背景融为一体。
3. **同步原生窗口底色**：Android 工程的 `themes.xml` 与 `values-night/themes.xml` 中的 `android:windowBackground` 必须与 `COUITheme.colorScheme.background` 保持一致，避免 Activity 或窗口转场瞬间出现白闪或黑闪。

---

## 六、典型界面标准实现

### 6.1 主 Tab 页外壳与底部悬浮导航栏（对标系统「相册 · 照片」）

主框架由一个外层 `Scaffold` 承载 `HorizontalPager`。底部悬浮导航栏浮于 `HorizontalPager` 之上并采样整个 Pager；每个 Tab 页面内部自带独立顶栏与单页 Backdrop。

```kotlin
@Composable
fun MainShell(pages: List<Tab>) {
    val pageColor = COUITheme.colorScheme.background
    val barBackdrop = rememberLayerBackdrop {
        drawRect(pageColor)
        drawContent()
    }
    val pagerState = rememberPagerState { pages.size }

    Scaffold(
        containerColor = pageColor,
        bottomBar = {
            CompositionLocalProvider(
                LocalBackdrop provides barBackdrop,
                LocalDensity provides rememberGalleryDensity(),
            ) {
                Box(Modifier.fillMaxWidth().navigationBarsPadding()) {
                    BottomFloatingNavigationBar {
                        pages.forEachIndexed { index, tab ->
                            BottomFloatingNavigationBarItem(
                                selected = pagerState.currentPage == index,
                                onClick = { /* 切换分页 */ },
                                icon = tab.icon,
                                selectedIcon = tab.selectedIcon,
                                label = tab.label,
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().layerBackdrop(barBackdrop),
        ) { page ->
            TabPage(pages[page], outerPadding = padding)
        }
    }
}
```

**实现要求**：
- **内容穿透底栏**：正文列表需将 `outerPadding.calculateBottomPadding()` 及安全余量作为列表自身的 `contentPadding` 底部留白，使列表滚动时可从悬浮胶囊下方穿过，同时最后一项能完整滚出胶囊上方。
- **惰性读取 `Scaffold` 内边距**：在高频滚动或含折叠动效的页面中，优先在布局/测量阶段消费 `PaddingValues`，避免在组合阶段直接解包动态高度引发整页逐帧重组。

### 6.2 主 Tab 页顶栏、折叠联动与右上角溢出菜单

```kotlin
@Composable
fun TabPage(tab: Tab, outerPadding: PaddingValues) {
    val scrollBehavior = COUIScrollBehavior()
    val pageColor = COUITheme.colorScheme.background
    val topBarBackdrop = rememberLayerBackdrop {
        drawRect(pageColor)
        drawContent()
    }
    Box(
        Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        TabContent(
            tab = tab,
            outerPadding = outerPadding,
            modifier = Modifier.fillMaxSize().layerBackdrop(topBarBackdrop),
        )
        CompositionLocalProvider(
            LocalBackdrop provides topBarBackdrop,
            LocalDensity provides rememberGalleryDensity(),
        ) {
            TopAppBar(
                title = tab.title,
                color = pageColor,
                scrollBehavior = scrollBehavior,
                gradientGlass = true,
                actions = { OverflowMenu(onOpenSettings = { /* 打开设置 */ }) },
            )
        }
    }
}

@Composable
fun OverflowMenu(onOpenSettings: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        HighlightedIconButton(
            onClick = { expanded = true },
            modifier = Modifier.couiTopAppBarActionItem(),
        ) {
            Icon(COUIIcons.More, contentDescription = "更多")
        }
        OverlayListPopup(
            show = expanded,
            alignment = PopupPositionProvider.Align.End,
            onDismissRequest = { expanded = false },
        ) {
            ListPopupColumn {
                DropdownImpl(
                    item = DropdownItem(text = "设置"),
                    optionSize = 1,
                    isSelected = false,
                    index = 0,
                    role = Role.Button,
                    onSelectedIndexChange = {
                        expanded = false
                        onOpenSettings()
                    },
                )
            }
        }
    }
}
```

**实现要求**：
- **挂载 `nestedScroll`**：外层容器必须挂载 `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)`，确保列表滚动增量正常传递至顶栏大标题折叠状态机。
- **使用 `Modifier.couiTopAppBarActionItem()`**：顶栏操作按钮统一添加该修饰符，确保按钮尺寸、内边距及溢出菜单右边缘锚点对齐系统相册（参照机实测菜单右边缘与系统相册同为 `1214px`）。
- **保持 `OverlayListPopup` 默认几何参数**：不要覆写 `minWidth`、不要给菜单行添加额外的 `heightIn`、不要传入自定义 `PopupPositionProvider`。组件内置的面板物理密度、行高（参照机单行 `191px` 对系统 `190px`）与连续圆角剖面已与真机校准完毕（单行菜单高 `57dp` 时，默认的 `28dp` 圆角半径自然形成胶囊半圆端头）。
- **语义角色**：触发页面跳转或普通命令的菜单项显式指定 `role = Role.Button`（默认的 `Role.RadioButton` 仅用于单选列表项）。

### 6.3 带两侧配件圆钮的悬浮底栏（对标系统「相册 · 创作」）

```kotlin
BottomFloatingAccessoryNavigationBar(
    startAccessoryVisible = showFilter,
    startAccessory = { FilterBall() },
    endAccessoryVisible = showCompose,
    endAccessory = {
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = null,
                    indication = null,
                    role = Role.Button,
                    onClick = onCompose,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = COUIIcons.Add,
                contentDescription = "发布",
                tint = COUITheme.colorScheme.onSurface,
            )
        }
    },
) {
    /* BottomFloatingNavigationBarItem 列表 */
}
```

**实现要求**：
- **区分槽位显隐与槽位释放**：
  - `startAccessoryVisible = false` / `endAccessoryVisible = false` 仅执行圆钮融回主胶囊的收起动画，**保留两侧槽位宽度**，主胶囊宽度与居中位置保持恒定；
  - 若需要让主胶囊扩展至全宽，应将 `startAccessory` / `endAccessory` 参数本身传为 `null`。
- **配件内部点击关闭重复指示器**：`BottomFloatingAccessoryNavigationBar` 已为配件圆钮提供了圆形缩放与高光按压反馈，内部 `clickable` 必须传入 `indication = null`，避免叠加矩形状态层。
- **跨分页切换配件图标**：不同 Tab 页共用同一个配件圆钮但图标不同时，保持配件圆钮槽位常驻，仅对圆钮内部的 `Icon` 做先缩小消失、再放大显现的切换动效。

### 6.4 二级设置页与分组列表（对标系统「设置」）

```kotlin
@Composable
fun SettingsPage(onBack: () -> Unit, onOverflow: () -> Unit) {
    val scrollBehavior = COUIScrollBehavior()
    val pageColor = COUITheme.colorScheme.background
    val backdrop = rememberLayerBackdrop {
        drawRect(pageColor)
        drawContent()
    }
    Scaffold(
        containerColor = pageColor,
        topBar = {
            CompositionLocalProvider(LocalBackdrop provides backdrop) {
                TopAppBar(
                    title = "设置",
                    color = pageColor,
                    scrollBehavior = scrollBehavior,
                    gradientGlass = true,
                    navigationIcon = {
                        HighlightedIconButton(
                            onClick = onBack,
                            modifier = Modifier.couiTopAppBarActionItem(),
                        ) {
                            Icon(COUIIcons.Back, contentDescription = "返回")
                        }
                    },
                    actions = { OverflowMenu(onOpenSettings = onOverflow) },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .layerBackdrop(backdrop),
            contentPadding = padding,
        ) {
            item {
                Card {
                    /* coui-preference 设置项组件 */
                }
            }
        }
    }
}
```

**实现要求**：
- **使用默认应用密度**：系统「设置」的顶栏按钮按系统显示密度渲染（参照机 `40dp = 155px`），二级设置页**不包裹** `rememberGalleryDensity()`。
- **卡片圆角读取主题 Token**：自定义容器需要与 `Card` 对齐圆角时，读取 `COUITheme.dimens.card.cornerRadius`（ColorOS 17 标准值为 `17dp`），不硬编码圆角常数。

### 6.5 分段控件、搜索框与下拉刷新

- **胶囊分段控件**：使用 `TabRowWithContour`；需要玻璃边缘高光时传入 `indicatorMaterial = SearchBarDefaults.searchBarStroke()`。
- **搜索框**：使用 `InputField`，内置 `40dp` 胶囊高度、边缘高光、编辑态焦散光效与清除按钮；通过软键盘 IME 搜索动作提交，不在搜索框旁额外放置独立的「搜索」文字按钮。
- **下拉刷新**：使用 `PullToRefresh(topAppBarScrollBehavior = scrollBehavior, contentPadding = padding)`，刷新指示球会自动定位于顶栏下方，同时保持列表内容从玻璃顶栏后方滚过。

---

## 七、形状、交互反馈、导航与图标规范

### 1. 连续曲率圆角（Squircle）与正圆胶囊

- `SquircleShape` 采用超椭圆连续曲率曲线。当圆角半径等于控件半高时，超椭圆在 45° 方向向外饱满延伸，两端会保留极短的近似垂直段，视觉上呈超椭圆跑道形而非正圆半弧。
- 若目标系统控件经参照机核对确认为**标准正圆胶囊（Stadium / Pill）**，在该子树局部提供 `CompositionLocalProvider(LocalSquircleEnabled provides false)`，`SquircleShape` 即自动退化为标准正圆弧 `Outline.Rounded(min(w, h) / 2)`。

### 2. 按压反馈（Indication）

- `COUITheme` 默认将 `LocalIndication` 设置为 `COUIIndication`，在组件矩形边界内铺设按压状态层。
- 编写自定义胶囊按钮、圆形按钮或分段控件时：
  - 若不裁剪组件边界，应传入 `indication = null` 关闭矩形状态层，改用按压缩放动效；或先用 `Modifier.clip(shape)` 裁剪后再挂载 `clickable`。
  - 处理轻点（Quick Tap）反馈时，应在点击触发瞬间点亮状态并保持最小可见时长，避免手指快速抬起导致按压态在同一帧内消失；若点击同时触发路由跳转，避免将未消退的按压遮罩截入页面转场快照。

### 3. 预测性返回与 Navigation 3

- **由 `NavDisplay` 统一接管返回手势**：在使用 `NavDisplay` 的导航架构中，不在业务页面内部重复注册 `PredictiveBackHandler`（内层后注册的 Handler 会拦截返回事件，导致 `NavDisplay` 无法渲染下层页面预览）。
- **保持 Manifest 声明的返回回调配置**：通过 `AndroidManifest.xml` 的 `android:enableOnBackInvokedCallback="true"` 启用系统预测性返回，不在运行时通过反射修改 `ApplicationInfo`。
- **页面状态绑定返回栈条目**：列表滚动位置、顶栏折叠进度、分页索引等状态应保存在对应路由的返回栈条目（BackStack Entry）中，使预测性返回手势过程中的下层页面实时重建预览与松手后的最终状态完全一致。

### 4. 图标字重规范

- `COUIIcons.*` 提供的五档字重（`Light` / `Normal` / `Regular` / `Medium` / `Demibold`）均为**实心路径字形**。底栏或标签切换通过字重与颜色区分选中态，不混用线框与面性两套风格。
- 组合多个图标时保持视觉线宽一致（例如 `COUIIcons.Tune` 线宽较细，与实心图标并列时注意视觉平衡）。

---

## 八、工程构建与真机校验规范

### 1. 本地发布与依赖联调

- **全模块一次性发布**：向本地仓库发布快照时，统一执行 `./gradlew publishAllToLocalRepository`，一次性发布全部模块（单独执行子模块发布任务会重建 `build/repository/local` 并清理此前已发布的其他模块）。
- **工作区洁净度**：发布本地快照前确认 Git 工作区无未提交的临时修改，确保构建产物与提交历史严格对应。
- **JDK 版本要求**：COUI 的 Android 与 Desktop 字节码目标版本为 Java 21（Class File 65），使用单元测试消费库产物的工程需将测试运行时的 JVM 配置为 JDK 21。

### 2. 真机同屏度量与性能基准

- **统一参照设备**：所有 ColorOS 17 视觉与几何基准均以 **OnePlus PLK110（ColorOS V17.0.0，1272×2772）** 实测为准，不混用 ColorOS 16 或其他分辨率机型的测量数据。
- **基于客观剖面数据比对**：
  - **圆角剖面**：对比距控件顶边 `0 / 4 / 8 / 13 / 20 / 30 / 40 / 60 / 80 px` 各行处的边缘水平内缩像素量（例如 `OverlayListPopup` 在 PLK110 上实测为 `63/50/40/32/24/16/10/3/1 px`，对应系统相册 `63/50/41/33/25/16/10/3/1 px`）；
  - **行高与间距**：通过 `uiautomator dump` 相邻文本节点的纵向 `bounds` 中心距核算；
  - **按钮与光学材质**：采集穿过控件圆心的水平/垂直亮度剖面，玻璃控件取样边缘时选择背景纯色无干扰的一侧；
  - **校验 `uiautomator dump` 有效性**：确认命令输出包含 `UI hierchary dumped to` 后再解析 XML，防止读取上次残留文件；注意区分外层可点击容器节点与内层图标节点（`content-desc` 常挂在内部图标节点上）的 `bounds`。
- **Release 构建性能测试**：120Hz 滚动帧率与玻璃着色器性能一律在 **Release 构建（开启 R8 / 代码优化）** 下采集与评估。
