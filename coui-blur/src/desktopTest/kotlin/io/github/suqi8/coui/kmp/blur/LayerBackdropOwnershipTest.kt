// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.blur

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * A [LayerBackdrop] is owned by the most recently attached `layerBackdrop` node. Other nodes must
 * neither record into the shared layer nor wipe the owner's coordinates when they detach.
 */
@OptIn(ExperimentalTestApi::class)
class LayerBackdropOwnershipTest {

    private val ownerSize = IntSize(50, 50)
    private val otherSize = IntSize(100, 100)

    @Test
    fun aDetachingNonOwnerKeepsTheOwnersCoordinates() = runSkikoComposeUiTest(
        size = Size(400f, 400f),
        density = Density(1f),
    ) {
        var backdrop: LayerBackdrop? = null
        var showOther by mutableStateOf(true)
        var showOwner by mutableStateOf(false)
        setContent {
            backdrop = rememberLayerBackdrop()
            Box {
                // Declared first so it draws first: the non-owner then draws last and would win the
                // layer if recording were not restricted to the owner.
                if (showOwner) Box(Modifier.size(50.dp).layerBackdrop(backdrop!!))
                if (showOther) Box(Modifier.size(100.dp).layerBackdrop(backdrop!!))
            }
        }
        waitForIdle()
        showOwner = true
        waitForIdle()

        val sharedBackdrop = backdrop!!
        assertEquals(2, sharedBackdrop.attachedNodeCount)
        assertEquals(ownerSize, sharedBackdrop.layerCoordinates?.size)
        assertEquals(ownerSize, sharedBackdrop.graphicsLayer.size)

        showOther = false
        waitForIdle()

        assertEquals(1, sharedBackdrop.attachedNodeCount)
        assertNotNull(sharedBackdrop.layerCoordinates)
        assertEquals(ownerSize, sharedBackdrop.layerCoordinates?.size)
        assertEquals(ownerSize, sharedBackdrop.graphicsLayer.size)
    }

    @Test
    fun detachingANonOwnerDoesNotClearTheRegisteredCoordinates() = runSkikoComposeUiTest(
        size = Size(400f, 400f),
        density = Density(1f),
    ) {
        var backdrop: LayerBackdrop? = null
        var showOther by mutableStateOf(true)
        var showOwner by mutableStateOf(false)
        setContent {
            backdrop = rememberLayerBackdrop()
            Box {
                if (showOwner) Box(Modifier.size(50.dp).layerBackdrop(backdrop!!))
                if (showOther) Box(Modifier.size(100.dp).layerBackdrop(backdrop!!))
            }
        }
        waitForIdle()
        // The second node attaches last and becomes the owner; the first one is the non-owner.
        showOwner = true
        waitForIdle()

        showOther = false
        waitForIdle()

        assertNotNull(backdrop!!.layerCoordinates)
    }

    // Re-targeting a node does not trigger a layout pass, so unlike a removal nothing re-publishes
    // the owner's coordinates afterwards: they only survive if the old backdrop is left untouched.
    @Test
    fun retargetingANonOwnerDoesNotClearTheOwnersCoordinates() = runSkikoComposeUiTest(
        size = Size(400f, 400f),
        density = Density(1f),
    ) {
        var shared: LayerBackdrop? = null
        var other: LayerBackdrop? = null
        var retarget by mutableStateOf(false)
        var showOwner by mutableStateOf(false)
        setContent {
            shared = rememberLayerBackdrop()
            other = rememberLayerBackdrop()
            Box {
                if (showOwner) Box(Modifier.size(50.dp).layerBackdrop(shared!!))
                Box(Modifier.size(100.dp).layerBackdrop(if (retarget) other!! else shared!!))
            }
        }
        waitForIdle()
        showOwner = true
        waitForIdle()

        retarget = true
        waitForIdle()

        assertEquals(ownerSize, shared!!.layerCoordinates?.size)
        assertEquals(otherSize, other!!.layerCoordinates?.size)
    }

    @Test
    fun theOwnerDetachingHandsTheBackdropToTheRemainingNode() = runSkikoComposeUiTest(
        size = Size(400f, 400f),
        density = Density(1f),
    ) {
        var backdrop: LayerBackdrop? = null
        var showOwner by mutableStateOf(false)
        setContent {
            backdrop = rememberLayerBackdrop()
            Box {
                if (showOwner) Box(Modifier.size(50.dp).layerBackdrop(backdrop!!))
                Box(Modifier.size(100.dp).layerBackdrop(backdrop!!))
            }
        }
        waitForIdle()
        showOwner = true
        waitForIdle()
        assertEquals(ownerSize, backdrop!!.layerCoordinates?.size)

        showOwner = false
        waitForIdle()

        assertEquals(1, backdrop!!.attachedNodeCount)
        assertEquals(otherSize, backdrop!!.layerCoordinates?.size)
        assertEquals(otherSize, backdrop!!.graphicsLayer.size)
    }

    @Test
    fun theLastNodeDetachingClearsTheCoordinates() = runSkikoComposeUiTest(
        size = Size(400f, 400f),
        density = Density(1f),
    ) {
        var backdrop: LayerBackdrop? = null
        var show by mutableStateOf(true)
        setContent {
            backdrop = rememberLayerBackdrop()
            Box {
                if (show) Box(Modifier.size(50.dp).layerBackdrop(backdrop!!))
            }
        }
        waitForIdle()
        assertNotNull(backdrop!!.layerCoordinates)

        show = false
        waitForIdle()

        assertEquals(0, backdrop!!.attachedNodeCount)
        assertNull(backdrop!!.layerCoordinates)
    }
}
