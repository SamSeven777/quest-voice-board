#!/usr/bin/env bash
set -e
if [ -z "$1" ]; then
    ATTACHED=$(adb devices 2>/dev/null | grep -w "device" | awk '{print $1}' | head -n 1)
    if [ -n "$ATTACHED" ]; then
        DEVICE="$ATTACHED"
    else
        DEVICE="172.17.166.241:5555"
    fi
else
    DEVICE="$1"
fi
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APK=""
for candidate in \
    "$PROJECT_DIR/dist/quest-voice-board-release.apk" \
    "$PROJECT_DIR/dist/quest-voice-board-debug.apk" \
    "$PROJECT_DIR/app/build/outputs/apk/release/app-release.apk" \
    "$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk" \
    $(find "$PROJECT_DIR/dist" "$PROJECT_DIR/app/build/outputs/apk" -name "*.apk" 2>/dev/null | sort -r); do
    if [ -f "$candidate" ]; then
        APK="$candidate"
        break
    fi
done

if [ -z "$APK" ]; then
    echo "错误：未找到构建好的安装包，请先执行 ./scripts/build.sh 或通过 Android Studio 编译"
    exit 1
fi
echo "使用安装包：$APK"

echo "=== [3/3] 正在通过 Wi-Fi 连接 Quest 3 ($DEVICE) 并安装 ==="
adb connect "$DEVICE" 2>/dev/null || true

PKG="xyz.sam7.questvoiceboard"

echo "正在推送安装..."
# 清理旧包名残留（如存在）
adb -s "$DEVICE" uninstall com.k2fsa.sherpa.onnx.simulate.streaming.asr 2>/dev/null || true

if ! adb -s "$DEVICE" install --no-incremental -r "$APK"; then
    echo "检测到安装冲突，正在卸载旧版本并重新安装..."
    adb -s "$DEVICE" uninstall "$PKG" 2>/dev/null || true
    adb -s "$DEVICE" install --no-incremental "$APK"
fi

echo "正在授予录音、通知与系统设置权限..."
adb -s "$DEVICE" shell pm grant "$PKG" android.permission.RECORD_AUDIO
adb -s "$DEVICE" shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS 2>/dev/null || true
adb -s "$DEVICE" shell pm grant "$PKG" android.permission.WRITE_SECURE_SETTINGS 2>/dev/null || true
adb -s "$DEVICE" shell appops set "$PKG" RECORD_AUDIO allow

echo "正在配置并启用无障碍自动粘贴服务..."
SERVICE="${PKG}/.VoiceAccessibilityService"
EXISTING=$(adb -s "$DEVICE" shell settings get secure enabled_accessibility_services 2>/dev/null | tr -d '\r\n')
if [ "$EXISTING" = "null" ] || [ -z "$EXISTING" ]; then
    adb -s "$DEVICE" shell settings put secure enabled_accessibility_services "$SERVICE"
elif [[ "$EXISTING" != *"$SERVICE"* ]]; then
    adb -s "$DEVICE" shell settings put secure enabled_accessibility_services "${EXISTING}:${SERVICE}"
fi
adb -s "$DEVICE" shell settings put secure accessibility_enabled 1

echo "正在启动应用..."
adb -s "$DEVICE" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1

echo "=== 部署完成！请戴上头显体验 ==="
