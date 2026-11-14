# SearchResultContainer

`SearchResultContainer` coordinates the search page's content transitions across prompt/suggestions, loading indicator, and results, and synchronizes with a companion [`SearchFilterTopAppBar`](./searchfiltertopappbar).

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.SearchResultContainer
import io.github.suqi8.coui.kmp.basic.SearchResultContainerDefaults
import io.github.suqi8.coui.kmp.basic.rememberSearchResultsTransitionState
```

## Basic usage

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
            // Search history / hot words / suggestions
        } else if (resultsVisible) {
            // Search result items
        }
    }
}
```

## Coordinated motion

1. **Submitting a query**: fades the prompt or suggestions out (`90ms`, `COUIInEase`). If data arrives within the fade-out plus `loadingEnterDelayMillis` (`50ms`), the loading spinner is skipped completely. Otherwise, the spinner fades and scales in gently (`160ms`, `COUIOutEase`).
2. **Results arrival**: lifts and dissolves the loading spinner (`120ms`, `COUIInEase`) at the exact frame `resultsVisible` becomes `true` with `riseProgress` already snapped to `0f` (eliminating a one-frame full-alpha flash), while results float up `48dp` over `360ms` (`COUIMoveEase`).
3. **Refreshing or switching filters inside results**: uses a lighter `20dp` rise over `260ms` (`RefreshRiseDistance` / `RefreshRiseMillis`) when `queryKey` is unchanged.
4. **Returning to prompt/history**: fades results (and any active spinner) out over `120ms` (`COUIInEase`), keeps `filtersVisible` true until the fade finishes so `SearchFilterTopAppBar` collapses together with the list, then swaps content and fades the prompt back in (`200ms`, `COUIOutEase`).
5. **Loading-only pages**: pass `riseTrigger = SearchResultsRiseTrigger.OnlyAfterLoading` via `SearchResultContainerDefaults.transitionSpec(...)` when a page should rise only if the loading spinner actually became visible, showing cached or already-loaded data immediately without animating.
