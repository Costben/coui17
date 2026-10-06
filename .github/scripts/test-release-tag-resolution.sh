#!/usr/bin/env bash
# Copyright 2026, compose-coui-ui contributors
# SPDX-License-Identifier: Apache-2.0
#
# Proves that a release run is built from the tag's commit and not from the branch it was
# dispatched on, and that the resolver's version derivation agrees with Gradle.
#
# The failure this guards against is a green release that quietly ships newer, unreleased branch
# code under an older tag: the version string alone cannot tell the two apart, so the drill tags a
# commit, moves the branch past it, and requires the resolver to keep reporting the tagged tree.
#
# The drill runs in a throwaway `git clone --shared` of this repository. Tags inherited from the
# source are removed inside that clone, so the drill is deterministic regardless of which release
# tags the caller's repository already carries — the real repository's tags are never touched.
#
# usage: test-release-tag-resolution.sh
set -euo pipefail

cd "$(dirname "$0")/../.."
root="$PWD"

# The test must never append to the step's own $GITHUB_OUTPUT. It drops any inherited value and
# sets the variable explicitly where the real Actions behaviour is being exercised.
unset GITHUB_OUTPUT

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

die() {
  echo "FAIL: $1" >&2
  exit 1
}

# Static precondition: every job that builds or publishes must check out the resolved SHA. Only
# the resolver itself checks out the default ref, because all it does is read git metadata.
checkouts="$(grep -c 'uses: actions/checkout@' .github/workflows/release.yml)"
pinned="$(grep -c 'ref: ${{ needs.resolve.outputs.sha }}' .github/workflows/release.yml)"
[ "$checkouts" = "$((pinned + 1))" ] ||
  die "$checkouts checkout steps but only $pinned pinned to the resolved SHA"
echo "PASS: $pinned of $checkouts checkout steps are pinned to the resolved SHA"

git clone --shared --quiet "$root" "$work/repo"
cd "$work/repo"

# Drop every tag inherited from the source repository so the drill owns its tag set. This runs in
# the throwaway clone only.
if [ -n "$(git tag -l)" ]; then
  git tag -d $(git tag -l) >/dev/null
fi

parent="$(git rev-parse HEAD)"
version="$(sed -n 's/.*LIBRARY_VERSION = "\([^"]*\)".*/\1/p' build-plugins/src/main/kotlin/BuildConfig.kt)"
echo "== Drill repository: $parent carries LIBRARY_VERSION $version =="

# Simulate the tag-trigger CI reality: the release tag already exists before the run. `-f` keeps
# the drill idempotent even when the tag is already present.
git tag -f "v$version" "$parent"

# Move the branch past the tag with a different version, so the two trees are distinguishable by
# more than their SHA.
sed -i.bak 's/LIBRARY_VERSION = "[^"]*"/LIBRARY_VERSION = "9.9.9"/' \
  build-plugins/src/main/kotlin/BuildConfig.kt
rm -f build-plugins/src/main/kotlin/BuildConfig.kt.bak
git -c user.name=drill -c user.email=drill@example.com commit -qam "drill: branch moves past the tag"
head="$(git rev-parse HEAD)"
[ "$head" != "$parent" ] || die "the drill did not advance the branch"

echo "== The branch head $head now reports version 9.9.9 =="

# `env -u GITHUB_OUTPUT` forces the resolver's default /dev/stdout sink; the GITHUB_OUTPUT case is
# exercised separately below.
resolve_to_stdout() { env -u GITHUB_OUTPUT bash "$root/.github/scripts/resolve-release-tag.sh" "$@"; }
resolve_to_ghoutput() {
  local file="$1"
  shift
  : > "$file"
  GITHUB_OUTPUT="$file" bash "$root/.github/scripts/resolve-release-tag.sh" "$@" >/dev/null
}

field() { sed -n "s/^$2=//p" "$1"; }

assert_field() {
  local file="$1" key="$2" expected="$3" actual
  actual="$(field "$file" "$key")"
  [ "$actual" = "$expected" ] || die "$key is '$actual', expected '$expected'"
}

expect_fail() {
  if resolve_to_stdout "$1" >/dev/null 2>&1; then
    die "'$1' was accepted"
  fi
  echo "PASS: rejected '$1'"
}

out="$work/out.txt"
resolve_to_stdout "v$version" >"$out"
sha="$(field "$out" sha)"
resolved_version="$(field "$out" version)"
release_version="$(field "$out" release_version)"
args="$(field "$out" gradle_args)"
prerelease="$(field "$out" prerelease)"

[ "$sha" = "$parent" ] || die "resolved $sha, expected the tag commit $parent"
echo "PASS: v$version resolves to $sha, the tag commit (branch head is $head)"
[ "$resolved_version" = "$version" ] || die "resolved version $resolved_version, expected $version"
[ "$release_version" = "$version" ] || die "resolved release_version $release_version, expected $version"
echo "PASS: the version comes from the tag's tree, not the branch's 9.9.9"
[ "$args" = "-Prelease" ] && [ "$prerelease" = "false" ] ||
  die "a final tag produced gradle_args '$args' / prerelease '$prerelease'"
echo "PASS: a final tag selects the release path"

# The same resolution must work when GITHUB_OUTPUT is set, which is how Actions really invokes it.
gh="$work/gh_output.txt"
resolve_to_ghoutput "$gh" "v$version"
for key in tag sha version release_version gradle_args prerelease; do
  grep -q "^$key=" "$gh" || die "GITHUB_OUTPUT is missing $key"
done
assert_field "$gh" sha "$parent"
assert_field "$gh" release_version "$version"
assert_field "$gh" gradle_args "-Prelease"
assert_field "$gh" prerelease "false"
echo "PASS: with GITHUB_OUTPUT set the resolver writes every output there and nothing is lost"

# What the workflow actually builds is the resolved SHA, so check that tree directly.
git checkout -q "$sha"
tree_version="$(sed -n 's/.*LIBRARY_VERSION = "\([^"]*\)".*/\1/p' build-plugins/src/main/kotlin/BuildConfig.kt)"
[ "$tree_version" = "$version" ] || die "checkout of $sha still reports $tree_version"
echo "PASS: a checkout of the resolved SHA builds LIBRARY_VERSION $tree_version"
git checkout -q -

echo "== Pre-release tags must not take the final-release path =="
for suffix in alpha1 beta2 rc1; do
  # Annotated tags carry a tag object; the resolver must dereference it to the commit.
  git -c user.name=drill -c user.email=drill@example.com tag -f -a "v$version-$suffix" \
    -m "drill annotated $suffix" "$parent"
  resolve_to_stdout "v$version-$suffix" >"$out"
  [ "$(field "$out" sha)" = "$parent" ] ||
    die "v$version-$suffix (annotated) did not dereference to $parent"
  [ "$(field "$out" prerelease)" = "true" ] || die "v$version-$suffix was not marked a pre-release"
  [ "$(field "$out" release_version)" = "$version-$suffix" ] ||
    die "v$version-$suffix produced release_version '$(field "$out" release_version)'"
  [ "$(field "$out" gradle_args)" = "-P${suffix%%[0-9]*}=${suffix#${suffix%%[0-9]*}}" ] ||
    die "v$version-$suffix produced gradle_args '$(field "$out" gradle_args)'"
  echo "PASS: annotated v$version-$suffix -> prerelease, gradle_args '$(field "$out" gradle_args)'"
done

# A tag whose tree declares a different version than the tag's prefix must be rejected, even when
# the suffix is a valid stage: the version always comes from the tagged tree.
git tag -f "v$version-alpha7" "$head"
expect_fail "v$version-alpha7"

echo "== Unresolvable or malformed tags must be rejected =="
git tag -f "v9.9.9" "$parent"
expect_fail "v9.9.9"
git tag -f "v$version-nope" "$parent"
expect_fail "v$version-nope"
git tag -f "v90.0.0" "$parent"
expect_fail "v90.0.0"
git tag -f "v$version-rc1-extra" "$parent"
expect_fail "v$version-rc1-extra"
git tag -f "v$version-alpha1x" "$parent"
expect_fail "v$version-alpha1x"
git tag -f "v$version-rc_1" "$parent"
expect_fail "v$version-rc_1"
git tag -f "v$version-RC1" "$parent"
expect_fail "v$version-RC1"
expect_fail "v1.2"
expect_fail "1.2.0"

echo "== The resolver's version must equal Gradle's printReleaseVersion =="
gradle_version() {
  (cd "$root" && ./gradlew -q printReleaseVersion "$@" --console=plain 2>/dev/null | tail -n 1)
}
[ "$(gradle_version -Prelease)" = "$version" ] ||
  die "Gradle printReleaseVersion -Prelease is '$(gradle_version -Prelease)', expected '$version'"
echo "PASS: -Prelease -> Gradle prints $version"
for suffix in alpha1 beta2 rc1; do
  stage="${suffix%%[0-9]*}"
  num="${suffix#"$stage"}"
  got="$(gradle_version "-P$stage=$num")"
  [ "$got" = "$version-$suffix" ] ||
    die "Gradle printReleaseVersion -P$stage=$num is '$got', expected '$version-$suffix'"
  echo "PASS: -P$stage=$num -> Gradle prints $got"
done

echo "All release-tag resolution scenarios behaved as expected."
