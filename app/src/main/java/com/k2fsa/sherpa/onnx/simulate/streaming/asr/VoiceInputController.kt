package com.k2fsa.sherpa.onnx.simulate.streaming.asr

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.HomophoneReplacerConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VoiceInputController private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private var recognizer: OfflineRecognizer? = null
    private var vad: Vad? = null
    private var audioRecord: AudioRecord? = null

    private var recordingJob: Job? = null
    private var processingJob: Job? = null
    private val samplesChannel = Channel<FloatArray>(capacity = Channel.UNLIMITED)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _latestText = MutableStateFlow("")
    val latestText: StateFlow<String> = _latestText.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _initError = MutableStateFlow<String?>(null)
    val initError: StateFlow<String?> = _initError.asStateFlow()

    private val _filterPunctuation = MutableStateFlow(true)
    val filterPunctuation: StateFlow<Boolean> = _filterPunctuation.asStateFlow()

    private val clipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (_isInitialized.value) return@withContext

        try {
            Log.i(TAG, "Initializing SenseVoice ASR & Silero VAD...")

            // 1. Initialize Silero VAD
            val vadConfig = VadModelConfig(
                sileroVadModelConfig = SileroVadModelConfig(
                    model = "silero_vad.onnx",
                    threshold = 0.5f,
                    minSilenceDuration = 0.25f,
                    minSpeechDuration = 0.25f,
                    windowSize = 512,
                    maxSpeechDuration = 30.0f,
                ),
                sampleRate = SAMPLE_RATE,
                numThreads = 1,
                provider = "cpu",
                debug = false,
            )
            vad = Vad(assetManager = context.assets, config = vadConfig)

            // 2. Initialize SenseVoice Offline Recognizer
            val senseVoiceConfig = OfflineSenseVoiceModelConfig(
                model = "sherpa-onnx-sense-voice-zh-en-ja-ko-yue-int8-2024-07-17/model.int8.onnx",
                language = "", // auto-detect (zh, en, ja, ko, yue)
                useInverseTextNormalization = true,
            )
            val modelConfig = OfflineModelConfig(
                senseVoice = senseVoiceConfig,
                tokens = "sherpa-onnx-sense-voice-zh-en-ja-ko-yue-int8-2024-07-17/tokens.txt",
                numThreads = 2,
                debug = false,
                modelType = "sense_voice",
            )
            val recognizerConfig = OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = SAMPLE_RATE, featureDim = 80),
                modelConfig = modelConfig,
                hr = HomophoneReplacerConfig(
                    lexicon = "lexicon.txt",
                    ruleFsts = "itn_zh_number.fst,replace.fst",
                ),
            )
            recognizer = OfflineRecognizer(assetManager = context.assets, config = recognizerConfig)

            _isInitialized.value = true
            _initError.value = null
            Log.i(TAG, "ASR & VAD initialization complete!")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to initialize ASR Engine", t)
            _initError.value = t.localizedMessage ?: "初始化失败"
        }
    }

    @Synchronized
    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (!_isInitialized.value || _isRecording.value) return

        // 0. Drain leftover samples from any previous session
        while (samplesChannel.tryReceive().isSuccess) { /* drain */ }

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 2

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord failed to initialize")
            return
        }

        audioRecord = record
        record.startRecording()
        _isRecording.value = true

        // Update accessibility service state & toast
        VoiceAccessibilityService.isVoiceInputEnabled = true
        VoiceAccessibilityService.showToggleToast(true)

        // 1. Audio recording thread
        recordingJob = scope.launch(Dispatchers.IO) {
            val shortBuffer = ShortArray(bufferSize / 2)
            while (isActive && _isRecording.value) {
                val readCount = record.read(shortBuffer, 0, shortBuffer.size)
                if (readCount > 0) {
                    val floatArray = FloatArray(readCount) { i ->
                        shortBuffer[i] / 32768.0f
                    }
                    samplesChannel.send(floatArray)
                }
            }
        }

        // 2. VAD & recognition processing thread
        processingJob = scope.launch(Dispatchers.Default) {
            val vadEngine = vad ?: return@launch
            val asrEngine = recognizer ?: return@launch

            vadEngine.clear()

            while (isActive) {
                val samples = samplesChannel.receiveCatching().getOrNull() ?: break
                vadEngine.acceptWaveform(samples)

                while (!vadEngine.empty()) {
                    val segment = vadEngine.front()
                    asrEngine.createStream().use { stream ->
                        stream.acceptWaveform(segment.samples, SAMPLE_RATE)
                        asrEngine.decode(stream)

                        val result = asrEngine.getResult(stream)
                        val rawText = result.text.trim()
                        if (rawText.isNotEmpty()) {
                            handleTranscriptionResult(rawText)
                        }
                    }
                    vadEngine.pop()
                }
            }
        }
    }

    @Synchronized
    fun stopRecording() {
        if (!_isRecording.value) return
        _isRecording.value = false

        VoiceAccessibilityService.isVoiceInputEnabled = false
        VoiceAccessibilityService.showToggleToast(false)

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping AudioRecord", e)
        }
        audioRecord = null

        recordingJob?.cancel()
        recordingJob = null

        // Flush VAD in background to ensure any trailing speech is recognized & pasted
        scope.launch(Dispatchers.Default) {
            try {
                val vadEngine = vad
                val asrEngine = recognizer
                if (vadEngine != null && asrEngine != null) {
                    vadEngine.flush()
                    while (!vadEngine.empty()) {
                        val segment = vadEngine.front()
                        asrEngine.createStream().use { stream ->
                            stream.acceptWaveform(segment.samples, SAMPLE_RATE)
                            asrEngine.decode(stream)

                            val result = asrEngine.getResult(stream)
                            val rawText = result.text.trim()
                            if (rawText.isNotEmpty()) {
                                handleTranscriptionResult(rawText)
                            }
                        }
                        vadEngine.pop()
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error flushing VAD on stop", t)
            } finally {
                processingJob?.cancel()
                processingJob = null
                vad?.clear()
                while (samplesChannel.tryReceive().isSuccess) { /* drain */ }
            }
        }
    }

    @Synchronized
    fun toggleRecording() {
        if (_isRecording.value) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private suspend fun handleTranscriptionResult(text: String) {
        val finalText = if (_filterPunctuation.value) filterPunctuation(text) else text
        if (finalText.isEmpty()) return

        Log.i(TAG, "ASR Result: $text -> Cleaned: $finalText")
        _latestText.value = finalText

        // 1. Sync to Android system clipboard & trigger auto-paste on Main thread
        withContext(Dispatchers.Main) {
            try {
                val clip = ClipData.newPlainText("QuestVoiceBoard", finalText)
                clipboardManager.setPrimaryClip(clip)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to copy to clipboard", e)
            }

            val pasted = VoiceAccessibilityService.pasteText(finalText)
            Log.i(TAG, "Auto-paste attempted for '$finalText', success=$pasted")
        }
    }

    fun clearText() {
        _latestText.value = ""
    }

    companion object {
        private const val TAG = "VoiceInputController"
        private const val SAMPLE_RATE = 16000

        fun filterPunctuation(text: String): String {
            var cleaned = text.replace(Regex("""[，。！？、：；“”‘’（）【】《》…—～]"""), "")
            cleaned = cleaned.replace(Regex("""[!?:;]"""), "")
            cleaned = cleaned.replace(Regex("""(?<!\d)[,.]|[,.](?!\d)"""), "")
            return cleaned.replace(Regex("""\s+"""), " ").trim()
        }

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: VoiceInputController? = null

        fun getInstance(context: Context): VoiceInputController {
            return instance ?: synchronized(this) {
                instance ?: VoiceInputController(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
