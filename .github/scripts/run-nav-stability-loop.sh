#!/usr/bin/env bash
# Copyright 2026, compose-coui-ui contributors
# SPDX-License-Identifier: Apache-2.0
#
# Runs the navigation test that has a history of failing intermittently (it used to overrun the
# framework's per-test-body timeout once the settle helper out-paced the driver) ten times in a row
# on the same checkout. A single green run says nothing about a timing-sensitive case; ten
# consecutive green runs on the candidate commit do.
#
# usage: run-nav-stability-loop.sh [runs]
set -euo pipefail

cd "$(dirname "$0")/../.."

runs="${1:-10}"
test_class="io.github.suqi8.coui.kmp.nav.core.NavNestedStressTest"

for run in $(seq 1 "$runs"); do
  echo "::group::$test_class run $run/$runs"
  # `--rerun` re-executes only the test task, so the loop does not pay for an up-to-date check that
  # would otherwise skip runs 2-10.
  ./gradlew :coui-nav:desktopTest --tests "$test_class" --rerun
  echo "::endgroup::"
done

echo "PASS: $test_class passed $runs consecutive runs"
