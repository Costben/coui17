// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import io.github.suqi8.coui.kmp.theme.COUITheme
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `COUINotificationSnackBar.onTouchEvent` decides the swipe axis on the first move and dismisses
 * once the drag passes `getMeasuredWidth() / 8`; both axes use the width, and the Y axis only
 * counts a downward drag. These drive real touch input through the ported gesture.
 */
@OptIn(ExperimentalTestApi::class)
class NotificationSnackBarSwipeTest {

    private var dismissed = 0

    @Test
    fun aHorizontalSwipePastAnEighthDismisses() = swipeThenCheck(dx = -120f, dy = 0f) {
        assertEquals(1, dismissed)
    }

    @Test
    fun aHorizontalSwipeShortOfAnEighthSpringsBack() = swipeThenCheck(dx = -24f, dy = 0f) {
        assertEquals(0, dismissed)
    }

    @Test
    fun anUpwardSwipeNeverDismisses() = swipeThenCheck(dx = 0f, dy = -160f) {
        assertEquals(0, dismissed)
    }

    private fun swipeThenCheck(dx: Float, dy: Float, check: () -> Unit) {
        dismissed = 0
        runSkikoComposeUiTest(size = Size(400f, 400f), density = Density(1f)) {
            setContent {
                COUITheme {
                    Box(Modifier.fillMaxSize()) {
                        NotificationSnackBar(
                            title = "Notification",
                            modifier = Modifier.testTag(SNACK_TAG),
                            onClose = {},
                            onSwipedDismiss = { dismissed += 1 },
                        )
                    }
                }
            }
            waitForIdle()
            onNodeWithTag(SNACK_TAG).performTouchInput {
                down(center)
                moveBy(Offset(dx / 4f, dy / 4f))
                moveBy(Offset(dx / 4f, dy / 4f))
                moveBy(Offset(dx / 4f, dy / 4f))
                moveBy(Offset(dx / 4f, dy / 4f))
                up()
            }
            waitForIdle()
            // The dismissal slide runs before the callback; let its tween finish.
            mainClock.advanceTimeBy(2_000)
            waitForIdle()
        }
        check()
    }
}

private const val SNACK_TAG = "notification-snack-bar"
