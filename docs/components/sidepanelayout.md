# SidePaneLayout

`SidePaneLayout` is a draggable pane pinned to the start edge of a page. The pane holds a navigation list, the content sits behind it, and a round fold button at the pane's top start toggles it. When `coverStyle` is set the pane slides over the content and dims it with the `coui_color_mask` scrim; otherwise the content is pushed aside by the pane's trailing edge. Opening and closing run a 483ms move-ease tween, and a drag can also be settled with a fling.

It ports `com.coui.appcompat.sidepane.COUISidePaneLayout`, which extends `android.widget.RelativeLayout`.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.SidePaneLayout
import io.github.suqi8.coui.kmp.basic.SidePaneDefaults
import io.github.suqi8.coui.kmp.basic.SidePaneFoldIconColors
import io.github.suqi8.coui.kmp.basic.SidePaneLayoutState
import io.github.suqi8.coui.kmp.basic.SidePaneMaskListener
import io.github.suqi8.coui.kmp.basic.SidePanePanelSlideListener
import io.github.suqi8.coui.kmp.basic.SidePaneState
import io.github.suqi8.coui.kmp.basic.SidePaneUtils
import io.github.suqi8.coui.kmp.basic.rememberSidePaneLayoutState
```

## Native Origin

The native view lays out three children: index 0 is the slideable pane, index 1 the content, and index 2 the fold button inflated from `layout/coui_sliding_icon_layout.xml` — an `ImageButton` with `drawable/coui_pane_icon_bg` as its background, `coui_side_pane_layout_icon_padding` (12dp) of padding and `drawable/coui_pane_fold_normal` as its source. Only indices 0 and 1 have Compose counterparts as slots; index 2 is the fold control this composable draws at the pane's top start, offset by `coui_side_pane_layout_icon_margin_start` (12dp) and `coui_side_pane_layout_icon_margin_top` (45dp).

A `ViewDragHelper` with sensitivity 0.5 captures the drag, with `MIN_FLING_VELOCITY` (400 dp/s) as its minimum fling speed. Opening and closing is a `ValueAnimator` of `DURATION_OF_SLIDING_ANIMATOR` (483ms) driven by `COUIMoveEaseInterpolator` — `PathInterpolator(0.3f, 0.0f, 0.1f, 1.0f)`. The device settles a released drag through `settleCapturedViewAt` and declares no separate duration for that path, so this composable settles both a gesture release and a programmatic open/close with the same 483ms move-ease tween.

The scrim is `coui_color_mask` (`#33000000`) painted at its own alpha multiplied by the slide offset, and it only shows when `coverStyle` or `alwaysShowMask` is set. `setAlwaysShowMask` keeps the mask on for a tap-to-close; `setTouchContentEnable` (default true) decides whether a tap outside the pane closes it at all.

`SidePaneUtils` carries the responsive helpers: `isLargeScreenStyle` is 600dp wide and 900dp tall, `isMediumScreenStyle` is 600dp wide and at least 480dp tall or in multi-window, `makeFirstEditIconAnim` ramps the fold icon's alpha across the last tenth of the slide, and `makeSecToolbarAnim` ramps a secondary toolbar's start margin from `coui_side_pane_layout_toolbar_margin_start` (72dp) down to 0.

> **Call site note.** Neither of the two studied system apps references `COUISidePaneLayout`: the class is shipped inside `Gallery.apk` and `Dialer.apk`, but a repository-wide search finds it only in its own source, the generated `R.java` / `R$styleable` classes and the dex binaries. The Gallery's side pane is a different class — `ColorOS.app.sidepane.ui.SidePaneLayout extends COUISideNavigationBar` (see [SideNavigationBar](../components/sidenavigationbar)). The demo position of this component is therefore a reconstruction from the device class, not a copy of an app screen.

## Basic Usage

```kotlin
val paneState = rememberSidePaneLayoutState()

SidePaneLayout(
    paneContent = { NavigationList() },
    state = paneState,
    coverStyle = true,
    alwaysShowMask = true
) {
    PhotoGrid()
}
```

## Parameters

| Parameter | Type | Description |
| :--- | :--- | :--- |
| `paneContent` | `@Composable () -> Unit` | The pane body, laid out at the start edge |
| `modifier` | `Modifier` | The modifier applied to the whole layout |
| `state` | `SidePaneLayoutState` | The pane state, defaulting to `rememberSidePaneLayoutState()` |
| `paneWidth` | `Dp` | The pane width, i.e. the `firstPaneWidth` / `expandPaneWidth` attribute default `coui_sliding_pane_width` (224dp) |
| `coverStyle` | `Boolean` | The `coverStyle` attribute (default false). When true the content keeps the full width and the pane covers it; when false the content is pushed by the pane's trailing edge |
| `alwaysShowMask` | `Boolean` | `setAlwaysShowMask` (default false). Together with `coverStyle` it enables the `coui_color_mask` scrim |
| `touchContentEnabled` | `Boolean` | `setTouchContentEnable` (device default true). A tap outside the pane closes it only when this, `coverStyle` and the open state all hold |
| `showFoldIcon` | `Boolean` | `setCreateIcon` (device default true). When false no fold button is placed |
| `foldIcon` | `ImageVector` | The glyph inside the fold button, defaulting to `COUIIcons.Basic.FoldPane` |
| `slideListener` | `SidePanePanelSlideListener?` | Mirrors `setPanelSlideListener` |
| `maskListener` | `SidePaneMaskListener?` | Mirrors `setOnMaskClickListener` |
| `scrimColor` | `Color` | The scrim colour, `coui_color_mask` (`#33000000`) |
| `paneColor` | `Color` | The pane background; the device paints none, so the port defaults to `surface` |
| `foldIconColors` | `SidePaneFoldIconColors` | The fold button colours |
| `content` | `@Composable () -> Unit` | The page content the pane slides over |

### SidePaneLayoutState

| Member | Type | Description |
| :--- | :--- | :--- |
| `state` | `SidePaneState` | The resting state, `Open` (0) or `Closed` (1), mirroring `isOpen()` inverted |
| `isOpen` | `Boolean` | Whether the pane is asked to be open |
| `slideOffset` | `Float` | The current position, 0f hidden to 1f fully shown; mirrors `mSlideOffset` |
| `isSliding` | `Boolean` | Whether the open/close animator is running; mirrors `isSliding()` |
| `open()` / `close()` / `toggle()` | — | `openPane()` / `closePane()`, and the fold button's flip |

### SidePaneUtils

| Constant | Type | Value | COUI Source |
| :--- | :--- | :--- | :--- |
| `LARGE_SCREEN_WIDTH_THRESHOLD` | `Int` | 600 | `COUISidePaneUtils.LARGE_SCREEN_WIDTH_DP_THRESHOLD` |
| `LARGE_SCREEN_HEIGHT_THRESHOLD` | `Int` | 900 | `COUISidePaneUtils.LARGE_SCREEN_HEIGHT_DP_THRESHOLD` |
| `SMALLEST_SCREEN_HEIGHT_THRESHOLD` | `Int` | 480 | `COUISidePaneUtils.SMALLEST_SCREEN_HEIGHT_DP_THRESHOLD` |
| `START_EDIT_ICON_ALPHA_OFFSET` | `Float` | 0.9f | `COUISidePaneUtils.START_EDIT_ICON_ALPHA_OFFSET` |

`isLargeScreenStyle(windowSize)`, `isMediumScreenStyle(windowSize, inMultiWindowMode)`, `editIconAlpha(progress)` and `secondaryToolbarMarginStart(progress, margin)` are the four device methods as pure functions over the value the caller would have written into the view.

### SidePaneDefaults

| Constant | Type | Value | COUI Source |
| :--- | :--- | :--- | :--- |
| `PaneWidth` | `Dp` | 224.dp | `dimen/coui_sliding_pane_width` |
| `OverhangSize` | `Dp` | 32.dp | `COUISidePaneLayout.DEFAULT_OVERHANG_SIZE` |
| `DURATION_MILLIS` | `Int` | 483 | `COUISidePaneLayout.DURATION_OF_SLIDING_ANIMATOR` |
| `MinFlingVelocity` | `Dp` | 400.dp | `COUISidePaneLayout.MIN_FLING_VELOCITY` |
| `COVER_STYLE` | `Boolean` | false | `coverStyle` attribute default |
| `ALWAYS_SHOW_MASK` | `Boolean` | false | `setAlwaysShowMask` default |
| `TOUCH_CONTENT_ENABLED` | `Boolean` | true | `setTouchContentEnable` default |
| `CREATE_ICON` | `Boolean` | true | `setCreateIcon` default |
| `FoldIconSize` | `Dp` | 48.dp | `dimen/coui_side_pane_layout_icon_size` |
| `FoldIconHeight` | `Dp` | 52.dp | `coui_pane_fold_normal` viewport |
| `FoldIconPadding` | `Dp` | 12.dp | `dimen/coui_side_pane_layout_icon_padding` |
| `FoldIconTouchSize` | `Dp` | 72.dp | `FoldIconSize + FoldIconPadding * 2` |
| `FoldIconMarginTop` | `Dp` | 45.dp | `dimen/coui_side_pane_layout_icon_margin_top` |
| `FoldIconMarginStart` | `Dp` | 12.dp | `dimen/coui_side_pane_layout_icon_margin_start` |
| `ToolbarMarginStart` | `Dp` | 72.dp | `dimen/coui_side_pane_layout_toolbar_margin_start` |
| `FOLD_ICON_CONTENT_DESCRIPTION` | `String` | "Shrink and expand button" | `string/coui_side_pane_icon_content_description` |

`scrimColor()` resolves to `coui_color_mask`, `paneColor()` to `surface`, and `foldIconColors()` to `coui_pane_icon_bg_color` (`#0d000000` light, `#1affffff` night) over a `couiColorLabelPrimary` tint.

## Recovered Tokens

Every value below was recovered from the ColorOS 17 resource dump of `Gallery.apk` or from the decompiled `COUISidePaneLayout` / `COUISidePaneUtils`; none of them is invented.

| Token | Value | Source |
| :--- | :--- | :--- |
| Pane width | 224dp | `dimen/coui_sliding_pane_width` |
| Slide duration | 483ms | `COUISidePaneLayout.DURATION_OF_SLIDING_ANIMATOR` |
| Move easing | `cubic-bezier(0.3, 0, 0.1, 1)` | `COUIMoveEaseInterpolator`, `PathInterpolator(0.3f, 0f, 0.1f, 1f)` |
| Min fling velocity | 400dp/s | `COUISidePaneLayout.MIN_FLING_VELOCITY` |
| Overhang size | 32dp | `COUISidePaneLayout.DEFAULT_OVERHANG_SIZE` |
| Open / close state | 0 / 1 | `COUISidePaneLayout.OPEN_STATE` / `CLOSE_STATE` |
| Scrim | `#33000000` | `color/coui_color_mask` |
| Fold icon size | 48dp | `dimen/coui_side_pane_layout_icon_size` |
| Fold icon padding | 12dp | `dimen/coui_side_pane_layout_icon_padding` |
| Fold icon margin | 12dp / 45dp | `coui_side_pane_layout_icon_margin_start` / `_margin_top` |
| Toolbar margin | 72dp | `dimen/coui_side_pane_layout_toolbar_margin_start` |
| Fold icon background | `#0d000000` light, `#1affffff` night | `color/coui_pane_icon_bg_color` |
| Fold glyph | 48x52dp, 1.4 stroke | `drawable/coui_pane_fold_normal` |
| Large screen | 600dp wide, 900dp tall | `COUISidePaneUtils.LARGE_SCREEN_WIDTH_DP_THRESHOLD` / `_HEIGHT_` |
| Medium screen | 600dp wide, 480dp tall | `COUISidePaneUtils.SMALLEST_SCREEN_HEIGHT_DP_THRESHOLD` |
| Edit icon alpha offset | 0.9f | `COUISidePaneUtils.START_EDIT_ICON_ALPHA_OFFSET` |
| Fold icon description | "Shrink and expand button" | `string/coui_side_pane_icon_content_description` |

## Not Ported

- **`LayoutParams` with `firstPaneWidth` / `expandPaneWidth` per child** — the pane is a slot and its width is the single `paneWidth` parameter.
- **`onInterceptTouchEvent` / `onTouchEvent` beyond the drag and mask tap** — the port relies on `Modifier.draggable` and a mask tap instead of a custom `ViewDragHelper` callback.
- **`AccessibilityDelegate` and `ExploreByTouchHelper`** — the pane and content are ordinary composables; only the fold button carries a `contentDescription`.
- **`COUISidePaneLifeCycleObserver`** — the responsive re-measure on configuration change is left to Compose's own recomposition.
- **`SavedState`'s `isDefalutOpen` (the device typo) and `state` fields** — the persisted state is `SidePaneLayoutState.Saver`, which keeps the open flag only.
- **`getDrawerViewPaddingTop` / `reMeasureContentView`** — the host's own content padding is not modelled; the caller sets it through `modifier`.
