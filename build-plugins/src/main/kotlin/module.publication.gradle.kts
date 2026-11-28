// Copyright 2025, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0

import java.io.FileInputStream
import java.util.Properties
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    `maven-publish`
    signing
    id("org.jetbrains.dokka")
}

// Shared with the root release aggregation so the two cannot drift apart.
val versionSuffix = releaseVersionSuffix()

group = BuildConfig.LIBRARY_GROUP_ID
version = "${BuildConfig.LIBRARY_VERSION}$versionSuffix"

// Keep Kotlin's JVM moduleName anchored to LIBRARY_PACKAGE_PREFIX so internal and
// ComposableSingletons mangled names stay binary-compatible with upstream io.github.suqi8.coui.kmp
// even though Maven artifacts publish under LIBRARY_GROUP_ID.
val compatModuleBaseName = "${BuildConfig.LIBRARY_PACKAGE_PREFIX}_${project.name}"
pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    extensions.configure<KotlinMultiplatformExtension> {
        targets.configureEach {
            compilations.configureEach {
                val compName = name
                compileTaskProvider.configure {
                    compilerOptions {
                        if (this is KotlinJvmCompilerOptions) {
                            moduleName.set(
                                if (compName == "main") compatModuleBaseName else "${compatModuleBaseName}_$compName",
                            )
                        }
                    }
                }
            }
        }
    }
}

val publicationExt = extensions.create<COUIPublicationExtension>("couiPublication")

val projectUrl = "https://github.com/Costben/coui17"

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
                // Files under `kit/` packages are ported from COUI-Kit and stay under MPL-2.0.
                license {
                    name.set("Mozilla Public License 2.0")
                    url.set("https://mozilla.org/MPL/2.0/")
                }
            }
            issueManagement {
                system.set("Github")
                url.set("$projectUrl/issues")
            }
            scm {
                connection.set("scm:git:$projectUrl.git")
                developerConnection.set("scm:git:ssh://git@github.com/Costben/coui17.git")
                url.set(projectUrl)
            }
            developers {
                developer {
                    id.set("costben")
                    name.set("Costben")
                    url.set("https://github.com/Costben")
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
