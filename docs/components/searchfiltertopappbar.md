# SearchFilterTopAppBar

`SearchFilterTopAppBar` is the search page's two-row bar, fixed to the roles it is named after: the search field on the first row, the filters for what is being searched on the second, both over one sheet of glass.

It is not a second `TwoRowTopAppBar`. [`TwoRowTopAppBar`](./tworowtopappbar) takes two arbitrary slots and paints both **inside** the material. That is right for the Notes bar, whose second row is a suggestion strip, and wrong for a search page, whose second row has to sit on the material instead of inside it. This component owns that ordering so a caller cannot get it wrong.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.SearchFilterTopAppBar
import io.github.suqi8.coui.kmp.basic.TwoRowTopAppBarDefaults
```

## Basic usage

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

Reserve the list's top inset with the same geometry the bar lays out, not by measuring it:

```kotlin
val barHeight = TwoRowTopAppBarDefaults.barHeight(
    firstRowHeight = TwoRowTopAppBarDefaults.FirstRowHeight,
    secondRowHeight = TwoRowTopAppBarDefaults.SecondRowHeight,
)
```

## Why the rows are drawn above the material

The bar records its own material into a private `LayerBackdrop` and then draws both rows **outside** that recording. The filters — and any glass inside the search row, through the `LocalBackdrop` the bar provides — sample the recording.

Skip the recording and the filters sample the page directly. A black video cover then passes under them at full contrast while the bar two pixels away has already been blurred and lifted by the 80% container colour: the chips read darker than their own surroundings, and the unselected wash (about 8% `ColorDodge` plus 8% `Luminosity`) is nowhere near enough to hide it. The chips look like a hole in the bar.

Draw the rows *inside* the recorded node and the chips are recorded into the layer they sample, feeding themselves back. Both mistakes are easy to make by eye and hard to diagnose from the symptom, which is why this component fixes the order internally.

The recorded material is paved with the page colour while `scrollFraction` is below 1, so a bar that is still fading in does not hand the filters a translucent picture.

## Geometry

| Parameter | Default | Meaning |
| :--- | :--- | :--- |
| `firstRowHeight` | `TwoRowTopAppBarDefaults.FirstRowHeight` (52dp) | The search row's height below the status bar |
| `filterRowHeight` | `TwoRowTopAppBarDefaults.SecondRowHeight` (32dp) | The filter row's height |
| `filterContentPadding` | `PaddingValues(horizontal = 16.dp)` | The padding before the first and after the last chip |

The gap between the rows (`SecondRowGap`, 11dp) and the room below the filter row (`SecondRowBottom`, 6dp) come from [`TwoRowTopAppBarDefaults`](./tworowtopappbar) and are not parameters.

The component reports the status bar plus the rows. The material's `extraHeight` fade band draws past that bottom edge without taking layout space, the same way [`AppBarBlurOverlay`](./appbarbluroverlay) does.

## Parameters

| Parameter | Default | Meaning |
| :--- | :--- | :--- |
| `filters` | — | The chips of the second row |
| `selectedFilter` | — | The selected chip's index |
| `onFilterSelected` | — | Called with the tapped chip's index |
| `searchRow` | — | The first row's content, centred in `firstRowHeight` |
| `leadingIcon` | `null` | An icon-only chip pinned before the filters, e.g. a sort menu. Its popup is the caller's to host |
| `onLeadingIconClick` | `{}` | Called when the leading chip is tapped |
| `containerColor` | `AppBarBlurOverlayDefaults.containerColor()` | The material's base colour |
| `backdrop` | `LocalBackdrop.current` | The page the material samples |
| `extraHeight` | `AppBarBlurOverlayDefaults.ExtraHeight` (8dp) | How far the material reaches past the bar |
| `scrollFraction` | `{ 1f }` | The material's alpha, read in the draw phase |
| `platformRenderer` | `false` | Whether the platform's own blur may be used. Leave it `false` here: the bar records its own material and the platform effect samples the whole window |
