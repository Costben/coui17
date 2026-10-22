// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

/**
 * The GitHub Packages endpoint every module publishes to.
 *
 * Resolution order:
 *
 * 1. `GITHUB_PACKAGES_URL` (environment)
 * 2. `-PcouiGithubPackagesUrl=…` (Gradle property)
 * 3. `https://maven.pkg.github.com/$GITHUB_REPOSITORY`, when CI provides it
 * 4. [DEFAULT_GITHUB_PACKAGES_URL]
 *
 * The last fallback deliberately names this fork rather than upstream (`suqi8/coui`): publishing
 * into another account's namespace cannot succeed, so defaulting there would only turn a missing
 * configuration into a 403 minutes into the build.
 *
 * A `file:` URL makes the whole publish path runnable without a token, which is how the repository
 * layout is verified locally:
 *
 * ```
 * ./gradlew publishAllToGithubPackages \
 *     -PcouiGithubPackagesUrl=file:///tmp/coui-github-packages
 * ```
 */
fun githubPackagesUrl(providers: ProviderFactory): Provider<String> =
    providers.environmentVariable("GITHUB_PACKAGES_URL")
        .orElse(providers.gradleProperty("couiGithubPackagesUrl"))
        .orElse(
            providers.environmentVariable("GITHUB_REPOSITORY")
                .map { "https://maven.pkg.github.com/$it" },
        )
        .orElse(DEFAULT_GITHUB_PACKAGES_URL)

/** The repository this fork publishes to when nothing else is configured. */
const val DEFAULT_GITHUB_PACKAGES_URL = "https://maven.pkg.github.com/Costben/coui17"
