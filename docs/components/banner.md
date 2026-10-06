# Banner

`Banner` is the ColorOS 17 auto-scrolling card banner: a horizontal row of rounded cards with a page indicator underneath. The scrolling row and the indicator are two separate pieces.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.Banner
import io.github.suqi8.coui.kmp.basic.BannerDefaults
import io.github.suqi8.coui.kmp.basic.BannerIndicator
import io.github.suqi8.coui.kmp.basic.BannerIndicatorColors
```

## Basic Usage

```kotlin
Banner(
    pageCount = 4,
    autoScrollIntervalMillis = 3000L,
    modifier = Modifier
        .fillMaxWidth()
        .height(160.dp),
) { page ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageColors[page]),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Card $page", color = Color.White)
    }
}
```

The pager fills whatever height the banner leaves over, so give the banner a height. The indicator sits under the row, centred, with `BannerDefaults.IndicatorMarginBottom` (10dp) below it.

## The Auto-Scroll Interval Is Required

There is no auto-scroll in the reference implementation. Neither the scrolling row nor the indicator owns a timer, and the layout resource is unreferenced — so the advance interval is whatever the host app would have written around them. `autoScrollIntervalMillis` therefore has no default: any value would be invented.

Pass `autoScrollEnabled = false`, a `pageCount` of at most 1, or an interval of `<= 0` to switch the auto-scroll off. A drag in progress is left alone; the tick is skipped rather than fought.

## The Indicator Is Not `PageIndicator`

`BannerIndicator` is a standalone indicator and is deliberately not a wrapper around [PageIndicator](pageindicator). The two share their geometry — both read `coui_page_indicator_dot_size` (6dp) and `coui_page_indicator_dot_spacing` (3dp), so this port reuses `PageIndicatorDefaults.DotSize` and `PageIndicatorDefaults.DotSpacing` and keeps the same 12dp centre-to-centre pitch — but they differ in what they can express:

| | BannerIndicator | PageIndicator |
| --- | --- | --- |
| Dot count | One per page, no cap (`layoutWidth = dotStepDistance * count`) | A sliding window of at most 6 |
| Active marker | A trace pill whose two edges move separately, so it can span two dots | A recoloured dot |
| Edge dots | Full size | Shrunk to `DotSizeMedium` at the window edges |
| Clicks | Dots take a listener in the native | Display-only |

`BannerIndicator` can also be used on its own:

```kotlin
BannerIndicator(
    count = 5,
    currentIndex = page,
)
```

## Properties

### Banner

| Property Name            | Type                        | Description                                                       | Default Value                       | Required |
| ------------------------ | --------------------------- | ----------------------------------------------------------------- | ----------------------------------- | -------- |
| pageCount                | Int                         | Number of cards; must match `state`                               | -                                   | Yes      |
| autoScrollIntervalMillis | Long                        | How long a card stays before the banner advances, in ms           | -                                   | Yes      |
| modifier                 | Modifier                    | Modifier applied to the banner                                    | Modifier                            | No       |
| state                    | PagerState                  | Pager state                                                       | rememberPagerState over pageCount   | No       |
| autoScrollEnabled        | Boolean                     | Whether the banner advances by itself                             | true                                | No       |
| itemCornerRadius         | Dp                          | Corner radius of a card                                           | CardDefaults.CornerRadius (17dp)    | No       |
| itemColor                | Color                       | Fill of a card                                                    | COUITheme.colorScheme.surfaceContainer | No    |
| contentPadding           | Dp                          | Horizontal inset of the card row                                  | 24.dp                               | No       |
| itemSpacing              | Dp                          | Gap between two cards                                             | 8.dp                                | No       |
| indicatorMarginBottom    | Dp                          | Space under the indicator                                         | 10.dp                               | No       |
| indicatorColors          | BannerIndicatorColors       | Dot and trace colours of the indicator                            | BannerDefaults.bannerIndicatorColors() | No    |
| content                  | @Composable (Int) -> Unit   | The card at the given page                                        | -                                   | Yes      |

### BannerIndicator

| Property Name | Type                  | Description                                          | Default Value                          | Required |
| ------------- | --------------------- | ---------------------------------------------------- | -------------------------------------- | -------- |
| count         | Int                   | Number of pages; `count <= 0` draws nothing          | -                                      | Yes      |
| currentIndex  | Int                   | Index of the active page, clamped into `0 until count` | -                                    | Yes      |
| modifier      | Modifier              | Modifier applied to the indicator                    | Modifier                               | No       |
| colors        | BannerIndicatorColors | Dot and trace colours                                | BannerDefaults.bannerIndicatorColors() | No       |

### BannerIndicatorColors

| Property   | Type  | Description                                  |
| ---------- | ----- | -------------------------------------------- |
| dotColor   | Color | The inactive dots (`dotColor`)               |
| traceColor | Color | The trace pill over the active dot (`traceDotColor`) |

### BannerDefaults

| Constant                            | Type                        | Value     |
| ----------------------------------- | --------------------------- | --------- |
| ContentPadding                      | Dp                          | 24.dp     |
| ItemSpacing                         | Dp                          | 8.dp      |
| IndicatorMarginBottom               | Dp                          | 10.dp     |
| FlingFrictionScale                  | Float                       | 2.5f      |
| TraceAnimationDurationMillis        | Int                         | 240       |
| TraceAnimationEasing                | Easing                      | cubic-bezier(0.25, 0.1, 0.25, 1) |
| IndicatorDotColorOnDarkBackground   | Color                       | #4dffffff  |
| IndicatorTraceColorOnDarkBackground | Color                       | #e6ffffff  |

#### Methods

| Method Name              | Type                  | Description                                                       |
| ------------------------ | --------------------- | ----------------------------------------------------------------- |
| bannerIndicatorColors()  | BannerIndicatorColors | The colours the banner layout asks for, painted for a photo background |
| indicatorColors()        | BannerIndicatorColors | The themed pair for an ordinary surface |

## Colours

| COUI token                                      | Light / Dark          | Used for                       | Port                          |
| ----------------------------------------------- | --------------------- | ------------------------------ | ----------------------------- |
| color/coui_page_indicator_dot_color             | #4d000000 / #4dffffff | Inactive dots (class default)  | not used, see below           |
| color/coui_page_indicator_trace_dot_color       | #ff666666 / #ffababab | Trace pill (class default)     | not used, see below           |
| attr/couiColorHintNeutral → color/coui_color_hint_neutral | #42000000 / #4dffffff | Inactive dots (style) | COUITheme.colorScheme.disabledOnSurface |
| color/coui_color_label_primary                  | #e6000000 / #e6ffffff | Trace pill (style)             | COUITheme.colorScheme.onSurface |
| color/coui_page_indicator_dot_color_in_dark_bg  | #4dffffff             | Inactive dots (banner layout)  | IndicatorDotColorOnDarkBackground |
| color/coui_color_label_primary_dark             | #e6ffffff             | Trace pill (banner layout)     | IndicatorTraceColorOnDarkBackground |

`BannerDefaults.bannerIndicatorColors()` returns the last two, because the banner layout sets `dotColor` and `traceDotColor` explicitly and overrides the style — the banner paints its dots for a photo background, and neither `_in_dark_bg` resource has a night qualifier. `BannerDefaults.indicatorColors()` returns the themed pair for an indicator on an ordinary surface.

`coui_page_indicator_dot_color` and `coui_page_indicator_trace_dot_color` are the values the indicator's fields are initialised with, but every style overwrites both from the style attributes above, so neither is reachable.

## Geometry

The indicator is `layoutWidth` x `dotSize` (`onMeasure`), where `layoutWidth = dotStepDistance * count` and `dotStepDistance = dotSize + dotSpacing * 2` = 12dp. Position `p`'s trace rect is `finalRight = dotSpacing + dotSize + p * dotStepDistance` with `finalLeft = finalRight - dotSize`; in an RTL layout direction `finalRight = layoutWidth - (dotSpacing + p * dotStepDistance)`. Dots and trace share `dotCornerRadius = dotSize / 2`, so both are circles and the trace is a capsule.

The card row uses a horizontal `LinearLayoutManager`, item align `1`, native over-scroll on, fling friction at 2.5x the platform scroll friction, `setPadding(24dp, 0, 24dp, 0)` with `setClipToPadding(false)`, and a horizontal `DividerItemDecoration` whose `coui_item_decoration_8dp` drawable is an 8dp-wide transparent shape.

## Behavior

- The trace pill's two edges travel from where they were to `finalLeft` / `finalRight` over 240ms on the `PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f)` curve. In ColorOS that animator is written but never actually run — the shipped component snaps the pill into place — so the port animates it, which is what the dead code was written to do.
- The active dot is not recoloured. All dots are drawn in `dotColor` and the trace capsule is drawn over them in `traceColor`, with the trace drawn after the dot views.
- The native's update listener only ever moves the left edge, because the branches it guards on are never assigned; the port interpolates both edges.

## Not Ported

- The scrolling-row half of the layout: the layout merges the scrolling row, a `ViewPager2` and the indicator as siblings, so a host only ever shows one of the two scrolling views. This port keeps the pager.
- `setHorizontalItemAlign(1)` and native over-scroll, which are ColorOS recycler-view features with no Compose counterpart.
- The 2.5x fling friction: `BannerDefaults.FlingFrictionScale` records it, but `HorizontalPager` exposes no fling-friction knob.
- The dot click path: the native listener interface is empty and the click-handler body is empty, so it cannot deliver anything even though the dots take a click listener.
- The sticky bezier: its start and end positions are only ever set to a sentinel that clears the paths, so no code fills them.
- The dot stroke style: the style attribute defaults to false and no style turns it on.
- The accessibility description string, which the reference does not expose.
- The item styling itself: the native view leaves the item views to the host app, so the rounded card here is a port-side addition. It borrows `CardDefaults.CornerRadius` (17dp), the radius ColorOS 17's `Card` uses, rather than a banner-specific value, because ColorOS defines none.
