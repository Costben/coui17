// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.compat

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runSkikoComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import io.github.suqi8.coui.kmp.basic.Button
import io.github.suqi8.coui.kmp.basic.ButtonDefaults
import io.github.suqi8.coui.kmp.basic.Chip
import io.github.suqi8.coui.kmp.basic.ChipDefaults
import io.github.suqi8.coui.kmp.basic.CircularProgressIndicator
import io.github.suqi8.coui.kmp.basic.ListPopupLayoutInfo
import io.github.suqi8.coui.kmp.basic.NavigationBar
import io.github.suqi8.coui.kmp.basic.PopupLayoutPosition
import io.github.suqi8.coui.kmp.basic.ProgressIndicatorColors
import io.github.suqi8.coui.kmp.basic.ProgressIndicatorDefaults
import io.github.suqi8.coui.kmp.basic.RangeSlider
import io.github.suqi8.coui.kmp.basic.Slider
import io.github.suqi8.coui.kmp.basic.SliderColors
import io.github.suqi8.coui.kmp.basic.SliderDefaults
import io.github.suqi8.coui.kmp.basic.SmallTopAppBar
import io.github.suqi8.coui.kmp.basic.TextButton
import io.github.suqi8.coui.kmp.basic.TopAppBar
import io.github.suqi8.coui.kmp.basic.TopTips
import io.github.suqi8.coui.kmp.basic.TopTipsColors
import io.github.suqi8.coui.kmp.basic.TopTipsDefaults
import io.github.suqi8.coui.kmp.basic.VerticalSlider
import io.github.suqi8.coui.kmp.layout.DialogButtonBar
import io.github.suqi8.coui.kmp.layout.DialogButtonBarAction
import io.github.suqi8.coui.kmp.overlay.OverlayCascadingListPopup
import io.github.suqi8.coui.kmp.preference.RecommendedPreference
import io.github.suqi8.coui.kmp.preference.RecommendedPreferenceColors
import io.github.suqi8.coui.kmp.preference.RecommendedPreferenceDefaults
import io.github.suqi8.coui.kmp.theme.COUITheme
import io.github.suqi8.coui.kmp.theme.darkColorScheme
import io.github.suqi8.coui.kmp.theme.lightColorScheme
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import kotlin.system.exitProcess

/**
 * An independent downstream consumer of the COUI 1.1.0 API surface, run in two channels:
 *
 *  1. `main` compiles this file against 1.1.0 and the `candidate` source set compiles the very same
 *     file against the candidate, so a build success proves the 1.1.0-era call shapes still compile.
 *  2. the classes `main` produced carry call instructions to the 1.1.0 mangled JVM names; executing
 *     them against the candidate proves the restored forwarders link and run.
 */
private const val STATUS_REQUIRED = "REQUIRED"
private const val STATUS_KNOWN_UNRESTORABLE = "KNOWN_UNRESTORABLE"
private const val LEGACY_ABI_RESOURCE = "/legacy-abi-required.tsv"

/**
 * Anchors class-loader-relative lookups to this consumer's own classes. Kotlin does not expose the
 * file facade class as a classifier, so the object stands in for the `LegacyConsumerKt` reference.
 */
private object ConsumerClasspath

fun main(args: Array<String>) {
    val candidateVersion = args.getOrElse(0) { "unspecified" }
    val legacyVersion = args.getOrElse(1) { "unspecified" }

    println("COUI compatibility consumer")
    println("  candidate: $candidateVersion")
    println("  legacy   : $legacyVersion")
    println()

    val sourceOk = checkSourceCompatibility(candidateVersion)
    val abiOk = checkRestoredDescriptors(candidateVersion)
    // Check 3 catches nothing on purpose: a missing forwarder must surface as a NoSuchMethodError.
    val sliderColors = executeLegacyBytecode()
    val valuesOk = checkRuntimeValues(sliderColors)

    println()
    if (!(sourceOk && abiOk && valuesOk)) {
        println("FAIL - the 1.1.0 consumer does not compare clean against $candidateVersion")
        exitProcess(1)
    }
    println("PASS - the 1.1.0 consumer compares clean against $candidateVersion")
}

/**
 * Check 1 - source compatibility. The whole file is the sample: written against the 1.1.0 API and
 * compiled unchanged by both source sets, it can only build if the candidate kept every call shape
 * below. There is nothing to run here.
 */
private fun checkSourceCompatibility(candidateVersion: String): Boolean {
    println("[source] PASS - 1.1.0-era call shapes compile against $candidateVersion")
    return true
}

/**
 * Check 3 - binary compatibility. [legacyApiUsage] was compiled against 1.1.0, so running it here
 * executes that release's mangled JVM names against the candidate artifacts.
 */
@OptIn(ExperimentalTestApi::class)
private fun executeLegacyBytecode(): SliderColors {
    var captured: SliderColors? = null
    runSkikoComposeUiTest(size = Size(600f, 900f), density = Density(1f)) {
        setContent {
            captured = SliderDefaults.sliderColors()
            legacyApiUsage()
        }
        waitForIdle()
    }
    val sliderColors = captured ?: error("the legacy composition produced no SliderColors")
    println("[link] PASS - legacy bytecode executed the restored composable forwarders")
    return sliderColors
}

/**
 * Every call below uses a parameter list that existed in 1.1.0, with 1.1.0 named arguments. Do not
 * pass a 1.2.0-only parameter and do not reorder anything: the point is that this is 1.1.0-shaped
 * code, and the legacy bytecode channel depends on it staying that way.
 */
@Suppress("UNUSED_VARIABLE")
@Composable
private fun legacyApiUsage() {
    Button(onClick = {}, content = {})
    TextButton(text = "Legacy", onClick = {})
    Chip(selected = false, onClick = {}, label = "Legacy")
    Slider(value = 0.5f, onValueChange = {})
    VerticalSlider(value = 0.5f, onValueChange = {})
    RangeSlider(value = 0.25f..0.75f, onValueChange = {})
    CircularProgressIndicator(progress = 0.5f)
    NavigationBar {}
    TopAppBar(title = "Legacy")
    SmallTopAppBar(title = "Legacy")
    TopTips(text = "Legacy")
    OverlayCascadingListPopup(show = false, entries = emptyList(), onDismissRequest = {})
    RecommendedPreference(title = "Legacy", items = emptyList())

    // Full positional call shapes: the parameter *order* has to survive, not just the names. Each
    // value below sits in a 1.1.0 position, so an inserted parameter anywhere in the old sequence
    // makes one of these fail to compile (in both this channel and the candidate channel).
    Button(
        {},
        Modifier,
        true,
        true,
        16.dp,
        64.dp,
        40.dp,
        ButtonDefaults.buttonColors(),
        ButtonDefaults.InsideMargin,
        null,
        null,
    ) {}
    TextButton(
        "Legacy",
        {},
        Modifier,
        true,
        true,
        16.dp,
        64.dp,
        40.dp,
        ButtonDefaults.textButtonColors(),
        ButtonDefaults.InsideMargin,
        COUITheme.textStyles.button,
        null,
        null,
    )
    Chip(
        false,
        {},
        "Legacy",
        Modifier,
        true,
        null,
        16.dp,
        ChipDefaults.MinWidth,
        ChipDefaults.MinHeight,
        ChipDefaults.MaxWidth,
        ChipDefaults.chipColors(),
        ChipDefaults.InsideMargin,
        null,
        null,
    )
    RecommendedPreference(
        "Legacy",
        emptyList(),
        Modifier,
        8.dp,
        RecommendedPreferenceDefaults.recommendedPreferenceColors(),
        RecommendedPreferenceDefaults.InsideMargin,
    )

    // Full positional shapes for the components whose 1.2.0 parameters were inserted mid-sequence.
    // Each call supplies a value of the *old* type at the position a new parameter would occupy, so
    // a visible new-only parameter list cannot satisfy it: the old overload is the only candidate.
    NavigationBar(Modifier, Color.Black, true) {}
    TopAppBar(
        "Legacy",
        Modifier,
        Color.Black,
        Color.White,
        "Large",
        Color.White,
        "",
        Color.Gray,
        Color.DarkGray,
        {},
        {},
        null,
        true,
        true,
        true,
        16.dp,
    )
    SmallTopAppBar(
        "Legacy",
        Modifier,
        Color.Black,
        Color.White,
        "",
        Color.Gray,
        Color.DarkGray,
        {},
        {},
        null,
        true,
        true,
        16.dp,
    )
    TopTips("Legacy", Modifier, true, {}, "Act", {}, {})
    DialogButtonBar(
        null,
        DialogButtonBarAction(text = "OK", onClick = {}),
        Modifier,
        null,
        true,
        true,
        true,
        ButtonDefaults.textButtonColorsBorderless(),
        Color.Red,
    )

    val sliderColors = SliderDefaults.sliderColors()
    val progressColors = ProgressIndicatorDefaults.progressIndicatorColors()
    val circularProgressColors = ProgressIndicatorDefaults.circularProgressIndicatorColors()
    val topTipsColors = TopTipsDefaults.topTipsColors()
    val recommendedColors = RecommendedPreferenceDefaults.recommendedPreferenceColors()
    val verticalPadding = TopTipsDefaults.VerticalPadding
}

/** Check 2 - every restored descriptor resolves, resolved reflectively so nothing is optimised away. */
private fun checkRestoredDescriptors(candidateVersion: String): Boolean {
    val rows = readAbiRows()
    val loader = ConsumerClasspath::class.java.classLoader ?: ClassLoader.getSystemClassLoader()

    var required = 0
    var resolved = 0
    val missing = mutableListOf<AbiRow>()
    val unexpected = mutableListOf<AbiRow>()

    rows.forEach { row ->
        val resolves = row.resolvesWith(loader)
        when (row.status) {
            STATUS_REQUIRED -> {
                required++
                if (resolves) {
                    resolved++
                } else {
                    missing += row
                }
            }

            STATUS_KNOWN_UNRESTORABLE -> if (resolves) unexpected += row
            else -> error("unknown status '${row.status}' on line ${row.lineNumber}")
        }
    }

    println("[abi] $resolved/$required required descriptors resolve against $candidateVersion")
    unexpected.forEach { row ->
        println("[abi] note - ${row.owner}.${row.method}${row.descriptor} unexpectedly resolves (${row.status})")
    }
    if (missing.isNotEmpty()) {
        missing.forEach { row -> println("[abi] missing - ${row.owner}.${row.method}${row.descriptor}") }
        println("[abi] FAIL - ${missing.size} required descriptor(s) do not resolve against $candidateVersion")
        return false
    }
    println("[abi] PASS - every required descriptor resolves against $candidateVersion")
    return true
}

/** Check 4 - real invocations of the 1.1.0 constructors and defaults, with their invariants. */
private fun checkRuntimeValues(sliderColors: SliderColors): Boolean {
    val assertions = Assertions()

    val light = lightColorScheme()
    val dark = darkColorScheme()
    assertions.that(light.primary != dark.primary) { "lightColorScheme().primary must differ from darkColorScheme().primary" }
    assertions.that(light.background != dark.background) { "lightColorScheme().background must differ from darkColorScheme().background" }
    val lightCopy = light.copy()
    assertions.that(lightCopy.primary == light.primary) { "Colors.copy() must keep primary" }
    assertions.that(lightCopy.surface == light.surface) { "Colors.copy() must keep surface" }
    assertions.that(lightCopy.sliderBackground == light.sliderBackground) { "Colors.copy() must keep sliderBackground" }
    assertions.that(lightCopy.windowDimming == light.windowDimming) { "Colors.copy() must keep windowDimming" }
    assertions.that(light.copy(primary = Color.Red).primary == Color.Red) { "Colors.copy(primary = ...) must take the new primary" }

    val topTips = TopTipsColors(
        containerColor = Color.Black,
        contentColor = Color.White,
        actionColor = Color.Red,
        closeButtonColor = Color.Green,
        closeIconColor = Color.Blue,
    )
    assertions.that(topTips.copy() == topTips) { "TopTipsColors.copy() must equal the receiver" }
    assertions.that(topTips.copy(actionColor = Color.Magenta).actionColor == Color.Magenta) {
        "TopTipsColors.copy(actionColor = ...) must take the new action color"
    }
    val titleColor = readTitleColor(topTips)
    if (titleColor == null) {
        println("[values] note - TopTipsColors.titleColor is absent, its fallback assertion was skipped")
    } else {
        assertions.that(titleColor == topTips.contentColor.value.toLong()) {
            "TopTipsColors.titleColor must fall back to contentColor when it is omitted"
        }
    }

    val position = PopupLayoutPosition(showBelow = true, showAbove = false, isRightAligned = false)
    val layoutInfo = ListPopupLayoutInfo(
        windowBounds = IntRect(0, 0, 320, 480),
        popupMargin = IntRect(0, 0, 8, 16),
        effectiveTransformOrigin = TransformOrigin.Center,
        localTransformOrigin = TransformOrigin.Center,
        popupLayoutPosition = position,
    )
    assertions.that(layoutInfo.copy() == layoutInfo) { "ListPopupLayoutInfo.copy() must equal the receiver" }
    assertions.that(layoutInfo.popupLayoutPosition == position) { "ListPopupLayoutInfo must keep the popup layout position" }
    val remargined = layoutInfo.copy(popupMargin = IntRect(1, 2, 3, 4))
    assertions.that(remargined.popupMargin == IntRect(1, 2, 3, 4)) {
        "ListPopupLayoutInfo.copy(popupMargin = ...) must take the new margin"
    }
    assertions.that(remargined != layoutInfo) { "ListPopupLayoutInfo.copy(popupMargin = ...) must differ from the receiver" }
    // 1.1.0-era source destructures the five leading properties and types the fifth as a position.
    // If a new property were inserted before it, this line would bind component5 to the wrong type.
    val (_, _, _, _, fifth) = layoutInfo
    val fifthAsPosition: PopupLayoutPosition = fifth
    assertions.that(fifthAsPosition == position) {
        "ListPopupLayoutInfo destructuring must keep component5 as PopupLayoutPosition"
    }

    val action = DialogButtonBarAction(text = "OK", enabled = true, onClick = {})
    assertions.that(action.copy() == action) { "DialogButtonBarAction.copy() must equal the receiver" }
    assertions.that(action.copy(text = "Cancel").text == "Cancel") {
        "DialogButtonBarAction.copy(text = ...) must take the new label"
    }
    assertions.that(!action.copy(enabled = false).enabled) { "DialogButtonBarAction.copy(enabled = false) must disable the action" }

    val progressColors = ProgressIndicatorColors(Color.Red, Color.Green, Color.Blue)
    assertions.that(progressColors.copy() == progressColors) { "ProgressIndicatorColors.copy() must equal the receiver" }
    val recoloredProgress = progressColors.copy(Color.Yellow)
    assertions.that(recoloredProgress != progressColors) { "ProgressIndicatorColors.copy(...) must differ from the receiver" }
    assertions.that(recoloredProgress.copy() == recoloredProgress) { "ProgressIndicatorColors.copy() must be idempotent" }

    val recommended = RecommendedPreferenceColors(Color.Black, Color.White, Color.Gray)
    assertions.that(recommended.copy() == recommended) { "RecommendedPreferenceColors.copy() must equal the receiver" }
    assertions.that(recommended.copy(titleColor = Color.Yellow).titleColor == Color.Yellow) {
        "RecommendedPreferenceColors.copy(titleColor = ...) must take the new title color"
    }

    assertions.that(sliderColors.copy() == sliderColors) { "SliderColors.copy() must equal the receiver" }
    val recoloredSlider = sliderColors.copy(Color.Magenta)
    assertions.that(recoloredSlider != sliderColors) { "SliderColors.copy(...) must differ from the receiver" }
    assertions.that(recoloredSlider.copy() == recoloredSlider) { "SliderColors.copy() must be idempotent" }

    assertions.that(TopTipsDefaults.VerticalPadding == 12.dp) { "TopTipsDefaults.VerticalPadding must stay 12.dp" }

    if (!assertions.report()) {
        println("[values] FAIL - assertions did not hold against the candidate")
        return false
    }
    println("[values] PASS - ${assertions.count} runtime assertions")
    return true
}

/**
 * Reads `titleColor` through the JVM getter instead of the property: 1.1.0 had no such property, so
 * the reference cannot appear in this source, but the candidate still has to default it.
 */
private fun readTitleColor(colors: Any): Long? {
    val getter = colors.javaClass.methods.firstOrNull { it.name.startsWith("getTitleColor") } ?: return null
    return getter.invoke(colors) as? Long
}

private class Assertions {
    private val failures = mutableListOf<String>()

    var count: Int = 0
        private set

    fun that(condition: Boolean, message: () -> String) {
        count++
        if (!condition) failures += message()
    }

    fun report(): Boolean {
        failures.forEach { failure -> println("  FAIL: $failure") }
        return failures.isEmpty()
    }
}

private data class AbiRow(
    val lineNumber: Int,
    val module: String,
    val owner: String,
    val method: String,
    val descriptor: String,
    val status: String,
)

private fun readAbiRows(): List<AbiRow> {
    val stream = ConsumerClasspath::class.java.getResourceAsStream(LEGACY_ABI_RESOURCE)
        ?: error("$LEGACY_ABI_RESOURCE is missing from the classpath")
    return stream.bufferedReader().useLines { lines ->
        lines.mapIndexedNotNull { index, line ->
            if (line.isBlank() || line.startsWith("#")) {
                null
            } else {
                val columns = line.split('\t')
                require(columns.size >= 5) { "malformed row on line ${index + 1}: $line" }
                AbiRow(
                    lineNumber = index + 1,
                    module = columns[0],
                    owner = columns[1],
                    method = columns[2],
                    descriptor = columns[3],
                    status = columns[4],
                )
            }
        }.toList()
    }
}

private fun AbiRow.resolvesWith(loader: ClassLoader): Boolean {
    val ownerClass = try {
        Class.forName(owner.replace('/', '.'), false, loader)
    } catch (_: ClassNotFoundException) {
        return false
    }
    val methodType = try {
        MethodType.fromMethodDescriptorString(descriptor, loader)
    } catch (_: RuntimeException) {
        return false
    }
    val lookup = MethodHandles.publicLookup()
    return try {
        if (method == "<init>") {
            lookup.findConstructor(ownerClass, methodType)
        } else {
            try {
                lookup.findStatic(ownerClass, method, methodType)
            } catch (_: NoSuchMethodException) {
                lookup.findVirtual(ownerClass, method, methodType)
            } catch (_: IllegalAccessException) {
                lookup.findVirtual(ownerClass, method, methodType)
            }
        }
        true
    } catch (_: ReflectiveOperationException) {
        false
    } catch (_: LinkageError) {
        false
    }
}
