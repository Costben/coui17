// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.runtime.Composable

/**
 * The strength of a COUI wheel-picker tick.
 *
 * Mirrors the `couiVibrateLevel` attribute of `COUINumberPicker`: [Crisp] is the default and
 * [Soft] is the gentler waveform.
 */
enum class CouiVibrateLevel {
    /** Crisp, higher-amplitude tick (`VIBRATE_LEVEL_CRISP`, 0). */
    Crisp,

    /** Soft, lower-amplitude tick (`VIBRATE_LEVEL_SOFT`, 1). */
    Soft,
}

/**
 * A wheel-picker tick whose strength follows the scroll velocity.
 *
 * On Android this drives the ColorOS linear motor through `VibrateUtils` when the vendor API is
 * available and falls back to the discrete granular waveform otherwise; on every other platform it
 * maps to a standard Compose tick.
 *
 * @param velocity The absolute scroll velocity in pixels per second.
 * @param level The configured vibrate level.
 */
@Composable
expect fun rememberCouiPickerHaptic(): (velocity: Float, level: CouiVibrateLevel) -> Unit

/**
 * The mechanical click a COUI wheel picker plays when it steps onto a new item.
 *
 * Android plays `coui_numberpicker_click` through a `SoundPool`; the audio asset is not part of this
 * library, so hosts that want the exact click add `res/raw/coui_numberpicker_click.ogg` to their own
 * module. Every other platform is silent.
 */
@Composable
expect fun rememberCouiPickerClick(): () -> Unit
