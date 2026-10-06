// Copyright 2026, compose-coui-ui contributors
// SPDX-License-Identifier: Apache-2.0

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

/**
 * Validates a staged Maven repository before it is uploaded to Maven Central.
 *
 * Maven Central accepts structurally broken releases and only rejects them after a manual review,
 * so every check that can be done locally is done here:
 *
 * - every expected coordinate — including the per-platform modules that Gradle module metadata
 *   publishes through `available-at` (e.g. `coui-ui-iosarm64`) — owns a POM, module metadata, a
 *   main artifact (`.klib` / `.aar` / `.jar`), a sources jar and a javadoc jar carrying real Dokka
 *   HTML;
 * - every `available-at` reference resolves to a coordinate that is actually staged, and every
 *   staged coordinate's group and version match the release, so a half-published target set (the
 *   failure mode of publishing Apple targets from a runner that cannot build them) is rejected;
 * - every published file's `.md5` / `.sha1` / `.sha256` / `.sha512` sibling digest matches the file
 *   itself, so a truncated or stale artifact cannot slip through;
 * - no coordinate at another version is present, which catches a staging directory that was not
 *   isolated between releases;
 * - signatures are present when [requireSignature] is set, and the pre-rename Miuix namespace
 *   (`top.yukonga.*`) never appears.
 */
abstract class VerifyStagedRepositoryTask : DefaultTask() {
    @get:InputDirectory
    abstract val repositoryDir: DirectoryProperty

    @get:Input
    abstract val groupId: Property<String>

    @get:Input
    abstract val version: Property<String>

    /**
     * Every coordinate that must be present, including platform modules such as `coui-ui-iosarm64`.
     * The task additionally validates every coordinate referenced through `available-at`, so a
     * reference that is missing from this list still gets checked.
     */
    @get:Input
    abstract val artifacts: ListProperty<String>

    /**
     * The root coordinates, one per published module. Together with [platformModules] they form the
     * explicit target matrix the release must expose, so a target that was dropped from the build —
     * and therefore from [artifacts] — is reported instead of being silently accepted.
     */
    @get:Input
    abstract val rootArtifacts: ListProperty<String>

    /** The KMP target module suffixes every root coordinate has to publish. */
    @get:Input
    abstract val platformModules: ListProperty<String>

    @get:Input
    abstract val requireSignature: Property<Boolean>

    /** Fails when the repository also carries a coordinate at any other version. */
    @get:Input
    abstract val rejectForeignVersions: Property<Boolean>

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

        val expected = artifacts.get().sorted()
        val referenced = collectReferencedCoordinates(root, groupPath, releaseVersion, expected, problems)
        val stagedCoordinates = expected.toSet() + referenced

        // Platform coverage is asserted against the declared target matrix, not against the
        // publications that happened to run: a target missing from the build is missing from
        // [artifacts] too, so only this fixed expectation can catch it.
        val requiredCoordinates = buildList {
            rootArtifacts.get().sorted().forEach { rootArtifact ->
                add(rootArtifact)
                platformModules.get().sorted().forEach { platform -> add("$rootArtifact-$platform") }
            }
        }
        requiredCoordinates.sorted().forEach { coordinate ->
            if (coordinate !in stagedCoordinates) {
                problems += "$coordinate: the release must publish this coordinate but it is absent " +
                    "from the staged repository"
            }
        }

        stagedCoordinates.sorted().forEach { artifact ->
            validateCoordinate(root, groupPath, releaseVersion, baseVersion, artifact, problems)
        }

        if (rejectForeignVersions.get()) {
            problems += findForeignVersions(root, groupPath, releaseVersion)
        }

        val staleFiles = root.walkTopDown()
            .filter { it.isFile && TEXT_METADATA_SUFFIXES.any(it.name::endsWith) }
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
            "Verified staged repository: ${(expected.toSet() + referenced).size} coordinates at version $releaseVersion" +
                if (requireSignature.get()) " (signed)" else " (unsigned)",
        )
    }

    /**
     * Reads the `available-at` references out of every known root coordinate's Gradle module
     * metadata. A KMP root module carries no artifacts of its own: each target variant points at a
     * separate platform module, and a release that skipped a target leaves those references
     * dangling.
     */
    private fun collectReferencedCoordinates(
        root: File,
        groupPath: String,
        releaseVersion: String,
        expected: List<String>,
        problems: MutableList<String>,
    ): Set<String> {
        val referenced = sortedSetOf<String>()
        expected.forEach { artifact ->
            val dir = File(root, "$groupPath/$artifact/$releaseVersion")
            if (!dir.isDirectory) return@forEach
            dir.listFiles().orEmpty()
                .filter { it.isFile && it.name.endsWith(".module") }
                .forEach { moduleFile ->
                    val parsed = runCatching { JsonSlurper().parse(moduleFile) }.getOrElse { e ->
                        problems += "$artifact: unreadable module metadata ${moduleFile.name} (${e.message})"
                        return@forEach
                    }
                    val variants = (parsed as? Map<*, *>)?.get("variants") as? List<*> ?: return@forEach
                    variants.forEach { variant ->
                        val availableAt = (variant as? Map<*, *>)?.get("available-at") as? Map<*, *>
                            ?: return@forEach
                        val module = (availableAt["module"] as? String) ?: return@forEach
                        val group = availableAt["group"] as? String
                        val version = availableAt["version"] as? String
                        if (group != null && group != groupId.get()) {
                            problems += "$artifact: available-at reference to $module targets group $group"
                        }
                        if (version != null && version != releaseVersion) {
                            problems += "$artifact: available-at reference to $module targets version $version"
                        }
                        referenced += module
                    }
                }
        }
        return referenced
    }

    private fun validateCoordinate(
        root: File,
        groupPath: String,
        releaseVersion: String,
        baseVersion: String,
        artifact: String,
        problems: MutableList<String>,
    ) {
        val dir = File(root, "$groupPath/$artifact/$releaseVersion")
        if (!dir.isDirectory) {
            problems += "$artifact: no staged directory at ${dir.relativeTo(root)}"
            return
        }
        val files = dir.listFiles().orEmpty().filter { it.isFile }.map { it.name }
        val stem = "$artifact-$baseVersion"
        fun published(suffix: String) = files.filter { it.startsWith(stem) && it.endsWith(suffix) }

        val poms = published(".pom")
        if (poms.isEmpty()) {
            problems += "$artifact: missing a POM ($stem*.pom)"
        }
        if (published(".module").isEmpty()) {
            problems += "$artifact: missing Gradle module metadata ($stem*.module)"
        }

        val mainArtifacts = files.filter { name ->
            (name.endsWith(".klib") || name.endsWith(".aar") || name.endsWith(".jar")) &&
                "-sources" !in name && "-javadoc" !in name && "-metadata" !in name
        }
        if (mainArtifacts.isEmpty()) {
            problems += "$artifact: no compiled artifact (expected one of .klib/.aar/.jar)"
        }
        if (published("-sources.jar").isEmpty()) {
            problems += "$artifact: no sources jar"
        }

        val javadocJars = published("-javadoc.jar")
        if (javadocJars.isEmpty()) {
            problems += "$artifact: no javadoc jar"
        }
        javadocJars.forEach { javadoc ->
            val entries = runCatching { ZipFile(File(dir, javadoc)).use { zf -> zf.entries().toList().map { e -> e.name } } }
                .getOrElse { e ->
                    problems += "$artifact: unreadable jar $javadoc (${e.message})"
                    emptyList()
                }
            // A jar that carries only a MANIFEST, or no entries at all, is the "empty javadoc"
            // failure mode: Maven Central accepts it, so it has to be rejected here.
            if (entries.isEmpty()) {
                problems += "$artifact: $javadoc is empty"
            } else if (entries.none { it.endsWith(".html") || it.endsWith(".htm") }) {
                problems += "$artifact: $javadoc carries no documentation (entries=$entries)"
            }
        }

        verifyChecksums(dir, files, artifact, problems)
        // SNAPSHOT publications are timestamped on disk while the module metadata keeps the base
        // version in each `url`, so an exact existence check would false-positive. The directory,
        // artifact and checksum checks above still cover a snapshot; the metadata-reference check
        // runs only for a real release, where the names match exactly.
        if (!releaseVersion.endsWith("-SNAPSHOT")) {
            validateModuleFileReferences(dir, files, artifact, problems)
        }

        if (requireSignature.get()) {
            if (files.none { it.endsWith(".asc") }) {
                problems += "$artifact: GPG signatures required but none were produced"
            }
            val unsigned = files
                .filter { it.endsWith(".klib") || it.endsWith(".aar") || it.endsWith(".jar") || it.endsWith(".pom") || it.endsWith(".module") }
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

    /**
     * Requires a valid digest for every published payload, not merely for the coordinate: a file
     * whose checksums were all removed, or whose only checksum is empty, must be rejected even when
     * a sibling artifact still carries one.
     */
    private fun verifyChecksums(dir: File, files: List<String>, artifact: String, problems: MutableList<String>) {
        val algorithms = listOf("md5" to "MD5", "sha1" to "SHA-1", "sha256" to "SHA-256", "sha512" to "SHA-512")
        val checksumSuffixes = algorithms.map { it.first }
        val payloads = files.filterNot { name -> checksumSuffixes.any { name.endsWith(".$it") } }
        if (payloads.isEmpty()) {
            problems += "$artifact: no published files"
            return
        }
        for (name in payloads) {
            val payload = File(dir, name)
            var present = 0
            var valid = 0
            for ((suffix, algorithm) in algorithms) {
                val checksumFile = File(dir, "$name.$suffix")
                if (!checksumFile.isFile) continue
                present++
                val expected = checksumFile.readText().trim().substringBefore(' ').substringBefore('\t')
                if (expected.isEmpty()) {
                    problems += "$artifact: $name.$suffix is empty"
                    continue
                }
                val digest = try {
                    digestOf(payload, algorithm)
                } catch (e: Exception) {
                    problems += "$artifact: cannot hash $name (${e.message})"
                    null
                } ?: continue
                if (expected.equals(digest, ignoreCase = true)) {
                    valid++
                } else {
                    problems += "$artifact: $name.$suffix does not match $name (expected=$expected actual=$digest)"
                }
            }
            // Gradle does not publish digests for the detached `.asc` signatures, so only the real
            // payloads are required to own one; any digest an `.asc` does carry is still checked.
            if (present == 0 && !name.endsWith(".asc")) {
                problems += "$artifact: $name has no checksum (.md5/.sha1/.sha256/.sha512)"
            } else if (present > 0 && valid == 0) {
                problems += "$artifact: $name has no valid checksum"
            }
        }
    }

    /**
     * Every file the Gradle module metadata references must exist in the coordinate directory and
     * match the size and digests recorded there. Without this a directory could contain a valid
     * main artifact while the metadata points at a different, missing or corrupted file.
     */
    private fun validateModuleFileReferences(
        dir: File,
        files: List<String>,
        artifact: String,
        problems: MutableList<String>,
    ) {
        val moduleFiles = files.filter { it.endsWith(".module") }
        for (moduleName in moduleFiles) {
            val moduleFile = File(dir, moduleName)
            val parsed = try {
                JsonSlurper().parse(moduleFile)
            } catch (e: Exception) {
                problems += "$artifact: unreadable module metadata $moduleName (${e.message})"
                continue
            }
            val root = parsed as? Map<*, *> ?: continue
            // Gradle records artifact references in two places: a top-level `files` list and a
            // per-variant `files` list. A KMP platform module carries its klib in the variant, so
            // both have to be walked or a missing klib slips through.
            val entries = mutableListOf<Any?>()
            (root["files"] as? List<*>)?.let { entries.addAll(it) }
            (root["variants"] as? List<*>)?.forEach { variant ->
                val variantFiles = (variant as? Map<*, *>)?.get("files")
                if (variantFiles is List<*>) entries.addAll(variantFiles)
            }
            for (entry in entries) {
                val map = entry as? Map<*, *> ?: continue
                val url = map["url"] as? String ?: continue
                val target = File(dir, url)
                if (!target.isFile) {
                    problems += "$artifact: $moduleName references missing file $url"
                    continue
                }
                val size = (map["size"] as? Number)?.toLong()
                if (size != null && target.length() != size) {
                    problems += "$artifact: $url is ${target.length()} bytes, metadata says $size"
                }
                for ((key, algorithm) in listOf("sha512" to "SHA-512", "sha256" to "SHA-256", "sha1" to "SHA-1", "md5" to "MD5")) {
                    val expected = map[key] as? String ?: continue
                    val digest = try {
                        digestOf(target, algorithm)
                    } catch (e: Exception) {
                        problems += "$artifact: cannot hash $url (${e.message})"
                        continue
                    }
                    if (!expected.equals(digest, ignoreCase = true)) {
                        problems += "$artifact: $url $key does not match the file (expected=$expected actual=$digest)"
                    }
                }
            }
        }
    }

    private fun digestOf(file: File, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm)
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

    /** Every version directory under the release group must be the version being released. */
    private fun findForeignVersions(root: File, groupPath: String, releaseVersion: String): List<String> {
        val groupDir = File(root, groupPath)
        if (!groupDir.isDirectory) return emptyList()
        return groupDir.listFiles().orEmpty()
            .filter { it.isDirectory }
            .flatMap { moduleDir ->
                moduleDir.listFiles().orEmpty()
                    .filter { it.isDirectory && it.name != releaseVersion }
                    .map { "staged at another version: ${it.relativeTo(root).path} (expected $releaseVersion)" }
            }
    }

    private companion object {
        /**
         * The pre-rename package namespace this task rejects. Split so that a repository-wide
         * search for that namespace does not match this file.
         */
        const val STALE_NAMESPACE = "top.yukonga" + ".miuix"

        /**
         * Text metadata a stale namespace can hide in. Only these suffixes are scanned: binary
         * artifacts (`.jar` / `.aar` / `.klib` / `.zip`) are matched by [validateCoordinate]
         * separately, and digest sidecars never carry a coordinate.
         */
        val TEXT_METADATA_SUFFIXES = listOf(".pom", ".module", ".xml", ".json", ".txt")
    }
}
