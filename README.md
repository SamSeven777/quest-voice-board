# Quest Voice Board 🎙️

<p align="center">
  <b>简体中文</b> | <a href="README_EN.md">English</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Meta%20Quest%203%20%7C%20Horizon%20OS-blue?logo=meta" alt="Platform" />
  <img src="https://img.shields.io/badge/ASR-SenseVoice%20Small%20(int8)-orange" alt="ASR Engine" />
  <img src="https://img.shields.io/badge/VAD-Silero%20VAD-green" alt="VAD Engine" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=android" alt="Compose" />
  <img src="https://img.shields.io/badge/Crafted%20with-Gemini-8E75B2?logo=google&logoColor=white" alt="Gemini" />
  <img src="https://img.shields.io/badge/License-Apache%202.0-lightgrey" alt="License" />
</p>

专为 **Meta Quest 3 (Horizon OS)** 深度定制的**端侧完全离线、中英双语极速语音打字板**。

基于阿里开源的 **SenseVoice** 多语言 ASR 模型与 **Silero VAD** 端点断句引擎，通过独立 2D 侧边浮窗、全局物理快捷键与无障碍智能静默粘贴，彻底解决 VR/MR 办公娱乐场景下“中英文混输困难、软键盘打字极慢、手柄/手势敲字疲劳”的长期痛点。

---

## 💡 为什么需要 Quest Voice Board？

在 Quest 3 (Horizon OS) 中使用浏览器查资料、Discord 聊天、Obsidian 记笔记或终端输入时：
1. **系统原生键盘痛苦**：手势或射线激光打字不仅慢且容易误触；
2. **中英混说无法输入**：日常技术交流常夹杂专业术语（如 *"把这个 PR merge 一下"*、*"打开 GitHub 看下 issue"*），传统输入法很难精准识别；
3. **在线 ASR 隐私与网络问题**：云端识别有网络延迟且存在隐私顾虑。

**Quest Voice Board** 依托第二代骁龙 XR2 芯片的端侧算力，实现了**零网络依赖、30~50ms 极速响应、中英无缝混说与全脱手自动粘贴**。

---

## 🌟 核心特性

- 🚀 **顶级中英混说（Code-Switching）**
  - 原生搭载阿里 SenseVoice-Small int8 多语言模型，在中英文夹杂、专业术语、英文缩写场景下的识别准确率极高。
- ⚡ **端侧毫秒级响应（Snapdragon XR2 Gen 2）**
  - 采用非自回归（Non-Autoregressive）结构，整句识别耗时仅需 **30~50ms**（RTF < 0.05），无自回归模型的幻觉复读问题，发热与电量消耗微乎其微。
- 🎯 **无障碍智能静默粘贴（Auto-Paste）**
  - 说话停顿即完成识别，后台无障碍服务自动定位当前获得焦点的目标输入框（支持系统浏览器、Web 应用、办公文档、各类第三方 2D App），直接填充文本，彻底告别手动点击复制粘贴。
  - 同步写入 Android 系统剪贴板（`ClipData`），双重保险。
- 🔘 **全局物理快捷键联动（双击音量下键）**
  - 在任何应用、浏览器或游戏画面中，**双击头显音量下键**即可全局开启/暂停听写，无需切换前台界面。
  - 自动消费双击按键事件，防止误调系统主音量；伴随优雅的 CJK 气泡 Toast 浮动状态反馈。
- 🪟 **Meta Quest 3 专属侧边栏体验**
  - 采用 **Jetpack Compose** 构建深色半透明极简 UI，预设最优黄金比例（360dp × 220dp），可优雅停靠在主工作窗口侧边。
  - 启用 `com.oculus.vr.focusaware` 焦点穿透机制，与浏览器或多任务窗口并排运行时不会被 Horizon OS 挂起冻结。
  - 优化 `singleTask` 与全量配置变更过滤，窗口缩放、位置拖拽无感平滑，不重载 ASR 引擎。
- 🧹 **智能标点过滤**
  - 自动过滤中文多余全角句读（，。！？），搜索、代码、聊天输入一气呵成不违和。

---

## 🎮 VR 使用体验与工作流

```text
[在 Quest 中打开目标应用] (如浏览器/聊天窗口，点击激活光标)
                    ↓
[双击音量下键] 或 [点击浮窗麦克风] ──→ 弹出 "听写中" 气泡
                    ↓
[自然中英文说话] (例如："帮我搜索一下 Three.js 最新版本文档")
                    ↓
[自然停顿说话] ──→ Silero VAD 自动断句 ──→ SenseVoice 30ms 极速出字
                    ↓
[文字自动秒贴到输入框] ──→ 伴随系统剪贴板更新
```

---

## 📥 安装与使用指南

### 方式 1：通过 GitHub Releases 直接安装（推荐用户）
1. 前往本仓库 [Releases 页面](https://github.com/SamSeven777/quest-voice-board/releases) 下载最新的 `quest-voice-board-vX.X.X.apk`。
2. 使用 **SideQuest** 或任意 APK 安装工具将 APK 拖拽安装至 Quest 3。
3. **权限开启（初次使用）**：
   - 戴上头显，在“未知来源应用”中打开 **Quest Voice Board**。
   - 首次启动允许**录音权限**。
   - 顶部若显示 `无障碍未激活`，点击后按提示前往系统设置开启无障碍服务；或者通过电脑执行一次 ADB 授权命令：
     ```bash
     adb shell pm grant xyz.sam7.questvoiceboard android.permission.RECORD_AUDIO
     adb shell pm grant xyz.sam7.questvoiceboard android.permission.WRITE_SECURE_SETTINGS
     adb shell settings put secure enabled_accessibility_services xyz.sam7.questvoiceboard/.VoiceAccessibilityService
     adb shell settings put secure accessibility_enabled 1
     ```

---

### 方式 2：一键无线全自动部署（推荐开发者）
如果您的电脑与 Quest 3 在同一 Wi-Fi 网络下（或通过 USB 连接）：

```bash
# 自动检测在线 Quest 设备并完成：无线连接 + 安装 + 权限授权 + 自动开启无障碍 + 启动应用
./scripts/deploy.sh

# 或者显式指定 Quest 3 的无线 IP:端口
./scripts/deploy.sh 192.168.1.100:5555
```

---

## 🛠️ 本地开发与编译

> [!TIP]
> **零 LFS 依赖，秒级克隆**：本仓库遵循开源模型最佳实践，纯代码仓库体积仅 **~3 MB**，无需配置 `git-lfs`。首次编译时，构建脚本或 Gradle 会自动从官方上游源拉取并校验 SenseVoice 离线模型，开箱即用。
> ```bash
> git clone https://github.com/SamSeven777/quest-voice-board.git
> cd quest-voice-board
> ```

### 1. 使用 Android Studio
- 使用 Android Studio (Ladybug 2024.2+ / Koala / Meerkat) 直接打开工程根目录。
- 采用 **JDK 21** 与 **Android SDK 34** 同步。在点击 Run 或 Build 时，Gradle 自动检测并下载离线模型。

### 2. 命令行自动化构建
```bash
# 构建 Debug 版本（首次执行自动拉取模型并输出至 dist/quest-voice-board-debug.apk）
./scripts/build.sh debug

# 构建 Release 版本（首次执行自动拉取模型并输出至 dist/quest-voice-board-release.apk）
./scripts/build.sh release
```

---

## 📁 模块架构

```text
quest-voice-board/
├── app/                                       # 核心 Android 原生模块
│   ├── build.gradle.kts                       # 构建脚本 (自动模型下载挂载 + Compose + JNI)
│   └── src/main/
│       ├── AndroidManifest.xml                # Horizon OS 浮窗、按键过滤与无障碍声明
│       ├── assets/
│       │   ├── sherpa-onnx-sense-voice-.../   # SenseVoice int8 权重与配置 (构建时自动拉取)
│       │   ├── silero_vad.onnx                # Silero VAD 神经网络
│       │   └── lexicon.txt / *.fst            # FST 文本归一化词典
│       ├── jniLibs/arm64-v8a/                 # sherpa-onnx 优化的 C++ JNI 动态库
│       └── java/
│           ├── com/k2fsa/sherpa/onnx/         # sherpa-onnx JNI 核心封装 (Vad / Recognizer)
│           └── xyz/sam7/questvoiceboard/
│               ├── MainActivity.kt            # Compose 界面入口与生命周期管理
│               ├── VoiceInputController.kt    # 音频流采集 + VAD 切片 + ASR 解码调度
│               ├── VoiceAccessibilityService.kt # 全局双击按键拦截 + 智能多窗口无障碍粘贴
│               └── screens/HomeScreen.kt      # 针对 VR 定制的极简深色半透明浮窗
├── scripts/
│   ├── download-models.sh                     # 官方 SenseVoice 离线模型校验与拉取脚本
│   ├── build.sh                               # 自动化模型校验、编译并打包至 dist/
│   └── deploy.sh                              # 一键无线 ADB 推送、静默授权与启动
├── .github/workflows/
│   ├── build.yml                              # CI 自动化构建与安装包校验
│   └── release.yml                            # Git Tag (v*) 自动构建并发布 GitHub Releases
├── settings.gradle.kts                        # Gradle 依赖仓库与工程声明
└── CHANGELOG.md                               # 版本更新履历
```

---

## 🤝 鸣谢与致敬

- [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx)：新一代 Kaldi 离线与流式语音识别运行时
- [SenseVoice](https://github.com/FunAudioLLM/SenseVoice)：阿里巴巴通义实验室开源的多语言语音大模型
- [Silero VAD](https://github.com/snakers4/silero-vad)：工业级高精度端侧语音活动检测模型
- [Google Gemini](https://deepmind.google/technologies/gemini/)：全流程 AI 结对编程伙伴，协同完成核心架构演进、Compose VR 交互定制、JNI 性能调优与工程化落地

---

## 📄 License

本项目基于 [Apache License 2.0](LICENSE) 协议开源。
