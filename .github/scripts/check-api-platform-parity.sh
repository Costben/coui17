#!/usr/bin/env bash
# Copyright 2026, compose-coui-ui contributors
# SPDX-License-Identifier: Apache-2.0
#
# Lightweight multiplatform API parity guard.
#
# The binary-compatibility-validator dump only describes the desktop JVM surface, so a change that
# drops a public class from the Android compilation would pass `apiCheck` unnoticed. This check
# reads each module's desktop dump and asserts its public top-level classes are also present in that
# module's Android AAR (`classes.jar`), with an explicit allowlist for classes that are legitimately
# desktop-only (skiko/desktop source sets).
#
# It is deliberately not a full KLIB dump: it covers the Android AAR's public top-level class set
# against the desktop API dump, which is what the CI can run cheaply and deterministically.
#
# The AAR must have been built first (`./gradlew :coui-<x>:assembleRelease` or a publish run), so
# this runs after the publication smoke task in CI.
#
# usage: check-api-platform-parity.sh
set -euo pipefail

cd "$(dirname "$0")/../.."

allowlist=".github/api-platform-parity-allowlist.txt"
[ -f "$allowlist" ] || { echo "missing allowlist $allowlist" >&2; exit 1; }

python3 - "$allowlist" <<'PY'
import glob
import io
import os
import re
import struct
import sys
import zipfile

allowlist_path = sys.argv[1]
allowlist = set()
with open(allowlist_path, encoding="utf-8") as handle:
    for line in handle:
        line = line.split("#", 1)[0].strip()
        if line:
            allowlist.add(line)

# A top-level declaration line in a BCV dump looks like one of
#   public final class io/.../FooKt {
#   public abstract interface class io/.../RuntimeShader {
#   public abstract interface annotation class io/.../ExperimentalFoo : ... {
# and nested classes carry a `$` in the name.
DECL = re.compile(r"\b(?:class|interface)\s+([\w/$]+)\s*(?::|\{)")


def dump_classes(path):
    found = set()
    with open(path, encoding="utf-8") as handle:
        for line in handle:
            if not line.startswith("public"):
                continue
            match = DECL.search(line)
            if match and "$" not in match.group(1):
                found.add(match.group(1))
    return found


def aar_classes(path):
    found = set()
    with zipfile.ZipFile(path) as archive:
        class_jars = [n for n in archive.namelist() if n.endswith("classes.jar")]
        if not class_jars:
            return found
        with zipfile.ZipFile(io.BytesIO(archive.read(class_jars[0]))) as classes:
            for entry in classes.namelist():
                if not entry.endswith(".class"):
                    continue
                name = entry[:-len(".class")]
                if "$" in name:
                    continue
                header = classes.read(entry)[:8]
                if len(header) < 8:
                    continue
                access_flags = struct.unpack(">H", header[6:8])[0]
                if access_flags & 0x0001:
                    found.add(name)
    return found


problems = []
checked = 0
for api in sorted(glob.glob("coui-*/api/desktop/*.api")):
    module = api.split("/", 1)[0]
    aar = f"{module}/build/outputs/aar/{module}.aar"
    if not os.path.isfile(aar):
        problems.append(f"{module}: no Android AAR at {aar}; assemble it before running this check")
        continue
    expected = dump_classes(api)
    actual = aar_classes(aar)
    if not expected:
        problems.append(f"{module}: the desktop dump {api} declares no public top-level classes")
        continue
    checked += 1
    missing = sorted(expected - actual)
    unexplained = [name for name in missing if name not in allowlist]
    if unexplained:
        problems.append(
            f"{module}: {len(unexplained)} public desktop class(es) absent from the Android AAR: "
            + ", ".join(unexplained)
        )
    print(
        f"{module}: dump={len(expected)} aar={len(actual)} shared={len(expected & actual)} "
        f"desktop-only={len(missing)} (allowlisted={len(missing) - len(unexplained)})"
    )

if problems:
    print("\nAPI platform parity failed:", file=sys.stderr)
    for problem in problems:
        print(f" - {problem}", file=sys.stderr)
    raise SystemExit(1)

print(f"API platform parity OK: {checked} module(s), desktop dump classes all present on Android")
PY
