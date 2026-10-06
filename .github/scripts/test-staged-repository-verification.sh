#!/usr/bin/env bash
# Copyright 2026, compose-coui-ui contributors
# SPDX-License-Identifier: Apache-2.0
#
# Exercises the staged-repository verifier end to end against an isolated copy of the shared local
# repository.
#
# The release blocker this guards against is a staged repository that looks complete at the level of
# the nine root coordinates while silently missing a whole target or a single file's checksum —
# exactly what happens when the Apple publications are skipped, a file is truncated, or a checksum
# is written empty. Every scenario mutates a throwaway copy under $TMPDIR; the shared repository is
# never modified, so a failure mid-scenario cannot leave the working tree dirty.
#
# Each scenario asserts only that the verifier rejects the mutation; the exact wording of its error
# is left free to change.
#
# usage: test-staged-repository-verification.sh
set -euo pipefail

cd "$(dirname "$0")/../.."

GRADLE=(./gradlew --no-configuration-cache -q --console=plain)
group_path="io/github/suqi8/coui/kmp"

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

die() {
  echo "FAIL: $1" >&2
  exit 1
}

echo "== Staging every published module into the shared local repository =="
"${GRADLE[@]}" publishAllToLocalRepository >/dev/null || die "staging the local repository failed"

version="$("${GRADLE[@]}" printReleaseVersion | tail -n 1)"
[ -n "$version" ] || die "could not resolve the version under test"
echo "== Version under test: $version =="

shared="build/repository/local"
[ -d "$shared/$group_path" ] || die "no staged repository at $shared/$group_path"

# Staging isolation: a repository that carries more than the version being released would let a
# historical build leak into the bundle.
versions="$(find "$shared/$group_path" -mindepth 2 -maxdepth 2 -type d | sed 's|.*/||' | sort -u)"
if [ "$(printf '%s\n' "$versions" | wc -l | tr -d ' ')" != "1" ] || [ "$versions" != "$version" ]; then
  die "the staged repository carries versions other than $version: $(printf '%s ' $versions)"
fi
echo "PASS: the staged repository carries only $version"

# The scenarios mutate this copy only.
work_repo="$work/repo"
cp -R "$shared" "$work_repo"
repo="$work_repo/$group_path"

verify() { "${GRADLE[@]}" :verifyLocalRepository -PverifyLocalRepoDir="$work_repo"; }

expect_pass() {
  local log="$work/last.log"
  if verify >"$log" 2>&1; then
    echo "PASS: $1"
  else
    echo "FAIL: expected the verifier to accept $1" >&2
    tail -n 30 "$log" >&2
    exit 1
  fi
}

expect_failure() {
  local desc="$1"
  local log="$work/last.log"
  if verify >"$log" 2>&1; then
    echo "FAIL: expected the verifier to reject $desc" >&2
    exit 1
  fi
  # Only the rejection is asserted: the exact wording of the verifier's message is an
  # implementation detail that must be free to change without breaking this drill.
  echo "PASS: rejected $desc"
}

apple="$repo/coui-ui-iosarm64/$version"
[ -d "$apple" ] || die "no Apple platform coordinate at $apple"
desktop="$repo/coui-ui-desktop/$version"
[ -d "$desktop" ] || die "no desktop coordinate at $desktop"

expect_pass "the freshly staged repository"

echo "== Scenario: a whole Apple platform coordinate is missing =="
mv "$apple" "$apple.hidden"
expect_failure "a repository without the coui-ui-iosarm64 coordinate"
mv "$apple.hidden" "$apple"
expect_pass "the repository after the Apple coordinate was restored"

echo "== Scenario: an Apple platform artifact is missing =="
klib="$(find "$apple" -maxdepth 1 -name '*.klib' -type f | head -n 1)"
[ -n "$klib" ] || die "no .klib inside $apple"
mv "$klib" "$klib.hidden"
expect_failure "a repository missing $(basename "$klib")"
mv "$klib.hidden" "$klib"
expect_pass "the repository after the Apple artifact was restored"

echo "== Scenario: an empty javadoc jar =="
javadoc="$(find "$desktop" -maxdepth 1 -name '*-javadoc.jar' -type f | head -n 1)"
[ -n "$javadoc" ] || die "no javadoc jar inside $desktop"
cp "$javadoc" "$javadoc.orig"
python3 -c "import zipfile,sys; zipfile.ZipFile(sys.argv[1],'w').close()" "$javadoc"
expect_failure "a repository whose javadoc jar is empty"
mv "$javadoc.orig" "$javadoc"
expect_pass "the repository after the javadoc jar was restored"

echo "== Scenario: a published checksum no longer matches its file =="
jar="$(find "$desktop" -maxdepth 1 -name '*-[0-9].jar' -type f | head -n 1)"
[ -n "$jar" ] || die "no desktop jar inside $desktop"
cp "$jar.sha256" "$jar.sha256.orig"
printf '%s\n' "0000000000000000000000000000000000000000000000000000000000000000" > "$jar.sha256"
expect_failure "a repository whose checksum does not match its artifact"
mv "$jar.sha256.orig" "$jar.sha256"
expect_pass "the repository after the checksum was restored"

echo "== Scenario: a checksum file is empty =="
cp "$jar.sha256" "$jar.sha256.orig"
: > "$jar.sha256"
expect_failure "a repository whose checksum file is empty"
mv "$jar.sha256.orig" "$jar.sha256"
expect_pass "the repository after the empty checksum was restored"

echo "== Scenario: one file loses every checksum =="
for suffix in md5 sha1 sha256 sha512; do
  [ -f "$jar.$suffix" ] && cp "$jar.$suffix" "$jar.$suffix.orig"
done
rm -f "$jar.md5" "$jar.sha1" "$jar.sha256" "$jar.sha512"
expect_failure "a repository where one file has no checksum at all"
for suffix in md5 sha1 sha256 sha512; do
  [ -f "$jar.$suffix.orig" ] && mv "$jar.$suffix.orig" "$jar.$suffix"
done
expect_pass "the repository after the missing checksums were restored"

echo "== Scenario: a file the module metadata references is missing =="
missing_name="$(python3 - "$desktop" <<'PY'
import glob, json, os, sys
d = sys.argv[1]


def resolve(url):
    # A release stages the file under the very name the metadata records. A SNAPSHOT is timestamped
    # on disk ("...-<timestamp>-<n>.jar") while the metadata keeps the stable "...-SNAPSHOT.jar"
    # name, so match on the stem before SNAPSHOT and require the numeric build suffix that only the
    # main artifact carries (not -sources/-javadoc).
    if os.path.isfile(os.path.join(d, url)):
        return url
    base, ext = os.path.splitext(url)
    stem = base.replace("-SNAPSHOT", "").replace("SNAPSHOT", "")
    matches = sorted(glob.glob(os.path.join(d, stem + "*[0-9]" + ext)))
    return os.path.basename(matches[0]) if matches else None


for module in sorted(glob.glob(os.path.join(d, '*.module'))):
    data = json.load(open(module))
    entries = list(data.get('files', []))
    for variant in data.get('variants', []):
        entries += variant.get('files', [])
    for entry in entries:
        url = entry.get('url')
        if url and url.endswith('.jar'):
            resolved = resolve(url)
            if resolved:
                print(resolved)
                raise SystemExit
PY
)"
[ -n "$missing_name" ] || die "no metadata-referenced jar found inside $desktop"
mv "$desktop/$missing_name" "$desktop/$missing_name.hidden"
expect_failure "a repository missing $missing_name that the module metadata references"
mv "$desktop/$missing_name.hidden" "$desktop/$missing_name"
expect_pass "the repository after the metadata-referenced file was restored"

echo "== Scenario: module metadata still references the pre-rename namespace =="
# A stale namespace hides in text metadata. The POM is checked on its own, but Gradle module
# metadata is not: the dependency group inside variants[].dependencies is only visible to the
# repository-wide namespace scan, so this is the scenario that fails if that scan is narrowed to
# a suffix that no staged file actually uses. The four digest sidecars are rewritten because the
# checksum check runs first and would otherwise mask the namespace failure.
module="$(find "$desktop" -maxdepth 1 -name '*.module' -type f | head -n 1)"
[ -n "$module" ] || die "no .module inside $desktop"
# The backups live outside the repository on purpose: a `*.orig` sibling would itself be an
# unchecksummed payload, so the checksum rule would reject the coordinate first and this scenario
# would pass without ever exercising the namespace scan.
module_name="$(basename "$module")"
backup="$work/module-backup"
mkdir -p "$backup"
before_files="$(find "$desktop" -maxdepth 1 -type f | wc -l | tr -d ' ')"
cp "$module" "$backup/$module_name"
for suffix in md5 sha1 sha256 sha512; do
  [ -f "$module.$suffix" ] && cp "$module.$suffix" "$backup/$module_name.$suffix"
done
python3 - "$module" <<'PY'
import hashlib, json, sys

path = sys.argv[1]
with open(path) as fh:
    data = json.load(fh)
variants = data.get("variants") or []
target = next((v for v in variants if v.get("files")), variants[0])
target.setdefault("dependencies", []).append(
    {"group": "top.yukonga.miuix", "module": "coui-core", "version": "1.1.0"}
)
with open(path, "w") as fh:
    json.dump(data, fh)
blob = open(path, "rb").read()
for algo in ("md5", "sha1", "sha256", "sha512"):
    with open(path + "." + algo, "w") as fh:
        fh.write(hashlib.new(algo, blob).hexdigest())
PY
# Guard the scenario itself: the namespace has to be in the mutated file, the digests have to be
# refreshed, and no stray file may be left in the coordinate — any of those failing would make the
# rejection below come from a different rule.
grep -q "top.yukonga.miuix" "$module" || die "the namespace mutation did not land in $module_name"
after_files="$(find "$desktop" -maxdepth 1 -type f | wc -l | tr -d ' ')"
[ "$before_files" = "$after_files" ] || die "the mutation changed the file count in $desktop ($before_files -> $after_files)"
expect_failure "module metadata that still references the pre-rename namespace"
cp "$backup/$module_name" "$module"
for suffix in md5 sha1 sha256 sha512; do
  [ -f "$backup/$module_name.$suffix" ] && cp "$backup/$module_name.$suffix" "$module.$suffix"
done
if grep -q "top.yukonga.miuix" "$module"; then
  die "the module metadata was not restored"
fi
expect_pass "the repository after the module metadata was restored"

echo "== Scenario: the repository carries a foreign version =="
mkdir -p "$repo/coui-ui/$version-foreign"
expect_failure "a repository that carries a second version"
rmdir "$repo/coui-ui/$version-foreign"
expect_pass "the repository after the foreign version was removed"

echo "All staged-repository verification scenarios behaved as expected."
