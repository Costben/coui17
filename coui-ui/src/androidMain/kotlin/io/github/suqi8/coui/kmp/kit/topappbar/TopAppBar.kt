// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.topappbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import io.github.suqi8.coui.kmp.kit.backdrop.CouiBackdrop
import io.github.suqi8.coui.kmp.kit.backdrop.LocalCouiBackdrop
import io.github.suqi8.coui.kmp.kit.text.CouiTextKind
import io.github.suqi8.coui.kmp.kit.text.rememberCouiTextStyle
import io.github.suqi8.coui.kmp.kit.theme.CouiTheme
import io.github.suqi8.coui.kmp.kit.theme.LocalCouiDarkTheme
import io.github.suqi8.coui.kmp.kit.theme.ProvideCouiContent
import kotlin.math.roundToInt

/** 完整紧凑顶栏；内部处理安全区和槽位布局，点击行为由宿主提供。 */
@Composable
internal fun TopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    dark: Boolean = CouiTheme.isDark,
    colors: CouiTopAppBarColors = CouiTopAppBarDefaults.colors(dark = dark),
    windowInsets: WindowInsets = CouiTopAppBarDefaults.windowInsets,
    backdrop: CouiBackdrop? = LocalCouiBackdrop.current,
    bottomContent: (@Composable () -> Unit)? = null,
    centerTitle: Boolean = false,
    centerTitleMargin: Dp? = null,
    subtitle: (@Composable () -> Unit)? = null,
    titlePadding: Dp? = null,
    navigationIconPadding: Dp? = null,
    actionIconPadding: Dp? = null,
    surfaceEnabled: Boolean = true,
    showDivider: Boolean = false,
    dividerColor: Color = Color.Unspecified,
    dividerAlpha: (() -> Float)? = null,
) {
    val barContent = @Composable {
        CompositionLocalProvider(LocalCouiDarkTheme provides dark) {
            AppBarLayout(
                title = title,
                navigationIcon = navigationIcon,
                actions = actions,
                colors = colors,
                windowInsets = windowInsets,
                expandedHeight = CouiTopAppBarDefaults.height,
                collapsedHeight = CouiTopAppBarDefaults.height,
                state = null,
                bottomContent = bottomContent,
                centerTitle = centerTitle,
                centerTitleMargin = centerTitleMargin,
                subtitle = subtitle,
                titlePadding = titlePadding,
                navigationIconPadding = navigationIconPadding,
                actionIconPadding = actionIconPadding,
                showDivider = showDivider,
                dividerColor = dividerColor,
                dividerAlpha = dividerAlpha,
            )
        }
    }
    if (surfaceEnabled) {
        TopAppBarSurface(
            modifier.fillMaxWidth(),
            backdrop,
            gradientColor = colors.backgroundColor,
            dark = dark,
        ) {
            barContent()
        }
    } else {
        Box(modifier.fillMaxWidth()) {
            barContent()
        }
    }
}

/**
 * 大标题缩为固定小标题；省略 scrollBehavior 时保持展开。
 * 连接挂到正文祖先，正文通过 scrollBehavior.state.contentPadding 补偿逻辑滚动与可见弹簧的差值。
 * 高度不含 windowInsets；expandedHeight 必须大于正数 collapsedHeight。
 * 字号为 Unspecified 时沿用预设：展开跟随 HomeTitle，收起使用固定 dp。
 * 显式字号必须是有限正数 sp；title 槽位须继承组件提供的字号。
 */
@Composable
internal fun LargeTopAppBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: CouiTopAppBarScrollBehavior? = null,
    dark: Boolean = CouiTheme.isDark,
    colors: CouiTopAppBarColors = CouiTopAppBarDefaults.colors(dark = dark),
    windowInsets: WindowInsets = CouiTopAppBarDefaults.windowInsets,
    expandedHeight: Dp = CouiTopAppBarDefaults.expandedHeight,
    collapsedHeight: Dp = CouiTopAppBarDefaults.collapsedHeight,
    backdrop: CouiBackdrop? = LocalCouiBackdrop.current,
    expandedTitleSize: TextUnit = TextUnit.Unspecified,
    collapsedTitleSize: TextUnit = TextUnit.Unspecified,
    bottomContent: (@Composable () -> Unit)? = null,
    subtitle: (@Composable () -> Unit)? = null,
    hideSubtitleOnCollapse: Boolean = true,
    titlePadding: Dp? = null,
    navigationIconPadding: Dp? = null,
    actionIconPadding: Dp? = null,
    surfaceEnabled: Boolean = true,
    showDivider: Boolean = false,
    dividerColor: Color = Color.Unspecified,
    dividerFraction: (() -> Float)? = null,
) {
    require(
        expandedHeight.value.isFinite() && collapsedHeight.value.isFinite() &&
            collapsedHeight > 0.dp && expandedHeight > collapsedHeight,
    ) {
        "展开高度必须大于收起高度，且高度必须是有限正数"
    }
    require(
        expandedTitleSize == TextUnit.Unspecified ||
            (expandedTitleSize.isSp && expandedTitleSize.value.isFinite() && expandedTitleSize.value > 0f),
    ) {
        "展开字号必须为 Unspecified 或有限正数 sp"
    }
    require(
        collapsedTitleSize == TextUnit.Unspecified ||
            (collapsedTitleSize.isSp && collapsedTitleSize.value.isFinite() && collapsedTitleSize.value > 0f),
    ) {
        "收起字号必须为 Unspecified 或有限正数 sp"
    }
    val density = LocalDensity.current
    val state = scrollBehavior?.state
    SideEffect {
        state?.updateRange(density, hasSearchBar = false, expandedHeight = expandedHeight, collapsedHeight = collapsedHeight)
        state?.updateSearch(expanded = false, active = false)
    }
    val barContent = @Composable {
        CompositionLocalProvider(LocalCouiDarkTheme provides dark) {
            AppBarLayout(
                title = title,
                navigationIcon = navigationIcon,
                actions = actions,
                colors = colors,
                windowInsets = windowInsets,
                expandedHeight = expandedHeight,
                collapsedHeight = collapsedHeight,
                state = state,
                expandedTitleSize = expandedTitleSize,
                collapsedTitleSize = collapsedTitleSize,
                bottomContent = bottomContent,
                subtitle = subtitle,
                hideSubtitleOnCollapse = hideSubtitleOnCollapse,
                titlePadding = titlePadding,
                navigationIconPadding = navigationIconPadding,
                actionIconPadding = actionIconPadding,
                showDivider = showDivider,
                dividerColor = dividerColor,
                dividerFraction = dividerFraction,
            )
        }
    }
    if (surfaceEnabled) {
        TopAppBarSurface(
            modifier.fillMaxWidth(),
            backdrop,
            referenceBlurRadius = with(density) { CouiCollapsingAppBarDefaults.referenceBlurRadiusPx.toDp() },
            bottomExtension = CouiCollapsingAppBarDefaults.titleOnlyBlurExtension,
            gradientSpeed = 3f,
            gradientColor = colors.backgroundColor,
            dark = dark,
            topExtension = {
                with(density) {
                    val range = (expandedHeight.roundToPx() - collapsedHeight.roundToPx()).coerceAtLeast(0)
                    (state?.offsetPx ?: 0f).coerceIn(0f, range.toFloat()).toDp()
                }
            },
        ) {
            barContent()
        }
    } else {
        Box(modifier.fillMaxWidth()) {
            barContent()
        }
    }
}

@Composable
internal fun AppBarLayout(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    colors: CouiTopAppBarColors,
    windowInsets: WindowInsets,
    expandedHeight: Dp,
    collapsedHeight: Dp,
    state: CouiCollapsingAppBarState?,
    expandedTitleSize: TextUnit = TextUnit.Unspecified,
    collapsedTitleSize: TextUnit = TextUnit.Unspecified,
    bottomContent: (@Composable () -> Unit)? = null,
    centerTitle: Boolean = false,
    centerTitleMargin: Dp? = null,
    subtitle: (@Composable () -> Unit)? = null,
    hideSubtitleOnCollapse: Boolean = true,
    titlePadding: Dp? = null,
    navigationIconPadding: Dp? = null,
    actionIconPadding: Dp? = null,
    showDivider: Boolean = false,
    dividerColor: Color = Color.Unspecified,
    dividerFraction: (() -> Float)? = null,
    dividerAlpha: (() -> Float)? = null,
    modifier: Modifier = Modifier,
) {
    val large = expandedHeight > collapsedHeight
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val defaultTitleStyle = rememberCouiTextStyle(if (large) CouiTextKind.HomeTitle else CouiTextKind.Toolbar)
    val titleStyle = if (large && expandedTitleSize != TextUnit.Unspecified) {
        // 同步行高与字号的像素比例，保留原字体度量及系统非线性字体缩放。
        val lineHeight = with(density) {
            (
                defaultTitleStyle.lineHeight.toPx() * expandedTitleSize.toPx() /
                    defaultTitleStyle.fontSize.toPx()
                ).toSp()
        }
        defaultTitleStyle.copy(fontSize = expandedTitleSize, lineHeight = lineHeight)
    } else {
        defaultTitleStyle
    }
    val fraction = state?.collapsedFraction ?: 0f
    val progress = if (large) CouiCollapsingAppBarDefaults.titleEasing.transform(fraction) else 0f
    val scale = if (large) {
        val collapsedScale = with(density) {
            val collapsedPixels = if (collapsedTitleSize == TextUnit.Unspecified) {
                CouiCollapsingAppBarDefaults.collapsedTitleSize.toPx()
            } else {
                collapsedTitleSize.toPx()
            }
            collapsedPixels / titleStyle.fontSize.toPx()
        }
        1f + (collapsedScale - 1f) * progress
    } else {
        1f
    }

    val resolvedDividerColor = if (dividerColor != Color.Unspecified) dividerColor else colors.titleColor.copy(alpha = 0.12f)
    val dividerModifier = if (showDivider) {
        Modifier.drawWithContent {
            drawContent()
            if (large) {
                val f = dividerFraction?.invoke() ?: fraction
                if (f > 0f) {
                    val inset = lerp(24.dp.toPx(), 0f, f.coerceIn(0f, 1f))
                    val stroke = 0.33.dp.toPx()
                    val y = size.height - stroke / 2f
                    drawLine(
                        color = resolvedDividerColor,
                        start = Offset(inset, y),
                        end = Offset(size.width - inset, y),
                        strokeWidth = stroke,
                        alpha = f.coerceIn(0f, 1f),
                    )
                }
            } else {
                val a = dividerAlpha?.invoke() ?: 0f
                if (a > 0f) {
                    val baseInset = (titlePadding ?: 16.dp).toPx()
                    val inset = lerp(baseInset, 0f, a.coerceIn(0f, 1f))
                    val stroke = 0.33.dp.toPx()
                    val y = size.height - stroke / 2f
                    drawLine(
                        color = resolvedDividerColor,
                        start = Offset(inset, y),
                        end = Offset(size.width - inset, y),
                        strokeWidth = stroke,
                        alpha = a.coerceIn(0f, 1f),
                    )
                }
            }
        }
    } else {
        Modifier
    }

    Layout(
        modifier = modifier
            .windowInsetsPadding(windowInsets)
            .then(dividerModifier),
        content = {
            Box {
                CompositionLocalProvider(LocalCouiToolbarContentColor provides colors.navigationIconColor) {
                    ProvideCouiContent(color = colors.navigationIconColor, content = navigationIcon)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompositionLocalProvider(LocalCouiToolbarContentColor provides colors.actionIconColor) {
                    ProvideCouiContent(color = colors.actionIconColor) { actions() }
                }
            }
            Box(
                Modifier.semantics { heading() }.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(if (direction == LayoutDirection.Rtl) 1f else 0f, 0f)
                    clip = true
                },
            ) {
                ProvideCouiContent(
                    color = colors.titleColor,
                    textStyle = titleStyle.copy(color = colors.titleColor),
                    mergeTextStyle = false,
                    content = title,
                )
            }
            if (subtitle != null) {
                Box(
                    Modifier.graphicsLayer {
                        if (large && hideSubtitleOnCollapse) {
                            alpha = (1f - progress).coerceIn(0f, 1f)
                        }
                    },
                ) {
                    ProvideCouiContent(
                        color = colors.titleColor.copy(alpha = 0.55f),
                        content = subtitle,
                    )
                }
            }
            if (bottomContent != null) {
                Box(Modifier.fillMaxWidth()) {
                    bottomContent()
                }
            }
        },
    ) { nodes, constraints ->
        require(constraints.hasBoundedWidth) { "顶栏需要有界宽度" }
        val width = constraints.maxWidth
        val expanded = expandedHeight.roundToPx()
        val collapsed = collapsedHeight.roundToPx()
        val range = (expanded - collapsed).coerceAtLeast(0)
        val offset = (state?.offsetPx ?: 0f).coerceIn(0f, range.toFloat())
        val barHeight = constraints.constrainHeight((expanded - offset).roundToInt())
        val toolbarHeight = if (large) minOf(collapsed, barHeight) else barHeight

        val navPaddingPx = navigationIconPadding?.roundToPx()
            ?: CouiTopAppBarDefaults.horizontalPadding.roundToPx().coerceAtMost(width / 2)
        val actPaddingPx = actionIconPadding?.roundToPx()
            ?: CouiTopAppBarDefaults.horizontalPadding.roundToPx().coerceAtMost(width / 2)
        val gap = CouiTopAppBarDefaults.titleGap.roundToPx()

        val navSlotWidth = (width - navPaddingPx - actPaddingPx).coerceAtLeast(0)
        val navigation = nodes[0].measure(Constraints(maxWidth = navSlotWidth, maxHeight = toolbarHeight))
        val action = nodes[1].measure(Constraints(maxWidth = (navSlotWidth - navigation.width).coerceAtLeast(0), maxHeight = toolbarHeight))

        val compactStart = if (large && navigation.width == 0) {
            titlePadding?.toPx() ?: CouiCollapsingAppBarDefaults.titleOnlyStart.toPx()
        } else {
            (navPaddingPx + navigation.width + if (navigation.width > 0) gap else 0).toFloat()
        }
        val compactEnd = actPaddingPx + action.width + if (action.width > 0) gap else 0
        val expandedStart = titlePadding?.toPx() ?: CouiTopAppBarDefaults.expandedTitleStart.toPx()
        val curvedTitle = large && navigation.width > 0
        // 弧线偏移在两端的位置与变化率均为零，保留标题原有的起步和收尾节奏。
        val arcEnvelope = progress * (1f - progress)
        val arcOffset = if (curvedTitle) 0.8f * arcEnvelope * arcEnvelope else 0f
        val horizontalProgress = progress + arcOffset
        val start = if (large) {
            expandedStart + (compactStart - expandedStart) * horizontalProgress
        } else {
            compactStart
        }
        val end = if (large && action.width == 0) {
            titlePadding?.toPx() ?: CouiCollapsingAppBarDefaults.titleOnlyStart.toPx()
        } else {
            compactEnd.toFloat()
        }
        // 用缩放后的可见宽度限制标题，避免收起时覆盖导航或操作按钮。
        val titleWidth = ((width - start - end).coerceAtLeast(0f) / scale).roundToInt()
        val titleNode = nodes[2].measure(Constraints(maxWidth = titleWidth, maxHeight = (barHeight / scale).toInt()))

        var nextNodeIndex = 3
        val subtitleNode = if (subtitle != null) {
            nodes[nextNodeIndex++].measure(Constraints(maxWidth = titleWidth, minHeight = 0))
        } else {
            null
        }
        val bottomNode = if (nodes.size > nextNodeIndex) nodes[nextNodeIndex].measure(constraints.copy(minHeight = 0)) else null
        val bottomHeight = bottomNode?.height ?: 0
        val totalHeight = barHeight + bottomHeight

        layout(width, totalHeight) {
            val navigationY = (toolbarHeight - navigation.height) / 2
            navigation.placeRelative(navPaddingPx, navigationY, zIndex = 1f)
            action.placeRelative(width - actPaddingPx - action.width, (toolbarHeight - action.height) / 2, zIndex = 1f)

            val titleY = if (large) {
                val expandedY = expanded - (
                    CouiTopAppBarDefaults.expandedHeight -
                        CouiCollapsingAppBarDefaults.titleOnlyTop
                    ).toPx()
                val collapsedY = (collapsed - titleNode.height * scale) / 2f
                if (curvedTitle) {
                    val verticalProgress = progress - arcOffset
                    expandedY + (collapsedY - expandedY) * verticalProgress
                } else {
                    expandedY + (range + collapsedY - expandedY) * progress - offset
                }
            } else {
                if (subtitleNode != null) {
                    val gapPx = 2.dp.roundToPx()
                    val totalH = titleNode.height + subtitleNode.height + gapPx
                    (barHeight - totalH) / 2f
                } else {
                    (barHeight - titleNode.height) / 2f
                }
            }

            val titleX = if (centerTitle && !large) {
                val centeredX = (width - titleNode.width) / 2
                val marginPx = centerTitleMargin?.roundToPx()
                if (marginPx != null) {
                    val minX = marginPx
                    val maxX = width - marginPx - titleNode.width
                    centeredX.coerceIn(minX, maxX.coerceAtLeast(minX))
                } else {
                    val minX = start.roundToInt()
                    val maxX = width - end.roundToInt() - titleNode.width
                    centeredX.coerceIn(minX, maxX.coerceAtLeast(minX))
                }
            } else {
                start.roundToInt()
            }

            val placedTitleY = (
                if (curvedTitle) {
                    titleY.coerceAtLeast(0f)
                } else {
                    titleY.coerceIn(0f, (barHeight - titleNode.height * scale).coerceAtLeast(0f))
                }
                ).roundToInt()

            titleNode.placeRelative(titleX, placedTitleY)

            if (subtitleNode != null) {
                val subtitleX = if (centerTitle && !large) {
                    val centeredSubX = (width - subtitleNode.width) / 2
                    val marginPx = centerTitleMargin?.roundToPx()
                    if (marginPx != null) {
                        val minX = marginPx
                        val maxX = width - marginPx - subtitleNode.width
                        centeredSubX.coerceIn(minX, maxX.coerceAtLeast(minX))
                    } else {
                        val minX = start.roundToInt()
                        val maxX = width - end.roundToInt() - subtitleNode.width
                        centeredSubX.coerceIn(minX, maxX.coerceAtLeast(minX))
                    }
                } else {
                    titleX
                }
                val subtitleY = if (large) {
                    val subtitleMarginTop = 4.dp.roundToPx()
                    (placedTitleY + titleNode.height * scale + subtitleMarginTop).roundToInt()
                } else {
                    placedTitleY + titleNode.height + 2.dp.roundToPx()
                }
                subtitleNode.placeRelative(subtitleX, subtitleY)
            }

            bottomNode?.placeRelative(0, barHeight)
        }
    }
}
