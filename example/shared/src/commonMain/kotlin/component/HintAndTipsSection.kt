// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

package component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.basic.TopTips
import io.github.suqi8.coui.kmp.basic.TopTipsBackgroundStyle
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

/** Renders the "TopTips Styles" gallery section content (a card with the AI payment and background tips). */
@Composable
fun TopTipsStylesDemo() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        TopTipsAiPaymentDemo()
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        TopTipsBackgroundDemo()
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        TopTipsCustomContentDemo()
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
            onHintAnimationChange = { index, hint -> lastHint = "$index: $hint" },
            modifier = Modifier.fillMaxWidth(),
        ) {
            InputField(
                query = query,
                onQueryChange = { query = it },
                onSearch = {},
                expanded = false,
                onExpandedChange = {},
                leadingIcon = {},
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
private fun TopTipsAiPaymentDemo() {
    var visible by remember { mutableStateOf(true) }
    var lastAction by remember { mutableStateOf("none") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TopTips(
            text = str("Review the payment details before you confirm this transaction"),
            title = str("AI payment"),
            visible = visible,
            startIcon = {
                Icon(
                    imageVector = COUIIcons.Basic.Search,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                )
            },
            style = TopTipsStyle.AiPayment,
            ignoreText = str("Not now"),
            onIgnore = {
                lastAction = "ignore"
                visible = false
            },
            actionText = str("Continue"),
            onAction = {
                lastAction = "action"
                visible = false
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
        Text(
            text = str("last action: {}", lastAction),
            style = COUITheme.textStyles.footnote1,
        )
    }
}

@Composable
private fun TopTipsBackgroundDemo() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TopTips(
            text = str("White tips fill with couiColorCard"),
            title = str("White background"),
            backgroundStyle = TopTipsBackgroundStyle.White,
            actionText = str("Got it"),
            onAction = {},
            modifier = Modifier.fillMaxWidth(),
        )
        TopTips(
            text = str("Gray tips fill with couiColorContainer4"),
            backgroundStyle = TopTipsBackgroundStyle.Gray,
            onClose = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Renders the custom-content slot of [TopTips]: the message body is the caller's composable, while
 * the icon, title, trailing buttons and the row layout stay the built-in ones. The `text` argument
 * is omitted entirely, which the default `""` now allows.
 */
@Composable
private fun TopTipsCustomContentDemo() {
    var visible by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TopTips(
            text = "",
            visible = visible,
            title = str("Custom content"),
            startIcon = {
                Icon(
                    imageVector = COUIIcons.Basic.Search,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                )
            },
            actionText = str("Got it"),
            onAction = { visible = false },
            onClose = { visible = false },
            modifier = Modifier.fillMaxWidth(),
        ) {
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
        if (!visible) {
            TextButton(
                text = str("Show again"),
                onClick = { visible = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
