// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0
package io.github.suqi8.coui.kmp.kit.topappbar

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.kit.effect.CouiFloatingRenderer
import io.github.suqi8.coui.kmp.kit.effect.CouiFloatingSurface
import io.github.suqi8.coui.kmp.kit.effect.SpotlightPreset
import io.github.suqi8.coui.kmp.kit.effect.couiFloatingSurface
import io.github.suqi8.coui.kmp.kit.effect.couiToolbarFeedback
import io.github.suqi8.coui.kmp.kit.effect.rememberCouiSpotlight
import io.github.suqi8.coui.kmp.kit.motion.couiPressMotion
import io.github.suqi8.coui.kmp.kit.motion.couiTouchMotion
import io.github.suqi8.coui.kmp.kit.motion.rememberCouiPressMotion
import io.github.suqi8.coui.kmp.kit.motion.rememberCouiTouchMotion
import io.github.suqi8.coui.kmp.kit.theme.Coui
import io.github.suqi8.coui.kmp.kit.theme.ProvideCouiContent

@Composable
internal fun TopAppBarButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    text: Boolean = false,
    enforceMinimumTouchTarget: Boolean = true,
    renderer: CouiFloatingRenderer? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        remember { CouiFloatingRenderer.standalone() }
    } else {
        null
    },
    style: CouiToolbarStyle = CouiTopAppBarButtonDefaults.style(),
    content: @Composable RowScope.() -> Unit,
) {
    val backdrop = LocalCouiToolbarBackdrop.current
    val dark = style.dark
    val contour = style.contourGlowEnabled
    var boundsSize by remember { mutableStateOf(IntSize.Zero) }
    val capsule = boundsSize.width != boundsSize.height
    val shape = if (capsule) style.capsuleShape else style.circleShape
    val material = remember(style, capsule) { style.material(capsule) }
    val shadow = remember(style, capsule) { style.shadow(capsule) }
    val contentLayer = rememberGraphicsLayer()
    val motion = rememberCouiTouchMotion(enabled)
    val press = rememberCouiPressMotion()
    val interactionSource = remember { MutableInteractionSource() }
    val (lightInput, drawLight) = rememberCouiSpotlight(
        interactive = enabled,
        preset = SpotlightPreset.OpaqueSmall2,
        dark = dark,
    )
    val contentColor = style.contentColor

    val isNativeSurfaceSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        renderer != null && backdrop != null

    Box(
        modifier
            .then(if (enforceMinimumTouchTarget) Coui.defaultStyle.minimumInteractiveComponentSize() else Modifier)
            .then(if (text) Modifier.widthIn(min = 60.dp).height(40.dp) else Modifier.size(40.dp))
            .onSizeChanged { boundsSize = it }
            .drawWithCache {
                val lightClip = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
                onDrawWithContent {
                    drawContent()
                    // 原厂直接混合已绘制的按钮表面, 不能先混入透明内容层再 SrcOver。
                    if (drawLight != null) {
                        withTransform({
                            translate(motion.translationX.value, motion.translationY.value)
                            scale(motion.scaleX.value + press.scale.value - 1f, motion.scaleY.value + press.scale.value - 1f, center)
                        }) {
                            clipPath(lightClip) { drawLight.invoke(this) }
                        }
                    }
                }
            }
            .then(
                if (isNativeSurfaceSupported) {
                    Modifier.couiFloatingSurface(
                        backdrop = backdrop,
                        renderer = renderer,
                        material = material,
                        shadow = shadow,
                        shape = shape,
                        contentLayer = contentLayer,
                        contourGlowEnabled = contour,
                        foregroundComposition = true,
                    ) {
                        CouiFloatingSurface(
                            bounds = Rect(
                                Offset(motion.translationX.value, motion.translationY.value),
                                Size(boundsSize.width.toFloat(), boundsSize.height.toFloat()),
                            ),
                            scaleX = motion.scaleX.value + press.scale.value - 1f,
                            scaleY = motion.scaleY.value + press.scale.value - 1f,
                            cornerWeight = if (capsule) 1f else 2f,
                        )
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        Row(
            Modifier
                .then(if (text) Modifier.widthIn(min = 60.dp).height(40.dp) else Modifier.fillMaxSize())
                .graphicsLayer {
                    translationX = motion.translationX.value
                    translationY = motion.translationY.value
                    scaleX = motion.scaleX.value + press.scale.value - 1f
                    scaleY = motion.scaleY.value + press.scale.value - 1f
                }
                .couiTouchMotion(motion, enabled)
                .couiPressMotion(press, enabled)
                .then(lightInput)
                .drawWithContent {
                    if (isNativeSurfaceSupported) {
                        contentLayer.record { this@drawWithContent.drawContent() }
                    } else {
                        drawContent()
                    }
                }
                .clip(shape)
                .couiToolbarFeedback(dark, interactionSource, enabled)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .then(if (text) Modifier.padding(horizontal = 12.dp) else Modifier),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProvideCouiContent(
                color = if (enabled) contentColor else contentColor.copy(alpha = contentColor.alpha * 0.38f),
                textStyle = style.textStyle,
            ) { content() }
        }
    }
}
