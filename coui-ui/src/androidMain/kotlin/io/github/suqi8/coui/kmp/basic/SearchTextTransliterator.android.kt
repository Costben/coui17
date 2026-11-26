// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.basic

import android.icu.text.Transliterator
import android.os.Build
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

internal actual object PlatformSearchTransliterator {
    private val t2s: Any? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { Transliterator.getInstance("Traditional-Simplified") }.getOrNull()
        } else {
            null
        }
    }

    private val hanLatin: Any? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { Transliterator.getInstance("Han-Latin") }.getOrNull()
        } else {
            null
        }
    }

    private val charCache = ConcurrentHashMap<Char, String>()

    actual fun normalizeBasic(input: String): String {
        val nfkc = Normalizer.normalize(input, Normalizer.Form.NFKC)
        return nfkc.lowercase(Locale.ROOT)
    }

    actual fun toSimplified(text: String): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return text
        val tr = t2s as? Transliterator ?: return text
        return runCatching { tr.transliterate(text) }.getOrDefault(text)
    }

    actual fun hanToPinyin(ch: Char): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return ""
        return charCache.getOrPut(ch) {
            val tr = hanLatin as? Transliterator ?: return@getOrPut ""
            val raw = runCatching { tr.transliterate(ch.toString()) }.getOrDefault("")
            stripToneMarksKeepUmlaut(raw)
        }
    }

    private fun stripToneMarksKeepUmlaut(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (c in raw.lowercase(Locale.ROOT)) {
            when (c) {
                'ǖ', 'ǘ', 'ǚ', 'ǜ', 'ü' -> sb.append('ü')

                else -> {
                    val decomposed = Normalizer.normalize(c.toString(), Normalizer.Form.NFD)
                    for (dc in decomposed) {
                        if (dc in 'a'..'z') sb.append(dc)
                    }
                }
            }
        }
        return sb.toString()
    }
}

internal actual fun <K : Any, V : Any> createSearchCache(): MutableMap<K, V> = ConcurrentHashMap()
