# Tooltip

`Tooltip` is a component in COUI that briefly describes an anchor element. Following Material 3, a single `TooltipBox` anchors a tooltip to its content; you fill its `tooltip` slot with a `PlainTooltip` (a short label on the COUI tips blue) or a `RichTooltip` (a surface-container card with an optional title and action). Tooltips are shown on hover (mouse) or long press (touch), or programmatically via `TooltipState`.

<div style="position: relative; height: 360px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../compose/index.html?id=tooltip" title="Demo" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" referrerpolicy="strict-origin-when-cross-origin"></iframe>
</div>

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.TooltipBox
import io.github.suqi8.coui.kmp.basic.RichTooltipBox
import io.github.suqi8.coui.kmp.basic.PlainTooltip
import io.github.suqi8.coui.kmp.basic.RichTooltip
import io.github.suqi8.coui.kmp.basic.TooltipState
import io.github.suqi8.coui.kmp.basic.rememberTooltipState
import io.github.suqi8.coui.kmp.basic.TooltipDefaults
import io.github.suqi8.coui.kmp.basic.TooltipAnchorPosition
```

## Basic Usage

The quickest way to label an icon-only control is the `text` convenience overload of `TooltipBox`:

```kotlin
TooltipBox(text = "Search") {
    IconButton(onClick = { /* ... */ }) {
        Icon(
            imageVector = COUIIcons.Search,
            contentDescription = "Search",
        )
    }
}
```

## TooltipBox

`TooltipBox` anchors a tooltip to its `content`. Its `tooltip` slot is a `TooltipScope` lambda, filled with `PlainTooltip` or `RichTooltip`.

```kotlin
@Composable
fun TooltipBox(
    positionProvider: PopupPositionProvider,
    tooltip: @Composable TooltipScope.() -> Unit,
    state: TooltipState,
    modifier: Modifier = Modifier,
    focusable: Boolean = false,
    enableUserInput: Boolean = true,
    content: @Composable () -> Unit,
)
```

| Parameter Name  | Type                                | Description                                                        | Default Value | Required |
| --------------- | ----------------------------------- | ----------------------------------------------------------------- | ------------- | -------- |
| positionProvider | PopupPositionProvider               | Positions the tooltip; see `TooltipDefaults.rememberTooltipPositionProvider` | -    | Yes      |
| tooltip         | @Composable TooltipScope.() -> Unit | The tooltip content (`PlainTooltip` / `RichTooltip`)              | -             | Yes      |
| state           | TooltipState                        | State controlling visibility                                      | -             | Yes      |
| modifier        | Modifier                            | Modifier applied to the anchor wrapper                            | Modifier      | No       |
| focusable       | Boolean                             | Whether the tooltip is focusable (true for interactive rich tooltips) | false    | No       |
| enableUserInput | Boolean                             | Whether hover / long press on the anchor shows the tooltip        | true          | No       |
| content         | @Composable () -> Unit              | The anchor content                                                | -             | Yes      |

```kotlin
TooltipBox(
    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
    tooltip = { PlainTooltip { Text("Add to favorites") } },
    state = rememberTooltipState(),
) {
    IconButton(onClick = { /* ... */ }) {
        Icon(imageVector = COUIIcons.Basic.Check, contentDescription = "Add to favorites")
    }
}
```

## PlainTooltip

`PlainTooltip` is a short, non-interactive label rendered on the COUI tips blue. It is a `TooltipScope` extension, so it is used inside the `TooltipBox` `tooltip` slot.

```kotlin
@Composable
fun TooltipScope.PlainTooltip(
    modifier: Modifier = Modifier,
    caretShape: Shape? = TooltipDefaults.caretShape(),
    maxWidth: Dp = TooltipDefaults.PlainTooltipMaxWidth,
    cornerRadius: Dp = TooltipDefaults.PlainTooltipCornerRadius,
    containerColor: Color = TooltipDefaults.plainTooltipContainerColor,
    contentColor: Color = TooltipDefaults.plainTooltipContentColor,
    insideMargin: PaddingValues = TooltipDefaults.PlainTooltipInsideMargin,
    content: @Composable () -> Unit,
)
```

By default the plain tooltip is a blue bubble with white text, matching ColorOS `COUIToolTips` (`couiColorTips` over `coui_color_white`). The bubble carries the COUI arrow by default — `COUIToolTips.show()` / `showWithDirection()` default `hasIndicator` to true — and the arrow points at the anchor. Pass `caretShape = null` for the caret-less Miuix bubble.

## RichTooltip

`RichTooltip` is a persistent, interactive card with an optional title and action, rendered on a normal `surfaceContainer`. It is a `TooltipScope` extension.

```kotlin
@Composable
fun TooltipScope.RichTooltip(
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
    caretShape: Shape? = TooltipDefaults.caretShape(),
    maxWidth: Dp = TooltipDefaults.RichTooltipMaxWidth,
    cornerRadius: Dp = TooltipDefaults.RichTooltipCornerRadius,
    colors: RichTooltipColors = TooltipDefaults.richTooltipColors(),
    insideMargin: PaddingValues = TooltipDefaults.RichTooltipInsideMargin,
    text: @Composable () -> Unit,
)
```

A rich tooltip is interactive, so set `focusable = true` on the `TooltipBox` (the convenience `RichTooltipBox` does this for you) so an outside tap dismisses it and its action is reachable.

```kotlin
val tooltipState = rememberTooltipState(isPersistent = true)
val scope = rememberCoroutineScope()

TooltipBox(
    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
    tooltip = {
        RichTooltip(
            title = { Text("What's new") },
            action = {
                TextButton(text = "Got it", onClick = { tooltipState.dismiss() })
            },
        ) {
            Text("Rich tooltips support a title, supporting text, and an action.")
        }
    },
    state = tooltipState,
    focusable = true,
) {
    IconButton(onClick = { scope.launch { tooltipState.show() } }) {
        Icon(imageVector = COUIIcons.Basic.Check, contentDescription = "What's new")
    }
}
```

## TooltipState

`TooltipState` controls the tooltip's visibility. A global `MutatorMutex` keeps only one tooltip visible at a time.

```kotlin
@Stable
interface TooltipState {
    val transition: MutableTransitionState<Boolean>
    val isVisible: Boolean
    val isPersistent: Boolean
    suspend fun show(mutatePriority: MutatePriority = MutatePriority.Default)
    fun dismiss()
    fun onDispose()
}

@Composable
fun rememberTooltipState(
    initialIsVisible: Boolean = false,
    isPersistent: Boolean = false,
    mutatorMutex: MutatorMutex = BasicTooltipDefaults.GlobalMutatorMutex,
): TooltipState
```

| Parameter Name  | Type         | Description                                              | Default Value                      | Required |
| --------------- | ------------ | ------------------------------------------------------- | ---------------------------------- | -------- |
| initialIsVisible | Boolean     | Whether the tooltip is initially visible                | false                              | No       |
| isPersistent    | Boolean      | Whether the tooltip stays visible until dismissed       | false                              | No       |
| mutatorMutex    | MutatorMutex | Mutex ensuring only one tooltip shows at a time         | BasicTooltipDefaults.GlobalMutatorMutex | No  |

When `isPersistent` is false (plain tooltips), the tooltip is dismissed automatically after a short timeout. When true (rich tooltips), it stays until `dismiss()`, an outside tap, or a back press. `show()` is a suspend function, so call it from a coroutine (e.g. `scope.launch { state.show() }`).

## TooltipDefaults

`TooltipDefaults` provides position providers, colors, the caret, and dimension constants.

### Position provider

```kotlin
@Composable
fun TooltipDefaults.rememberTooltipPositionProvider(
    positioning: TooltipAnchorPosition = TooltipAnchorPosition.Below,
    spacingBetweenTooltipAndAnchor: Dp = TooltipDefaults.SpacingBetweenTooltipAndAnchor,
): PopupPositionProvider
```

`TooltipAnchorPosition` selects the preferred side: `Above`, `Below`, `Left`, `Right`, `Start`, `End`. The tooltip is centered on the cross axis and flips to the opposite side when there is not enough room.

### Colors

```kotlin
// Plain tooltip (COUI tips blue)
val plainContainer = TooltipDefaults.plainTooltipContainerColor   // primary
val plainContent = TooltipDefaults.plainTooltipContentColor       // onPrimary

// Rich tooltip (surface container)
val richColors = TooltipDefaults.richTooltipColors(
    containerColor = COUITheme.colorScheme.surfaceContainer,
    contentColor = COUITheme.colorScheme.onSurfaceContainerVariant,
    titleContentColor = COUITheme.colorScheme.onSurfaceContainer,
    actionContentColor = COUITheme.colorScheme.primary,
)
```

`RichTooltipColors` is a data class of `containerColor`, `contentColor`, `titleContentColor`, and `actionContentColor`.

### Caret

```kotlin
fun TooltipDefaults.caretShape(): Shape
val TooltipDefaults.caretSize: DpSize // 16 x 8 dp
```

The bubble indicator is on by default. It is the `_sharp_` arrow of `COUIToolTips`: the centred 20×10.1dp `coui_*_sharp_middle_arrow_up` when the anchor is clear of the bubble's corners, and the 16.27×10.29dp `coui_*_sharp_edges_arrow_up_right` near an end. Its base curves (5.19dp / 7.92dp) follow the 17dp bubble corner, the tip is a 2dp radius, and the arrow overlaps the bubble edge by `CaretEmbedDistance` (0.33dp) so the silhouettes merge. It rotates to whichever side faces the anchor (`Above` / `Below` / `Left` / `Right`) and mirrors toward the nearer end. Pass `caretShape = null` to keep the caret-less Miuix bubble.

### Constants

| Constant Name              | Type          | Description                              | Default Value                                      |
| -------------------------- | ------------- | ---------------------------------------- | -------------------------------------------------- |
| SpacingBetweenTooltipAndAnchor | Dp        | Spacing between a tooltip and its anchor | 8.dp                                               |
| HorizontalEdgeMargin           | Dp            | Minimum gap to the left and right window edges | 16.dp                                              |
| PlainTooltipMaxWidth       | Dp            | Maximum width of a plain tooltip         | 200.dp                                             |
| PlainTooltipCornerRadius   | Dp            | Corner radius of a plain tooltip         | 17.dp                                              |
| PlainTooltipInsideMargin   | PaddingValues | Inner padding of a plain tooltip         | PaddingValues(all = 12.dp)                         |
| CaretWidth                 | Dp            | Width of the centred bubble indicator     | 20.dp                                              |
| CaretHeight                | Dp            | Height of the centred bubble indicator    | 10.1.dp                                            |
| CaretEdgeWidth             | Dp            | Width of the end bubble indicator         | 16.27.dp                                           |
| CaretEdgeHeight            | Dp            | Height of the end bubble indicator        | 10.29.dp                                           |
| CaretEmbedDistance         | Dp            | Overlap between the indicator and the bubble edge | 0.33.dp                                    |
| CaretEdgeExtraDistance     | Dp            | Clearance the end indicator keeps from the bubble end | 2.dp                                  |
| CaretPadding               | Dp            | Space the bubble leaves for the indicator | 9.96.dp                                            |
| RichTooltipMaxWidth        | Dp            | Maximum width of a rich tooltip          | 320.dp                                             |
| RichTooltipCornerRadius    | Dp            | Corner radius of a rich tooltip          | 17.dp                                              |
| RichTooltipInsideMargin    | PaddingValues | Inner padding of a rich tooltip          | PaddingValues(all = 16.dp)                         |
| RichTooltipActionCornerRadius | Dp         | Corner radius of a rich tooltip action button | 8.dp                                          |
| RichTooltipActionInsideMargin | PaddingValues | Inner padding of a rich tooltip action button | PaddingValues(horizontal = 12.dp, vertical = 6.dp) |
| caretSize                  | DpSize        | Size of the caret                        | DpSize(16.dp, 8.dp)                                |

## Convenience overloads

For the common cases you can skip the position provider and content composables.

### Plain tooltip

```kotlin
@Composable
fun TooltipBox(
    text: String,
    modifier: Modifier = Modifier,
    state: TooltipState = rememberTooltipState(isPersistent = false),
    enabled: Boolean = true,
    positioning: TooltipAnchorPosition = TooltipAnchorPosition.Below,
    containerColor: Color = TooltipDefaults.plainTooltipContainerColor,
    contentColor: Color = TooltipDefaults.plainTooltipContentColor,
    content: @Composable () -> Unit,
)
```

### Rich tooltip

```kotlin
@Composable
fun RichTooltipBox(
    text: String,
    modifier: Modifier = Modifier,
    state: TooltipState = rememberTooltipState(isPersistent = true),
    title: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    positioning: TooltipAnchorPosition = TooltipAnchorPosition.Below,
    colors: RichTooltipColors = TooltipDefaults.richTooltipColors(),
    content: @Composable () -> Unit,
)
```

`RichTooltipBox` is persistent and focusable. Trigger it via long press / hover, or hoist `state` and call `state.show()` from the anchor's own click. The action invokes `onActionClick` then dismisses the tooltip.

```kotlin
val richState = rememberTooltipState(isPersistent = true)
val scope = rememberCoroutineScope()

RichTooltipBox(
    title = "Rich tooltip",
    text = "Rich tooltips support a title, supporting text, and an action.",
    actionText = "Got it",
    onActionClick = { /* ... */ },
    state = richState,
) {
    TextButton(
        text = "Show rich tooltip",
        onClick = { scope.launch { richState.show() } },
    )
}
```
