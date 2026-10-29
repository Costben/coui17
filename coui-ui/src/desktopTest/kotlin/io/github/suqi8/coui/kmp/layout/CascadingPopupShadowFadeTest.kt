// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.suqi8.coui.kmp.basic.DropdownColors
import io.github.suqi8.coui.kmp.basic.DropdownEntry
import io.github.suqi8.coui.kmp.basic.DropdownItem
import io.github.suqi8.coui.kmp.basic.PopupLayoutPosition
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The cascading panel fades in through `popupOpacity`, not a `graphicsLayer` alpha: the latter
 * composites the panel into an offscreen the size of its own bounds, which crops the exterior drop
 * shadow to a hard-edged rectangle for the whole entrance animation.
 *
 * Compared against the settled frame rather than against absolute colours, so the assertion holds
 * for any shadow geometry the theme ships.
 */
@OptIn(ExperimentalTestApi::class)
class CascadingPopupShadowFadeTest {
    @Test
    fun fadingFramesKeepTheShadowOutsideThePanelBounds() {
        runSkikoComposeUiTest(size = Size(400f, 320f), density = Density(1f)) {
            var opacity by mutableFloatStateOf(1f)
            var panel = Rect.Zero
            setContent {
                COUITheme {
                    val backdrop = rememberLayerBackdrop { drawContent() }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Box(Modifier.fillMaxSize().layerBackdrop(backdrop).background(Color.White))
                        CompositionLocalProvider(LocalBackdrop provides backdrop) {
                            Box(Modifier.onGloballyPositioned { panel = it.boundsInRoot() }) {
                                CascadingPrimaryContent(
                                    entries = listOf(
                                        DropdownEntry(
                                            items = listOf(
                                                DropdownItem(text = "Local"),
                                                DropdownItem(text = "Export"),
                                            ),
                                        ),
                                    ),
                                    expandedItem = null,
                                    onExpand = {},
                                    onLeafSelected = {},
                                    onAnchorBounds = { _, _ -> },
                                    enterFraction = { 1f },
                                    enterAlpha = { opacity },
                                    popupLayoutPosition = PopupLayoutPosition(
                                        showBelow = false,
                                        showAbove = true,
                                        isRightAligned = false,
                                    ),
                                    transformOrigin = TransformOrigin(0f, 1f),
                                    shrinkTransformOrigin = TransformOrigin.Center,
                                    surfaceColor = Color.White,
                                    dropdownColors = TestDropdownColors,
                                    primaryScale = { 1f },
                                    primaryAlpha = { 1f },
                                    primaryTranslationY = { 0f },
                                    onCollapseSecondary = {},
                                    enableGlass = true,
                                )
                            }
                        }
                    }
                }
            }
            waitForIdle()
            val settled = onRoot().captureToImage().toPixelMap()
            // Two pixels outside each straight edge: inside the shadow's blur, outside the panel.
            val samples = listOf(
                (panel.left - 3).toInt() to panel.center.y.toInt(),
                (panel.right + 2).toInt() to panel.center.y.toInt(),
                panel.center.x.toInt() to (panel.bottom + 3).toInt(),
            )
            for (alpha in listOf(0.25f, 0.5f, 0.9f, 0.999f)) {
                opacity = alpha
                waitForIdle()
                val faded = onRoot().captureToImage().toPixelMap()
                for ((x, y) in samples) {
                    val restDarkness = 1f - settled[x, y].red
                    val fadeDarkness = 1f - faded[x, y].red
                    println("[cascading-shadow] alpha=$alpha sample=$x,$y rest=$restDarkness faded=$fadeDarkness")
                    assertTrue(restDarkness > 0.01f, "The settled shadow must cover the reference sample")
                    assertTrue(
                        abs(fadeDarkness - restDarkness * alpha) < 2f / 255f,
                        "The exterior shadow must fade with the panel, without a rectangular cutoff",
                    )
                }
            }
        }
    }

    private companion object {
        val TestDropdownColors = DropdownColors(
            contentColor = Color.Black,
            summaryColor = Color.Gray,
            containerColor = Color.Transparent,
            selectedContentColor = Color.Black,
            selectedSummaryColor = Color.Black,
            selectedContainerColor = Color.Transparent,
            selectedIndicatorColor = Color.Black,
        )
    }
}
