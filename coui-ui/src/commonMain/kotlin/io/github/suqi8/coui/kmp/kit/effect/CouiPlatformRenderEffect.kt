// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.effect

import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.TileMode
import io.github.suqi8.coui.kmp.shader.RuntimeShader
import io.github.suqi8.coui.kmp.shader.isRuntimeShaderSupported

/** True when the current platform supports native floating surface rendering (Android API 34+ or Skiko). */
internal expect fun isKitSurfaceSupported(): Boolean

/** True when the current platform supports [RenderEffect] composition (Android API 31+ or Skiko). */
internal expect fun isKitRenderEffectSupported(): Boolean

/**
 * True when the metaball search toolbar renders its material correctly on this platform.
 *
 * The search toolbar stacks several floating surfaces into one fusion patch with a motion-blur
 * shader; on Skiko that composition produces no visible material and animates slowly, so the
 * toolbar falls back to the Compose material path ([SearchViewAnimate]) there.
 */
internal expect fun isKitSearchBarSupported(): Boolean

/** True when the current platform supports runtime shader compilation (Android API 33+ or Skiko). */
internal fun isKitRuntimeShaderSupported(): Boolean = isRuntimeShaderSupported()

/** True when shader source supports AGSL ES 3.0 directives such as `#version 300` and `fwidth`. */
internal expect val isAgslEs3Supported: Boolean

/**
 * True on single-threaded / Skiko-Metal hosts where animated floating capsules (such as
 * `TopAppBarSearchPair`) should skip secondary offscreen shadow mesh re-tessellation, caustic
 * blur, and per-frame text motion-blur passes while morphing so the expand/collapse spring holds
 * 120Hz without reallocating 6+ offscreen textures every frame.
 */
internal expect val preferFastAnimatingFloatingSurfaces: Boolean

/** Wraps this [RuntimeShader] as a Compose [RenderEffect] sampling its input from [inputUniformName]. */
internal expect fun RuntimeShader.asComposeRenderEffect(inputUniformName: String = "composable"): RenderEffect

/** Creates a platform blur [RenderEffect] using Compose radius semantics and [edgeTreatment]. */
internal expect fun createBlurRenderEffect(
    radiusX: Float,
    radiusY: Float,
    edgeTreatment: TileMode = TileMode.Clamp,
): RenderEffect

/** Chains [outer] after [inner] into a single [RenderEffect]. */
internal expect fun createChainRenderEffect(outer: RenderEffect, inner: RenderEffect): RenderEffect

/** Creates a translation offset [RenderEffect]. */
internal expect fun createOffsetRenderEffect(offsetX: Float = 0f, offsetY: Float = 0f): RenderEffect

/** Blends [src] over [dst] using [blendMode]. */
internal expect fun createBlendModeRenderEffect(
    dst: RenderEffect,
    src: RenderEffect,
    blendMode: BlendMode,
): RenderEffect

/** Applies a [BlendMode] color filter over [renderEffect]. */
internal expect fun createBlendModeColorFilterRenderEffect(
    color: Color,
    blendMode: BlendMode,
    renderEffect: RenderEffect,
): RenderEffect

/**
 * Builds the blurred + two-stage color-blended input effect for a floating surface material.
 * On Skiko/Metal, large Mirror blurs fold both color filters into the downscaled blur pass before
 * upscaling so ColorDodge and Luminosity do not run on full-resolution Retina offscreen targets.
 */
internal expect fun createFloatingMaterialInputEffect(
    blurRadius: Float,
    firstColor: Color,
    firstBlendMode: BlendMode,
    secondColor: Color,
    secondBlendMode: BlendMode,
): RenderEffect

/** Platform-native render-effect handle preserved for legacy Android callers during migration. */
internal expect class PlatformRenderEffect

/** Converts a [PlatformRenderEffect] into a Compose [RenderEffect]. */
internal expect fun PlatformRenderEffect.asComposePlatformRenderEffect(): RenderEffect

/** Extracts the [PlatformRenderEffect] backing a Compose [RenderEffect]. */
internal expect fun RenderEffect.asPlatformRenderEffect(): PlatformRenderEffect
