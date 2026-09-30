// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.MarqueeText
import io.github.suqi8.coui.kmp.basic.NotificationSnackBar
import io.github.suqi8.coui.kmp.basic.NotificationSnackBarHost
import io.github.suqi8.coui.kmp.basic.NotificationSnackBarHostState
import io.github.suqi8.coui.kmp.basic.NotificationSnackBarImageType
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.TagView
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Email
import io.github.suqi8.coui.kmp.icon.extended.Photos
import io.github.suqi8.coui.kmp.icon.extended.WorldClock
import kotlinx.coroutines.launch

fun LazyListScope.legacyComponentSection() {
    item(key = "tagView") {
        SmallTitle(text = "TagView")
        TagViewDemo()
    }
    item(key = "notificationSnackBar") {
        SmallTitle(text = "NotificationSnackBar")
        NotificationSnackBarDemo()
    }
    item(key = "marqueeTextView") {
        SmallTitle(text = "MarqueeText")
        MarqueeTextDemo()
    }
}

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
            TagView(text = "NEW", cornerRadius = 4.dp)
            TagView(text = "BETA", cornerRadius = 4.dp)
            TagView(text = "9.9.9", cornerRadius = 4.dp)
        }
    }
}

/** Renders the "NotificationSnackBar" gallery section content: sample notifications plus trigger buttons and a snackbar host. */
@Composable
fun NotificationSnackBarDemo() {
    val hostState = remember { NotificationSnackBarHostState() }
    val scope = rememberCoroutineScope()

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
                    title = "New message",
                    subtitle = "Square image, 44dp",
                    icon = COUIIcons.Email,
                    imageType = NotificationSnackBarImageType.Square,
                    actionText = "Open",
                    onAction = { },
                    onClose = { },
                )
                NotificationSnackBar(
                    title = "Now playing",
                    subtitle = "Vertical image, 36dp x 48dp",
                    icon = COUIIcons.Photos,
                    imageType = NotificationSnackBarImageType.Vertical,
                    actionText = "Pause",
                    onAction = { },
                    onClose = { },
                )
                NotificationSnackBar(
                    title = "Alarm set",
                    subtitle = "Horizontal image, 58dp x 44dp",
                    icon = COUIIcons.WorldClock,
                    imageType = NotificationSnackBarImageType.Horizontal,
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
                    text = "Show",
                    onClick = {
                        scope.launch {
                            hostState.showNotification(
                                title = "Charging",
                                subtitle = "Battery is at 80%",
                                icon = COUIIcons.WorldClock,
                                actionText = "Details",
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = "Show vertical",
                    onClick = {
                        scope.launch {
                            hostState.showNotification(
                                title = "New photo",
                                subtitle = "Tap to view the album",
                                icon = COUIIcons.Photos,
                                imageType = NotificationSnackBarImageType.Vertical,
                                actionText = "View",
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

/** Renders the "MarqueeText" gallery section content: long and short marquee text samples. */
@Composable
fun MarqueeTextDemo() {
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
                text = "This sentence is far too long for the box it is given",
                modifier = Modifier.width(160.dp),
            )
            MarqueeText(
                text = "Short text",
                modifier = Modifier.width(160.dp),
            )
        }
    }
}
