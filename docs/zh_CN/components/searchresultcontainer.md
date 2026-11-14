# SearchResultContainer

`SearchResultContainer` 把搜索页的「搜索首页 / 建议 ↔ 加载指示器 ↔ 搜索结果」过渡与顶栏筛选条收起联合成同一条状态机，可与 [`SearchFilterTopAppBar`](./searchfiltertopappbar) 配套使用。

## 引入

```kotlin
import io.github.suqi8.coui.kmp.basic.SearchResultContainer
import io.github.suqi8.coui.kmp.basic.SearchResultContainerDefaults
import io.github.suqi8.coui.kmp.basic.rememberSearchResultsTransitionState
```

## 基本用法

```kotlin
val transitionState = rememberSearchResultsTransitionState(
    showingResults = showingResults,
    loading = isLoading,
    queryKey = submittedQuery,
)

SearchFilterTopAppBar(
    filters = if (transitionState.filtersVisible) tabs else null,
    selectedFilter = selectedTab,
    onFilterSelected = onTabSelect,
    searchRow = { /* TopAppBarSearchBar */ },
)

SearchResultContainer(
    state = transitionState,
    modifier = Modifier.fillMaxSize(),
    loadingTopPadding = barHeight,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (!shownResults) {
            // 搜索历史 / 热搜 / 搜索建议
        } else if (resultsVisible) {
            // 搜索结果列表项
        }
    }
}
```

## 联动行为

1. **提交搜索**：先将搜索首页或建议页淡出（`90ms`，`COUIInEase`）。若结果在淡出加 `loadingEnterDelayMillis`（`50ms`）内返回，直接跳过加载球；否则加载球柔和淡入并伴随轻缩放上托（`160ms`，`COUIOutEase`）。
2. **结果到达**：在把偏移复位到起点并归零透明度之后才打开 `resultsVisible`（避免首帧全不透明闪现），同时加载球向上淡出收走（`120ms`，`COUIInEase`），结果列表从下方 `48dp` 处上浮落定（`360ms`，`COUIMoveEase`）。
3. **结果态内切换分类或刷新**：`queryKey` 未变时改用较轻的 `20dp` / `260ms` 上浮。
4. **退回搜索首页**：结果与加载球先淡出（`120ms`，`COUIInEase`），`filtersVisible` 等淡出结束才收起以同步顶栏高度，随后切换内容并淡入搜索首页（`200ms`，`COUIOutEase`）。
5. **仅在出现加载时上浮**：通过 `SearchResultContainerDefaults.transitionSpec(riseTrigger = SearchResultsRiseTrigger.OnlyAfterLoading)`，让已缓存或无需转圈的内容直接落位，只有实际出现过加载球时才在结束后上浮。
