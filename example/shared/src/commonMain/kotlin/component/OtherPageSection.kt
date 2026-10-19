// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import appstate.LocalNavigator
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.preference.ArrowPreference
import navigation3.Route
import kotlin.random.Random

/** Renders the "Other" gallery section content: arrows that navigate to the test pages. */
@Composable
fun OtherDemo() {
    val navigator = LocalNavigator.current
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp),
    ) {
        ArrowPreference(
            title = str("PullToRefresh Test"),
            summary = str("Navigate to a PullToRefresh Page"),
            onClick = {
                navigator.push(Route.PullToRefresh)
            },
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        ArrowPreference(
            title = str("Navigation test"),
            summary = str("Navigate to a Navigation Page"),
            onClick = { navigator.push(Route.Navigation(Random.nextLong().toString())) },
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        ArrowPreference(
            title = str("MultiScaffold Test"),
            summary = str("Navigate to a MultiScaffold Page"),
            onClick = { navigator.push(Route.MultiScaffold) },
        )
    }
}
