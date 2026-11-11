// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

package component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.MarqueeText
import io.github.suqi8.coui.kmp.basic.NotificationSnackBar
import io.github.suqi8.coui.kmp.basic.NotificationSnackBarHost
import io.github.suqi8.coui.kmp.basic.NotificationSnackBarHostState
import io.github.suqi8.coui.kmp.basic.NotificationSnackBarImageType
import io.github.suqi8.coui.kmp.basic.TagView
import io.github.suqi8.coui.kmp.basic.TagViewDefaults
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Email
import io.github.suqi8.coui.kmp.icon.extended.Photos
import io.github.suqi8.coui.kmp.icon.extended.WorldClock
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.launch

/** Renders the "TagView" gallery section content: three tag chips in a row. */
@Composable
fun TagViewDemo() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TagView(
                text = str("NEW"),
                cornerRadius = 4.dp,
                colors = TagViewDefaults.tagViewColors(
                    containerColor = COUITheme.colorScheme.primary,
                    contentColor = COUITheme.colorScheme.onPrimary,
                ),
            )
            TagView(
                text = str("BETA"),
                cornerRadius = 4.dp,
                colors = TagViewDefaults.tagViewColors(
                    containerColor = COUITheme.colorScheme.primaryContainer,
                    contentColor = COUITheme.colorScheme.onSurface,
                ),
            )
            TagView(
                text = "9.9.9",
                cornerRadius = 4.dp,
                colors = TagViewDefaults.tagViewColors(
                    containerColor = COUITheme.colorScheme.secondaryVariant,
                    contentColor = COUITheme.colorScheme.onSurface,
                ),
            )
        }
    }
}

/**
 * Renders the "NotificationSnackBar" gallery section content: one sample notification whose image
 * type comes from the options, plus a trigger and a snackbar host.
 */
@Composable
fun NotificationSnackBarDemo() {
    val hostState = remember { NotificationSnackBarHostState() }
    val scope = rememberCoroutineScope()
    val imageTypeIndex = remember { mutableIntStateOf(0) }
    val imageType = NotificationSnackBarImageType.entries[imageTypeIndex.intValue]
    val chargingTitle = str("Charging")
    val batteryIsAt80 = str("Battery is at 80%")
    val detailsAction = str("Details")

    DemoOptions(DemoChoice("Image type", NotificationSnackBarImageType.entries.map { it.name }, imageTypeIndex))
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                NotificationSnackBar(
                    title = str("New message"),
                    subtitle = str("Image type: {}", imageType.name),
                    icon = COUIIcons.Email,
                    imageType = imageType,
                    actionText = str("Open"),
                    onAction = { },
                    onClose = { },
                )
            }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    text = str("Show"),
                    onClick = {
                        scope.launch {
                            hostState.showNotification(
                                title = chargingTitle,
                                subtitle = batteryIsAt80,
                                icon = COUIIcons.WorldClock,
                                imageType = imageType,
                                actionText = detailsAction,
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            ) {
                NotificationSnackBarHost(
                    state = hostState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

/** Renders the "MarqueeText" gallery section content: one marquee whose text length comes from the option. */
@Composable
fun MarqueeTextDemo() {
    val longText = remember { mutableStateOf(true) }

    DemoOptions(DemoToggle("Long text", longText))
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MarqueeText(
                text = if (longText.value) {
                    str("This sentence is far too long for the box it is given")
                } else {
                    str("Short text")
                },
                modifier = Modifier.width(160.dp),
            )
        }
    }
}
