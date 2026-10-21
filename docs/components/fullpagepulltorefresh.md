# FullPagePullToRefresh

`FullPagePullToRefresh` is a full-page pull-to-refresh container modelled after the "Featured" tab of the OPPO App Market. Unlike the miuix-style [PullToRefresh](./pulltorefresh), the whole page follows the finger, a full-screen refresh layer is revealed underneath, and the hint text emerges from the top edge instead of a ball growing above the list.

::: warning
This component is only available in touch-enabled environments.
:::

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.FullPagePullToRefresh
import io.github.suqi8.coui.kmp.basic.rememberFullPagePullToRefreshState
```

## Basic Usage

```kotlin
var isRefreshing by rememberSaveable { mutableStateOf(false) }
val refreshState = rememberFullPagePullToRefreshState()
var items by remember { mutableIntStateOf(12) }
val contentPadding = PaddingValues(top = 72.dp)

LaunchedEffect(isRefreshing) {
    if (isRefreshing) {
        delay(1000)
        items += 4
        isRefreshing = false
    }
}

FullPagePullToRefresh(
    isRefreshing = isRefreshing,
    onRefresh = { isRefreshing = true },
    state = refreshState,
    contentPadding = contentPadding,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        items(items) { index ->
            Card(Modifier.fillMaxWidth()) {
                BasicComponent(title = "Resource ${index + 1}")
            }
        }
    }
}
```

::: tip The top padding is the reveal line
Pass the same top padding to the scrollable content that you pass as `contentPadding`. Its top value is used as the immersive height: the hint starts at that line and slides down with the page. If the content paints an opaque background above its first item, the hint will be hidden behind it.
:::

## Component States

1. `Idle`: no interaction, the page is at rest
2. `Pulling`: the page follows the finger with 0.5 linear damping
3. `ThresholdReached`: the pull passed the refresh threshold
4. `Refreshing`: the page is held at the threshold and the spinner rotates
5. `RefreshComplete`: the success hint is shown for 800ms before the page rebounds

## Properties

### FullPagePullToRefresh Properties

| Property Name               | Type                                     | Description                                              | Default Value                              | Required |
| --------------------------- | ---------------------------------------- | -------------------------------------------------------- | ------------------------------------------ | -------- |
| isRefreshing                | Boolean                                  | Hoisted refresh state                                    | None                                       | Yes      |
| onRefresh                   | () -> Unit                               | Called after a gesture-triggered pull settles            | None                                       | Yes      |
| modifier                    | Modifier                                 | Container modifier                                       | Modifier                                   | No       |
| state                       | FullPagePullToRefreshState               | Visual state holder                                      | rememberFullPagePullToRefreshState()       | No       |
| contentPadding              | PaddingValues                            | Content padding; its top is the hint reveal line         | PaddingValues(0.dp)                        | No       |
| topAppBarScrollBehavior     | ScrollBehavior?                          | Top app bar scroll behavior                              | null                                       | No       |
| hapticFeedbackEnabled       | Boolean                                  | Haptic tick when the threshold is crossed                | true                                       | No       |
| completeHoldDurationMillis  | Long                                     | How long the success hint stays visible                  | 800                                        | No       |
| colors                      | FullPagePullToRefreshColors              | Background, hint and spinner colors                      | fullPagePullToRefreshColors()              | No       |
| refreshThreshold            | Dp                                       | Pull distance that triggers a refresh                    | 93.187.dp                                  | No       |
| textStyle                   | TextStyle                                | Hint text style                                          | 14sp                                       | No       |
| refreshTexts                | FullPagePullToRefreshTexts              | Hint strings for the four states                         | English defaults                           | No       |
| background                  | (@Composable BoxScope.(State) -> Unit)?   | Full-screen layer behind the content                     | null                                       | No       |
| content                     | @Composable () -> Unit                   | Scrollable page content                                  | None                                       | Yes      |

### FullPagePullToRefreshState

| Property Name | Type                 | Description                                      |
| ------------- | -------------------- | ------------------------------------------------ |
| refreshState  | FullPageRefreshState | Current visual state                             |
| dragOffset    | Float                | Current page displacement in pixels              |
| pullProgress  | Float                | Pull progress from 0 to 1 relative to the threshold |

`startRefresh(onRefresh)` starts the refresh animation without a gesture. The caller still owns the hoisted `isRefreshing` flag.

### FullPagePullToRefreshDefaults

| Property Name              | Type                        | Description                            | Default Value |
| -------------------------- | --------------------------- | -------------------------------------- | ------------- |
| refreshThreshold           | Dp                          | App Market threshold                   | 93.187.dp     |
| spinnerSize                | Dp                          | Loading arc diameter                   | 26.dp         |
| textStyle                  | TextStyle                   | Hint text style                        | 14sp          |
| COMPLETE_HOLD_DURATION_MILLIS | Long                    | Success hint hold                      | 800           |
| refreshTexts               | FullPagePullToRefreshTexts  | Default hint strings                   | English       |

## Background Layer

The `background` slot is drawn full-screen behind the content and receives the state, so promotional artwork can fade or parallax with the pull:

```kotlin
FullPagePullToRefresh(
    // ...
    background = { state ->
        Image(
            painter = painterResource("refresh_bg.png"),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = state.pullProgress },
            contentScale = ContentScale.Crop,
        )
    },
) {
    // Content
}
```

## Animation Details

The defaults reproduce the market implementation:

- Damping: linear, `dragOffset = fingerDistance * 0.5`
- Release to threshold: `DecelerateInterpolator(2)`-style curve, duration scaled by distance up to 500ms
- Rebound when released below the threshold: same curve, duration scaled by distance up to 500ms
- Success hint: visible for 800ms after `isRefreshing` becomes `false`, then the page rebounds
- A new touch during the 800ms hold cancels it and rebounds immediately

## Notes

- The content must be scrollable and should consume the same top padding so the hint is not covered.
- While refreshing or holding the success hint, vertical nested scroll is consumed, so the page cannot be scrolled.
- `PullToRefresh` and `FullPagePullToRefresh` are independent components; use one of them per page.
