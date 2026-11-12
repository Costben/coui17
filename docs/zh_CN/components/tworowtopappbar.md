# TwoRowTopAppBar

`TwoRowTopAppBar` 是一条材质跨一到两行的顶部应用栏，对标的正是 ColorOS 17 便签（`便签`）那一条：第一行搜索框，紧接一行分类胶囊，两行共用一整块连续的玻璃。

[`PinnedTopAppBar`](./pinnedtopappbar) 与 [`SmallTopAppBar`](./topappbar) 只接收标题字符串和一个可选的 `bottomContent`；本组件接收两行任意内容，并把行高钉死，所以几何在布局之前就能算出来。如果调用方自己在别的栏下面再叠一行，就会得到**两块**材质：给第二行单独配一个 `AppBarBlurOverlay` 会在栏自己的渐变带下面再叠一条渐变带，接缝看起来就是两行之间的一条暗带。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.TwoRowTopAppBar
import io.github.suqi8.coui.kmp.basic.TwoRowTopAppBarDefaults
```

## 基本用法

```kotlin
TwoRowTopAppBar(
    modifier = Modifier.fillMaxWidth(),
    firstRowHeight = 52.dp,
    secondRow = {
        ChipTabGroup(
            tabs = tabs,
            selectedIndex = selected,
            onSelectedChange = { selected = it },
            contentPadding = PaddingValues(horizontal = 16.dp),
            backdrop = LocalBackdrop.current,
        )
    },
) {
    TopAppBarSearchBar(
        query = query,
        onQueryChange = { query = it },
        expanded = true,
        onExpandedChange = {},
    )
}
```

只有一行时把 `secondRow` 留在 `null`——不要传一个空的 composable。栏高取决于这一行在不在：

```kotlin
val barHeight = TwoRowTopAppBarDefaults.barHeight(
    firstRowHeight = 52.dp,
    secondRowHeight = if (showChips) TwoRowTopAppBarDefaults.SecondRowHeight else null,
)
```

把这个值用作列表要给栏让出的内边距。改成去测量栏本身，会把列表的内边距和一次布局过程绑在一起。

## 一块材质，不是每行一块

这条栏就是一个 [`AppBarBlurOverlay`](./appbarbluroverlay)：两行都是画在它上面的内容，它的渐变在栏自己的下沿之前一直保持本体强度，之后才用 `AppBarBlurOverlayDefaults.ExtraHeight` 的带宽淡出。行里的控件一律不带自己的材质。`ChipTabGroup` 的胶囊和搜索框经 `LocalToolbarBackdropBlurFraction` 采样 backdrop——这个值由本栏驱动——所以它们的次级模糊会衰减到 `TOOLBAR_NESTED_BLUR_SCALE`，而不是叠成一圈过软的辉光。

状态栏内边距在材质内部处理，玻璃能一直盖到窗口顶端。组件报出的高度是状态栏加上这两行。

## 两行

| 参数 | 默认值 | 含义 |
| :--- | :--- | :--- |
| `firstRowHeight` | `TwoRowTopAppBarDefaults.FirstRowHeight`（52dp） | 第一行在状态栏下方的高度 |
| `secondRowHeight` | `TwoRowTopAppBarDefaults.SecondRowHeight`（32dp） | 胶囊行的高度 |
| `secondRow` | `null` | 胶囊行内容；单行栏传 `null` |

两行各自在自己的高度里居中。第二行与第一行之间隔 `TwoRowTopAppBarDefaults.SecondRowGap`（11dp），与栏的下沿之间隔 `TwoRowTopAppBarDefaults.SecondRowBottom`（6dp）。

## 默认值

| 常量 | 值 | 依据 |
| :--- | :--- | :--- |
| `FirstRowHeight` | 52dp | `TopAppBarDefaults.CollapsedHeight`，ColorOS 17 相册工具栏 |
| `SecondRowGap` | 11dp | 便签：搜索框到胶囊 16.6dp，减去搜索框在自己行里让出的下方 6dp |
| `SecondRowHeight` | 32dp | `ChipTabDefaults.Height` |
| `SecondRowBottom` | 6dp | 与搜索框在第一行里让出的下方内边距一致 |

以上数值均在参考机型上按 3.5 px/dp 实测。

## 参数

| 参数 | 默认值 | 说明 |
| :--- | :--- | :--- |
| `modifier` | `Modifier` | 作用于栏本身 |
| `secondRow` | `null` | 胶囊行内容，单行时为 `null` |
| `firstRowHeight` | `TwoRowTopAppBarDefaults.FirstRowHeight` | 第一行高度 |
| `secondRowHeight` | `TwoRowTopAppBarDefaults.SecondRowHeight` | 胶囊行高度 |
| `containerColor` | `AppBarBlurOverlayDefaults.containerColor()` | 材质的基色 |
| `backdrop` | `LocalBackdrop.current` | 材质采样的内容 |
| `extraHeight` | `AppBarBlurOverlayDefaults.ExtraHeight` | 材质越过栏下沿的距离 |
| `scrollFraction` | `{ 1f }` | 整体 alpha，在绘制阶段读取 |
| `platformRenderer` | `true` | 是否允许走平台自带的背景模糊 |
| `defaultWindowInsetsPadding` | `true` | 两行是否避让屏幕切口与导航条的水平内边距 |
| `firstRow` | — | 第一行内容，在 `firstRowHeight` 的行里居中 |

和任何 `AppBarBlurOverlay` 一样，这条栏要求 `LocalEdgeToEdge` 为 `true`——在 `Scaffold(edgeToEdge = true)` 里，或由调用方自己提供。没有它，材质会退化成包住这两行的空盒子。
