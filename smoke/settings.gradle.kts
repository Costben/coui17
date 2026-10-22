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
val smokeRepo = providers.gradleProperty("smokeRepo").orNull
val smokeLocalOnly = providers.gradleProperty("smokeLocalOnly").orNull?.toBoolean() ?: false

check(!smokeLocalOnly || smokeRepo != null) {
    "smokeLocalOnly requires smokeRepo to point at the candidate repository under test"
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        val central = mavenCentral()
        if (smokeLocalOnly) {
            // Force COUI onto the candidate repository. Without this an identical version that
            // already exists on Maven Central would satisfy the dependency, and the smoke test
            // would pass without ever reading the artifacts under test.
            central.content { excludeGroup(couiGroup) }
        }
        // Points at the release candidate under test: the shared staging repository during CI,
        // Maven Central for the post-release smoke, or GitHub Packages for the snapshot channel.
        maven {
            name = "smoke"
            url = uri(smokeRepo ?: "https://repo1.maven.org/maven2")
            // GitHub Packages requires authentication even for a public package. Credentials are
            // optional so a `file:` staging repository keeps working without them.
            val actor = System.getenv("GITHUB_ACTOR")
            val token = System.getenv("GITHUB_TOKEN")
            if (!actor.isNullOrBlank() && !token.isNullOrBlank()) {
                credentials {
                    username = actor
                    password = token
                }
            }
        }
    }
}

rootProject.name = "coui-smoke"
