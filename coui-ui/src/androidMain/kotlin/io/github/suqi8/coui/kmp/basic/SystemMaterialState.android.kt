// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

/** `COUIMaterialStateManager.MATERIAL_BLUR`, a `Settings.System` int. */
private const val MATERIAL_BLUR = "system_material_blur_enable"

/** `COUIMaterialStateManager.SUPER_POWER_SAVE_MODE`, a `Settings.System` int. */
private const val SUPER_POWER_SAVE_MODE = "super_powersave_mode_state"

/** `COUIMaterialStateManager.LOW_POWER_MODE`, a `Settings.Global` int. */
private const val LOW_POWER_MODE = "low_power"

/** `COUIMaterialStateManager.DISABLE_WINDOW_BLURS`, a `Settings.Global` int. */
private const val DISABLE_WINDOW_BLURS = "disable_window_blurs"

private var observerInstalled = false

@Composable
internal actual fun ObserveSystemMaterialState() {
    val context = LocalContext.current.applicationContext
    LaunchedEffect(context) {
        if (observerInstalled) return@LaunchedEffect
        observerInstalled = true
        SystemMaterialState.blurEnabled = readBlurEnabled(context)
        // The settings are process-wide, so the observer lives as long as the process does.
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                SystemMaterialState.blurEnabled = readBlurEnabled(context)
            }
        }
        val resolver = context.contentResolver
        resolver.registerContentObserver(Settings.System.getUriFor(MATERIAL_BLUR), false, observer)
        resolver.registerContentObserver(Settings.System.getUriFor(SUPER_POWER_SAVE_MODE), false, observer)
        resolver.registerContentObserver(Settings.Global.getUriFor(LOW_POWER_MODE), false, observer)
        resolver.registerContentObserver(Settings.Global.getUriFor(DISABLE_WINDOW_BLURS), false, observer)
    }
}

/**
 * `COUIMaterialStateManager.updateBlurStates`: blur needs the material switch on and neither
 * power-saving mode active. A ROM without the switch (anything but ColorOS) has no setting at all,
 * which reads as "allowed" here so the effects keep working off ColorOS.
 */
private fun readBlurEnabled(context: Context): Boolean {
    val resolver = context.contentResolver
    val switch = Settings.System.getString(resolver, MATERIAL_BLUR)
    val switchOn = switch == null || switch.trim() == "1"
    val lowPower = Settings.Global.getInt(resolver, LOW_POWER_MODE, 0) == 1
    val superPowerSave = Settings.System.getInt(resolver, SUPER_POWER_SAVE_MODE, 0) == 1
    val windowBlursDisabled = Settings.Global.getInt(resolver, DISABLE_WINDOW_BLURS, 0) == 1
    return switchOn && !lowPower && !superPowerSave && !windowBlursDisabled
}
