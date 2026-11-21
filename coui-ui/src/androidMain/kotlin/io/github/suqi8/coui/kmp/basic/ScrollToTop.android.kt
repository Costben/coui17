// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo

private const val ACTION_OPLUS_CLICK_TOP = "com.oplus.clicktop"
private const val ACTION_COLOR_CLICK_TOP = "com.color.clicktop"

@SuppressLint("UnspecifiedRegisterReceiverFlag")
@Composable
internal actual fun PlatformStatusBarScrollToTopEffect(controller: ScrollToTopController) {
    val context = LocalContext.current
    val isWindowFocused = LocalWindowInfo.current.isWindowFocused
    DisposableEffect(context, controller, isWindowFocused) {
        if (!isWindowFocused) {
            return@DisposableEffect onDispose {}
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val action = intent?.action ?: return
                if (action == ACTION_OPLUS_CLICK_TOP || action == ACTION_COLOR_CLICK_TOP) {
                    controller.triggerScrollToTop()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(ACTION_OPLUS_CLICK_TOP)
            addAction(ACTION_COLOR_CLICK_TOP)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }
}
