// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")
@file:OptIn(ExperimentalCOUIApi::class)

package component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.HintAnimationLayout
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.InputField
import io.github.suqi8.coui.kmp.basic.SearchBarDefaults
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.basic.TopTips
import io.github.suqi8.coui.kmp.basic.TopTipsBackgroundStyle
import io.github.suqi8.coui.kmp.basic.TopTipsDefaults
import io.github.suqi8.coui.kmp.basic.TopTipsStyle
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Search
import io.github.suqi8.coui.kmp.theme.COUITheme

/** Renders the "HintAnimationLayout" gallery section content (a card wrapping the hint animation demo). */
@Composable
fun HintAnimationLayoutDemo() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        HintAnimationDemo()
    }
}

/**
 * Renders the "TopTips" gallery section content: one tips bar assembled from the options.
 *
 * The AI payment style adds the ignore button next to the action. With "Custom content" the message
 * body is the caller's composable, while the icon, title, trailing buttons and the row layout stay
 * the built-in ones, and the `text` argument is left empty.
 */
@Composable
fun TopTipsDemo() {
    val styleIndex = remember { mutableIntStateOf(0) }
    val backgroundIndex = remember { mutableIntStateOf(TopTipsBackgroundStyle.Gray.ordinal) }
    val showTitle = remember { mutableStateOf(false) }
    val showIcon = remember { mutableStateOf(true) }
    val showAction = remember { mutableStateOf(false) }
    val showClose = remember { mutableStateOf(true) }
    val longText = remember { mutableStateOf(false) }
    val customContent = remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(true) }
    val style = TopTipsStyle.entries[styleIndex.intValue]
    val aiPayment = style == TopTipsStyle.AiPayment

    DemoOptions(
        DemoChoice("Style", TopTipsStyle.entries.map { it.name }, styleIndex),
        DemoChoice("Background", TopTipsBackgroundStyle.entries.map { it.name }, backgroundIndex),
        DemoToggle("Show title", showTitle),
        DemoToggle("Show icon", showIcon),
        DemoToggle("Action button", showAction),
        DemoToggle("Close button", showClose),
        DemoToggle("Long text", longText),
        DemoToggle("Custom content", customContent),
    )
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val backgroundStyle = TopTipsBackgroundStyle.entries[backgroundIndex.intValue]
            val hide = { visible = false }
            TopTips(
                text = when {
                    customContent.value -> ""
                    longText.value -> str("This longer message shows how the action label drops to its own row when the text would collide with it")
                    else -> str("New security update available")
                },
                visible = visible,
                title = if (showTitle.value) str("Tips title") else null,
                startIcon = if (showIcon.value) {
                    {
                        Icon(
                            imageVector = COUIIcons.Basic.Search,
                            contentDescription = null,
                            // The titled and AI payment layouts use the 36dp icon slot.
                            modifier = Modifier.size(if (showTitle.value || aiPayment) 36.dp else 24.dp),
                        )
                    }
                } else {
                    null
                },
                actionText = if (showAction.value || aiPayment) str("Continue") else null,
                onAction = if (showAction.value || aiPayment) hide else null,
                ignoreText = if (aiPayment) str("Not now") else null,
                onIgnore = if (aiPayment) hide else null,
                onClose = if (showClose.value) hide else null,
                style = style,
                backgroundStyle = backgroundStyle,
                colors = TopTipsDefaults.topTipsColors(backgroundStyle),
                content = if (customContent.value) {
                    { TopTipsCustomBody() }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
            )
            if (!visible) {
                TextButton(
                    text = str("Show again"),
                    onClick = { visible = true },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun HintAnimationDemo() {
    var query by remember { mutableStateOf("") }
    var lastHint by remember { mutableStateOf("") }
    val hints = listOf(
        str("Search settings"),
        str("Search messages"),
        str("Search apps and files"),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HintAnimationLayout(
            hints = hints,
            query = query,
            // `COUIHintAnimationLayout.setTextSize((int) editText.getTextSize())`: the hint copies
            // the search field's own text size, which is the input field's 15sp here.
            textSize = SearchBarDefaults.InputFieldFontSize,
            hintStartPadding = SearchBarDefaults.LeadingIconStartPadding +
                24.dp +
                SearchBarDefaults.LeadingIconEndPadding,
            textColor = COUITheme.colorScheme.onSurfaceVariantSummary,
            onHintAnimationChange = { index, hint -> lastHint = "$index: $hint" },
            modifier = Modifier.fillMaxWidth(),
        ) {
            InputField(
                query = query,
                onQueryChange = { query = it },
                onSearch = {},
                expanded = false,
                onExpandedChange = {},
                label = "",
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            text = str("query: \"{}\"", query),
            style = COUITheme.textStyles.footnote1,
        )
        Text(
            text = str("last hint: {}", lastHint),
            style = COUITheme.textStyles.footnote1,
        )
    }
}

@Composable
private fun TopTipsCustomBody() {
    val highlight = str("The message body is a custom slot.")
    val rest = str("Keep it short.")
    val accent = COUITheme.colorScheme.primary
    // One paragraph, so the plain sentence wraps after the highlighted one instead of being
    // squeezed into the width the first text leaves over.
    Text(
        text = remember(highlight, rest, accent) {
            buildAnnotatedString {
                withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Medium)) { append(highlight) }
                append(' ')
                append(rest)
            }
        },
        style = COUITheme.textStyles.footnote1,
    )
}
