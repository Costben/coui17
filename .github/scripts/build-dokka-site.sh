#!/usr/bin/env bash
# Copyright 2026, compose-coui-ui contributors
# SPDX-License-Identifier: Apache-2.0
#
# Collects the Dokka HTML output of every published module into docs/public/dokka
# so the documentation site exposes the API of all nine modules behind the stable
# /coui/dokka/index.html entry point.
set -euo pipefail

modules=(
  coui-core
  coui-ui
  coui-preference
  coui-icons
  coui-blur
  coui-squircle
  coui-shader
  coui-nav
  coui-navigation3-ui
)

out=./docs/public/dokka
rm -rf "$out"
mkdir -p "$out"

missing=()
for module in "${modules[@]}"; do
  if [ -d "./$module/build/dokka/html" ]; then
    mkdir -p "$out/$module"
    cp -R "./$module/build/dokka/html/." "$out/$module/"
  else
    missing+=("$module")
  fi
done

# A module that silently produces no HTML would ship a documentation site whose API reference has a
# hole in it, so treat it as the failure it is.
if [ "${#missing[@]}" -ne 0 ]; then
  echo "::error::no Dokka HTML for: ${missing[*]}" >&2
  echo "FAIL: no Dokka HTML for: ${missing[*]}" >&2
  exit 1
fi

{
  echo '<!doctype html><html lang="en"><head><meta charset="utf-8">'
  echo '<meta name="viewport" content="width=device-width,initial-scale=1">'
  echo '<title>COUI API documentation</title>'
  echo '<style>body{font-family:system-ui,sans-serif;max-width:42rem;margin:4rem auto;padding:0 1rem;line-height:1.6}'
  echo 'li{margin:.35rem 0}code{background:#8881;padding:.1rem .3rem;border-radius:.25rem}</style>'
  echo '</head><body>'
  echo '<h1>COUI API documentation</h1>'
  echo '<p>Generated with Dokka. Each published module is documented separately:</p><ul>'
  for module in "${modules[@]}"; do
    if [ -d "$out/$module" ]; then
      echo "<li><a href=\"$module/index.html\"><code>$module</code></a></li>"
    fi
  done
  echo '</ul></body></html>'
} > "$out/index.html"

found=$(find "$out" -maxdepth 2 -name index.html | wc -l | tr -d ' ')
echo "Dokka site assembled: $found index.html file(s) under $out"
