#!/usr/bin/env bash
# Copyright 2026, compose-coui-ui contributors
# SPDX-License-Identifier: Apache-2.0
#
# Asserts the invariants that keep a parallel staging run from deleting its own output:
#
#   * every real `PublishToMavenRepository` task depends on the clean-up of the repository it
#     writes, so no clean can run after a publish has written files;
#   * both verification probes `mustRunAfter` every publisher of the repository they read, so a
#     probe can never observe a half-written repository (and still runs standalone);
#   * `publishToMavenCentral` does not depend on the staging graph, so the workflow's post-consumer
#     upload ships the already-verified bytes instead of re-cleaning and re-staging.
#
# The check runs against a SNAPSHOT graph, so it needs no signing key and executes no task.
#
# usage: test-publish-task-graph.sh
set -euo pipefail

cd "$(dirname "$0")/../.."

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

init="$work/task-graph-check.init.gradle"
cat > "$init" <<'GRADLE'
import org.gradle.api.GradleException
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository

gradle.taskGraph.whenReady { graph ->
    def problems = []
    def root = gradle.rootProject
    def cleanStaging = root.tasks.findByName('cleanStagingMavenCentral')
    if (cleanStaging == null) {
        // The init script is also applied to the included `build-plugins` build; only the release
        // build carries these tasks.
        return
    }
    def cleanLocal = root.tasks.findByName('cleanLocalRepository')
    def probe = root.tasks.findByName('verifyStagedRepository')
    def localProbe = root.tasks.findByName('verifyLocalRepository')
    def sonatype = []
    def local = []
    graph.allTasks.each { t ->
        if (t instanceof PublishToMavenRepository) {
            def name = t.repository.name
            if (name == 'sonatype') {
                sonatype << t
                if (!t.taskDependencies.getDependencies(t).contains(cleanStaging)) {
                    problems << "${t.path} does not depend on ${cleanStaging.path}"
                }
            } else if (name == 'local') {
                local << t
                if (!t.taskDependencies.getDependencies(t).contains(cleanLocal)) {
                    problems << "${t.path} does not depend on ${cleanLocal.path}"
                }
            }
        }
    }
    def sonatypeDeps = probe.mustRunAfter.getDependencies(probe)
    sonatype.each { if (!sonatypeDeps.contains(it)) problems << "verifyStagedRepository does not mustRunAfter ${it.path}" }
    def localDeps = localProbe.mustRunAfter.getDependencies(localProbe)
    local.each { if (!localDeps.contains(it)) problems << "verifyLocalRepository does not mustRunAfter ${it.path}" }
    sonatype.each { if (probe.taskDependencies.getDependencies(probe).contains(it)) problems << "verifyStagedRepository hard-depends on ${it.path} and can no longer run standalone" }
    local.each { if (localProbe.taskDependencies.getDependencies(localProbe).contains(it)) problems << "verifyLocalRepository hard-depends on ${it.path} and can no longer run standalone" }
    def upload = root.tasks.findByName('publishToMavenCentral')
    def uploadDeps = upload.taskDependencies.getDependencies(upload)
    [root.tasks.findByName('stageMavenCentralRelease'), root.tasks.findByName('verifyStagedMavenCentral')].each { bad ->
        if (uploadDeps.contains(bad)) problems << "publishToMavenCentral depends on ${bad.path}"
    }
    if (problems) {
        throw new GradleException('publish task-graph invariants violated:\n - ' + problems.join('\n - '))
    }
    println "TASKGRAPH-OK sonatypePublishes=${sonatype.size()} localPublishes=${local.size()} uploadDeps=${uploadDeps.size()}"
}
GRADLE

echo "== Checking the staging task graph (dry run, no key required) =="
./gradlew -I "$init" verifyStagedMavenCentral verifyLocalPublication \
  --dry-run --no-daemon --console=plain 2>&1 | tee "$work/out.log" | grep -E 'TASKGRAPH-OK|invariants violated|^ - ' || true
if ! grep -q 'TASKGRAPH-OK' "$work/out.log"; then
  echo "FAIL: the publish task-graph invariants were not satisfied" >&2
  tail -n 40 "$work/out.log" >&2
  exit 1
fi
echo "PASS: clean-up precedes every publish, probes follow every publisher, upload does not re-stage"
