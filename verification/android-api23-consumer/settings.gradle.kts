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
val consumerRepo = providers.gradleProperty("consumerRepo").orNull

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        // COUI is served exclusively by the candidate repository. `exclusiveContent` removes the
        // group from every other repository by construction, so an identically-versioned artifact
        // already published to Maven Central can never satisfy the dependency and hide a broken or
        // missing candidate — repository order is not relied on.
        exclusiveContent {
            forRepository {
                maven {
                    name = "couiCandidate"
                    url = uri(consumerRepo ?: "https://repo1.maven.org/maven2")
                }
            }
            filter { includeGroup(couiGroup) }
        }
        mavenCentral()
    }
}

rootProject.name = "coui-android-api23-consumer"
