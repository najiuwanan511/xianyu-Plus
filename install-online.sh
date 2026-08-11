#!/usr/bin/env bash

set -Eeuo pipefail

REPOSITORY="https://github.com/najiuwanan511/xianyu-Plus.git"
TARGET_DIR="${XIANYU_PLUS_INSTALL_DIR:-$HOME/xianyu-plus}"

command -v git >/dev/null 2>&1 || {
    echo "缺少 git，无法下载项目。" >&2
    exit 1
}

if [ -e "$TARGET_DIR" ]; then
    echo "安装目录已存在: $TARGET_DIR" >&2
    echo "为避免覆盖现有文件，安装已取消。" >&2
    exit 1
fi

mkdir -p "$(dirname "$TARGET_DIR")"
git clone --depth 1 "$REPOSITORY" "$TARGET_DIR"
cd "$TARGET_DIR"
./install.sh
