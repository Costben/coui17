# TwoRowTopAppBar

`TwoRowTopAppBar` is an app bar whose material spans one or two rows, the way the ColorOS 17 Notes bar (`便签`) does: a search field on the first row, a row of chips right below it, all over one continuous sheet of glass.

[`PinnedTopAppBar`](./pinnedtopappbar) and [`SmallTopAppBar`](./topappbar) take a title string and an optional `bottomContent`; this bar takes two arbitrary rows and pins their heights, so the geometry is known before the layout pass. A caller that stacks its own rows under an existing bar instead ends up with **two** materials: matching the first row's glass with a second `AppBarBlurOverlay` adds a second gradient band under the bar's own, and the seam reads as a dark strip between the rows.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.TwoRowTopAppBar
import io.github.suqi8.coui.kmp.basic.TwoRowTopAppBarDefaults
```

## Basic usage

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

Leave `secondRow` at `null` — do not pass an empty composable — when the bar has only one row. The height depends on whether the row is present:

```kotlin
val barHeight = TwoRowTopAppBarDefaults.barHeight(
    firstRowHeight = 52.dp,
    secondRowHeight = if (showChips) TwoRowTopAppBarDefaults.SecondRowHeight else null,
)
```

Use that value for the inset a list has to reserve under the bar. Measuring the bar instead couples the list's padding to a layout pass.

## One material, not one per row

The bar is a single [`AppBarBlurOverlay`](./appbarbluroverlay): both rows are content drawn on top of it, and its gradient keeps its body strength down to the bar's own bottom edge before fading out over `AppBarBlurOverlayDefaults.ExtraHeight`. Controls inside the rows never carry a material of their own. A `ChipTabGroup`'s chips and a search field sample the backdrop through `LocalToolbarBackdropBlurFraction`, which this bar drives, so their secondary blur attenuates to `TOOLBAR_NESTED_BLUR_SCALE` instead of compounding into an overly soft halo.

The status bar inset is applied inside the material, so the glass reaches the top of the window. The reported size is the status bar plus the rows.

## Rows

| Parameter | Default | Meaning |
| :--- | :--- | :--- |
| `firstRowHeight` | `TwoRowTopAppBarDefaults.FirstRowHeight` (52dp) | First row's height below the status bar |
| `secondRowHeight` | `TwoRowTopAppBarDefaults.SecondRowHeight` (32dp) | Chip row's height |
| `secondRow` | `null` | Chip row content; `null` for a single-row bar |

Both rows are centred in their own height. The second row is inset from the first by `TwoRowTopAppBarDefaults.SecondRowGap` (11dp) and from the bar's bottom edge by `TwoRowTopAppBarDefaults.SecondRowBottom` (6dp).

## Defaults

| Constant | Value | Source |
| :--- | :--- | :--- |
| `FirstRowHeight` | 52dp | `TopAppBarDefaults.CollapsedHeight`, the ColorOS 17 Gallery toolbar |
| `SecondRowGap` | 11dp | Notes: 16.6dp from the search field to the chips, minus the 6dp the field sits above the first row's bottom edge |
| `SecondRowHeight` | 32dp | `ChipTabDefaults.Height` |
| `SecondRowBottom` | 6dp | The same inset the search field leaves inside the first row |

All values were measured on the reference device at 3.5 px/dp.

## Parameters

| Parameter | Default | Description |
| :--- | :--- | :--- |
| `modifier` | `Modifier` | Applied to the bar |
| `secondRow` | `null` | Chip row content, or `null` for a first row only |
| `firstRowHeight` | `TwoRowTopAppBarDefaults.FirstRowHeight` | First row's height |
| `secondRowHeight` | `TwoRowTopAppBarDefaults.SecondRowHeight` | Chip row's height |
| `containerColor` | `AppBarBlurOverlayDefaults.containerColor()` | Base colour of the material |
| `backdrop` | `LocalBackdrop.current` | Content the material samples |
| `extraHeight` | `AppBarBlurOverlayDefaults.ExtraHeight` | How far the material reaches past the bar's bottom edge |
| `scrollFraction` | `{ 1f }` | Overall alpha, read in the draw phase |
| `platformRenderer` | `true` | Whether the platform's own backdrop blur may be used |
| `defaultWindowInsetsPadding` | `true` | Inset the rows from the display cutout and the navigation bars on the horizontal sides |
| `firstRow` | — | First row content, centred in a `firstRowHeight` tall row |

The bar needs `LocalEdgeToEdge` to be `true` — inside `Scaffold(edgeToEdge = true)`, or provided by the caller — the way any `AppBarBlurOverlay` does. Without it the material degrades to a plain box around the rows.
