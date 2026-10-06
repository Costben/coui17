#!/usr/bin/env bash
# Copyright 2026, compose-coui-ui contributors
# SPDX-License-Identifier: Apache-2.0
#
# Renders the GitHub Release title and body from the per-version copy committed under
# .github/release-notes/, and rejects copy that does not follow the house format:
# a `# COUI v<version> - <one-line description>` title and 3-6 user-visible bullets.
#
# The <version> argument is the version actually being released (`${TAG#v}`), including any
# -alpha/-beta/-rc suffix. When no file exists for that exact version the base version's file is
# reused, but the rendered title always carries the real version, so an alpha never ships a
# `v1.2.0` title.
#
# usage: render-release-notes.sh <version> <output-dir>
set -euo pipefail

version="${1:?usage: render-release-notes.sh <version> <output-dir>}"
out="${2:?usage: render-release-notes.sh <version> <output-dir>}"

base="${version%%-*}"
# The copy directory is overridable so the rendering drill can exercise the format checks against
# throwaway files instead of the release notes currently committed.
notes_dir="${COUI_RELEASE_NOTES_DIR:-.github/release-notes}"

notes="$notes_dir/v$version.md"
if [ ! -f "$notes" ]; then
  notes="$notes_dir/v$base.md"
fi
if [ ! -f "$notes" ]; then
  echo "::error::missing release copy $notes_dir/v$version.md (and no v$base.md fallback)" >&2
  exit 1
fi

title="$(head -n 1 "$notes")"
case "$title" in
  "# COUI v$version - "?* | "# COUI v$base - "?*) ;;
  *)
    echo "::error::$notes must start with '# COUI v$version - <one-line description>'" >&2
    exit 1
    ;;
esac

# Re-stamp the title with the version actually being released, so a reused base-version file still
# produces a title that matches the tag.
rendered="$(printf '%s\n' "$title" | sed -E "s|^# COUI v[^ ]+ - |# COUI v$version - |")"

mkdir -p "$out"
printf '%s\n' "${rendered#'# '}" > "$out/title.txt"
# The body's own `## COUI v<version>` heading is re-stamped too: a reused base-version file must not
# ship an alpha under a final version's heading.
tail -n +2 "$notes" | awk 'found || NF { found = 1; print }' \
  | sed -E "s|^## COUI v[^ ]+$|## COUI v$version|" > "$out/body.md"

if ! grep -qxF "## COUI v$version" "$out/body.md"; then
  echo "::error::$notes must carry a '## COUI v$version' heading the release body can be stamped from" >&2
  exit 1
fi

bullets="$(grep -c '^- ' "$out/body.md" || true)"
if [ "$bullets" -lt 3 ] || [ "$bullets" -gt 6 ]; then
  echo "::error::$notes must list 3 to 6 user-visible changes, found $bullets" >&2
  exit 1
fi

# `\b` is not portable across GNU and BSD grep, so the bot/robot words are anchored with a POSIX
# character class instead. A bare `bot` substring would also reject ordinary prose ("both",
# "robotics", "bottle"), which would block a release over a word that says nothing about who wrote
# the copy; the anchored form still catches a standalone bot/robot and the 🤖 signature.
if grep -qiE 'co-?authored-by|generated with|(^|[^[:alnum:]])(ro)?bots?([^[:alnum:]]|$)|🤖' "$out/body.md"; then
  echo "::error::$notes must not carry attribution or signature lines" >&2
  exit 1
fi

echo "Rendered release copy for v$version (source $notes):"
cat "$out/title.txt"
