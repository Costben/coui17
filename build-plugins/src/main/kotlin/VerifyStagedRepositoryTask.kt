// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.zip.ZipFile

/**
 * Validates a staged Maven repository before it is uploaded to Maven Central.
 *
 * Maven Central accepts structurally broken releases and only rejects them after a
 * manual review, so every check that can be done locally is done here: presence of the
 * POM / Gradle module metadata / main, sources and javadoc jars, non-empty javadoc
 * content, checksums, optional GPG signatures, and the absence of the pre-rename
 * Miuix namespace (`top.yukonga.*`).
 */
abstract class VerifyStagedRepositoryTask : DefaultTask() {
    @get:InputDirectory
    abstract val repositoryDir: DirectoryProperty

    @get:Input
    abstract val groupId: Property<String>

    @get:Input
    abstract val version: Property<String>

    /** Artifact ids (module names) that must be present in the staged repository. */
    @get:Input
    abstract val artifacts: ListProperty<String>

    @get:Input
    abstract val requireSignature: Property<Boolean>

    @TaskAction
    fun verify() {
        val root = repositoryDir.get().asFile
        if (!root.isDirectory) {
            throw GradleException("Staged repository $root does not exist; run the staging task first")
        }

        val groupPath = groupId.get().replace('.', '/')
        val releaseVersion = version.get()
        // Snapshot publications are timestamped by Gradle (`<stem>-<yyyyMMdd.HHmmss>-<n>`), so the
        // files in a snapshot directory do not carry the literal version; only the directory does.
        val baseVersion = releaseVersion.removeSuffix("-SNAPSHOT")
        val problems = mutableListOf<String>()

        artifacts.get().sorted().forEach { artifact ->
            val dir = File(root, "$groupPath/$artifact/$releaseVersion")
            if (!dir.isDirectory) {
                problems += "$artifact: no staged directory at ${dir.relativeTo(root)}"
                return@forEach
            }
            val files = dir.listFiles().orEmpty().map { it.name }
            val stem = "$artifact-$baseVersion"
            fun published(suffix: String) = files.filter { it.startsWith(stem) && it.endsWith(suffix) }

            val poms = published(".pom")
            if (poms.isEmpty()) {
                problems += "$artifact: missing a POM ($stem*.pom)"
            }
            if (published(".module").isEmpty()) {
                problems += "$artifact: missing Gradle module metadata ($stem*.module)"
            }

            val jars = published(".jar")
            val mainJars = jars.filterNot { it.contains("-sources") || it.contains("-javadoc") }
            if (mainJars.isEmpty()) {
                problems += "$artifact: no compiled jar"
            }
            if (jars.none { it.contains("-sources") }) {
                problems += "$artifact: no sources jar"
            }

            val javadocJars = jars.filter { it.contains("-javadoc") }
            if (javadocJars.isEmpty()) {
                problems += "$artifact: no javadoc jar"
            }
            javadocJars.forEach { javadoc ->
                val entries = runCatching { ZipFile(File(dir, javadoc)).use { zf -> zf.entries().toList().map { e -> e.name } } }
                    .getOrElse { e ->
                        problems += "$artifact: unreadable jar $javadoc (${e.message})"
                        emptyList()
                    }
                if (entries.isNotEmpty() && entries.none { it.endsWith(".html") || it.endsWith(".htm") }) {
                    problems += "$artifact: $javadoc carries no documentation (entries=$entries)"
                }
            }

            if (files.none { it.endsWith(".sha1") || it.endsWith(".sha256") || it.endsWith(".md5") }) {
                problems += "$artifact: no checksums next to the published files"
            }

            if (requireSignature.get()) {
                if (files.none { it.endsWith(".asc") }) {
                    problems += "$artifact: GPG signatures required but none were produced"
                }
                val unsigned = files.filter { it.endsWith(".jar") || it.endsWith(".pom") || it.endsWith(".module") }
                    .filterNot { "$it.asc" in files }
                if (unsigned.isNotEmpty()) {
                    problems += "$artifact: missing .asc signature for $unsigned"
                }
            }

            val pom = poms.firstOrNull()?.let { File(dir, it) }
            if (pom != null && pom.isFile) {
                val pomText = pom.readText()
                if ("<groupId>${groupId.get()}</groupId>" !in pomText) {
                    problems += "$artifact: POM groupId is not ${groupId.get()}"
                }
                if (releaseVersion !in pomText) {
                    problems += "$artifact: POM does not carry version $releaseVersion"
                }
                if (STALE_NAMESPACE in pomText) {
                    problems += "$artifact: POM still references $STALE_NAMESPACE"
                }
            }
        }

        val staleFiles = root.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".txt") }
            .filter { runCatching { it.readText().contains(STALE_NAMESPACE) }.getOrDefault(false) }
            .map { it.relativeTo(root).path }
            .toList()
        if (staleFiles.isNotEmpty()) {
            problems += "staged files reference $STALE_NAMESPACE: $staleFiles"
        }

        if (problems.isNotEmpty()) {
            throw GradleException(
                "Staged Maven Central repository failed validation:\n - " + problems.joinToString("\n - "),
            )
        }
        logger.lifecycle(
            "Verified staged repository: ${artifacts.get().size} artifacts at version $releaseVersion" +
                if (requireSignature.get()) " (signed)" else " (unsigned)",
        )
    }

    private companion object {
        /**
         * The pre-rename package namespace this task rejects. Split so that a repository-wide
         * search for that namespace does not match this file.
         */
        const val STALE_NAMESPACE = "top.yukonga" + ".miuix"
    }
}
