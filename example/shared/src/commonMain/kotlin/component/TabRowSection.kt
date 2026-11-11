// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")

package component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.HighlightedIconButton
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.LocalEdgeToEdge
import io.github.suqi8.coui.kmp.basic.SmallTopAppBar
import io.github.suqi8.coui.kmp.basic.TabRow
import io.github.suqi8.coui.kmp.basic.TabRowDefaults
import io.github.suqi8.coui.kmp.basic.TabRowWithContour
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Back
import io.github.suqi8.coui.kmp.icon.extended.More
import io.github.suqi8.coui.kmp.icon.extended.Scan
import io.github.suqi8.coui.kmp.squircle.squircleClip
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * Renders the "TabRow" gallery section content: one segment bar whose material tier comes from the
 * option. The switches decide whether the bar sits in the floating top app bar and whether the
 * sample content behind it is drawn.
 *
 * The tiers run from the most transparent to the most solid: the frosted blur highlight, the
 * transparent contour capsule, the solid capsule, the 32dp tiny form, and finally the frameless
 * row.
 */
@Composable
fun TabRowDemo() {
    val styleIndex = remember { mutableIntStateOf(0) }
    val inTopBar = remember { mutableStateOf(true) }
    val showBackground = remember { mutableStateOf(true) }

    DemoOptions(
        DemoChoice(
            "Style",
            listOf("Transparent blur highlight", "Contour", "Solid", "Tiny", "Frameless"),
            styleIndex,
        ),
        DemoToggle("In top app bar", inTopBar),
        DemoToggle("Show background", showBackground),
    )
    TabRowStage(
        styleIndex = styleIndex.intValue,
        inTopBar = inTopBar.value,
        showBackground = showBackground.value,
    )
}

/**
 * The single segment-bar stage. The style option picks the tier — frosted blur highlight, contour,
 * solid, tiny, or the frameless row — while the switches move the bar in or out of the floating
 * toolbar and show or hide the sample content it blurs.
 */
@Composable
private fun TabRowStage(
    styleIndex: Int,
    inTopBar: Boolean,
    showBackground: Boolean,
) {
    val tabs = listOf(str("Year"), str("Month"), str("Day"))
    var selectedTab by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState(initial = if (inTopBar) 110 else 0)
    val surfaceColor = COUITheme.colorScheme.background
    val isDark = surfaceColor.luminance() < 0.5f
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
    val tiny = styleIndex == 3

    val tabRow: @Composable () -> Unit = {
        if (styleIndex == 4) {
            TabRow(
                tabs = tabs,
                selectedTabIndex = selectedTab,
                onTabSelected = { selectedTab = it },
            )
        } else {
            TabRowWithContour(
                tabs = tabs,
                selectedTabIndex = selectedTab,
                onTabSelected = { selectedTab = it },
                colors = if (styleIndex == 2) {
                    TabRowDefaults.opaqueTabRowColors(
                        selectedBackgroundColor = if (isDark) Color(0xFF5D5D5D) else Color(0xFFF6F7F7),
                    )
                } else {
                    TabRowDefaults.tabRowColors()
                },
                height = if (tiny) {
                    TabRowDefaults.TabRowWithContourTinyHeight
                } else {
                    TabRowDefaults.TabRowWithContourHeight
                },
                cornerRadius = if (tiny) {
                    TabRowDefaults.TabRowWithContourTinyCornerRadius
                } else {
                    TabRowDefaults.TabRowWithContourCornerRadius
                },
                contourPadding = if (tiny) {
                    TabRowDefaults.TabRowWithContourTinyPadding
                } else {
                    TabRowDefaults.TabRowWithContourPadding
                },
                backdrop = if (styleIndex == 0) backdrop else null,
                containerMaterial = if (styleIndex == 2) {
                    TabRowDefaults.containerStroke(isDark)
                } else {
                    null
                },
                indicatorMaterial = if (styleIndex == 2) {
                    TabRowDefaults.indicatorStroke(isDark)
                } else {
                    null
                },
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .then(if (inTopBar || showBackground) Modifier.height(296.dp) else Modifier)
            .then(
                if (showBackground) {
                    Modifier
                        .squircleClip(24.dp)
                        .background(surfaceColor)
                } else {
                    Modifier
                },
            ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (!inTopBar) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    tabRow()
                }
            }
            if (showBackground) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .layerBackdrop(backdrop)
                        .verticalScroll(scrollState)
                        .padding(
                            top = if (inTopBar) 120.dp else 12.dp,
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 24.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SampleHeroBanner(selectedTab = selectedTab)
                    repeat(3) {
                        Text(
                            text = str("Sample text"),
                            style = COUITheme.textStyles.body2,
                            color = COUITheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
            }
        }

        if (inTopBar) {
            CompositionLocalProvider(
                LocalBackdrop provides backdrop,
                LocalEdgeToEdge provides true,
            ) {
                SmallTopAppBar(
                    title = str("TabRow"),
                    color = surfaceColor,
                    defaultWindowInsetsPadding = false,
                    showDivider = false,
                    gradientGlass = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    navigationIcon = {
                        HighlightedIconButton(
                            onClick = {},
                            backdrop = backdrop,
                        ) {
                            Icon(
                                imageVector = COUIIcons.Back,
                                contentDescription = str("Back"),
                            )
                        }
                    },
                    actions = {
                        HighlightedIconButton(
                            onClick = {},
                            backdrop = backdrop,
                        ) {
                            Icon(
                                imageVector = COUIIcons.Scan,
                                contentDescription = null,
                            )
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        HighlightedIconButton(
                            onClick = {},
                            backdrop = backdrop,
                        ) {
                            Icon(
                                imageVector = COUIIcons.More,
                                contentDescription = null,
                            )
                        }
                    },
                    bottomContent = {
                        Box(
                            modifier = Modifier.padding(
                                top = 4.dp,
                                bottom = 8.dp,
                                start = 16.dp,
                                end = 16.dp,
                            ),
                        ) {
                            tabRow()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SampleHeroBanner(selectedTab: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .squircleClip(20.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = when (selectedTab) {
                        0 -> listOf(Color(0xFF4295D6), Color(0xFF2CC5C9))
                        1 -> listOf(Color(0xFF3E7BD6), Color(0xFF5A54D1))
                        else -> listOf(Color(0xFF7B5AD6), Color(0xFFD65A9E))
                    },
                ),
            ),
    )
}
