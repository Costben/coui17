// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0

object BuildConfig {
    const val LIBRARY_VERSION = "1.2.0"
    const val LIBRARY_ID = "io.github.suqi8.coui.kmp"
    const val APPLICATION_NAME = "COUI"
    const val APPLICATION_VERSION_NAME = "1.0.9"
    const val APPLICATION_ID = "io.github.suqi8.coui17"
    const val APPLICATION_SHARED_ID = "io.github.suqi8.coui.shared"
    const val COMPILE_SDK = 37
    const val COMPILE_SDK_MINOR = 0
    const val TARGET_SDK = 37
    const val MIN_SDK = 23
    const val BUILD_TOOLS_VERSION = "37.0.0"
    const val JDK_VERSION = 21
}

fun org.gradle.api.Project.getGitVersionCode(): Int {
    return runCatching {
        providers.exec {
            workingDir(rootDir)
            commandLine("git", "rev-list", "--count", "HEAD")
        }.standardOutput.asText.get().trim().toInt()
    }.getOrElse { 1 }
}

fun org.gradle.api.Project.getGitHashShort(): String {
    return runCatching {
        providers.exec {
            workingDir(rootDir)
            commandLine("git", "rev-parse", "--short", "HEAD")
        }.standardOutput.asText.get().trim()
    }.getOrElse { "dev" }
}

/**
 * The version suffix every published module shares, resolved from the release flags.
 *
 * `-ProllingSnapshot` pins it at `-SNAPSHOT`: that is the GitHub Packages channel, where the version
 * must stay put so a consumer can depend on `1.2.0-SNAPSHOT` once and have every push replace what
 * that coordinate resolves to. Without the flag the version carries the commit hash, so separate
 * commits publish as separate coordinates — which is what the local and staging repositories want.
 *
 * Lives here rather than in each build script because the publication plugin and the root release
 * aggregation have to agree on the string, and they previously carried two copies of this `when`.
 */
fun org.gradle.api.Project.releaseVersionSuffix(): String = when {
    hasProperty("release") -> ""
    hasProperty("alpha") ->
        property("alpha")?.toString()?.takeIf { it.isNotBlank() }?.let { "-alpha$it" } ?: "-alpha"

    hasProperty("beta") ->
        property("beta")?.toString()?.takeIf { it.isNotBlank() }?.let { "-beta$it" } ?: "-beta"

    hasProperty("rc") ->
        property("rc")?.toString()?.takeIf { it.isNotBlank() }?.let { "-rc$it" } ?: "-rc"

    hasProperty("rollingSnapshot") -> "-SNAPSHOT"
    else -> "-${getGitHashShort()}-SNAPSHOT"
}
