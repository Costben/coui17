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

val couiGroup = "io.github.costben.coui17"
val couiRepo = providers.gradleProperty("couiRepo").orNull

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        // The candidate fork coordinates (`io.github.costben.coui17`) are served exclusively by the
        // candidate repository so Maven Central can never satisfy them by accident, while the
        // upstream 1.1.0 legacy coordinates (`io.github.suqi8.coui.kmp`) keep resolving from Central.
        exclusiveContent {
            forRepository {
                maven {
                    name = "couiCandidate"
                    url = uri(couiRepo ?: "https://repo1.maven.org/maven2")
                }
            }
            filter { includeGroup(couiGroup) }
        }
        mavenCentral()
    }
}

rootProject.name = "coui-compat-consumer"
