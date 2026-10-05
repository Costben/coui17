// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("unused")

package io.github.suqi8.smoke

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.suqi8.coui.kmp.basic.Button
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.ChevronBackward
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * References at least one public symbol from each published COUI module so that
 * compiling this build proves the whole coordinate set is resolvable and usable
 * from a plain Maven repository.
 */
object CouiSmoke {
    val moduleSymbols: List<Class<*>> = listOf(
        io.github.suqi8.coui.kmp.utils.Platform::class.java, // coui-core
        io.github.suqi8.coui.kmp.squircle.SquircleDefaults::class.java, // coui-squircle
        io.github.suqi8.coui.kmp.shader.RuntimeShader::class.java, // coui-shader
        io.github.suqi8.coui.kmp.blur.Backdrop::class.java, // coui-blur
        io.github.suqi8.coui.kmp.nav.core.NavKey::class.java, // coui-nav
        io.github.suqi8.coui.kmp.preference.RecommendedPreferenceDefaults::class.java, // coui-preference
        androidx.navigation3.scene.SceneStrategy::class.java, // coui-navigation3-ui
    )

    val extendedIcon: ImageVector = COUIIcons.ChevronBackward // coui-icons

    @Composable
    fun content() { // coui-ui
        COUITheme {
            Column {
                Button(onClick = {}) {
                    Text("COUI")
                }
            }
        }
    }
}
