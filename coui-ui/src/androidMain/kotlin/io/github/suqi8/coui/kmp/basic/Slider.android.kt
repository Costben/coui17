// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import io.github.suqi8.coui.kmp.kit.shape.CouiCapsuleShape
import io.github.suqi8.coui.kmp.kit.slider.CouiKitSlider
import io.github.suqi8.coui.kmp.kit.slider.CouiSliderColors
import io.github.suqi8.coui.kmp.theme.COUITheme

@Composable
internal actual fun CouiPlatformSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)?,
    enabled: Boolean,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    reverseDirection: Boolean,
    height: Dp,
    colors: SliderColors,
    hapticEffect: SliderDefaults.SliderHapticEffect,
    showKeyPoints: Boolean,
    keyPoints: List<Float>?,
    magnetThreshold: Float,
    secondaryProgress: Float?,
    showProgress: Boolean,
    showThumb: Boolean,
    startFromMiddle: Boolean,
    moveByFinger: Boolean,
    modifier: Modifier,
) {
    val dark = COUITheme.colorScheme.background.luminance() < 0.5f
    val kitColors = CouiSliderColors(
        activeTrack = colors.foregroundColor(enabled),
        inactiveTrack = colors.backgroundColor(enabled),
        thumb = colors.thumbColor(enabled),
        activeTick = colors.keyPointForegroundColor(),
        inactiveTick = colors.keyPointColor(),
        thumbShadow = Color(if (dark) 0x40000000 else 0x1a000000),
        secondaryTrack = colors.secondaryProgressColor(enabled),
    )
    val hasTicks = showKeyPoints || steps > 0 || (keyPoints != null && keyPoints.isNotEmpty())
    CouiKitSlider(
        modifier = modifier,
        value = value,
        onValueChange = onValueChange,
        colors = kitColors,
        dark = dark,
        shape = CouiCapsuleShape,
        valueRange = valueRange,
        steps = steps,
        showTickMarks = hasTicks,
        enabled = enabled,
        reverseDirection = reverseDirection,
        height = height,
        hapticEffect = hapticEffect,
        showKeyPoints = showKeyPoints,
        keyPoints = keyPoints,
        magnetThreshold = magnetThreshold,
        secondaryProgress = secondaryProgress,
        showProgress = showProgress,
        showThumb = showThumb,
        startFromMiddle = startFromMiddle,
        moveByFinger = moveByFinger,
        onValueChangeFinished = onValueChangeFinished ?: {},
    )
}
