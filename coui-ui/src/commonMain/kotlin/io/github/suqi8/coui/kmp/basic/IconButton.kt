// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.interfaces.HoldDownObserver
import io.github.suqi8.coui.kmp.squircle.squircleSurface

/**
 * A [IconButton] component with COUI style.
 *
 * Icon buttons help people take supplementary actions with a single tap. They’re used when a
 * compact button is required, such as in a toolbar or image list.
 *
 * @param onClick The callback when the [IconButton] is clicked.
 * @param modifier The modifier to be applied to the [IconButton]
 * @param enabled Whether the [IconButton] is enabled.
 * @param holdDownState Used to determine whether it is in the pressed state.
 * @param backgroundColor The background color of of the [IconButton].
 * @param cornerRadius The corner radius of of the [IconButton].
 * @param minHeight The minimum height of of the [IconButton].
 * @param minWidth The minimum width of the [IconButton].
 * @param content The content of this icon button, typically an [Icon].
 */
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    holdDownState: Boolean = false,
    backgroundColor: Color = Color.Unspecified,
    cornerRadius: Dp = IconButtonDefaults.CornerRadius,
    minHeight: Dp = IconButtonDefaults.MinHeight,
    minWidth: Dp = IconButtonDefaults.MinWidth,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    @Suppress("NAME_SHADOWING")
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }

    HoldDownObserver(holdDownState, interactionSource)

    val currentOnClick by rememberUpdatedState(onClick)
    val clickableModifier = remember(enabled, interactionSource) {
        if (enabled) {
            Modifier.clickable(
                role = Role.Button,
                interactionSource = interactionSource,
                onClick = { currentOnClick() },
            )
        } else {
            Modifier
        }
    }

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = minWidth, minHeight = minHeight)
            // COUIIconButton inherits COUIButton's COUITouchMotionAnimator, which shrinks the
            // surface while pressed (setPressScaleEnable) and springs back on release.
            .couiTouchMotion(
                enabled = enabled,
                pressScaleEnabled = true,
                lightEnabled = false,
                consumeDrag = false,
            )
            .squircleSurface(color = backgroundColor, cornerRadius = cornerRadius)
            .then(clickableModifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.padding(IconButtonDefaults.IconPadding)) {
            content()
        }
    }
}

/** Binary-compatibility bridge for the [IconButton] overload that predates `interactionSource`. */
@Suppress("DEPRECATION")
@Deprecated(
    message = "Migrated to the overload that adds an interactionSource.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    holdDownState: Boolean = false,
    backgroundColor: Color = Color.Unspecified,
    cornerRadius: Dp = IconButtonDefaults.CornerRadius,
    minHeight: Dp = IconButtonDefaults.MinHeight,
    minWidth: Dp = IconButtonDefaults.MinWidth,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        holdDownState = holdDownState,
        backgroundColor = backgroundColor,
        cornerRadius = cornerRadius,
        minHeight = minHeight,
        minWidth = minWidth,
        interactionSource = null,
        content = content,
    )
}

object IconButtonDefaults {

    /**
     * The padding around the icon (COUIIconButton `iconPadding` / `iconPaddingLeft` /
     * `iconPaddingRight` / `iconPaddingTop` / `iconPaddingBottom`, all DEF_ICON_PADDING = 8dp).
     */
    val IconPadding = 8.dp

    /** The default minimum width of the [IconButton]. */
    val MinWidth = 48.dp

    /** The default minimum height of the [IconButton]; see [MinWidth]. */
    val MinHeight = 48.dp

    /**
     * The default corner radius of the [IconButton]. COUIIconButton has no radius of its own and
     * inherits COUIButton's `mRadius`, whose default is 21dp.
     */
    val CornerRadius = 21.dp
}
