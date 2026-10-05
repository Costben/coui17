---
title: OverlayProgressDialog
requiresScaffoldHost: true
prerequisites:
  - 必须在 `Scaffold` 内使用，以提供 `COUIPopupHost`
  - 在 `Scaffold` 外使用会导致弹窗内容无法渲染
  - 支持嵌套或并列的多个 `Scaffold`，无需额外配置
hostComponent: Scaffold
popupHost: COUIPopupHost
---

# OverlayProgressDialog

`OverlayProgressDialog` 是 `LoadingDialog` 的横向进度版本，对应 ColorOS 的横向进度对话框（COUIRotatingDialogBuilder / `coui_progress_dialog_horizontal.xml`）：同样是始终居中的小卡片，只是把旋转加载指示器换成横向进度条，并在其下方展示可选文字。

当 `progress` 为 `0.0f` 到 `1.0f` 之间的值时进度条为定值态，为 null 时为不定值态。1.2.0 新增。

::: danger 注意
该组件依赖 `Scaffold` 提供 `COUIPopupHost` 来渲染弹窗内容，必须在 `Scaffold` 内使用，否则弹窗内容无法正确渲染。
:::

## 引入

```kotlin
import io.github.suqi8.coui.kmp.overlay.OverlayProgressDialog
import io.github.suqi8.coui.kmp.layout.ProgressDialogDefaults
import io.github.suqi8.coui.kmp.layout.ProgressDialogColors
```

## 基本用法

定值态下载对话框：

```kotlin
var showProgress by remember { mutableStateOf(false) }
var progress by remember { mutableStateOf(0f) }

Scaffold {
    TextButton(
        text = "下载",
        onClick = { showProgress = true }
    )

    OverlayProgressDialog(
        show = showProgress,
        progress = progress,
        text = "下载中...",
    )

    LaunchedEffect(showProgress) {
        if (showProgress) {
            repeat(40) { step ->
                progress = step / 40f
                delay(60)
            }
            showProgress = false
        }
    }
}
```

当总大小尚不可知时传入 `progress = null`，进度条会改为 `LinearProgressIndicator` 的不定值扫动：

```kotlin
OverlayProgressDialog(
    show = showProgress,
    progress = null,
    text = "连接中...",
)
```

文字是可选的，也可以只显示进度条：

```kotlin
OverlayProgressDialog(show = showProgress, progress = 0.5f)
```

## 用户主动关闭

默认情况下该对话框不能被用户关闭，与 COUI 不可取消的进度对话框一致。传入 `onDismissRequest` 后可通过点击外部或返回手势关闭：

```kotlin
OverlayProgressDialog(
    show = showProgress,
    progress = progress,
    text = "下载中...",
    onDismissRequest = { showProgress = false }
)
```

## 属性

### OverlayProgressDialog 属性

| 属性名               | 类型                  | 说明                                                                                    | 默认值                                              | 是否必须 |
| -------------------- | --------------------- | --------------------------------------------------------------------------------------- | --------------------------------------------------- | -------- |
| show                 | Boolean               | 是否显示对话框                                                                          | -                                                   | 是       |
| modifier             | Modifier              | 应用于对话框的修饰符                                                                    | Modifier                                            | 否       |
| progress             | Float?                | 0.0f 到 1.0f 之间的进度值；为 null 时进度条为不定值态                                   | null                                                | 否       |
| text                 | String?               | 进度条下方的文字                                                                        | null                                                | 否       |
| colors               | ProgressDialogColors  | 文字与进度条的颜色配置                                                                  | ProgressDialogDefaults.progressDialogColors()       | 否       |
| backgroundColor      | Color                 | 卡片背景色                                                                              | DialogDefaults.backgroundColor()                    | 否       |
| enableWindowDim      | Boolean               | 是否启用遮罩层                                                                          | true                                                | 否       |
| onDismissRequest     | (() -> Unit)?         | 点击外部或返回时触发；为 null 时无法被用户关闭                                          | null                                                | 否       |
| onDismissFinished    | (() -> Unit)?         | 关闭动画完成后调用                                                                      | null                                                | 否       |
| renderInRootScaffold | Boolean               | 是否渲染在根（最外层）`Scaffold` 中。为 true 时覆盖全屏，为 false 时限制在当前 `Scaffold` 范围内 | true                                          | 否       |

### ProgressDialogDefaults 对象

#### 属性

| 属性名                    | 类型 | 说明                                                                        |
| ------------------------- | ---- | --------------------------------------------------------------------------- |
| CardWidth                 | Dp   | 卡片宽度，沿用加载卡片的 COUI `coui_spinner_layout_width`（152.dp）          |
| CardMinHeight             | Dp   | 卡片最小高度，COUI `coui_spinner_layout_min_height`（120.dp）                |
| CornerRadius              | Dp   | 卡片圆角，COUI `coui_round_corner_m_radius`（9.dp）                          |
| ContentHorizontalPadding  | Dp   | 卡片内水平内边距，避免进度条贴边（16.dp）                                    |
| ProgressBarHeight         | Dp   | 进度条高度，COUI `coui_loading_dialog_progress_height`（4.dp）               |
| TextMarginTop             | Dp   | 进度条与文字之间的间距（12.dp）                                              |

#### 函数

| 函数名                 | 返回类型             | 说明                     |
| ---------------------- | -------------------- | ------------------------ |
| progressDialogColors() | ProgressDialogColors | 创建默认颜色配置（见下） |
| textColor()            | Color                | 默认文字颜色（主要标签色） |

### `progressDialogColors()` 工厂

| 参数           | 类型                    | 默认值                                                | COUI 来源                    |
| -------------- | ----------------------- | ----------------------------------------------------- | ---------------------------- |
| textColor      | Color                   | COUITheme.colorScheme.onBackground                    | couiColorLabelPrimary        |
| progressColors | ProgressIndicatorColors | ProgressIndicatorDefaults.progressIndicatorColors()   | COUIProgressHorizontal 样式  |

### ProgressDialogColors

| 属性名         | 类型                    | 说明             |
| -------------- | ----------------------- | ---------------- |
| textColor      | Color                   | 文字颜色         |
| progressColors | ProgressIndicatorColors | 进度条轨道与填充颜色 |

## 行为

- 卡片始终居中，并使用 COUI 居中对话框动效，与 `LoadingDialog` 一致。
- 进度条复用共享的 `LinearProgressIndicator`，因此两种模式都带有 `ProgressBarRangeInfo` 语义：定值态给出 `0f..1f` 范围的读数，`progress` 为 null 时为 `Indeterminate`。
- 超出 `0.0f..1.0f` 的 `progress` 会被指示器收敛到范围内。
- 文字为 `body2` 字号并居中，与加载对话框的文字一致。
