# SearchFilterTopAppBar

`SearchFilterTopAppBar` 是搜索结果页的两行顶栏，行和角色都钉死：第一行搜索框，第二行筛选「搜索什么」的胶囊，两行共用一整块玻璃。

它不是第二个 [`TwoRowTopAppBar`](./tworowtopappbar)。那个组件接收两个任意槽位，两行都画在材质**里面**——这对便签那种第二行是搜索建议的栏是对的，但对搜索页是错的：搜索页的第二行要坐**在材质之上**，而不是进到材质里面。本组件把这层顺序封在内部，调用方想出错都难。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.SearchFilterTopAppBar
import io.github.suqi8.coui.kmp.basic.TwoRowTopAppBarDefaults
```

## 基本用法

```kotlin
SearchFilterTopAppBar(
    filters = listOf(ChipTabData("视频"), ChipTabData("用户"), ChipTabData("专栏")),
    selectedFilter = selected,
    onFilterSelected = { selected = it },
    modifier = Modifier.fillMaxWidth(),
    leadingIcon = COUIIcons.Sort,
    searchRow = {
        TopAppBarSearchBar(
            query = query,
            onQueryChange = { query = it },
            expanded = true,
            onExpandedChange = {},
            modifier = Modifier.fillMaxWidth(),
            placeholder = "搜索视频 / 用户 / 专栏",
            cancelText = "取消",
        )
    },
)
```

给列表让出的上内边距，用同一套几何算，不要去测量栏本身：

```kotlin
val barHeight = TwoRowTopAppBarDefaults.barHeight(
    firstRowHeight = TwoRowTopAppBarDefaults.FirstRowHeight,
    secondRowHeight = TwoRowTopAppBarDefaults.SecondRowHeight,
)
```

## 为什么两行要画在材质之上

这条栏先把自己那层材质录进一个私有的 `LayerBackdrop`，再把两行画在录制**之外**。胶囊——以及搜索行里任何带玻璃的控件（通过本栏提供的 `LocalBackdrop`）——采样的都是这份录制。

省掉录制，胶囊就直接采样页面。这时一张黑色视频封面从底下经过，胶囊处是全对比度的黑，而两像素之外的栏已经被模糊过、又被 80% 容器色提亮：胶囊比它自己的周围更暗，而未选中的那层洗色（约 8% `ColorDodge` 加 8% `Luminosity`）根本压不住。看上去就像栏上破了个洞。

把两行画进录制节点里则是另一种错：胶囊被录进它自己采样的那一层，形成自采样反馈。两个错误肉眼都很难发现，但从症状倒推都很费劲，所以本组件把顺序封在内部。

`scrollFraction` 小于 1 时，录制层会先铺一层页面底色，这样还在淡入的栏不会交给胶囊一张半透明的画面。

## 几何

| 参数 | 默认值 | 含义 |
| :--- | :--- | :--- |
| `firstRowHeight` | `TwoRowTopAppBarDefaults.FirstRowHeight`（52dp） | 搜索行在状态栏下方的高度 |
| `filterRowHeight` | `TwoRowTopAppBarDefaults.SecondRowHeight`（32dp） | 筛选行的高度 |
| `filterContentPadding` | `PaddingValues(horizontal = 16.dp)` | 第一个胶囊之前与最后一个之后的留白 |

两行之间的间距（`SecondRowGap`，11dp）和筛选行下方的留白（`SecondRowBottom`，6dp）来自 [`TwoRowTopAppBarDefaults`](./tworowtopappbar)，不是参数。

组件报出的高度是状态栏加上两行。材质那条 `extraHeight` 的渐隐带画在这个下沿之外，不占布局——和 [`AppBarBlurOverlay`](./appbarbluroverlay) 一样。

## 参数

| 参数 | 默认值 | 含义 |
| :--- | :--- | :--- |
| `filters` | — | 第二行的胶囊 |
| `selectedFilter` | — | 选中胶囊的下标 |
| `onFilterSelected` | — | 点击胶囊时回调其下标 |
| `searchRow` | — | 第一行内容，在 `firstRowHeight` 里居中 |
| `leadingIcon` | `null` | 钉在胶囊之前的图标胶囊，例如排序菜单；它弹出的菜单由调用方自己托管 |
| `onLeadingIconClick` | `{}` | 点击图标胶囊时回调 |
| `containerColor` | `AppBarBlurOverlayDefaults.containerColor()` | 材质底色 |
| `backdrop` | `LocalBackdrop.current` | 材质采样的页面 |
| `extraHeight` | `AppBarBlurOverlayDefaults.ExtraHeight`（8dp） | 材质越过栏底边的距离 |
| `scrollFraction` | `{ 1f }` | 材质的整体透明度，在绘制阶段读取 |
| `platformRenderer` | `false` | 是否允许用平台自己的模糊。这里保持 `false`：栏自己录了材质，平台效果采的是整个窗口 |
