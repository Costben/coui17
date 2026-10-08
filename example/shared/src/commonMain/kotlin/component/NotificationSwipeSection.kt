// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

package component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.NotificationSwipeBox
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.basic.rememberNotificationSwipeState
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Delete
import io.github.suqi8.coui.kmp.icon.extended.Email
import io.github.suqi8.coui.kmp.icon.extended.Settings
import io.github.suqi8.coui.kmp.shared.generated.resources.Res
import io.github.suqi8.coui.kmp.shared.generated.resources.blur_test_bg
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import utils.WallpaperState
import utils.rememberBlurBackdrop

/** An interactive notification over the same wallpaper backdrop as the melt-ball demo. */
@Composable
fun NotificationSwipeDemo() {
    val state = rememberNotificationSwipeState()
    val scope = rememberCoroutineScope()
    val backdrop = rememberBlurBackdrop()
    var deleted by remember { mutableStateOf(false) }
    var settingsSelected by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.matchParentSize().then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)) {
            WallpaperState.bitmap?.let {
                Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } ?: Image(painterResource(Res.drawable.blur_test_bg), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        CompositionLocalProvider(LocalBackdrop provides backdrop) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(str("Swipe left to reveal notification actions"))
                if (!deleted) {
                    NotificationSwipeBox(
                        onSettings = { settingsSelected = true },
                        onDismiss = { deleted = true },
                        settingsIcon = COUIIcons.Settings,
                        dismissIcon = COUIIcons.Delete,
                        state = state,
                        minHeight = 0.dp,
                        settingsDescription = str("Settings"),
                        dismissDescription = str("Delete"),
                    ) {
                        Row(
                            // The compact reference notification measured about 60dp in the device hierarchy.
                            modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(COUIIcons.Email, null, Modifier.size(36.dp))
                            Column {
                                Text(str("New message"), style = COUITheme.textStyles.body2)
                                Text(str("Drag slowly to see the melt separate"), style = COUITheme.textStyles.footnote2)
                            }
                        }
                    }
                } else {
                    Text(str("Notification deleted"))
                }
                if (settingsSelected) Text(str("Notification settings selected"))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(
                        text = str("Reveal actions"),
                        enabled = !deleted,
                        onClick = { scope.launch { state.reveal() } },
                    )
                    TextButton(
                        text = str("Reset"),
                        onClick = {
                            scope.launch {
                                state.close()
                                deleted = false
                                settingsSelected = false
                            }
                        },
                    )
                }
            }
        }
    }
}
