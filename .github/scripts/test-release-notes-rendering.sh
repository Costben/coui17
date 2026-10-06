#!/usr/bin/env bash
# Copyright 2026, compose-coui-ui contributors
# SPDX-License-Identifier: Apache-2.0
#
# Exercises `render-release-notes.sh` against throwaway copy, so both halves of its job are proven:
# the four version shapes it has to stamp, and every format rule it claims to enforce.
#
# The regression this guards against is a rule that rejects copy it should accept — the guard runs
# before anything is uploaded, so a false positive blocks the whole release. The attribution check
# used to match a bare `bot` anywhere in the body, which also matched ordinary prose such as "both".
#
# usage: test-release-notes-rendering.sh
set -euo pipefail

cd "$(dirname "$0")/../.."

script=".github/scripts/render-release-notes.sh"
[ -f "$script" ] || { echo "FAIL: missing $script" >&2; exit 1; }

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

notes="$work/notes"
mkdir -p "$notes"
# The drill renders against its own copy directory, never the notes committed for the next release.
export COUI_RELEASE_NOTES_DIR="$notes"

out="$work/out"

# Writes a well-formed copy for <version> with <count> bullets. Extra bullets are appended after the
# caller-provided ones so a body can be made deliberately over- or under-length.
write_copy() {  # <version> <bullet-count> [extra-line]
  local version="$1" count="$2" extra="${3:-}"
  {
    echo "# COUI v$version - 测试版本说明"
    echo
    echo "## COUI v$version"
    echo
    local i=0
    while [ "$i" -lt "$count" ]; do
      echo "- 变更条目 $((i + 1))"
      i=$((i + 1))
    done
    if [ -n "$extra" ]; then
      echo "$extra"
    fi
  } > "$notes/v$version.md"
}

expect_ok() {  # <version> <description>
  local version="$1" desc="$2"
  if ! bash "$script" "$version" "$out" > "$work/last.log" 2>&1; then
    echo "FAIL: expected '$version' to render ($desc)" >&2
    cat "$work/last.log" >&2
    exit 1
  fi
  echo "PASS: rendered $version ($desc)"
}

expect_fail() {  # <version> <description>
  local version="$1" desc="$2"
  if bash "$script" "$version" "$out" > "$work/last.log" 2>&1; then
    echo "FAIL: expected '$version' to be rejected ($desc)" >&2
    exit 1
  fi
  echo "PASS: rejected $version ($desc)"
}

assert_title() {  # <expected>
  local actual
  actual="$(cat "$out/title.txt")"
  [ "$actual" = "$1" ] || { echo "FAIL: title is '$actual', expected '$1'" >&2; exit 1; }
}

assert_heading() {  # <expected>
  grep -qxF "$1" "$out/body.md" || {
    echo "FAIL: body does not carry the heading '$1'" >&2
    cat "$out/body.md" >&2
    exit 1
  }
}

echo "== Every version shape must be stamped with the version being released =="
write_copy 9.9.9 3
expect_ok 9.9.9 "the final release"
assert_title "COUI v9.9.9 - 测试版本说明"
assert_heading "## COUI v9.9.9"

for stage in alpha1 beta2 rc1; do
  # No copy exists for the pre-release, so the base file is reused and re-stamped.
  expect_ok "9.9.9-$stage" "the base copy reused for a pre-release"
  assert_title "COUI v9.9.9-$stage - 测试版本说明"
  assert_heading "## COUI v9.9.9-$stage"
done

echo "== A pre-release with its own copy is used as-is =="
write_copy 9.9.9-rc2 3
expect_ok 9.9.9-rc2 "a pre-release that ships its own copy"
assert_title "COUI v9.9.9-rc2 - 测试版本说明"
assert_heading "## COUI v9.9.9-rc2"

echo "== Ordinary prose must not trip the attribution check =="
# `both`, `robotics` and `bottle` all contain the substring a bare `bot` pattern would match.
write_copy 9.9.9 3 "- both hosts 均支持，robotics 与 bottle 之类的词不应触发误报"
expect_ok 9.9.9 "a bullet containing 'both' / 'robotics' / 'bottle'"

echo "== Attribution and signature lines must still be rejected =="
for line in "Co-Authored-By: someone <someone@example.com>" \
            "Co-authored-by: someone" \
            "Generated with an assistant" \
            "🤖 由助手生成" \
            "该版本由 dependabot 之外的一个 bot 提交"; do
  write_copy 9.9.9 3 "- $line"
  expect_fail 9.9.9 "a body carrying '$line'"
done

echo "== The bullet-count rule must hold =="
write_copy 9.9.9 2
expect_fail 9.9.9 "a body with only two bullets"
write_copy 9.9.9 6
expect_ok 9.9.9 "a body at the upper limit of six bullets"
write_copy 9.9.9 7
expect_fail 9.9.9 "a body with seven bullets"

echo "== The title and heading rules must hold =="
{
  echo "# COUI v9.9.9"
  echo
  echo "## COUI v9.9.9"
  echo
  echo "- 一"
  echo "- 二"
  echo "- 三"
} > "$notes/v9.9.9.md"
expect_fail 9.9.9 "a title without the ' - <description>' suffix"

write_copy 9.9.9 3
{
  echo "# COUI v9.9.9 - 测试版本说明"
  echo
  echo "## 没有版本标题"
  echo
  echo "- 一"
  echo "- 二"
  echo "- 三"
} > "$notes/v9.9.9.md"
expect_fail 9.9.9 "a body without a '## COUI v<version>' heading"

echo "== A missing copy must be reported, not rendered empty =="
rm -f "$notes/v9.9.9.md"
expect_fail 9.9.9 "a version with no copy at all"
expect_fail 9.9.9-alpha1 "a pre-release with no copy and no base copy"

echo "All release-copy rendering scenarios behaved as expected."
