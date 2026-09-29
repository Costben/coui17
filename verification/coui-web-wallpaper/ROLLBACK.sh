#!/bin/sh
set -eu

target=${1:?usage: ROLLBACK.sh TARGET BASELINE}
baseline=${2:?usage: ROLLBACK.sh TARGET BASELINE}
cp "$baseline" "$target"
printf 'restored %s from %s\n' "$target" "$baseline"
