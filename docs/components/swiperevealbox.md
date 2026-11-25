# SwipeRevealBox

A general-purpose basic swipe container with ColorOS 17 liquid-melt (`MetaBallSurface`) left-swipe action reveal.

Unlike [NotificationSwipeBox](./notificationswipebox.md), `SwipeRevealBox` is decoupled from SystemUI notification shade visuals. By default it uses `SwipeRevealDefaults.cardStyle()`, matching a standard COUI [Card](./card.md): `17.dp` Lp `3.0` squircle corners, opaque `surfaceContainer` fill, no backdrop blur, no glass rim highlight, compact `48.dp × 48.dp` centered floating action buttons, and critically damped settle physics so slow drags never over-bounce.

Every visual, geometric, and motion trait (corner radius, action corner radius, corner curve family, fill color, glass blur, edge rim, button sizing, gaps, follow distance, and settle/recoil springs) lives in `SwipeRevealStyle` and can be customized or turned off independently.

## Usage

Import `SwipeRevealBox`, `SwipeRevealAction`, `SwipeRevealDefaults`, and `rememberSwipeRevealState` from `io.github.suqi8.coui.kmp.basic`, and opt in to `ExperimentalCOUIApi`.

```kotlin
@OptIn(ExperimentalCOUIApi::class)
@Composable
fun SwipeableCardRow(onSettings: () -> Unit, onDelete: () -> Unit) {
    val state = rememberSwipeRevealState()
    SwipeRevealBox(
        actions = listOf(
            SwipeRevealAction(
                icon = COUIIcons.Settings,
                description = "Settings",
                onClick = onSettings,
            ),
            SwipeRevealAction(
                icon = null, // built-in animated NotificationTrashIcon
                description = "Delete",
                color = COUITheme.colorScheme.error,
                onClick = onDelete,
            ),
        ),
        state = state,
        dismissOnFullSwipe = true,
    ) {
        BasicComponent(
            title = "Meeting notes.m4a",
            summary = "12.4 MB · Today 10:20",
        )
    }
}
```

Supply foreground content without an opaque background so it does not cover the melt silhouette.

## Style Presets & Button Variants

`SwipeRevealDefaults` ships five composable presets whose parameters can all be overridden individually:

| Preset | Card corner | Button geometry | Blur / Rim | Motion |
| --- | --- | --- | --- | --- |
| `SwipeRevealDefaults.cardStyle()` | `17.dp` Lp `3.0` squircle (`CardDefaults.CornerRadius`) | Compact `48.dp × 48.dp`, `14.dp` corner radius, centered | Both `false` | Critically damped settle (`1.0`, `320f`), gentle `6.dp` follow |
| `SwipeRevealDefaults.circleActionStyle()` | `17.dp` Lp `3.0` squircle | COUI-Kit droplet `44.dp × 44.dp` circle (`22.dp` radius), `10.dp` gap | Both `false` | Critically damped settle, gentle `6.dp` follow |
| `SwipeRevealDefaults.roundRectActionStyle()` | `17.dp` Lp `3.0` squircle | Upstream `SwipeActionItem`-style `68.dp × 52.dp`, `12.dp` radius, `6.dp` gap | Both `false` | Critically damped settle, gentle `6.dp` follow |
| `SwipeRevealDefaults.groupCardStyle()` | `24.dp` Lp `2.5` squircle (`CardDefaults.GroupCornerRadius`) | Compact `48.dp × 48.dp`, `14.dp` corner radius, centered | Both `false` | Critically damped settle, gentle `6.dp` follow |
| `SwipeRevealDefaults.notificationStyle()` | `16.dp` `CONIC` (`weight = 1.1`, `24.dp` Bezier span) | `60.dp × 68.dp` top-aligned (`8.dp` inset), `10.dp` gap, `4.dp` end padding | Both `true` | SystemUI notification springs (`0.75`, `200f`) |

### Colored pills, text labels, and icon + text buttons

Each `SwipeRevealAction` can provide its own `containerColor` (with smooth liquid-melt color blending between the card and buttons), `label`, and/or `icon`:

```kotlin
SwipeRevealBox(
    actions = listOf(
        SwipeRevealAction(
            icon = COUIIcons.Pin,
            label = "Pin",
            containerColor = COUITheme.colorScheme.primary,
            color = Color.White,
            onClick = onPin,
        ),
        SwipeRevealAction(
            label = "Delete",
            useTrashIcon = true,
            containerColor = COUITheme.colorScheme.error,
            color = Color.White,
            onClick = onDelete,
        ),
    ),
    style = SwipeRevealDefaults.roundRectActionStyle(),
) {
    // Foreground content
}
```

### COUI-Kit two-step confirmation button

Set `requireConfirmation = true` so tapping once switches the button glyph into a confirmation checkmark (`✓`), and tapping a second time executes `onClick` (closing the row resets confirmation):

```kotlin
SwipeRevealBox(
    actions = listOf(
        SwipeRevealAction(
            icon = null,
            description = "Delete",
            confirmDescription = "Confirm delete",
            color = COUITheme.colorScheme.error,
            requireConfirmation = true,
            onClick = onDelete,
        ),
    ),
    style = SwipeRevealDefaults.circleActionStyle(),
    dismissOnFullSwipe = true,
) {
    // Foreground content
}
```

## State & Interaction

- `SwipeRevealState` provides `revealFraction` (`0f..1f`), `isRevealed`, `suspend fun reveal()`, and `suspend fun close()`, and saves its target state across recreation via `rememberSwipeRevealState()`.
- Supply up to 3 actions in visual left-to-right order: the last action sits at the trailing edge and is revealed first.
- With `dismissOnFullSwipe = true`, pulling `SwipeRevealDefaults.MergeDistance` (`40.dp`) past the revealed menu merges the primary cell across secondary cells, triggers haptic feedback, opens the trash lid (when `useTrashIcon = true`), and sweeps the row out on release.
- For classic ColorOS list swipe rows without liquid-melt physics, see [SlideView](./slideview.md).
