// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

/**
 * Platform-level font weight adjustment applied by COUI text primitives when the host OS enables
 * system-wide Bold Text (e.g. iOS Settings → Display & Brightness → Bold Text, where Compose
 * Multiplatform Skiko does not read `UIAccessibilityIsBoldTextEnabled()`).
 *
 * Android returns `0` because Compose's `AndroidFontResolveInterceptor` already applies
 * `Configuration.fontWeightAdjustment` natively.
 */
internal val LocalPlatformFontWeightAdjustment = compositionLocalOf { 0 }

@Composable
internal expect fun rememberPlatformFontWeightAdjustment(): Int

internal fun adjustPlatformFontWeight(
    weight: FontWeight?,
    adjustment: Int,
): FontWeight? {
    if (adjustment == 0) return weight
    val base = (weight ?: FontWeight.Normal).weight
    return FontWeight((base + adjustment).coerceIn(1, 1000))
}

internal fun AnnotatedString.withPlatformFontWeightAdjustment(adjustment: Int): AnnotatedString {
    if (adjustment == 0 || spanStyles.isEmpty()) return this
    var hasExplicitWeight = false
    for (i in 0 until spanStyles.size) {
        if (spanStyles[i].item.fontWeight != null) {
            hasExplicitWeight = true
            break
        }
    }
    if (!hasExplicitWeight) return this
    return buildAnnotatedString {
        append(this@withPlatformFontWeightAdjustment)
        for (i in 0 until spanStyles.size) {
            val range = spanStyles[i]
            val w = range.item.fontWeight ?: continue
            addStyle(
                SpanStyle(fontWeight = FontWeight((w.weight + adjustment).coerceIn(1, 1000))),
                range.start,
                range.end,
            )
        }
    }
}
