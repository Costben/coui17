# Banner

`Banner` is the ColorOS 17 auto-scrolling card banner, the port of `res/layout/coui_banner_content_layout.xml` of the Clock APK: a horizontal row of rounded cards with a page indicator underneath. It is built from two classes, `COUIBannerRecyclerView` (`com.coui.appcompat.banner`) for the scrolling row and `COUIPageIndicatorKit` (same package) for the indicator.

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

There is no auto-scroll anywhere in the native. Neither `COUIBannerRecyclerView` nor `COUIPageIndicatorKit` owns a timer, and no class in the Clock dex references either of them — `coui_banner_content_layout` is an unreferenced resource of the APK — so the advance interval is whatever the host app would have written around them. `autoScrollIntervalMillis` therefore has no default: any value would be invented.

Pass `autoScrollEnabled = false`, a `pageCount` of at most 1, or an interval of `<= 0` to switch the auto-scroll off. A drag in progress is left alone; the tick is skipped rather than fought.

## The Indicator Is Not `PageIndicator`

`BannerIndicator` ports `COUIPageIndicatorKit` and is deliberately not a wrapper around [PageIndicator](pageindicator). The two share their geometry — both read `coui_page_indicator_dot_size` (6dp) and `coui_page_indicator_dot_spacing` (3dp), so this port reuses `PageIndicatorDefaults.DotSize` and `PageIndicatorDefaults.DotSpacing` and keeps the same 12dp centre-to-centre pitch — but they differ in what they can express:

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

| Constant                            | Type                        | Value     | COUI Source                                                     |
| ----------------------------------- | --------------------------- | --------- | --------------------------------------------------------------- |
| ContentPadding                      | Dp                          | 24.dp     | COUIBannerRecyclerView.c setPadding(24, 0, 24, 0)               |
| ItemSpacing                         | Dp                          | 8.dp      | coui_item_decoration_8dp                                        |
| IndicatorMarginBottom               | Dp                          | 10.dp     | layout_marginBottom on COUIPageIndicatorKit                     |
| FlingFrictionScale                  | Float                       | 2.5f      | setHorizontalFlingFriction(getScrollFriction() * 2.5f)          |
| TraceAnimationDurationMillis        | Int                         | 240       | traceAnimator.setDuration(240L)                                 |
| TraceAnimationEasing                | Easing                      | cubic-bezier(0.25, 0.1, 0.25, 1) | PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f)     |
| IndicatorDotColorOnDarkBackground   | Color                       | #4dffffff  | color/coui_page_indicator_dot_color_in_dark_bg                  |
| IndicatorTraceColorOnDarkBackground | Color                       | #e6ffffff  | color/coui_color_label_primary_dark                             |

#### Methods

| Method Name              | Type                  | Description                                                       |
| ------------------------ | --------------------- | ----------------------------------------------------------------- |
| bannerIndicatorColors()  | BannerIndicatorColors | The colours the banner layout asks for, painted for a photo background |
| indicatorColors()        | BannerIndicatorColors | `Widget.COUI.COUIPageIndicator`'s themed pair, for an ordinary surface |

## Colours

| COUI token                                      | Light / Dark          | Used for                       | Port                          |
| ----------------------------------------------- | --------------------- | ------------------------------ | ----------------------------- |
| color/coui_page_indicator_dot_color             | #4d000000 / #4dffffff | Inactive dots (class default)  | not used, see below           |
| color/coui_page_indicator_trace_dot_color       | #ff666666 / #ffababab | Trace pill (class default)     | not used, see below           |
| attr/couiColorHintNeutral → color/coui_color_hint_neutral | #42000000 / #4dffffff | Inactive dots (style) | COUITheme.colorScheme.disabledOnSurface |
| color/coui_color_label_primary                  | #e6000000 / #e6ffffff | Trace pill (style)             | COUITheme.colorScheme.onSurface |
| color/coui_page_indicator_dot_color_in_dark_bg  | #4dffffff             | Inactive dots (banner layout)  | IndicatorDotColorOnDarkBackground |
| color/coui_color_label_primary_dark             | #e6ffffff             | Trace pill (banner layout)     | IndicatorTraceColorOnDarkBackground |

`BannerDefaults.bannerIndicatorColors()` returns the last two, because `coui_banner_content_layout` sets `dotColor` and `traceDotColor` explicitly and overrides the style — the Clock banner paints its dots for a photo background, and neither `_in_dark_bg` resource has a night qualifier. `BannerDefaults.indicatorColors()` returns the themed pair for an indicator on an ordinary surface.

`coui_page_indicator_dot_color` and `coui_page_indicator_trace_dot_color` are the values the `COUIPageIndicatorKit` fields are initialised with, but every style in the APK overwrites both from the style attributes above, so neither is reachable.

## Geometry

The indicator is `layoutWidth` x `dotSize` (`onMeasure`), where `layoutWidth = dotStepDistance * count` and `dotStepDistance = dotSize + dotSpacing * 2` = 12dp. Position `p`'s trace rect is `finalRight = dotSpacing + dotSize + p * dotStepDistance` with `finalLeft = finalRight - dotSize`; in an RTL layout direction `finalRight = layoutWidth - (dotSpacing + p * dotStepDistance)`. Dots and trace share `dotCornerRadius = dotSize / 2`, so both are circles and the trace is a capsule.

The card row is configured by `COUIBannerRecyclerView.c()`: a horizontal `LinearLayoutManager`, `setHorizontalItemAlign(1)`, native over-scroll on, `setHorizontalFlingFriction` at 2.5x the platform scroll friction, `setPadding(24dp, 0, 24dp, 0)` with `setClipToPadding(false)`, and a horizontal `DividerItemDecoration` whose `coui_item_decoration_8dp` drawable is an 8dp-wide transparent shape.

## Behavior

- The trace pill's two edges travel from where they were to `finalLeft` / `finalRight` over 240ms on the `PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f)` curve, ported from `COUIPageIndicatorKit.c(ValueAnimator)`. The native never actually runs that animator — `B()` is the only caller of `traceAnimator.start()`, it is reached only from the handler's `MSG_START_TRACE_ANIMATION` (17), and nothing in the class ever sends that message — so the shipped component snaps the pill into place through `A(position)`. The port animates it, which is what the dead code was written to do.
- The active dot is not recoloured. All dots are drawn in `dotColor` and the trace capsule is drawn over them in `traceColor`, exactly as `dispatchDraw` draws `traceRect` after the dot views.
- The native's update listener only ever moves the left edge, because the `needSettlePositionTemp` and `tranceCutTailRight` branches it guards on are never assigned; the port interpolates both edges.

## Not Ported

- The `COUIBannerRecyclerView` half of the layout. `coui_banner_content_layout` is a `merge` of `COUIBannerRecyclerView`, `ViewPager2` and `COUIPageIndicatorKit`, all three as siblings, so the Clock app only ever shows one of the two scrolling views. This port keeps the pager.
- `setHorizontalItemAlign(1)` and `setIsUseNativeOverScroll(true)`, which are `COUIRecyclerView` features with no Compose counterpart.
- The 2.5x fling friction: `BannerDefaults.FlingFrictionScale` records it, but `HorizontalPager` exposes no fling-friction knob.
- `COUIPageIndicatorKit.setIsClickable` / `setOnDotClickListener`: the listener interface is empty (`COUIPageIndicatorKit.e` declares no members) and the click handler's body is empty, so the dot click path cannot deliver anything even though the dots take a click listener.
- The sticky bezier of `mPortStickyPath` / `mDepartStickyPath`: `mPortPosition` and `mDepartPosition` are only ever set to `MIS_POSITION` by `w()`, which clears the paths, so no code fills them.
- `dotIsStrokeStyle` and `coui_page_indicator_dot_stroke`: the style attribute defaults to false and no style in the APK turns it on.
- The accessibility description built from `coui_page_indicator_description`, whose string the report does not list.
- The item styling itself: the native RecyclerView leaves the item views to the host app, so the rounded card here is a port-side addition. It borrows `CardDefaults.CornerRadius` (17dp), the radius ColorOS 17's `Card` uses, rather than a banner-specific value, because the native defines none.
