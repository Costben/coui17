// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ktlint:compose:multiple-emitters-check")

package pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import component.BottomFloatingScrollToolbarDemo
import component.NoteToolbarDemo
import i18n.str
import io.github.suqi8.coui.kmp.basic.BottomFloatingAccessoryNavigationBar
import io.github.suqi8.coui.kmp.basic.BottomFloatingNavigationBarItem
import io.github.suqi8.coui.kmp.basic.Button
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.FloatingActionButton
import io.github.suqi8.coui.kmp.basic.HighlightedIconButton
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.InputField
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.TabRowWithContour
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.basic.materialInnerGlow
import io.github.suqi8.coui.kmp.blur.LayerBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Add
import io.github.suqi8.coui.kmp.icon.extended.Back
import io.github.suqi8.coui.kmp.icon.extended.Home
import io.github.suqi8.coui.kmp.icon.extended.Messages
import io.github.suqi8.coui.kmp.icon.extended.More
import io.github.suqi8.coui.kmp.icon.extended.Search
import io.github.suqi8.coui.kmp.shared.generated.resources.Res
import io.github.suqi8.coui.kmp.shared.generated.resources.blur_test_bg
import io.github.suqi8.coui.kmp.squircle.SquircleShape
import io.github.suqi8.coui.kmp.theme.COUITheme
import org.jetbrains.compose.resources.painterResource
import utils.WallpaperState
import utils.rememberBlurBackdrop

/**
 * Side-by-side checks for the ColorOS 17 material paths: each stage puts the controls over a
 * wallpaper they can sample, so the opt-in blurred presets can be compared against the flat ones.
 */
@Composable
fun SystemAlignmentPage(
    padding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
    ) {
        item { FloatingToolbarStage() }
        item { BatteryModeStage() }
        item { UsageTabStage() }
        item { SegmentStage() }
        item { ButtonStage() }
        item { ToolbarButtonStage() }
        item { SearchStage() }
        item { BottomBarStage() }
        item { Note(str("With material blur turned off in system settings, or in power saving, every glass surface above falls back to its flat fill.")) }
    }
}

private class BatteryMode(
    val label: String,
    val accent: Color,
    val fillLight: Color,
    val fillDark: Color,
    val outlineIcon: ImageVector,
    val filledIcon: ImageVector,
)

/** `BatteryModePreference`: the accents and tinted fills are the app's own `battery_mode_*` colours. */
private val BatteryModes = listOf(
    BatteryMode("Balanced", Color(0xFF00990F), Color(0x1400BD13), Color(0x1A24B232), BatteryModeIcons.BalancedOutline, BatteryModeIcons.BalancedFilled),
    BatteryMode("Power saving", Color(0xFFDB9A00), Color(0x26E19F04), Color(0x1AE5A100), BatteryModeIcons.PowerSaveOutline, BatteryModeIcons.PowerSaveFilled),
    BatteryMode("High performance", Color(0xFF0080FF), Color(0x140066FF), Color(0x261A8CFF), BatteryModeIcons.PerformanceOutline, BatteryModeIcons.PerformanceFilled),
)

private val BatteryModeShape = SquircleShape(20.dp)
private val BatteryModeIdleLight = Color(0x0A000000)
private val BatteryModeIdleDark = Color(0x14FFFFFF)

@Composable
private fun BatteryModeStage() {
    var selected by remember { mutableIntStateOf(0) }
    SmallTitle(text = str("Selected option: tinted fill and inner glow"))
    Card(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BatteryModes.forEachIndexed { index, mode ->
                BatteryModeItem(
                    mode = mode,
                    selected = selected == index,
                    onClick = { selected = index },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BatteryModeItem(
    mode: BatteryMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = COUITheme.colorScheme.background.luminance() < 0.5f
    val fill = when {
        !selected -> if (isDark) BatteryModeIdleDark else BatteryModeIdleLight
        isDark -> mode.fillDark
        else -> mode.fillLight
    }
    val titleColor = if (selected) mode.accent else COUITheme.colorScheme.onSurface
    val summaryColor = if (selected) mode.accent else COUITheme.colorScheme.onSurfaceVariantSummary
    val iconColor = when {
        selected -> mode.accent
        isDark -> Color(0xCEFFFFFF)
        else -> Color(0xCE000000)
    }
    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 88.dp)
            .clip(BatteryModeShape)
            .then(if (selected) Modifier.materialInnerGlow() else Modifier)
            .background(fill)
            .then(if (selected) Modifier.border(1.dp, mode.accent, BatteryModeShape) else Modifier)
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .semantics { this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (selected) mode.filledIcon else mode.outlineIcon,
            contentDescription = null,
            modifier = Modifier.padding(top = 10.dp),
            tint = iconColor,
        )
        Text(
            text = str(mode.label),
            modifier = Modifier.padding(horizontal = 4.dp),
            color = titleColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = str("Demo"),
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp),
            color = summaryColor,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FloatingToolbarStage() {
    SmallTitle(text = str("Notes toolbar"))
    NoteToolbarDemo()
    SmallTitle(text = str("Photos editor bar"))
    BottomFloatingScrollToolbarDemo()
}

@Composable
private fun UsageTabStage() {
    var selected by remember { mutableIntStateOf(0) }
    val tabs = listOf(str("Demo one"), str("Demo two"))
    SmallTitle(text = str("Usage tabs: squash, drag and continuous curvature"))
    Card(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            TabRowWithContour(
                tabs = tabs,
                selectedTabIndex = selected,
                onTabSelected = { selected = it },
            )
            Text(
                text = tabs[selected],
                modifier = Modifier.padding(top = 16.dp),
                style = COUITheme.textStyles.body2,
                color = COUITheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

@Composable
private fun SegmentStage() {
    var selected by remember { mutableIntStateOf(0) }
    var committed by remember { mutableIntStateOf(0) }
    val tabs = listOf(str("Day"), str("Week"), str("Month"), str("Year"))
    SmallTitle(text = str("Segmented control: drag from the selected segment"))
    Stage(height = 168.dp) { backdrop ->
        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TabRowWithContour(
                tabs = tabs,
                selectedTabIndex = selected,
                onTabSelected = { selected = it },
                backdrop = backdrop,
            )
            TabRowWithContour(
                tabs = tabs,
                selectedTabIndex = committed,
                onTabSelected = { committed = it },
                backdrop = backdrop,
                commitOnRelease = true,
            )
        }
    }
    Note(str("Top: selection follows the finger. Bottom: commits on release."))
}

@Composable
private fun ButtonStage() {
    SmallTitle(text = str("Buttons: touch motion and content button material"))
    Stage(height = 184.dp) { backdrop ->
        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    text = str("Secondary"),
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    backdrop = backdrop,
                )
                TextButton(
                    text = str("Recommend button"),
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    backdrop = backdrop,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    text = str("Flat"),
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    enabled = false,
                    colors = ButtonDefaults.buttonColorsPrimary(),
                    backdrop = backdrop,
                ) {
                    Text(text = str("Disabled button"))
                }
            }
        }
    }
    Note(str("Top row blurs the wallpaper; bottom left is the flat fill for comparison."))
}

@Composable
private fun ToolbarButtonStage() {
    SmallTitle(text = str("Toolbar buttons: default vs blurred preset"))
    Stage(height = 120.dp) { backdrop ->
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HighlightedIconButton(onClick = {}) {
                Icon(imageVector = COUIIcons.Back, contentDescription = str("Back"))
            }
            HighlightedIconButton(onClick = {}, backdrop = backdrop) {
                Icon(imageVector = COUIIcons.Back, contentDescription = str("Back"))
            }
            HighlightedIconButton(onClick = {}, backdrop = backdrop) {
                Icon(imageVector = COUIIcons.More, contentDescription = str("More"))
            }
        }
    }
    Note(str("Left: the default preset, unblurred like the system's. Right: TYPE_FRAMEWORK_TOP_BAR_BLUR."))
}

@Composable
private fun SearchStage() {
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf("") }
    SmallTitle(text = str("Floating search field"))
    Stage(height = 120.dp) { backdrop ->
        Row(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InputField(
                query = query,
                onQueryChange = { query = it },
                onSearch = { submitted = it },
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.weight(1f),
                label = str("Search"),
                backdrop = backdrop,
            )
            if (expanded) {
                TextButton(text = str("Cancel"), onClick = { expanded = false }, backdrop = backdrop)
            }
        }
    }
    Note(
        if (submitted.isEmpty()) {
            str("Search from the keyboard: the keyboard hides and the query stays. Cancel clears it.")
        } else {
            str("Searched") + ": " + submitted
        },
    )
}

@Composable
private fun BottomBarStage() {
    var selectedIndex by remember { mutableIntStateOf(0) }
    SmallTitle(text = str("Accessory bar and floating button: bottom bar / FAB material"))
    Stage(height = 200.dp) { backdrop ->
        FloatingActionButton(
            onClick = {},
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            backdrop = backdrop,
        ) {
            Icon(imageVector = COUIIcons.Add, contentDescription = str("Add"), tint = COUITheme.colorScheme.onPrimary)
        }
        BottomFloatingAccessoryNavigationBar(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
            backdrop = backdrop,
            endAccessory = {
                Icon(imageVector = COUIIcons.Search, contentDescription = str("Search"))
            },
        ) {
            listOf(str("Home") to COUIIcons.Home, str("Messages") to COUIIcons.Messages).forEachIndexed { index, (label, icon) ->
                BottomFloatingNavigationBarItem(
                    selected = selectedIndex == index,
                    onClick = { selectedIndex = index },
                    icon = icon,
                    label = label,
                )
            }
        }
    }
}

/**
 * A wallpaper the controls in [content] can sample. The wallpaper layer is a sibling of the
 * controls, never their ancestor, so the backdrop never records the controls themselves. [content]
 * receives `null` when blur is off in the gallery settings.
 */
@Composable
private fun Stage(
    height: Dp,
    content: @Composable BoxScope.(LayerBackdrop?) -> Unit,
) {
    val backdrop = rememberBlurBackdrop()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
        ) {
            WallpaperState.bitmap?.let { wallpaper ->
                Image(
                    bitmap = wallpaper,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } ?: Image(
                painter = painterResource(Res.drawable.blur_test_bg),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        content(backdrop)
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        style = COUITheme.textStyles.footnote1,
        color = COUITheme.colorScheme.onSurfaceVariantSummary,
    )
}
