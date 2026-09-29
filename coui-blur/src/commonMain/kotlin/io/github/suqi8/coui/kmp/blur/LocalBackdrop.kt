// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.blur

import androidx.compose.runtime.compositionLocalOf

/**
 * The [Backdrop] that translucent surfaces sample from.
 *
 * ColorOS materialises its "light-sensing glass" through the system, not through the component:
 * a surface does not receive the backdrop as a parameter, it blurs whatever is behind it. This
 * local is the Compose analogue — publish it once around the content that scrolls behind the
 * translucent surfaces and every surface picks it up.
 *
 * Defaults to `null`, which means "no backdrop available". Surfaces then keep their opaque fill,
 * so hosts that never publish a backdrop render exactly as before.
 *
 * @see LayerBackdrop
 * @see rememberLayerBackdrop
 */
val LocalBackdrop = compositionLocalOf<Backdrop?> { null }
