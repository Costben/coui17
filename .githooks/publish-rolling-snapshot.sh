#!/usr/bin/env bash
# Publishes the Android-facing coordinates to build/repository/local as the rolling `1.2.0-SNAPSHOT`,
# overwriting the previous one, so apps on this machine that depend on that coordinate build against
# the latest commit without anyone bumping a version.
#
# The post-commit / post-merge / post-rewrite hooks next to this file call it with --background once
# the hooks path is enabled:
#
#   git config core.hooksPath .githooks
#
# Run it by hand to publish in the foreground; --force republishes even when no library path changed.
# Gradle builds the working tree, so uncommitted edits to library sources are published too. The
# publish wipes build/repository/local first, so an app build running at that moment cannot resolve
# COUI until it finishes.
# Log of background runs: build/rolling-snapshot.log.
set -euo pipefail

# A hook runs with GIT_DIR / GIT_INDEX_FILE pointing at whatever git was doing; none of that may leak
# into the Gradle build or the detached child.
unset GIT_DIR GIT_INDEX_FILE GIT_WORK_TREE GIT_PREFIX

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOCK="${ROOT}/build/rolling-snapshot.lock"
PENDING="${ROOT}/build/rolling-snapshot.pending"
# Kept outside build/repository/local: every local publish wipes that directory first.
STAMP="${ROOT}/build/rolling-snapshot.commit"

# Same set as publish-local in the consumer apps: the KMP root coordinate plus its Android variant.
MODULES=(coui-core coui-ui coui-preference coui-icons coui-blur coui-squircle coui-navigation3-ui coui-nav coui-shader)
# Mirrors the path filter of .github/workflows/publish.yml.
LIBRARY_PATHS=("${MODULES[@]}" build-plugins gradle settings.gradle.kts build.gradle.kts gradle.properties)

mkdir -p "${ROOT}/build"

if [[ "${1:-}" == "--background" ]]; then
  # Fully detached: git must not wait on our stdout, and closing the terminal must not kill the build.
  nohup "${BASH_SOURCE[0]}" </dev/null >>"${ROOT}/build/rolling-snapshot.log" 2>&1 &
  exit 0
fi

FORCE=0
[[ "${1:-}" == "--force" ]] && FORCE=1

publish_once() {
  local head last
  head="$(git -C "${ROOT}" rev-parse HEAD)"
  if [[ "${FORCE}" == "0" && -f "${STAMP}" ]]; then
    last="$(cat "${STAMP}")"
    if git -C "${ROOT}" cat-file -e "${last}^{commit}" 2>/dev/null \
      && git -C "${ROOT}" diff --quiet "${last}" "${head}" -- "${LIBRARY_PATHS[@]}"; then
      echo "== $(date '+%F %T') ${head:0:8}: no library change since ${last:0:8}, skipped"
      return
    fi
  fi

  local tasks=()
  for module in "${MODULES[@]}"; do
    tasks+=(":${module}:publishKotlinMultiplatformPublicationToLocalRepository")
    tasks+=(":${module}:publishAndroidPublicationToLocalRepository")
  done

  echo "== $(date '+%F %T') ${head:0:8}: publishing 1.2.0-SNAPSHOT"
  (cd "${ROOT}" && ./gradlew "${tasks[@]}" -ProllingSnapshot --console=plain --quiet)
  echo "${head}" >"${STAMP}"
  echo "== $(date '+%F %T') ${head:0:8}: published"
}

# Commits landing while a publish runs only leave a marker; the running publish loops until the
# marker stays gone, so the last commit is always the one that ends up published.
touch "${PENDING}"
if ! mkdir "${LOCK}" 2>/dev/null; then
  if [[ -f "${LOCK}/pid" ]] && kill -0 "$(cat "${LOCK}/pid")" 2>/dev/null; then
    echo "== $(date '+%F %T') publish already running (pid $(cat "${LOCK}/pid")), queued"
    exit 0
  fi
  rm -rf "${LOCK}"
  mkdir "${LOCK}"
fi
echo $$ >"${LOCK}/pid"
trap 'rm -rf "${LOCK}"' EXIT

while [[ -f "${PENDING}" ]]; do
  rm -f "${PENDING}"
  publish_once
done

rm -rf "${LOCK}"
trap - EXIT
# Closes the window between the last check above and releasing the lock.
if [[ -f "${PENDING}" ]]; then
  exec "${BASH_SOURCE[0]}"
fi
