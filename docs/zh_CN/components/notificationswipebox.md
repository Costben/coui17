# NotificationSwipeBox

<div style="position: relative; height: 200px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../../compose/index.html?id=notificationSwipeBox" title="Demo"></iframe>
</div>

实验性通知左滑菜单。通知卡片和两个操作按钮共用融合球轮廓：向左拖动时连接逐渐拉开，设置、删除按钮依次分离。通知内容保持原宽度，在保留圆角的可见区域内向左移动。

## 使用

从 `io.github.suqi8.coui.kmp.basic` 导入 `NotificationSwipeBox`、`rememberNotificationSwipeState`，并声明 `ExperimentalCOUIApi`。示例图标来自 `coui-icons`。

```kotlin
@OptIn(ExperimentalCOUIApi::class)
@Composable
fun Notification() {
    val state = rememberNotificationSwipeState()
    NotificationSwipeBox(
        onSettings = { /* Open notification settings */ },
        onDismiss = { /* Remove the notification from your list */ },
        settingsIcon = COUIIcons.Settings,
        dismissIcon = COUIIcons.Delete,
        state = state,
        settingsDescription = "Settings",
        dismissDescription = "Delete",
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("New message")
            Text("Swipe left to reveal actions")
        }
    }
}
```

内容槽只放前景内容，避免使用不透明卡片背景遮住融合材质。通过 `LocalBackdrop` 提供同级壁纸后沿用现有玻璃效果；未提供背景时使用不透明材质。Android 13 以下显示独立形状，不绘制融合连接。颜色、壁纸染色与边缘光照沿用主题和 `MetaBallSurface`，尚未逐项对齐 SystemUI 的通知专用材质配方。

## 状态与交互

`state.reveal()`、`state.close()` 是挂起的弹簧动画；`revealFraction` 表示当前 0–1 视觉进度，`isRevealed` 表示目标状态。目标状态随页面重建保存。拖动可以打断动画；短距离慢拖回到关闭位置，长距离拖动或快速甩动展开菜单，向右拖动收回。即使内容采用 RTL 布局，手势仍是物理方向的左滑。

菜单停在 144dp 处；删除需要明确点击删除按钮，由 `onDismiss` 回调让调用方移除通知。本组件没有移植继续左滑后的二合一删除阶段。点击设置或删除后菜单收回。`enabled = false` 禁用拖动和操作。被完全遮住的按钮不参与点击或无障碍节点；容器也提供设置、删除的自定义无障碍操作。请传入本地化的按钮说明。

## 参数来源

取自 ColorOS 17 通知菜单资源和一段动效参数：

| 参数 | 默认值 | 来源 |
| --- | --- | --- |
| 按钮宽 / 高 | 60dp / 68dp | `notification_menu_icon_with_background_width` / `standard_height` |
| 按钮上边距 | 8dp | `notification_menu_icon_with_background_standard_height_diff` |
| 通知默认最小高度 | 84dp | `notification_menu_icon_with_background_parent_standard_height` |
| 按钮间距 / 外侧留白 | 10dp / 4dp | `notification_menu_icon_margin` / `margin_extra` |
| 卡片圆角 | 20dp | `notification_corner_radius` 16dp 连续曲率，按圆形拟合 |
| 按钮圆角 | 16dp | `RadiusParams.CARD_MENU_CONER` / `notif_menu_icon_background` |
| 融合范围 / 横纵系数 | 3.5dp / 1.2 / 2.0 | 通知融合控制器 |
| 跟随位移 | 8dp | 一段动效设置、删除按钮位移 |
| 图标渐显区间 | 单格露出进度 0.6–1.0 | `AnimateParamOne.FADE_START/END` |
| 回弹 | response 0.55s / bounce 0.4 | `AnimateParamOne.oneAnimate` |

拖动要求容器具有大于 144dp 的有限宽度。高通知的按钮仍与顶部保持 8dp 间距，与系统一致。

`rimColor` 是沿融合轮廓描出的高光。通知栏卡片的上下边缘在 `#2D2D2D` 卡身上测得 `#4E4E4E`，需要不透明白色；默认的半透明白更适合浮在亮色壁纸上的卡片。

紧凑通知可设置 `minHeight = 0.dp` 以内容确定高度；按钮高度随卡片高度减去 16dp，最大为 68dp。

示例应用中打开「设置项 → 通知左滑菜单」，即可体验左滑、设置、删除和重置操作。
