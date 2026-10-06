// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform") version "2.4.10"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
    id("org.jetbrains.compose") version "1.11.1"
}

// A deliberately separate build that consumes COUI the way a downstream Apple/web project does,
// from a Maven repository rather than from the in-repo project dependencies. It answers the
// question the library's own build cannot: do the published iosArm64 / iosSimulatorArm64 /
// macosArm64 / js / wasmJs artifacts actually resolve, compile and link for a consumer?
//
//   ../../gradlew -p verification/apple-web-consumer \
//       -PcouiVersion=<candidate> -PcouiRepo=file:///path/to/repository/local \
//       compileKotlinIosArm64 compileKotlinIosSimulatorArm64 compileKotlinMacosArm64 \
//       compileKotlinJs compileKotlinWasmJs \
//       linkDebugFrameworkIosArm64 linkDebugFrameworkMacosArm64
//
// Every module is declared on `commonMain`, so a missing or incompatible platform artifact for any
// one of them fails the compile and link of every target.

val couiVersion: String = providers.gradleProperty("couiVersion").getOrElse("1.2.0")

val couiModules = listOf(
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

kotlin {
    jvmToolchain(21)

    iosArm64 {
        binaries.framework {
            baseName = "CouiAppleWebConsumer"
        }
    }
    iosSimulatorArm64 {
        binaries.framework {
            baseName = "CouiAppleWebConsumer"
        }
    }
    macosArm64 {
        binaries.framework {
            baseName = "CouiAppleWebConsumer"
        }
    }

    js(IR) {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            couiModules.forEach { module ->
                implementation("io.github.suqi8.coui.kmp:$module:$couiVersion")
            }
        }
    }
}
