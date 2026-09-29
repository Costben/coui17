// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    id("module.kotlin-jvm-toolchain")
    id("module.spotless")
}

dependencies {
    implementation(projects.couiUi)
    implementation(projects.couiPreference)
    implementation(projects.couiIcons)
    implementation(libs.androidx.activity)
}

@Suppress("UnstableApiUsage")
android {
    buildToolsVersion = BuildConfig.BUILD_TOOLS_VERSION
    compileSdk {
        version =
            release(BuildConfig.COMPILE_SDK) {
                minorApiLevel = BuildConfig.COMPILE_SDK_MINOR
            }
    }
    defaultConfig {
        applicationId = "io.github.suqi8.coui17"
        minSdk = BuildConfig.MIN_SDK
        targetSdk = BuildConfig.TARGET_SDK
        versionName = BuildConfig.APPLICATION_VERSION_NAME
        versionCode = getGitVersionCode()
    }
    namespace = "io.github.suqi8.coui17"
    buildTypes {
        release {
            optimization.enable = true
            vcsInfo.include = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    packaging {
        jniLibs {
            excludes += "lib/*/libandroidx.graphics.path.so"
        }
    }
}

base {
    archivesName.set("COUI17-v${BuildConfig.APPLICATION_VERSION_NAME}(${getGitVersionCode()})")
}
