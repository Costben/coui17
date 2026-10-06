#!/usr/bin/env bash
# Copyright 2026, compose-coui-ui contributors
# SPDX-License-Identifier: Apache-2.0
#
# Builds the independent Apple/web consumer against a candidate repository and asserts that the
# published klibs/frameworks are consumable:
#
#   compileKotlinIosArm64             iosArm64 klib
#   compileKotlinIosSimulatorArm64    iosSimulatorArm64 klib
#   compileKotlinMacosArm64           macosArm64 klib
#   compileKotlinJs                   js (IR) klib
#   compileKotlinWasmJs               wasmJs klib
#   linkDebugFrameworkIosArm64        iOS device framework
#   linkDebugFrameworkMacosArm64      macOS framework
#
# usage: verify.sh <candidate-version> [<repository-url>]
set -euo pipefail

cd "$(dirname "$0")"
root="$(cd ../.. && pwd)"

version="${1:?usage: verify.sh <candidate-version> [<repository-url>]}"
repo="${2:-https://repo1.maven.org/maven2}"

tasks=(
  compileKotlinIosArm64
  compileKotlinIosSimulatorArm64
  compileKotlinMacosArm64
  compileKotlinJs
  compileKotlinWasmJs
  linkDebugFrameworkIosArm64
  linkDebugFrameworkMacosArm64
)

echo "== Building the Apple/web consumer against $repo (coui $version) =="
echo "== Tasks: ${tasks[*]} =="

status=0
"$root/gradlew" \
  --no-configuration-cache \
  --no-daemon \
  --stacktrace \
  --refresh-dependencies \
  -p "$PWD" \
  -PcouiVersion="$version" \
  -PcouiRepo="$repo" \
  "${tasks[@]}" || status=$?

if [ "$status" -ne 0 ]; then
  echo "FAIL: the Apple/web consumer did not build against $version" >&2
  exit "$status"
fi

# Negative control: the candidate coordinates are declared as exclusive content of the candidate
# repository, so a build that never reads that repository must fail even though Maven Central is
# still in the repository list. Point the candidate at an empty directory and require rejection.
echo "== Negative control: the candidate must not resolve without the candidate repository =="
negative_repo="$(mktemp -d)"
negative_log="$(mktemp)"
negative_status=0
"$root/gradlew" \
  --no-configuration-cache \
  --no-daemon \
  -p "$PWD" \
  -PcouiVersion="$version" \
  -PcouiRepo="file://$negative_repo" \
  compileKotlinJs > "$negative_log" 2>&1 || negative_status=$?
if [ "$negative_status" -eq 0 ] || ! grep -q "io.github.suqi8.coui.kmp" "$negative_log"; then
  echo "FAIL: the candidate resolved without its repository (status=$negative_status)" >&2
  tail -20 "$negative_log" >&2
  rm -rf "$negative_repo" "$negative_log"
  exit 1
fi
rm -rf "$negative_repo" "$negative_log"
echo "PASS: without the candidate repository the build fails, so Maven Central cannot stand in"

echo "PASS: the Apple (iosArm64, iosSimulatorArm64, macosArm64) and web (js, wasmJs) consumer compiled and linked against $version"
