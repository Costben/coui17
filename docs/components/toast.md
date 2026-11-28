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

## Fixed Position Across Immersive Pages & Keyboard Handling

By default (`applySystemInsets = true`, `lockNavigationBarInsets = true`), `ToastHost` locks the non-immersive navigation-bar height so toasts always appear at the exact same vertical position across regular screens and immersive screens (such as image/video viewers using `CouiSystemBarsEffect(navigationBarVisible = false)`), even if the first toast is triggered while the navigation bar is hidden or animating.

When the software keyboard (IME) opens, `ToastHost` computes:

```kotlin
max(lockedNavigationBarBottom + contentPadding.bottom, imeBottom + imeBottomPadding)
```

This avoids both double-counting the navigation bar and floating tall bottom-bar offsets above the keyboard. For an app with a floating bottom bar, pass only `contentPadding`:

```kotlin
ToastHost(
    state = toastState,
    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 82.dp),
)
```

While idle (`state.hasActiveToast == false`), `ToastHost` never subscribes to `WindowInsets.ime`, keeping keyboard open/close animations free of extra recompositions.

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
| `applySystemInsets` | `Boolean` | Applies navigation-bar and IME insets while active | `true` |
| `lockNavigationBarInsets` | `Boolean` | Locks non-immersive navigation-bar height across immersive pages and system-bar animations | `true` |
| `imeBottomPadding` | `Dp` | Clearance above the software keyboard when IME lifts the toast past its resting position | `ToastDefaults.ImeBottomPadding` (`24.dp`) |
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
