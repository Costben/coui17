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

    companion object {
        val Chinese = AppStrings(zhStrings)
        val English = AppStrings(emptyMap())
    }
}

/**
 * The copy for [source] in the language the app is currently set to.
 *
 * Every screen reads this instead of holding a language of its own, so switching language in the
 * settings page recomposes the whole app and nothing keeps a stale string.
 */
@Composable
fun str(source: String): String = LocalAppStrings.current[source]

val LocalAppStrings = compositionLocalOf { AppStrings.English }

/**
 * Whether the host draws a language entry in the settings page.
 *
 * The ColorOS 17 gallery opts in. The coui16 demo leaves it off, so its own screens keep the copy
 * they have always had.
 */
val LocalLanguageSwitchEnabled = staticCompositionLocalOf { false }
