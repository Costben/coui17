// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.blur

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import io.github.suqi8.coui.kmp.shader.isRuntimeShaderSupported

/**
 * Global opt-out for backdrop blur effects (default `true`).
 *
 * Provide `false` at the root of an application or subtree to turn every blur modifier and COUI
 * glass surface off in one line, without swapping out [LocalBackdrop] or wrapping [layerBackdrop]
 * call sites:
 * ```kotlin
 * CompositionLocalProvider(LocalBlurEnabled provides enableBlur) {
 *     AppContent()
 * }
 * ```
 * When `false`, glass surfaces fall back to their opaque fills, top bars revert to solid container
 * colors instead of translucent ramps, and [LayerBackdrop] stops recording until an active blur
 * consumer re-appears.
 */
val LocalBlurEnabled = compositionLocalOf { true }

/**
 * Whether backdrop blur effects are active in the current composition — combines
 * [LocalBlurEnabled], the platform's system material/power-saving blur state, and
 * [isRuntimeShaderSupported].
 */
@Composable
@ReadOnlyComposable
fun isBlurEnabled(): Boolean = LocalBlurEnabled.current && isSystemBlurEnabled() && isRuntimeShaderSupported()
