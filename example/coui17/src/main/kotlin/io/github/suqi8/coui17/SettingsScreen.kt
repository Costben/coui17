// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui17

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.BasicComponent
import io.github.suqi8.coui.kmp.basic.Button
import io.github.suqi8.coui.kmp.basic.Card
import io.github.suqi8.coui.kmp.basic.CircularProgressIndicator
import io.github.suqi8.coui.kmp.basic.HorizontalDivider
import io.github.suqi8.coui.kmp.basic.Icon
import io.github.suqi8.coui.kmp.basic.InputField
import io.github.suqi8.coui.kmp.basic.LinearProgressIndicator
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
import io.github.suqi8.coui.kmp.preference.ArrowPreference
import io.github.suqi8.coui.kmp.preference.ButtonPreference
import io.github.suqi8.coui.kmp.preference.CheckboxPreference
import io.github.suqi8.coui.kmp.preference.ListPreference
import io.github.suqi8.coui.kmp.preference.ListPreferenceEntry
import io.github.suqi8.coui.kmp.preference.RadioButtonPreference
import io.github.suqi8.coui.kmp.preference.SliderPreference
import io.github.suqi8.coui.kmp.preference.SwitchPreference
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.utils.overScrollVertical
import io.github.suqi8.coui.kmp.utils.scrollEndHaptic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val IconBlue = Color(0xFF3B82F6)
private val IconGreen = Color(0xFF22C55E)
private val IconOrange = Color(0xFFF59E0B)
private val IconPurple = Color(0xFF8B5CF6)
private val IconRed = Color(0xFFEF4444)
private val IconTeal = Color(0xFF14B8A6)

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

    var wifi by remember { mutableStateOf(true) }
    var bluetooth by remember { mutableStateOf(true) }
    var hotspot by remember { mutableStateOf(false) }
    var nfc by remember { mutableStateOf(false) }

    var brightness by remember { mutableFloatStateOf(0.72f) }
    var autoBrightness by remember { mutableStateOf(true) }
    var colorModeIndex by remember { mutableIntStateOf(0) }
    var refreshIndex by remember { mutableIntStateOf(1) }
    var showRefreshSheet by remember { mutableStateOf(false) }

    var mediaVolume by remember { mutableFloatStateOf(0.45f) }
    var silent by remember { mutableStateOf(false) }
    var ringtoneIndex by remember { mutableIntStateOf(0) }
    var showRingtoneSheet by remember { mutableStateOf(false) }

    var powerSaving by remember { mutableStateOf(false) }
    var notifyEnabled by remember { mutableStateOf(true) }
    var lockScreenNotify by remember { mutableStateOf(true) }
    var notifyDetail by remember { mutableStateOf(true) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val colorModes = remember { listOf("自然", "鲜艳", "柔和") }
    val refreshRates = remember { listOf("60Hz", "120Hz", "智能切换") }
    val ringtones = remember { listOf("清晨", "潮汐", "静谧", "无") }
    val suggestions = remember {
        listOf("WLAN 与网络", "蓝牙", "显示与亮度", "声音与振动", "电池", "通知与状态栏")
    }

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
                        modifier = Modifier.padding(end = 16.dp),
                    )
                },
                expanded = searchExpanded,
                onExpandedChange = { searchExpanded = it },
            ) {
                val visible = suggestions
                    .filter { query.isEmpty() || it.contains(query, ignoreCase = true) }
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

        item(key = "connection") {
            SmallTitle(text = "连接与共享")
            SettingsCard {
                SwitchPreference(
                    title = "WLAN",
                    summary = "COUI-Lab-5G",
                    startAction = { SettingIcon(COUIIcons.Community, IconBlue) },
                    checked = wifi,
                    onCheckedChange = { wifi = it },
                )
                SettingsDivider()
                SwitchPreference(
                    title = "蓝牙",
                    summary = "已连接 2 台设备",
                    startAction = { SettingIcon(COUIIcons.Link, IconBlue) },
                    checked = bluetooth,
                    onCheckedChange = { bluetooth = it },
                )
                SettingsDivider()
                ArrowPreference(
                    title = "移动网络",
                    summary = "5G · 中国移动",
                    startAction = { SettingIcon(COUIIcons.Carrier, IconGreen) },
                    onClick = { notify(scope, snackbarHostState, "移动网络设置暂不可用") },
                )
                SettingsDivider()
                SwitchPreference(
                    title = "个人热点",
                    startAction = { SettingIcon(COUIIcons.Share, IconOrange) },
                    checked = hotspot,
                    onCheckedChange = { hotspot = it },
                )
                SettingsDivider()
                SwitchPreference(
                    title = "NFC",
                    summary = "读卡、刷卡与数据传输",
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
                    title = "亮度",
                    valueText = "${(brightness * 100).toInt()}%",
                    startAction = { SettingIcon(COUIIcons.Theme, IconOrange) },
                    showKeyPoints = true,
                )
                SettingsDivider()
                SwitchPreference(
                    title = "自动调节亮度",
                    startAction = { SettingIcon(COUIIcons.Show, IconOrange) },
                    checked = autoBrightness,
                    onCheckedChange = { autoBrightness = it },
                )
                SettingsDivider()
                SwitchPreference(
                    title = "深色模式",
                    summary = "跟随开关即时生效",
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
                    dialogTitle = "色彩模式",
                    cancelButtonText = "取消",
                    startAction = { SettingIcon(COUIIcons.Album, IconTeal) },
                    showValue = true,
                )
                SettingsDivider()
                ArrowPreference(
                    title = "屏幕刷新率",
                    summary = refreshRates[refreshIndex],
                    startAction = { SettingIcon(COUIIcons.Update, IconGreen) },
                    onClick = { showRefreshSheet = true },
                )
            }
        }

        item(key = "sound") {
            SmallTitle(text = "声音与振动")
            SettingsCard {
                SliderPreference(
                    value = mediaVolume,
                    onValueChange = { mediaVolume = it },
                    title = "媒体音量",
                    valueText = "${(mediaVolume * 100).toInt()}%",
                    startAction = { SettingIcon(COUIIcons.VolumeUp, IconBlue) },
                )
                SettingsDivider()
                SwitchPreference(
                    title = "静音",
                    summary = "来电与通知不响铃",
                    startAction = { SettingIcon(COUIIcons.VolumeOff, IconRed) },
                    checked = silent,
                    onCheckedChange = { silent = it },
                )
                SettingsDivider()
                ArrowPreference(
                    title = "铃声",
                    summary = ringtones[ringtoneIndex],
                    startAction = { SettingIcon(COUIIcons.Music, IconPurple) },
                    onClick = { showRingtoneSheet = true },
                )
            }
        }

        item(key = "battery") {
            SmallTitle(text = "电池")
            SettingsCard {
                BasicComponent(
                    title = "电池电量",
                    summary = "约 9 小时 12 分可用",
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
                    summary = "限制后台活动以延长续航",
                    startAction = { SettingIcon(COUIIcons.Update, IconGreen) },
                    checked = powerSaving,
                    onCheckedChange = { powerSaving = it },
                )
            }
        }

        item(key = "notification") {
            SmallTitle(text = "通知与状态栏")
            SettingsCard {
                SwitchPreference(
                    title = "允许通知",
                    startAction = { SettingIcon(COUIIcons.Messages, IconBlue) },
                    checked = notifyEnabled,
                    onCheckedChange = { notifyEnabled = it },
                )
                SettingsDivider()
                CheckboxPreference(
                    title = "锁屏显示通知",
                    startAction = { SettingIcon(COUIIcons.Lock, IconOrange) },
                    checked = lockScreenNotify,
                    onCheckedChange = { lockScreenNotify = it },
                )
                SettingsDivider()
                RadioButtonPreference(
                    title = "显示通知内容",
                    summary = "仅在解锁后显示完整内容",
                    startAction = { SettingIcon(COUIIcons.Notes, IconTeal) },
                    selected = notifyDetail,
                    onClick = { notifyDetail = true },
                )
                SettingsDivider()
                RadioButtonPreference(
                    title = "隐藏通知内容",
                    startAction = { SettingIcon(COUIIcons.Hide, IconRed) },
                    selected = !notifyDetail,
                    onClick = { notifyDetail = false },
                )
            }
        }

        item(key = "floatingBar") {
            SmallTitle(text = "外观与壁纸")
            SettingsCard {
                SwitchPreference(
                    title = "展开熔球",
                    summary = "底部悬浮栏旁的圆形按钮从胶囊里熔出",
                    startAction = { SettingIcon(COUIIcons.Merge, IconPurple) },
                    checked = meltBallExpanded,
                    onCheckedChange = onMeltBallExpandedChange,
                )
                SettingsDivider()
                ArrowPreference(
                    title = "壁纸",
                    summary = when (wallpaperSource) {
                        WallpaperSource.System -> "系统壁纸"
                        WallpaperSource.Lock -> "锁屏壁纸"
                        WallpaperSource.Bundled -> "内置图片 · 系统为动态壁纸"
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
            SettingsCard {
                ArrowPreference(
                    title = "型号",
                    summary = "OnePlus PLK110",
                    startAction = { SettingIcon(COUIIcons.Info, IconBlue) },
                    onClick = { showAboutDialog = true },
                )
                SettingsDivider()
                ButtonPreference(
                    title = "系统更新",
                    buttonText = "检查",
                    startAction = { SettingIcon(COUIIcons.UploadCloud, IconTeal) },
                    onButtonClick = { notify(scope, snackbarHostState, "已是最新版本") },
                )
                SettingsDivider()
                ArrowPreference(
                    title = "ColorOS 版本",
                    summary = "V17.0.0",
                    startAction = { SettingIcon(COUIIcons.Info, IconGreen) },
                    onClick = { showAboutDialog = true },
                )
            }
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
                Button(onClick = { notify(scope, snackbarHostState, "已优化 12 项设置") }) {
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
    }

    OverlayBottomSheet(
        show = showRefreshSheet,
        title = "屏幕刷新率",
        onDismissRequest = { showRefreshSheet = false },
    ) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            refreshRates.forEachIndexed { index, rate ->
                RadioButtonPreference(
                    title = rate,
                    summary = when (rate) {
                        "120Hz" -> "更流畅，耗电略高"
                        "智能切换" -> "根据内容自动调节"
                        else -> "更省电"
                    },
                    selected = refreshIndex == index,
                    onClick = {
                        refreshIndex = index
                        showRefreshSheet = false
                    },
                )
            }
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
