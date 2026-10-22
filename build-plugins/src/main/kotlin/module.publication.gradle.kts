// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0

import java.io.FileInputStream
import java.util.Properties
import org.gradle.api.provider.Provider

plugins {
    `maven-publish`
    signing
    id("org.jetbrains.dokka")
}

val versionSuffix = when {
    project.hasProperty("release") -> ""
    project.hasProperty("alpha") -> {
        val v = project.property("alpha")?.toString()
        if (v.isNullOrBlank()) "-alpha" else "-alpha$v"
    }

    project.hasProperty("beta") -> {
        val v = project.property("beta")?.toString()
        if (v.isNullOrBlank()) "-beta" else "-beta$v"
    }

    project.hasProperty("rc") -> {
        val v = project.property("rc")?.toString()
        if (v.isNullOrBlank()) "-rc" else "-rc$v"
    }

    else -> "-${getGitHashShort()}-SNAPSHOT"
}

group = BuildConfig.LIBRARY_ID
version = "${BuildConfig.LIBRARY_VERSION}$versionSuffix"

val publicationExt = extensions.create<COUIPublicationExtension>("couiPublication")

val projectUrl = "https://github.com/suqi8/coui"

/** Resolved by [githubPackagesUrl] in `GithubPackages.kt`; see there for the resolution order. */
val githubPackagesRepositoryUrl: Provider<String> = githubPackagesUrl(providers)

// Every published module stages into the same root-level directories, so a release
// produces exactly one Maven Central bundle and one local repository that a smoke
// consumer can resolve the whole coordinate set from.
val sonatypePackageUrl = rootProject.layout.buildDirectory.dir("publishing/mavenCentral")
val localPackageUrl = rootProject.layout.buildDirectory.dir("repository/local")

val localPropertiesFile: File = project.rootProject.file("local.properties")
val localProperties = Properties()
if (localPropertiesFile.exists()) {
    FileInputStream(localPropertiesFile).use { localProperties.load(it) }
}

val javadocJar by tasks.registering(Jar::class) {
    description = "javadocJar"
    archiveClassifier.set("javadoc")
}

// Wire the real Dokka HTML output into the javadoc artifact. Without this the module
// would ship a javadoc jar that contains nothing but a MANIFEST, which Maven Central
// accepts but which is useless (and is rejected by strict consumers).
pluginManager.withPlugin("org.jetbrains.dokka") {
    javadocJar.configure {
        dependsOn("dokkaGenerate")
        from(layout.buildDirectory.dir("dokka/html"))
    }
}

publishing {
    repositories {
        maven {
            name = "local"
            url = uri(localPackageUrl)
        }
        maven {
            name = "sonatype"
            url = uri(sonatypePackageUrl)
        }
        maven {
            name = "github"
            url = uri(githubPackagesRepositoryUrl.get())
            // Only configured when both halves are present: Gradle 9 rejects a publish task whose
            // `credentials.username` resolves to null, and a `file:` endpoint needs no credentials at
            // all. A missing token against the real endpoint therefore fails at the HTTP layer, where
            // the 401 says what is wrong, instead of during task configuration.
            val actor = System.getenv("GITHUB_ACTOR") ?: localProperties.getProperty("GITHUB_ACTOR")
            val token = System.getenv("GITHUB_TOKEN") ?: localProperties.getProperty("GITHUB_TOKEN")
            if (!actor.isNullOrBlank() && !token.isNullOrBlank()) {
                credentials {
                    username = actor
                    password = token
                }
            }
        }
    }
    publications.withType<MavenPublication> {
        artifact(javadocJar.get())
        pom {
            name.set(project.name)
            description.set(publicationExt.description.orElse("COUI module ${project.name}"))
            url.set(projectUrl)
            licenses {
                license {
                    name.set("The Apache Software License, Version 2.0")
                    url.set("$projectUrl/blob/main/LICENSE")
                }
            }
            issueManagement {
                system.set("Github")
                url.set("$projectUrl/issues")
            }
            scm {
                connection.set("scm:git:$projectUrl.git")
                developerConnection.set("scm:git:ssh://git@github.com/suqi8/coui.git")
                url.set(projectUrl)
            }
            developers {
                developer {
                    id.set("suqi8")
                    name.set("suqi8")
                    url.set("https://github.com/suqi8")
                }
            }
        }
    }
}

val signingKey = System.getenv("GPG_SIGNING_KEY") ?: localProperties.getProperty("GPG_SIGNING_KEY")
val signingPassword = System.getenv("GPG_PASSPHRASE") ?: localProperties.getProperty("GPG_PASSPHRASE")
if (!signingKey.isNullOrBlank()) {
    signing {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
    tasks.withType<PublishToMavenRepository>().configureEach {
        dependsOn(tasks.withType<Sign>())
    }
}
