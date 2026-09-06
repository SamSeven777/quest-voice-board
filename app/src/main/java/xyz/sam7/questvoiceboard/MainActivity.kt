package xyz.sam7.questvoiceboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import xyz.sam7.questvoiceboard.screens.HomeScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var controller: VoiceInputController

    private val requestAudioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                lifecycleScope.launch {
                    controller.initialize()
                }
            } else {
                Toast.makeText(this, getString(R.string.toast_audio_permission_required), Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        controller = VoiceInputController.getInstance(this)

        // Ensure accessibility service is active
        VoiceAccessibilityService.ensureServiceEnabled(this)

        // Initialize ASR engine in background immediately
        lifecycleScope.launch {
            controller.initialize()
        }

        // Check audio recording permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            HomeScreen(controller = controller)
        }
    }
}
