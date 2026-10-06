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

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

val couiGroup = "io.github.suqi8.coui.kmp"
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
        // COUI is served exclusively by the candidate repository: `exclusiveContent` strips the
        // group from Maven Central and every other repository, so a same-version artifact on Central
        // can never satisfy the dependency and make the verification pass without reading the
        // candidate. Repository ordering is not relied on.
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

rootProject.name = "coui-apple-web-consumer"
