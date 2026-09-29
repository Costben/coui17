// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.squircle.squircleSurface
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.delay

/**
 * The ColorOS 17 auto-scrolling banner: a horizontal row of rounded cards with a page indicator
 * underneath, the port of `res/layout/coui_banner_content_layout.xml` of the Clock APK.
 *
 * That layout is a `merge` of three siblings — `COUIBannerRecyclerView` (match_parent),
 * `androidx.viewpager2.widget.ViewPager2` (match_parent) and `COUIPageIndicatorKit` (wrap_content,
 * `layout_marginBottom` 10dp, `forceDarkAllowed` false, `dotColor` `@color/coui_page_indicator_dot_color_in_dark_bg`,
 * `traceDotColor` `@color/coui_color_label_primary_dark`) — so the Clock app itself only ever shows one
 * of the two scrolling views. This port keeps the pager half and drops the RecyclerView half:
 * [HorizontalPager] reproduces `ViewPager2`, and [BannerIndicator] reproduces `COUIPageIndicatorKit`.
 *
 * The scrolling view is configured by `COUIBannerRecyclerView.c()`: a horizontal `LinearLayoutManager`,
 * `setHorizontalItemAlign(1)`, native over-scroll on, `setHorizontalFlingFriction` at 2.5x the
 * platform scroll friction, `setPadding(24dp, 0, 24dp, 0)` with `setClipToPadding(false)`, and a
 * horizontal `DividerItemDecoration` whose `coui_item_decoration_8dp` drawable is an 8dp-wide
 * transparent shape. [contentPadding] and [itemSpacing] carry the 24dp and the 8dp; the over-scroll,
 * the item align and the fling friction have no [HorizontalPager] equivalent and are not applied.
 *
 * **There is no auto-scroll in the native at all.** Neither `COUIBannerRecyclerView` nor
 * `COUIPageIndicatorKit` owns a timer, and no class in the Clock dex references either of them — the
 * layout is an unreferenced resource of the APK, so the advance interval is whatever the host app
 * would have written. That is why [autoScrollIntervalMillis] has no default: any value here would be
 * invented.
 *
 * Not ported:
 * - `setHorizontalItemAlign(1)` and `setIsUseNativeOverScroll(true)`, which are
 *   `COUIRecyclerView` features with no Compose counterpart;
 * - the 2.5x fling friction ([BannerDefaults.FlingFrictionScale] is exposed but unapplied);
 * - `COUIPageIndicatorKit.setIsClickable` / `setOnDotClickListener`: the listener interface is empty
 *   (`COUIPageIndicatorKit.e` declares no members) and the click handler's body is empty, so the dot
 *   click path cannot deliver anything even though the dots take a click listener;
 * - the sticky bezier of `mPortStickyPath` / `mDepartStickyPath`: `mPortPosition` and `mDepartPosition`
 *   are only ever set to `MIS_POSITION` by `w()`, which clears the paths, so no code fills them.
 *
 * @param pageCount The number of cards. It must match the page count of [state], because the pager
 *   takes its page count from [state] while the indicator and the auto-scroll wrap-around take theirs
 *   from here.
 * @param autoScrollIntervalMillis How long a card stays before the banner advances to the next one,
 *   in milliseconds. Required, because the native defines no interval — see above. Values `<= 0`, a
 *   [pageCount] of at most 1, and [autoScrollEnabled] `false` all switch the auto-scroll off.
 * @param modifier The modifier to be applied to the banner. The pager fills the height it leaves over,
 *   so give the banner a height for the cards to be visible.
 * @param state The pager state. Defaults to a fresh state over [pageCount].
 * @param autoScrollEnabled Whether the banner advances by itself.
 * @param itemCornerRadius The corner radius of a card. The native has no item styling at all — the
 *   cards are host-provided views — so the port draws a [squircleSurface] card and borrows
 *   [CardDefaults.CornerRadius], the same 17dp the ColorOS 17 [Card] uses.
 * @param itemColor The fill of a card, [COUITheme.colorScheme.surfaceContainer] by default.
 * @param contentPadding The horizontal inset of the card row, `setPadding(24, 0, 24, 0)`, 24dp.
 * @param itemSpacing The gap between two cards, `coui_item_decoration_8dp`, 8dp.
 * @param indicatorMarginBottom The space under the indicator, the layout's `layout_marginBottom`,
 *   10dp.
 * @param indicatorColors The dot and trace colours of the indicator.
 * @param content The card at the given page.
 */
@Composable
fun Banner(
    pageCount: Int,
    autoScrollIntervalMillis: Long,
    modifier: Modifier = Modifier,
    state: PagerState = rememberPagerState(pageCount = { pageCount }),
    autoScrollEnabled: Boolean = true,
    itemCornerRadius: Dp = CardDefaults.CornerRadius,
    itemColor: Color = COUITheme.colorScheme.surfaceContainer,
    contentPadding: Dp = BannerDefaults.ContentPadding,
    itemSpacing: Dp = BannerDefaults.ItemSpacing,
    indicatorMarginBottom: Dp = BannerDefaults.IndicatorMarginBottom,
    indicatorColors: BannerIndicatorColors = BannerDefaults.bannerIndicatorColors(),
    content: @Composable (page: Int) -> Unit,
) {
    Column(modifier = modifier) {
        HorizontalPager(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = contentPadding),
            pageSpacing = itemSpacing,
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .squircleSurface(color = itemColor, cornerRadius = itemCornerRadius),
            ) {
                content(page)
            }
        }
        BannerIndicator(
            count = pageCount,
            currentIndex = state.currentPage,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = indicatorMarginBottom),
            colors = indicatorColors,
        )
    }

    LaunchedEffect(state, pageCount, autoScrollEnabled, autoScrollIntervalMillis) {
        if (!autoScrollEnabled || pageCount <= 1 || autoScrollIntervalMillis <= 0L) return@LaunchedEffect
        while (true) {
            delay(autoScrollIntervalMillis)
            // A drag in progress owns the pager; skip this tick instead of fighting it.
            if (!state.isScrollInProgress) {
                state.animateScrollToPage((state.currentPage + 1) % pageCount)
            }
        }
    }
}

/**
 * The ColorOS 17 page indicator of the banner, the port of `COUIPageIndicatorKit`
 * (`com.coui.appcompat.banner`, Clock APK).
 *
 * It is deliberately **not** a [PageIndicator] wrapper. The two share their geometry — both read
 * `coui_page_indicator_dot_size` (6dp) and `coui_page_indicator_dot_spacing` (3dp), so this port
 * reuses [PageIndicatorDefaults.DotSize] and [PageIndicatorDefaults.DotSpacing] and keeps the same
 * 12dp centre-to-centre pitch — but they differ in what they can express:
 * - `COUIPageIndicatorKit` draws one dot per page, `layoutWidth = dotStepDistance * count`, with no
 *   cap, while [PageIndicator] shows a sliding window of at most
 *   [PageIndicatorDefaults.MaxVisibleDots] (6) dots and shrinks the two dots at the window edges;
 * - the active dot here is a *trace pill*, a round rect drawn over the dot row in the trace colour
 *   whose two edges are interpolated separately, so it can span two dots while it moves. [PageIndicator]
 *   has no trace and only recolours the active dot;
 * - the dots take a click listener in the native (`dotIsClickable` defaults to true), which
 *   [PageIndicator] does not have.
 *
 * Geometry is `D(position)`: with the dot at `dotSpacing + position * dotStepDistance` from the
 * leading edge, `finalRight = dotSpacing + dotSize + position * dotStepDistance` and
 * `finalLeft = finalRight - dotSize`; in an RTL layout direction `finalRight = layoutWidth -
 * (dotSpacing + position * dotStepDistance)`. The view is `layoutWidth` x `dotSize` (`onMeasure`), and
 * the dots and the trace share `dotCornerRadius = dotSize / 2`, so both are circles / a capsule.
 *
 * The trace animation is a port of `COUIPageIndicatorKit.c(ValueAnimator)`, the update listener of the
 * 240ms `PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f)` animator: the two edges travel from where they
 * were to `finalLeft` / `finalRight` over [BannerDefaults.TraceAnimationDurationMillis]. The native
 * never actually runs that animator — `B()` is the only caller of `traceAnimator.start()`, it is
 * reached only from the handler's `MSG_START_TRACE_ANIMATION` (17), and nothing in the class ever
 * sends that message — so the shipped component snaps the pill into place through `A(position)`. The
 * port animates it, which is what the dead code was written to do.
 *
 * Not ported:
 * - `dotIsStrokeStyle` and `coui_page_indicator_dot_stroke`: the style attribute defaults to false,
 *   and no style in the APK turns it on;
 * - the sticky bezier paths and the `needSettlePositionTemp` / `tranceCutTailRight` branches of the
 *   update listener, which no assignment in the class ever reaches;
 * - the accessibility description built from `coui_page_indicator_description`, whose string the
 *   report does not list.
 *
 * @param count The number of pages. `count <= 0` draws nothing.
 * @param currentIndex The index of the active page. Clamped into `0 until count`.
 * @param modifier The modifier to be applied to the indicator.
 * @param colors The dot and trace colours.
 */
@Composable
fun BannerIndicator(
    count: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier,
    colors: BannerIndicatorColors = BannerDefaults.bannerIndicatorColors(),
) {
    if (count <= 0) return
    val index = currentIndex.coerceIn(0, count - 1)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val dotSize = PageIndicatorDefaults.DotSize
    val dotSpacing = PageIndicatorDefaults.DotSpacing
    val dotStepDistance = dotSize + dotSpacing * 2f
    val layoutWidth = dotStepDistance * count
    val density = LocalDensity.current

    val targetRight = if (isRtl) {
        layoutWidth - (dotSpacing + dotStepDistance * index)
    } else {
        dotSpacing + dotSize + dotStepDistance * index
    }
    val targetLeft = targetRight - dotSize
    val traceEase = BannerDefaults.TraceAnimationEasing
    val traceLeft by animateFloatAsState(
        targetValue = with(density) { targetLeft.toPx() },
        animationSpec = tween(BannerDefaults.TraceAnimationDurationMillis, easing = traceEase),
        label = "bannerTraceLeft",
    )
    val traceRight by animateFloatAsState(
        targetValue = with(density) { targetRight.toPx() },
        animationSpec = tween(BannerDefaults.TraceAnimationDurationMillis, easing = traceEase),
        label = "bannerTraceRight",
    )

    Canvas(modifier = modifier.size(width = layoutWidth, height = dotSize)) {
        val dotPx = dotSize.toPx()
        val spacingPx = dotSpacing.toPx()
        val stepPx = dotStepDistance.toPx()
        val centreY = size.height / 2f
        repeat(count) { page ->
            val left = if (isRtl) {
                size.width - spacingPx - page * stepPx - dotPx
            } else {
                spacingPx + page * stepPx
            }
            drawCircle(
                color = colors.dotColor,
                radius = dotPx / 2f,
                center = Offset(left + dotPx / 2f, centreY),
            )
        }
        drawRoundRect(
            color = colors.traceColor,
            topLeft = Offset(traceLeft, 0f),
            size = Size((traceRight - traceLeft).coerceAtLeast(0f), dotPx),
            cornerRadius = CornerRadius(dotPx / 2f),
        )
    }
}

/**
 * The colours of a [BannerIndicator] — the `dotColor` and `traceDotColor` attributes of
 * `Widget.COUI.COUIPageIndicator`.
 *
 * @param dotColor The inactive dots. The layout sets `dotColor` to
 *   `@color/coui_page_indicator_dot_color_in_dark_bg`, the style to `?attr/couiColorHintNeutral`.
 * @param traceColor The trace pill over the active dot. The layout sets `traceDotColor` to
 *   `@color/coui_color_label_primary_dark`, the style to `@color/coui_color_label_primary`.
 */
@Immutable
data class BannerIndicatorColors(
    val dotColor: Color,
    val traceColor: Color,
)

/** Contains the default values used by [Banner] and [BannerIndicator]. */
object BannerDefaults {

    /**
     * The horizontal inset of the card row. `COUIBannerRecyclerView.c`'s
     * `setPadding(w3.a.a(24), 0, w3.a.a(24), 0)`, 24dp.
     */
    val ContentPadding = 24.dp

    /**
     * The gap between two cards. `COUIBannerRecyclerView.c` adds a horizontal `DividerItemDecoration`
     * whose `coui_item_decoration_8dp` drawable is an 8dp x 1dp transparent shape, 8dp.
     */
    val ItemSpacing = 8.dp

    /**
     * The space under the page indicator. The layout's
     * `android:layout_marginBottom="10.000000dp"` on `COUIPageIndicatorKit`, 10dp.
     */
    val IndicatorMarginBottom = 10.dp

    /**
     * The multiplier `COUIBannerRecyclerView.c` applies to the platform scroll friction in
     * `setHorizontalFlingFriction(ViewConfiguration.getScrollFriction() * 2.5f)`.
     *
     * Exposed for completeness: [HorizontalPager] exposes no fling-friction knob, so the port cannot
     * apply it.
     */
    val FlingFrictionScale = 2.5f

    /**
     * The duration of the trace pill's travel. `COUIPageIndicatorKit`'s
     * `traceAnimator.setDuration(240L)`, 240ms. The class also carries an unused
     * `DURATION_TRACE_ANIMATION` constant of 300.
     */
    val TraceAnimationDurationMillis = 240

    /**
     * The curve of the trace pill's travel, `COUIPageIndicatorKit`'s
     * `PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f)`.
     */
    val TraceAnimationEasing: Easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

    /**
     * The inactive dot colour the banner layout asks for,
     * `@color/coui_page_indicator_dot_color_in_dark_bg`, `#4dffffff`. The `_in_dark_bg` family has no
     * night qualifier, so this is the same in both themes.
     */
    val IndicatorDotColorOnDarkBackground = Color(0x4DFFFFFF)

    /**
     * The trace colour the banner layout asks for, `@color/coui_color_label_primary_dark`,
     * `#e6ffffff`. `coui_color_label_primary_dark` has no night qualifier either.
     */
    val IndicatorTraceColorOnDarkBackground = Color(0xE6FFFFFF)

    /**
     * The indicator colours of the banner layout, which paints its dots for a photo background:
     * [IndicatorDotColorOnDarkBackground] and [IndicatorTraceColorOnDarkBackground].
     *
     * The layout sets them explicitly, overriding `Widget.COUI.COUIPageIndicator`'s
     * `?attr/couiColorHintNeutral` / `@color/coui_color_label_primary`. Reach for [indicatorColors] to
     * get that themed pair back.
     */
    @Composable
    fun bannerIndicatorColors(): BannerIndicatorColors = remember {
        BannerIndicatorColors(
            dotColor = IndicatorDotColorOnDarkBackground,
            traceColor = IndicatorTraceColorOnDarkBackground,
        )
    }

    /**
     * The indicator colours of `Widget.COUI.COUIPageIndicator`, the style [BannerIndicator] draws by
     * default when it sits on an ordinary surface rather than over a photo.
     *
     * @param dotColor The inactive dots. The style resolves `dotColor` from `?attr/couiColorHintNeutral`,
     *   the theme's [COUITheme.colorScheme.disabledOnSurface].
     * @param traceColor The trace pill. The style resolves `traceDotColor` from
     *   `@color/coui_color_label_primary`, the theme's [COUITheme.colorScheme.onSurface].
     */
    @Composable
    fun indicatorColors(
        dotColor: Color = COUITheme.colorScheme.disabledOnSurface,
        traceColor: Color = COUITheme.colorScheme.onSurface,
    ): BannerIndicatorColors = remember(dotColor, traceColor) {
        BannerIndicatorColors(dotColor = dotColor, traceColor = traceColor)
    }
}
