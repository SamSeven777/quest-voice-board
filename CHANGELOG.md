# Changelog

## [v1.0.0] - 2026-09-06

### 🚀 核心架构与现代化演进
- **全面重构为现代 Android 原生工程**：采用 Kotlin + Jetpack Compose + sherpa-onnx 原生 JNI 架构，基于标准 Gradle (AGP 9.4 + Kotlin 2.2) 编译，彻底取代早期逆向 smali 修包模式。
- **纯正端侧离线 ASR**：集成阿里开源 SenseVoice int8 多语言模型与 Silero VAD 端点检测引擎，提供毫秒级超快响应与高准确率中英混说（Code-Switching）能力。
- **无障碍智能静默粘贴（Auto-Paste）**：
  - 基于 Android 无障碍服务 (`VoiceAccessibilityService`)，智能查找当前活跃焦点窗口与输入框。
  - 支持 `ACTION_PASTE` 与 `ACTION_SET_TEXT` 级联填充，兼容各类原生输入框、Web 表单与复杂 UI。
  - 自动同步写入系统剪贴板 (`ClipData`)，双重保障。
- **Meta Quest 3 专属沉浸式优化**：
  - 极简深色半透明侧边栏 UI，默认 360dp × 220dp 黄金尺寸。
  - 开启 `com.oculus.vr.focusaware`，多任务并排运行时保持前台活跃，不被 Horizon OS 挂起。
  - Activity 配置 `singleTask` 与全量 `configChanges`，窗口缩放、横竖屏切换无缝平滑，不重载 ASR 引擎。
- **全局快捷物理按键支持**：
  - 双击音量下键全局快速开启/暂停听写，并消费按键事件避免误调系统音量。
  - 伴随平滑 CJK 浮动气泡 Toast 状态提示。
- **录音与断句健壮性**：
  - 停止录音时后台异步执行 VAD 尾音冲洗 (`flush`) 与剩余识别流解码，杜绝句尾吞字。
  - 启动新录音前自动排空历史采样通道。
  - Native `OfflineStream` 资源生命周期严格收口与即时释放。

### 🛠️ 工具链与 CI/CD
- 自动化一键无线部署脚本 (`scripts/deploy.sh`)：自动检测设备、静默授权录音/通知/系统设置、免手动开启无障碍服务并拉起浮窗。
- 自动化构建脚本 (`scripts/build.sh`)：支持快速编译 Debug 与 Release APK。
- GitHub Actions CI/CD：自动化测试编译与 Git Tag (`v*`) 自动发版交付。
