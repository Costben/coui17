// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0

@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

val couiGroup = "io.github.suqi8.coui.kmp"
val couiRepo = providers.gradleProperty("couiRepo").orNull
val couiVersion = providers.gradleProperty("couiVersion").orNull

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        // The candidate version of the COUI group is served by the candidate repository; the 1.1.0
        // legacy release stays on Maven Central. The routing is a per-repository content filter, not
        // `exclusiveContent`: `exclusiveContent` claims the whole group for the candidate repository
        // once any of its filters matches, which also stripped 1.1.0 from Central. Claiming only the
        // candidate version (`group + module + version`) routes the candidate to the file repository
        // while 1.1.0 still resolves from Central.
        if (couiVersion != null) {
            maven {
                name = "couiCandidate"
                url = uri(couiRepo ?: "https://repo1.maven.org/maven2")
                content {
                    includeVersionByRegex(
                        couiGroup.replace(".", "\\."),
                        ".*",
                        Regex.escape(couiVersion),
                    )
                }
            }
        }
        mavenCentral()
    }
}

rootProject.name = "coui-compat-consumer"
