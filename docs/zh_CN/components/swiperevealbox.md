# SwipeRevealBox

通用的基础融球侧滑容器，将 ColorOS 17 的 `MetaBallSurface` 融球侧滑操作效果从通知样式中解耦为独立的基础组件。

与 [NotificationSwipeBox](./notificationswipebox.md) 不同，`SwipeRevealBox` 默认使用 `SwipeRevealDefaults.cardStyle()`，与标准 COUI [Card](./card.md) 保持一致的圆角与底色：`17.dp` Lp `3.0` 超椭圆圆角、不透明 `surfaceContainer` 填充、不启用玻璃模糊与边缘高光、展开的操作按钮采用居中紧凑的 `48.dp × 48.dp` 浮岛比例（不再与整张卡片等高），并使用临界阻尼回弹曲线，慢速拖动与松手归位都不会出现夸张弹跳。

所有视觉、几何与动效特性（卡片圆角、按钮圆角、圆角曲线类型、底色、玻璃模糊、边缘高光、按钮尺寸、间距、跟随距离与回弹弹簧）均集中在 `SwipeRevealStyle` 中，可按需组合或逐项卸除。

## 使用

从 `io.github.suqi8.coui.kmp.basic` 导入 `SwipeRevealBox`、`SwipeRevealAction`、`SwipeRevealDefaults` 与 `rememberSwipeRevealState`，并声明 `ExperimentalCOUIApi`。

```kotlin
@OptIn(ExperimentalCOUIApi::class)
@Composable
fun SwipeableCardRow(onSettings: () -> Unit, onDelete: () -> Unit) {
    val state = rememberSwipeRevealState()
    SwipeRevealBox(
        actions = listOf(
            SwipeRevealAction(
                icon = COUIIcons.Settings,
                description = "设置",
                onClick = onSettings,
            ),
            SwipeRevealAction(
                icon = null, // 使用内置带开盖动效的 NotificationTrashIcon
                description = "删除",
                color = COUITheme.colorScheme.error,
                onClick = onDelete,
            ),
        ),
        state = state,
        dismissOnFullSwipe = true,
    ) {
        BasicComponent(
            title = "会议记录.m4a",
            summary = "12.4 MB · 今天 10:20",
        )
    }
}
```

前景内容槽不要自带不透明背景色，以免遮住底层的融合轮廓。

## 样式预设与多种按钮形态

`SwipeRevealDefaults` 提供五套可组合预设，每个参数均支持单独覆写：

| 预设 | 卡片圆角 | 按钮几何 | 模糊 / 边缘光 | 动效阻尼 |
| --- | --- | --- | --- | --- |
| `SwipeRevealDefaults.cardStyle()` | `17.dp` Lp `3.0` 超椭圆（`CardDefaults.CornerRadius`） | 居中紧凑 `48.dp × 48.dp`，`14.dp` 圆角 | 均关闭 | 临界阻尼停靠（`1.0`，`320f`），轻柔 `6.dp` 跟随 |
| `SwipeRevealDefaults.circleActionStyle()` | `17.dp` Lp `3.0` 超椭圆 | COUI-Kit 圆形融球 `44.dp × 44.dp`（`22.dp` 圆角），`10.dp` 间距 | 均关闭 | 临界阻尼停靠，轻柔 `6.dp` 跟随 |
| `SwipeRevealDefaults.roundRectActionStyle()` | `17.dp` Lp `3.0` 超椭圆 | 上游 `SwipeActionItem` 风格 `68.dp × 52.dp`，`12.dp` 圆角，`6.dp` 间距 | 均关闭 | 临界阻尼停靠，轻柔 `6.dp` 跟随 |
| `SwipeRevealDefaults.groupCardStyle()` | `24.dp` Lp `2.5` 超椭圆（`CardDefaults.GroupCornerRadius`） | 居中紧凑 `48.dp × 48.dp`，`14.dp` 圆角 | 均关闭 | 临界阻尼停靠，轻柔 `6.dp` 跟随 |
| `SwipeRevealDefaults.notificationStyle()` | `16.dp` `CONIC`（`weight = 1.1`，`24.dp` 贝塞尔跨度） | 顶距 `8.dp` 的 `60.dp × 68.dp` 按钮，`10.dp` 间距，`4.dp` 右留白 | 均开启 | SystemUI 通知弹簧（`0.75`，`200f`） |

### 彩色胶囊、纯文字与图标 + 文字按钮

每个 `SwipeRevealAction` 都可以单独指定 `containerColor`（融合连接处自动做双色平滑过渡）、`label` 与 `icon`：

```kotlin
SwipeRevealBox(
    actions = listOf(
        SwipeRevealAction(
            icon = COUIIcons.Pin,
            label = "置顶",
            containerColor = COUITheme.colorScheme.primary,
            color = Color.White,
            onClick = onPin,
        ),
        SwipeRevealAction(
            label = "删除",
            useTrashIcon = true,
            containerColor = COUITheme.colorScheme.error,
            color = Color.White,
            onClick = onDelete,
        ),
    ),
    style = SwipeRevealDefaults.roundRectActionStyle(),
) {
    // 前景内容
}
```

### COUI-Kit 风格二次确认按钮

设置 `requireConfirmation = true` 后，首次点击会把删除图标切换为勾选确认（`✓`），再次点击才真正触发 `onClick`；收起菜单时自动取消待确认状态：

```kotlin
SwipeRevealBox(
    actions = listOf(
        SwipeRevealAction(
            icon = null,
            description = "删除",
            confirmDescription = "确认删除",
            color = COUITheme.colorScheme.error,
            requireConfirmation = true,
            onClick = onDelete,
        ),
    ),
    style = SwipeRevealDefaults.circleActionStyle(),
    dismissOnFullSwipe = true,
) {
    // 前景内容
}
```

## 状态与交互

- `SwipeRevealState` 提供 `revealFraction`（`0f..1f`）、`isRevealed`、`suspend fun reveal()` 与 `suspend fun close()`，并通过 `rememberSwipeRevealState()` 在重建时保留目标状态。
- 最多传入 3 个操作，按视觉从左到右排列；最后一个位于最右侧，左滑时最先露出。
- 开启 `dismissOnFullSwipe = true` 时，越过菜单再左滑 `SwipeRevealDefaults.MergeDistance`（`40.dp`）会触发触感反馈并合并次级按钮；`useTrashIcon = true` 时同步掀开垃圾桶盖，松手即滑走并触发主操作。
- 如果需要无融球融合效果的传统侧滑列表行，请参见 [SlideView](./slideview.md)。
