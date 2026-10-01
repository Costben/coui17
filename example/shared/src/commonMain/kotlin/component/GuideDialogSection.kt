// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.LocalAppStrings
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.PageIndicator
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.preference.ArrowPreference
import io.github.suqi8.coui.kmp.shared.generated.resources.Res
import io.github.suqi8.coui.kmp.shared.generated.resources.blur_test_bg
import io.github.suqi8.coui.kmp.window.GuidePage
import io.github.suqi8.coui.kmp.window.WindowGuideDialog
import org.jetbrains.compose.resources.painterResource

fun LazyListScope.guideDialogSection() {
    item(key = "guideDialog") {
        SmallTitle(text = "GuideDialog")
        GuideDialogDemo()
    }

    item(key = "pageIndicator") {
        SmallTitle(text = "PageIndicator")
        PageIndicatorDemo()
    }
}

/** Renders the "GuideDialog" gallery section content: the guide entry card and its two dialogs. */
@Composable
fun GuideDialogDemo() {
    var showMultiPageGuide by remember { mutableStateOf(false) }
    var multiPageGuideHoldDown by remember { mutableStateOf(false) }
    var showSinglePageGuide by remember { mutableStateOf(false) }
    var singlePageGuideHoldDown by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        ArrowPreference(
            title = str("GuideDialog (3 pages)"),
            summary = str("A first-run guide with a skip button and an indicator"),
            onClick = {
                showMultiPageGuide = true
                multiPageGuideHoldDown = true
            },
            holdDownState = multiPageGuideHoldDown,
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        ArrowPreference(
            title = str("GuideDialog (single page)"),
            summary = str("One page: the skip button and the indicator are hidden"),
            onClick = {
                showSinglePageGuide = true
                singlePageGuideHoldDown = true
            },
            holdDownState = singlePageGuideHoldDown,
        )
    }

    MultiPageGuideDemo(
        show = showMultiPageGuide,
        onDismissRequest = { showMultiPageGuide = false },
        onDismissFinished = { multiPageGuideHoldDown = false },
    )
    SinglePageGuideDemo(
        show = showSinglePageGuide,
        onDismissRequest = { showSinglePageGuide = false },
        onDismissFinished = { singlePageGuideHoldDown = false },
    )
}

/** Renders the "PageIndicator" gallery section content: a card of page indicators with varying counts. */
@Composable
fun PageIndicatorDemo() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PageIndicator(count = 3, currentIndex = 1)
            PageIndicator(count = 5, currentIndex = 0)
            PageIndicator(count = 8, currentIndex = 6)
            PageIndicator(count = 0, currentIndex = 0)
        }
    }
}

@Composable
private fun MultiPageGuideDemo(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit,
) {
    val image = painterResource(Res.drawable.blur_test_bg)
    val s = LocalAppStrings.current
    val pages = remember(image, s) {
        listOf(
            GuidePage(
                title = s["Welcome to COUI"],
                description = s["A ColorOS component library for Compose Multiplatform."],
                image = image,
            ),
            GuidePage(
                title = s["Every component is themed"],
                description = s["The theme drives colours, text styles and shapes from a single place."],
                image = image,
            ),
            GuidePage(
                title = s["Ready to build"],
                description = s["Browse the sections of this page and drop a component into your app."],
                image = image,
            ),
        )
    }

    WindowGuideDialog(
        show = show,
        pages = pages,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
    )
}

@Composable
private fun SinglePageGuideDemo(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onDismissFinished: () -> Unit,
) {
    val image = painterResource(Res.drawable.blur_test_bg)
    val s = LocalAppStrings.current
    val pages = remember(image, s) {
        listOf(
            GuidePage(
                title = s["A single-page guide"],
                description = s["With one page the indicator is hidden and the primary button fills the row."],
                image = image,
            ),
        )
    }

    WindowGuideDialog(
        show = show,
        pages = pages,
        onDismissRequest = onDismissRequest,
        onDismissFinished = onDismissFinished,
    )
}
