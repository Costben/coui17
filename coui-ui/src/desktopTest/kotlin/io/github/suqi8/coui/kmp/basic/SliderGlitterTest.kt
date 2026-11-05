// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import kotlinx.coroutines.CoroutineScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The seek bar glitter follows `COUISeekBarAdvanced`: it flashes when a drag runs the progress
 * into an end while still moving towards it, once per contact, and fades back to nothing.
 */
@OptIn(ExperimentalTestApi::class)
class SliderGlitterTest {

    @Test
    fun alphaFollowsTheNativeCurve() {
        assertEquals(0f, glitterAlpha(0f))
        // 255 * (1 - exp(-ln(85) / 360 * 360)) = 255 * 84 / 85.
        assertEquals(84f / 85f, glitterAlpha(360f), 1e-4f)
    }

    @Test
    fun aDragIntoTheEndFlashesOnceAndFades() = glitterTest { glitter, scope, frames ->
        drag(glitter, scope, 0.9f, 0.95f, 1f, 1f)
        frames(100)
        assertTrue(glitter.alpha > 0f, "running into the end flashes")
        assertTrue(glitter.atEnd)
        frames(3000)
        assertEquals(0f, glitter.alpha, "the flash settles back to nothing")

        // Still pressing against the end: the contact has already flashed.
        drag(glitter, scope, 1f, 1f)
        frames(100)
        assertEquals(0f, glitter.alpha, "a held contact does not flash again")

        // Leaving the end re-arms it.
        drag(glitter, scope, 0.8f, 0.9f, 1f)
        frames(100)
        assertTrue(glitter.alpha > 0f, "hitting the end again flashes again")
    }

    @Test
    fun movingAwayFromTheEndNeverFlashes() = glitterTest { glitter, scope, frames ->
        // Arriving at 1 while the finger moves back towards the start.
        var time = 0L
        val tracker = SliderGlitter { time }
        listOf(1f, 1f, 1f).forEach {
            time += 16
            tracker.onDrag(scope, fraction = it, delta = -10f, bothEdges = false)
        }
        frames(100)
        assertEquals(0f, tracker.alpha)
    }

    @Test
    fun onlyAMiddleBarFlashesAtTheStart() = glitterTest { _, scope, frames ->
        var time = 0L
        val plain = SliderGlitter { time }
        val middle = SliderGlitter { time }
        listOf(0.1f, 0.05f, 0f, 0f).forEach {
            time += 16
            plain.onDrag(scope, fraction = it, delta = -10f, bothEdges = false)
            middle.onDrag(scope, fraction = it, delta = -10f, bothEdges = true)
        }
        frames(100)
        assertEquals(0f, plain.alpha, "a bar that grows from the start only flashes at the end")
        assertTrue(middle.alpha > 0f, "a bar that grows from the middle flashes at the start")
        assertTrue(!middle.atEnd)
    }

    private var dragTime = 0L

    private fun drag(glitter: SliderGlitter, scope: CoroutineScope, vararg fractions: Float) {
        fractions.forEach {
            dragTime += 16
            glitter.onDrag(scope, fraction = it, delta = 20f, bothEdges = false)
        }
    }

    private fun glitterTest(
        body: (glitter: SliderGlitter, scope: CoroutineScope, frames: (Long) -> Unit) -> Unit,
    ) = runSkikoComposeUiTest {
        mainClock.autoAdvance = false
        lateinit var glitter: SliderGlitter
        lateinit var scope: CoroutineScope
        setContent {
            glitter = remember { SliderGlitter { dragTime } }
            scope = rememberCoroutineScope()
        }
        waitForIdle()
        body(glitter, scope) { ms ->
            mainClock.advanceTimeBy(ms)
        }
    }
}
