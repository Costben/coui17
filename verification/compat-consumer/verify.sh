#!/usr/bin/env bash
# Copyright 2026, compose-coui-ui contributors
# SPDX-License-Identifier: Apache-2.0
#
# Runs the compatibility channels of the independent consumer against a candidate repository:
#
#   1. 1.1.0-era source recompiled against the candidate       -> :compileCandidateKotlin
#   2. bytecode compiled against 1.1.0, executed against the
#      candidate (real calls, no recompilation)                -> :runLegacyBinaryOnCandidate
#
# The second task runs all of the consumer's runtime checks: the restored descriptor table resolves,
# the legacy bytecode links and composes, and the restored constructors and defaults keep their values.
#
# usage: verify.sh <candidate-version> [<repository-url>] [<legacy-version>]
set -euo pipefail

cd "$(dirname "$0")"
root="$(cd ../.. && pwd)"

version="${1:?usage: verify.sh <candidate-version> [<repository-url>] [<legacy-version>]}"
repo="${2:-https://repo1.maven.org/maven2}"
legacy="${3:-1.1.0}"

echo "== Channel 1: 1.1.0-era source recompiled against $version =="
"$root/gradlew" --no-configuration-cache --refresh-dependencies -p "$PWD" \
  -PcouiVersion="$version" \
  -PlegacyVersion="$legacy" \
  -PcouiRepo="$repo" \
  compileCandidateKotlin

echo "== Channel 2: bytecode compiled against $legacy, run against $version =="
"$root/gradlew" --no-configuration-cache --refresh-dependencies -p "$PWD" \
  -PcouiVersion="$version" \
  -PlegacyVersion="$legacy" \
  -PcouiRepo="$repo" \
  runLegacyBinaryOnCandidate

# Negative control: the candidate coordinates must only be reachable through the candidate
# repository. Point it at an empty directory; the candidate channel must then fail, proving the
# build never falls back to Maven Central for the candidate version.
echo "== Negative control: the candidate must not resolve without the candidate repository =="
negative_repo="$(mktemp -d)"
negative_log="$(mktemp)"
negative_status=0
"$root/gradlew" --no-configuration-cache -p "$PWD" \
  -PcouiVersion="$version" \
  -PlegacyVersion="$legacy" \
  -PcouiRepo="file://$negative_repo" \
  compileCandidateKotlin > "$negative_log" 2>&1 || negative_status=$?
if [ "$negative_status" -eq 0 ] || ! grep -q "io.github.suqi8.coui.kmp" "$negative_log"; then
  echo "FAIL: the candidate resolved without its repository (status=$negative_status)" >&2
  tail -20 "$negative_log" >&2
  rm -rf "$negative_repo" "$negative_log"
  exit 1
fi
rm -rf "$negative_repo" "$negative_log"
echo "PASS: without the candidate repository the build fails, so Maven Central cannot stand in"

echo "PASS: the 1.1.0 consumer compares clean against $version"
