# NotificationSwipeBox

<div style="position: relative; height: 200px; border-radius: 10px; overflow: hidden; border: 1px solid #777;">
    <iframe id="demoIframe" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;" src="../compose/index.html?id=notificationSwipeBox" title="Demo"></iframe>
</div>

An experimental notification container with a ColorOS 17 left-swipe menu. The card and both action backgrounds share a melt-ball silhouette: pulling the card left stretches their connection until the settings and delete buttons separate. The notification foreground slides inside a rounded visible-card mask.

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

Use foreground content without an opaque card background. Provide a sibling wallpaper through `LocalBackdrop` for the existing glass rendering; without a backdrop the material is opaque. Below Android 13, separate opaque shapes replace the shader bridge. Menu colors use the shared theme material; wallpaper tint and lighting reuse `MetaBallSurface` rather than the SystemUI-specific material recipe.

## State and interaction

`state.reveal()` and `state.close()` are suspending spring animations. `revealFraction` exposes the current 0–1 visual progress; `isRevealed` exposes the destination. The destination is saved across recreation. A drag interrupts an animation. A short slow drag returns closed, while a long drag or a fling settles open. Dragging right closes the menu. The gesture is a physical left swipe even with RTL foreground content.

The menu stops at 144dp. Deletion requires the explicit delete action; the system's further-swipe merge-to-delete phase is not part of this component. `onDismiss` lets the caller remove the notification. Each button starts closing the menu after activation. `enabled = false` disables dragging and actions. Covered buttons have no clickable or accessibility nodes; settings and delete are also exposed as custom accessibility actions. Pass localized button descriptions.

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
| Icon fade interval | 0.6–1.0 of cell exposure | `AnimateParamOne.FADE_START/END` |
| Recoil spring | response 0.55s, bounce 0.4 | `AnimateParamOne.oneAnimate` |

The container needs a bounded width greater than 144dp for dragging. Tall notifications keep the actions 8dp below the top, matching the system menu.

`rimColor` is the light traced along the merged outline. A shade card's top and bottom edges measure `#4E4E4E` over a `#2D2D2D` body, which needs opaque white; the default half-transparent white suits a card floating on a bright wallpaper instead.

Set `minHeight = 0.dp` to size compact notifications from their content; action height then follows the card height minus 16dp, up to 68dp.

In the example app, open Settings → NotificationSwipeBox to try the swipe, settings, delete and reset actions.
