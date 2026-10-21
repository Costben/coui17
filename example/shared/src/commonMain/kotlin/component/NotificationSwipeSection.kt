// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

package component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.NotificationSwipeBox
import io.github.suqi8.coui.kmp.basic.NotificationSwipeState
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.basic.rememberNotificationSwipeState
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Delete
import io.github.suqi8.coui.kmp.icon.extended.Messages
import io.github.suqi8.coui.kmp.icon.extended.Notes
import io.github.suqi8.coui.kmp.icon.extended.Photos
import io.github.suqi8.coui.kmp.icon.extended.Settings
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.launch
import utils.rememberBlurBackdrop

/** Shade fill measured on ColorOS 17 (`#111111`). */
private val ShadeColor = Color(0xFF111111)

/**
 * Card fill measured on ColorOS 17 (`#2D2D2D` over the shade). The surface blends its tint over
 * the backdrop, so the tint is set eight levels lighter to land on that measured fill.
 */
private val CardColor = Color(0xFF353535)

/** `notification_info_text_size` renders at 13sp in a muted 62% white. */
private val InfoColor = Color(0xFF9F9F9F)

/** The blurred wallpaper that sits behind the shade's top edge. */
private val ShadeTopGlow = listOf(Color(0xFF3F4644), Color(0x00111111))

/**
 * The shade's four colours.
 *
 * The dark values are measured on a ColorOS 17 device. The light shade is a translucent card over
 * the wallpaper, so it has no fixed fill to measure; its colours come from the light theme instead.
 */
private class ShadeColors(val shade: Color, val card: Color, val text: Color, val info: Color) {
    val dark: Boolean get() = shade.luminance() < 0.5f
}

@Composable
private fun rememberShadeColors(): ShadeColors {
    val colors = COUITheme.colorScheme
    return remember(colors.background) {
        if (colors.background.luminance() < 0.5f) {
            ShadeColors(ShadeColor, CardColor, Color.White, InfoColor)
        } else {
            ShadeColors(colors.surface, colors.surfaceVariant, colors.onSurface, colors.onSurfaceVariantSummary)
        }
    }
}

private data class DemoNotification(
    val icon: ImageVector,
    val iconColor: Color,
    val title: String,
    val body: String,
    val time: String,
    val bodyLines: Int,
)

/**
 * Notifications rebuilt to the ColorOS 17 shade: a `#111111` backdrop, `#2D2D2D` cards 8dp apart,
 * 32dp rounded app icons, one 14sp white line for both the title and the body, and a 12sp muted
 * timestamp on the first line. The light theme takes the shade and card from the theme, because the
 * device's light shade is a translucent card over the wallpaper and has no fixed fill to measure.
 */
@Composable
fun NotificationSwipeDemo() {
    val backdrop = rememberBlurBackdrop()
    val scope = rememberCoroutineScope()
    val shade = rememberShadeColors()
    val firstState = rememberNotificationSwipeState()
    val secondState = rememberNotificationSwipeState()
    val thirdState = rememberNotificationSwipeState()
    val states = listOf(firstState, secondState, thirdState)
    var dismissed by remember { mutableStateOf(setOf<Int>()) }
    var settingsSelected by remember { mutableStateOf(false) }

    val notifications = listOf(
        DemoNotification(
            icon = COUIIcons.Messages,
            iconColor = Color(0xFF1777FD),
            title = str("Design review moved to 10:00 tomorrow"),
            body = str("Room changed to 3F, Building A"),
            time = str("Just now"),
            bodyLines = 1,
        ),
        DemoNotification(
            icon = COUIIcons.Notes,
            iconColor = Color(0xFFFF7700),
            title = str("This week's update is ready"),
            body = str("Adds full-page pull to refresh, notification swipe actions and the search capsule"),
            time = "09:41",
            bodyLines = 2,
        ),
        DemoNotification(
            icon = COUIIcons.Photos,
            iconColor = Color(0xFF34C759),
            title = str("Photos backed up"),
            body = str("128 items, 2.4 GB in total"),
            time = str("Yesterday"),
            bodyLines = 1,
        ),
    )

    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.matchParentSize().then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)) {
            Box(Modifier.fillMaxSize().background(shade.shade))
            // The blurred wallpaper the shade reveals above its first card.
            if (shade.dark) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(88.dp)
                        .background(Brush.verticalGradient(ShadeTopGlow)),
                )
            }
        }
        CompositionLocalProvider(LocalBackdrop provides backdrop) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 88.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                notifications.forEachIndexed { index, item ->
                    if (index !in dismissed) {
                        DemoNotificationCard(
                            item = item,
                            state = states[index],
                            shade = shade,
                            onSettings = { settingsSelected = true },
                            onDismiss = { dismissed = dismissed + index },
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(str("Swipe left to reveal notification actions"), color = shade.info)
                if (settingsSelected) Text(str("Notification settings selected"), color = shade.info)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(
                        text = str("Reveal actions"),
                        enabled = dismissed.size < notifications.size,
                        onClick = { scope.launch { states.firstOrNull { !it.isRevealed }?.reveal() } },
                    )
                    TextButton(
                        text = str("Reset"),
                        onClick = {
                            scope.launch {
                                states.forEach { it.close() }
                                dismissed = emptySet()
                                settingsSelected = false
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DemoNotificationCard(
    item: DemoNotification,
    state: NotificationSwipeState,
    shade: ShadeColors,
    onSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    NotificationSwipeBox(
        onSettings = onSettings,
        onDismiss = onDismiss,
        settingsIcon = COUIIcons.Settings,
        dismissIcon = COUIIcons.Delete,
        state = state,
        minHeight = 0.dp,
        containerColor = shade.card,
        rimColor = Color.White,
        settingsDescription = str("Settings"),
        dismissDescription = str("Delete"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
            // The app icon centres on the whole card, so a three-line notification does not leave
            // the icon hanging under the top edge.
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.5.dp)).background(item.iconColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(item.icon, null, Modifier.size(24.dp), tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = item.title,
                        modifier = Modifier.weight(1f),
                        style = COUITheme.textStyles.body2.copy(lineHeight = 16.5.sp, fontWeight = FontWeight.Medium),
                        color = shade.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = item.time,
                        // 13sp renders 8% wider than the shade's timestamp; 12sp matches its ink.
                        style = COUITheme.textStyles.footnote1.copy(fontSize = 12.sp, lineHeight = 16.5.sp),
                        color = shade.info,
                        maxLines = 1,
                    )
                }
                Text(
                    text = item.body,
                    style = COUITheme.textStyles.body2.copy(
                        lineHeight = 20.sp,
                        // The shade centres the body inside its taller line box.
                        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
                    ),
                    color = shade.text,
                    maxLines = item.bodyLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
