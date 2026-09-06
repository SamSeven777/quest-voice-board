#!/usr/bin/env bash
set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET_DIR="$PROJECT_DIR/app/src/main/assets/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-int8-2024-07-17"
TARGET_MODEL="$TARGET_DIR/model.int8.onnx"

# 检查模型是否已存在且不是 Git LFS 指针文件（正常模型 > 10MB）
if [ -f "$TARGET_MODEL" ]; then
    FILE_SIZE=$(wc -c < "$TARGET_MODEL" 2>/dev/null || stat -c %s "$TARGET_MODEL" 2>/dev/null || echo 0)
    if [ "$FILE_SIZE" -gt 10485760 ]; then
        echo "=== [SenseVoice ASR] 离线模型已就绪 ($((FILE_SIZE / 1024 / 1024)) MB)，跳过下载 ==="
        exit 0
    fi
fi

mkdir -p "$TARGET_DIR"
MODEL_NAME="sherpa-onnx-sense-voice-zh-en-ja-ko-yue-int8-2024-07-17"
ARCHIVE_NAME="${MODEL_NAME}.tar.bz2"
UPSTREAM_URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/${ARCHIVE_NAME}"

echo "=== [SenseVoice ASR] 正在从官方源下载端侧模型 (~228 MB) ==="
echo "下载地址: $UPSTREAM_URL"

TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT

if command -v curl >/dev/null 2>&1; then
    curl -L --progress-bar "$UPSTREAM_URL" -o "$TMP_DIR/$ARCHIVE_NAME"
elif command -v wget >/dev/null 2>&1; then
    wget -q --show-progress "$UPSTREAM_URL" -O "$TMP_DIR/$ARCHIVE_NAME"
else
    echo "错误：未检测到 curl 或 wget，请先安装下载工具"
    exit 1
fi

echo "=== 正在解压模型文件到 assets 目录 ==="
tar -xjf "$TMP_DIR/$ARCHIVE_NAME" -C "$TMP_DIR"
cp "$TMP_DIR/$MODEL_NAME/model.int8.onnx" "$TARGET_MODEL"

echo "=== SenseVoice 模型配置完成！==="
