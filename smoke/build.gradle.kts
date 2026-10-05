// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.10"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
}

// This is a deliberately separate build: it must resolve COUI the way a real
// downstream consumer does, i.e. from a Maven repository rather than from the
// in-repo project dependencies. Run it with:
//
//   ./gradlew -p smoke compileKotlin -PcouiVersion=<version> \
//       -PsmokeRepo=<file:///path/to/repository/local>
val couiVersion: String = providers.gradleProperty("couiVersion").getOrElse("1.1.0")

val publishedModules = listOf(
    "coui-core",
    "coui-ui",
    "coui-preference",
    "coui-shader",
    "coui-blur",
    "coui-squircle",
    "coui-icons",
    "coui-navigation3-ui",
    "coui-nav",
)

val composeVersion = "1.11.1"

kotlin {
    jvmToolchain(21)

    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    implementation("org.jetbrains.compose.runtime:runtime:$composeVersion")
    implementation("org.jetbrains.compose.foundation:foundation:$composeVersion")
    implementation("org.jetbrains.compose.ui:ui:$composeVersion")
    implementation("org.jetbrains.compose.ui:ui-graphics:$composeVersion")

    publishedModules.forEach { module ->
        implementation("io.github.suqi8.coui.kmp:$module:$couiVersion")
    }
}
