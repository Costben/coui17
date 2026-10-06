# SideNavigationBar

`SideNavigationBar` is the ColorOS side drawer: a navigation list that slides in from the start edge over the page. On compact windows it floats above the content and dims it with the `coui_color_mask` scrim; once the window is wide enough it becomes a fixed column pinned beside the content, the scrim clears and the content stays interactive. A 20dp edge strip and a drag on the drawer itself both move it, and a release settles it onto whichever side of its travel it passed.

It mirrors ColorOS's side-navigation drawer, which extends `androidx.drawerlayout.widget.DrawerLayout`.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.SideNavigationBar
import io.github.suqi8.coui.kmp.basic.SideNavigationBarDefaults
import io.github.suqi8.coui.kmp.basic.SideNavigationBarListener
import io.github.suqi8.coui.kmp.basic.SideNavigationBarMode
import io.github.suqi8.coui.kmp.basic.SideNavigationBarState
import io.github.suqi8.coui.kmp.basic.rememberSideNavigationBarState
```

## Native Origin

The native class is a `DrawerLayout` with a drawer child at the start edge and a content child behind it. `updateDrawerState` decides the `DRAWER_MODE_*` from two weights: the window size class (compact / medium / expanded = 1 / 2 / 3) minus the content structure weight (normal / parent-child = 1 / 2). A positive remainder is `DRAWER_MODE_FIXED` (1), otherwise `DRAWER_MODE_TEMPORARY` (0). `calculateDrawerWidth` then gives a temporary drawer `coui_side_navigation_bar_min_width` (300dp) and a fixed drawer `min(coui_side_navigation_bar_max_width, windowWidth * 0.382)`. `setDrawerMode` additionally paints `coui_color_mask` (as `mMaskColor`) for the temporary mode and clears the scrim for the fixed one, so on a fixed drawer only the edit-state animator can bring the mask back.

`startMaskFadeAnimation` builds its `ValueAnimator` without a duration or interpolator, so the platform defaults of 300ms and `AccelerateDecelerateInterpolator` apply. The settle animation itself is inherited from `DrawerLayout`, which routes it through `androidx.customview.widget.ViewDragHelper#settleCapturedViewAt`; COUI declares no drawer duration or curve token, so `SideNavigationBarDefaults.settleDurationMillis` reproduces `ViewDragHelper#computeAxisDuration` (256ms base, 600ms cap) instead.

## Basic Usage

```kotlin
val drawerState = rememberSideNavigationBarState()

SideNavigationBar(
    drawerContent = { NavigationList() },
    state = drawerState
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = "相册",
                navigationIcon = {
                    IconButton(onClick = { drawerState.open() }) {
                        Icon(COUIIcons.Basic.FoldPane, contentDescription = "打开侧边导航")
                    }
                }
            )
        }
    ) { padding ->
        PhotoGrid(Modifier.padding(padding))
    }
}
```

## Parameters

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `drawerContent` | `@Composable () -> Unit` | The drawer body, laid out at the start edge |
| `modifier` | `Modifier` | The modifier applied to the whole navigation bar |
| `state` | `SideNavigationBarState` | The drawer state, defaulting to `rememberSideNavigationBarState()` |
| `parentChildHierarchy` | `Boolean` | Raises the content structure weight from 1 to 2 (`setParentChildHierarchy`), which can demote a medium window back to a temporary drawer |
| `editState` | `Boolean` | The host is in an edit state (`setIsInEditState`). On a fixed drawer this fades the mask in; on a temporary one it is inert, as in the device view |
| `handleEditModeMask` | `Boolean` | The device `getHandlerEditModeMask()` flag; when false the edit-state mask is never faded in. Defaults to the device value `true` |
| `pushContent` | `Boolean` | Pads the content by the drawer's current trailing edge instead of overlaying it. The device base class always overlays; this is the behaviour the Gallery's `SidePaneLayout` subclass adds in its own `t()` |
| `scrimColor` | `Color` | The color painted over the content in `Temporary` mode, `coui_color_mask` (`#33000000`) |
| `dividerColor` | `Color` | The color of the hairline on the drawer's trailing edge |
| `content` | `@Composable () -> Unit` | The page content behind the drawer |

### SideNavigationBarState

| Member | Type | Description |
| :--- | :--- | :--- |
| `drawerWidth` | `Dp` | The drawer width the layout resolved, mirroring `getDrawerViewWidth()` |
| `mode` | `SideNavigationBarMode` | The mode the layout resolved, mirroring `getDrawerMode()` |
| `isOpen` | `Boolean` | Whether the drawer is asked to be open |
| `slideOffset` | `Float` | The current position, 0f closed to 1f fully open |
| `open()` / `close()` / `toggle()` | — | `openDrawer(View, boolean)` / `closeDrawer(View, boolean)` and the fold button's flip |
| `addListener()` / `removeListener()` | — | `addSideNavigationBarListener` / `removeSideNavigationBarListener` |

### SideNavigationBarDefaults

| Constant | Type | Value | COUI Source |
| :--- | :--- | :--- | :--- |
| `WIDTH_PROPORTION` | `Float` | 0.382f | `COUISideNavigationBar.DRAWER_WIDTH_PROPORTION` |
| `MinWidth` | `Dp` | 300.dp | `dimen/coui_side_navigation_bar_min_width` |
| `MaxWidth` | `Dp` | 280.dp | `dimen/coui_side_navigation_bar_max_width` |
| `MinDrawerMargin` | `Dp` | 64.dp | `COUISideNavigationBar.MIN_DRAWER_MARGIN` |
| `DividerWidth` | `Dp` | 0.33.dp | `dimen/coui_side_navigation_drawer_divider_width` |
| `EdgeSize` | `Dp` | 20.dp | `ViewDragHelper.DEFAULT_EDGE_SIZE` |
| `MinFlingVelocity` | `Dp` | 400.dp | `ViewDragHelper.MIN_VELOCITY` |
| `BASE_SETTLE_DURATION_MILLIS` | `Int` | 256 | `ViewDragHelper.BASE_SETTLE_DURATION` |
| `MAX_SETTLE_DURATION_MILLIS` | `Int` | 600 | `ViewDragHelper.MAX_SETTLE_DURATION` |
| `EDIT_MASK_DURATION_MILLIS` | `Int` | 300 | `ValueAnimator` default, via `startMaskFadeAnimation` |

`scrimColor()` resolves to `coui_color_mask` (`#33000000`), `dividerColor()` to `attr/couiColorDivider` (falling back to `color/coui_color_divider`). `drawerMode(sizeClass, parentChildHierarchy)`, `drawerWidth(mode, windowWidth)` and `settleDurationMillis(dx, velocity, motionRange, parentWidth)` carry the three device calculations described above.

Note that `coui_side_navigation_bar_min_width` (300dp) is larger than `coui_side_navigation_bar_max_width` (280dp). In the device code the 300dp value is the temporary-mode width and the 280dp value caps the fixed-mode proportion, so the two names describe different modes rather than a min/max pair.

## Recovered Tokens

Every value below is aligned with the ColorOS 17 implementation; none of them is invented.

| Token | Value | Source |
| :--- | :--- | :--- |
| `WidthProportion` | 0.382 | `COUISideNavigationBar.DRAWER_WIDTH_PROPORTION` |
| Temporary width | 300dp | `dimen/coui_side_navigation_bar_min_width` |
| Fixed width cap | 280dp | `dimen/coui_side_navigation_bar_max_width` |
| Min drawer margin | 64dp | `COUISideNavigationBar.MIN_DRAWER_MARGIN` |
| Divider width | 0.33000004dp | `dimen/coui_side_navigation_drawer_divider_width` |
| Scrim | `#33000000` | `color/coui_color_mask` |
| Divider colour | `#1f000000` | `color/coui_color_divider` |
| Edge strip | 20dp | `ViewDragHelper.DEFAULT_EDGE_SIZE` |
| Min fling velocity | 400dp/s | `ViewDragHelper.MIN_VELOCITY` |
| Settle base / cap | 256ms / 600ms | `ViewDragHelper.BASE_SETTLE_DURATION` / `MAX_SETTLE_DURATION` |
| Edit-mask fade | 300ms, `AccelerateDecelerateInterpolator` | `ValueAnimator` defaults, `startMaskFadeAnimation` |

## Not Ported

- **`DrawerLayout`'s `DrawerListener` / `setDrawerLockMode` / `openDrawer(gravity)`** — the drawer is always the start-edge child, so gravity and lock mode have no counterpart; a fixed drawer is expressed through the resolved `SideNavigationBarMode`.
- **`onDrawerSlide(View, float)` with the drawer `View`** — `SideNavigationBarListener.onDrawerSlide` reports `(slideOffset, slideWidth, mode)` and drops the view, since the drawer is a slot rather than a child view group.
- **`setScrimColor(int)` / `getScrimColor()` beyond `coui_color_mask`** — `scrimColor` carries the single COUI mask colour.
- **`COUISideNavigationBarSavedState`'s parcel** — the persisted state is the `SideNavigationBarState.Saver`, which keeps the open flag only.
- **The `mExplicitDrawerState` / `mImplicitDrawerState` split** — the device tracks whether the user or the framework last moved the drawer; the port stores a single `isOpen` command.
