// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.consumer

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.Button
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.blur.BlurDefaults
import io.github.suqi8.coui.kmp.blur.blur
import io.github.suqi8.coui.kmp.blur.drawBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.blur.textureBlur
import io.github.suqi8.coui.kmp.preference.SwitchPreference
import io.github.suqi8.coui.kmp.shader.isRenderEffectSupported
import io.github.suqi8.coui.kmp.shader.isRuntimeShaderSupported
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * The smallest realistic consumer: it declares minSdk 23, uses coui-ui and coui-preference, and
 * *actually calls* the public blur modifiers coui-blur exposes.
 *
 * The blur path is expected to be a silent no-op below API 33: `drawBackdrop` / `textureBlur` set
 * their internal `enabled` flag from `isRuntimeShaderSupported()`, so on an API 23 device the
 * backdrop node degrades to a plain content draw and `onDrawSurface` never fires. The markers below
 * make that observable from `adb logcat` on either side of the gate:
 *
 *  - `PASS blur-layout`          always, at layout completion, and it reports the gate readings.
 *  - `PASS blur-shader-executed` only when the runtime-shader path actually drew (API 33+).
 *  - `PASS blur-toggle`          whenever the user flips the button's blur state.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(
            TAG,
            "sdk=${Build.VERSION.SDK_INT} runtimeShaderSupported=${isRuntimeShaderSupported()} " +
                "renderEffectSupported=${isRenderEffectSupported()} blurDefaultRadius=${BlurDefaults.BlurRadius}",
        )
        setContent {
            COUITheme {
                ConsumerScreen()
            }
        }
    }
}

private const val TAG = "CouiApi23Consumer"

@Composable
private fun ConsumerScreen() {
    var checked by remember { mutableStateOf(false) }
    var blurEnabled by remember { mutableStateOf(true) }
    var shaderExecuted by remember { mutableStateOf(false) }
    var layoutLogged by remember { mutableStateOf(false) }
    val backdrop = rememberLayerBackdrop()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "COUI on API ${Build.VERSION.SDK_INT}: " +
                "runtimeShader=${isRuntimeShaderSupported()}, renderEffect=${isRenderEffectSupported()}",
        )
        SwitchPreference(
            checked = checked,
            onCheckedChange = { checked = it },
            title = "Preference module is on the classpath",
        )
        Button(
            onClick = {
                blurEnabled = !blurEnabled
                Log.i(TAG, "PASS blur-toggle enabled=$blurEnabled sdk=${Build.VERSION.SDK_INT}")
            },
        ) {
            Text(if (blurEnabled) "Blur: on" else "Blur: off")
        }

        // Captured backdrop: this box's pixels are what the surface below samples.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .layerBackdrop(backdrop),
        ) {
            Text("backdrop source (layerBackdrop)")
        }

        // Foreground surface: the runtime-shader path. Its `onDrawSurface` callback only runs when
        // the effect is enabled, so its absence on API 23 is the degradation evidence.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(16.dp) },
                    effects = { blur(radiusX = 8f) },
                    enabled = blurEnabled,
                    onDrawSurface = {
                        if (!shaderExecuted) {
                            shaderExecuted = true
                            Log.i(TAG, "PASS blur-shader-executed sdk=${Build.VERSION.SDK_INT}")
                        }
                    },
                )
                .onGloballyPositioned {
                    if (!layoutLogged) {
                        layoutLogged = true
                        val supported = isRuntimeShaderSupported()
                        Log.i(
                            TAG,
                            "PASS blur-layout sdk=${Build.VERSION.SDK_INT} " +
                                "runtimeShaderSupported=$supported " +
                                "renderEffectSupported=${isRenderEffectSupported()} " +
                                "blurModifiers=drawBackdrop,textureBlur " +
                                "expectedPath=${if (supported) "shader" else "degraded"}",
                        )
                    }
                },
        ) {
            Text("blurred surface (drawBackdrop)")
        }

        // The second public blur modifier, on its own small surface: it has to link and draw as well.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .textureBlur(
                    backdrop = backdrop,
                    shape = RoundedCornerShape(12.dp),
                    enabled = blurEnabled,
                ),
        ) {
            Text("blurred surface (textureBlur)")
        }
    }
}
