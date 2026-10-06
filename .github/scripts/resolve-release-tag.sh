#!/usr/bin/env bash
# Copyright 2026, compose-coui-ui contributors
# SPDX-License-Identifier: Apache-2.0
#
# Resolves the tag a release run targets into the commit that must be built, and derives the
# Gradle version arguments. Every later job checks out the SHA this prints, so the release is built
# from the tag's tree and never from whatever the branch happens to contain.
#
# usage: resolve-release-tag.sh <tag>
#
# Outputs (written to $GITHUB_OUTPUT when set, otherwise stdout):
#   tag              the tag that was resolved
#   sha              the commit the tag points at (annotated or lightweight)
#   version          the base <major>.<minor>.<patch> read from the tagged tree
#   release_version  the full version this release publishes, suffix included
#   gradle_args      the Gradle property that reproduces release_version
#   prerelease       "true" for -alpha/-beta/-rc tags, "false" for a final tag
set -euo pipefail

tag="${1:?usage: resolve-release-tag.sh <tag>}"
output="${GITHUB_OUTPUT:-/dev/stdout}"

fail() {
  echo "::error::$1" >&2
  echo "$1" >&2
  exit 1
}

# The tag shape is validated before any git call so a malformed tag can never be read as a version.
if ! printf '%s' "$tag" | grep -Eq '^v[0-9]+\.[0-9]+\.[0-9]+(-[0-9A-Za-z.]+)?$'; then
  fail "tag '$tag' is not a v<major>.<minor>.<patch>[-<prerelease>] release tag"
fi

# `^{commit}` dereferences annotated tags to the commit they point at, so both tag kinds resolve.
if ! git rev-parse --verify --quiet "refs/tags/$tag^{commit}" >/dev/null; then
  fail "tag '$tag' does not exist in this repository"
fi
sha="$(git rev-parse "refs/tags/$tag^{commit}")"

# The version is read from the tagged commit, not from the working tree: a branch that already
# moved past the tag must not influence which version is released.
build_config="$(git show "$sha:build-plugins/src/main/kotlin/BuildConfig.kt")"
version="$(printf '%s\n' "$build_config" | sed -n 's/.*LIBRARY_VERSION = "\([^"]*\)".*/\1/p')"
[ -n "$version" ] || fail "could not read LIBRARY_VERSION from commit $sha"
printf '%s' "$version" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+$' ||
  fail "LIBRARY_VERSION '$version' in commit $sha is not <major>.<minor>.<patch>"

if [ "$tag" = "v$version" ]; then
  prerelease=false
  gradle_args="-Prelease"
  release_version="$version"
else
  # Only the three Central pre-release stages are recognised. The identifier after the stage is
  # either absent or a dot-separated run of digits, which is what Gradle's versionSuffix accepts.
  rest="${tag#v"$version"-}"
  if ! printf '%s' "$rest" | grep -Eq '^(alpha|beta|rc)([0-9]+(\.[0-9]+)*)?$'; then
    fail "tag '$tag' matches neither v$version nor a pre-release of it (v$version-alpha/-beta/-rc)"
  fi
  stage="${rest%%[0-9]*}"
  num="${rest#"$stage"}"
  prerelease=true
  gradle_args="-P$stage=$num"
  release_version="$version-$stage$num"
fi

{
  echo "tag=$tag"
  echo "sha=$sha"
  echo "version=$version"
  echo "release_version=$release_version"
  echo "gradle_args=$gradle_args"
  echo "prerelease=$prerelease"
} >> "$output"

echo "$tag -> $sha (version $release_version, gradle_args '$gradle_args', prerelease=$prerelease)"
