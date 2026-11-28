// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Close
import io.github.suqi8.coui.kmp.squircle.isSquircleEnabled
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.LocalCouiSystemBarsController
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

/**
 * Immutable data describing a single ColorOS 17 [Toast] request.
 *
 * @param id Monotonically increasing identifier assigned by [ToastHostState].
 * @param message Text displayed inside the toast capsule (clamped to two lines).
 * @param durationMillis How long the toast stays visible after finishing its enter animation.
 * @param actionLabel Optional trailing action label (for example `"Open"` or `"Undo"`).
 * @param withDismissAction Whether to show a trailing close icon button.
 * @param onAction Optional callback invoked at most once when the action or toast capsule is tapped.
 */
@Immutable
data class ToastData(
    val id: Long,
    val message: String,
    val durationMillis: Long = ToastDefaults.ShortDurationMillis,
    val actionLabel: String? = null,
    val withDismissAction: Boolean = false,
    val onAction: (() -> Unit)? = null,
)

/**
 * Colors used by a ColorOS 17 [Toast].
 *
 * Defaults follow ColorOS 17 `SystemUI` (`oplus_toast_bg_color`, `oplus_toast_text_color` and
 * `OplusToastLayout` shadow colours): `#FFFFFF` / `#FF000000` with `#80000000` spot shadow in
 * light theme, and `#333333` / `#E6FFFFFF` with `#88AAAAAA` spot shadow in dark theme.
 *
 * @param containerColor Solid capsule fill color.
 * @param contentColor Primary message text color.
 * @param actionContentColor Color of the optional trailing action label.
 * @param dismissActionContentColor Tint of the optional trailing close icon.
 * @param spotShadowColor Spot shadow color rendered under the elevated capsule.
 * @param ambientShadowColor Ambient shadow color (transparent by default to match SystemUI).
 */
@Immutable
data class ToastColors(
    val containerColor: Color,
    val contentColor: Color,
    val actionContentColor: Color,
    val dismissActionContentColor: Color,
    val spotShadowColor: Color,
    val ambientShadowColor: Color,
)

/**
 * State holder for [ToastHost], mirroring ColorOS 17 `OplusToastUI`'s single-slot replacement
 * behavior.
 *
 * At most one toast is visible at a time in a fixed bottom-center slot without vertical sliding or
 * stacking. Sending a new toast while one is entering or holding interrupts the current toast,
 * plays the 250ms exit animation, and then plays the 333ms enter animation for the newest request
 * in place; any extra requests arriving during the exit animation are coalesced so only the latest
 * message is shown.
 */
@Stable
class ToastHostState {
    internal val pendingRequests = Channel<ToastData>(Channel.CONFLATED)
    internal val manualDismiss = Channel<Unit>(Channel.CONFLATED)
    private var nextId = 0L

    /** The toast currently being presented (including during enter/exit animation), or `null`. */
    var currentToast: ToastData? by mutableStateOf(null)
        internal set

    /**
     * Whether a toast is currently active. Callers can inspect this before reading
     * `WindowInsets.ime` or other expensive insets so keyboard animations avoid redundant
     * recompositions while idle.
     */
    val hasActiveToast: Boolean
        get() = currentToast != null

    internal var motionState: ToastMotionState? = null

    /**
     * Enqueues a toast message for display in [ToastHost].
     *
     * Calling this while another toast is active interrupts it and transitions in place after its
     * exit animation completes. Rapid successive calls coalesce to the latest message.
     *
     * @param message Text to display inside the toast capsule.
     * @param durationMillis Hold duration in milliseconds after enter completes. Must be positive.
     * @param actionLabel Optional trailing action label.
     * @param withDismissAction Whether to show a trailing close icon button.
     * @param onAction Optional callback invoked when the action or capsule is tapped.
     */
    fun showToast(
        message: String,
        durationMillis: Long = ToastDefaults.ShortDurationMillis,
        actionLabel: String? = null,
        withDismissAction: Boolean = false,
        onAction: (() -> Unit)? = null,
    ) {
        require(durationMillis > 0L) { "durationMillis must be greater than 0" }
        val request = ToastData(
            id = ++nextId,
            message = message,
            durationMillis = durationMillis,
            actionLabel = actionLabel,
            withDismissAction = withDismissAction,
            onAction = onAction,
        )
        pendingRequests.trySend(request)
    }

    /**
     * Dismisses the currently visible toast, playing its exit animation if one is active.
     */
    fun dismiss() {
        if (currentToast != null) {
            manualDismiss.trySend(Unit)
        }
    }
}

/**
 * Remembers a [ToastHostState] across recompositions.
 */
@Composable
fun rememberToastHostState(): ToastHostState = remember { ToastHostState() }

/**
 * Single-slot host for ColorOS 17 native [Toast]s (`OplusToastUI` + `OplusToastDefaultAnimation`).
 *
 * Keep `ToastHost` mounted in composition; when [ToastHostState.hasActiveToast] is `false`, it
 * returns immediately without emitting any full-screen container or subscribing to
 * `WindowInsets.ime`, preventing frame drops during keyboard animations.
 *
 * Enter animation (`333ms`):
 * - container scale `0.9 -> 1.0` (`333ms`, `CubicBezierEasing(0f, 0f, 0f, 1f)`)
 * - container alpha `0 -> 1` (`66ms`, `LinearEasing`)
 * - content alpha `0 -> 1` (`delay = 50ms`, `283ms`, `LinearEasing`)
 * - shadow elevation fixed at `12.dp`
 *
 * Exit animation (`250ms`):
 * - container scale `1.0 -> 0.9` (`250ms`, `CubicBezierEasing(0.3f, 0f, 1f, 1f)`)
 * - content alpha `1 -> 0` (`166ms`, `LinearEasing`)
 * - shadow elevation `12.dp -> 0.dp` (`delay = 150ms`, `40ms`, `LinearEasing`)
 * - container alpha `1 -> 0` (`delay = 150ms`, `100ms`, `LinearEasing`)
 *
 * @param state State controlling which toast is displayed.
 * @param modifier Modifier applied to the overlay container when a toast is active.
 * @param contentAlignment Alignment of the toast inside the host container.
 * @param contentPadding Outer margins applied around the toast inside the host container.
 * @param applySystemInsets Whether to apply navigation-bar and IME insets when a toast is active.
 *   Set to `false` if the parent already applies system/IME insets.
 * @param lockNavigationBarInsets When [applySystemInsets] is `true`, locks the non-immersive
 *   navigation-bar height so toasts pop and stay at the same fixed vertical position across normal
 *   and immersive pages (including when the system navigation bar is hidden or animating). Defaults
 *   to `true`.
 * @param imeBottomPadding Clearance between the top of the software keyboard (IME) and the toast
 *   when [applySystemInsets] is `true` and the keyboard rises above the resting bottom offset
 *   (`lockedNavigationBarBottom + contentPadding.calculateBottomPadding()`). Defaults to
 *   [ToastDefaults.ImeBottomPadding] (`24.dp`), avoiding double-counting navigation-bar height or
 *   floating tall bottom-bar offsets above the keyboard.
 * @param toast Composable rendering each [ToastData]; defaults to [Toast].
 */
@Composable
fun ToastHost(
    state: ToastHostState,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.BottomCenter,
    contentPadding: PaddingValues = ToastDefaults.HostContentPadding,
    applySystemInsets: Boolean = true,
    lockNavigationBarInsets: Boolean = true,
    imeBottomPadding: Dp = ToastDefaults.ImeBottomPadding,
    toast: @Composable (ToastData) -> Unit = { data ->
        Toast(
            data = data,
            onDismiss = state::dismiss,
        )
    },
) {
    val motionState = remember(state) { ToastMotionState() }
    val navInsetCache = remember { ToastNavigationBarInsetCache() }
    val overrides = LocalToastInsetsOverrides.current
    val platformLockedNavInsets = overrides?.platformLockedNavigationBarsPx
        ?: rememberPlatformLockedNavigationBarInsetsPx()

    if (applySystemInsets && lockNavigationBarInsets &&
        (overrides != null || platformNeedsCompositionNavBarObserver())
    ) {
        ToastNavigationBarInsetObserver(
            cache = navInsetCache,
            navigationBars = overrides?.navigationBars,
        )
    }

    LaunchedEffect(state) {
        state.motionState = motionState
        try {
            var current = state.pendingRequests.receive()
            while (true) {
                // Drain any stale manual dismiss signal so a newly arriving toast does not exit immediately.
                state.manualDismiss.tryReceive()
                motionState.prepareEnter()
                state.currentToast = current

                val holdJob = launch {
                    motionState.playEnter()
                    delay(current.durationMillis)
                }
                var nextQueued = select<ToastData?> {
                    holdJob.onJoin { null }
                    state.pendingRequests.onReceive { it }
                    state.manualDismiss.onReceive { null }
                }
                if (holdJob.isActive) holdJob.cancelAndJoin()

                val outJob = launch { motionState.playExit() }
                while (outJob.isActive) {
                    val arrived = select<ToastData?> {
                        outJob.onJoin { null }
                        state.pendingRequests.onReceive { it }
                    }
                    if (arrived != null) nextQueued = arrived
                }

                // Prefer any newest request that raced with the final frame of the exit animation.
                val next = state.pendingRequests.tryReceive().getOrNull() ?: nextQueued
                if (next != null) {
                    current = next
                } else {
                    state.currentToast = null
                    current = state.pendingRequests.receive()
                }
            }
        } finally {
            state.currentToast = null
            if (state.motionState === motionState) {
                state.motionState = null
            }
        }
    }

    // Avoid composing a full-screen Box or subscribing to WindowInsets.ime while idle.
    val activeToast = state.currentToast ?: return

    ToastHostOverlay(
        activeToast = activeToast,
        motionState = motionState,
        navInsetCache = navInsetCache,
        platformLockedNavInsets = platformLockedNavInsets,
        overrides = overrides,
        modifier = modifier,
        contentAlignment = contentAlignment,
        contentPadding = contentPadding,
        applySystemInsets = applySystemInsets,
        lockNavigationBarInsets = lockNavigationBarInsets,
        imeBottomPadding = imeBottomPadding,
        toast = toast,
    )
}

@Composable
private fun ToastNavigationBarInsetObserver(
    cache: ToastNavigationBarInsetCache,
    navigationBars: WindowInsets?,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val containerSize = LocalWindowInfo.current.containerSize
    val systemBarsController = LocalCouiSystemBarsController.current
    val navBarVisible by remember(systemBarsController) {
        derivedStateOf {
            systemBarsController?.resolve(fallbackDarkIcons = true)?.navigationBarVisible ?: true
        }
    }
    val resolvedNavBars = navigationBars ?: WindowInsets.navigationBars
    val liveNav = ToastSystemBarInsetsPx(
        left = resolvedNavBars.getLeft(density, layoutDirection).coerceAtLeast(0),
        right = resolvedNavBars.getRight(density, layoutDirection).coerceAtLeast(0),
        bottom = resolvedNavBars.getBottom(density).coerceAtLeast(0),
    )
    cache.record(
        containerSize = containerSize,
        navigationBarVisible = navBarVisible,
        liveInsets = liveNav,
    )
}

@Composable
private fun ToastHostOverlay(
    activeToast: ToastData,
    motionState: ToastMotionState,
    navInsetCache: ToastNavigationBarInsetCache,
    platformLockedNavInsets: () -> ToastSystemBarInsetsPx?,
    overrides: ToastInsetsOverrides?,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment,
    contentPadding: PaddingValues,
    applySystemInsets: Boolean,
    lockNavigationBarInsets: Boolean,
    imeBottomPadding: Dp,
    toast: @Composable (ToastData) -> Unit,
) {
    val paddingModifier = if (applySystemInsets) {
        val density = LocalDensity.current
        val layoutDirection = LocalLayoutDirection.current
        val containerSize = LocalWindowInfo.current.containerSize
        val navBars = overrides?.navigationBars ?: WindowInsets.navigationBars
        val liveNav = ToastSystemBarInsetsPx(
            left = navBars.getLeft(density, layoutDirection).coerceAtLeast(0),
            right = navBars.getRight(density, layoutDirection).coerceAtLeast(0),
            bottom = navBars.getBottom(density).coerceAtLeast(0),
        )
        val systemBarsController = LocalCouiSystemBarsController.current
        val navBarVisible = systemBarsController?.resolve(fallbackDarkIcons = true)?.navigationBarVisible ?: true
        val effectiveNav = if (lockNavigationBarInsets) {
            navInsetCache.record(
                containerSize = containerSize,
                navigationBarVisible = navBarVisible,
                liveInsets = liveNav,
            )
            val platformNav = platformLockedNavInsets()
            when {
                platformNav != null && !platformNeedsCompositionNavBarObserver() -> platformNav
                platformNav != null -> navInsetCache.resolve(containerSize, liveNav).maxWith(platformNav)
                else -> navInsetCache.resolve(containerSize, liveNav)
            }
        } else {
            liveNav
        }

        val contentStartPx = with(density) { contentPadding.calculateStartPadding(layoutDirection).roundToPx() }
        val contentEndPx = with(density) { contentPadding.calculateEndPadding(layoutDirection).roundToPx() }
        val contentTopPx = with(density) { contentPadding.calculateTopPadding().roundToPx() }
        val contentBottomPx = with(density) { contentPadding.calculateBottomPadding().roundToPx() }
        val navStartPx = if (layoutDirection == LayoutDirection.Ltr) effectiveNav.left else effectiveNav.right
        val navEndPx = if (layoutDirection == LayoutDirection.Ltr) effectiveNav.right else effectiveNav.left
        val restingBottomPx = effectiveNav.bottom + contentBottomPx
        val ime = overrides?.ime ?: WindowInsets.ime
        val imeBottomPx = ime.getBottom(density).coerceAtLeast(0)
        val effectiveBottomPx = if (imeBottomPx > 0) {
            val imePaddingPx = with(density) { imeBottomPadding.roundToPx().coerceAtLeast(0) }
            maxOf(restingBottomPx, imeBottomPx + imePaddingPx)
        } else {
            restingBottomPx
        }

        Modifier.padding(
            start = with(density) { (navStartPx + contentStartPx).toDp() },
            top = with(density) { contentTopPx.toDp() },
            end = with(density) { (navEndPx + contentEndPx).toDp() },
            bottom = with(density) { effectiveBottomPx.toDp() },
        )
    } else {
        Modifier.padding(contentPadding)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(paddingModifier),
        contentAlignment = contentAlignment,
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = motionState.containerScale.value
                scaleY = motionState.containerScale.value
                alpha = motionState.containerAlpha.value
            },
        ) {
            CompositionLocalProvider(LocalToastMotionState provides motionState) {
                toast(activeToast)
            }
        }
    }
}

/**
 * ColorOS 17 native-style capsule toast (`text_toast_oplus.xml` + `OplusToastLayout`) rendered
 * from [ToastData].
 *
 * Uses [CapsuleOutlineShape] for the continuous-curvature capsule background, shadow, and click
 * clipping, with strict vertical text centering (`LineHeightStyle.Alignment.Center` +
 * `LineHeightStyle.Trim.Both`).
 *
 * @param data Toast payload supplied by [ToastHost].
 * @param modifier Modifier applied to the toast capsule.
 * @param colors Capsule, text, action, close icon, and shadow colors.
 * @param elevation Shadow elevation of the capsule (`12.dp` by default).
 * @param onDismiss Callback invoked when the user taps the action or close button.
 * @param dismissContentDescription Accessibility content description for the close button.
 */
@Composable
fun Toast(
    data: ToastData,
    modifier: Modifier = Modifier,
    colors: ToastColors = ToastDefaults.colors(),
    elevation: Dp = ToastDefaults.Elevation,
    onDismiss: (() -> Unit)? = null,
    dismissContentDescription: String = ToastDefaults.DismissContentDescription,
) {
    var interactionConsumed by remember(data.id) { mutableStateOf(false) }

    Toast(
        message = data.message,
        modifier = modifier,
        actionLabel = data.actionLabel,
        withDismissAction = data.withDismissAction,
        onAction = data.onAction?.let { callback ->
            {
                if (!interactionConsumed) {
                    interactionConsumed = true
                    callback()
                }
            }
        },
        onDismiss = onDismiss?.let { dismissCallback ->
            {
                interactionConsumed = true
                dismissCallback()
            }
        },
        colors = colors,
        elevation = elevation,
        dismissContentDescription = dismissContentDescription,
    )
}

/**
 * ColorOS 17 native-style capsule toast (`text_toast_oplus.xml` + `OplusToastLayout`).
 *
 * Can be rendered inside [ToastHost] (where it automatically coordinates content alpha and shadow
 * fade-out with `OplusToastDefaultAnimation`) or standalone for previews and custom layouts.
 *
 * @param message Primary toast text, clamped to two lines with ellipsis.
 * @param modifier Modifier applied to the toast capsule.
 * @param actionLabel Optional trailing action label. When non-blank, tapping either the capsule or
 *   the action label triggers [onAction] followed by [onDismiss].
 * @param withDismissAction Whether to render a trailing close icon button that invokes [onDismiss].
 * @param onAction Optional callback invoked when the action is triggered.
 * @param onDismiss Optional callback invoked when the toast is dismissed via action or close tap.
 * @param colors Capsule, text, action, close icon, and shadow colors.
 * @param elevation Shadow elevation of the capsule (`12.dp` by default).
 * @param dismissContentDescription Accessibility content description for the close button.
 */
@Composable
fun Toast(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    withDismissAction: Boolean = false,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    colors: ToastColors = ToastDefaults.colors(),
    elevation: Dp = ToastDefaults.Elevation,
    dismissContentDescription: String = ToastDefaults.DismissContentDescription,
) {
    val motionState = LocalToastMotionState.current
    val squircleEnabled = isSquircleEnabled()
    val shape = remember(squircleEnabled) { CapsuleOutlineShape(squircleEnabled = squircleEnabled) }
    val textStyle = ToastDefaults.textStyle()
    val hasAction = !actionLabel.isNullOrBlank()
    var lineCount by remember(message) { mutableIntStateOf(1) }
    val verticalPadding = if (lineCount >= 2) {
        ToastDefaults.DoubleLineVerticalPadding
    } else {
        ToastDefaults.SingleLineVerticalPadding
    }

    val containerPaddingModifier = when {
        hasAction && withDismissAction ->
            Modifier
                .clip(shape)
                .clickable {
                    onAction?.invoke()
                    onDismiss?.invoke()
                }
                .padding(
                    start = ToastDefaults.InteractiveStartPadding,
                    top = ToastDefaults.InteractiveVerticalPadding,
                    end = ToastDefaults.InteractiveDismissEndPadding,
                    bottom = ToastDefaults.InteractiveVerticalPadding,
                )

        hasAction ->
            Modifier
                .clip(shape)
                .clickable {
                    onAction?.invoke()
                    onDismiss?.invoke()
                }
                .padding(
                    start = ToastDefaults.InteractiveStartPadding,
                    top = ToastDefaults.InteractiveVerticalPadding,
                    end = ToastDefaults.InteractiveActionEndPadding,
                    bottom = ToastDefaults.InteractiveVerticalPadding,
                )

        withDismissAction ->
            Modifier
                .clip(shape)
                .padding(
                    start = ToastDefaults.InteractiveStartPadding,
                    top = ToastDefaults.InteractiveVerticalPadding,
                    end = ToastDefaults.InteractiveDismissEndPadding,
                    bottom = ToastDefaults.InteractiveVerticalPadding,
                )

        else ->
            Modifier.padding(
                horizontal = ToastDefaults.HorizontalPadding,
                vertical = verticalPadding,
            )
    }

    Box(
        modifier = modifier
            .semantics { liveRegion = LiveRegionMode.Polite }
            .graphicsLayer {
                val fraction = motionState?.elevationFraction?.value ?: 1f
                shadowElevation = (elevation.value * fraction).coerceAtLeast(0f).dp.toPx()
                this.shape = shape
                clip = false
                ambientShadowColor = colors.ambientShadowColor
                spotShadowColor = colors.spotShadowColor
            }
            .background(colors.containerColor, shape)
            .heightIn(min = ToastDefaults.MinHeight)
            .widthIn(max = ToastDefaults.MaxWidth)
            .then(containerPaddingModifier),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.graphicsLayer {
                alpha = motionState?.contentAlpha?.value ?: 1f
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                style = textStyle,
                color = colors.contentColor,
                maxLines = ToastDefaults.MaxLines,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { result -> lineCount = result.lineCount },
                modifier = Modifier.weight(1f, fill = false),
            )
            if (hasAction) {
                Spacer(Modifier.width(ToastDefaults.ActionSpacing))
                Text(
                    text = actionLabel.orEmpty(),
                    style = textStyle,
                    color = colors.actionContentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(shape)
                        .clickable {
                            onAction?.invoke()
                            onDismiss?.invoke()
                        }
                        .padding(
                            horizontal = ToastDefaults.ActionHorizontalPadding,
                            vertical = ToastDefaults.ActionVerticalPadding,
                        ),
                )
            }
            if (withDismissAction) {
                Spacer(
                    Modifier.width(
                        if (hasAction) {
                            ToastDefaults.ActionToDismissSpacing
                        } else {
                            ToastDefaults.DismissOnlySpacing
                        },
                    ),
                )
                Box(
                    modifier = Modifier
                        .size(ToastDefaults.DismissButtonSize)
                        .clip(CircleShape)
                        .clickable { onDismiss?.invoke() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = COUIIcons.Basic.Close,
                        contentDescription = dismissContentDescription,
                        tint = colors.dismissActionContentColor,
                        modifier = Modifier.size(ToastDefaults.DismissIconSize),
                    )
                }
            }
        }
    }
}

/**
 * Default tokens, timings, curves, typography, and colors for ColorOS 17 [Toast] and [ToastHost].
 */
object ToastDefaults {
    /** Default short toast hold duration (`2000ms`), matching ColorOS 17 `Toast.LENGTH_SHORT`. */
    val ShortDurationMillis: Long = 2000L

    /** Long toast hold duration (`3500ms`), matching `Toast.LENGTH_LONG`. */
    val LongDurationMillis: Long = 3500L

    /** Total duration of the `OplusToastDefaultAnimation` enter sequence (`333ms`). */
    val EnterDurationMillis: Int = 333

    /** Total duration of the `OplusToastDefaultAnimation` exit sequence (`250ms`). */
    val ExitDurationMillis: Int = 250

    /** Maximum number of text lines rendered inside a toast (`text_toast_oplus.xml`). */
    val MaxLines: Int = 2

    /** Default accessibility description for the dismiss button. */
    val DismissContentDescription: String = "Dismiss"

    /** Default capsule shadow elevation (`12.dp`). */
    val Elevation: Dp = 12.dp

    /** Minimum capsule height (`oplus_toast_min_height`, `48.dp`). */
    val MinHeight: Dp = 48.dp

    /** Maximum capsule width (`328.dp`). */
    val MaxWidth: Dp = 328.dp

    /** Horizontal padding for plain text-only toasts (`24.dp`). */
    val HorizontalPadding: Dp = 24.dp

    /** Vertical padding for single-line plain toasts (`14.dp`). */
    val SingleLineVerticalPadding: Dp = 14.dp

    /** Vertical padding for two-line plain toasts (`oplus_toast_double_content_padding_top`, `12.dp`). */
    val DoubleLineVerticalPadding: Dp = 12.dp

    /** Default outer padding applied by [ToastHost]. */
    val HostContentPadding: PaddingValues = PaddingValues(start = 24.dp, end = 24.dp, bottom = 44.dp)

    /**
     * Default clearance between the top of the software keyboard (IME) and the toast capsule when
     * the keyboard lifts the toast above its resting position (`24.dp`).
     */
    val ImeBottomPadding: Dp = 24.dp

    /** Container scale easing during enter (`CubicBezierEasing(0f, 0f, 0f, 1f)`). */
    val EnterScaleEasing: Easing = CubicBezierEasing(0f, 0f, 0f, 1f)

    /** Container scale easing during exit (`CubicBezierEasing(0.3f, 0f, 1f, 1f)`). */
    val ExitScaleEasing: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)

    internal val InteractiveStartPadding: Dp = 20.dp
    internal val InteractiveVerticalPadding: Dp = 10.dp
    internal val InteractiveActionEndPadding: Dp = 12.dp
    internal val InteractiveDismissEndPadding: Dp = 10.dp
    internal val ActionSpacing: Dp = 12.dp
    internal val ActionHorizontalPadding: Dp = 8.dp
    internal val ActionVerticalPadding: Dp = 4.dp
    internal val ActionToDismissSpacing: Dp = 2.dp
    internal val DismissOnlySpacing: Dp = 8.dp
    internal val DismissButtonSize: Dp = 28.dp
    internal val DismissIconSize: Dp = 14.dp

    /**
     * Default toast text style (`14.sp`, `20.sp` line height, `FontWeight.Medium`).
     *
     * Explicitly enforces [LineHeightStyle.Alignment.Center] and [LineHeightStyle.Trim.Both] so
     * text stays strictly vertically centered inside the capsule instead of inheriting
     * `COUITheme`'s top-aligned `LineHeightStyle`.
     */
    @Composable
    fun textStyle(): TextStyle {
        val base = COUITheme.textStyles.body2
        return remember(base) {
            base.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                lineHeightStyle = ToastLineHeightStyle,
            )
        }
    }

    /**
     * Resolves ColorOS 17 light/dark [ToastColors] from the active [COUITheme].
     */
    @Composable
    fun colors(
        containerColor: Color = defaultContainerColor(),
        contentColor: Color = defaultContentColor(),
        actionContentColor: Color = COUITheme.colorScheme.primary,
        dismissActionContentColor: Color = contentColor.copy(alpha = 0.55f),
        spotShadowColor: Color = defaultSpotShadowColor(),
        ambientShadowColor: Color = Color.Transparent,
    ): ToastColors = remember(
        containerColor,
        contentColor,
        actionContentColor,
        dismissActionContentColor,
        spotShadowColor,
        ambientShadowColor,
    ) {
        ToastColors(
            containerColor = containerColor,
            contentColor = contentColor,
            actionContentColor = actionContentColor,
            dismissActionContentColor = dismissActionContentColor,
            spotShadowColor = spotShadowColor,
            ambientShadowColor = ambientShadowColor,
        )
    }

    @Composable
    private fun isDark(): Boolean = COUITheme.colorScheme.background.luminance() < 0.5f

    @Composable
    private fun defaultContainerColor(): Color = if (isDark()) ToastBgDark else ToastBgLight

    @Composable
    private fun defaultContentColor(): Color = if (isDark()) ToastTextDark else ToastTextLight

    @Composable
    private fun defaultSpotShadowColor(): Color = if (isDark()) ToastSpotShadowDark else ToastSpotShadowLight
}

@Stable
internal class ToastMotionState {
    val containerScale: Animatable<Float, AnimationVector1D> = Animatable(TOAST_MIN_SCALE)
    val containerAlpha: Animatable<Float, AnimationVector1D> = Animatable(0f)
    val contentAlpha: Animatable<Float, AnimationVector1D> = Animatable(0f)
    val elevationFraction: Animatable<Float, AnimationVector1D> = Animatable(0f)

    suspend fun prepareEnter() {
        containerScale.snapTo(TOAST_MIN_SCALE)
        containerAlpha.snapTo(0f)
        contentAlpha.snapTo(0f)
        elevationFraction.snapTo(1f)
    }

    suspend fun playEnter() = coroutineScope {
        launch {
            containerScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = ToastDefaults.EnterDurationMillis,
                    easing = ToastDefaults.EnterScaleEasing,
                ),
            )
        }
        launch {
            containerAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = TOAST_ENTER_CONTAINER_ALPHA_DURATION_MS,
                    easing = LinearEasing,
                ),
            )
        }
        launch {
            contentAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = TOAST_ENTER_CONTENT_ALPHA_DURATION_MS,
                    delayMillis = TOAST_ENTER_CONTENT_ALPHA_DELAY_MS,
                    easing = LinearEasing,
                ),
            )
        }
    }

    suspend fun playExit() = coroutineScope {
        launch {
            containerScale.animateTo(
                targetValue = TOAST_MIN_SCALE,
                animationSpec = tween(
                    durationMillis = ToastDefaults.ExitDurationMillis,
                    easing = ToastDefaults.ExitScaleEasing,
                ),
            )
        }
        launch {
            contentAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = TOAST_EXIT_CONTENT_ALPHA_DURATION_MS,
                    easing = LinearEasing,
                ),
            )
        }
        launch {
            elevationFraction.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = TOAST_EXIT_ELEVATION_DURATION_MS,
                    delayMillis = TOAST_EXIT_ELEVATION_DELAY_MS,
                    easing = LinearEasing,
                ),
            )
        }
        launch {
            containerAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = TOAST_EXIT_CONTAINER_ALPHA_DURATION_MS,
                    delayMillis = TOAST_EXIT_CONTAINER_ALPHA_DELAY_MS,
                    easing = LinearEasing,
                ),
            )
        }
    }
}

private val LocalToastMotionState = compositionLocalOf<ToastMotionState?> { null }

internal data class ToastInsetsOverrides(
    val navigationBars: WindowInsets? = null,
    val ime: WindowInsets? = null,
    val platformLockedNavigationBarsPx: (() -> ToastSystemBarInsetsPx?)? = null,
)

internal val LocalToastInsetsOverrides = compositionLocalOf<ToastInsetsOverrides?> { null }

/**
 * Per-window-size cache of non-immersive navigation-bar insets.
 *
 * Only records insets while `navigationBarVisible` is `true` and merges with component-wise `max`
 * within the same `containerSize`, so neither immersive hiding nor hide/restore transition frames
 * shrink the locked height, while orientation / window-size changes keep independent entries.
 */
internal class ToastNavigationBarInsetCache {
    private val lockedBySize = LinkedHashMap<IntSize, ToastSystemBarInsetsPx>()

    fun record(
        containerSize: IntSize,
        navigationBarVisible: Boolean,
        liveInsets: ToastSystemBarInsetsPx,
    ) {
        if (!navigationBarVisible || !liveInsets.isNonZero) return
        val existing = lockedBySize.remove(containerSize)
        lockedBySize[containerSize] = existing?.maxWith(liveInsets) ?: liveInsets
        while (lockedBySize.size > MAX_CACHED_WINDOW_SIZES) {
            val oldestKey = lockedBySize.entries.firstOrNull()?.key ?: break
            lockedBySize.remove(oldestKey)
        }
    }

    fun resolve(
        containerSize: IntSize,
        liveInsets: ToastSystemBarInsetsPx,
    ): ToastSystemBarInsetsPx {
        val cached = lockedBySize[containerSize]
        return cached?.maxWith(liveInsets) ?: liveInsets
    }
}

private const val MAX_CACHED_WINDOW_SIZES = 4
private const val TOAST_MIN_SCALE = 0.9f
private const val TOAST_ENTER_CONTAINER_ALPHA_DURATION_MS = 66
private const val TOAST_ENTER_CONTENT_ALPHA_DELAY_MS = 50
private const val TOAST_ENTER_CONTENT_ALPHA_DURATION_MS = 283
private const val TOAST_EXIT_CONTENT_ALPHA_DURATION_MS = 166
private const val TOAST_EXIT_ELEVATION_DELAY_MS = 150
private const val TOAST_EXIT_ELEVATION_DURATION_MS = 40
private const val TOAST_EXIT_CONTAINER_ALPHA_DELAY_MS = 150
private const val TOAST_EXIT_CONTAINER_ALPHA_DURATION_MS = 100

private val ToastBgLight = Color(0xFFFFFFFF)
private val ToastBgDark = Color(0xFF333333)
private val ToastTextLight = Color(0xFF000000)
private val ToastTextDark = Color(0xE6FFFFFF)
private val ToastSpotShadowLight = Color(0x80000000)
private val ToastSpotShadowDark = Color(0x88AAAAAA)

private val ToastLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.Both,
)
