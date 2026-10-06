// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0

package io.github.suqi8.coui.consumer.appleweb

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.navigation3.ui.NavDisplayTransitionEffects
import io.github.suqi8.coui.kmp.blur.BlendColorEntry
import io.github.suqi8.coui.kmp.blur.isRuntimeShaderSupported as isBlurRuntimeShaderSupported
import io.github.suqi8.coui.kmp.icon.COUIIcons
import io.github.suqi8.coui.kmp.icon.extended.Add
import io.github.suqi8.coui.kmp.nav.core.NavController
import io.github.suqi8.coui.kmp.nav.core.NavKey
import io.github.suqi8.coui.kmp.nav.core.navBackStackOf
import io.github.suqi8.coui.kmp.preference.AppInfoPreference
import io.github.suqi8.coui.kmp.preference.AppInfoPreferenceDefaults
import io.github.suqi8.coui.kmp.shader.isRenderEffectSupported
import io.github.suqi8.coui.kmp.shader.isRuntimeShaderSupported
import io.github.suqi8.coui.kmp.squircle.SquircleDefaults
import io.github.suqi8.coui.kmp.theme.COUITheme

/**
 * Routes of the consumer's back stack. `coui-nav` places no serializability requirement on keys
 * that only ever reach [navBackStackOf].
 */
private data object Home : NavKey

private data object Details : NavKey

/**
 * The platform gates the consumer reads at start-up. Two of the three close over `coui-shader`'s
 * probes and the third over `coui-blur`'s re-export, so a missing or incompatible web/native
 * artifact for either module fails here first.
 */
object CouiConsumerCapabilities {
    /** `coui-shader`'s runtime shader gate. */
    val runtimeShaderSupported: Boolean = isRuntimeShaderSupported()

    /** `coui-shader`'s `RenderEffect` gate. */
    val renderEffectSupported: Boolean = isRenderEffectSupported()

    /** `coui-blur`'s own runtime shader gate, re-exported through its module. */
    val blurRuntimeShaderSupported: Boolean = isBlurRuntimeShaderSupported()
}

/** The icon the consumer renders, taken from `coui-icons` in COUI's regular weight. */
val consumerAddIcon: ImageVector = COUIIcons.Regular.Add

/** The corner extension `coui-squircle` draws with, read from its public defaults object. */
val consumerCornerExtension: Float = SquircleDefaults.Extension

/** The about-page header geometry `coui-preference` publishes. */
val consumerIconSize: Dp = AppInfoPreferenceDefaults.IconSize

/** The transition effects `coui-navigation3-ui` applies while the consumer swaps panes. */
val consumerTransitionEffects: NavDisplayTransitionEffects = NavDisplayTransitionEffects.Default

/**
 * Builds the consumer's navigation stack through `coui-nav`'s [NavController] and returns it with
 * the surplus entry popped, exercising the push/pop surface rather than only the constructor.
 */
fun createNavController(): NavController {
    val controller = NavController(navBackStackOf(Home, Details))
    controller.pop()
    return controller
}

/**
 * Describes the COUI surface the consumer resolved. Every one of the nine modules contributes a
 * value here, so the line can only be produced by a build that read all nine platform artifacts.
 */
fun describeConsumerSurface(): String {
    val capabilities = CouiConsumerCapabilities
    val blend = BlendColorEntry(color = Color.White)
    return buildString {
        append("icon=").append(consumerAddIcon.name)
        append(" cornerExtension=").append(consumerCornerExtension)
        append(" iconSize=").append(consumerIconSize)
        append(" runtimeShader=").append(capabilities.runtimeShaderSupported)
        append(" renderEffect=").append(capabilities.renderEffectSupported)
        append(" blurRuntimeShader=").append(capabilities.blurRuntimeShaderSupported)
        append(" blend=").append(blend.color)
        append(" transitionEffects=").append(consumerTransitionEffects)
        append(" icons=").append(COUIIcons.Regular)
    }
}

/**
 * The screen the iOS/macOS frameworks and the browser bundles present. It composes inside
 * `coui-ui`'s [COUITheme] and renders `coui-preference`'s about header off the `coui-nav` stack.
 */
@Composable
fun AppleWebConsumerScreen(modifier: Modifier = Modifier) {
    val controller = remember { createNavController() }
    COUITheme {
        Column(modifier = modifier.padding(consumerIconSize)) {
            AppInfoPreference(
                appName = "COUI Apple/web consumer",
                appVersion = "back stack depth ${controller.backStack.size}, ${COUITheme.colorScheme.onBackground}",
            )
        }
    }
}
