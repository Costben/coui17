# Toast

`Toast` is a ColorOS 17 native-style capsule toast (`OplusToastLayout` + `OplusToastDefaultAnimation` + `OplusToastUI`) for brief, non-stacking bottom feedback with optional lightweight actions.

## Import

```kotlin
import io.github.suqi8.coui.kmp.basic.Toast
import io.github.suqi8.coui.kmp.basic.ToastColors
import io.github.suqi8.coui.kmp.basic.ToastData
import io.github.suqi8.coui.kmp.basic.ToastDefaults
import io.github.suqi8.coui.kmp.basic.ToastHost
import io.github.suqi8.coui.kmp.basic.ToastHostState
import io.github.suqi8.coui.kmp.basic.rememberToastHostState
```

## Basic Usage

Keep `ToastHost` mounted in your root overlay. When `state.hasActiveToast` is `false`, `ToastHost` returns immediately without measuring a full-screen container or subscribing to `WindowInsets.ime`.

```kotlin
val toastState = rememberToastHostState()

Box(Modifier.fillMaxSize()) {
    // App content
    TextButton(
        text = "Save",
        onClick = { toastState.showToast("Saved to gallery") },
    )

    ToastHost(state = toastState)
}
```

## Lightweight Action & Dismiss

`showToast` supports an optional `actionLabel` / `onAction` and a close icon (`withDismissAction = true`):

```kotlin
toastState.showToast(
    message = "Clipboard link detected",
    durationMillis = 6_000L,
    actionLabel = "Open",
    withDismissAction = true,
    onAction = { openLink() },
)
```

## Skipping `WindowInsets.ime` When Idle

If your layout computes dynamic bottom offsets based on keyboard visibility, guard the `WindowInsets.ime` read with `state.hasActiveToast`:

```kotlin
val bottomPadding = if (toastState.hasActiveToast) {
    val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    if (!imeOpen && hasFloatingBottomBar) 82.dp else 44.dp
} else {
    44.dp
}

ToastHost(
    state = toastState,
    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = bottomPadding),
)
```

## Properties

### ToastHostState

| Member | Type / Signature | Description |
| --- | --- | --- |
| `currentToast` | `ToastData?` | Currently active toast (including enter/exit animation), or `null` |
| `hasActiveToast` | `Boolean` | Whether a toast is currently active |
| `showToast(...)` | `(message, durationMillis = 2000L, actionLabel = null, withDismissAction = false, onAction = null)` | Enqueues a toast; interrupts any active toast in place and coalesces rapid updates during exit |
| `dismiss()` | `() -> Unit` | Plays the exit animation and clears the active toast |

### ToastHost

| Property | Type | Description | Default Value |
| --- | --- | --- | --- |
| `state` | `ToastHostState` | State controlling toast presentation | Required |
| `modifier` | `Modifier` | Modifier applied to the host overlay while active | `Modifier` |
| `contentAlignment` | `Alignment` | Toast alignment inside the host | `Alignment.BottomCenter` |
| `contentPadding` | `PaddingValues` | Outer padding around the toast | `ToastDefaults.HostContentPadding` |
| `applySystemInsets` | `Boolean` | Applies `navigationBarsPadding()` and `imePadding()` while active | `true` |
| `toast` | `@Composable (ToastData) -> Unit` | Custom toast renderer | `{ Toast(it, onDismiss = state::dismiss) }` |

### Toast

| Property | Type | Description | Default Value |
| --- | --- | --- | --- |
| `message` | `String` | Message text (up to 2 lines with ellipsis) | Required |
| `modifier` | `Modifier` | Modifier applied to the capsule | `Modifier` |
| `actionLabel` | `String?` | Optional trailing action label | `null` |
| `withDismissAction` | `Boolean` | Whether to render a trailing close icon | `false` |
| `onAction` | `(() -> Unit)?` | Callback when the action or capsule is clicked | `null` |
| `onDismiss` | `(() -> Unit)?` | Callback when dismissed | `null` |
| `colors` | `ToastColors` | Container, text, action, close, and shadow colors | `ToastDefaults.colors()` |
| `elevation` | `Dp` | Capsule shadow elevation | `ToastDefaults.Elevation` (`12.dp`) |
| `dismissContentDescription` | `String` | Accessibility label for the close icon | `ToastDefaults.DismissContentDescription` |
