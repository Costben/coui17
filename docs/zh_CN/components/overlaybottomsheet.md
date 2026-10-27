---
title: OverlayBottomSheet
requiresScaffoldHost: true
prerequisites:
  - 必须在 `Scaffold` 中使用以提供 `COUIPopupHost`
  - 未在 `Scaffold` 中使用将导致弹出内容无法渲染
  - 支持多个嵌套或并列的 `Scaffold`，无需额外配置
hostComponent: Scaffold
popupHost: COUIPopupHost
---

# OverlayBottomSheet

`OverlayBottomSheet` 是 COUI 中的底部抽屉组件，从屏幕底部滑入显示。支持拖拽手势关闭和自定义样式。

<div style="position: relative; height: 240px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../../compose/index.html?id=overlayBottomSheet" title="Demo" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin"></iframe>
</div>

::: danger 使用前提
此组件依赖 `Scaffold` 提供的 `COUIPopupHost` 以显示弹出内容。必须在 `Scaffold` 中使用，否则弹出内容无法正常渲染。
:::

## 引入

```kotlin
import io.github.suqi8.coui.kmp.overlay.OverlayBottomSheet
import io.github.suqi8.coui.kmp.theme.LocalDismissState
```

## 基本用法

OverlayBottomSheet 组件提供了基础的底部抽屉功能:

```kotlin
var showBottomSheet by remember { mutableStateOf(false) }

Scaffold {
    TextButton(
        text = "显示底部抽屉",
        onClick = { showBottomSheet = true }
    )

    OverlayBottomSheet(
        show = showBottomSheet,
        title = "底部抽屉标题",
        onDismissRequest = { showBottomSheet = false }
    ) {
        Text(text = "这是底部抽屉的内容")
    }
}
```

## 属性

### OverlayBottomSheet 属性

| 属性名                     | 类型                      | 说明                                         | 默认值                                     | 是否必须 |
| -------------------------- | ------------------------- | -------------------------------------------- | ------------------------------------------ | -------- |
| show                       | Boolean                   | 是否显示底部抽屉                             | -                                          | 是       |
| modifier                   | Modifier                  | 应用于底部抽屉的修饰符                       | Modifier                                   | 否       |
| title                      | String?                   | 底部抽屉的标题                               | null                                       | 否       |
| startAction                | @Composable (() -> Unit)? | 可选的左侧操作按钮(例如关闭按钮)，标题栏图标请用 `SheetActionButton` | null                                       | 否       |
| endAction                  | @Composable (() -> Unit)? | 可选的右侧操作按钮(例如提交按钮)             | null                                       | 否       |
| backgroundColor            | Color                     | 底部抽屉背景色                               | BottomSheetDefaults.backgroundColor() | 否       |
| enableWindowDim            | Boolean                   | 是否启用遮罩层                               | true                                       | 否       |
| cornerRadius               | Dp                        | 顶部圆角半径                                 | BottomSheetDefaults.cornerRadius      | 否       |
| sheetMaxWidth              | Dp                        | 底部抽屉的最大宽度                           | BottomSheetDefaults.maxWidth          | 否       |
| onDismissRequest           | (() -> Unit)?             | 当用户请求关闭（点击遮罩层或返回手势）时触发 | null                                       | 否       |
| onDismissFinished          | (() -> Unit)?             | 关闭动画完成后调用；若关闭过程被中途取消（例如 `show` 被设回 true），则不会触发 | null     | 否       |
| outsideMargin              | DpSize                    | 底部抽屉外部边距                             | BottomSheetDefaults.outsideMargin     | 否       |
| insideMargin               | DpSize                    | 底部抽屉内部内容的边距                       | BottomSheetDefaults.insideMargin      | 否       |
| defaultWindowInsetsPadding | Boolean                   | 是否应用默认窗口插入内边距                   | true                                       | 否       |
| dragHandleColor            | Color                     | 拖拽指示器的颜色                             | BottomSheetDefaults.dragHandleColor() | 否       |
| allowDismiss               | Boolean                   | 是否允许通过拖拽或返回手势关闭抽屉           | true                                       | 否       |
| enableNestedScroll         | Boolean                   | 是否允许内容嵌套滚动                         | true                                       | 否       |
| renderInRootScaffold       | Boolean                   | 是否在根（最外层）Scaffold 中渲染底部抽屉。为 true 时，覆盖全屏。为 false 时，在当前 Scaffold 的范围内渲染 | true | 否 |
| content                    | @Composable () -> Unit    | 底部抽屉的内容                               | -                                          | 是       |

### BottomSheetDefaults

#### BottomSheetDefaults 属性

| 属性名        | 类型   | 说明                  |
| ------------- | ------ | --------------------- |
| cornerRadius  | Dp     | 默认圆角半径 (20.dp，COUI couiRoundCornerXL) |
| maxWidth      | Dp     | 默认宽度上限 (Dp.Infinity，实际宽度由 COUI 响应式栅格决定) |
| outsideMargin | DpSize | 底部抽屉外部默认边距 (DpSize(0.dp, 0.dp)) |
| insideMargin  | DpSize | 底部抽屉内部默认边距 (DpSize(24.dp, 0.dp)；宽 = 水平内边距，高 = 底部内边距) |

#### BottomSheetDefaults 函数

| 函数名            | 返回类型 | 说明                   |
| ----------------- | -------- | ---------------------- |
| backgroundColor() | Color    | 获取默认背景颜色       |
| dragHandleColor() | Color    | 获取默认拖拽指示器颜色 |

## 进阶用法

### 自定义样式底部抽屉

```kotlin
var showBottomSheet by remember { mutableStateOf(false) }

Scaffold {
    TextButton(
        text = "显示自定义样式底部抽屉",
        onClick = { showBottomSheet = true }
    )

    OverlayBottomSheet(
        show = showBottomSheet,
        title = "自定义样式",
        backgroundColor = COUITheme.colorScheme.surfaceVariant,
        dragHandleColor = COUITheme.colorScheme.primary,
        outsideMargin = DpSize(16.dp, 0.dp),
        insideMargin = DpSize(32.dp, 16.dp),
        onDismissRequest = { showBottomSheet = false }
    ) {
        Column {
            Text("自定义样式的底部抽屉")
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                text = "关闭",
                onClick = { showBottomSheet = false },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
```

### 带列表内容的底部抽屉

```kotlin
var showBottomSheet by remember { mutableStateOf(false) }
var selectedItem by remember { mutableStateOf("") }

Scaffold {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(
            text = "显示选择列表",
            onClick = { showBottomSheet = true }
        )
        
        Text("已选择: $selectedItem")
    }

    OverlayBottomSheet(
        show = showBottomSheet,
        title = "选择项目",
        onDismissRequest = { showBottomSheet = false }
    ) {
        LazyColumn {
            items(20) { index ->
                Text(
                    text = "项目 ${index + 1}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedItem = "项目 ${index + 1}"
                            showBottomSheet = false
                        }
                        .padding(vertical = 12.dp)
                )
            }
        }
    }
}
```

### 不使用遮罩层

```kotlin
var showBottomSheet by remember { mutableStateOf(false) }

Scaffold {
    TextButton(
        text = "显示无遮罩底部抽屉",
        onClick = { showBottomSheet = true }
    )

    OverlayBottomSheet(
        show = showBottomSheet,
        title = "无遮罩层",
        enableWindowDim = false,
        onDismissRequest = { showBottomSheet = false }
    ) {
        Text("这个底部抽屉没有背景遮罩层")
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            text = "关闭",
            onClick = { showBottomSheet = false },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
```

### 带操作按钮的底部抽屉

```kotlin
var showBottomSheet by remember { mutableStateOf(false) }

Scaffold {
    TextButton(
        text = "显示带操作按钮的底部抽屉",
        onClick = { showBottomSheet = true }
    )

    OverlayBottomSheet(
        show = showBottomSheet,
        title = "操作面板",
        startAction = {
            val dismiss = LocalDismissState.current
            TextButton(
                text = "取消",
                onClick = { dismiss?.invoke() }
            )
        },
        endAction = {
            val dismiss = LocalDismissState.current
            TextButton(
                text = "确认",
                onClick = { dismiss?.invoke() },
                colors = ButtonDefaults.textButtonColorsPrimary()
            )
        },
        onDismissRequest = { showBottomSheet = false }
    ) {
        Text("带有自定义标题栏操作按钮的内容")
        Spacer(modifier = Modifier.height(16.dp))
        Text("左右两侧的操作按钮显示在标题栏中")
    }
}
```

### 带表单的底部抽屉

```kotlin
var showBottomSheet by remember { mutableStateOf(false) }
var textFieldValue by remember { mutableStateOf("") }
var switchState by remember { mutableStateOf(false) }

Scaffold {
    TextButton(
        text = "显示表单底部抽屉",
        onClick = { showBottomSheet = true }
    )

    OverlayBottomSheet(
        show = showBottomSheet,
        title = "设置表单",
        onDismissRequest = { showBottomSheet = false }
    ) {
        Card(
            colors = CardDefaults.defaultColors(
                color = COUITheme.colorScheme.secondaryContainer,
            ),
        ) {
            TextField(
                modifier = Modifier.padding(vertical = 12.dp),
                value = textFieldValue,
                label = "输入内容",
                maxLines = 1,
                onValueChange = { textFieldValue = it }
            )
            
            SwitchPreference(
                title = "开关选项",
                checked = switchState,
                onCheckedChange = { switchState = it }
            )
        }
        
        Spacer(Modifier.height(12.dp))
        
        Row(
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                text = "取消",
                onClick = { showBottomSheet = false },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(20.dp))
            TextButton(
                text = "确认",
                onClick = { showBottomSheet = false },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.textButtonColorsPrimary()
            )
        }
    }
}
```

### 自适应内容高度

```kotlin
var showBottomSheet by remember { mutableStateOf(false) }

Scaffold {
    TextButton(
        text = "显示自适应高度底部抽屉",
        onClick = { showBottomSheet = true }
    )

    OverlayBottomSheet(
        show = showBottomSheet,
        title = "自适应高度",
        onDismissRequest = { showBottomSheet = false }
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Text("高度会自动适应内容")
            Spacer(modifier = Modifier.height(16.dp))
            Text("可以添加任意多的内容")
            Spacer(modifier = Modifier.height(16.dp))
            Text("但不会覆盖到状态栏区域")
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                text = "关闭",
                onClick = { showBottomSheet = false },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
```

### 禁止关闭的底部抽屉

```kotlin
var showBottomSheet by remember { mutableStateOf(false) }

Scaffold {
    TextButton(
        text = "显示禁止关闭的底部抽屉",
        onClick = { showBottomSheet = true }
    )

    OverlayBottomSheet(
        show = showBottomSheet,
        title = "禁止关闭",
        allowDismiss = false,
        onDismissRequest = { showBottomSheet = false }
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Text("这个底部抽屉无法通过拖拽或返回手势关闭")
            Spacer(modifier = Modifier.height(16.dp))
            Text("您必须通过下面的按钮显式关闭它")
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                text = "关闭",
                onClick = { showBottomSheet = false },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
```

## 标题栏图标按钮

标题栏是一行 50dp 的 `COUIToolbar`（`coui_list_dialog_toolbar_height`），左右各 16dp 内边距
（`toolbar_center_menu_padding_horizontal_compat`，`values-v37`）。放在里面的图标按钮在原生里是
`COUIActionMenuItemView`：

| 项 | 值 | 来源 |
| :--- | :--- | :--- |
| 按钮 | 40dp × 40dp | `coui_action_menu_item_min_width`，`values-v37` 从 48dp 降到 40dp |
| 圆角 | 20dp（正圆） | `ToolbarMaterialEffectDelegate` 取 `min(w, h) / 2` |
| 常态填充 | 无 | `new ColorDrawable(0)`，只有按下才画 |
| 按下 | `?couiColorPress` 圆形（`#1F000000` / 夜间 `#33FFFFFF`），`response 0.3 / bounce 0` 弹簧 | `COUIMaskEffectDrawable` |
| 图标 | 24dp，四周内缩 8dp | `abc_action_menu_item_layout.xml` |
| 图标色 | `?couiColorLabelPrimary`（`#E6000000` / `#E6FFFFFF`） | `menu_ic_confirm_normal.xml` |
| 同侧间距 | 10dp | `coui_actionbar_menuitemview_item_spacing`，`values-v37` |

所以标题栏两端要用 `SheetActionButton`，不要用 `IconButton` —— 后者是 48dp 加圆角方形的水波纹，
会把这个 50dp 的行撑高，按下时的形状也不对：

```kotlin
OverlayBottomSheet(
    show = show,
    title = "操作面板",
    startAction = {
        SheetActionButton(onClick = { dismiss?.invoke() }) {
            Icon(COUIIcons.Close, contentDescription = "取消",
                modifier = Modifier.size(BottomSheetDefaults.ActionButtonIconSize))
        }
    },
    endAction = {
        SheetActionButton(onClick = { confirm() }) {
            Icon(COUIIcons.Ok, contentDescription = "确定",
                modifier = Modifier.size(BottomSheetDefaults.ActionButtonIconSize))
        }
    },
    onDismissRequest = { show = false },
) { /* ... */ }
```

### 光效开关（按真机取值）

同一个 delegate 还负责两样东西，它们各自有独立的系统开关，**在 PLK110 / V17.0.0 上的实测值是**：

| 效果 | 开关 | 真机值 | 结论 |
| :--- | :--- | :--- | :--- |
| 圆形描边 `TYPE_FRAMEWORK_CIRCLE_1`（1.9dp，α0.2 / 夜间 0.6） | `system_material_stroke_enable` | **1** | **画**，`SheetActionButton` 已经带上 |
| AGSL 光斑 `TYPE_OPAQUE_SMALL_2` | `system_material_blur_enable` × `com.oplus.appcompat.texture_effect.Enable` | 1 × 未设置 | **不画**（`AppFeatureProviderUtils.getBoolean(..., false)` 默认 false） |

判定来自 `COUIMaterialStateManager`：`isStrokeEffectEnable()` 直接返回
`MATERIAL_STROKE_ENABLE`（= `system_material_stroke_enable == 1`），而
`isSpotLightEffectEnable()` 是 `MATERIAL_BLUR_ENABLE && MATERIAL_FEATURE_ENABLE`。
所以真机的勾/叉**有描边、没有光斑** —— 光斑默认打开会比真机亮一档。
:::
