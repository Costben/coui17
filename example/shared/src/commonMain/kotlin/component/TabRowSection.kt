// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")

package component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.TabRow
import io.github.suqi8.coui.kmp.basic.TabRowDefaults
import io.github.suqi8.coui.kmp.basic.TabRowWithContour
import io.github.suqi8.coui.kmp.basic.Text
import kotlinx.coroutines.launch

/** Renders the "TabRow" gallery section content: a frameless tab row plus contour tab rows over a pager. */
@Composable
fun TabRowDemo() {
    val framelessTabs = listOf(str("Day"), str("Week"), str("Month"))
    var framelessSelected by remember { mutableIntStateOf(0) }
    TabRow(
        tabs = framelessTabs,
        selectedTabIndex = framelessSelected,
        onTabSelected = { framelessSelected = it },
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        insideMargin = PaddingValues(16.dp),
    ) {
        // COUI segment button, as used by the special access page in ColorOS Settings.
        val segmentTabs = listOf(str("All"), str("Allowed"), str("Denied"))
        val scope = rememberCoroutineScope()
        val pagerState = rememberPagerState(pageCount = { segmentTabs.size })
        TabRowWithContour(
            tabs = segmentTabs,
            selectedTabIndex = pagerState.currentPage,
            onTabSelected = {
                scope.launch {
                    pagerState.animateScrollToPage(it)
                }
            },
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .padding(bottom = 16.dp),
            userScrollEnabled = true,
            key = { it },
            pageContent = { page ->
                Text(
                    text = str("Content of {}", segmentTabs[page]),
                    modifier = Modifier
                        .fillMaxWidth(),
                )
            },
        )
        // The Privacy & security dashboard's opaque capsule (Privacy / Security tabs).
        val privacyTabs = listOf(str("Privacy"), str("Security"))
        var privacySelected by remember { mutableIntStateOf(0) }
        TabRowWithContour(
            tabs = privacyTabs,
            selectedTabIndex = privacySelected,
            onTabSelected = { privacySelected = it },
            colors = TabRowDefaults.opaqueTabRowColors(),
            modifier = Modifier.padding(bottom = 16.dp),
        )
        // SegmentButton.Tiny variant (32dp height, 2dp contour inset).
        val tinyTabs = listOf(str("Photo"), str("Video"), str("Portrait"), str("Pano"))
        var tinySelected by remember { mutableIntStateOf(0) }
        TabRowWithContour(
            tabs = tinyTabs,
            selectedTabIndex = tinySelected,
            onTabSelected = { tinySelected = it },
            height = TabRowDefaults.TabRowWithContourTinyHeight,
            cornerRadius = TabRowDefaults.TabRowWithContourTinyCornerRadius,
            contourPadding = TabRowDefaults.TabRowWithContourTinyPadding,
        )
    }
}
