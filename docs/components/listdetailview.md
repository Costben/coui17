# ListDetailView

`ListDetailView` is a basic layout component in COUI: a responsive list-detail container that shows one pane at a time on a narrow window and splits into a two-pane list + detail layout once the window is wide enough. It is aligned with ColorOS's list-detail layout.

The device class holds three `FragmentContainerView`s — main, sub and emptyPage — plus a hairline divider, and lays them out by width in `onMeasure` / `onLayout`. Because Fragments are an Android host concern, the Compose port takes composable slots instead: `mainContent`, `detailContent` and `emptyPageContent`. While the window is narrower than the COUI `WindowTotalSizeClass.Compact` boundary (600dp) only one of `mainContent` / `detailContent` is composed, chosen by `detailVisible`; from 600dp on the main pane takes a fraction of the width, a divider follows, and the detail (or `emptyPageContent` when nothing is selected) fills the rest.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.ListDetailView
import io.github.suqi8.coui.kmp.basic.ListDetailViewDefaults
```

## Basic Usage

```kotlin
var selected by remember { mutableStateOf<Item?>(null) }

ListDetailView(
    detailVisible = selected != null,
    mainContent = { ItemList(onSelect = { selected = it }) },
    detailContent = { selected?.let { ItemDetail(it) } },
    emptyPageContent = { Text("Select an item") }
)
```

## Properties

| Property Name      | Type                 | Description                                                                 | Default Value                     | Required |
| ------------------ | -------------------- | --------------------------------------------------------------------------- | --------------------------------- | -------- |
| mainContent        | @Composable () -> Unit | The list pane, on the leading edge in split mode                           | -                                 | Yes      |
| detailContent      | @Composable () -> Unit | The detail pane                                                            | -                                 | Yes      |
| detailVisible      | Boolean              | Whether the detail pane is selected                                         | -                                 | Yes      |
| modifier           | Modifier             | Modifier applied to the container                                           | Modifier                          | No       |
| emptyPageContent   | @Composable () -> Unit | Placeholder shown in split mode while nothing is selected                  | {}                                | No       |
| mainFragmentPercent| Float                | The main pane's share of the width, clamped by min / max                    | ListDetailViewDefaults.MainFragmentPercent | No |
| minMainWidth       | Dp                   | Floor of the main pane width                                                | ListDetailViewDefaults.MinMainWidth | No     |
| maxMainWidth       | Dp                   | Ceiling of the main pane width                                              | ListDetailViewDefaults.MaxMainWidth | No     |
| dividerWidth       | Dp                   | Width of the hairline between the panes                                     | ListDetailViewDefaults.DividerWidth | No     |
| dividerColor       | Color                | Color of the hairline                                                       | COUITheme.colorScheme.dividerLine | No       |

## ListDetailViewDefaults Object

| Property Name      | Type    | Description                              | Default Value |
| ------------------ | ------- | ---------------------------------------- | ------------- |
| MainFragmentPercent| Float   | Main pane's width share                  | 0.4f          |
| MinMainWidth       | Dp      | Floor of the main pane width             | 280.dp        |
| MaxMainWidth       | Dp      | Ceiling of the main pane width           | 412.dp        |
| DividerWidth       | Dp      | Width of the panes' hairline divider     | 0.33000004.dp |
| SplitModeMinWidth  | Dp      | Width from which the two-pane split appears | 600.dp     |

`ListDetailViewDefaults.isSplitMode(width)` reports whether a window of the given width gets the two-pane split.

## Behaviour and Tokens

- **Split threshold.** The device asks `WindowSizeClass.calculateFromSize(...).getWindowTotalSizeClass() != Compact`; `Compact` is width below `Breakpoints.BP_MEDIUM_WINDOW_BASE_WIDTH` (600dp), so the split begins at 600dp.
- **Main pane width.** `min(max(width * percent, maxMainWidth), minMainWidth)` — `COUIListDetailView.DEFAULT_MAIN_WIDTH_PERCENT = 0.4f`, clamped by `coui_main_fragment_min_width` (280dp) and `coui_main_fragment_max_width` (412dp).
- **Divider.** `coui_fragment_gap_width` = 0.33000004dp, coloured by `?attr/couiColorDivider` (`coui_color_divider`).
- **Compact behaviour.** The device measures main and sub at the full width, hides the empty page (`View.GONE`) and leaves the visible fragment to its host; the Compose port exposes that choice as `detailVisible`.
