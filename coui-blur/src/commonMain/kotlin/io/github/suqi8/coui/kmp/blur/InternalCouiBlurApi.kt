// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
package io.github.suqi8.coui.kmp.blur

/**
 * Marks blur API that only exists so other COUI modules can coordinate with the backdrop pipeline.
 * It carries no compatibility guarantee and may change or disappear in any release.
 */
@RequiresOptIn(
    message = "This API is internal to the COUI libraries and has no compatibility guarantee.",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class InternalCouiBlurApi
