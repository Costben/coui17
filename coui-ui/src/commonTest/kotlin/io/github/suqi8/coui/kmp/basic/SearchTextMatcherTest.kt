// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchTextMatcherTest {

    @Test
    fun scoresFullPinyinInitialsHybridSubsequenceAndTypo() {
        assertTrue(SearchTextMatcher.scoreItem("深色模式", listOf("shensemoshi")) > 0)
        assertTrue(SearchTextMatcher.scoreItem("深色模式", listOf("ssms")) > 0)
        assertTrue(SearchTextMatcher.scoreItem("深色模式", listOf("shensm")) > 0)
        assertTrue(SearchTextMatcher.scoreItem("深色模式", listOf("深模")) > 0)
        assertTrue(SearchTextMatcher.scoreItem("滑动自动隐藏浮动栏", listOf("ycfl")) > 0)
        assertTrue(SearchTextMatcher.scoreItem("清理缓存", listOf("快取")) > 0)
        assertTrue(SearchTextMatcher.scoreItem("实况图自动播放", listOf("shikuagn")) > 0)
        assertEquals(0, SearchTextMatcher.scoreItem("深色模式", listOf("zzzznotfound")))
    }

    @Test
    fun avoidsCrossKeywordFalsePositives() {
        assertEquals(
            0,
            SearchTextMatcher.scoreItem(
                title = "强调色",
                summary = "默认",
                keywords = "主题色 配色 颜色 色彩 accent color",
                terms = listOf("sp"),
            ),
        )
        assertEquals(
            0,
            SearchTextMatcher.scoreItem(
                title = "预测性返回手势设置",
                summary = "已开启",
                keywords = "侧滑 返回 手势 轨迹 模糊 跟手 predictive back gesture",
                terms = listOf("ssms"),
            ),
        )
    }

    @Test
    fun resolvesHighlightRangesAcrossModes() {
        assertEquals(listOf(0..3), SearchTextMatcher.findHighlightRanges("深色模式", listOf("ssms")))
        assertEquals(listOf(0..1), SearchTextMatcher.findHighlightRanges("深色模式", listOf("shense")))
        assertEquals(listOf(0..2), SearchTextMatcher.findHighlightRanges("深色模式", listOf("shensm")))
        assertEquals(listOf(0..0, 2..2), SearchTextMatcher.findHighlightRanges("深色模式", listOf("深模")))
        assertEquals(listOf(0..1, 3..5), SearchTextMatcher.findHighlightRanges("显示 VIP 等级", listOf("xsvip")))
        assertEquals(listOf(4..6, 8..8), SearchTextMatcher.findHighlightRanges("滑动自动隐藏浮动栏", listOf("ycfl")))
        assertEquals(listOf(2..3), SearchTextMatcher.findHighlightRanges("清理缓存", listOf("快取")))
        assertEquals(listOf(0..1, 5..6), SearchTextMatcher.findHighlightRanges("视频播放时返回自动悬浮", listOf("sp", "fh")))
        assertEquals(listOf(0..1), SearchTextMatcher.findHighlightRanges("实况图自动播放", listOf("shikuagn")))
        assertEquals(listOf(1..1, 3..3), SearchTextMatcher.findHighlightRanges("「我」页图标样式", listOf("我页")))
    }

    @Test
    fun buildsHighlightedAnnotatedStringWithSpanColor() {
        val accent = Color(0xFF0080FF)
        val styled = SearchTextMatcher.highlightText("深色模式", listOf("shense"), accent)
        assertEquals("深色模式", styled.text)
        assertEquals(1, styled.spanStyles.size)
        assertEquals(0, styled.spanStyles.first().start)
        assertEquals(2, styled.spanStyles.first().end)
        assertEquals(accent, styled.spanStyles.first().item.color)
    }

    @Test
    fun computesGroupCardPositionsBelowEmbeddedHeader() {
        assertEquals(CardListPosition.Tail, SearchResultGroupCardDefaults.itemPositionFor(0, 1))
        assertEquals(CardListPosition.Middle, SearchResultGroupCardDefaults.itemPositionFor(0, 3))
        assertEquals(CardListPosition.Middle, SearchResultGroupCardDefaults.itemPositionFor(1, 3))
        assertEquals(CardListPosition.Tail, SearchResultGroupCardDefaults.itemPositionFor(2, 3))
    }
}
