---
title: WindowAlertDialog
requiresScaffoldHost: false
prerequisites:
  - 可以在任何地方使用，不需要 `Scaffold` 或 `COUIPopupHost`
  - 在窗口层级渲染
hostComponent: None
popupHost: None
---

# WindowAlertDialog

`WindowAlertDialog` 是 `OverlayAlertDialog` 的窗口级版本：同一个居中警示对话框，但通过平台 `Dialog` 渲染而非 `COUIPopupHost`，因此无需 `Scaffold`。它保持 COUI 居中警示样式（`AlertDialogBuildStyle.Center` / `coui_center_alert_dialog_layout.xml`），并额外应用顶部插入内边距，使面板不会滑到状态栏、标题栏或挖孔区域之下。1.2.0 新增。

::: tip 提示
该组件不依赖 `Scaffold`，可在任意 Composable 作用域中使用。
:::

## 引入

```kotlin
import io.github.suqi8.coui.kmp.window.WindowAlertDialog
import io.github.suqi8.coui.kmp.layout.AlertDialogDefaults
import io.github.suqi8.coui.kmp.layout.AlertDialogColors
```

## 基本用法

```kotlin
var showAlert by remember { mutableStateOf(false) }

TextButton(
    text = "删除备份",
    onClick = { showAlert = true }
)

WindowAlertDialog(
    show = showAlert,
    onConfirm = { /* 执行删除 */ showAlert = false },
    onCancel = { showAlert = false },
    title = "删除该备份？",
    summary = "该备份将从云端以及所有已同步设备上移除。",
    confirmText = "删除",
    cancelText = "取消",
)
```

## 自定义正文插槽

```kotlin
WindowAlertDialog(
    show = showAlert,
    onConfirm = { /* ... */ },
    onCancel = { showAlert = false },
    title = "删除该备份？",
    summary = "该备份将从云端以及所有已同步设备上移除。",
    content = {
        Text(
            text = "该操作无法撤销。",
            style = COUITheme.textStyles.footnote1,
        )
    },
)
```

## 属性

### WindowAlertDialog 属性

| 属性名            | 类型                      | 说明                                                        | 默认值                                   | 是否必须 |
| ----------------- | ------------------------- | ----------------------------------------------------------- | ---------------------------------------- | -------- |
| show              | Boolean                   | 是否显示对话框                                              | -                                        | 是       |
| onConfirm         | () -> Unit                | 点击确认（肯定）按钮时触发                                  | -                                        | 是       |
| onCancel          | () -> Unit                | 点击取消（否定）按钮、点击面板外部或返回时触发              | -                                        | 是       |
| modifier          | Modifier                  | 应用于对话框的修饰符                                        | Modifier                                 | 否       |
| title             | String?                   | 对话框标题                                                  | null                                     | 否       |
| summary           | String?                   | 对话框消息                                                  | null                                     | 否       |
| confirmText       | String                    | 确认（肯定）按钮的文案                                      | AlertDialogDefaults.ConfirmText          | 否       |
| cancelText        | String                    | 取消（否定）按钮的文案                                      | AlertDialogDefaults.CancelText           | 否       |
| colors            | AlertDialogColors         | 标题、消息与面板的颜色配置                                  | AlertDialogDefaults.alertDialogColors()  | 否       |
| enableWindowDim   | Boolean                   | 是否启用遮罩层                                              | true                                     | 否       |
| onDismissFinished | (() -> Unit)?             | 关闭动画完成后调用                                          | null                                     | 否       |
| cornerRadius      | Dp?                       | 圆角半径覆写；为 null 时使用 `AlertDialogDefaults.CornerRadius` | null                                 | 否       |
| maxWidth          | Dp                        | 对话框最大宽度                                              | AlertDialogDefaults.MaxWidth             | 否       |
| content           | (@Composable () -> Unit)? | 按钮行上方的可选自定义正文                                  | null                                     | 否       |

### AlertDialogDefaults 对象

默认值与 `OverlayAlertDialog` 共用，完整表格见 [OverlayAlertDialog](./overlayalertdialog)；`ConfirmText`、`CancelText`、`CornerRadius`、`MaxWidth`、`ButtonBarMarginTop` 以及 `alertDialogColors()` 工厂均相同。

## 行为

- 呈现方式与 `OverlayAlertDialog` 完全一致：始终居中，使用 COUI 居中对话框的缩放 + 淡入淡出动效，并带 `DialogButtonBar` 按钮行。
- 平台对话框会清除默认背景、遮罩与动画效果，因此只绘制 COUI 的圆角面板。
- 点击平台窗口外部与返回手势都会通过 `onCancel` 上报否定选择。
