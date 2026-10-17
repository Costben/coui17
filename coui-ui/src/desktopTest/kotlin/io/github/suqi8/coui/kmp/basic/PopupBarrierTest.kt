// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.lightColorScheme
import kotlin.test.Test
import kotlin.test.assertEquals

private const val WINDOW_HEIGHT = 800
private const val BAR_HEIGHT = 64

@OptIn(ExperimentalTestApi::class)
class PopupBarrierTest {
    private fun windowBottom(
        windowHeight: Int = WINDOW_HEIGHT,
        bar: @Composable () -> Unit,
        popupHost: @Composable (@Composable () -> Unit) -> Unit = { it() },
    ): Int {
        var bottom = -1
        runSkikoComposeUiTest(size = Size(400f, windowHeight.toFloat()), density = Density(1f)) {
            setContent {
                COUITheme(colors = lightColorScheme()) {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.align(Alignment.BottomCenter)) { bar() }
                        popupHost {
                            bottom = rememberListPopupLayoutInfo(
                                alignment = PopupPositionProvider.Align.TopStart,
                                popupPositionProvider = ListPopupDefaults.DropdownPositionProvider,
                                parentBounds = IntRect(0, 100, 400, 140),
                                popupContentSize = IntSize.Zero,
                            ).windowBounds.bottom
                        }
                    }
                }
            }
            waitForIdle()
        }
        return bottom
    }

    @Composable
    private fun BarrierBar() {
        Box(Modifier.fillMaxWidth().height(BAR_HEIGHT.dp).popupBottomBarrier())
    }

    @Test
    fun aPopupStaysTheNavigationGapAboveABottomBarrier() {
        val bottom = windowBottom(bar = { BarrierBar() })
        assertEquals(WINDOW_HEIGHT - BAR_HEIGHT - 8, bottom)
    }

    @Test
    fun withoutABarrierOnlyTheWindowBarrierApplies() {
        val bottom = windowBottom(bar = { Box(Modifier.fillMaxWidth().height(BAR_HEIGHT.dp)) })
        assertEquals(WINDOW_HEIGHT - 32, bottom)
    }

    @Test
    fun aShortWindowIgnoresTheBarrier() {
        val bottom = windowBottom(windowHeight = 300, bar = { BarrierBar() })
        assertEquals(300 - 32, bottom)
    }

    @Test
    fun aPopupInsideADialogIgnoresThePageBarrier() {
        val bottom = windowBottom(bar = { BarrierBar() }, popupHost = { PopupBarrierBoundary(it) })
        assertEquals(WINDOW_HEIGHT - 32, bottom)
    }
}
