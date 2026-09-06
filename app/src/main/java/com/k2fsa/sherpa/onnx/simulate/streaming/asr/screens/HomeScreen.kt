package com.k2fsa.sherpa.onnx.simulate.streaming.asr.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.k2fsa.sherpa.onnx.simulate.streaming.asr.R
import com.k2fsa.sherpa.onnx.simulate.streaming.asr.VoiceAccessibilityService
import com.k2fsa.sherpa.onnx.simulate.streaming.asr.VoiceInputController
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(controller: VoiceInputController) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val isRecording by controller.isRecording.collectAsState()
    val latestText by controller.latestText.collectAsState()
    val isInitialized by controller.isInitialized.collectAsState()
    val initError by controller.initError.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121318)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Title & Status Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quest Voice Board",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                val a11yActive by VoiceAccessibilityService.isConnected.collectAsState()
                if (!a11yActive) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFC62828), RoundedCornerShape(4.dp))
                            .clickable {
                                val enabled = VoiceAccessibilityService.ensureServiceEnabled(context)
                                if (!enabled) {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        })
                                        Toast.makeText(context, context.getString(R.string.toast_enable_a11y_prompt), Toast.LENGTH_LONG).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, context.getString(R.string.toast_run_deploy_prompt), Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.status_a11y_inactive),
                            fontSize = 10.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // Central Area: Latest Transcribed Text Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E2029)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isRecording) Color(0xFF4E7FFF).copy(alpha = 0.6f) else Color(0xFF2C2F3D)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {
                    if (latestText.isEmpty()) {
                        val statusText = when {
                            initError != null -> stringResource(R.string.status_init_failed, initError ?: "")
                            !isInitialized -> stringResource(R.string.status_loading_engine)
                            isRecording -> stringResource(R.string.status_listening)
                            else -> stringResource(R.string.status_ready)
                        }
                        val statusColor = if (initError != null) Color(0xFFEF5350) else Color(0xFF7E8494)
                        Text(
                            text = statusText,
                            fontSize = 13.sp,
                            color = statusColor,
                            modifier = Modifier.align(Alignment.Center),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = latestText,
                                fontSize = 14.sp,
                                color = Color(0xFFE8EAED),
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // Bottom Control Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (initError != null) {
                    Button(
                        onClick = { scope.launch { controller.initialize() } },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text(stringResource(R.string.btn_retry_init), fontSize = 13.sp)
                    }
                } else if (!isInitialized) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                        color = Color(0xFF4E7FFF)
                    )
                } else {
                    val buttonColor by animateColorAsState(
                        targetValue = if (isRecording) Color(0xFFE53935) else Color(0xFF2979FF),
                        label = "btnColor"
                    )

                    Button(
                        onClick = { controller.toggleRecording() },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = stringResource(if (isRecording) R.string.content_desc_stop else R.string.content_desc_start),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(if (isRecording) R.string.btn_stop_recording else R.string.btn_start_dictation),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
