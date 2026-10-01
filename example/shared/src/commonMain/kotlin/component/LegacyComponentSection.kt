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
import i18n.str
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
            TagView(text = str("NEW"), cornerRadius = 4.dp)
            TagView(text = str("BETA"), cornerRadius = 4.dp)
            TagView(text = "9.9.9", cornerRadius = 4.dp)
        }
    }
}

/** Renders the "NotificationSnackBar" gallery section content: sample notifications plus trigger buttons and a snackbar host. */
@Composable
fun NotificationSnackBarDemo() {
    val hostState = remember { NotificationSnackBarHostState() }
    val scope = rememberCoroutineScope()
    val chargingTitle = str("Charging")
    val batteryIsAt80 = str("Battery is at 80%")
    val detailsAction = str("Details")
    val newPhotoTitle = str("New photo")
    val tapToViewTheAlbum = str("Tap to view the album")
    val viewAction = str("View")

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
                    subtitle = str("Square image, 44dp"),
                    icon = COUIIcons.Email,
                    imageType = NotificationSnackBarImageType.Square,
                    actionText = str("Open"),
                    onAction = { },
                    onClose = { },
                )
                NotificationSnackBar(
                    title = str("Now playing"),
                    subtitle = str("Vertical image, 36dp x 48dp"),
                    icon = COUIIcons.Photos,
                    imageType = NotificationSnackBarImageType.Vertical,
                    actionText = str("Pause"),
                    onAction = { },
                    onClose = { },
                )
                NotificationSnackBar(
                    title = str("Alarm set"),
                    subtitle = str("Horizontal image, 58dp x 44dp"),
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
                    text = str("Show"),
                    onClick = {
                        scope.launch {
                            hostState.showNotification(
                                title = chargingTitle,
                                subtitle = batteryIsAt80,
                                icon = COUIIcons.WorldClock,
                                actionText = detailsAction,
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = str("Show vertical"),
                    onClick = {
                        scope.launch {
                            hostState.showNotification(
                                title = newPhotoTitle,
                                subtitle = tapToViewTheAlbum,
                                icon = COUIIcons.Photos,
                                imageType = NotificationSnackBarImageType.Vertical,
                                actionText = viewAction,
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
                text = str("This sentence is far too long for the box it is given"),
                modifier = Modifier.width(160.dp),
            )
            MarqueeText(
                text = str("Short text"),
                modifier = Modifier.width(160.dp),
            )
        }
    }
}
