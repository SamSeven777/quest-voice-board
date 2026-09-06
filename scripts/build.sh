#!/usr/bin/env bash
set -e
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"

mkdir -p dist

# 确保离线 ASR 模型已就绪
"$PROJECT_DIR/scripts/download-models.sh"

BUILD_TYPE="${1:-debug}"
BUILD_TASK="assembleDebug"
if [ "$BUILD_TYPE" = "release" ]; then
    BUILD_TASK="assembleRelease"
fi

echo "=== [1/2] 正在执行 Android 原生编译 (Gradle: $BUILD_TASK) ==="

if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    ./gradlew "$BUILD_TASK"

    APK_SRC=$(find app/build/outputs/apk -name "*.apk" | sort -r | head -n 1)
    if [ -n "$APK_SRC" ]; then
        DEST_NAME="quest-voice-board-${BUILD_TYPE}.apk"
        cp "$APK_SRC" "dist/$DEST_NAME"
        echo "=== [2/2] 构建成功！产物已复制到：dist/$DEST_NAME ==="
        exit 0
    else
        echo "错误：未找到生成的 APK 产物"
        exit 1
    fi
else
    echo "错误：未找到 gradlew，请确保在项目根目录下执行"
    exit 1
fi
