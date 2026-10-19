// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

/**
 * This test class generates a baseline profile for the target package.
 *
 * It covers app startup and critical user journeys: scrolling the gallery, the two
 * capsule tabs, and the two panel entries. Labels are matched in English and
 * Chinese so the run works whichever language the device is set to.
 *
 * Run the generator with:
 * ```
 * ./gradlew :example:android:generateReleaseBaselineProfile
 * ```
 *
 * After you run the generator, you can verify the improvements running the
 * [StartupBenchmarks] benchmark.
 *
 * When using this class to generate a baseline profile, only API 33+ or rooted
 * API 28+ are supported.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    private val packageName: String
        get() = InstrumentationRegistry.getArguments().getString("targetAppId")
            ?: throw Exception("targetAppId not passed as instrumentation runner arg")

    @Test
    fun startup() {
        rule.collect(
            packageName = packageName,
            includeInStartupProfile = true,
        ) {
            pressHome()
            startActivityAndWait()
            device.wait(Until.hasObject(By.text("COUI")), 2_000)
        }
    }

    @Test
    fun interactions() {
        rule.collect(
            packageName = packageName,
            includeInStartupProfile = false,
        ) {
            pressHome()
            startActivityAndWait()
            device.wait(Until.hasObject(By.text("COUI")), 5_000)

            scroll(4)

            openTab("Icons", "图标")
            scroll(3)

            // The colour tab hides both accessory balls, so the panel entries are opened before it.
            openPanelEntry("Text style", "文字样式")
            scroll(3)
            click("Back", "返回")

            openPanelEntry("Settings items", "设置项")
            scroll(3)
            click("Back", "返回")

            openTab("Color", "配色")
            scroll(3)
        }
    }
}

private fun MacrobenchmarkScope.scroll(times: Int) {
    repeat(times) {
        try {
            device.findObject(By.scrollable(true))?.fling(Direction.DOWN)
        } catch (_: StaleObjectException) {
            // The list replaced the node while the fling was starting; the next pass picks up
            // the fresh one.
        }
        device.waitForIdle()
    }
}

private fun MacrobenchmarkScope.openTab(label: String, localized: String) {
    val pattern = labels(label, localized)
    clickNode("\"$label\" / \"$localized\"") {
        // The tab label also appears in the gallery content, so take the bottom-most match.
        (device.findObjects(By.desc(pattern)) + device.findObjects(By.text(pattern)))
            .maxByOrNull { it.visibleBounds.centerY() }
    }
}

private fun MacrobenchmarkScope.openPanelEntry(label: String, localized: String) {
    click("More", "更多")
    click(label, localized)
}

private fun MacrobenchmarkScope.click(label: String, localized: String) {
    val pattern = labels(label, localized)
    clickNode("\"$label\" / \"$localized\"") {
        device.wait(Until.findObject(By.desc(pattern)), 5_000)
            ?: device.wait(Until.findObject(By.text(pattern)), 5_000)
    }
}

private fun MacrobenchmarkScope.clickNode(what: String, find: () -> UiObject2?) {
    repeat(3) {
        val node = find() ?: error("no node matching $what")
        try {
            node.click()
            settle()
            return
        } catch (_: StaleObjectException) {
            // The page replaced the node between lookup and click; look it up again.
        }
    }
    error("could not click $what")
}

/**
 * Waits for the page transition to finish.
 *
 * The gallery's pages live in a pager, and the bar drops its panel state when the pager settles.
 * Opening the panel while the return animation is still running gets it dismissed again, so give
 * the animation a moment past the accessibility idle signal.
 */
private fun MacrobenchmarkScope.settle() {
    device.waitForIdle()
    Thread.sleep(600)
}

private fun labels(label: String, localized: String): Pattern = Pattern.compile("${Pattern.quote(label)}|${Pattern.quote(localized)}")
