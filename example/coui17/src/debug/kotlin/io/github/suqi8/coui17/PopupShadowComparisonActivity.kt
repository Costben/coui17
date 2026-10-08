// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui17

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.anim.BezierGravityPathGeometry
import io.github.suqi8.coui.kmp.basic.ListPopupContent
import io.github.suqi8.coui.kmp.basic.PopupLayoutPosition
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import io.github.suqi8.coui.kmp.blur.layerBackdrop
import io.github.suqi8.coui.kmp.blur.rememberLayerBackdrop
import io.github.suqi8.coui.kmp.theme.COUITheme

/** Holds a real popup at a requested animation frame for device pixel comparisons. */
class PopupShadowComparisonActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val opacity = intent.getFloatExtra("opacity", 0.5f)
        val scale = intent.getFloatExtra("scale", 0.8f)
        val glass = intent.getBooleanExtra("glass", true)
        setContent {
            COUITheme {
                val backdrop = rememberLayerBackdrop { drawContent() }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(Modifier.fillMaxSize().layerBackdrop(backdrop).background(Color.White))
                    CompositionLocalProvider(LocalBackdrop provides if (glass) backdrop else null) {
                        ListPopupContent(
                            popupContentSize = IntSize.Zero,
                            onPopupContentSizeChange = {},
                            fractionProgress = { scale },
                            alphaProgress = { opacity },
                            pathProgress = { 0f },
                            gravityPath = BezierGravityPathGeometry(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
                            popupLayoutPosition = PopupLayoutPosition(showBelow = false, showAbove = true, isRightAligned = true),
                            localTransformOrigin = TransformOrigin.Center,
                            enableGlass = glass,
                        ) {
                            Box(Modifier.size(168.dp, 88.dp).semantics { contentDescription = "popup-shadow-probe" })
                        }
                    }
                }
            }
        }
    }
}
