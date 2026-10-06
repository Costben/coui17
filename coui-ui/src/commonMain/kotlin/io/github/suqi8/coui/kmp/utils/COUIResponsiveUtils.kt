// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Width tiers ColorOS picks its responsive resource buckets from.
 *
 * ColorOS 17 ships the tier-specific values under 600dp and 840dp resource qualifiers, so the
 * cut-offs are the qualifiers themselves: below 600dp is `compat`, 600–840dp is `medium`, 840dp and
 * above is `expanded`.
 */
enum class COUIWindowSizeClass {
    /** Below 600dp wide — the `_compat` token family. */
    Compat,

    /** 600dp up to 840dp wide — the `_medium` token family. */
    Medium,

    /** 840dp wide or more — the `_expanded` token family. */
    Expanded,
}

/**
 * Buckets a width into COUI's [COUIWindowSizeClass] and picks the matching value from a token
 * family such as `coui_appbar_title_expanded_margin_start_compat` / `_medium` / `_expanded`.
 */
object COUIResponsiveUtils {

    /** Lower bound of the `medium` bucket (the 600dp qualifier). */
    val MediumMinWidth: Dp = 600.dp

    /** Lower bound of the `expanded` bucket (the 840dp qualifier). */
    val ExpandedMinWidth: Dp = 840.dp

    /** Buckets [width] into the COUI width tiers. */
    fun windowSizeClass(width: Dp): COUIWindowSizeClass = when {
        width >= ExpandedMinWidth -> COUIWindowSizeClass.Expanded
        width >= MediumMinWidth -> COUIWindowSizeClass.Medium
        else -> COUIWindowSizeClass.Compat
    }

    /**
     * Resolves a three-step token family. COUI leaves the unsuffixed token equal to the `medium`
     * value (e.g. `coui_appbar_title_expanded_margin_start` = 24dp = `..._medium`), so [medium]
     * defaults to [compat] and [expanded] to [medium].
     */
    fun <T> pick(
        windowSizeClass: COUIWindowSizeClass,
        compat: T,
        medium: T = compat,
        expanded: T = medium,
    ): T = when (windowSizeClass) {
        COUIWindowSizeClass.Compat -> compat
        COUIWindowSizeClass.Medium -> medium
        COUIWindowSizeClass.Expanded -> expanded
    }
}
