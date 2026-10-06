#!/usr/bin/env bash
# Copyright 2026, compose-coui-ui contributors
# SPDX-License-Identifier: Apache-2.0
#
# Builds the independent API 23 consumer against a candidate repository and asserts that the
# manifest merge kept minSdk 23 without any `tools:overrideLibrary`.
#
# usage: verify.sh <coui-version> [<repository-url>]
set -euo pipefail

cd "$(dirname "$0")"
root="$(cd ../.. && pwd)"

version="${1:?usage: verify.sh <coui-version> [<repository-url>]}"
repo="${2:-https://repo1.maven.org/maven2}"

# XML comments are stripped first: the manifest explains in a comment that it deliberately avoids
# `tools:overrideLibrary`, and a naive grep would flag that explanation as a violation.
strip_comments() {
  perl -0777 -pe 's/<!--.*?-->//gs' "$1"
}

if strip_comments src/main/AndroidManifest.xml | grep -q 'overrideLibrary'; then
  echo "FAIL: the consumer project must not rely on tools:overrideLibrary" >&2
  exit 1
fi

echo "== Building the API 23 consumer against $repo (coui $version) =="
# `--refresh-dependencies` forces re-resolution: when the same URL is re-staged under the same
# SNAPSHOT version, Gradle would otherwise keep serving the previously cached bytes.
"$root/gradlew" --no-configuration-cache --refresh-dependencies -p "$PWD" \
  -PcouiVersion="$version" \
  -PconsumerRepo="$repo" \
  assembleDebug

manifest="$(find build/intermediates -path '*merged_manifest*' -name AndroidManifest.xml | head -n 1)"
if [ -z "$manifest" ]; then
  echo "FAIL: the build produced no merged manifest" >&2
  exit 1
fi

if ! grep -q 'minSdkVersion="23"' "$manifest"; then
  echo "FAIL: the merged manifest does not keep minSdkVersion=23:" >&2
  cat "$manifest" >&2
  exit 1
fi

if strip_comments "$manifest" | grep -q 'overrideLibrary'; then
  echo "FAIL: the merged manifest carries an overrideLibrary entry" >&2
  exit 1
fi

# Negative control: the candidate coordinates must only be reachable through the candidate
# repository. Point it at an empty directory; the build must then fail, proving Maven Central
# cannot stand in for the candidate.
echo "== Negative control: the candidate must not resolve without the candidate repository =="
negative_repo="$(mktemp -d)"
negative_log="$(mktemp)"
negative_status=0
"$root/gradlew" --no-configuration-cache -p "$PWD" \
  -PcouiVersion="$version" \
  -PconsumerRepo="file://$negative_repo" \
  assembleDebug > "$negative_log" 2>&1 || negative_status=$?
if [ "$negative_status" -eq 0 ] || ! grep -q "io.github.suqi8.coui.kmp" "$negative_log"; then
  echo "FAIL: the candidate resolved without its repository (status=$negative_status)" >&2
  tail -20 "$negative_log" >&2
  rm -rf "$negative_repo" "$negative_log"
  exit 1
fi
rm -rf "$negative_repo" "$negative_log"
echo "PASS: without the candidate repository the build fails, so Maven Central cannot stand in"

echo "PASS: the API 23 consumer built and merged cleanly ($manifest)"
