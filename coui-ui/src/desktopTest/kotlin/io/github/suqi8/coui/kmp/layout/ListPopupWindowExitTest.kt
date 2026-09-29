// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.layout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.suqi8.coui.kmp.basic.ListPopupColumn
import io.github.suqi8.coui.kmp.basic.Scaffold
import io.github.suqi8.coui.kmp.basic.Text
import io.github.suqi8.coui.kmp.overlay.OverlayListPopup
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The popup host must outlive the content exit springs.
 *
 * `COUIPopupListWindow.dismiss` posts `forceDismiss` 350ms after it starts hiding the content, and
 * the window only then plays `Animation.COUI.PopupListWindow.Dismiss` — the 200ms
 * `coui_popup_list_window_exit` alpha ([io.github.suqi8.coui.kmp.basic.ListPopupDefaults.WindowExitAnimationSpec]).
 * The window is therefore gone at roughly 350 + 200 = 550ms, not when the ~340ms close opacity
 * spring settles, which is what the manual clock below separates.
 */
@OptIn(ExperimentalTestApi::class)
class ListPopupWindowExitTest {

    @Test
    fun theHostStaysMountedPastTheContentExitSprings() = runSkikoComposeUiTest(
        size = Size(400f, 600f),
        density = Density(1f),
    ) {
        var show by mutableStateOf(true)
        var dismissFinished = false
        setContent {
            Scaffold {
                OverlayListPopup(
                    show = show,
                    onDismissRequest = { show = false },
                    onDismissFinished = { dismissFinished = true },
                ) {
                    ListPopupColumn {
                        Text("Item")
                    }
                }
            }
        }
        waitForIdle()

        mainClock.autoAdvance = false
        show = false
        // 400ms: the close opacity spring (response 0.23) has settled, so an unmount wired only to
        // the springs would already have fired here.
        mainClock.advanceTimeBy(400)
        assertFalse(
            dismissFinished,
            "the window must still be mounted while its exit fade runs",
        )

        // 900ms: content springs plus the 200ms window fade are both long over.
        mainClock.advanceTimeBy(500)
        assertTrue(
            dismissFinished,
            "the window must unmount once the exit fade has finished",
        )
    }
}
