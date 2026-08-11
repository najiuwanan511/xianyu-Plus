#!/usr/bin/env bash

set -Eeuo pipefail

if [ "${1:-}" != "--yes" ]; then
    echo "此脚本只用于删除旧仓库安装，并会永久删除其数据库和全部配置。" >&2
    echo "确认删除请重新执行并传入 --yes。" >&2
    exit 1
fi

if [ "$(id -u)" -ne 0 ]; then
    echo "请使用 root 权限执行。" >&2
    exit 1
fi

is_current_repository() {
    local directory="$1"
    local remote

    [ -d "$directory/.git" ] || return 1
    remote="$(git -C "$directory" remote get-url origin 2>/dev/null || true)"
    printf '%s\n' "$remote" | grep -Eqi 'github\.com[:/]najiuwanan511/xianyu-Plus(\.git)?$'
}

declare -a install_dirs=(
    /root/xianyu-plus
    /root/xianyu-Plus
    /root/xianyusmart
    /root/XianYuSmart
)

# New and legacy releases share Docker names, so repository origin is the
# reliable guard against accidentally deleting an already migrated install.
for directory in "${install_dirs[@]}"; do
    if is_current_repository "$directory"; then
        echo "检测到新仓库项目，已拒绝删除: $directory" >&2
        echo "此命令只能在尚未安装新版本的旧项目服务器上执行。" >&2
        exit 1
    fi
done

for project in xianyu-plus xianyusmart; do
    docker ps -aq --filter "label=com.docker.compose.project=$project" | xargs -r docker rm -f
    docker volume ls -q --filter "label=com.docker.compose.project=$project" | xargs -r docker volume rm
done

docker volume rm \
    xianyu-plus-mysql-data xianyu-plus-app-data xianyu-plus-app-logs \
    xianyusmart_mysql-data xianyusmart_app-data xianyusmart_app-logs \
    2>/dev/null || true

docker network rm xianyu-plus xianyusmart_xianyusmart 2>/dev/null || true

systemctl disable --now xianyu-plus-update.path 2>/dev/null || true
rm -f /etc/systemd/system/xianyu-plus-update.service /etc/systemd/system/xianyu-plus-update.path
rm -rf /etc/xianyu-plus /usr/local/lib/xianyu-plus
rm -rf "${install_dirs[@]}"
systemctl daemon-reload

echo "旧版 XianYuPlus/XianYuSmart 已彻底清理完成。"
