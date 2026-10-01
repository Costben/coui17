// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package i18n

/**
 * A language the gallery can render itself in.
 *
 * [label] is written in the language's own script, so the entry a user is hunting for reads the
 * same whichever language the app happens to be in when they open the list.
 */
enum class AppLanguage(val tag: String, val label: String) {
    Chinese("zh-CN", "中文"),
    English("en", "English"),
    ;

    /** The copy table this language resolves its source strings through. */
    val strings: AppStrings
        get() = when (this) {
            Chinese -> AppStrings.Chinese
            English -> AppStrings.English
        }
}
