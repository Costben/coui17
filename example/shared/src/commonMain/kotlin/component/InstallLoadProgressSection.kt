// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.InstallLoadProgress
import io.github.suqi8.coui.kmp.basic.InstallLoadStyle
import io.github.suqi8.coui.kmp.basic.LoadProgressState
import io.github.suqi8.coui.kmp.basic.Slider
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.delay

fun LazyListScope.installLoadProgressSection() {
    item(key = "installLoadProgress") {
        SmallTitle(text = "InstallLoadProgress")
        InstallLoadProgressDemo()
    }
}

/** Renders the "InstallLoadProgress" gallery section content (a card listing every load style plus the special states). */
@Composable
fun InstallLoadProgressDemo() {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        InstallLoadStyleDemo()
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        InstallLoadSpecialStatesDemo()
    }
}

@Composable
private fun InstallLoadStyleDemo() {
    var state by remember { mutableStateOf(LoadProgressState.Default) }
    var progress by remember { mutableIntStateOf(0) }

    LaunchedEffect(state) {
        if (state == LoadProgressState.Loading) {
            progress = 0
            while (progress < 100) {
                delay(120)
                progress += 5
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // The same state across the three couiStyle tiers: only bigRoundButton is accent filled at
        // rest, and only the two pill tiers paint a fill while a transfer is running.
        InstallLoadStyle.entries.forEach { style ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InstallLoadProgress(
                    onClick = { state = state.toggled() },
                    state = state,
                    progress = progress,
                    text = if (state == LoadProgressState.Loading) "$progress%" else str("Download"),
                    style = style,
                )
                Text(
                    text = "${style.name} · couiStyle=${style.value}",
                    color = COUITheme.colorScheme.onBackgroundVariant,
                    style = COUITheme.textStyles.footnote1,
                )
            }
        }
        Text(
            text = str("state: {} ({}) · progress: {} / 100", state.name, state.value, progress),
            color = COUITheme.colorScheme.onBackgroundVariant,
            style = COUITheme.textStyles.footnote1,
        )
    }
}

@Composable
private fun InstallLoadSpecialStatesDemo() {
    var progress by remember { mutableIntStateOf(60) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Press and hold: mPressedAnimator insets the path by 5% of the measured size, narrows the
        // label to 0.92 and scales the brightness to 0.8 in HSL.
        InstallLoadProgress(
            onClick = { },
            state = LoadProgressState.Loading,
            progress = progress,
            max = 100,
            text = "$progress%",
            style = InstallLoadStyle.BigRound,
            hapticFeedback = true,
        )
        Slider(
            value = progress.toFloat(),
            onValueChange = { progress = it.toInt() },
            valueRange = 0f..100f,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // mState == 3 paints the whole path with the accent colour, whatever mProgress says.
            InstallLoadProgress(
                onClick = { },
                state = LoadProgressState.Fail,
                progress = progress,
                text = str("Retry"),
                style = InstallLoadStyle.Default,
            )
            // mState == 4 matches no onDraw branch: the shape is never painted, only the label.
            InstallLoadProgress(
                onClick = { },
                state = LoadProgressState.InstallHaveGift,
                progress = progress,
                text = str("Gift"),
                style = InstallLoadStyle.Default,
            )
        }
        Text(
            text = str("Fail fills the whole shape; InstallHaveGift paints none"),
            color = COUITheme.colorScheme.onBackgroundVariant,
            style = COUITheme.textStyles.footnote1,
        )
    }
}
