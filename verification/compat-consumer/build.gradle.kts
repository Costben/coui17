// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.10"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
    // Supplies `compose.desktop.currentOs` below; keep in step with `composeVersion`.
    id("org.jetbrains.compose") version "1.11.1"
}

// A deliberately separate build that consumes COUI the way a downstream project does, from a Maven
// repository rather than from the in-repo project dependencies. It answers four questions the
// library's own build cannot:
//
//  1. does 1.1.0-era source still compile against the candidate release?             (:compileCandidateKotlin)
//  2. does every descriptor the 1.1.0 binary relied on still resolve?          (runLegacyBinaryOnCandidate)
//  3. does bytecode compiled against 1.1.0 still link and run against the
//     candidate, i.e. is the release binary compatible?                       (runLegacyBinaryOnCandidate)
//  4. do the restored constructors and defaults still carry the same values?   (runLegacyBinaryOnCandidate)
//
// `main` is compiled against `legacyVersion`; the `candidate` source set compiles the very same
// sources against `couiVersion`. A single shared source tree is what makes the two channels
// comparable: neither can drift from the other.
//
//   ../../gradlew -p verification/compat-consumer \
//       -PcouiVersion=<candidate> -PcouiRepo=file:///path/to/repository/local \
//       compileCandidateKotlin runLegacyBinaryOnCandidate

val legacyVersion: String = providers.gradleProperty("legacyVersion").getOrElse("1.1.0")
val couiVersion: String = providers.gradleProperty("couiVersion").getOrElse("1.2.0")
val composeVersion = "1.11.1"

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

    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

sourceSets {
    // `main` is compiled against the *previous* release. Running its output against the candidate
    // is the binary-compatibility channel.
    create("candidate") {
        kotlin.srcDir("src/main/kotlin")
        resources.srcDir("src/main/resources")
    }
}

dependencies {
    implementation("org.jetbrains.compose.runtime:runtime:$composeVersion")
    implementation("org.jetbrains.compose.foundation:foundation:$composeVersion")
    implementation("org.jetbrains.compose.ui:ui:$composeVersion")
    implementation("org.jetbrains.compose.ui:ui-graphics:$composeVersion")
    implementation("org.jetbrains.compose.ui:ui-test:$composeVersion")
    implementation(compose.desktop.currentOs)

    couiModules.forEach { module ->
        implementation("io.github.suqi8.coui.kmp:$module:$legacyVersion")
    }

    "candidateImplementation"("org.jetbrains.compose.runtime:runtime:$composeVersion")
    "candidateImplementation"("org.jetbrains.compose.foundation:foundation:$composeVersion")
    "candidateImplementation"("org.jetbrains.compose.ui:ui:$composeVersion")
    "candidateImplementation"("org.jetbrains.compose.ui:ui-graphics:$composeVersion")
    "candidateImplementation"("org.jetbrains.compose.ui:ui-test:$composeVersion")
    "candidateImplementation"(compose.desktop.currentOs)
    couiModules.forEach { module ->
        "candidateImplementation"("io.github.suqi8.coui.kmp:$module:$couiVersion")
    }
}

// Runs the classes that were compiled against `legacyVersion` with the `couiVersion` artifacts on
// the classpath. Every call the legacy bytecode makes therefore resolves against the candidate.
tasks.register<JavaExec>("runLegacyBinaryOnCandidate") {
    group = "verification"
    description = "Runs the legacy-compiled consumer against the candidate artifacts"
    dependsOn("classes")
    val candidateRuntime = configurations.named("candidateRuntimeClasspath")
    classpath = sourceSets["main"].output + candidateRuntime.get()
    mainClass.set("io.github.suqi8.coui.compat.LegacyConsumerKt")
    // The Compose test harness renders offscreen, so the AWT side can stay headless and this task
    // also runs on a CI machine with no display.
    systemProperty("java.awt.headless", "true")
    args(couiVersion, legacyVersion)
}
