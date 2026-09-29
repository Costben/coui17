// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui17

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.Button
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CircularProgressIndicator
import io.github.suqi8.coui.kmp.basic.CompleteState
import io.github.suqi8.coui.kmp.basic.CompleteStateDefaults
import io.github.suqi8.coui.kmp.basic.CompleteStateStatus
import io.github.suqi8.coui.kmp.basic.EmptyState
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.InputField
import io.github.suqi8.coui.kmp.basic.LinearProgressIndicator
import io.github.suqi8.coui.kmp.basic.LoadProgressState
import io.github.suqi8.coui.kmp.basic.ScrollBehavior
import io.github.suqi8.coui.kmp.basic.SearchBar
import io.github.suqi8.coui.kmp.basic.SmallTitle
import io.github.suqi8.coui.kmp.basic.SnackbarHostState
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Album
import io.github.suqi8.coui.kmp.icon.extended.Background
import io.github.suqi8.coui.kmp.icon.extended.BankCards
import io.github.suqi8.coui.kmp.icon.extended.Carrier
import io.github.suqi8.coui.kmp.icon.extended.Community
import io.github.suqi8.coui.kmp.icon.extended.Hide
import io.github.suqi8.coui.kmp.icon.extended.Image
import io.github.suqi8.coui.kmp.icon.extended.Info
import io.github.suqi8.coui.kmp.icon.extended.Link
import io.github.suqi8.coui.kmp.icon.extended.Lock
import io.github.suqi8.coui.kmp.icon.extended.Merge
import io.github.suqi8.coui.kmp.icon.extended.Messages
import io.github.suqi8.coui.kmp.icon.extended.Music
import io.github.suqi8.coui.kmp.icon.extended.Notes
import io.github.suqi8.coui.kmp.icon.extended.Share
import io.github.suqi8.coui.kmp.icon.extended.Show
import io.github.suqi8.coui.kmp.icon.extended.Theme
import io.github.suqi8.coui.kmp.icon.extended.Timer
import io.github.suqi8.coui.kmp.icon.extended.Update
import io.github.suqi8.coui.kmp.icon.extended.UploadCloud
import io.github.suqi8.coui.kmp.icon.extended.VolumeOff
import io.github.suqi8.coui.kmp.icon.extended.VolumeUp
import io.github.suqi8.coui.kmp.overlay.OverlayBottomSheet
import io.github.suqi8.coui.kmp.overlay.OverlayDialog
import io.github.suqi8.coui.kmp.preference.AppInfoPreference
import io.github.suqi8.coui.kmp.preference.ArrowPreference
import io.github.suqi8.coui.kmp.preference.BottomPreference
import io.github.suqi8.coui.kmp.preference.CheckBoxPreferenceCategory
import io.github.suqi8.coui.kmp.preference.CheckBoxWithDividerPreference
import io.github.suqi8.coui.kmp.preference.EditTextPreference
import io.github.suqi8.coui.kmp.preference.InputPreference
import io.github.suqi8.coui.kmp.preference.ListPreference
import io.github.suqi8.coui.kmp.preference.ListPreferenceEntry
import io.github.suqi8.coui.kmp.preference.LoadInstallProgressPreference
import io.github.suqi8.coui.kmp.preference.LoadingPreferenceCategory
import io.github.suqi8.coui.kmp.preference.MarkWithDividerPreference
import io.github.suqi8.coui.kmp.preference.OverlayDropdownPreference
import io.github.suqi8.coui.kmp.preference.PagerFooterPreference
import io.github.suqi8.coui.kmp.preference.PagerHeaderPreference
import io.github.suqi8.coui.kmp.preference.RadioButtonPreference
import io.github.suqi8.coui.kmp.preference.SliderPreference
import io.github.suqi8.coui.kmp.preference.SpannablePreference
import io.github.suqi8.coui.kmp.preference.StatusSwitchingContent
import io.github.suqi8.coui.kmp.preference.StatusSwitchingPreference
import io.github.suqi8.coui.kmp.preference.StepperPreference
import io.github.suqi8.coui.kmp.preference.SwitchPreference
import io.github.suqi8.coui.kmp.preference.SwitchWithDividerPreference
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.overScrollVertical
import io.github.suqi8.coui.kmp.utils.scrollEndHaptic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val IconBlue = Color(0xFF3B82F6)
private val IconGreen = Color(0xFF22C55E)
private val IconOrange = Color(0xFFF59E0B)
private val IconPurple = Color(0xFF8B5CF6)
private val IconRed = Color(0xFFEF4444)
private val IconTeal = Color(0xFF14B8A6)

/**
 * The per-row floor of ColorOS 17's settings home page, from the device token
 * `homepage_preference_min_height` (52dp). It sits above the 48dp `support_preference_min_height`
 * that [BasicComponent] applies to a standard preference row, and an outer `heightIn` wins.
 */
private val HomepageRowMinHeight = 52.dp

/** The ColorOS 17 settings list, with every control sitting where the real app puts it. */
@Composable
internal fun SettingsScreen(
    padding: PaddingValues,
    scrollBehavior: ScrollBehavior,
    snackbarHostState: SnackbarHostState,
    wallpaperSource: WallpaperSource,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    meltBallExpanded: Boolean,
    onMeltBallExpandedChange: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }

    var showOptimizeResult by remember { mutableStateOf(false) }

    var wifi by remember { mutableStateOf(true) }
    var bluetooth by remember { mutableStateOf(true) }
    var hotspot by remember { mutableStateOf(false) }
    var nfc by remember { mutableStateOf(false) }

    var brightness by remember { mutableFloatStateOf(0.72f) }
    var autoBrightness by remember { mutableStateOf(true) }
    var colorModeIndex by remember { mutableIntStateOf(0) }
    var refreshIndex by remember { mutableIntStateOf(1) }
    var fontScaleIndex by remember { mutableIntStateOf(1) }

    var mediaVolume by remember { mutableFloatStateOf(0.45f) }
    var silent by remember { mutableStateOf(false) }
    var ringtoneIndex by remember { mutableIntStateOf(0) }
    var showRingtoneSheet by remember { mutableStateOf(false) }

    var powerSaving by remember { mutableStateOf(false) }
    var notifyEnabled by remember { mutableStateOf(true) }
    var lockScreenNotify by remember { mutableStateOf(true) }
    var notifyDetail by remember { mutableStateOf(true) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // `COUILoadingPreferenceCategory` is a transient state on the device too: the settings app shows
    // it while it re-reads the battery stats and swaps the widget out when the read returns.
    var batteryLoading by remember { mutableStateOf(true) }
    var deviceName by remember { mutableStateOf("OnePlus PLK110") }
    var bluetoothName by remember { mutableStateOf("COUI-Lab 17") }
    var systemUpdateState by remember { mutableStateOf(LoadProgressState.Default) }
    var systemUpdateProgress by remember { mutableIntStateOf(0) }

    val colorModes = remember { listOf("自然", "鲜艳", "柔和") }
    val refreshRates = remember { listOf("60Hz", "120Hz", "智能切换") }
    val ringtones = remember { listOf("清晨", "潮汐", "静谧", "无") }
    val fontScales = remember { listOf("小", "标准", "大", "超大") }
    val appIcon = rememberAppIcon()
    val suggestions = remember {
        listOf("WLAN 与网络", "蓝牙", "显示与亮度", "声音与振动", "电池", "通知与状态栏")
    }

    // A category-level "select all" box reads `Indeterminate` while only part of the group is on,
    // which is exactly what `COUICheckBox`'s tri-state is for.
    val notifyAllState = when {
        notifyEnabled && lockScreenNotify && notifyDetail -> ToggleableState.On
        !notifyEnabled && !lockScreenNotify && !notifyDetail -> ToggleableState.Off
        else -> ToggleableState.Indeterminate
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                // ColorOS 17's settings list is opaque: the device renders this page as a uniform
                // `coui_color_background_with_card` (#F0F1F2) with no wallpaper showing through, which
                // is what `colorScheme.surface` carries. A translucent scrim here reads as #F9FAFA —
                // wallpaper-dependent, and not a value the device colour table contains at all.
                .background(COUITheme.colorScheme.surface)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            // While the search page is open the device replaces the settings list entirely, so the
            // list must not be reachable behind the results.
            userScrollEnabled = !searchExpanded,
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 8.dp,
            ),
        ) {
            item(key = "search") {
                SearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        // No horizontal padding here: SearchBarDefaults.InsideMargin already insets the
                        // capsule by 16dp x 6dp, which is exactly the device's geometry (capsule 40dp tall
                        // inside a 52dp row at a 16dp page margin). Adding another 16dp doubles it to 32dp.
                        .padding(bottom = 8.dp),
                    inputField = {
                        InputField(
                            query = query,
                            onQueryChange = { query = it },
                            onSearch = { searchExpanded = false },
                            expanded = searchExpanded,
                            onExpandedChange = { searchExpanded = it },
                            label = "搜索设置项",
                        )
                    },
                    outsideEndAction = {
                        Text(
                            text = "取消",
                            color = COUITheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable {
                                    query = ""
                                    searchExpanded = false
                                }
                                .padding(end = 16.dp),
                        )
                    },
                    expanded = searchExpanded,
                    onExpandedChange = { searchExpanded = it },
                ) {
                    val visible = suggestions
                        .filter { query.isEmpty() || it.contains(query, ignoreCase = true) }
                    if (visible.isEmpty()) {
                        EmptyState(
                            title = "未找到结果",
                            subtitle = "试试搜索“WLAN”“蓝牙”或“电池”等设置项",
                            actionText = "清除搜索内容",
                            onActionClick = { query = "" },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Column {
                            visible.forEach { suggestion ->
                                BasicComponent(
                                    title = suggestion,
                                    onClick = {
                                        query = suggestion
                                        searchExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            if (searchExpanded) {
                // ColorOS covers the settings list with the search page, so the list stays neither
                // visible nor scrollable behind the results.
                item(key = "searchCover") {
                    Box(
                        modifier = Modifier
                            .fillParentMaxHeight()
                            .background(COUITheme.colorScheme.surface),
                    )
                }
            }

            // `COUIPagerHeaderPreference` is the note a ColorOS settings sub-page opens with. This
            // demo has no sub-page, so it takes the page-top slot right below the search bar.
            item(key = "pageHeader") {
                PagerHeaderPreference(
                    summary = "以下设置项按 ColorOS 17 设置首页的排布放置，每个控件都是 COUI 17 组件。",
                )
            }

            item(key = "connection") {
                SmallTitle(text = "连接与共享")
                SettingsCard {
                    SwitchPreference(
                        title = "WLAN",
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                        endActions = { InlineValue("COUI-Lab-5G") },
                        startAction = { SettingIcon(COUIIcons.Community, IconBlue) },
                        checked = wifi,
                        onCheckedChange = { wifi = it },
                    )
                    SettingsDivider()
                    SwitchPreference(
                        title = "蓝牙",
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                        endActions = { InlineValue("已连接") },
                        startAction = { SettingIcon(COUIIcons.Link, IconBlue) },
                        checked = bluetooth,
                        onCheckedChange = { bluetooth = it },
                    )
                    SettingsDivider()
                    InputPreference(
                        title = "蓝牙名称",
                        value = bluetoothName,
                        onValueChange = { bluetoothName = it },
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                        maxCount = 24,
                        showClearButton = true,
                    )
                    SettingsDivider()
                    ArrowPreference(
                        title = "移动网络",
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                        endActions = { InlineValue("5G · 中国移动") },
                        startAction = { SettingIcon(COUIIcons.Carrier, IconGreen) },
                        onClick = { notify(scope, snackbarHostState, "移动网络设置暂不可用") },
                    )
                    SettingsDivider()
                    SwitchPreference(
                        title = "个人热点",
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                        startAction = { SettingIcon(COUIIcons.Share, IconOrange) },
                        checked = hotspot,
                        onCheckedChange = { hotspot = it },
                    )
                    SettingsDivider()
                    SwitchPreference(
                        title = "NFC",
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                        startAction = { SettingIcon(COUIIcons.BankCards, IconPurple) },
                        checked = nfc,
                        onCheckedChange = { nfc = it },
                    )
                }
            }

            item(key = "display") {
                SmallTitle(text = "显示与亮度")
                SettingsCard {
                    SliderPreference(
                        value = brightness,
                        onValueChange = { brightness = it },
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                        title = "亮度",
                        valueText = "${(brightness * 100).toInt()}%",
                        startAction = { SettingIcon(COUIIcons.Theme, IconOrange) },
                        showKeyPoints = true,
                    )
                    SettingsDivider()
                    SwitchPreference(
                        title = "自动调节亮度",
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                        startAction = { SettingIcon(COUIIcons.Show, IconOrange) },
                        checked = autoBrightness,
                        onCheckedChange = { autoBrightness = it },
                    )
                    SettingsDivider()
                    SwitchPreference(
                        title = "深色模式",
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                        startAction = { SettingIcon(COUIIcons.Background, IconPurple) },
                        checked = darkMode,
                        onCheckedChange = onDarkModeChange,
                    )
                    SettingsDivider()
                    ListPreference(
                        entries = colorModes.map { ListPreferenceEntry(text = it) },
                        selectedIndex = colorModeIndex,
                        onSelectedIndexChange = { colorModeIndex = it },
                        title = "色彩模式",
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                        dialogTitle = "色彩模式",
                        cancelButtonText = "取消",
                        startAction = { SettingIcon(COUIIcons.Album, IconTeal) },
                        showValue = true,
                    )
                    SettingsDivider()
                    StepperPreference(
                        value = fontScaleIndex,
                        onValueChange = { fontScaleIndex = it },
                        title = "字体大小",
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                        summary = fontScales[fontScaleIndex],
                        minValue = 0,
                        maxValue = fontScales.lastIndex,
                        startAction = { SettingIcon(COUIIcons.Notes, IconOrange) },
                    )
                    SettingsDivider()
                    OverlayDropdownPreference(
                        items = refreshRates,
                        selectedIndex = refreshIndex,
                        title = "屏幕刷新率",
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                        startAction = { SettingIcon(COUIIcons.Update, IconGreen) },
                        onSelectedIndexChange = { refreshIndex = it },
                    )
                }
            }

            item(key = "sound") {
                SmallTitle(text = "声音与振动")
                SettingsCard {
                    SliderPreference(
                        value = mediaVolume,
                        onValueChange = { mediaVolume = it },
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                        title = "媒体音量",
                        valueText = "${(mediaVolume * 100).toInt()}%",
                        startAction = { SettingIcon(COUIIcons.VolumeUp, IconBlue) },
                    )
                    SettingsDivider()
                    SwitchPreference(
                        title = "静音",
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                        startAction = { SettingIcon(COUIIcons.VolumeOff, IconRed) },
                        checked = silent,
                        onCheckedChange = { silent = it },
                    )
                    SettingsDivider()
                    ArrowPreference(
                        title = "铃声",
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                        endActions = { InlineValue(ringtones[ringtoneIndex]) },
                        startAction = { SettingIcon(COUIIcons.Music, IconPurple) },
                        onClick = { showRingtoneSheet = true },
                    )
                }
            }

            item(key = "battery") {
                // `COUILoadingPreferenceCategory`: the device shows the spinner while it re-reads the
                // battery stats, then swaps the widget out. The "after loading" layout is empty here,
                // so the header simply settles on its title.
                LoadingPreferenceCategory(
                    title = "电池",
                    loading = batteryLoading,
                    loadingText = "正在统计用电情况…",
                )
                SettingsCard {
                    StatusSwitchingPreference(
                        title = "电池电量",
                        status = StatusSwitchingContent.Text("约 9 小时 12 分可用"),
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                        startAction = { SettingIcon(COUIIcons.Timer, IconGreen) },
                        bottomAction = {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    progress = 0.82f,
                                )
                            }
                        },
                    )
                    SettingsDivider()
                    SwitchPreference(
                        title = "省电模式",
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                        startAction = { SettingIcon(COUIIcons.Update, IconGreen) },
                        checked = powerSaving,
                        onCheckedChange = { powerSaving = it },
                    )
                }
            }

            item(key = "notification") {
                CheckBoxPreferenceCategory(
                    title = "通知与状态栏",
                    state = notifyAllState,
                    onClick = {
                        val turnOn = notifyAllState != ToggleableState.On
                        notifyEnabled = turnOn
                        lockScreenNotify = turnOn
                        notifyDetail = turnOn
                    },
                )
                SettingsCard {
                    SwitchWithDividerPreference(
                        title = "允许通知",
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                        startAction = { SettingIcon(COUIIcons.Messages, IconBlue) },
                        checked = notifyEnabled,
                        onCheckedChange = { notifyEnabled = it },
                    )
                    SettingsDivider()
                    CheckBoxWithDividerPreference(
                        title = "锁屏显示通知",
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                        startAction = { SettingIcon(COUIIcons.Lock, IconOrange) },
                        checked = lockScreenNotify,
                        onCheckedChange = { lockScreenNotify = it },
                    )
                    SettingsDivider()
                    MarkWithDividerPreference(
                        title = "显示通知内容",
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                        startAction = { SettingIcon(COUIIcons.Notes, IconTeal) },
                        checked = notifyDetail,
                        onClick = { notifyDetail = true },
                    )
                    SettingsDivider()
                    MarkWithDividerPreference(
                        title = "隐藏通知内容",
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                        startAction = { SettingIcon(COUIIcons.Hide, IconRed) },
                        checked = !notifyDetail,
                        onClick = { notifyDetail = false },
                    )
                }
            }

            item(key = "floatingBar") {
                SmallTitle(text = "外观与壁纸")
                SettingsCard {
                    SwitchPreference(
                        title = "展开熔球",
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                        startAction = { SettingIcon(COUIIcons.Merge, IconPurple) },
                        checked = meltBallExpanded,
                        onCheckedChange = onMeltBallExpandedChange,
                    )
                    SettingsDivider()
                    ArrowPreference(
                        title = "壁纸",
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                        endActions = {
                            InlineValue(
                                when (wallpaperSource) {
                                    WallpaperSource.System -> "系统壁纸"
                                    WallpaperSource.Lock -> "锁屏壁纸"
                                    WallpaperSource.Bundled -> "内置图片"
                                },
                            )
                        },
                        startAction = { SettingIcon(COUIIcons.Image, IconTeal) },
                        onClick = {
                            notify(scope, snackbarHostState, "光感材质取此图折射")
                        },
                    )
                }
            }

            item(key = "about") {
                SmallTitle(text = "关于本机")
                AppInfoPreference(
                    appName = "COUI 17",
                    appVersion = "V17.0.0 · ColorOS 17 组件演示",
                    icon = appIcon,
                    onVersionLongClick = { notify(scope, snackbarHostState, "版本号已复制") },
                )
                SettingsCard {
                    EditTextPreference(
                        title = "设备名称",
                        value = deviceName,
                        onValueChange = { deviceName = it },
                        confirmButtonText = "确定",
                        cancelButtonText = "取消",
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                        dialogTitle = "设备名称",
                        hint = "最多 24 个字符",
                    )
                    SettingsDivider()
                    ArrowPreference(
                        title = "型号",
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                        endActions = { InlineValue("OnePlus PLK110") },
                        startAction = { SettingIcon(COUIIcons.Info, IconBlue) },
                        onClick = { showAboutDialog = true },
                    )
                    SettingsDivider()
                    LoadInstallProgressPreference(
                        title = "系统更新",
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                        summary = "已是最新版本 V17.0.0",
                        startAction = { SettingIcon(COUIIcons.UploadCloud, IconTeal) },
                        state = systemUpdateState,
                        progress = systemUpdateProgress,
                        progressText = if (systemUpdateState == LoadProgressState.Loading) "暂停" else "检查更新",
                        onClick = {
                            if (systemUpdateState == LoadProgressState.Loading) {
                                systemUpdateState = LoadProgressState.Default
                                systemUpdateProgress = 0
                            } else {
                                systemUpdateProgress = 0
                                systemUpdateState = LoadProgressState.Loading
                                scope.launch {
                                    repeat(5) {
                                        delay(160)
                                        systemUpdateProgress += 20
                                    }
                                    if (systemUpdateState == LoadProgressState.Loading) {
                                        systemUpdateState = LoadProgressState.Default
                                        systemUpdateProgress = 0
                                        notify(scope, snackbarHostState, "已是最新版本")
                                    }
                                }
                            }
                        },
                    )
                    SettingsDivider()
                    ArrowPreference(
                        title = "ColorOS 版本",
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                        endActions = { InlineValue("V17.0.0") },
                        startAction = { SettingIcon(COUIIcons.Info, IconGreen) },
                        onClick = { showAboutDialog = true },
                    )
                    SettingsDivider()
                    SpannablePreference(
                        title = "法律信息",
                        summary = buildAnnotatedString {
                            append("《")
                            withLink(
                                LinkAnnotation.Clickable(tag = "privacy") {
                                    notify(scope, snackbarHostState, "打开《隐私政策》")
                                },
                            ) {
                                append("隐私政策")
                            }
                            append("》与《")
                            withLink(
                                LinkAnnotation.Clickable(tag = "terms") {
                                    notify(scope, snackbarHostState, "打开《用户协议》")
                                },
                            ) {
                                append("用户协议")
                            }
                            append("》")
                        },
                        modifier = Modifier.heightIn(min = HomepageRowMinHeight),
                    )
                }
                PagerFooterPreference(
                    summary = AnnotatedString("本页仅用于 COUI 17 组件演示，所列数据均为示例。"),
                )
            }

            item(key = "actions") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(onClick = { showOptimizeResult = true }) {
                        Text(text = "立即优化")
                    }
                    TextButton(
                        text = "恢复默认",
                        onClick = {
                            wifi = true
                            bluetooth = true
                            hotspot = false
                            brightness = 0.72f
                            notify(scope, snackbarHostState, "已恢复默认设置")
                        },
                    )
                }
            }

            // `COUIBottomPreference` draws nothing; it is the invisible 32dp the device keeps between
            // the last card and the end of the list.
            item(key = "bottom") {
                BottomPreference()
            }
        }

        // The device's completion page replaces the page content and carries no button; it stays for as
        // long as raw/complete_page_success_anim.json runs, then the caller dismisses it.
        if (showOptimizeResult) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(COUITheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                CompleteState(
                    status = CompleteStateStatus.Success,
                    title = "已优化 12 项设置",
                    subtitle = "系统状态已更新",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        LaunchedEffect(showOptimizeResult) {
            if (showOptimizeResult) {
                delay(CompleteStateDefaults.SuccessDuration)
                showOptimizeResult = false
            }
        }

        LaunchedEffect(Unit) {
            delay(1200)
            batteryLoading = false
        }
    }

    OverlayBottomSheet(
        show = showRingtoneSheet,
        title = "铃声",
        onDismissRequest = { showRingtoneSheet = false },
    ) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            ringtones.forEachIndexed { index, ringtone ->
                RadioButtonPreference(
                    title = ringtone,
                    selected = ringtoneIndex == index,
                    onClick = {
                        ringtoneIndex = index
                        showRingtoneSheet = false
                    },
                )
            }
        }
    }

    OverlayDialog(
        show = showAboutDialog,
        title = "关于本机",
        summary = "COUI 17 · ColorOS 17 组件演示",
        onDismissRequest = { showAboutDialog = false },
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .height(72.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(progress = 0.82f)
            }
            InfoLine("型号", "OnePlus PLK110")
            InfoLine("系统版本", "ColorOS V17.0.0")
            InfoLine("Android 版本", "17 (API 37)")
            InfoLine("应用版本", "COUI 17")
        }
    }
}

/** A ColorOS settings row leading icon: 24dp glyph, tinted, spaced off the title. */
@Composable
private fun SettingIcon(icon: ImageVector, tint: Color) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(24.dp),
    )
}

/** The value ColorOS prints at the end of a settings row, ahead of the switch or chevron. */
@Composable
private fun InlineValue(text: String) {
    Text(
        text = text,
        style = COUITheme.textStyles.footnote1,
        color = COUITheme.colorScheme.onSurfaceSecondary,
        maxLines = 1,
        modifier = Modifier.padding(end = 4.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp),
        content = content,
    )
}

/**
 * The demo app's own launcher icon, drawn as the "about" header's 60dp squircle.
 *
 * `PackageManager.getApplicationIcon` is the only lookup that survives an adaptive icon: on API 26+
 * `@mipmap/ic_launcher` resolves to an XML `AdaptiveIconDrawable`, which `BitmapFactory` cannot decode
 * and whose intrinsic size is -1. Bounds are therefore pinned to a fixed square instead of trusting
 * a size the drawable does not report.
 */
@Composable
private fun rememberAppIcon(): Painter? {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            val drawable = context.packageManager.getApplicationIcon(context.packageName)
            val size = 120
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(Canvas(bitmap))
            BitmapPainter(bitmap.asImageBitmap())
        }.getOrNull()
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, color = COUITheme.colorScheme.onSurfaceVariantSummary)
        Text(text = value)
    }
}

private fun notify(scope: CoroutineScope, state: SnackbarHostState, message: String) {
    scope.launch { state.showSnackbar(message) }
}
