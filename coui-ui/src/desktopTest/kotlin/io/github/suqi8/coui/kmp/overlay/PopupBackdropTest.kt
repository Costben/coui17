// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.overlay

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import io.github.suqi8.coui.kmp.blur.Backdrop
import io.github.suqi8.coui.kmp.blur.LocalBackdrop
import kotlin.test.Test
import kotlin.test.assertSame

/**
 * A menu must sample the layer the *screen* is recorded into, not the layer the page it was opened
 * from records: the page's own layer can carry the wallpaper, and a white page then frosts into the
 * wallpaper's colours. [PopupBackdrop] prefers whatever the popup host already publishes and uses the
 * call site's only as a fallback.
 */
@OptIn(ExperimentalTestApi::class)
class PopupBackdropTest {

    private val host: Backdrop = BackdropSink("host")
    private val callSite: Backdrop = BackdropSink("call-site")

    @Test
    fun theHostBackdropWinsOverTheCallSite() = runSkikoComposeUiTest(size = androidx.compose.ui.geometry.Size(400f, 200f), density = androidx.compose.ui.unit.Density(1f)) {
        var seen: Backdrop? = null
        setContent {
            // The popup composes where its host publishes: that value is current, and the layer
            // the opening page handed in is only the fallback.
            CompositionLocalProvider(LocalBackdrop provides host) {
                PopupBackdrop(hostBackdrop = callSite) {
                    seen = LocalBackdrop.current
                }
            }
        }
        waitForIdle()
        assertSame(host, seen)
    }

    @Test
    fun theCallSiteBackdropIsTheFallbackWhenTheHostPublishesNone() = runSkikoComposeUiTest(size = androidx.compose.ui.geometry.Size(400f, 200f), density = androidx.compose.ui.unit.Density(1f)) {
        var seen: Backdrop? = null
        setContent {
            PopupBackdrop(hostBackdrop = callSite) {
                seen = LocalBackdrop.current
            }
        }
        waitForIdle()
        assertSame(callSite, seen)
    }
}

/** A [Backdrop] that only carries an identity, so the assertions read as "which source". */
private class BackdropSink(private val label: String) : Backdrop {
    override val isCoordinatesDependent: Boolean = false

    override fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBackdrop(
        density: androidx.compose.ui.unit.Density,
        coordinates: androidx.compose.ui.layout.LayoutCoordinates?,
        layerBlock: (androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit)?,
        downscaleFactor: Int,
    ) = Unit

    override fun toString(): String = label
}
