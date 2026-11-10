// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0

import com.diffplug.spotless.LineEnding

plugins {
    id("com.diffplug.spotless")
}

val composeKtlintRules = "io.nlopez.compose.rules:ktlint:0.6.3"
val copyrightFile = rootProject.file("spotless/copyright.txt")

// Sources ported from COUI-Kit (ItosEO/COUI-Kit) stay under MPL-2.0 and live in `kit/` packages.
val mplCopyrightFile = rootProject.file("spotless/copyright-mpl.txt")
val mplSources = "src/**/kit/**/*.kt"
val copyrightDelimiter = "^(?![ \\t]*(?:\\/\\/|\\/\\*)).*[\\w].*$"

spotless {
    lineEndings = LineEnding.PLATFORM_NATIVE

    kotlin {
        target("src/**/*.kt")
        targetExclude(
            "**/build/**/*.kt",
            "**/icon/**/*.kt",
            "**/navigation3/ListUtils.kt",
            "**/navigation3/scene/*.kt",
            "**/navigation3/ui/*.kt",
            mplSources,
        )
        ktlint()
            .customRuleSets(listOf(composeKtlintRules))
            .editorConfigOverride(
                mapOf(
                    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                    "ktlint_compose_modifier-missing-check" to "disabled",
                    "ktlint_compose_compositionlocal-allowlist" to "disabled",
                    "ktlint_compose_mutable-state-param-check" to "disabled",
                    "ktlint_compose_parameter-naming" to "disabled",
                    "ktlint_compose_modifier-naming" to "disabled",
                    "ktlint_compose_param-order-check" to "disabled",
                ),
            )
        licenseHeaderFile(copyrightFile, copyrightDelimiter)
    }

    format("kotlinMpl", com.diffplug.gradle.spotless.KotlinExtension::class.java) {
        target(mplSources)
        targetExclude("**/build/**/*.kt")
        ktlint()
            .customRuleSets(listOf(composeKtlintRules))
            .editorConfigOverride(
                mapOf(
                    "ktlint_function_naming_ignore_when_annotated_with" to "Composable",
                    "ktlint_compose_modifier-missing-check" to "disabled",
                    "ktlint_compose_compositionlocal-allowlist" to "disabled",
                    "ktlint_compose_mutable-state-param-check" to "disabled",
                    "ktlint_compose_parameter-naming" to "disabled",
                    "ktlint_compose_modifier-naming" to "disabled",
                    "ktlint_compose_param-order-check" to "disabled",
                    "ktlint_standard_property-naming" to "disabled",
                    "ktlint_standard_filename" to "disabled",
                    "ktlint_compose_content-slot-reused" to "disabled",
                    "ktlint_compose_lambda-param-in-effect" to "disabled",
                ),
            )
        licenseHeaderFile(mplCopyrightFile, copyrightDelimiter)
    }

    kotlinGradle {
        target("*.kts")
        targetExclude("**/build/**/*.kts")
        ktlint()
        licenseHeaderFile(copyrightFile, copyrightDelimiter)
    }
}
