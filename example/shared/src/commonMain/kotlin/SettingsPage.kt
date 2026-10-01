// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalScrollBarApi::class)

package pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import appstate.LocalAppState
import appstate.LocalIsWideScreen
import appstate.LocalNavigator
import appstate.LocalUpdateAppState
import i18n.AppLanguage
import i18n.LocalLanguageSwitchEnabled
import i18n.str
import io.github.suqi8.coui.kmp.basic.COUIScrollBehavior
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.basic.ScrollBehavior
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.VerticalScrollBar
import io.github.suqi8.coui.kmp.basic.rememberScrollBarAdapter
import io.github.suqi8.coui.kmp.blur.LayerBackdrop
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.isRuntimeShaderSupported
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.interfaces.ExperimentalScrollBarApi
import io.github.suqi8.coui.kmp.preference.ArrowPreference
import io.github.suqi8.coui.kmp.preference.OverlayDropdownPreference
import io.github.suqi8.coui.kmp.preference.SwitchPreference
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.ThemeColorSpec
import io.github.suqi8.coui.kmp.theme.ThemePaletteStyle
import misc.VersionInfo
import navigation3.Route
import utils.AdaptiveTopAppBar
import utils.pageContentPadding
import utils.pageScrollModifiers
import utils.rememberBlurBackdrop

private val NavigationBarDisplayModeOptions = listOf("IconAndText", "IconOnly", "TextOnly", "IconWithSelectedLabel")
private val NavigationRailDisplayModeOptions = listOf("IconAndText", "IconOnly", "TextOnly", "IconWithSelectedLabel")
private val FloatingNavigationBarStyleOptions = listOf("Default", "iOS-like")
private val FloatingNavigationBarPositionOptions = listOf("Center", "Start", "End")
private val FloatingToolbarPositionOptions =
    listOf("TopStart", "CenterStart", "BottomStart", "TopEnd", "CenterEnd", "BottomEnd", "TopCenter", "BottomCenter")
private val FloatingToolbarOrientationOptions = listOf("Horizontal", "Vertical")
private val FabPositionOptions = listOf("Start", "Center", "End", "EndOverlay")
private val ColorModeOptions = listOf("System", "Light", "Dark", "MonetSystem", "MonetLight", "MonetDark")
private val PaletteStyleOptions = ThemePaletteStyle.entries.map { it.name }
private val ColorSpecOptions = ThemeColorSpec.entries.map { it.name }
private val KeyColorOptions = listOf("Default") + ui.KeyColors.map { it.first }

@Composable
fun SettingsPage(
    padding: PaddingValues,
) {
    val appState = LocalAppState.current
    val isWideScreen = LocalIsWideScreen.current
    val backdrop = rememberBlurBackdrop()
    val topAppBarScrollBehavior = COUIScrollBehavior()

    Scaffold(
        topBar = {
            // The bar reads the content layer behind it, so it must be a sibling of that layer,
            // never inside it — LocalBackdrop is provided to the bar slot alone.
            CompositionLocalProvider(LocalBackdrop provides backdrop) {
                AdaptiveTopAppBar(
                    title = str("Settings"),
                    showTopAppBar = appState.showTopAppBar,
                    isWideScreen = isWideScreen,
                    scrollBehavior = topAppBarScrollBehavior,
                    color = COUITheme.colorScheme.surface,
                    subtitle = "v${VersionInfo.VERSION_NAME} (${VersionInfo.VERSION_CODE})",
                )
            }
        },
    ) { innerPadding ->
        SettingsContent(
            innerPadding = innerPadding,
            outerPadding = padding,
            topAppBarScrollBehavior = topAppBarScrollBehavior,
            backdrop = backdrop,
        )
    }
}

@Composable
private fun SettingsContent(
    innerPadding: PaddingValues,
    outerPadding: PaddingValues,
    topAppBarScrollBehavior: ScrollBehavior,
    backdrop: LayerBackdrop?,
) {
    val appState = LocalAppState.current
    val isWideScreen = LocalIsWideScreen.current
    val updateAppState = LocalUpdateAppState.current
    val navigator = LocalNavigator.current
    val lazyListState = rememberLazyListState()

    // The host reserves its own bar's band and the scaffold reserves this page's: both are handed to
    // the padding helper so the content starts clear of whichever bar is actually in the way. Keeping
    // only the scaffold's inset here is what used to let the first row start under the host's toolbar.
    val contentPadding = pageContentPadding(innerPadding, outerPadding, isWideScreen, extraTop = 12.dp)
    Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.pageScrollModifiers(
                appState.enableScrollEndHaptic,
                appState.showTopAppBar,
                topAppBarScrollBehavior,
            ),
            contentPadding = contentPadding,
        ) {
            // The language entry sits above everything else and is the only row that changes what
            // the rest of the page says, so it leads rather than hides among the switches.
            item(key = "settingsLanguage") {
                if (LocalLanguageSwitchEnabled.current) {
                    SmallTitle(str("Language"))
                    Card(
                        modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
                    ) {
                        OverlayDropdownPreference(
                            title = str("Language"),
                            // Each language names itself, so the entry a user is hunting for reads
                            // the same whichever language the app is currently showing.
                            items = AppLanguage.entries.map { it.label },
                            selectedIndex = appState.language.ordinal,
                            onSelectedIndexChange = { index ->
                                updateAppState { state ->
                                    state.copy(language = AppLanguage.entries[index])
                                }
                            },
                        )
                    }
                }
            }
            item(key = "settingsUi") {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                ) {
                    SwitchPreference(
                        title = str("Show FPS Monitor"),
                        checked = appState.showFPSMonitor,
                        onCheckedChange = { updateAppState { state -> state.copy(showFPSMonitor = it) } },
                    )
                    AnimatedVisibility(visible = isRuntimeShaderSupported()) {
                        Column {
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            SwitchPreference(
                                title = str("Enable Squircle Shapes"),
                                checked = appState.enableSquircle,
                                onCheckedChange = { updateAppState { state -> state.copy(enableSquircle = it) } },
                            )
                        }
                    }
                    AnimatedVisibility(visible = isRuntimeShaderSupported()) {
                        Column {
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            SwitchPreference(
                                title = str("Enable Blur Effect"),
                                checked = appState.enableBlur,
                                onCheckedChange = { updateAppState { state -> state.copy(enableBlur = it) } },
                            )
                        }
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    SwitchPreference(
                        title = str("Show TopAppBar"),
                        checked = appState.showTopAppBar,
                        onCheckedChange = { updateAppState { state -> state.copy(showTopAppBar = it) } },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    SwitchPreference(
                        title = if (isWideScreen) str("Show NavigationRail") else str("Show NavigationBar"),
                        checked = appState.showNavigationBar,
                        onCheckedChange = { updateAppState { state -> state.copy(showNavigationBar = it) } },
                    )
                    AnimatedVisibility(visible = appState.showNavigationBar && !isWideScreen && !appState.useFloatingNavigationBar) {
                        Column {
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            OverlayDropdownPreference(
                                title = str("NavigationBar Mode"),
                                items = NavigationBarDisplayModeOptions.map { str(it) },
                                selectedIndex = appState.navigationBarMode,
                                onSelectedIndexChange = { updateAppState { state -> state.copy(navigationBarMode = it) } },
                            )
                        }
                    }
                    AnimatedVisibility(visible = appState.showNavigationBar && isWideScreen) {
                        Column {
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            OverlayDropdownPreference(
                                title = str("NavigationRail Mode"),
                                items = NavigationRailDisplayModeOptions.map { str(it) },
                                selectedIndex = appState.navigationRailMode,
                                onSelectedIndexChange = { updateAppState { state -> state.copy(navigationRailMode = it) } },
                            )
                        }
                    }
                    AnimatedVisibility(visible = appState.showNavigationBar && !isWideScreen) {
                        Column {
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            SwitchPreference(
                                title = str("Use FloatingNavigationBar"),
                                checked = appState.useFloatingNavigationBar,
                                onCheckedChange = { updateAppState { state -> state.copy(useFloatingNavigationBar = it) } },
                            )
                            AnimatedVisibility(visible = appState.useFloatingNavigationBar) {
                                Column {
                                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                                    OverlayDropdownPreference(
                                        title = str("FloatingNavigationBar Style"),
                                        items = FloatingNavigationBarStyleOptions.map { str(it) },
                                        selectedIndex = appState.floatingNavigationBarStyle,
                                        onSelectedIndexChange = { updateAppState { state -> state.copy(floatingNavigationBarStyle = it) } },
                                    )
                                    AnimatedVisibility(visible = appState.floatingNavigationBarStyle == 0) {
                                        Column {
                                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                                            OverlayDropdownPreference(
                                                title = str("FloatingNavigationBar Position"),
                                                items = FloatingNavigationBarPositionOptions.map { str(it) },
                                                selectedIndex = appState.floatingNavigationBarPosition,
                                                onSelectedIndexChange = { updateAppState { state -> state.copy(floatingNavigationBarPosition = it) } },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    SwitchPreference(
                        title = str("Show FloatingToolbar"),
                        checked = appState.showFloatingToolbar,
                        onCheckedChange = { updateAppState { state -> state.copy(showFloatingToolbar = it) } },
                    )
                    AnimatedVisibility(visible = appState.showFloatingToolbar) {
                        Column {
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            OverlayDropdownPreference(
                                title = str("FloatingToolbar Position"),
                                items = FloatingToolbarPositionOptions.map { str(it) },
                                selectedIndex = appState.floatingToolbarPosition,
                                onSelectedIndexChange = { updateAppState { state -> state.copy(floatingToolbarPosition = it) } },
                            )
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            OverlayDropdownPreference(
                                title = str("FloatingToolbar Orientation"),
                                items = FloatingToolbarOrientationOptions.map { str(it) },
                                selectedIndex = appState.floatingToolbarOrientation,
                                onSelectedIndexChange = { updateAppState { state -> state.copy(floatingToolbarOrientation = it) } },
                            )
                        }
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    SwitchPreference(
                        title = str("Show FloatingActionButton"),
                        checked = appState.showFloatingActionButton,
                        onCheckedChange = { updateAppState { state -> state.copy(showFloatingActionButton = it) } },
                    )
                    AnimatedVisibility(visible = appState.showFloatingActionButton) {
                        Column {
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            OverlayDropdownPreference(
                                title = str("FloatingActionButton Position"),
                                items = FabPositionOptions.map { str(it) },
                                selectedIndex = appState.floatingActionButtonPosition,
                                onSelectedIndexChange = { updateAppState { state -> state.copy(floatingActionButtonPosition = it) } },
                            )
                        }
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    SwitchPreference(
                        title = str("Enable Scroll End Haptic"),
                        checked = appState.enableScrollEndHaptic,
                        onCheckedChange = { updateAppState { state -> state.copy(enableScrollEndHaptic = it) } },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    SwitchPreference(
                        title = str("Enable Page User Scroll"),
                        checked = appState.enablePageUserScroll,
                        onCheckedChange = { updateAppState { state -> state.copy(enablePageUserScroll = it) } },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    OverlayDropdownPreference(
                        title = str("Color Mode"),
                        items = ColorModeOptions.map { str(it) },
                        selectedIndex = appState.colorMode,
                        onSelectedIndexChange = { updateAppState { state -> state.copy(colorMode = it) } },
                    )
                    AnimatedVisibility(visible = appState.colorMode in 3..5) {
                        Column {
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            OverlayDropdownPreference(
                                title = str("Key Color"),
                                items = KeyColorOptions.map { str(it) },
                                selectedIndex = appState.seedIndex,
                                onSelectedIndexChange = { updateAppState { state -> state.copy(seedIndex = it) } },
                            )
                        }
                    }
                    AnimatedVisibility(visible = appState.colorMode in 3..5 && appState.seedIndex > 0) {
                        Column {
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            OverlayDropdownPreference(
                                title = str("Palette Style"),
                                items = PaletteStyleOptions,
                                selectedIndex = appState.paletteStyle,
                                onSelectedIndexChange = { updateAppState { state -> state.copy(paletteStyle = it) } },
                            )
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            OverlayDropdownPreference(
                                title = str("Color Spec"),
                                items = ColorSpecOptions,
                                selectedIndex = appState.colorSpec,
                                onSelectedIndexChange = { updateAppState { state -> state.copy(colorSpec = it) } },
                            )
                        }
                    }
                }
            }
            item(key = "settingsTransition") {
                SmallTitle(str("Navigation3"))
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
                ) {
                    SwitchPreference(
                        title = str("Enable Corner Clip"),
                        summary = str("Clip the top scene with rounded corners during transitions"),
                        checked = appState.enableCornerClip,
                        onCheckedChange = { updateAppState { state -> state.copy(enableCornerClip = it) } },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    SwitchPreference(
                        title = str("Enable Dim"),
                        summary = str("Dim the scene behind during transitions"),
                        checked = appState.enableDim,
                        onCheckedChange = { updateAppState { state -> state.copy(enableDim = it) } },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    SwitchPreference(
                        title = str("Block Input During Transition"),
                        summary = str("Block touch input on the non-target scene"),
                        checked = appState.blockInputDuringTransition,
                        onCheckedChange = { updateAppState { state -> state.copy(blockInputDuringTransition = it) } },
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    SwitchPreference(
                        title = str("Pop Follows Swipe Edge"),
                        summary = str("Pop animation direction follows the finger swipe edge"),
                        checked = appState.popDirectionFollowsSwipeEdge,
                        onCheckedChange = { updateAppState { state -> state.copy(popDirectionFollowsSwipeEdge = it) } },
                    )
                }
            }
            item(key = "settingsAbout") {
                SmallTitle(str("Other"))
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    ArrowPreference(
                        title = str("About"),
                        summary = str("About this example App"),
                        onClick = { navigator.push(Route.About) },
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
        VerticalScrollBar(
            adapter = rememberScrollBarAdapter(lazyListState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            trackPadding = contentPadding,
        )
    }
}
