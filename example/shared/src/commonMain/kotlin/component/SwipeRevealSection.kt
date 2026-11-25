// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalCOUIApi::class)

package component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import i18n.str
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CardDefaults
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.SwipeRevealAction
import io.github.suqi8.coui.kmp.basic.SwipeRevealBox
import io.github.suqi8.coui.kmp.basic.SwipeRevealDefaults
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.basic.rememberSwipeRevealState
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.core.ExperimentalCOUIApi
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Favorites
import io.github.suqi8.coui.kmp.icon.extended.Pin
import io.github.suqi8.coui.kmp.icon.extended.Settings
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlinx.coroutines.launch
import utils.rememberBlurBackdrop

private val InitialSwipeItems = listOf(
    "Meeting notes.m4a" to "12.4 MB · Today 10:20",
    "Voice memo 12.m4a" to "4.8 MB · Yesterday",
    "Interview draft.m4a" to "28.1 MB · Mon",
)

private val SwipeRevealDarkBackdrop = listOf(Color(0xFF283A44), Color(0xFF14181C), Color(0xFF1E2228))
private val SwipeRevealLightBackdrop = listOf(Color(0xFFD6E4F0), Color(0xFFEAF0F6), Color(0xFFF3F4F6))
private val SwipeRevealPinColor = Color(0xFF0072F5)
private val SwipeRevealFavoriteColor = Color(0xFFFF7700)

/**
 * General-purpose [SwipeRevealBox] demo in the Basic category: lets you compare card corners
 * against a standard [Card], switch between compact squircle buttons, COUI-Kit circular droplets
 * (including two-step confirm delete), and upstream [SwipeActionItem]-style colored / text /
 * icon+text buttons, and independently toggle corner radius, blur, and rim.
 */
@Composable
fun SwipeRevealDemo() {
    val cardPresetIndex = remember { mutableIntStateOf(0) }
    val buttonStyleIndex = remember { mutableIntStateOf(0) }
    val radiusIndex = remember { mutableIntStateOf(1) }
    val enableBlur = remember { mutableStateOf(false) }
    val enableRim = remember { mutableStateOf(false) }
    val fullSwipeDelete = remember { mutableStateOf(true) }

    LaunchedEffect(cardPresetIndex.intValue) {
        when (cardPresetIndex.intValue) {
            0 -> {
                radiusIndex.intValue = 1
                enableBlur.value = false
                enableRim.value = false
            }

            1 -> {
                radiusIndex.intValue = 2
                enableBlur.value = false
                enableRim.value = false
            }

            2 -> {
                radiusIndex.intValue = 1
                enableBlur.value = true
                enableRim.value = true
            }
        }
    }

    val items = remember { mutableStateListOf(*InitialSwipeItems.toTypedArray()) }
    var statusMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val backdrop = rememberBlurBackdrop()
    val colors = COUITheme.colorScheme
    val isDark = colors.background.luminance() < 0.5f

    DemoOptions(
        DemoChoice("Card style", listOf("Card", "Group card", "Notification"), cardPresetIndex),
        DemoChoice(
            "Button style",
            listOf("Compact", "Circle (Kit)", "Colored", "Text", "Icon + Text", "Confirm"),
            buttonStyleIndex,
        ),
        DemoChoice("Corner radius", listOf("12dp", "17dp", "24dp"), radiusIndex),
        DemoToggle("Glass blur", enableBlur),
        DemoToggle("Edge rim", enableRim),
        DemoToggle("Full swipe to delete", fullSwipeDelete),
    )

    val chosenRadius = when (radiusIndex.intValue) {
        0 -> 12.dp
        2 -> CardDefaults.GroupCornerRadius
        else -> CardDefaults.CornerRadius
    }
    val chosenWeight = if (cardPresetIndex.intValue == 1) CardDefaults.GroupWeight else CardDefaults.Weight
    val chosenConicWeight = if (cardPresetIndex.intValue == 2) 1.1f else 0f
    val chosenContainerColor = if (cardPresetIndex.intValue == 2 && enableBlur.value) {
        Color.Unspecified
    } else {
        colors.surfaceContainer
    }

    val style = when (buttonStyleIndex.intValue) {
        1, 5 -> SwipeRevealDefaults.circleActionStyle(
            cornerRadius = chosenRadius,
            cornerWeight = chosenWeight,
            conicCornerWeight = chosenConicWeight,
            containerColor = chosenContainerColor,
            enableBlur = enableBlur.value,
            enableRim = enableRim.value,
        )

        2, 3, 4 -> SwipeRevealDefaults.roundRectActionStyle(
            cornerRadius = chosenRadius,
            cornerWeight = chosenWeight,
            conicCornerWeight = chosenConicWeight,
            containerColor = chosenContainerColor,
            enableBlur = enableBlur.value,
            enableRim = enableRim.value,
        )

        else -> if (cardPresetIndex.intValue == 2) {
            SwipeRevealDefaults.notificationStyle(
                cornerRadius = chosenRadius,
                enableBlur = enableBlur.value,
                enableRim = enableRim.value,
                minHeight = 0.dp,
                containerColor = chosenContainerColor,
            )
        } else {
            SwipeRevealDefaults.cardStyle(
                cornerRadius = chosenRadius,
                cornerWeight = chosenWeight,
                conicCornerWeight = chosenConicWeight,
                containerColor = chosenContainerColor,
                enableBlur = enableBlur.value,
                enableRim = enableRim.value,
            )
        }
    }

    val showGlassBackdrop = enableBlur.value || cardPresetIndex.intValue == 2
    val backdropBrush = remember(isDark) {
        Brush.verticalGradient(if (isDark) SwipeRevealDarkBackdrop else SwipeRevealLightBackdrop)
    }

    val pinLabel = str("Pin")
    val favoriteLabel = str("Favorite")
    val settingsLabel = str("Settings")
    val deleteLabel = str("Delete")
    val confirmDeleteLabel = str("Confirm delete")

    Box(Modifier.fillMaxWidth()) {
        if (showGlassBackdrop) {
            Box(
                Modifier
                    .matchParentSize()
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                    .background(backdropBrush),
            )
        }
        CompositionLocalProvider(LocalBackdrop provides if (showGlassBackdrop) backdrop else null) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SmallTitle(str("Reference Card (same corner radius)"))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    cornerRadius = chosenRadius,
                    weight = chosenWeight,
                ) {
                    BasicComponent(
                        title = str("Standard Card"),
                        summary = str("Compare corners and surface with the swipe rows below"),
                    )
                }

                SmallTitle(str("SwipeRevealBox"))
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items.forEach { entry ->
                        val (title, summary) = entry
                        key(title, buttonStyleIndex.intValue) {
                            val state = rememberSwipeRevealState()
                            val settingsMessage = str("Opened settings for {}", title)
                            val pinnedMessage = str("Pinned {}", title)
                            val favoritedMessage = str("Favorited {}", title)
                            val actions = when (buttonStyleIndex.intValue) {
                                // COUI-Kit circular droplets (Settings + red trash icon)
                                1 -> listOf(
                                    SwipeRevealAction(
                                        icon = COUIIcons.Pin,
                                        description = pinLabel,
                                        color = SwipeRevealPinColor,
                                        onClick = { statusMessage = pinnedMessage },
                                    ),
                                    SwipeRevealAction(
                                        icon = null,
                                        description = deleteLabel,
                                        color = colors.error,
                                        onClick = { items.remove(entry) },
                                    ),
                                )

                                // Upstream SwipeActionItem colored RoundRect pills
                                2 -> listOf(
                                    SwipeRevealAction(
                                        icon = COUIIcons.Pin,
                                        description = pinLabel,
                                        containerColor = SwipeRevealPinColor,
                                        color = Color.White,
                                        onClick = { statusMessage = pinnedMessage },
                                    ),
                                    SwipeRevealAction(
                                        icon = COUIIcons.Favorites,
                                        description = favoriteLabel,
                                        containerColor = SwipeRevealFavoriteColor,
                                        color = Color.White,
                                        onClick = { statusMessage = favoritedMessage },
                                    ),
                                    SwipeRevealAction(
                                        icon = null,
                                        description = deleteLabel,
                                        containerColor = colors.error,
                                        color = Color.White,
                                        onClick = { items.remove(entry) },
                                    ),
                                )

                                // Upstream SwipeActionItem text-only pills
                                3 -> listOf(
                                    SwipeRevealAction(
                                        label = pinLabel,
                                        description = pinLabel,
                                        containerColor = SwipeRevealPinColor,
                                        color = Color.White,
                                        onClick = { statusMessage = pinnedMessage },
                                    ),
                                    SwipeRevealAction(
                                        label = deleteLabel,
                                        description = deleteLabel,
                                        containerColor = colors.error,
                                        color = Color.White,
                                        onClick = { items.remove(entry) },
                                    ),
                                )

                                // Upstream SwipeActionItem icon + text pills
                                4 -> listOf(
                                    SwipeRevealAction(
                                        icon = COUIIcons.Pin,
                                        label = pinLabel,
                                        description = pinLabel,
                                        containerColor = SwipeRevealPinColor,
                                        color = Color.White,
                                        onClick = { statusMessage = pinnedMessage },
                                    ),
                                    SwipeRevealAction(
                                        label = deleteLabel,
                                        description = deleteLabel,
                                        useTrashIcon = true,
                                        containerColor = colors.error,
                                        color = Color.White,
                                        onClick = { items.remove(entry) },
                                    ),
                                )

                                // COUI-Kit single circular droplet with two-step checkmark confirm
                                5 -> listOf(
                                    SwipeRevealAction(
                                        icon = null,
                                        description = deleteLabel,
                                        confirmDescription = confirmDeleteLabel,
                                        color = colors.error,
                                        requireConfirmation = true,
                                        onClick = { items.remove(entry) },
                                    ),
                                )

                                // Default compact floating squircle buttons
                                else -> listOf(
                                    SwipeRevealAction(
                                        icon = COUIIcons.Settings,
                                        description = settingsLabel,
                                        onClick = { statusMessage = settingsMessage },
                                    ),
                                    SwipeRevealAction(
                                        icon = null,
                                        description = deleteLabel,
                                        color = colors.error,
                                        onClick = { items.remove(entry) },
                                    ),
                                )
                            }
                            SwipeRevealBox(
                                actions = actions,
                                state = state,
                                style = style,
                                dismissOnFullSwipe = fullSwipeDelete.value,
                                dismissPrimaryOnClick = true,
                            ) {
                                BasicComponent(
                                    title = title,
                                    summary = summary,
                                    onClick = {
                                        if (state.isRevealed) {
                                            scope.launch { state.close() }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                if (statusMessage.isNotEmpty()) {
                    SmallTitle(statusMessage)
                }
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(
                        text = str("Reset"),
                        onClick = {
                            items.clear()
                            items.addAll(InitialSwipeItems)
                            statusMessage = ""
                        },
                    )
                }
            }
        }
    }
}
