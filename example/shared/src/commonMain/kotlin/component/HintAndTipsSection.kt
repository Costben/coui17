// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.HintAnimationLayout
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.InputField
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.basic.TopTips
import io.github.suqi8.coui.kmp.basic.TopTipsBackgroundStyle
import io.github.suqi8.coui.kmp.basic.TopTipsStyle
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.basic.Search
import io.github.suqi8.coui.kmp.theme.COUITheme

fun LazyListScope.hintAndTipsSection() {
    item(key = "hintAnimationLayout") {
        SmallTitle(text = "HintAnimationLayout")
        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            HintAnimationDemo()
        }
    }
    item(key = "topTipsStyles") {
        SmallTitle(text = "TopTips Styles")
        Card(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            TopTipsAiPaymentDemo()
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            TopTipsBackgroundDemo()
        }
    }
}

@Composable
private fun HintAnimationDemo() {
    var query by remember { mutableStateOf("") }
    var lastHint by remember { mutableStateOf("") }
    val hints = remember {
        listOf(
            "Search settings",
            "Search messages",
            "Search apps and files",
        )
    }

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
            text = "query: \"$query\"",
            style = COUITheme.textStyles.footnote1,
        )
        Text(
            text = "last hint: $lastHint",
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
            text = "Review the payment details before you confirm this transaction",
            title = "AI payment",
            visible = visible,
            startIcon = {
                Icon(
                    imageVector = COUIIcons.Basic.Search,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                )
            },
            style = TopTipsStyle.AiPayment,
            ignoreText = "Not now",
            onIgnore = {
                lastAction = "ignore"
                visible = false
            },
            actionText = "Continue",
            onAction = {
                lastAction = "action"
                visible = false
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (!visible) {
            TextButton(
                text = "Show again",
                onClick = { visible = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            text = "last action: $lastAction",
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
            text = "White tips fill with couiColorCard",
            title = "White background",
            backgroundStyle = TopTipsBackgroundStyle.White,
            actionText = "Got it",
            onAction = {},
            modifier = Modifier.fillMaxWidth(),
        )
        TopTips(
            text = "Gray tips fill with couiColorContainer4",
            backgroundStyle = TopTipsBackgroundStyle.Gray,
            onClose = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
