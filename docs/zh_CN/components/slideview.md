# SlideView

<div style="position: relative; height: 260px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../../compose/index.html?id=slideView" title="Demo"></iframe>
</div>

实验性的侧滑列表行：向起始方向滑动，内容让开后露出背后的操作按钮，对应 ColorOS 17 的侧滑删除（`COUISlideView` 的矩形样式，日历的自定义提醒列表使用的就是这一种）。操作按钮铺满整行高度，四角为直角，第一个按钮贴在末端边缘。

## 使用

从 `io.github.suqi8.coui.kmp.basic` 导入 `SlideView`、`SlideMenuItem` 和 `rememberSlideViewState`，并 opt-in `ExperimentalCOUIApi`。操作按钮画在内容下层，所以内容需要自带背景。

```kotlin
@OptIn(ExperimentalCOUIApi::class)
@Composable
fun ReminderRow(title: String, onRemove: () -> Unit) {
    val state = rememberSlideViewState()
    val scope = rememberCoroutineScope()
    SlideView(
        menuItems = listOf(SlideMenuItem(text = null, icon = COUIIcons.Delete, contentDescription = "删除")),
        onMenuItemClick = {
            scope.launch {
                state.delete() // 播放删除动画
                onRemove()     // 再从列表中移除这一项
            }
        },
        state = state,
    ) {
        BasicComponent(modifier = Modifier.background(CardDefaults.defaultColors().color), title = title)
    }
}
```

系统的删除按钮是一个 24dp 的垃圾桶图标，没有文字。库里不附带这个图标，请传入自己的图标，或者通过 `text` 传文字。

## 交互

- 向起始方向滑动时，在菜单完全露出之前，内容 1:1 跟手。越过菜单后拖动带阻尼，按钮随之分散，文字始终居中在变宽的色块里。
- 松手时，速度超过 1000px/s 的快速滑动直接打开或关闭；否则按是否过半决定停在哪边。回位使用刚度 200 的临界阻尼弹簧。
- 打开状态下点内容会收起，最长 200ms。点操作按钮会以按钮下标回调 `onMenuItemClick`。
- `state.delete()` 用 330ms 把内容滑出整行，同时用 400ms 把行高收起，两者都用 `COUIMoveEase`。行高收完后函数返回。
- `fastDeleteEnabled = true` 时，越过菜单 48dp 以上松手，或越过 30dp 以上快速滑动，会直接删除这一行，之后回调 `onFastDelete`。
- 每个操作按钮同时提供为无障碍自定义操作。

## 属性

| 属性 | 类型 | 说明 | 默认值 |
| --- | --- | --- | --- |
| menuItems | `List<SlideMenuItem>` | 操作按钮，从末端边缘向内排列 | — |
| onMenuItemClick | `(Int) -> Unit` | 点击按钮时回调，参数为按钮下标 | — |
| modifier | `Modifier` | 行的修饰符 | `Modifier` |
| state | `SlideViewState` | 行的位置与删除状态 | `rememberSlideViewState()` |
| enabled | `Boolean` | 是否允许滑动 | `true` |
| fastDeleteEnabled | `Boolean` | 越过菜单足够远时是否直接删除 | `false` |
| onFastDelete | `() -> Unit` | 快速删除收起行之后回调 | `{}` |
| colors | `SlideViewColors` | 按钮底色、文字和图标颜色 | `SlideViewDefaults.slideViewColors()` |
| textStyle | `TextStyle` | 按钮文字样式 | `SlideViewDefaults.textStyle()` |
| content | `@Composable () -> Unit` | 行内容 | — |

### SlideMenuItem

| 属性 | 类型 | 说明 | 默认值 |
| --- | --- | --- | --- |
| text | `String?` | 居中绘制的文字 | — |
| icon | `ImageVector?` | 按原始尺寸居中绘制的图标 | `null` |
| contentDescription | `String?` | 无障碍标签 | `text` |
| color | `Color` | 按钮底色；`Color.Unspecified` 取 `colors.menuColor` | `Color.Unspecified` |
| width | `Dp` | 最小宽度；文字较长时按钮随之加宽 | `80.dp` |

### SlideViewState

| 成员 | 说明 |
| --- | --- |
| `isOpen` | 菜单是否已打开或正在打开 |
| `isDeleting` | 删除动画是否正在进行或已结束 |
| `open()` / `close()` | 用松手时的弹簧打开 / 用短滑动收起 |
| `delete()` | 播放删除动画，行高收完后返回 |

## SlideViewDefaults

| 值 | 默认 | 出处 |
| --- | --- | --- |
| `MenuItemWidth` | 80dp | `coui_slideview_menuitem_width` |
| `TextPadding` | 12dp | `coui_slide_view_text_padding` |
| `OverSlideDeleteDistance` | 48dp | `coui_slideview_over_slide_delete` |
| `QuickDeleteDistance` | 30dp | `coui_slideview_quick_delete` |
| `FlingVelocity` | 1000px/s | `COUISlideView.onTouchEvent` |
| `ReleaseSpring` | 阻尼 1，刚度 200 | `animationScrollTo` |
| `SmoothScrollEasing` | cubic-bezier(0.133, 0, 0.3, 1)，每像素 3ms，最长 200ms | `smoothScrollTo` |
| `DeleteSlideDurationMillis` / `DeleteCollapseDurationMillis` | 330ms / 400ms（延迟 10ms） | `COUISlideDeleteAnimation` |
| `slideViewColors()` | `colorScheme.error`、文字 `#F9F9F9`、图标白色 | `couiColorError`、`coui_slideview_textcolor` |
| `textStyle()` | 15sp，随字体缩放最多放大到 1.15 倍 | `coui_slide_view_text_size` |
