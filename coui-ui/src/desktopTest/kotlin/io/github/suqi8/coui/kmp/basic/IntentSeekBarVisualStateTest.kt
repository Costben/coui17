// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import kotlin.test.Test
import kotlin.test.assertEquals

class IntentSeekBarVisualStateTest {
    @Test
    fun playbackStateMapsToTheNativeVisualState() {
        assertEquals(
            IntentSeekBarVisualState.Static,
            resolveIntentSeekBarVisualState(IntentSeekBarPlaybackState.Idle, isDragging = false),
        )
        assertEquals(
            IntentSeekBarVisualState.LoadingFull,
            resolveIntentSeekBarVisualState(IntentSeekBarPlaybackState.LoadingInitial, isDragging = false),
        )
        assertEquals(
            IntentSeekBarVisualState.PlayingConeLight,
            resolveIntentSeekBarVisualState(IntentSeekBarPlaybackState.Playing, isDragging = false),
        )
        assertEquals(
            IntentSeekBarVisualState.Static,
            resolveIntentSeekBarVisualState(IntentSeekBarPlaybackState.Paused, isDragging = false),
        )
        assertEquals(
            IntentSeekBarVisualState.LoadingPartial,
            resolveIntentSeekBarVisualState(IntentSeekBarPlaybackState.LoadingBuffering, isDragging = false),
        )
    }

    @Test
    fun draggingOnlyOverridesPlayingAndBuffering() {
        assertEquals(
            IntentSeekBarVisualState.DraggingConeLight,
            resolveIntentSeekBarVisualState(IntentSeekBarPlaybackState.Playing, isDragging = true),
        )
        assertEquals(
            IntentSeekBarVisualState.DraggingConeLight,
            resolveIntentSeekBarVisualState(IntentSeekBarPlaybackState.LoadingBuffering, isDragging = true),
        )
        assertEquals(
            IntentSeekBarVisualState.LoadingFull,
            resolveIntentSeekBarVisualState(IntentSeekBarPlaybackState.LoadingInitial, isDragging = true),
        )
        assertEquals(
            IntentSeekBarVisualState.Static,
            resolveIntentSeekBarVisualState(IntentSeekBarPlaybackState.Idle, isDragging = true),
        )
    }
}
