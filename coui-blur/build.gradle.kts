// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinMultiplatform)
    id("module.kotlin-jvm-toolchain")
    id("module.publication")
    id("module.spotless")
}

couiPublication {
    description.set("Blur effect library for COUI")
}

kotlin {
    withSourcesJar(true)

    android {
        buildToolsVersion = BuildConfig.BUILD_TOOLS_VERSION
        compileSdk {
            version =
                release(BuildConfig.COMPILE_SDK) {
                    minorApiLevel = BuildConfig.COMPILE_SDK_MINOR
                }
        }
        // Every RenderEffect/RuntimeShader call in this module sits behind the runtime gate in
        // `isRuntimeShaderSupported()`, so the module is safe to depend on from an API 23 app: the
        // blur simply does not render. Declaring 33 here would force every consumer of coui-ui
        // (which exposes this module through `api`) to accept a manifest-merge failure or paper over
        // it with `tools:overrideLibrary`.
        minSdk = BuildConfig.MIN_SDK
        namespace = "${BuildConfig.LIBRARY_ID}.blur"
    }

    jvm("desktop")

    iosArm64()
    iosSimulatorArm64()
    macosArm64()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    js {
        browser()
    }

    applyCOUISourceSetHierarchy()

    sourceSets {
        commonMain.dependencies {
            api(projects.couiShader)
            implementation(libs.jetbrains.compose.foundation)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        val desktopTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.jetbrains.compose.ui.test)
            }
        }
    }
}
