// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0

import java.io.FileInputStream
import java.util.Properties

/**
 * Root-project aggregation for releasing every published module as one Maven Central
 * bundle, plus the local staging/verification used by the consumption smoke test.
 *
 * The per-module `module.publication` plugin stages into the shared root build
 * directories declared here, so a single bundle carries all coordinates.
 */
val releaseVersion = run {
    val suffix = when {
        project.hasProperty("release") -> ""
        project.hasProperty("alpha") -> project.property("alpha")?.toString()?.takeIf { it.isNotBlank() }?.let { "-alpha$it" } ?: "-alpha"
        project.hasProperty("beta") -> project.property("beta")?.toString()?.takeIf { it.isNotBlank() }?.let { "-beta$it" } ?: "-beta"
        project.hasProperty("rc") -> project.property("rc")?.toString()?.takeIf { it.isNotBlank() }?.let { "-rc$it" } ?: "-rc"
        else -> "-${getGitHashShort()}-SNAPSHOT"
    }
    "${BuildConfig.LIBRARY_VERSION}$suffix"
}

val stagingDir = layout.buildDirectory.dir("publishing/mavenCentral")
val localRepoDir = layout.buildDirectory.dir("repository/local")

val localPropertiesFile = file("local.properties")
val localProperties = Properties()
if (localPropertiesFile.exists()) {
    FileInputStream(localPropertiesFile).use { localProperties.load(it) }
}

fun credential(name: String) = providers.environmentVariable(name)
    .orElse(providers.provider { localProperties.getProperty(name) ?: "" })

val signingRequired = (System.getenv("GPG_SIGNING_KEY") ?: localProperties.getProperty("GPG_SIGNING_KEY"))
    ?.isNotBlank() == true

val stageSnapshot = tasks.register("stageMavenCentralRelease") {
    group = "publishing"
    description = "Stages every published module into one shared Maven Central repository"
}

val verifySnapshot = tasks.register<VerifyStagedRepositoryTask>("verifyStagedMavenCentral") {
    group = "publishing"
    description = "Validates the staged Maven Central repository before upload"
    dependsOn(stageSnapshot)
    repositoryDir.set(stagingDir)
    groupId.set(BuildConfig.LIBRARY_ID)
    version.set(releaseVersion)
    requireSignature.set(signingRequired)
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

val verifyLocal = tasks.register<VerifyStagedRepositoryTask>("verifyLocalPublication") {
    group = "publishing"
    description = "Validates the shared local repository that the smoke consumer resolves"
    dependsOn(stageLocal)
    repositoryDir.set(localRepoDir)
    groupId.set(BuildConfig.LIBRARY_ID)
    version.set(releaseVersion)
    requireSignature.set(false)
}

tasks.register<CentralPublisherUploadTask>("publishToMavenCentral") {
    group = "publishing"
    description = "Uploads the verified bundle of every module through the Central Publisher API"
    dependsOn(verifySnapshot)
    sonatypeRepoDir.set(stagingDir)
    bundleFileName.set("coui-$releaseVersion.zip")
    bundleFile.set(layout.buildDirectory.file("publishing/coui-$releaseVersion.zip"))
    centralTokenId.set(credential("CENTRAL_TOKEN_ID"))
    centralTokenSecret.set(credential("CENTRAL_TOKEN_SECRET"))
}

gradle.projectsEvaluated {
    val published = rootProject.subprojects
        .filter { it.plugins.hasPlugin("maven-publish") }
        .sortedBy { it.path }
    val names = published.map { it.name }

    verifySnapshot.configure { artifacts.set(names) }
    verifyLocal.configure { artifacts.set(names) }
    stageSnapshot.configure {
        dependsOn(published.map { "${it.path}:publishAllPublicationsToSonatypeRepository" })
    }
    stageLocal.configure {
        dependsOn(published.map { "${it.path}:publishAllPublicationsToLocalRepository" })
    }
    publishGithub.configure {
        dependsOn(published.map { "${it.path}:publishAllPublicationsToGithubRepository" })
    }
}
