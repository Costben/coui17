// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

internal actual object PlatformSearchTransliterator {
    actual fun normalizeBasic(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            val mapped = when (ch) {
                '\u3000' -> ' '
                in '\uFF01'..'\uFF5E' -> (ch.code - 0xFEE0).toChar()
                else -> ch
            }
            sb.append(mapped.lowercaseChar())
        }
        return sb.toString()
    }

    actual fun toSimplified(text: String): String = text

    actual fun hanToPinyin(ch: Char): String = ""
}

internal actual fun <K : Any, V : Any> createSearchCache(): MutableMap<K, V> = HashMap()
