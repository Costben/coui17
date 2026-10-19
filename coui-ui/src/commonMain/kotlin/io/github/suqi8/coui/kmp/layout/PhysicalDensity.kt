// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.layout

/**
 * The panel's density in dpi, as ColorOS system apps see it, or `null` on a platform that has no
 * display-size override to look past.
 */
internal expect fun platformPhysicalDensityDpi(): Int?
