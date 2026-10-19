// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
plugins {
    id("module.spotless")
    id("module.central-release")
    alias(libs.plugins.binaryCompatibilityValidator)
}

// Only the nine published libraries are under API compatibility guard; the sample
// apps, the docs tooling and the baseline-profile generator are not part of the
// public surface and would otherwise drown the dump in demo noise.
apiValidation {
    ignoredProjects.addAll(
        listOf("baselineprofile", "shared", "android", "desktop", "web", "macos", "demo", "iconGen"),
    )
}

// `apiCheck` reads the dump files that `apiDump` rewrites. Requesting both in one invocation leaves
// Gradle unable to order them and it fails validation, so declare the ordering explicitly.
subprojects {
    tasks.matching { it.name.endsWith("ApiCheck") }.configureEach {
        mustRunAfter(tasks.matching { it.name.endsWith("ApiDump") })
    }
}
