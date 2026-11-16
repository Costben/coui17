// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.text

// Ported from COUI-Kit (ItosEO/COUI-Kit@8a09da4) coui/src/main/java/com/itos/coui/text/Text.kt.

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import io.github.suqi8.coui.kmp.kit.theme.CouiTheme
import kotlin.math.roundToInt

/** COUI semantic typography presets; hosts can pass [rememberCouiTextStyle] to Compose text and editors. */
internal enum class CouiTextKind(val size: Float, val family: String, val weight: Int) {
    PreferenceTitle(16f, "sans-serif-medium", 500),
    PreferenceSummary(13f, "sans-serif", 400),
    Category(12f, "sans-serif", 400),
    Toolbar(18f, "sans-serif-medium", 550),
    PageTitle(24f, "sans-serif-semibold", 600),
    SectionTitle(18f, "sans-serif-medium", 600),
    Body(15f, "sans-serif", 400),
    HomeTitle(40f, "sans-serif-semibold", 700),
    Search(15f, "sans-serif", 400),
    SearchCancel(14f, "sans-serif-medium", 500),
    PreferenceValue(14f, "sans-serif", 400),
    GroupTitle(12f, "sans-serif-medium", 500),
    Stepper(18f, "sans-serif-medium", 500),
    Progress(14f, "sans-serif-medium", 500),
    Description(12f, "sans-serif", 400),
}

/** System resources drive typography; body and description default to start alignment while line count and overflow are handled by [BasicText]. */
@Composable
internal fun Text(
    modifier: Modifier = Modifier,
    text: String,
    kind: CouiTextKind,
    color: Color = Color.Unspecified,
    onLineCount: (Int) -> Unit = {},
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val style = rememberCouiTextStyle(kind)
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(
            color = color.takeOrElse { CouiTheme.contentColor },
            textAlign = textAlign ?: when (kind) {
                CouiTextKind.PreferenceTitle,
                CouiTextKind.PreferenceSummary,
                CouiTextKind.Body,
                CouiTextKind.Description,
                -> TextAlign.Start

                else -> TextAlign.Center
            },
        ),
        onTextLayout = { onLineCount(it.lineCount) },
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
internal fun rememberCouiTextStyle(kind: CouiTextKind): TextStyle {
    val density = LocalDensity.current
    val fontScale = rememberCouiTextFontScale()
    return remember(kind, density, fontScale) {
        val fixedDp = kind == CouiTextKind.Toolbar || kind == CouiTextKind.PageTitle || kind == CouiTextKind.PreferenceValue
        val pixels = if (fixedDp) {
            kind.size * density.density
        } else {
            couiTextSize(kind, fontScale) {
                with(density) { it.sp.toPx() }
            }
        }
        couiTextStyle(
            familyName = kind.family,
            weight = kind.weight,
            pixels = pixels,
            lineSpacing = couiLineSpacing(kind),
            includeFontPadding = kind != CouiTextKind.Toolbar && kind != CouiTextKind.PageTitle && kind != CouiTextKind.HomeTitle,
            density = density,
        )
    }
}

private fun couiTextSize(kind: CouiTextKind, fontScale: Float, spToPx: (Float) -> Float): Float {
    val pixels = spToPx(kind.size).roundToInt().toFloat()
    if (kind == CouiTextKind.SearchCancel) {
        val safeScale = if (fontScale > 0f) fontScale else 1f
        val base = (pixels / safeScale).roundToInt()
        return base * if (safeScale < 1.15f) 1f else 1.15f
    }
    return pixels
}

@Composable
internal expect fun rememberCouiTextFontScale(): Float

internal expect fun couiLineSpacing(kind: CouiTextKind): Float
