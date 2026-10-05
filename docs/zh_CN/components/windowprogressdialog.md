---
title: WindowProgressDialog
requiresScaffoldHost: false
prerequisites:
  - 可以在任何地方使用，不需要 `Scaffold` 或 `COUIPopupHost`
  - 在窗口层级渲染
hostComponent: None
popupHost: None
---

# WindowProgressDialog

`WindowProgressDialog` 是 `OverlayProgressDialog` 的窗口级版本：同样是带横向进度条与可选文字的小型居中卡片，但通过平台 `Dialog` 渲染而非 `COUIPopupHost`，因此无需 `Scaffold`。对应 ColorOS 的横向进度对话框（COUIRotatingDialogBuilder / `coui_progress_dialog_horizontal.xml`）。1.2.0 新增。

当 `progress` 为 `0.0f` 到 `1.0f` 之间的值时进度条为定值态，为 null 时为不定值态。

::: tip 提示
该组件不依赖 `Scaffold`，可在任意 Composable 作用域中使用。
:::

## 引入

```kotlin
import io.github.suqi8.coui.kmp.window.WindowProgressDialog
import io.github.suqi8.coui.kmp.layout.ProgressDialogDefaults
import io.github.suqi8.coui.kmp.layout.ProgressDialogColors
```

## 基本用法

```kotlin
var showProgress by remember { mutableStateOf(false) }
var progress by remember { mutableStateOf<Float?>(null) }

TextButton(
    text = "下载",
    onClick = { showProgress = true }
)

WindowProgressDialog(
    show = showProgress,
    progress = progress,
    text = "下载中...",
    onDismissRequest = { showProgress = false },
)
```

传入 `progress = null` 使用不定值态扫动：

```kotlin
WindowProgressDialog(
    show = showProgress,
    progress = null,
    text = "连接中...",
)
```

## 属性

### WindowProgressDialog 属性

| 属性名            | 类型                 | 说明                                                                  | 默认值                                         | 是否必须 |
| ----------------- | -------------------- | --------------------------------------------------------------------- | ---------------------------------------------- | -------- |
| show              | Boolean              | 是否显示对话框                                                        | -                                              | 是       |
| modifier          | Modifier             | 应用于对话框的修饰符                                                  | Modifier                                       | 否       |
| progress          | Float?               | 0.0f 到 1.0f 之间的进度值；为 null 时进度条为不定值态                 | null                                           | 否       |
| text              | String?              | 进度条下方的文字                                                      | null                                           | 否       |
| colors            | ProgressDialogColors | 文字与进度条的颜色配置                                                | ProgressDialogDefaults.progressDialogColors()  | 否       |
| backgroundColor   | Color                | 卡片背景色                                                            | DialogDefaults.backgroundColor()               | 否       |
| enableWindowDim   | Boolean              | 是否启用遮罩层                                                        | true                                           | 否       |
| onDismissRequest  | (() -> Unit)?        | 点击外部或返回时触发；为 null 时无法被用户关闭                        | null                                           | 否       |
| onDismissFinished | (() -> Unit)?        | 关闭动画完成后调用                                                    | null                                           | 否       |

### ProgressDialogDefaults 对象

默认值与 `OverlayProgressDialog` 共用，完整表格见 [OverlayProgressDialog](./overlayprogressdialog)；`CardWidth`、`CardMinHeight`、`CornerRadius`、`ContentHorizontalPadding`、`ProgressBarHeight`、`TextMarginTop` 以及 `progressDialogColors()` 工厂均相同。

## 行为

- 呈现方式与 `OverlayProgressDialog` 完全一致：始终居中，使用 COUI 居中对话框动效。
- 进度条复用共享的 `LinearProgressIndicator`，因此两种模式都带有 `ProgressBarRangeInfo` 语义：定值态给出 `0f..1f` 范围的读数，`progress` 为 null 时为 `Indeterminate`。
- 平台对话框会清除默认背景、遮罩与动画效果，因此只绘制 COUI 的圆角卡片。
