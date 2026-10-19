// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.FullPageStatement
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme

/** Renders the "FullPageStatement" gallery section content (the statement page hosted in a card). */
@Composable
fun FullPageStatementDemoContent() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        FullPageStatementDemo()
    }
}

@Composable
private fun FullPageStatementDemo() {
    val waiting = str("Waiting for action…")
    val intro = str("Welcome! Before you continue, please read the user agreement and privacy policy carefully. ")
    val collect = str("We collect only the data required to provide core features, and we never share your personal information with third parties without your consent. ")
    val withdraw = str("You can withdraw your consent at any time in Settings. ")
    val continuing = str("Continuing means you have read and agreed to the full statement. ")
    val agreed = str("Agreed")
    val exited = str("Exited")
    var result by remember { mutableStateOf(waiting) }

    Column {
        // The statement page is designed to fill a whole window; the demo hosts it in a
        // fixed-height box so it can live inside the component list.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp),
        ) {
            FullPageStatement(
                title = str("User Agreement & Privacy Policy"),
                content = buildString {
                    append(intro)
                    append(collect)
                    repeat(6) {
                        append(withdraw)
                        append(continuing)
                    }
                },
                primaryButtonText = str("Agree"),
                onPrimaryButtonClick = { result = agreed },
                secondaryButtonText = str("Disagree and exit"),
                onSecondaryButtonClick = { result = exited },
            )
        }
        Text(
            text = result,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
            fontSize = COUITheme.textStyles.body2.fontSize,
            color = COUITheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}
