// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.viewinterop.UIKitViewController
import androidx.compose.ui.window.ComposeUIViewController
import coui17.Coui17App
import interop.NativeHostBackend
import platform.UIKit.UIView
import platform.UIKit.UIViewController

@OptIn(ExperimentalComposeUiApi::class)
@Suppress("unused")
fun main(): UIViewController = ComposeUIViewController(
    configure = {
        parallelRendering = true
    },
) {
    Coui17App()
}

/**
 * Hosts [Coui17App] with a Swift-provided [NativeHostBackend] and UIKit view / view-controller
 * factories rendered through [UIKitView] and [UIKitViewController].
 */
@OptIn(ExperimentalComposeUiApi::class)
@Suppress("unused")
fun mainWithNativeHost(
    backend: NativeHostBackend,
    uiViewFactory: () -> UIView,
    uiViewControllerFactory: () -> UIViewController,
): UIViewController = ComposeUIViewController(
    configure = {
        parallelRendering = true
    },
) {
    Coui17App(
        nativeHostBackend = backend,
        nativeViewSlot = { modifier ->
            UIKitView(
                factory = uiViewFactory,
                modifier = modifier,
            )
        },
        nativeViewControllerSlot = { modifier ->
            UIKitViewController(
                factory = uiViewControllerFactory,
                modifier = modifier,
            )
        },
    )
}
