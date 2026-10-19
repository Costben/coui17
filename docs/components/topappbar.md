# TopAppBar

`TopAppBar` is a top application bar component in COUI, used to provide navigation, title, and action buttons at the top of the interface. It supports both large title and regular modes, as well as dynamic effects during scrolling.

This component is typically used in conjunction with the `Scaffold` component to maintain consistent layout and behavior across different pages in the application.

<div style="position: relative; height: 300px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../compose/index.html?id=topAppBar" title="Demo" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin"></iframe>
</div>

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.TopAppBar
import io.github.suqi8.coui.kmp.basic.SmallTopAppBar
import io.github.suqi8.coui.kmp.basic.COUIScrollBehavior
import io.github.suqi8.coui.kmp.basic.rememberTopAppBarState
```

## Basic Usage

### Small TopAppBar

```kotlin
Scaffold(
    topBar = {
        SmallTopAppBar(
            title = "Title",
            navigationIcon = {
                IconButton(onClick = { /* Handle click event */ }) {
                    Icon(COUIIcons.Back, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { /* Handle click event */ }) {
                    Icon(COUIIcons.More, contentDescription = "More")
                }
            }
        )
    }
)
```

### Large TopAppBar

```kotlin
Scaffold(
    topBar = {
        TopAppBar(
            title = "Title",
            largeTitle = "Large Title", // If not specified, title value will be used
            navigationIcon = {
                IconButton(onClick = { /* Handle click event */ }) {
                    Icon(COUIIcons.Back, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { /* Handle click event */ }) {
                    Icon(COUIIcons.More, contentDescription = "More")
                }
            }
        )
    }
)
```

### With Subtitle

Both bars can show a subtitle below the title. On the collapsible `TopAppBar` the subtitle
fades out while the bar collapses by default; set `hideSubtitleOnCollapse = false` to keep
it visible and slide it into the collapsed bar below the title:

```kotlin
TopAppBar(
    title = "Title",
    largeTitle = "Large Title",
    subtitle = "Subtitle",
    hideSubtitleOnCollapse = false
)
```

## Large TopAppBar Scroll Behavior (Using Scaffold)

TopAppBar supports changing its display state when content scrolls:

```kotlin
val scrollBehavior = COUIScrollBehavior()

Scaffold(
    topBar = {
        TopAppBar(
            title = "Title",
            largeTitle = "Large Title", // If not specified, title value will be used
            scrollBehavior = scrollBehavior
        )
    }
) { paddingValues ->
    // Content area needs to consider padding
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            // If you want to add the overscroll effect, please add it before the scroll behavior
            .overScrollVertical()
            // Bind TopAppBar scroll behavior
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(top = paddingValues.calculateTopPadding())
    ) {
        // List content
    }
}
```

## Custom Styles

### Custom Colors

```kotlin
TopAppBar(
    title = "Title",
    color = COUITheme.colorScheme.primary,
    titleColor = COUITheme.colorScheme.onPrimary,
    largeTitleColor = COUITheme.colorScheme.onPrimary
)
```

### Custom Content Padding

```kotlin
TopAppBar(
    title = "Title",
    titlePadding = 32.dp
)
```

### Custom Icon Padding

```kotlin
TopAppBar(
    title = "Title",
    navigationIconPadding = 12.dp,
    actionIconPadding = 12.dp
)
```

## Centred Title and Segment Slot

`SmallTopAppBar` also covers the two COUI toolbar forms that are not about the collapsible title:
the centred title and the embedded segment slot. Both are `SmallTopAppBar`-only — COUI has them on
the plain `COUIToolbar`, and there is no evidence of a collapsible-bar equivalent.

### Centred Title

Set `centerTitle = true` to centre the title (and the subtitle) between the navigation icon and the
actions, matching COUI's `setIsTitleCenterStyle`. In this form the bar reserves `centerTitleMargin`
at each end in place of the usual `titlePadding` / `navigationIconPadding` / `actionIconPadding`,
and that margin follows COUI's width tiers: 16dp below 600dp, 24dp from 600dp to 840dp, and 40dp
from 840dp up.

```kotlin
SmallTopAppBar(
    title = "Title",
    centerTitle = true,
    navigationIcon = { /* ... */ },
    actions = { /* ... */ }
)
```

Pass `centerTitleMargin` explicitly to pin the margin instead of following the window width.

### Segment Slot

`segmentButton` slots a segmented control between the navigation icon and the title, matching
COUI's `setSegmentButtons(View)`. The slot is 32dp tall, at least 108dp and at most 207dp wide,
shrinks when the title needs the room, and is not laid out at all while `centerTitle` is set —
the two forms are mutually exclusive.

```kotlin
SmallTopAppBar(
    title = "Title",
    segmentButton = {
        Row(Modifier.padding(4.dp)) { /* segment buttons */ }
    }
)
```

### Gradient Glass

`gradientGlass = true` switches the app bar material from the uniform blur to COUI's gradient
shape. The material is not painted on the bar itself: it is a material layer measured to the bar
plus `AppBarBlurOverlayDefaults.ExtraHeight` (24dp) and drawn behind the bar content, while the
layout reports only the bar's height, so the material reaches past the bar's bottom edge without
changing the space the bar takes. The blur is strongest at the top edge, and the bar's bottom edge
keeps some blur and surface alpha that fade out over that extra band. The divider is suppressed
while the material is on, so it cannot cut across the fade.

With a `scrollBehavior` the material is invisible at rest and reaches full strength after 30dp of
content has scrolled under the bar. Without a `scrollBehavior` it is always on.

It needs the same `LocalBackdrop` as the uniform material; without one — or without runtime shader
support — the bar falls back to the gradient colour fill alone.

```kotlin
CompositionLocalProvider(LocalBackdrop provides backdrop) {
    SmallTopAppBar(title = "Title", gradientGlass = true)
}
```

### Gradient Glass Overlay

`gradientGlass` lays the material out behind the bar itself. When the page puts the material on a separate node that it lays out itself — the ColorOS 17 home page inflates `AppBarBlurOverlayView` as a sibling of the app bar and hollows the bar out — use [`AppBarBlurOverlay`](../components/appbarbluroverlay) instead. The two share the same measure behaviour and the same `AppBarGlassDefaults` blur radius and blend curve; the difference is who lays the node out, and where the alpha comes from: `AppBarBlurOverlay` takes it from its `scrollFraction`, while `gradientGlass` derives it from the bar's `scrollBehavior`.

## Properties

### TopAppBar Properties

| Property Name              | Type                            | Description                                    | Default Value                   | Required |
| -------------------------- | ------------------------------- | ---------------------------------------------- | ------------------------------- | -------- |
| title                      | String                          | Top bar title                                  | -                               | Yes      |
| modifier                   | Modifier                        | Modifier applied to the top bar                | Modifier                        | No       |
| color                      | Color                           | Top bar background color                       | COUITheme.colorScheme.surface  | No       |
| titleColor                 | Color                           | Color of the collapsed small title text        | COUITheme.colorScheme.onSurface | No       |
| largeTitle                 | String                          | Large title text                               | title                           | No       |
| largeTitleColor            | Color                           | Color of the expanded large title text         | COUITheme.colorScheme.onSurface | No       |
| subtitle                   | String                          | Subtitle text displayed below the title bar    | ""                              | No       |
| subtitleColor              | Color                           | Color of the subtitle text                     | COUITheme.colorScheme.onSurfaceVariantSummary | No       |
| dividerColor               | Color                           | Color of the bottom hairline divider revealed while collapsing | COUITheme.colorScheme.dividerLine | No       |
| navigationIcon             | @Composable () -> Unit          | Composable function for navigation icon area   | {}                              | No       |
| actions                    | @Composable RowScope.() -> Unit | Composable function for action buttons area (24dp icons recommended) | {}                              | No       |
| scrollBehavior             | ScrollBehavior?                 | Controls top bar scroll behavior               | null                            | No       |
| defaultWindowInsetsPadding | Boolean                         | Whether to apply default window insets padding | true                            | No       |
| showDivider                | Boolean                         | Whether to draw the bottom hairline divider that fades in as the bar collapses | true | No       |
| hideSubtitleOnCollapse     | Boolean                         | Whether the subtitle fades out while the bar collapses; when false it stays opaque and slides into the collapsed bar below the title | true | No       |
| titlePadding          | Dp                              | Horizontal content padding                     | TopAppBarDefaults.TitlePadding | No       |
| navigationIconPadding      | Dp                              | Start padding of the navigation icon           | TopAppBarDefaults.NavigationIconPadding | No       |
| actionIconPadding          | Dp                              | End padding of the action icons                | TopAppBarDefaults.ActionIconPadding | No       |
| gradientGlass              | Boolean                         | Paint the ColorOS gradient-blur app bar material instead of the uniform one; it reaches `AppBarBlurOverlayDefaults.ExtraHeight` (24dp) past the bar's bottom edge while the bar keeps its height, and the divider is hidden. With `scrollBehavior` it appears after 30dp of scroll (see Gradient Glass) | false | No |
| bottomContent              | @Composable () -> Unit          | Composable content displayed below the title bar area | {} | No |

### SmallTopAppBar Properties

| Property Name              | Type                            | Description                                    | Default Value                   | Required |
| -------------------------- | ------------------------------- | ---------------------------------------------- | ------------------------------- | -------- |
| title                      | String                          | Top bar title                                  | -                               | Yes      |
| modifier                   | Modifier                        | Modifier applied to the top bar                | Modifier                        | No       |
| color                      | Color                           | Top bar background color                       | COUITheme.colorScheme.surface  | No       |
| titleColor                 | Color                           | Color of the title text                        | COUITheme.colorScheme.onSurface | No       |
| subtitle                   | String                          | Subtitle text displayed below the title bar    | ""                              | No       |
| subtitleColor              | Color                           | Color of the subtitle text                     | COUITheme.colorScheme.onSurfaceVariantSummary | No       |
| dividerColor               | Color                           | Color of the bottom hairline divider revealed on scroll | COUITheme.colorScheme.dividerLine | No       |
| navigationIcon             | @Composable () -> Unit          | Composable function for navigation icon area   | {}                              | No       |
| actions                    | @Composable RowScope.() -> Unit | Composable function for action buttons area (24dp icons recommended) | {}                              | No       |
| scrollBehavior             | ScrollBehavior?                 | Controls top bar scroll behavior               | null                            | No       |
| defaultWindowInsetsPadding | Boolean                         | Whether to apply default window insets padding | true                            | No       |
| showDivider                | Boolean                         | Whether to draw the bottom hairline divider as content scrolls beneath the bar (requires scrollBehavior) | true | No       |
| centerTitle                | Boolean                         | Centre the title between the two icon slots (COUI `setIsTitleCenterStyle`); replaces the three paddings below with `centerTitleMargin` | false | No |
| centerTitleMargin          | Dp                              | Horizontal margin the centred title reserves at each end | tier for the current window width | No |
| segmentButton              | (@Composable () -> Unit)?       | Segmented control slotted between the navigation icon and the title; ignored while `centerTitle` is set | null | No |
| gradientGlass              | Boolean                         | Paint the ColorOS gradient-blur app bar material instead of the uniform one; it reaches `AppBarBlurOverlayDefaults.ExtraHeight` (24dp) past the bar's bottom edge while the bar keeps its height, and the divider is hidden. With `scrollBehavior` it appears after 30dp of scroll (see Gradient Glass) | false | No |
| titlePadding          | Dp                              | Horizontal content padding                     | TopAppBarDefaults.TitlePadding | No       |
| navigationIconPadding      | Dp                              | Start padding of the navigation icon           | TopAppBarDefaults.NavigationIconPadding | No       |
| actionIconPadding          | Dp                              | End padding of the action icons                | TopAppBarDefaults.ActionIconPadding | No       |
| bottomContent              | @Composable () -> Unit          | Composable content displayed below the title bar area | {} | No |

### TopAppBarDefaults Object

The TopAppBarDefaults object provides default values for TopAppBar and SmallTopAppBar components.

#### Constants

| Constant Name             | Type | Description                                        | Default Value |
| ------------------------- | ---- | -------------------------------------------------- | ------------- |
| TitlePadding    | Dp   | Horizontal padding of the title and large title    | 16.dp         |
| LargeTitleTopPadding | Dp | Top padding of the expanded large title           | 54.dp         |
| NavigationIconPadding      | Dp   | Start padding of the navigation icon              | 16.dp         |
| ActionIconPadding          | Dp   | End padding of the action icons                   | 16.dp         |
| CollapsedHeight            | Dp   | Collapsed height of the TopAppBar                 | 52.dp         |
| ExpandedHeight             | Dp   | Minimum expanded height of the TopAppBar without a subtitle | 107.dp |
| SmallTopAppBarCenterHeight | Dp   | Vertical center height for SmallTopAppBar layout  | 52.dp         |
| LargeTitleBottomPadding    | Dp   | Bottom padding below the large title block when expanded (interpolates to 0.dp as the bar collapses) | 12.dp |
| SubtitleBottomPadding      | Dp   | Bottom padding below the subtitle when it overflows the collapsed bar | 8.dp  |
| SubtitleMarginTop          | Dp   | Vertical gap between the title and the subtitle   | 3.5.dp        |
| NavigationIconGap          | Dp   | Horizontal gap between the navigation icon and the collapsed title | 4.dp |
| ActionIconGap              | Dp   | Horizontal gap between the title and the action icons | 8.dp      |
| ActionMenuItemSpacing      | Dp   | Spacing between action menu items                  | 10.dp         |
| CenterTitleMarginCompat    | Dp   | Centred title's horizontal margin, `compat` tier (below 600dp) | 16.dp |
| CenterTitleMarginMedium    | Dp   | Centred title's horizontal margin, `medium` tier (600–840dp) | 24.dp |
| CenterTitleMarginExpanded  | Dp   | Centred title's horizontal margin, `expanded` tier (840dp and up) | 40.dp |
| CenterTitlePadding         | Dp   | Fallback centred title padding when the window width is unknown | 4.dp |
| SectionGap                 | Dp   | Gap the segment slot keeps on each side of itself, `medium` / `expanded` tiers | 40.dp |
| SectionGapSmall            | Dp   | Section gap in the `compat` tier                  | 0.dp          |
| ActionMenuInnerPadding     | Dp   | Padding inside the action-menu container, folded into the centred title's slot | 0.dp |
| SegmentButtonHeight        | Dp   | Height of the segment slot                         | 32.dp         |
| SegmentButtonMinWidth      | Dp   | Width below which the segment slot no longer counts as fitting beside the title | 108.dp |
| SegmentButtonMaxWidth      | Dp   | Segment slot's width ceiling in the `compat` tier  | 207.dp        |

### ScrollBehavior

COUIScrollBehavior is a configuration object used to control the scroll behavior of the top bar.

#### rememberTopAppBarState

Used to create and remember TopAppBarState:

```kotlin
val scrollBehavior = COUIScrollBehavior(
    state = rememberTopAppBarState(),
    canScroll = { true }
)
```

| Parameter Name     | Type                        | Default Value              | Description                                      |
| ------------------ | --------------------------- | -------------------------- | ------------------------------------------------ |
| state              | TopAppBarState              | rememberTopAppBarState()   | State object controlling scroll state            |
| canScroll          | () -> Boolean               | { true }                   | Callback to control whether scrolling is allowed |
| snapAnimationSpec  | AnimationSpec\<Float>?      | 180ms decelerate tween     | Defines the snap to fully expanded/collapsed when scrolling stops midway |
| flingAnimationSpec | DecayAnimationSpec\<Float>? | rememberSplineBasedDecay() | Defines decay animation for fling                |
| expandOnContentScroll | Boolean                  | false                      | Expand the bar when the content is dragged down anywhere in the list, not only once the list has reached its top |

By default the bar only expands once the list has reached its top. ColorOS 17's home page brings
its collapsed header row back as soon as the list is dragged down, even from the middle of the
list; set `expandOnContentScroll = true` for that behavior:

```kotlin
val scrollBehavior = COUIScrollBehavior(expandOnContentScroll = true)
```

The bar follows the content's actual downward displacement, so it moves with the list instead of
snapping open on a collapse ratio. That also keeps a material whose alpha is driven by the same
displacement — such as [`AppBarBlurOverlay`](./appbarbluroverlay) — from thinning out while the bar
expands. The flag does not consume extra scroll: the list keeps the delta it already consumed.

## Advanced Usage

### Handling Window Insets

```kotlin
TopAppBar(
    title = "Title",
    largeTitle = "Large Title",
    defaultWindowInsetsPadding = false // Handle window insets manually
)
```

### Custom Scroll Behavior Animation

```kotlin
var isScrollingEnabled by remember { mutableStateOf(true) }
val scrollBehavior = COUIScrollBehavior(
    snapAnimationSpec = tween(durationMillis = 100),
    flingAnimationSpec = rememberSplineBasedDecay(),
    canScroll = { isScrollingEnabled } // Can dynamically control whether scrolling is allowed
)

TopAppBar(
    title = "Title",
    largeTitle = "Large Title",
    scrollBehavior = scrollBehavior
)
```

### Combining Large and Small Titles

```kotlin
var useSmallTopBar by remember { mutableStateOf(false) }

Box(modifier = Modifier.fillMaxSize()) {
    if (useSmallTopBar) {
        SmallTopAppBar(
            title = "Compact Mode",
            navigationIcon = {
                IconButton(onClick = { useSmallTopBar = false }) {
                    Icon(
                        imageVector = COUIIcons.Back,
                        contentDescription = "Switch to Large Title",
                        tint = COUITheme.colorScheme.onBackground
                    )
                }
            }
        )
    } else {
        TopAppBar(
            title = "Title",
            largeTitle = "Expanded Mode",
            navigationIcon = {
                IconButton(onClick = { useSmallTopBar = true }) {
                    Icon(
                        imageVector = COUIIcons.Back,
                        contentDescription = "Switch to Small Title",
                        tint = COUITheme.colorScheme.onBackground
                    )
                }
            }
        )
    }
}
```
