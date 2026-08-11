#!/usr/bin/env bash

set -Eeuo pipefail

if [ "${1:-}" != "--yes" ]; then
    echo "此脚本会永久删除 XianYuPlus/XianYuSmart 的容器、数据卷、配置和项目目录。" >&2
    echo "确认删除请重新执行并传入 --yes。" >&2
    exit 1
fi

if [ "$(id -u)" -ne 0 ]; then
    echo "请使用 root 权限执行。" >&2
    exit 1
fi

for project in xianyu-plus xianyusmart; do
    docker ps -aq --filter "label=com.docker.compose.project=$project" | xargs -r docker rm -f
    docker volume ls -q --filter "label=com.docker.compose.project=$project" | xargs -r docker volume rm
done

docker volume rm \
    xianyu-plus-mysql-data xianyu-plus-app-data xianyu-plus-app-logs \
    xianyusmart_mysql-data xianyusmart_app-data xianyusmart_app-logs \
    2>/dev/null || true

systemctl disable --now xianyu-plus-update.path 2>/dev/null || true
rm -f /etc/systemd/system/xianyu-plus-update.service /etc/systemd/system/xianyu-plus-update.path
rm -rf /etc/xianyu-plus /usr/local/lib/xianyu-plus /root/xianyu-plus /root/xianyu-Plus
systemctl daemon-reload

echo "XianYuPlus 已彻底清理完成。"
