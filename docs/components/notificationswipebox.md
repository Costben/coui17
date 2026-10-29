# NotificationSwipeBox

<div style="position: relative; height: 200px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../compose/index.html?id=notificationSwipeBox" title="Demo"></iframe>
</div>

An experimental notification container with a ColorOS 17 left-swipe menu. The card and both action backgrounds share a melt-ball silhouette: pulling the card left stretches their connection until the settings and delete buttons separate. The notification card keeps its left edge while its right edge retreats ahead of the menu, and its content slides left with the finger. Pass `onSettings = null` and `settingsIcon = null` for a delete-only notification: the menu narrows to one cell.

## Usage

Import `NotificationSwipeBox` and `rememberNotificationSwipeState` from `io.github.suqi8.coui.kmp.basic`, and opt in to `ExperimentalCOUIApi`. The sample icons come from `coui-icons`.

```kotlin
@OptIn(ExperimentalCOUIApi::class)
@Composable
fun Notification() {
    val state = rememberNotificationSwipeState()
    NotificationSwipeBox(
        onSettings = { /* Open notification settings */ },
        onDismiss = { /* Remove the notification from your list */ },
        settingsIcon = COUIIcons.Settings,
        dismissIcon = COUIIcons.Delete,
        state = state,
        settingsDescription = "Settings",
        dismissDescription = "Delete",
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("New message")
            Text("Swipe left to reveal actions")
        }
    }
}
```

For a delete-only notification, pass `null` for both settings parameters; the menu narrows to one 74dp cell and the delete button still sits against the outer padding:

```kotlin
NotificationSwipeBox(
    onSettings = null,
    onDismiss = { /* Remove this row from the list */ },
    settingsIcon = null,
    dismissIcon = null, // the built-in NotificationTrashIcon
    state = state,
    dismissOnFullSwipe = true,
    dismissDescription = "Delete",
) {
    // Foreground content
}
```

Use foreground content without an opaque card background. Provide a sibling wallpaper through `LocalBackdrop` for the existing glass rendering; without a backdrop the material is opaque. Below Android 13, separate opaque shapes replace the shader bridge. Menu colors use the shared theme material; wallpaper tint and lighting reuse `MetaBallSurface` rather than the SystemUI-specific material recipe.

## State and interaction

`state.reveal()` and `state.close()` are suspending spring animations. `revealFraction` exposes the current 0–1 visual progress; `isRevealed` exposes the destination. The destination is saved across recreation. A drag interrupts an animation. A short slow drag returns closed, while a long drag or a fling settles open. Dragging right closes the menu. The gesture is a physical left swipe even with RTL foreground content.

The menu stops at 144dp for two cells and 74dp for one — each cell is 60dp plus a 10dp gap, followed by the 4dp outer padding. Deletion is either the explicit delete action or, with `dismissOnFullSwipe`, a full swipe. Throughout the swipe the card keeps its left edge while its content slides left with the finger inside it. Past the menu the card keeps following the finger and the delete cell stretches into the space it leaves, carrying the settings cell along. `NotificationSwipeDefaults.MergeDistance` (40dp) past the menu the delete cell glides across the settings cell, which shrinks to a fifth of its size and melts into it; dragging back inside that distance springs them apart again. That point is `NotificationSwipeDefaults.dismissThreshold` (the menu plus 40dp): crossing it plays a haptic tick, backing out ticks again, and once the tick has been felt a release always deletes. The bin itself only lifts its lid; it does not wobble. With `dismissIcon = null` the delete cell draws the built-in `NotificationTrashIcon`, the paths and lid keyframes of SystemUI's `notification_menu_delete_icon_lottie`: the lid pivots on its right end and lifts its left end open in one 150ms move at that point, without tracking the finger, and it closes over 200ms as the row goes; a custom icon stays rigid. Nothing is deleted while the finger is down. A deleting release sweeps the row off with SwipeHelper's timing — 200ms speeding up for a plain release, the finger's own speed for a fling and never slower — with no spring tail, and `onDismiss` is called on the frame the row leaves; the state resets to closed so the caller can remove the row. A release short of the threshold springs back to the menu, whatever its velocity. Each button starts closing the menu after activation. `enabled = false` disables dragging and actions. Covered buttons have no clickable or accessibility nodes; settings and delete are also exposed as custom accessibility actions. Pass localized button descriptions.

## Source values

ColorOS 17 notification menu resources and animation parameters:

| Value | Default | Source |
| --- | --- | --- |
| Action width | 60dp | `notification_menu_icon_with_background_width` |
| Action height | 68dp | `notification_menu_icon_with_background_standard_height` |
| Action top inset | 8dp | `notification_menu_icon_with_background_standard_height_diff` |
| Default minimum notification height | 84dp | `notification_menu_icon_with_background_parent_standard_height` |
| Gap / end inset | 10dp / 4dp | `notification_menu_icon_margin` / `notification_menu_icon_margin_extra` |
| Card radius | 20dp | `notification_corner_radius` 16dp continuous corner, fitted as circular |
| Button radius | 16dp | `RadiusParams.CARD_MENU_CONER` / `notif_menu_icon_background` |
| Blend range / factors | 3.5dp / 1.2 / 2.0 | notification melt-ball controllers |
| Follow distance | 8dp | stage-one settings and delete distances |
| Merge distance | 40dp | `AnimateParamTwo.TWO_ANIMATE_THRESHOLD` (its comment reads "0-158.4px (40dp)") |
| Lid opening distance | 12.8dp | stage two `getDeleteDistance` |
| Merge spring | response 0.3s, bounce 0.5 | `AnimateParamTwo.twoAnimatedelete/Setting` |
| Dismiss threshold | menu + 40dp, where the haptic tick plays | the merge, where `NotificationMenuRow.onTouchMove` ticks |
| Dismiss sweep | 200ms accelerating; a fling at the finger's speed | `SwipeHelper` / `FlingAnimationUtils.applyDismissing` |
| Trash icon | 84×84 at 60fps, shown at 28dp; lid -18° → 0° → -18° | `notification_menu_delete_icon_lottie_*_final.json` |
| Delete fade / lid close | response 0.3s, bounce 0.5 / 200ms | `AnimateParamThree` |
| Icon fade interval | 0.6–1.0 of cell exposure | `AnimateParamOne.FADE_START/END` |
| Recoil spring | response 0.55s, bounce 0.4 | `AnimateParamOne.oneAnimate` |

The container needs a bounded width greater than the menu width (144dp for two cells, 74dp for one) to drag. Tall notifications keep the actions 8dp below the top, matching the system menu.

`rimColor` is the light traced along the merged outline. A shade card's top and bottom edges measure `#4E4E4E` over a `#2D2D2D` body, which needs opaque white; the default half-transparent white suits a card floating on a bright wallpaper instead.

Set `minHeight = 0.dp` to size compact notifications from their content; action height then follows the card height minus 16dp, up to 68dp.

In the example app, open Settings → NotificationSwipeBox to try the swipe, settings, delete and reset actions.
