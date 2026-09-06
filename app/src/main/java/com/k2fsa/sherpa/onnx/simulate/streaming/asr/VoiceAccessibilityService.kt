package com.k2fsa.sherpa.onnx.simulate.streaming.asr

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VoiceAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isConnected.value = true
        Log.i(TAG, "VoiceAccessibilityService connected")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
            _isConnected.value = false
            currentToast = null
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) {
            return super.onKeyEvent(event)
        }

        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val now = System.currentTimeMillis()
            if (now - lastVolumeKeyTime < 500) {
                lastVolumeKeyTime = 0L
                toggleVoiceInput()
                return true // 消费双击事件，防止调小系统主音量
            } else {
                lastVolumeKeyTime = now
            }
        }

        return super.onKeyEvent(event)
    }

    companion object {
        private const val TAG = "VoiceAccessibility"

        @Volatile
        var instance: VoiceAccessibilityService? = null

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

        @Volatile
        var isVoiceInputEnabled: Boolean = false

        private var lastVolumeKeyTime: Long = 0L
        private var currentToast: Toast? = null

        fun toggleVoiceInput() {
            val srv = instance
            if (srv != null) {
                val controller = VoiceInputController.getInstance(srv)
                controller.toggleRecording()
                return
            }

            isVoiceInputEnabled = !isVoiceInputEnabled
            showToggleToast(isVoiceInputEnabled)
        }

        fun showToggleToast(enabled: Boolean) {
            val ctx = instance ?: return
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                currentToast?.cancel()
                val message = ctx.getString(if (enabled) R.string.toast_listening else R.string.toast_paused)
                currentToast = Toast.makeText(ctx, message, Toast.LENGTH_SHORT).apply {
                    show()
                }
            }
        }

        fun ensureServiceEnabled(context: Context): Boolean {
            try {
                val cr = context.contentResolver
                val expectedService = "${context.packageName}/${VoiceAccessibilityService::class.java.name}"
                val currentServices = Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
                if (!currentServices.contains(expectedService)) {
                    val updatedServices = if (currentServices.isEmpty()) expectedService else "$currentServices:$expectedService"
                    Settings.Secure.putString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, updatedServices)
                    Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
                    Log.i(TAG, "Self-enabled accessibility service successfully")
                    return true
                }
                return true
            } catch (e: Exception) {
                Log.d(TAG, "Could not self-enable accessibility service: ${e.message}")
                return false
            }
        }

        fun pasteText(text: String?): Boolean {
            if (text.isNullOrBlank()) {
                return false
            }
            val service = instance ?: run {
                Log.w(TAG, "VoiceAccessibilityService is not connected")
                return false
            }

            try {
                var targetNode: AccessibilityNodeInfo? = null

                // 1. Try active window focused node
                val activeRoot = service.rootInActiveWindow
                if (activeRoot != null) {
                    val focused = activeRoot.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                    if (isValidInputNode(focused)) {
                        targetNode = focused
                    }
                }

                // 2. Fallback: Search across all interactive windows
                if (targetNode == null) {
                    val windowList = service.windows
                    if (windowList != null) {
                        for (window in windowList) {
                            val root = window.root ?: continue
                            val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                            if (isValidInputNode(focused)) {
                                targetNode = focused
                                break
                            }
                        }
                    }
                }

                if (targetNode == null) {
                    Log.d(TAG, "No valid focused input node found, skipping paste")
                    return false
                }

                // Try ACTION_PASTE first
                var success = targetNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)

                // Fallback to ACTION_SET_TEXT if paste is not directly handled
                if (!success) {
                    val args = Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                    }
                    success = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                }

                Log.i(TAG, "Paste action executed, result=$success")
                return success
            } catch (t: Throwable) {
                Log.e(TAG, "Error performing paste action", t)
                return false
            }
        }

        private fun isValidInputNode(node: AccessibilityNodeInfo?): Boolean {
            if (node == null) return false
            val isInputCandidate = node.isEditable ||
                node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_PASTE || it.id == AccessibilityNodeInfo.ACTION_SET_TEXT }
            if (!isInputCandidate) return false
            val pkg = node.packageName?.toString() ?: return true
            return pkg != "com.k2fsa.sherpa.onnx.simulate.streaming.asr"
        }
    }
}
