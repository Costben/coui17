// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SliderCustomizationTest {

    @Test
    fun magnetThresholdSnapsToClosestKeyPointWhenWithinThreshold() {
        val keyPoints = listOf(0f, 25f, 50f, 75f, 100f)
        val range = 0f..100f
        val stepFractions = floatArrayOf()
        val allFractions = computeAllKeyPointFractions(keyPoints, stepFractions, range)

        // 0.49f is within 0.02f of 0.50f (50f)
        val snapped = resolveValueFromFraction(
            fraction = 0.49f,
            valueRange = range,
            steps = 0,
            allKeyPointFractions = allFractions,
            magnetThreshold = 0.02f,
        )
        assertEquals(50f, snapped, 1e-4f)

        // 0.45f is NOT within 0.02f of 50f or 25f
        val free = resolveValueFromFraction(
            fraction = 0.45f,
            valueRange = range,
            steps = 0,
            allKeyPointFractions = allFractions,
            magnetThreshold = 0.02f,
        )
        assertEquals(45f, free, 1e-4f)
    }

    @Test
    fun computeKeyPointFractionsHandlesCustomPointsAndSteps() {
        val range = 0f..200f
        val points = listOf(0f, 50f, 150f, 200f)
        val fractions = computeKeyPointFractions(
            keyPoints = points,
            stepFractions = floatArrayOf(0f, 0.5f, 1f),
            valueRange = range,
            showKeyPoints = true,
        )
        assertEquals(4, fractions.size)
        assertEquals(0f, fractions[0], 1e-6f)
        assertEquals(0.25f, fractions[1], 1e-6f)
        assertEquals(0.75f, fractions[2], 1e-6f)
        assertEquals(1f, fractions[3], 1e-6f)
    }

    @Test
    fun computeKeyPointFractionsFallsBackToStepFractionsWhenShowKeyPointsIsTrue() {
        val range = 0f..100f
        val steps = 3
        val stepFractions = stepsToTickFractions(steps)
        val fractions = computeKeyPointFractions(
            keyPoints = null,
            stepFractions = stepFractions,
            valueRange = range,
            showKeyPoints = true,
        )
        assertEquals(5, fractions.size)
        assertEquals(0f, fractions[0], 1e-6f)
        assertEquals(0.25f, fractions[1], 1e-6f)
        assertEquals(0.5f, fractions[2], 1e-6f)
        assertEquals(0.75f, fractions[3], 1e-6f)
        assertEquals(1f, fractions[4], 1e-6f)
    }

    @Test
    fun computeKeyPointFractionsReturnsEmptyWhenNotShown() {
        val range = 0f..100f
        val stepFractions = stepsToTickFractions(3)
        val fractions = computeKeyPointFractions(
            keyPoints = null,
            stepFractions = stepFractions,
            valueRange = range,
            showKeyPoints = false,
        )
        assertTrue(fractions.isEmpty())
    }

    @Test
    fun horizontalVisualFractionCalculatesCorrectly() {
        val capInset = 14f
        val sizeWidth = 100
        val available = sizeWidth - 2 * capInset // 72

        val start = horizontalVisualFraction(14f, sizeWidth, capInset)
        assertEquals(0f, start, 1e-6f)

        val mid = horizontalVisualFraction(14f + 36f, sizeWidth, capInset)
        assertEquals(0.5f, mid, 1e-6f)

        val end = horizontalVisualFraction(14f + 72f, sizeWidth, capInset)
        assertEquals(1f, end, 1e-6f)
    }
}
