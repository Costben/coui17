// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0

import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Properties
import org.gradle.api.GradleException
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.Delete

/**
 * Root-project aggregation for releasing every published module as one Maven Central
 * bundle, plus the local staging/verification used by the consumption smoke test.
 *
 * The per-module `module.publication` plugin stages into the shared root build
 * directories declared here, so a single bundle carries all coordinates.
 */

fun sha256Of(file: java.io.File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(1 shl 16)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

// Shared with the publication plugin so every module and the verification tasks agree on the string.
// `-ProllingSnapshot` pins the suffix at `-SNAPSHOT` for the GitHub Packages channel.
val releaseVersion = "${BuildConfig.LIBRARY_VERSION}${releaseVersionSuffix()}"

val stagingDir = layout.buildDirectory.dir("publishing/mavenCentral")
val localRepoDir = layout.buildDirectory.dir("repository/local")

// Every file the verifier saw, keyed by its repository-relative path. The upload task re-checks
// this manifest so the bytes sent to Maven Central are exactly the bytes the consumers accepted.
val verifiedManifest = layout.buildDirectory.file("publishing/staging-verified.manifest")

val localPropertiesFile = file("local.properties")
val localProperties = Properties()
if (localPropertiesFile.exists()) {
    FileInputStream(localPropertiesFile).use { localProperties.load(it) }
}

fun credential(name: String) = providers.environmentVariable(name)
    .orElse(providers.provider { localProperties.getProperty(name) ?: "" })

val releaseBuild = project.hasProperty("release")
val prereleaseBuild = project.hasProperty("alpha") || project.hasProperty("beta") || project.hasProperty("rc")
// Maven Central rejects unsigned artifacts, and it does not care whether the version carries a
// pre-release suffix: an alpha/beta/rc publication is as official as the final release. Every
// non-SNAPSHOT build therefore requires a signature, not just `-Prelease`.
val signedRelease = releaseBuild || prereleaseBuild
val signingKeyPresent = (System.getenv("GPG_SIGNING_KEY") ?: localProperties.getProperty("GPG_SIGNING_KEY"))
    ?.isNotBlank() == true
// A snapshot may legitimately be staged unsigned, but a release build must never be: the verifier
// rejects the bundle when the signatures are absent, so a missing key fails the release instead of
// silently shipping unsigned artifacts to Maven Central.
//
// The name deliberately differs from the task property it feeds: inside a `tasks.register { }`
// block a bare `requireSignature` would resolve to the task's own property, and `set(...)` on it
// turns into a self-reference that Gradle rejects as a circular evaluation.
val requireReleaseSignature = signedRelease || signingKeyPresent

// A non-SNAPSHOT publication that reaches staging or upload without a signing key is rejected
// before any artifact is built, instead of failing minutes later inside the verifier. The check is
// bound to the signing tasks, so unrelated `-Prelease` invocations (Dokka, version printing) and
// the unsigned local consumption path keep working.
gradle.taskGraph.whenReady {
    val signingTasks = setOf(
        "stageMavenCentralRelease",
        "verifyStagedMavenCentral",
        "verifyStagedRepository",
        "publishToMavenCentral",
    )
    if (signedRelease && !signingKeyPresent && allTasks.any { it.name in signingTasks }) {
        throw GradleException(
            "A ${if (releaseBuild) "release" else "pre-release"} build requires GPG_SIGNING_KEY and " +
                "GPG_PASSPHRASE: Maven Central rejects unsigned artifacts. Configure the secrets, or run " +
                "without -Prelease/-Palpha/-Pbeta/-Prc.",
        )
    }
}

// Both shared repositories are wiped before every staging run. Gradle appends timestamped snapshot
// builds and never removes the previous version, so without this a bundle would carry artifacts
// from earlier releases of the same working tree.
val cleanStaging = tasks.register<Delete>("cleanStagingMavenCentral") {
    group = "publishing"
    description = "Removes any previously staged Maven Central repository"
    delete(stagingDir)
}

val cleanLocal = tasks.register<Delete>("cleanLocalRepository") {
    group = "publishing"
    description = "Removes the shared local repository consumed by the smoke project"
    delete(localRepoDir)
}

val stageSnapshot = tasks.register("stageMavenCentralRelease") {
    group = "publishing"
    description = "Stages every published module into one shared Maven Central repository"
}

val stageLocal = tasks.register("publishAllToLocalRepository") {
    group = "publishing"
    description = "Publishes every module into the shared local repository for consumption tests"
}

val publishGithub = tasks.register("publishAllToGithubPackages") {
    group = "publishing"
    description = "Publishes every module to GitHub Packages (used for main-branch snapshots)"
}

tasks.register("printReleaseVersion") {
    group = "publishing"
    description = "Prints the version every module in this build publishes under"
    val resolved = releaseVersion
    doLast { println(resolved) }
}

/**
 * Verifies a shared repository without staging it again, so a repository that was mutated by hand
 * can be probed in isolation. This is what the release failure-scenario check exercises.
 */
val verifySnapshotProbe = tasks.register<VerifyStagedRepositoryTask>("verifyStagedRepository") {
    group = "publishing"
    description = "Validates the staged Maven Central repository without re-staging it"
    repositoryDir.set(stagingDir)
    groupId.set(BuildConfig.LIBRARY_ID)
    version.set(releaseVersion)
    requireSignature.set(requireReleaseSignature)
    rejectForeignVersions.set(true)
}

val verifyLocalProbe = tasks.register<VerifyStagedRepositoryTask>("verifyLocalRepository") {
    group = "publishing"
    description = "Validates the shared local repository without publishing to it again"
    repositoryDir.set(localRepoDir)
    groupId.set(BuildConfig.LIBRARY_ID)
    version.set(releaseVersion)
    requireSignature.set(false)
    rejectForeignVersions.set(true)
    // The failure-scenario check points the probe at an isolated copy of the repository so its
    // destructive mutations can never touch the shared one.
    providers.gradleProperty("verifyLocalRepoDir").orNull?.let { repositoryDir.set(file(it)) }
}

val verifySnapshot = tasks.register("verifyStagedMavenCentral") {
    group = "publishing"
    description = "Stages and validates the Maven Central repository before upload"
    dependsOn(stageSnapshot, verifySnapshotProbe)
    val manifest = verifiedManifest
    val repo = stagingDir
    doLast {
        val root = repo.get().asFile
        val lines = root.walkTopDown()
            .filter { it.isFile }
            .sortedBy { it.relativeTo(root).path }
            .joinToString("\n") { file -> "${sha256Of(file)}  ${file.relativeTo(root).path.replace('\\', '/')}" }
        manifest.get().asFile.apply { parentFile.mkdirs() }.writeText(lines + "\n")
        logger.lifecycle("Recorded the verified staging manifest at ${manifest.get().asFile}")
    }
}

val verifyLocal = tasks.register("verifyLocalPublication") {
    group = "publishing"
    description = "Validates the shared local repository that the smoke consumer resolves"
    dependsOn(stageLocal, verifyLocalProbe)
}

/**
 * The GitHub Packages repository, resolved as a local directory.
 *
 * `VerifyStagedRepositoryTask` walks a directory, so it can only inspect a `file:` repository — which
 * is exactly how the publish path is exercised without a token. An `https:` endpoint has nothing
 * local to walk, so asking for it here fails loudly rather than silently skipping the check.
 */
val githubPackagesRepoDir = providers.provider {
    val url = githubPackagesUrl(providers).get()
    if (!url.startsWith("file:")) {
        throw GradleException(
            "verifyGithubPackagesRepository inspects a local repository. Point -PcouiGithubPackagesUrl " +
                "at a file: URL (for example " +
                "file://${layout.buildDirectory.dir("repository/github").get().asFile.absolutePath}) " +
                "to validate the layout. The current endpoint is $url.",
        )
    }
    java.io.File(java.net.URI(url))
}

val verifyGithubPackages = tasks.register<VerifyStagedRepositoryTask>("verifyGithubPackagesRepository") {
    group = "publishing"
    description = "Validates the GitHub Packages repository a file: endpoint points at"
    repositoryDir.set(layout.dir(githubPackagesRepoDir))
    groupId.set(BuildConfig.LIBRARY_ID)
    version.set(releaseVersion)
    requireSignature.set(false)
    rejectForeignVersions.set(true)
}

/**
 * Publishes every module to GitHub Packages and validates what landed there.
 *
 * Against the real `https:` endpoint the publish is the only meaningful step; the verification task
 * then refuses the endpoint and says so. Against a `file:` endpoint both run, which is how the
 * coordinate layout a downstream project resolves is checked without credentials:
 *
 * ```
 * ./gradlew publishAndVerifyGithubPackages \
 *     -PcouiGithubPackagesUrl=file:///tmp/coui-github-packages
 * ```
 */
tasks.register("publishAndVerifyGithubPackages") {
    group = "publishing"
    description = "Publishes every module to GitHub Packages and validates a file: endpoint"
    dependsOn(publishGithub, verifyGithubPackages)
}

verifyGithubPackages.configure {
    // The verification reads what the publish wrote.
    mustRunAfter(publishGithub)
}

/**
 * Uploads the staged bundle. This task deliberately carries no dependency on the staging or
 * verification graph: the release workflow uploads after the consumers have accepted the staged
 * artifacts, and re-running staging there would re-clean and re-write the repository, so the bytes
 * sent to Central would no longer be the bytes that were verified. The manifest written by
 * `verifyStagedMavenCentral` is re-checked at execution time instead.
 */
tasks.register<CentralPublisherUploadTask>("publishToMavenCentral") {
    group = "publishing"
    description = "Uploads the verified bundle of every module through the Central Publisher API"
    sonatypeRepoDir.set(stagingDir)
    bundleFileName.set("coui-$releaseVersion.zip")
    bundleFile.set(layout.buildDirectory.file("publishing/coui-$releaseVersion.zip"))
    centralTokenId.set(credential("CENTRAL_TOKEN_ID"))
    centralTokenSecret.set(credential("CENTRAL_TOKEN_SECRET"))
    val manifest = verifiedManifest
    val repo = stagingDir
    doFirst {
        val root = repo.get().asFile
        val manifestFile = manifest.get().asFile
        if (!manifestFile.isFile) {
            throw GradleException(
                "No verified staging manifest at $manifestFile; run verifyStagedMavenCentral before uploading",
            )
        }
        val expected = manifestFile.readLines().filter { it.isNotBlank() }.associate { line ->
            line.substringAfter("  ") to line.substringBefore("  ")
        }
        val actual = root.walkTopDown().filter { it.isFile }
            .associate { it.relativeTo(root).path.replace('\\', '/') to sha256Of(it) }
        val problems = mutableListOf<String>()
        expected.forEach { (path, hash) ->
            if (actual[path] != hash) problems += "changed or missing since verification: $path"
        }
        actual.keys.filterNot { it in expected }.forEach { problems += "added since verification: $it" }
        if (problems.isNotEmpty()) {
            throw GradleException(
                "Refusing to upload a staging directory that differs from the verified one:\n - " +
                    problems.joinToString("\n - "),
            )
        }
        logger.lifecycle("Staging directory matches the verified manifest (${expected.size} files)")
    }
}

gradle.projectsEvaluated {
    val published = rootProject.subprojects
        .filter { it.plugins.hasPlugin("maven-publish") }
        .sortedBy { it.path }

    // Every coordinate the release must carry, including the per-platform modules Gradle links from
    // the root module through `available-at`. Checking them here is what turns "the nine root
    // coordinates exist" into "no target was silently dropped".
    val coordinates = published
        .flatMap { publishedProject ->
            publishedProject.extensions.getByType<PublishingExtension>()
                .publications.withType(MavenPublication::class.java)
                .map { it.artifactId }
        }
        .distinct()
        .sorted()

    // The fixed target matrix every published module must expose. Deriving the expected roots and
    // suffixes from the publications themselves would move with the very drift the check is meant
    // to catch, so the suffixes are listed literally here.
    val targetSuffixes = listOf(
        "android", "desktop", "iosarm64", "iossimulatorarm64", "macosarm64", "js", "wasm-js",
    )
    val rootCoordinates = coordinates.filterNot { coordinate ->
        targetSuffixes.any { suffix -> coordinate.endsWith("-$suffix") }
    }

    verifySnapshotProbe.configure {
        artifacts.set(coordinates)
        rootArtifacts.set(rootCoordinates)
        platformModules.set(targetSuffixes)
    }
    verifyLocalProbe.configure {
        artifacts.set(coordinates)
        rootArtifacts.set(rootCoordinates)
        platformModules.set(targetSuffixes)
    }
    // The GitHub Packages endpoint receives the same coordinate set as the local and Central ones,
    // so it is validated against the same fixed matrix.
    verifyGithubPackages.configure {
        artifacts.set(coordinates)
        rootArtifacts.set(rootCoordinates)
        platformModules.set(targetSuffixes)
    }

    stageSnapshot.configure {
        dependsOn(published.map { "${it.path}:publishAllPublicationsToSonatypeRepository" })
    }
    stageLocal.configure {
        dependsOn(published.map { "${it.path}:publishAllPublicationsToLocalRepository" })
    }

    // Every task that actually writes a repository wipes it first. Attaching the clean-up to the
    // `publishAllPublicationsTo...` aggregate instead would leave it unordered against the
    // aggregate's publication children — the aggregate has no action of its own, so with
    // `org.gradle.parallel` a clean could run after (or alongside) a publish and delete files that
    // had just been written.
    published.forEach { publishedProject ->
        publishedProject.tasks.withType<PublishToMavenRepository>().configureEach {
            when (repository.name) {
                "sonatype" -> dependsOn(cleanStaging)
                "local" -> dependsOn(cleanLocal)
            }
        }
    }

    val sonatypePublishes = published.flatMap { publishedProject ->
        publishedProject.tasks.withType<PublishToMavenRepository>().matching { it.repository.name == "sonatype" }
    }
    val localPublishes = published.flatMap { publishedProject ->
        publishedProject.tasks.withType<PublishToMavenRepository>().matching { it.repository.name == "local" }
    }

    // The probes deliberately carry no dependencies so they can validate a hand-mutated repository
    // on their own. Pulled into a staging run they still have to wait for the publishers: without
    // this they can read the repository mid-write and either report a partial bundle as broken or,
    // worse, validate the previous run's contents and pass.
    verifySnapshotProbe.configure { mustRunAfter(*sonatypePublishes.toTypedArray()) }
    verifyLocalProbe.configure { mustRunAfter(*localPublishes.toTypedArray()) }

    publishGithub.configure {
        dependsOn(published.map { "${it.path}:publishAllPublicationsToGithubRepository" })
    }
}
