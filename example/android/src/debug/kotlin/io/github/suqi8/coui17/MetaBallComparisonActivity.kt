// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui17

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.MetaBallParticle
import io.github.suqi8.coui.kmp.basic.MetaBallSurface

/** Debug-only, physical-pixel fixture shared with the device's native View reference probe. */
class MetaBallComparisonActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ballX = intent.getIntExtra("ball_x", 786)
        val ballY = intent.getIntExtra("ball_y", 500)
        val scaleX = intent.getFloatExtra("scale_x", 1f)
        val scaleY = intent.getFloatExtra("scale_y", 1f)
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(Modifier.fillMaxSize().background(Color.Green)) {
                    MetaBallSurface(
                        particles = listOf(
                            MetaBallParticle(480.dp, 590.dp, 600.dp, 180.dp),
                            MetaBallParticle.circle((ballX + 90).dp, (ballY + 90).dp, 180.dp).copy(scaleX = scaleX, scaleY = scaleY),
                        ),
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Blue,
                        backdrop = null,
                        rimColor = null,
                        extension = 14.dp,
                    )
                }
            }
        }
    }
}
