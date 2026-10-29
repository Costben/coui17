# PinnedTopAppBar

`PinnedTopAppBar` is the ColorOS 17 Gallery-style top app bar: a large title on the **first row**, next to the action buttons, that never moves. [`TopAppBar`](./topappbar) puts its large title on a second row and collapses it while the content scrolls; this bar keeps a single 52dp row below the status bar and lets only the material change.

The material is [`AppBarBlurOverlay`](./appbarbluroverlay): invisible at rest, so the page background shows through, and fading in over the first 30dp of content scrolled under the bar. The alpha is read in the draw phase, so scrolling never recomposes the bar.

It is driven by `PinnedScrollBehavior`, a `ScrollBehavior` that never collapses the bar and only reports how far the content has scrolled under it.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.PinnedTopAppBar
import io.github.suqi8.coui.kmp.basic.PinnedTopAppBarDefaults
import io.github.suqi8.coui.kmp.basic.PinnedScrollBehavior
import io.github.suqi8.coui.kmp.basic.couiTopAppBarActionItem
```

## Basic Usage

```kotlin
val scrollBehavior = PinnedScrollBehavior()

Box(Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // The list scrolls under the bar; leave room for it at the top.
        Spacer(Modifier.height(PinnedTopAppBarDefaults.Height))
        Content()
    }
    PinnedTopAppBar(
        title = "Photos",
        scrollBehavior = scrollBehavior,
        actions = {
            IconButton(onClick = { /* Handle click event */ }, modifier = Modifier.couiTopAppBarActionItem()) {
                Icon(COUIIcons.Basic.Search, contentDescription = "Search")
            }
            IconButton(onClick = { /* Handle click event */ }, modifier = Modifier.couiTopAppBarActionItem()) {
                Icon(COUIIcons.More, contentDescription = "More")
            }
        },
    )
}
```

The material samples `LocalBackdrop`; provide one for the blur, or the plain alpha ramp is painted. Without a `scrollBehavior` the material stays at its resting alpha, `0`.

### Navigation Icon

`navigationIcon` is optional. The gap before the title (`PinnedTopAppBarDefaults.NavigationIconGap`) is only inserted when it is present.

```kotlin
PinnedTopAppBar(
    title = "Albums",
    scrollBehavior = scrollBehavior,
    navigationIcon = {
        IconButton(onClick = { /* Handle click event */ }, modifier = Modifier.couiTopAppBarActionItem()) {
            Icon(COUIIcons.Back, contentDescription = "Back")
        }
    },
)
```

### Title Style

The title uses `COUITheme.textStyles.title1` (32sp). Its font weight was not measured on the device, so none is imposed; pass `titleStyle` to override:

```kotlin
PinnedTopAppBar(
    title = "Photos",
    titleStyle = PinnedTopAppBarDefaults.titleStyle().copy(fontWeight = FontWeight.Medium),
)
```

## PinnedScrollBehavior

```kotlin
val scrollBehavior = PinnedScrollBehavior(state = rememberTopAppBarState())
```

| Parameter | Type | Default Value | Description |
| --------- | ---- | ------------- | ----------- |
| state | TopAppBarState | rememberTopAppBarState() | The state whose `contentOffset` the behavior writes |
| canScroll | () -> Boolean | { true } | While it returns `false` the content offset is left untouched |

Unlike `COUIScrollBehavior` it never writes `heightOffset`, has no snap or fling animation (`isPinned = true`), and does not intercept the pre-scroll phase, so the content always gets the whole gesture. In `onPostScroll` it accumulates `state.contentOffset += consumed.y`, clamped to `<= 0`: `-state.contentOffset` is the distance the content has scrolled under the bar.

Handed to a `TopAppBar` it keeps that bar fully expanded, and the bar's divider fades in with the share of the bar the content overlaps instead of the collapse fraction. Handed to a `SmallTopAppBar` it drives the divider like any other behavior.

## Parameters

### PinnedTopAppBar Properties

| Property Name | Type | Description | Default Value | Required |
| ------------- | ---- | ----------- | ------------- | -------- |
| title | String | The title, a single ellipsized line at the start of the row | - | Yes |
| modifier | Modifier | Applied to the bar; it reports the status-bar inset plus `Height`, the material band takes no layout space | Modifier | No |
| scrollBehavior | ScrollBehavior? | Drives the material alpha from `-state.contentOffset`; `null` keeps the resting alpha, `0` | null | No |
| defaultWindowInsetsPadding | Boolean | Inset the row from the display cutout and navigation bars horizontally; the status-bar inset is always applied, like `TopAppBar` | true | No |
| containerColor | Color | The material base colour | AppBarBlurOverlayDefaults.containerColor() | No |
| titleColor | Color | The title colour | COUITheme.colorScheme.onSurface | No |
| titleStyle | TextStyle | The title text style | PinnedTopAppBarDefaults.titleStyle() | No |
| backdrop | Backdrop? | The content the material samples | LocalBackdrop.current | No |
| extraHeight | Dp | How far the material reaches past the bar's bottom edge | AppBarBlurOverlayDefaults.ExtraHeight | No |
| startPadding | Dp | Inset between the start edge and the first child | PinnedTopAppBarDefaults.StartPadding | No |
| endPadding | Dp | Inset between the end edge and the last action | PinnedTopAppBarDefaults.EndPadding | No |
| navigationIcon | (@Composable () -> Unit)? | Optional start icon | null | No |
| actions | @Composable RowScope.() -> Unit | The end actions, centred vertically | {} | No |

### PinnedTopAppBarDefaults Object

| Constant / Function | Type | Value | Source |
| ------------------- | ---- | ----- | ------ |
| Height | Dp | 52.dp | Gallery `toolbar` node 184px at 3.53125 px/dp = 52.1dp; reuses `TopAppBarDefaults.CollapsedHeight` |
| StartPadding | Dp | 16.dp | Title `TextView` starts at x=57px = 16.1dp |
| EndPadding | Dp | 16.dp | Last action ends 56px = 15.9dp from the edge |
| ActionSpacing | Dp | 10.dp | Two actions 35px = 9.9dp apart; `TopAppBarDefaults.ActionMenuItemSpacing` |
| ActionItemSize | Dp | 40.dp | Actions 141px = 39.9dp square; `TopAppBarDefaults.ActionItemMinWidth` |
| NavigationIconGap | Dp | 10.dp | Not measured on Gallery; the Settings toolbar's `TopAppBarDefaults.NavigationIconGap` |
| TitleActionGap | Dp | 4.dp | `TopAppBarDefaults.ActionIconGap` |
| titleStyle() | TextStyle | `title1` (32sp) | Title ink "照片" is 29.2dp tall, two glyphs 59dp wide; weight not measured |

## Measurements

Taken from the 照片 tab of the ColorOS 17 Gallery on a OnePlus PLK110 (ColorOS V17.0.0, 1272x2772), with `uiautomator dump` and pixel sampling in the dark theme. `com.coloros.gallery3d` renders at 565 dpi, i.e. 3.53125 px/dp (a 360dp short edge).

- The status bar is 141px; the `toolbar` node spans y 141..325, 184px = 52.1dp.
- The title starts at x=57px (16dp); its ink spans x 63..271 and y 181..283, consistent with a 32dp headline.
- The two actions (`action_main_tab_search` and the overflow key) are 141px = 40dp square, 35px = 10dp apart, 56px = 16dp from the end edge, vertically centred in the row.
- After scrolling the timeline by ~1000px the title and the actions keep identical bounds.
- The dark page background measures 48 / 48 / 48 = `AppBarBlurOverlayDefaults.DarkContainerColor`.

## Gallery Density

The Gallery ignores the display-size setting: `BaseActivity.adjustDensity` pins its resources to a fixed dpi before any view is inflated, so its toolbar keys, bottom bar and accessory balls measure the same pixels at any density the user picks. `rememberGalleryDensity()` returns that density, with the platform `fontScale` kept:

- On a device that can switch the panel's resolution (the `oplus.software.display.resolution_switch_support` feature) the display's short edge is pinned to 360dp, `(shortEdgePx / 360 * 160).toInt()`: 565 dpi = 3.53125 px/dp on the 1272px PLK110.
- On every other device it is the panel's own density: 560 dpi = 3.5 px/dp on the 1240px PKT110, against 620 dpi for the rest of the system. There a 56dp accessory ball measures 196px and a 40dp toolbar key 140px, identical to the real Gallery's.

```kotlin
import io.github.suqi8.coui.kmp.utils.rememberGalleryDensity

CompositionLocalProvider(LocalDensity provides rememberGalleryDensity()) {
    BottomFloatingNavigationBar { /* items */ }
}
```

Keep the layout around that chrome — its alignment, insets and outer margins — at the system density. Anchored popups convert to the panel density on their own, so they are safe either side of the scope. Desktop, Apple and web targets have no display-size override and get the platform density. The math is exposed as `GalleryDensityDefaults.densityDpi(physicalDpi, shortEdgePx, resolutionSwitch)`.
