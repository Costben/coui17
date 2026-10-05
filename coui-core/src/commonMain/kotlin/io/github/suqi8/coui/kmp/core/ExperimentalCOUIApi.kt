// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.core

/**
 * Marks COUI APIs that are not yet stable as experimental.
 *
 * The annotated API is usable, but its surface is not frozen: it may change, or be removed, in a
 * later release. Callers opt in explicitly so the instability stays visible at the call site.
 *
 * Kotlin rejects opt-in markers on value parameters ("Opt-in requirement marker annotation cannot be
 * used on parameter"), so a parameter added to an already-stable composable — a new argument that
 * cannot carry its own marker — is called out in its KDoc `@param` text instead.
 */
@Suppress("ExperimentalAnnotationRetention")
@RequiresOptIn(
    message = "This COUI API is experimental. It may change without notice.",
    level = RequiresOptIn.Level.WARNING,
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
)
annotation class ExperimentalCOUIApi
