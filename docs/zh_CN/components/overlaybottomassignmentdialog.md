---
title: OverlayBottomAssignmentDialog
requiresScaffoldHost: true
prerequisites:
  - 必须在 `Scaffold` 内使用，由 `Scaffold` 提供 `COUIPopupHost`
  - 在 `Scaffold` 外使用会导致浮层内容无法渲染
  - 支持多层嵌套或并排的 `Scaffold`，无需额外配置
hostComponent: Scaffold
popupHost: COUIPopupHost
---

# OverlayBottomAssignmentDialog

`OverlayBottomAssignmentDialog` 是 COUI 的 `COUIAlertDialog.BottomAssignment`：窗口在**任何尺寸下都贴住屏幕底边**的警告对话框。

<div style="position: relative; height: 240px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../compose/index.html?id=overlayBottomAssignmentDialog" title="Demo" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin"></iframe>
</div>

::: danger 前置条件
本组件依赖 `Scaffold` 提供 `COUIPopupHost` 来渲染浮层内容，必须在 `Scaffold` 内使用，否则浮层内容无法正确显示。
:::

它不是底部面板：面板用的是警告对话框的材质（`coui_alert_dialog_builder_background`，内外边距由 `coui_dialog_layout_margin_horizontal` / `..._vertical` 决定），从底边滑入，没有手柄、没有拖动关闭手势，也没有面板的模糊材质。`BottomAssignment` 把窗口 gravity 设为 bottom（`0x50`）并保持不变，所以大屏的居中呈现也不会生效。

::: warning 实验性
组件标注了 `@ExperimentalCOUIApi`，使用前需要 `@OptIn(ExperimentalCOUIApi::class)`。
:::

## 导入

```kotlin
import io.github.suqi8.coui.kmp.overlay.OverlayBottomAssignmentDialog
```

## 基础用法

```kotlin
var show by remember { mutableStateOf(false) }

OverlayBottomAssignmentDialog(
    show = show,
    title = "贴底对话框",
    summary = "这个面板在任何窗口尺寸下都贴在底边。",
    onDismissRequest = { show = false },
) {
    DialogButtonBar(
        negative = DialogButtonBarAction(text = "取消", onClick = { show = false }),
        positive = DialogButtonBarAction(text = "确定", onClick = { show = false }),
    )
}
```

`content` 是对话框主体，按钮栏由调用方自己提供——和 COUI 里自带按钮面板的偏好编辑器一致。标准警告对话框动作传入 [`DialogButtonBar`](./dialogbuttonbar) 即可。

## 属性

| 名称 | 类型 | 默认值 | 说明 |
| :--- | :--- | :--- | :--- |
| `show` | `Boolean` | — | 是否显示对话框。 |
| `onDismissRequest` | `() -> Unit` | — | 点击外部或返回时调用。 |
| `modifier` | `Modifier` | `Modifier` | 应用到对话框内容。 |
| `title` | `String?` | `null` | 标题，排布同警告对话框的 `alertTitle`。 |
| `summary` | `String?` | `null` | 正文，排布同警告对话框的 `message`。 |
| `colors` | `AlertDialogColors` | `AlertDialogDefaults.alertDialogColors()` | 标题、正文与面板颜色。 |
| `enableWindowDim` | `Boolean` | `true` | 显示时是否压暗窗口。 |
| `onDismissFinished` | `(() -> Unit)?` | `null` | 关闭动画结束后的回调。 |
| `renderInRootScaffold` | `Boolean` | `true` | 在最外层 Scaffold 渲染以覆盖整屏；`false` 时限制在当前 Scaffold 内。 |
| `cornerRadius` | `Dp?` | `null` | 圆角覆盖值，`null` 时用 `AlertDialogDefaults.CornerRadius`。 |
| `maxWidth` | `Dp` | `AlertDialogDefaults.MaxWidth` | 面板最大宽度。 |
| `content` | `@Composable () -> Unit` | — | 对话框主体，位于标题与正文之下。 |

## 与原生实现的差异

COUI 的 `create(preferenceView, point)` 还会把窗口锚定到打开它的那一行。Compose 里由 Scaffold 承载的对话框是单层全屏，因此这一版保留贴底摆放、去掉行锚定。
