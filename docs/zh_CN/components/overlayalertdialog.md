---
title: OverlayAlertDialog
requiresScaffoldHost: true
prerequisites:
  - 必须在 `Scaffold` 内使用，以提供 `COUIPopupHost`
  - 在 `Scaffold` 外使用会导致弹窗内容无法渲染
  - 支持嵌套或并列的多个 `Scaffold`，无需额外配置
hostComponent: Scaffold
popupHost: COUIPopupHost
---

# OverlayAlertDialog

`OverlayAlertDialog` 是对话框家族中的居中警示形态，对应 COUI 的居中样式警示对话框（`AlertDialogBuildStyle.Center` / `coui_center_alert_dialog_layout.xml`）。它展示标题、消息、可选的自定义正文插槽，以及「取消 / 确认」按钮行。

与 `OverlayDialog` 的区别在于呈现方式：`OverlayDialog` 在紧凑窗口下吸附底部、仅在大屏居中，而 `OverlayAlertDialog` 始终居中，并始终使用 COUI 居中对话框的缩放 + 淡入淡出动效，与原生警示/确认弹窗一致。1.2.0 新增。

<div style="position: relative; height: 240px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../../compose/index.html?id=overlayAlertDialog" title="Demo" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin"></iframe>
</div>

`style` 选择构建样式。`AlertDialogStyle.Center`（默认）对应 `AlertDialogBuildStyle.Center`；`AlertDialogStyle.CenterTiny` 对应 `AlertDialogBuildStyle.Center.Tiny`，标题与消息内容不变，但面板压缩后吸附屏幕底边，见 [Center.Tiny](#center-tiny)。

::: danger 注意
该组件依赖 `Scaffold` 提供 `COUIPopupHost` 来渲染弹窗内容，必须在 `Scaffold` 内使用，否则弹窗内容无法正确渲染。
:::

## 引入

```kotlin
import io.github.suqi8.coui.kmp.overlay.OverlayAlertDialog
import io.github.suqi8.coui.kmp.layout.AlertDialogDefaults
import io.github.suqi8.coui.kmp.layout.AlertDialogColors
```

## 基本用法

```kotlin
var showAlert by remember { mutableStateOf(false) }

Scaffold {
    TextButton(
        text = "删除备份",
        onClick = { showAlert = true }
    )

    OverlayAlertDialog(
        show = showAlert,
        onConfirm = { /* 执行删除 */ showAlert = false },
        onCancel = { showAlert = false },
        title = "删除该备份？",
        summary = "该备份将从云端以及所有已同步设备上移除。",
        confirmText = "删除",
        cancelText = "取消",
    )
}
```

点击面板外部、返回手势与取消按钮都会走 `onCancel`，与 COUI 把返回键上报为否定选择的行为一致。

## 自定义正文插槽

`content` 是渲染在消息与按钮行之间的可选正文。标题与消息仍由面板内置渲染，插槽只承载内置文本无法表达的内容：

```kotlin
OverlayAlertDialog(
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

### OverlayAlertDialog 属性

| 属性名               | 类型                      | 说明                                                        | 默认值                                    | 是否必须 |
| -------------------- | ------------------------- | ----------------------------------------------------------- | ----------------------------------------- | -------- |
| show                 | Boolean                   | 是否显示对话框                                              | -                                         | 是       |
| onConfirm            | () -> Unit                | 点击确认（肯定）按钮时触发                                  | -                                         | 是       |
| onCancel             | () -> Unit                | 点击取消（否定）按钮、点击面板外部或返回时触发              | -                                         | 是       |
| modifier             | Modifier                  | 应用于对话框的修饰符                                        | Modifier                                  | 否       |
| title                | String?                   | 对话框标题                                                  | null                                      | 否       |
| summary              | String?                   | 对话框消息                                                  | null                                      | 否       |
| confirmText          | String                    | 确认（肯定）按钮的文案                                      | AlertDialogDefaults.ConfirmText           | 否       |
| cancelText           | String                    | 取消（否定）按钮的文案                                      | AlertDialogDefaults.CancelText            | 否       |
| neutralText          | String?                   | 可选第三个（中性）按钮的文案，位于两个按钮之间              | null                                      | 否       |
| onNeutral            | (() -> Unit)?             | 点击中性按钮时触发                                          | null                                      | 否       |
| recommendedButton    | AlertDialogButton?        | 作为推荐（实心主色）按钮的动作：`Negative` / `Neutral` / `Positive` | null                              | 否       |
| style                | AlertDialogStyle          | Center 家族构建样式：`Center` 或 `CenterTiny`               | AlertDialogStyle.Center                   | 否       |
| colors               | AlertDialogColors         | 标题、消息与面板的颜色配置                                  | AlertDialogDefaults.alertDialogColors()   | 否       |
| enableWindowDim      | Boolean                   | 是否启用遮罩层                                              | true                                      | 否       |
| onDismissFinished    | (() -> Unit)?             | 关闭动画完成后调用；若关闭被中途取消则不会触发              | null                                      | 否       |
| renderInRootScaffold | Boolean                   | 是否渲染在根（最外层）`Scaffold` 中。为 true 时覆盖全屏，为 false 时限制在当前 `Scaffold` 范围内 | true | 否 |
| cornerRadius         | Dp?                       | 圆角半径覆写；为 null 时使用 `AlertDialogDefaults.CornerRadius` | null                                  | 否       |
| maxWidth             | Dp                        | 对话框窗口的最大宽度；面板在两侧各内缩 16dp                  | AlertDialogDefaults.MaxWidth              | 否       |
| content              | (@Composable () -> Unit)? | 按钮行上方的可选自定义正文                                  | null                                      | 否       |

### AlertDialogDefaults 对象

#### 属性

| 属性名             | 类型   | 说明                                                                             |
| ------------------ | ------ | -------------------------------------------------------------------------------- |
| ConfirmText        | String | 确认按钮默认文案（`OK`）                                                          |
| CancelText         | String | 取消按钮默认文案（`Cancel`）                                                      |
| CornerRadius       | Dp     | 面板圆角（45.dp），与 `DialogDefaults.CornerRadius` 相同                          |
| MaxWidth           | Dp     | 对话框窗口最大宽度（392.dp），与 `DialogDefaults.MaxWidth` 相同；面板比它窄 32dp   |
| ButtonBarMarginTop | Dp     | 面板内容与按钮行之间的间距（8.dp，COUI `coui_alert_dialog_customer_layout_padding_bottom`） |
| TinyMaxHeight      | Dp     | Tiny 面板高度上限（320.dp，即 `coui_tiny_dialog_max_height` 352dp 减去原生最大高度算法固定扣除的 32dp） |
| TinyInsideMargin   | DpSize | Tiny 标题内边距：两侧 16dp、顶部 16dp（`coui_tiny_dialog_message_padding_horizontal`） |
| TinyTitleMarginBottom | Dp  | Tiny 标题下边距（8dp）                                                            |
| TinyTitleFontSize  | TextUnit | Tiny 标题字号（18.sp，`COUIDialogTextAppearance.Title.Tiny`）                     |
| TinyMessageFontSize | TextUnit | Tiny 消息字号（14.sp，`COUIAlertDialogMessageStyle.Tiny`）                        |
| TinyMessageMarginTop | Dp   | Tiny 消息滚动区上内边距（4dp，`center_dialog_scroll_padding_top`）                 |
| TinyMessageMarginBottom | Dp | Tiny 消息滚动区下内边距（8dp，`coui_tiny_dialog_scroll_padding_bottom`）           |
| TinyButtonBarPaddingBottom | Dp | Tiny 按钮栏下内边距（12dp，`coui_tiny_dialog_btn_bar_padding_vertical`）         |
| TinyButtonMarginVertical | Dp | Tiny 胶囊上下外边距（4dp，`coui_tiny_dialog_btn_margin_vertical`）                |
| TinyButtonFill     | Color  | Tiny 胶囊底色（`coui_alert_dialog_button_color_tiny`，`#E6E6E6`）                  |
| TinyButtonTextColor | Color | Tiny 胶囊文字色（`coui_alert_dialog_button_text_color_tiny`，`#000000`）           |

#### 函数

| 函数名             | 返回类型          | 说明                       |
| ------------------ | ----------------- | -------------------------- |
| alertDialogColors()| AlertDialogColors | 创建默认颜色配置（见下）   |
| titleColor()       | Color             | 默认标题颜色               |
| summaryColor()     | Color             | 默认消息颜色               |
| backgroundColor()  | Color             | 默认面板背景颜色           |
| tinyButtonColors() | TextButtonColors  | Tiny 胶囊颜色，含随主题变化的禁用底色 |

### `alertDialogColors()` 工厂

| 参数           | 类型  | 默认值                           | COUI 来源              |
| -------------- | ----- | -------------------------------- | ---------------------- |
| titleColor     | Color | DialogDefaults.titleColor()      | couiColorLabelPrimary  |
| summaryColor   | Color | DialogDefaults.summaryColor()    | couiColorLabelPrimary  |
| backgroundColor| Color | DialogDefaults.backgroundColor() | couiColorBackground    |

### AlertDialogColors

| 属性名          | 类型  | 说明           |
| --------------- | ----- | -------------- |
| titleColor      | Color | 标题颜色       |
| summaryColor    | Color | 消息颜色       |
| backgroundColor | Color | 面板背景颜色   |

## Center.Tiny

`AlertDialogStyle.CenterTiny` 是该家族的紧凑形态，对应 `AlertDialogBuildStyle.Center.Tiny`：

```kotlin
OverlayAlertDialog(
    show = showTiny,
    onConfirm = { showTiny = false },
    onCancel = { showTiny = false },
    style = AlertDialogStyle.CenterTiny,
    title = "开启定位？",
    summary = "天气需要你的位置来展示当前区域的预报。",
    confirmText = "开启",
    cancelText = "暂不",
)
```

尽管名为 Center，原生样式把 `android:gravity` 重新设回 `bottom`：面板吸附屏幕底边、铺满窗口宽度（Tiny 背景不带 `coui_dialog_layout_margin_horizontal` 内缩）、只圆上面两个角、使用 250ms 的底部滑入 `Animation.COUI.Dialog`、高度上限为 `TinyMaxHeight`，按钮行是 `coui_center_alert_dialog_button_panel_tiny.xml` 的全宽胶囊——中性 / 肯定 / 否定，两侧内缩 16dp、彼此间隔 4dp、距面板底边 12dp。这些胶囊在深色主题下同样是固定的 `#E6E6E6` 底色加黑色文字，与系统一致。

## 行为

- `AlertDialogStyle.Center` 始终居中，并使用 COUI 居中对话框动效：进入时 250ms 由 0.8 缩放到 1 并淡入，退出时 150ms 淡出。
- `AlertDialogStyle.CenterTiny` 在同样的 250ms 内自底边滑入，退出时淡出。
- 按钮行是 `DialogButtonBar`，遵循 COUI 按钮栏规则：只有恰好两个文案都放得下时才横排，否则整栏纵排（中性、肯定、否定）。
- `recommendedButton` 指定按钮栏的推荐按钮，即 `COUIButtonBarLayout.setRecommendButtonId` 绘制的实心主色胶囊。
- 点击面板外部与返回手势都会通过 `onCancel` 上报否定选择。
- 居中面板高度超过窗口三分之二时会被限制在该高度，与共享的对话框布局一致。
