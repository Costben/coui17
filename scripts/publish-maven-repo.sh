#!/usr/bin/env bash
# 将本机 COUI 17 的 1.2.0-SNAPSHOT 产物发布到 Costben/coui17 的单提交孤儿分支 `maven-repo`（coui-repo/）。
# 用法：
#   ./scripts/publish-maven-repo.sh                         # 编译发布全平台本地快照，再强推至远端 maven-repo
#   ./scripts/publish-maven-repo.sh --android-only          # 仅编译发布 KMP + Android 本地快照，再强推至远端 maven-repo
#   ./scripts/publish-maven-repo.sh --no-build              # 直接提取当前 build/repository/local 强推至远端 maven-repo
#   ./scripts/publish-maven-repo.sh --no-build --extract-only <dir>  # 仅提取精简仓库至指定目录（供 CI 复用）
set -euo pipefail

unset GIT_DIR GIT_INDEX_FILE GIT_WORK_TREE GIT_PREFIX

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOCAL_REPO="${ROOT}/build/repository/local"
TARGET_BRANCH="maven-repo"
VERSION="1.2.0-SNAPSHOT"
GROUP_PATH="io/github/costben/coui17"
MODULES=(coui-core coui-ui coui-preference coui-icons coui-blur coui-squircle coui-navigation3-ui coui-nav coui-shader)

BUILD_MODE="all"
EXTRACT_ONLY_DIR=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-build)
      BUILD_MODE="none"
      shift
      ;;
    --android-only)
      BUILD_MODE="android"
      shift
      ;;
    --extract-only)
      EXTRACT_ONLY_DIR="${2:?--extract-only 需要目标目录参数}"
      shift 2
      ;;
    -h|--help)
      sed -n '2,8p' "${BASH_SOURCE[0]}"
      exit 0
      ;;
    *)
      echo "未知参数: $1" >&2
      exit 1
      ;;
  esac
done

if [[ "${BUILD_MODE}" == "all" ]]; then
  echo "==> 编译并发布全平台本地快照 (${VERSION}) ..."
  (cd "${ROOT}" && ./gradlew publishAllToLocalRepository -ProllingSnapshot --no-configuration-cache --console=plain)
elif [[ "${BUILD_MODE}" == "android" ]]; then
  tasks=()
  for module in "${MODULES[@]}"; do
    tasks+=(":${module}:publishKotlinMultiplatformPublicationToLocalRepository")
    tasks+=(":${module}:publishAndroidPublicationToLocalRepository")
  done
  echo "==> 编译并发布本地 Android 快照 (${VERSION}) ..."
  (cd "${ROOT}" && ./gradlew "${tasks[@]}" -ProllingSnapshot --console=plain)
fi

if [[ ! -d "${LOCAL_REPO}/${GROUP_PATH}" ]]; then
  echo "找不到本地仓库目录: ${LOCAL_REPO}/${GROUP_PATH}" >&2
  exit 1
fi

TMP_DIR=""
if [[ -n "${EXTRACT_ONLY_DIR}" ]]; then
  REPO_OUT="${EXTRACT_ONLY_DIR}"
  rm -rf "${REPO_OUT}"
  mkdir -p "${REPO_OUT}"
else
  TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/coui17-maven-XXXXXX")"
  trap 'rm -rf "${TMP_DIR}"' EXIT
  REPO_OUT="${TMP_DIR}/coui-repo"
fi

echo "==> 提取精简全平台快照（仅保留最新时间戳、剔除 sources/javadoc）..."
python3 - "${LOCAL_REPO}" "${REPO_OUT}" "${VERSION}" "${GROUP_PATH}" "${MODULES[@]}" <<'PY'
import os
import shutil
import sys
import xml.etree.ElementTree as ET

src_root = os.path.join(sys.argv[1], *sys.argv[4].split("/"))
dst_root = os.path.join(sys.argv[2], *sys.argv[4].split("/"))
version = sys.argv[3]
required_roots = sys.argv[5:]

artifacts = sorted(
    entry for entry in os.listdir(src_root)
    if os.path.isdir(os.path.join(src_root, entry, version))
)
missing_roots = [m for m in required_roots if m not in artifacts]
if missing_roots:
    raise SystemExit(f"缺少根模块目录: {', '.join(missing_roots)}")

total_files = 0
total_bytes = 0

for art in artifacts:
    src_art = os.path.join(src_root, art)
    dst_art = os.path.join(dst_root, art)
    os.makedirs(dst_art, exist_ok=True)

    # 复制顶层 maven-metadata.xml 及其校验和
    for fn in os.listdir(src_art):
        p = os.path.join(src_art, fn)
        if os.path.isfile(p) and fn.startswith("maven-metadata.xml"):
            shutil.copy2(p, os.path.join(dst_art, fn))
            total_files += 1
            total_bytes += os.path.getsize(p)

    src_ver = os.path.join(src_art, version)
    dst_ver = os.path.join(dst_art, version)
    os.makedirs(dst_ver, exist_ok=True)

    meta_path = os.path.join(src_ver, "maven-metadata.xml")
    tree = ET.parse(meta_path)
    root = tree.getroot()
    snap = root.find("versioning/snapshot")
    if snap is None:
        raise SystemExit(f"无法解析快照时间戳: {meta_path}")
    ts = snap.findtext("timestamp")
    bn = snap.findtext("buildNumber")
    base_ver = version.removesuffix("-SNAPSHOT")
    latest_prefix = f"{art}-{base_ver}-{ts}-{bn}"

    for fn in os.listdir(src_ver):
        p = os.path.join(src_ver, fn)
        if not os.path.isfile(p):
            continue
        if fn.startswith("maven-metadata.xml"):
            keep = True
        elif fn.startswith(latest_prefix):
            # 跳过 sources / javadoc，大幅缩小孤儿分支体积
            if "-sources." in fn or "-javadoc." in fn:
                keep = False
            else:
                keep = True
        else:
            keep = False

        if keep:
            shutil.copy2(p, os.path.join(dst_ver, fn))
            total_files += 1
            total_bytes += os.path.getsize(p)

print(f"已提取 {len(artifacts)} 个坐标，共 {total_files} 个文件，{total_bytes / 1024 / 1024:.2f} MiB")
PY

if [[ -n "${EXTRACT_ONLY_DIR}" ]]; then
  exit 0
fi

REMOTE_URL="$(git -C "${ROOT}" remote get-url origin)"
HEAD_SHORT="$(git -C "${ROOT}" rev-parse --short HEAD)"
DIRTY=""
if ! git -C "${ROOT}" diff --quiet || ! git -C "${ROOT}" diff --cached --quiet; then
  DIRTY="-dirty"
fi

echo "==> 创建孤儿提交并推送到 ${REMOTE_URL} (${TARGET_BRANCH}) ..."
git -C "${TMP_DIR}" init -q -b "${TARGET_BRANCH}"
git -C "${TMP_DIR}" remote add origin "${REMOTE_URL}"
git -C "${TMP_DIR}" add coui-repo
git -C "${TMP_DIR}" commit -q -m "chore(maven): 更新 COUI 17 快照 (${HEAD_SHORT}${DIRTY})"
git -C "${TMP_DIR}" \
  -c http.version=HTTP/1.1 \
  -c http.postBuffer=524288000 \
  -c http.lowSpeedLimit=0 \
  -c http.lowSpeedTime=999999 \
  push --force origin "${TARGET_BRANCH}"

SHA="$(git -C "${TMP_DIR}" rev-parse HEAD)"
echo ""
echo "✅ 推送完成！"
echo "   分支: ${TARGET_BRANCH} (${SHA:0:12})"
echo "   地址: https://raw.githubusercontent.com/Costben/coui17/${TARGET_BRANCH}/coui-repo"
