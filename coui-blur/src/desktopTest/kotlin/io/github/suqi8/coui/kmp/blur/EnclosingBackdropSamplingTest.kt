// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(InternalCouiBlurApi::class)

package io.github.suqi8.coui.kmp.blur

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A node inside a `layerBackdrop` hears when that backdrop starts and stops being sampled, so a
 * platform view drawn there can react before the sampled frame is rendered; a node outside it does
 * not.
 */
@OptIn(ExperimentalTestApi::class)
class EnclosingBackdropSamplingTest {

    @Test
    fun anEnclosedNodeFollowsTheBackdropsConsumers() = runSkikoComposeUiTest(
        size = Size(200f, 200f),
        density = Density(1f),
    ) {
        val inside = mutableListOf<Boolean>()
        val outside = mutableListOf<Boolean>()
        var showConsumer by mutableStateOf(false)
        setContent {
            val backdrop = rememberLayerBackdrop()
            Box {
                Box(Modifier.size(100.dp).layerBackdrop(backdrop)) {
                    Box(Modifier.size(10.dp).onEnclosingBackdropSampling { inside += it })
                }
                Box(Modifier.size(10.dp).onEnclosingBackdropSampling { outside += it })
                if (showConsumer) Box(Modifier.size(20.dp).textureBlur(backdrop, RectangleShape))
            }
        }
        waitForIdle()
        assertEquals(emptyList(), inside)

        showConsumer = true
        waitForIdle()
        assertEquals(listOf(true), inside)

        showConsumer = false
        waitForIdle()
        assertEquals(listOf(true, false), inside)
        assertEquals(emptyList(), outside)
    }

    @Test
    fun aNodeAttachedWhileSampledIsToldAtOnce() = runSkikoComposeUiTest(
        size = Size(200f, 200f),
        density = Density(1f),
    ) {
        val events = mutableListOf<Boolean>()
        var showNode by mutableStateOf(false)
        setContent {
            val backdrop = rememberLayerBackdrop()
            Box {
                Box(Modifier.size(100.dp).layerBackdrop(backdrop)) {
                    if (showNode) Box(Modifier.size(10.dp).onEnclosingBackdropSampling { events += it })
                }
                Box(Modifier.size(20.dp).textureBlur(backdrop, RectangleShape))
            }
        }
        waitForIdle()
        showNode = true
        waitForIdle()
        assertEquals(listOf(true), events)
    }
}
