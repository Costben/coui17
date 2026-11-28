// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.luminance
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * Partial system-bar style requested by a single screen or overlay on the [CouiSystemBarsController]
 * stack. Any `null` field inherits from the next active entry below it (or the theme fallback).
 */
@Immutable
data class CouiSystemBarsStyle(
    val darkStatusBarIcons: Boolean? = null,
    val darkNavigationBarIcons: Boolean? = null,
    val statusBarVisible: Boolean? = null,
    val navigationBarVisible: Boolean? = null,
)

/**
 * Fully resolved system-bar state after folding the active [CouiSystemBarsController] stack against
 * the theme fallback.
 */
@Immutable
data class ResolvedCouiSystemBarsStyle(
    val darkStatusBarIcons: Boolean,
    val darkNavigationBarIcons: Boolean,
    val statusBarVisible: Boolean,
    val navigationBarVisible: Boolean,
)

/**
 * Platform sink that applies a [ResolvedCouiSystemBarsStyle] to the host window or root view
 * controller. Android provides a default implementation backed by `WindowInsetsControllerCompat`;
 * Skiko/iOS apps can supply their root `UIViewController` bridge via [LocalCouiSystemBarsApplier]
 * or [CouiSystemBarsHost].
 */
fun interface CouiSystemBarsApplier {
    fun apply(style: ResolvedCouiSystemBarsStyle)
}

/** Optional host applier override (used on iOS/Skiko to bridge to `UIViewController`). */
val LocalCouiSystemBarsApplier = staticCompositionLocalOf<CouiSystemBarsApplier?> { null }

/** Enclosing [CouiSystemBarsController] stack; automatically provided by [COUITheme]. */
val LocalCouiSystemBarsController = staticCompositionLocalOf<CouiSystemBarsController?> { null }

@Stable
internal class CouiSystemBarsEntry(
    initialStyle: CouiSystemBarsStyle,
    initialActive: () -> Boolean,
) {
    var style by mutableStateOf(initialStyle)
    var activeProvider by mutableStateOf(initialActive)
}

/**
 * Stack-based coordinator for status-bar and navigation-bar icon contrast and visibility.
 *
 * Overlays push an entry via [CouiSystemBarsEffect] with a draw/transition-driven `active` lambda
 * (for example `{ !leaving && scrimAlpha >= 0.5f }`). The moment an overlay begins exiting, its
 * entry turns inactive and the stack immediately resolves back to the underlying route's style
 * without waiting for `onDispose` after the exit animation completes.
 */
@Stable
class CouiSystemBarsController {
    private val entries = mutableStateListOf<CouiSystemBarsEntry>()

    internal val hasEntries: Boolean
        get() = entries.isNotEmpty()

    internal fun attach(entry: CouiSystemBarsEntry) {
        if (entry !in entries) {
            entries.add(entry)
        }
    }

    internal fun detach(entry: CouiSystemBarsEntry) {
        entries.remove(entry)
    }

    /**
     * Resolves the topmost active value for each system-bar property, falling back to
     * [fallbackDarkIcons] for icon contrast and `true` for bar visibility.
     */
    fun resolve(fallbackDarkIcons: Boolean): ResolvedCouiSystemBarsStyle {
        var darkStatus: Boolean? = null
        var darkNav: Boolean? = null
        var statusVisible: Boolean? = null
        var navVisible: Boolean? = null
        for (i in entries.lastIndex downTo 0) {
            val entry = entries[i]
            if (!entry.activeProvider()) continue
            val s = entry.style
            if (darkStatus == null) darkStatus = s.darkStatusBarIcons
            if (darkNav == null) darkNav = s.darkNavigationBarIcons
            if (statusVisible == null) statusVisible = s.statusBarVisible
            if (navVisible == null) navVisible = s.navigationBarVisible
            if (darkStatus != null && darkNav != null && statusVisible != null && navVisible != null) {
                break
            }
        }
        return ResolvedCouiSystemBarsStyle(
            darkStatusBarIcons = darkStatus ?: fallbackDarkIcons,
            darkNavigationBarIcons = darkNav ?: fallbackDarkIcons,
            statusBarVisible = statusVisible ?: true,
            navigationBarVisible = navVisible ?: true,
        )
    }
}

/**
 * Installs a [CouiSystemBarsController] scope and binds it to [applier] (defaulting to
 * [LocalCouiSystemBarsApplier] or the platform applier). [COUITheme] installs this automatically
 * when no outer host exists; call directly at the app root on iOS/Skiko to wire a custom applier.
 */
@Composable
fun CouiSystemBarsHost(
    applier: CouiSystemBarsApplier? = null,
    content: @Composable () -> Unit,
) {
    val controller = remember { CouiSystemBarsController() }
    val platformApplier = rememberPlatformSystemBarsApplier()
    val effectiveApplier = applier ?: LocalCouiSystemBarsApplier.current ?: platformApplier
    val fallbackDarkIcons = COUITheme.colorScheme.background.luminance() >= 0.5f

    CompositionLocalProvider(
        LocalCouiSystemBarsController provides controller,
        LocalCouiSystemBarsApplier provides effectiveApplier,
    ) {
        content()
        CouiSystemBarsSync(
            controller = controller,
            fallbackDarkIcons = fallbackDarkIcons,
            applier = effectiveApplier,
        )
    }
}

@Composable
private fun CouiSystemBarsSync(
    controller: CouiSystemBarsController,
    fallbackDarkIcons: Boolean,
    applier: CouiSystemBarsApplier,
) {
    val hasEntries by remember(controller) { derivedStateOf { controller.hasEntries } }
    val resolved by remember(controller, fallbackDarkIcons) {
        derivedStateOf { controller.resolve(fallbackDarkIcons) }
    }
    var lastApplied by remember(applier) { mutableStateOf<ResolvedCouiSystemBarsStyle?>(null) }
    if (hasEntries || lastApplied != null) {
        // Read resolved during composition so snapshot changes inside any active() predicate
        // immediately recompose only this tiny sync leaf and invoke the applier in SideEffect.
        val target = resolved
        SideEffect {
            if (lastApplied != target) {
                lastApplied = target
                applier.apply(target)
            }
        }
    }
}

/**
 * Declares system-bar appearance and visibility for the current screen or overlay.
 *
 * Pass [active] driven by transition / drag progress (e.g. `{ !exiting && scrimAlpha >= 0.5f }`)
 * so that dismissing a dark overlay restores the underlying page's status-bar icon color on the
 * very first frame instead of waiting for composition disposal after the exit animation finishes.
 *
 * @param darkStatusBarIcons `true` for dark icons (light backgrounds), `false` for light icons
 *   (dark backgrounds), or `null` to inherit from the entry underneath.
 * @param darkNavigationBarIcons Contrast for navigation-bar icons; defaults to [darkStatusBarIcons].
 * @param statusBarVisible `true` to show the status bar, `false` to hide it, or `null` to inherit.
 * @param navigationBarVisible `true` to show the navigation bar / home indicator, `false` to hide
 *   it, or `null` to inherit.
 * @param active Snapshot-aware predicate controlling whether this entry currently participates in
 *   the stack. When `false`, the stack immediately resolves to lower active entries.
 */
@Composable
fun CouiSystemBarsEffect(
    darkStatusBarIcons: Boolean? = null,
    darkNavigationBarIcons: Boolean? = darkStatusBarIcons,
    statusBarVisible: Boolean? = null,
    navigationBarVisible: Boolean? = null,
    active: () -> Boolean = { true },
) {
    val controller = LocalCouiSystemBarsController.current ?: return
    val currentActive by rememberUpdatedState(active)
    val style = CouiSystemBarsStyle(
        darkStatusBarIcons = darkStatusBarIcons,
        darkNavigationBarIcons = darkNavigationBarIcons,
        statusBarVisible = statusBarVisible,
        navigationBarVisible = navigationBarVisible,
    )
    val entry = remember { CouiSystemBarsEntry(style) { currentActive() } }
    SideEffect {
        entry.style = style
    }
    DisposableEffect(controller, entry) {
        controller.attach(entry)
        onDispose {
            controller.detach(entry)
        }
    }
}

@Composable
internal expect fun rememberPlatformSystemBarsApplier(): CouiSystemBarsApplier
