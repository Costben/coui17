// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.delay

/** Renders the "InstallLoadProgress" gallery section content: the scripted system-update flow and one interactive button whose style and state come from the options. */
@Composable
fun InstallLoadProgressDemo() {
    val styleIndex = remember { mutableIntStateOf(0) }
    val stateIndex = remember { mutableIntStateOf(0) }
    val style = InstallLoadStyle.entries[styleIndex.intValue]
    val state = LoadProgressState.entries[stateIndex.intValue]

    DemoOptions(
        DemoChoice("Style", InstallLoadStyle.entries.map { it.name }, styleIndex),
        DemoChoice("State", LoadProgressState.entries.map { it.name }, stateIndex),
    )
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        SystemUpdateDemo()
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        InstallLoadInteractiveDemo(style = style, state = state)
    }
}

/** The steps of the system update screen, in the order `FragmentBusy` reports them. */
private enum class SystemUpdateStep { Ready, Downloading, Paused, Installing, Finished }

private const val SYSTEM_UPDATE_MAX = 1000
private const val SYSTEM_UPDATE_STEP = 25
private const val SYSTEM_UPDATE_TICK_MILLIS = 100L

/**
 * The system update flow: "Download now" fills the pill while the transfer runs, a tap pauses it,
 * and the install that follows refills the pill before "Reboot now". `max` carries the one decimal
 * the system prints in the label.
 */
@Composable
private fun SystemUpdateDemo() {
    var step by remember { mutableStateOf(SystemUpdateStep.Ready) }
    var progress by remember { mutableIntStateOf(0) }

    LaunchedEffect(step) {
        if (step == SystemUpdateStep.Downloading || step == SystemUpdateStep.Installing) {
            while (progress < SYSTEM_UPDATE_MAX) {
                delay(SYSTEM_UPDATE_TICK_MILLIS)
                progress = (progress + SYSTEM_UPDATE_STEP).coerceAtMost(SYSTEM_UPDATE_MAX)
            }
            progress = 0
            step = if (step == SystemUpdateStep.Downloading) SystemUpdateStep.Installing else SystemUpdateStep.Finished
        }
    }

    val percent = "${progress / 10}.${progress % 10}%"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        InstallLoadProgress(
            onClick = {
                when (step) {
                    SystemUpdateStep.Ready, SystemUpdateStep.Paused -> step = SystemUpdateStep.Downloading

                    SystemUpdateStep.Downloading -> step = SystemUpdateStep.Paused

                    // The system disables the button while it installs.
                    SystemUpdateStep.Installing -> Unit

                    SystemUpdateStep.Finished -> step = SystemUpdateStep.Ready
                }
            },
            modifier = Modifier
                .widthIn(max = 280.dp)
                .fillMaxWidth(),
            state = when (step) {
                SystemUpdateStep.Ready, SystemUpdateStep.Finished -> LoadProgressState.Default
                SystemUpdateStep.Downloading, SystemUpdateStep.Installing -> LoadProgressState.Loading
                SystemUpdateStep.Paused -> LoadProgressState.Wait
            },
            progress = progress,
            max = SYSTEM_UPDATE_MAX,
            text = when (step) {
                SystemUpdateStep.Ready -> str("Download now")
                SystemUpdateStep.Downloading -> str("Downloading {}", percent)
                SystemUpdateStep.Paused -> str("Continue")
                SystemUpdateStep.Installing -> str("Installing {}", percent)
                SystemUpdateStep.Finished -> str("Reboot now")
            },
            style = InstallLoadStyle.BigRound,
        )
    }
}

/**
 * The interactive sample: the chosen style and state on one button, with a slider standing in for a
 * transfer's progress. The 220dp big pill leaves too little width for a caption beside it on a 360dp
 * screen, so the readout sits underneath.
 */
@Composable
private fun InstallLoadInteractiveDemo(style: InstallLoadStyle, state: LoadProgressState) {
    var progress by remember { mutableIntStateOf(60) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        InstallLoadProgress(
            onClick = { },
            state = state,
            progress = progress,
            max = 100,
            text = when {
                state == LoadProgressState.Loading -> "$progress%"

                state == LoadProgressState.Fail -> str("Retry")

                state == LoadProgressState.InstallHaveGift -> str("Gift")

                // The 52dp small pill fits a short verb only.
                style == InstallLoadStyle.Default -> str("Get")

                else -> str("Download")
            },
            style = style,
            hapticFeedback = state == LoadProgressState.Loading,
        )
        Slider(
            value = progress.toFloat(),
            onValueChange = { progress = it.toInt() },
            valueRange = 0f..100f,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = str("style: {} ({}) · state: {} ({}) · progress: {} / 100", style.name, style.value, state.name, state.value, progress),
            color = COUITheme.colorScheme.onBackgroundVariant,
            style = COUITheme.textStyles.footnote1,
        )
        Text(
            text = str("Fail fills the whole shape; InstallHaveGift paints none"),
            color = COUITheme.colorScheme.onBackgroundVariant,
            style = COUITheme.textStyles.footnote1,
        )
    }
}
