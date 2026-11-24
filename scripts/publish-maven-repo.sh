#!/usr/bin/env bash
# 将本机 COUI 的 Android 1.2.0-SNAPSHOT 产物一键发布到 Costben/coui17 的孤儿分支 `maven-repo`。
# 用法：
#   ./scripts/publish-maven-repo.sh            # 先编译发布本地 Android 快照，再同步推送到远端 maven-repo
#   ./scripts/publish-maven-repo.sh --no-build # 直接把当前 build/repository/local 同步推送到远端 maven-repo
set -euo pipefail

unset GIT_DIR GIT_INDEX_FILE GIT_WORK_TREE GIT_PREFIX

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOCAL_REPO="${ROOT}/build/repository/local"
REMOTE_URL="$(git -C "${ROOT}" remote get-url origin)"
TARGET_BRANCH="maven-repo"
VERSION="1.2.0-SNAPSHOT"
MODULES=(coui-core coui-ui coui-preference coui-icons coui-blur coui-squircle coui-navigation3-ui coui-nav coui-shader)

BUILD=1
for arg in "$@"; do
  case "${arg}" in
    --no-build) BUILD=0 ;;
    -h|--help)
      sed -n '2,6p' "${BASH_SOURCE[0]}"
      exit 0
      ;;
    *)
      echo "未知参数: ${arg}" >&2
      exit 1
      ;;
  esac
done

if [[ "${BUILD}" == "1" ]]; then
  tasks=()
  for module in "${MODULES[@]}"; do
    tasks+=(":${module}:publishKotlinMultiplatformPublicationToLocalRepository")
    tasks+=(":${module}:publishAndroidPublicationToLocalRepository")
  done
  echo "==> 编译并发布本地 Android 快照 (${VERSION}) ..."
  (cd "${ROOT}" && ./gradlew "${tasks[@]}" -ProllingSnapshot --console=plain)
fi

if [[ ! -d "${LOCAL_REPO}/io/github/suqi8/coui/kmp" ]]; then
  echo "找不到本地仓库目录: ${LOCAL_REPO}/io/github/suqi8/coui/kmp" >&2
  exit 1
fi

TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/coui17-maven-XXXXXX")"
trap 'rm -rf "${TMP_DIR}"' EXIT

echo "==> 提取精简 Android 快照（仅保留最新时间戳、去除 sources/javadoc/iOS）..."
python3 - "${LOCAL_REPO}" "${TMP_DIR}/coui-repo" "${VERSION}" "${MODULES[@]}" <<'PY'
import os
import shutil
import sys
import xml.etree.ElementTree as ET

src_root = os.path.join(sys.argv[1], "io", "github", "suqi8", "coui", "kmp")
dst_root = os.path.join(sys.argv[2], "io", "github", "suqi8", "coui", "kmp")
version = sys.argv[3]
modules = sys.argv[4:]

artifacts = []
for m in modules:
    artifacts.append(m)
    artifacts.append(f"{m}-android")

total_files = 0
total_bytes = 0

for art in artifacts:
    src_art = os.path.join(src_root, art)
    if not os.path.isdir(src_art):
        raise SystemExit(f"缺少模块目录: {src_art}")
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
    if not os.path.isdir(src_ver):
        raise SystemExit(f"缺少版本目录: {src_ver}")
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
            # 跳过 sources / javadoc，大幅缩小体积
            if "-sources." in fn or "-javadoc." in fn or "-kotlin-tooling-metadata." in fn:
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

HEAD_SHORT="$(git -C "${ROOT}" rev-parse --short HEAD)"
DIRTY=""
if ! git -C "${ROOT}" diff --quiet || ! git -C "${ROOT}" diff --cached --quiet; then
  DIRTY="-dirty"
fi

echo "==> 创建孤儿提交并推送到 ${REMOTE_URL} (${TARGET_BRANCH}) ..."
git -C "${TMP_DIR}" init -q -b "${TARGET_BRANCH}"
git -C "${TMP_DIR}" remote add origin "${REMOTE_URL}"
git -C "${TMP_DIR}" add coui-repo
git -C "${TMP_DIR}" commit -q -m "chore(maven): 更新 Android COUI 快照 (${HEAD_SHORT}${DIRTY})"
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
