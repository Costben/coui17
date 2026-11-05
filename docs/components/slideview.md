# SlideView

<div style="position: relative; height: 260px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../compose/index.html?id=slideView" title="Demo"></iframe>
</div>

An experimental list row that slides aside to reveal actions behind it, the swipe-to-delete row of ColorOS 17 (`COUISlideView` in its rectangular style, as used by Calendar's custom reminders). The actions fill the full row height with square corners. The first action sits at the end edge.

## Usage

Import `SlideView`, `SlideMenuItem` and `rememberSlideViewState` from `io.github.suqi8.coui.kmp.basic`, and opt in to `ExperimentalCOUIApi`. Give the row content its own background, because the actions are drawn behind it.

```kotlin
@OptIn(ExperimentalCOUIApi::class)
@Composable
fun ReminderRow(title: String, onRemove: () -> Unit) {
    val state = rememberSlideViewState()
    val scope = rememberCoroutineScope()
    SlideView(
        menuItems = listOf(SlideMenuItem(text = null, icon = COUIIcons.Delete, contentDescription = "Delete")),
        onMenuItemClick = {
            scope.launch {
                state.delete() // plays the delete animation
                onRemove()     // then remove the item from your list
            }
        },
        state = state,
    ) {
        BasicComponent(modifier = Modifier.background(CardDefaults.defaultColors().color), title = title)
    }
}
```

The system delete action is a 24dp trash icon with no label. That icon is not bundled with the library; pass your own icon, or a label through `text`.

## Interaction

- Swiping towards the start moves the content one-to-one until the menu is fully revealed. Past the menu the drag is damped and the actions spread out, keeping their labels centred in the growing fill.
- On release a fling faster than 1000px/s opens or closes the menu; otherwise the row settles on the side of the half-way point it is on. The settle is a critically damped spring with stiffness 200.
- Tapping the content of an open row closes it in at most 200ms. Tapping an action calls `onMenuItemClick` with its index.
- `state.delete()` slides the content out across the row in 330ms while the row height collapses over 400ms, both on `COUIMoveEase`. It returns once the row has collapsed.
- With `fastDeleteEnabled = true`, releasing more than 48dp past the menu, or flinging more than 30dp past it, deletes the row on its own and then calls `onFastDelete`.
- Each action is also exposed as a custom accessibility action.

## Properties

| Property | Type | Description | Default |
| --- | --- | --- | --- |
| menuItems | `List<SlideMenuItem>` | Actions, from the end edge inwards | — |
| onMenuItemClick | `(Int) -> Unit` | Called with the tapped action's index | — |
| modifier | `Modifier` | Modifier for the row | `Modifier` |
| state | `SlideViewState` | Row position and delete state | `rememberSlideViewState()` |
| enabled | `Boolean` | Whether the row can be slid | `true` |
| fastDeleteEnabled | `Boolean` | Whether sliding far past the menu deletes the row | `false` |
| onFastDelete | `() -> Unit` | Called after a fast delete has collapsed the row | `{}` |
| colors | `SlideViewColors` | Action fill, label and icon colours | `SlideViewDefaults.slideViewColors()` |
| textStyle | `TextStyle` | Action label style | `SlideViewDefaults.textStyle()` |
| content | `@Composable () -> Unit` | Row content | — |

### SlideMenuItem

| Property | Type | Description | Default |
| --- | --- | --- | --- |
| text | `String?` | Label drawn centred in the action | — |
| icon | `ImageVector?` | Icon drawn centred at its intrinsic size | `null` |
| contentDescription | `String?` | Accessibility label | `text` |
| color | `Color` | Action fill; `Color.Unspecified` uses `colors.menuColor` | `Color.Unspecified` |
| width | `Dp` | Minimum width; a longer label widens the action | `80.dp` |

### SlideViewState

| Member | Description |
| --- | --- |
| `isOpen` | Whether the menu is open or opening |
| `isDeleting` | Whether a delete animation is running or has finished |
| `open()` / `close()` | Open with the release spring / close with the short glide |
| `delete()` | Play the delete animation; returns once the row has collapsed |

## SlideViewDefaults

| Value | Default | Source |
| --- | --- | --- |
| `MenuItemWidth` | 80dp | `coui_slideview_menuitem_width` |
| `TextPadding` | 12dp | `coui_slide_view_text_padding` |
| `OverSlideDeleteDistance` | 48dp | `coui_slideview_over_slide_delete` |
| `QuickDeleteDistance` | 30dp | `coui_slideview_quick_delete` |
| `FlingVelocity` | 1000px/s | `COUISlideView.onTouchEvent` |
| `ReleaseSpring` | damping 1, stiffness 200 | `animationScrollTo` |
| `SmoothScrollEasing` | cubic-bezier(0.133, 0, 0.3, 1), 3ms/px up to 200ms | `smoothScrollTo` |
| `DeleteSlideDurationMillis` / `DeleteCollapseDurationMillis` | 330ms / 400ms (10ms delay) | `COUISlideDeleteAnimation` |
| `slideViewColors()` | `colorScheme.error`, `#F9F9F9` labels, white icons | `couiColorError`, `coui_slideview_textcolor` |
| `textStyle()` | 15sp, growing at most 1.15x with font scale | `coui_slide_view_text_size` |
