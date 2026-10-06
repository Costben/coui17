// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0

plugins {
    // AGP 9 ships built-in Kotlin support, so `org.jetbrains.kotlin.android` must NOT be applied:
    // AGP fails the build outright when it is present.
    id("com.android.application") version "9.3.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
}

// An ordinary consumer app: it declares minSdk 23 and depends on the published COUI artifacts.
// There is deliberately no `tools:overrideLibrary` anywhere in this project — the whole point is to
// prove that a plain consumer can merge the library manifests.
val couiVersion: String = providers.gradleProperty("couiVersion").getOrElse("1.2.0")

android {
    namespace = "io.github.suqi8.coui.consumer"
    buildToolsVersion = "37.0.0"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }
    defaultConfig {
        applicationId = "io.github.suqi8.coui.consumer"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
    lint {
        abortOnError = true
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation("io.github.suqi8.coui.kmp:coui-ui:$couiVersion")
    implementation("io.github.suqi8.coui.kmp:coui-preference:$couiVersion")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("org.jetbrains.compose.runtime:runtime:1.11.1")
}
