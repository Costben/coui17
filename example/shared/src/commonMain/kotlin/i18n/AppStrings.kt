// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The gallery's copy, resolved for one language.
 *
 * A key is the English source string a component is written with, so call sites read
 * `str("Cancel")` and the English table is the identity: a language only carries the entries it
 * actually translates, and anything it leaves out falls back to the source rather than to a blank.
 */
class AppStrings internal constructor(private val table: Map<String, String>) {

    /** The copy for [source] in this language, or [source] itself when there is no entry. */
    operator fun get(source: String): String = table[source] ?: source

    /**
     * The copy for [source] with every `{}` replaced by the matching argument, in order.
     *
     * A demo that reads out a live value — a switch's state, a slider's reading — still has an
     * English sentence around that value, so the sentence has to be translatable while the value
     * stays whatever it is. The placeholders are positional rather than named because a language
     * is free to move them: `Played {}%` can become `已播放 {}%` without the call site caring.
     */
    fun format(source: String, args: List<Any?>): String {
        val template = this[source]
        if (args.isEmpty()) return template
        val out = StringBuilder(template.length + 16)
        var cursor = 0
        var next = 0
        while (true) {
            val at = template.indexOf("{}", cursor)
            if (at < 0) break
            out.append(template, cursor, at)
            out.append(if (next < args.size) args[next].toString() else "{}")
            next++
            cursor = at + 2
        }
        out.append(template, cursor, template.length)
        return out.toString()
    }

    companion object {
        val Chinese = AppStrings(zhStrings)
        val English = AppStrings(emptyMap())
    }
}

/**
 * The copy for [source] in the language the app is currently set to.
 *
 * Every screen reads this instead of holding a language of its own, so switching language in the
 * settings page recomposes the whole app and nothing keeps a stale string. Pass [args] when the
 * sentence carries a live value, e.g. `str("State: {}", checked)`.
 */
@Composable
fun str(source: String, vararg args: Any?): String = LocalAppStrings.current.format(source, args.toList())

val LocalAppStrings = compositionLocalOf { AppStrings.English }

/**
 * Whether the host draws a language entry in the settings page.
 *
 * The ColorOS 17 gallery opts in. The coui16 demo leaves it off, so its own screens keep the copy
 * they have always had.
 */
val LocalLanguageSwitchEnabled = staticCompositionLocalOf { false }
