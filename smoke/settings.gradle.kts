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

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        // Points at the release candidate under test: the shared local staging
        // repository during CI, Maven Central for the post-release smoke.
        maven {
            name = "smoke"
            url = uri(providers.gradleProperty("smokeRepo").getOrElse("https://repo1.maven.org/maven2"))
        }
    }
}

rootProject.name = "coui-smoke"
