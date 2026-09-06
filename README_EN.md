# Quest Voice Board 🎙️

<p align="center">
  <a href="README.md">简体中文</a> | <b>English</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Meta%20Quest%203%20%7C%20Horizon%20OS-blue?logo=meta" alt="Platform" />
  <img src="https://img.shields.io/badge/ASR-SenseVoice%20Small%20(int8)-orange" alt="ASR Engine" />
  <img src="https://img.shields.io/badge/VAD-Silero%20VAD-green" alt="VAD Engine" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=android" alt="Compose" />
  <img src="https://img.shields.io/badge/License-Apache%202.0-lightgrey" alt="License" />
</p>

A dedicated **100% on-device, offline, ultra-fast bilingual (Chinese & English) voice dictation board** tailored for **Meta Quest 3 (Horizon OS)**.

Powered by Alibaba's open-source **SenseVoice** multilingual ASR model and **Silero VAD** endpoint detector. Featuring an independent 2D floating panel, global physical hotkey toggle, and silent accessibility auto-paste, it fundamentally solves the pain points of slow virtual keyboard typing, fatigue from controllers/hand gestures, and cloud latency/privacy issues in VR/MR workflows.

---

## 💡 Why Quest Voice Board?

When browsing the web, chatting in Discord, taking notes in Obsidian, or typing in terminals on Meta Quest 3:
1. **Virtual Keyboard Pain**: Hand gestures or laser pointer raycasts are slow, imprecise, and exhausting;
2. **Code-Switching Failure**: Daily conversations and developer work often mix technical terms (*"Check this GitHub PR"*, *"Search Three.js documentation"*), which native voice inputs fail to transcribe;
3. **Cloud Latency & Privacy Concerns**: Online cloud-based ASR incurs network roundtrips, timeouts in weak Wi-Fi, and privacy risks.

**Quest Voice Board** harnesses the on-device NPU/CPU compute of the Snapdragon XR2 Gen 2 platform to deliver **zero network dependency, 30~50ms ultra-low latency, seamless English/Chinese code-switching, and hands-free automatic pasting**.

---

## 🌟 Key Features

- 🚀 **Top-Tier Multilingual & Code-Switching (中英混说)**
  - Natively equipped with Alibaba's SenseVoice-Small int8 model. Exceptional accuracy with technical jargon, abbreviations, and mixed Chinese-English speech. Also supports Cantonese, Japanese, and Korean.
- ⚡ **Millisecond On-Device Latency (Snapdragon XR2 Gen 2)**
  - Non-autoregressive Transformer architecture delivers full-sentence inference in **30~50ms** (RTF < 0.05). Zero hallucination loops and minimal CPU heat / battery consumption.
- 🎯 **Hands-Free Accessibility Auto-Paste**
  - Pausing speech triggers instant transcription. The background accessibility service automatically traverses active windows, detects the focused input field (system browser, web apps, editors, terminal), and silently pastes the text without any manual click.
  - Automatically syncs with Android system clipboard (`ClipData`).
- 🔘 **Global Physical Hotkey (Double-tap Volume Down)**
  - In **any** VR app, browser, or 2D window, **double-click the headset's Volume Down button** to toggle dictation on/off globally.
  - Consumes the double-click event to prevent accidental system volume changes; accompanied by floating CJK/English status toasts (`Listening...` / `Paused`).
- 🪟 **Meta Quest 3 Dedicated Floating Experience**
  - Crafted with **Jetpack Compose** with a dark translucent UI and optimal compact ratio (360dp × 220dp) to dock neatly beside primary workspace windows.
  - Configured with `com.oculus.vr.focusaware` so it stays alive alongside other windows without being frozen by Horizon OS.
  - Hardened with `singleTask` and full configuration change filters, ensuring smooth window resizing without reloading the 228MB neural model into RAM.
- 🧹 **Smart Punctuation Filtering**
  - Strips redundant full-width Chinese punctuation so search queries, chat messages, and code snippets flow naturally into input fields.
- 🌐 **Full Internationalization (i18n)**
  - UI and toasts automatically adapt to your headset's system language (English / Chinese).

---

## 🎮 VR Workflow & Interaction

```text
[Click input box in target app] (e.g. Browser, Discord, Obsidian)
                    ↓
[Double-tap Volume Down] or [Tap Mic Button] ──→ Toast: "Listening..."
                    ↓
[Speak naturally] (e.g., "Search Three.js documentation for shaders")
                    ↓
[Natural pause] ──→ Silero VAD detects end ──→ SenseVoice 30ms inference
                    ↓
[Text automatically pasted into field] ──→ Clipboard synced!
```

---

## 📥 Installation Guide

### Method 1: Download from GitHub Releases (Recommended for users)
1. Go to [Releases](https://github.com/SamSeven777/quest-voice-board/releases) and download the latest `quest-voice-board-vX.X.X.apk`.
2. Drag and drop the APK into **SideQuest** or use any APK installer to install it onto your Quest 3.
3. **Grant Permissions (First Launch)**:
   - Put on your headset and open **Quest Voice Board** under "Unknown Sources".
   - Allow **Microphone permission**.
   - If the banner says `Accessibility Inactive (Tap to Fix)`, click it to enable the service in System Settings, or run this one-liner via ADB from your PC:
     ```bash
     adb shell pm grant com.k2fsa.sherpa.onnx.simulate.streaming.asr android.permission.RECORD_AUDIO
     adb shell pm grant com.k2fsa.sherpa.onnx.simulate.streaming.asr android.permission.WRITE_SECURE_SETTINGS
     adb shell settings put secure enabled_accessibility_services com.k2fsa.sherpa.onnx.simulate.streaming.asr/com.k2fsa.sherpa.onnx.simulate.streaming.asr.VoiceAccessibilityService
     adb shell settings put secure accessibility_enabled 1
     ```

---

### Method 2: Wireless One-Click Auto Deployment (For developers)
If your computer and Quest 3 are on the same Wi-Fi network (or connected via USB):

```bash
# Auto-detects connected Quest device, installs APK, silently grants permissions, enables accessibility, and launches the app:
./scripts/deploy.sh

# Or specify your Quest 3 IP:Port directly:
./scripts/deploy.sh 192.168.1.100:5555
```

---

## 🛠️ Build from Source

> [!TIP]
> **Ultra-light repository**: The Git repository contains only clean source code (~3 MB) and does not store huge binary weights. On first build, the script or Gradle task automatically downloads and caches the official SenseVoice model (~228 MB).
> ```bash
> git clone https://github.com/SamSeven777/quest-voice-board.git
> cd quest-voice-board
> ```

### 1. Using Android Studio
- Open the project directory in Android Studio (Ladybug 2024.2+ / Koala / Meerkat).
- Sync with **JDK 21** and **Android SDK 34**. When building or running, Gradle automatically downloads the model.

### 2. Command-Line Build
```bash
# Build Debug APK (outputs to dist/quest-voice-board-debug.apk)
./scripts/build.sh debug

# Build Release APK (outputs to dist/quest-voice-board-release.apk)
./scripts/build.sh release
```

---

## 📁 Project Architecture

```text
quest-voice-board/
├── app/                                       # Core Android module
│   ├── build.gradle.kts                       # Build configuration (auto-model download + Compose + JNI)
│   └── src/main/
│       ├── AndroidManifest.xml                # Horizon OS floating window, key filtering, & a11y declaration
│       ├── assets/
│       │   ├── sherpa-onnx-sense-voice-.../   # SenseVoice int8 weights (auto-downloaded on build)
│       │   ├── silero_vad.onnx                # Silero VAD neural network
│       │   └── lexicon.txt / *.fst            # FST text normalization lexicons
│       ├── jniLibs/arm64-v8a/                 # Optimized sherpa-onnx C++ JNI libraries
│       └── java/com/k2fsa/sherpa/onnx/
│           ├── Vad.kt / OfflineRecognizer.kt  # JNI bindings
│           └── simulate/streaming/asr/
│               ├── MainActivity.kt            # Compose UI entrypoint & lifecycle
│               ├── VoiceInputController.kt    # Audio recording + VAD segmentation + ASR decoding
│               ├── VoiceAccessibilityService.kt # Global double-tap hotkey + multi-window auto-paste
│               └── screens/HomeScreen.kt      # Floating dark translucent VR panel
├── scripts/
│   ├── download-models.sh                     # Upstream SenseVoice model download & validation script
│   ├── build.sh                               # Automatic build script outputs to dist/
│   └── deploy.sh                              # One-click wireless ADB deployment & permission grant
├── .github/workflows/
│   ├── build.yml                              # CI build & APK validation workflow
│   └── release.yml                            # Git Tag (v*) automated GitHub Release workflow
├── settings.gradle.kts                        # Gradle repositories & project setup
└── CHANGELOG.md                               # Version changelog
```

---

## 🤝 Acknowledgments

- [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx): Next-gen Kaldi speech recognition runtime
- [SenseVoice](https://github.com/FunAudioLLM/SenseVoice): Alibaba Tongyi Lab's multilingual speech foundation model
- [Silero VAD](https://github.com/snakers4/silero-vad): Industrial-grade, high-performance voice activity detector

---

## 📄 License

This project is licensed under the [Apache License 2.0](LICENSE).
