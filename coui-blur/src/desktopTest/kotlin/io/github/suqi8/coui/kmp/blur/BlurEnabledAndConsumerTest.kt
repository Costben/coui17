// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.blur

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class BlurEnabledAndConsumerTest {

    @Test
    fun layerBackdropSkipsRecordingWhileUnconsumedAndStartsOnceBlurAttaches() = runSkikoComposeUiTest(
        size = Size(200f, 200f),
        density = Density(1f),
    ) {
        var recordCount = 0
        var backdropRef: LayerBackdrop? = null
        var showBlur by mutableStateOf(false)
        var contentTick by mutableIntStateOf(0)

        setContent {
            val backdrop = rememberLayerBackdrop {
                recordCount++
                drawContent()
            }
            backdropRef = backdrop
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.size(100.dp).layerBackdrop(backdrop)) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(if (contentTick % 2 == 0) Color.Blue else Color.Red)
                    }
                }
                if (showBlur) {
                    Box(Modifier.size(40.dp).textureBlur(backdrop, RectangleShape))
                }
            }
        }
        waitForIdle()

        val backdrop = backdropRef!!
        assertEquals(0, backdrop.consumerCount)
        assertEquals(0, recordCount, "idle layerBackdrop must not record when nothing consumes it")
        assertEquals(IntSize.Zero, backdrop.graphicsLayer.size)

        contentTick++
        waitForIdle()
        assertEquals(0, recordCount, "redrawing behind an unconsumed layerBackdrop must stay free")

        showBlur = true
        waitForIdle()
        assertEquals(1, backdrop.consumerCount)
        assertTrue(recordCount >= 1, "attaching a blur consumer must trigger recording")
        assertEquals(IntSize(100, 100), backdrop.graphicsLayer.size)

        showBlur = false
        waitForIdle()
        assertEquals(0, backdrop.consumerCount)
        val countAfterDetach = recordCount

        contentTick++
        waitForIdle()
        assertEquals(countAfterDetach, recordCount, "detaching the last blur consumer must stop recording again")
    }

    @Test
    fun localBlurEnabledTogglesBlurAndBackdropRecording() = runSkikoComposeUiTest(
        size = Size(200f, 100f),
        density = Density(1f),
    ) {
        var blurEnabled by mutableStateOf(true)
        var observedBlurEnabled = false
        var recordCount = 0
        var backdropRef: LayerBackdrop? = null
        var contentTick by mutableIntStateOf(0)

        setContent {
            CompositionLocalProvider(LocalBlurEnabled provides blurEnabled) {
                observedBlurEnabled = isBlurEnabled()
                val backdrop = rememberLayerBackdrop {
                    recordCount++
                    drawContent()
                }
                backdropRef = backdrop
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                        Canvas(Modifier.fillMaxSize()) {
                            val half = size.width / 2f
                            drawRect(if (contentTick >= 0) Color.Blue else Color.Black, size = Size(half, size.height))
                            drawRect(Color.Red, topLeft = Offset(half, 0f), size = Size(size.width - half, size.height))
                        }
                    }
                    // Opaque green cover between the backdrop and the blur node: when blur is active
                    // the node samples blue/red from the backdrop; when blur is off it draws nothing
                    // over the green cover.
                    Box(Modifier.fillMaxSize().background(Color.Green))
                    Box(Modifier.fillMaxSize().textureBlur(backdrop, RectangleShape))
                }
            }
        }
        waitForIdle()

        val backdrop = backdropRef!!
        assertTrue(observedBlurEnabled)
        assertEquals(1, backdrop.consumerCount)
        val blurredPixels = onRoot().captureToImage().toPixelMap()
        assertNotEquals(
            Color.Green.toArgb(),
            blurredPixels[50, 50].toArgb(),
            "with LocalBlurEnabled=true, textureBlur must sample the backdrop instead of exposing the cover",
        )

        blurEnabled = false
        waitForIdle()
        assertFalse(observedBlurEnabled)
        assertEquals(0, backdrop.consumerCount, "LocalBlurEnabled=false must release the backdrop consumer")
        val disabledPixels = onRoot().captureToImage().toPixelMap()
        assertEquals(
            Color.Green.toArgb(),
            disabledPixels[50, 50].toArgb(),
            "with LocalBlurEnabled=false, textureBlur must skip backdrop rendering",
        )

        val countWhileDisabled = recordCount
        contentTick++
        waitForIdle()
        assertEquals(countWhileDisabled, recordCount, "layerBackdrop must not record while LocalBlurEnabled=false")

        blurEnabled = true
        waitForIdle()
        assertTrue(observedBlurEnabled)
        assertEquals(1, backdrop.consumerCount)
        val restoredPixels = onRoot().captureToImage().toPixelMap()
        assertNotEquals(Color.Green.toArgb(), restoredPixels[50, 50].toArgb())
    }

    @Test
    fun systemBlurStateDisablesBlurAndStopsBackdropRecording() {
        try {
            runSkikoComposeUiTest(
                size = Size(200f, 100f),
                density = Density(1f),
            ) {
                var observedBlurEnabled = true
                var backdropRef: LayerBackdrop? = null

                setContent {
                    observedBlurEnabled = isBlurEnabled()
                    val backdrop = rememberLayerBackdrop { drawContent() }
                    backdropRef = backdrop
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize().layerBackdrop(backdrop).background(Color.Blue))
                        Box(Modifier.fillMaxSize().background(Color.Green))
                        Box(Modifier.fillMaxSize().textureBlur(backdrop, RectangleShape))
                    }
                }
                waitForIdle()
                val backdrop = backdropRef!!
                assertTrue(observedBlurEnabled)
                assertEquals(1, backdrop.consumerCount)

                SystemBlurState.overrideForTest = false
                waitForIdle()
                assertFalse(observedBlurEnabled)
                assertEquals(0, backdrop.consumerCount)
                val disabledPixels = onRoot().captureToImage().toPixelMap()
                assertEquals(Color.Green.toArgb(), disabledPixels[50, 50].toArgb())
            }
        } finally {
            SystemBlurState.overrideForTest = null
        }
    }

    @Test
    fun upstreamBackdropTracksDownstreamConsumers() = runSkikoComposeUiTest(
        size = Size(200f, 100f),
        density = Density(1f),
    ) {
        var pageRecords = 0
        var pageRef: LayerBackdrop? = null
        var barRef: LayerBackdrop? = null
        var showFirst by mutableStateOf(false)
        var showSecond by mutableStateOf(false)

        setContent {
            val pageBackdrop = rememberLayerBackdrop {
                pageRecords++
                drawContent()
            }
            val barBackdrop = rememberLayerBackdrop(upstream = pageBackdrop) {
                drawRect(Color.Black)
                drawLayer(pageBackdrop.graphicsLayer)
            }
            pageRef = pageBackdrop
            barRef = barBackdrop
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .layerBackdrop(barBackdrop)
                        .layerBackdrop(pageBackdrop),
                ) {
                    Canvas(Modifier.fillMaxSize()) { drawRect(Color.Red) }
                }
                if (showFirst) Box(Modifier.size(40.dp).textureBlur(barBackdrop, RectangleShape))
                if (showSecond) Box(Modifier.size(40.dp).textureBlur(barBackdrop, RectangleShape))
            }
        }
        waitForIdle()

        val page = pageRef!!
        val bar = barRef!!
        assertEquals(0, page.consumerCount)
        assertEquals(0, bar.consumerCount)
        assertEquals(0, pageRecords)

        showFirst = true
        waitForIdle()
        assertEquals(1, bar.consumerCount)
        assertEquals(1, page.consumerCount)
        assertTrue(pageRecords >= 1)

        showSecond = true
        waitForIdle()
        assertEquals(2, bar.consumerCount)
        assertEquals(1, page.consumerCount, "upstream holds one cascaded reference regardless of downstream fan-out")

        showFirst = false
        waitForIdle()
        assertEquals(1, bar.consumerCount)
        assertEquals(1, page.consumerCount)

        showSecond = false
        waitForIdle()
        assertEquals(0, bar.consumerCount)
        assertEquals(0, page.consumerCount)
    }
}
