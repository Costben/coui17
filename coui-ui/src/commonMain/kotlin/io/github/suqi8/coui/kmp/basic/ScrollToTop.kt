// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val LAZY_FAST_JUMP_THRESHOLD = 20

/**
 * Coordinates tap-to-scroll-to-top across a screen's top app bar, platform status bar events,
 * and the active scrollable container.
 *
 * [Scaffold] provides an instance through [LocalScrollToTopController] and connects platform
 * status bar tap listeners (such as ColorOS `com.oplus.clicktop`). Scrollable containers bind via
 * [BindScrollToTop] or [RegisterScrollToTop], and top app bars trigger it via
 * [Modifier.scrollToTopOnTap].
 */
@Stable
class ScrollToTopController internal constructor(
    private val scope: CoroutineScope,
) {
    private data class TargetEntry(
        val id: Any,
        val onScrollToTop: suspend () -> Unit,
    )

    private data class HeaderResetEntry(
        val id: Any,
        val onReset: suspend () -> Unit,
    )

    private val targets = mutableListOf<TargetEntry>()
    private val headerResets = mutableListOf<HeaderResetEntry>()
    private var blockerCount by mutableIntStateOf(0)
    private var activeJob: Job? = null

    /** Whether at least one scrollable target is registered and no blocker is active. */
    val hasTarget: Boolean
        get() = targets.isNotEmpty() && blockerCount == 0

    internal fun register(id: Any, onScrollToTop: suspend () -> Unit) {
        targets.removeAll { it.id == id }
        targets.add(TargetEntry(id = id, onScrollToTop = onScrollToTop))
    }

    internal fun unregister(id: Any) {
        targets.removeAll { it.id == id }
    }

    internal fun registerHeaderReset(id: Any, onReset: suspend () -> Unit) {
        headerResets.removeAll { it.id == id }
        headerResets.add(HeaderResetEntry(id = id, onReset = onReset))
    }

    internal fun unregisterHeaderReset(id: Any) {
        headerResets.removeAll { it.id == id }
    }

    internal fun pushBlocker() {
        blockerCount += 1
    }

    internal fun popBlocker() {
        if (blockerCount > 0) blockerCount -= 1
    }

    /** Triggers smooth scroll-to-top on the currently active registered target, if unblocked. */
    fun triggerScrollToTop() {
        if (blockerCount > 0) return
        val entry = targets.lastOrNull() ?: return
        activeJob?.cancel()
        activeJob = scope.launch {
            launch { entry.onScrollToTop() }
            headerResets.toList().forEach { reset ->
                launch { reset.onReset() }
            }
        }
    }
}

/** Provides the active [ScrollToTopController] for the current [Scaffold] subtree. */
val LocalScrollToTopController = staticCompositionLocalOf<ScrollToTopController?> { null }

/** Remembers a [ScrollToTopController] bound to the current composition's coroutine scope. */
@Composable
fun rememberScrollToTopController(): ScrollToTopController {
    val scope = rememberCoroutineScope()
    return remember(scope) { ScrollToTopController(scope) }
}

/**
 * Temporarily suppresses [LocalScrollToTopController] triggers while [active] is `true`, e.g. when
 * an in-window modal sheet or dialog obscures the page content.
 */
@Composable
fun ScrollToTopBlocker(active: Boolean) {
    val controller = LocalScrollToTopController.current ?: return
    DisposableEffect(controller, active) {
        if (!active) return@DisposableEffect onDispose {}
        controller.pushBlocker()
        onDispose { controller.popBlocker() }
    }
}

/**
 * Provides a [ScrollToTopController] to [content] and connects platform status bar tap events.
 *
 * Use this at the root of a screen shell that lays its bars out by hand instead of using
 * [Scaffold]. When a parent already provides a controller, this composes [content] unchanged so a
 * nested host never installs a second platform listener.
 */
@Composable
fun ScrollToTopHost(content: @Composable () -> Unit) {
    if (LocalScrollToTopController.current != null) {
        content()
        return
    }
    val controller = rememberScrollToTopController()
    PlatformStatusBarScrollToTopEffect(controller)
    CompositionLocalProvider(LocalScrollToTopController provides controller) {
        content()
    }
}

/**
 * Scopes a pager page to its own scroll-to-top handling while it is not [active].
 *
 * Pagers often keep neighbouring pages composed (`beyondViewportPageCount` > 0). Those pages would
 * otherwise register their scrollables with the controller the visible page uses, and the
 * controller triggers the most recently registered target, so a status bar tap could scroll an
 * off-screen page. An inactive page gets a detached controller: its bindings still compose and
 * dispose normally, but nothing triggers them, and they never displace the visible page's target.
 */
@Composable
fun ScrollToTopPage(
    active: Boolean,
    content: @Composable () -> Unit,
) {
    val parent = LocalScrollToTopController.current
    if (parent == null) {
        content()
        return
    }
    val detached = rememberScrollToTopController()
    CompositionLocalProvider(
        LocalScrollToTopController provides if (active) parent else detached,
    ) {
        content()
    }
}

/**
 * Registers a suspend [onScrollToTop] handler with [LocalScrollToTopController] while [enabled] is
 * `true` and this composable remains in composition.
 */
@Composable
fun RegisterScrollToTop(
    enabled: Boolean = true,
    onScrollToTop: suspend () -> Unit,
) {
    val controller = LocalScrollToTopController.current ?: return
    val latestAction by rememberUpdatedState(onScrollToTop)
    val key = remember { Any() }
    DisposableEffect(controller, enabled, key) {
        if (!enabled) return@DisposableEffect onDispose {}
        controller.register(key) { latestAction() }
        onDispose { controller.unregister(key) }
    }
}

/** Binds a [LazyListState] to [LocalScrollToTopController] for animated scroll-to-top. */
@Composable
fun BindScrollToTop(
    state: LazyListState,
    enabled: Boolean = true,
) {
    RegisterScrollToTop(enabled = enabled) {
        if (state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0) return@RegisterScrollToTop
        if (state.firstVisibleItemIndex > LAZY_FAST_JUMP_THRESHOLD) {
            state.scrollToItem(LAZY_FAST_JUMP_THRESHOLD)
        }
        state.animateScrollToItem(0)
    }
}

/** Binds a [LazyGridState] to [LocalScrollToTopController] for animated scroll-to-top. */
@Composable
fun BindScrollToTop(
    state: LazyGridState,
    enabled: Boolean = true,
) {
    RegisterScrollToTop(enabled = enabled) {
        if (state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0) return@RegisterScrollToTop
        if (state.firstVisibleItemIndex > LAZY_FAST_JUMP_THRESHOLD) {
            state.scrollToItem(LAZY_FAST_JUMP_THRESHOLD)
        }
        state.animateScrollToItem(0)
    }
}

/** Binds a [LazyStaggeredGridState] to [LocalScrollToTopController] for animated scroll-to-top. */
@Composable
fun BindScrollToTop(
    state: LazyStaggeredGridState,
    enabled: Boolean = true,
) {
    RegisterScrollToTop(enabled = enabled) {
        if (state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0) return@RegisterScrollToTop
        if (state.firstVisibleItemIndex > LAZY_FAST_JUMP_THRESHOLD) {
            state.scrollToItem(LAZY_FAST_JUMP_THRESHOLD)
        }
        state.animateScrollToItem(0)
    }
}

/** Binds a [ScrollState] to [LocalScrollToTopController] for animated scroll-to-top. */
@Composable
fun BindScrollToTop(
    state: ScrollState,
    enabled: Boolean = true,
) {
    RegisterScrollToTop(enabled = enabled) {
        if (state.value == 0) return@RegisterScrollToTop
        state.animateScrollTo(0)
    }
}

/**
 * Triggers [LocalScrollToTopController.triggerScrollToTop] on single tap of non-interactive areas.
 * Child clickable controls consume their own pointer events in the main pass before this modifier
 * sees them, so navigation and action buttons keep working normally.
 */
@Composable
fun Modifier.scrollToTopOnTap(
    enabled: Boolean = true,
): Modifier {
    val controller = LocalScrollToTopController.current
    if (!enabled || controller == null) return this
    val latestController by rememberUpdatedState(controller)
    return this.pointerInput(latestController) {
        detectTapGestures(
            onTap = {
                if (latestController.hasTarget) {
                    latestController.triggerScrollToTop()
                }
            },
        )
    }
}

/**
 * Registers a suspend [onReset] callback with [LocalScrollToTopController] to expand or reset a
 * custom top bar when scroll-to-top triggers, while [enabled] is `true` and this composable
 * remains in composition.
 */
@Composable
fun RegisterScrollToTopHeaderReset(
    enabled: Boolean = true,
    onReset: suspend () -> Unit,
) {
    val controller = LocalScrollToTopController.current ?: return
    val latestReset by rememberUpdatedState(onReset)
    val key = remember { Any() }
    DisposableEffect(controller, enabled, key) {
        if (!enabled) return@DisposableEffect onDispose {}
        controller.registerHeaderReset(key) { latestReset() }
        onDispose { controller.unregisterHeaderReset(key) }
    }
}

/**
 * Binds [scrollBehavior] to [LocalScrollToTopController] so tapping the top bar or status bar
 * expands the header and resets its scroll state alongside the list scrolling to top.
 */
@Composable
fun BindTopAppBarScrollReset(
    scrollBehavior: ScrollBehavior?,
    enabled: Boolean = true,
) {
    val state = scrollBehavior?.state ?: return
    RegisterScrollToTopHeaderReset(enabled = enabled) {
        state.contentOffset = 0f
        if (state.heightOffset < 0f) {
            Animatable(state.heightOffset).animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
            ) {
                state.heightOffset = value
            }
        }
    }
}

@Composable
internal expect fun PlatformStatusBarScrollToTopEffect(controller: ScrollToTopController)
