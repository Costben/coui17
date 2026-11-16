// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package interop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Host platform service contract implemented natively (for example in Swift on iOS) and injected
 * into Compose Multiplatform via [LocalNativeHostBackend].
 */
interface NativeHostBackend {
    /** Human-readable name and OS description of the host platform. */
    val hostPlatformName: String

    /** Returns a host-generated greeting for [topic]. */
    fun fetchHostGreeting(topic: String): String

    /** Executes [action] on the host platform and returns a status message. */
    fun dispatchHostAction(action: String): String
}

/**
 * Default KMP fallback [NativeHostBackend] used on targets or previews that do not inject a
 * platform-specific implementation.
 */
object DefaultHostBackend : NativeHostBackend {
    override val hostPlatformName: String = "KMP Common Fallback"

    override fun fetchHostGreeting(topic: String): String {
        val resolved = topic.trim().ifEmpty { "COUI" }
        return "Common fallback greeting for \"$resolved\" (no native host injected)."
    }

    override fun dispatchHostAction(action: String): String {
        val resolved = action.trim().ifEmpty { "ping" }
        return "Common fallback handled action \"$resolved\"."
    }
}

/** Provides the active [NativeHostBackend] implementation to the Compose tree. */
val LocalNativeHostBackend = staticCompositionLocalOf<NativeHostBackend> { DefaultHostBackend }

/**
 * Optional slot that renders an embedded native `UIView` when supplied by an iOS host.
 * Defaults to `null` so non-iOS targets render a graceful placeholder.
 */
val LocalNativeViewSlot = staticCompositionLocalOf<(@Composable (Modifier) -> Unit)?> { null }

/**
 * Optional slot that renders an embedded native `UIViewController` when supplied by an iOS host.
 * Defaults to `null` so non-iOS targets render a graceful placeholder.
 */
val LocalNativeViewControllerSlot = staticCompositionLocalOf<(@Composable (Modifier) -> Unit)?> { null }
