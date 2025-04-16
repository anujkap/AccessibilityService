// --- File: SpeechToTextListener.kt ---
package com.example.accessibilityservice // Adjust package name if needed

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat // Keep this for checking
import java.util.Locale
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog // Or MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class SpeechToTextListener(private val context: Context,
                           private val callback: (String) -> Unit,
                           private val didFinishDetection: () -> Unit) :
    RecognitionListener {

    private val speechRecognizer: SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(context)
    private val recognizerIntent: Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }
    private val mainHandler = Handler(Looper.getMainLooper())

    init {
        // Ensure the listener is set *after* speechRecognizer is initialized
        try {
            speechRecognizer.setRecognitionListener(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting RecognitionListener: ${e.message}", e)
            // Consider how to handle this failure - maybe disable the listener?
        }
    }

    fun startListening() {
        // The CALLER (Accessibility Service/Activity) is responsible for ensuring permission BEFORE calling this.
        // We still check here as a safeguard, but we DON'T request.
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "RECORD_AUDIO permission not granted. Cannot start listening. Request permission from an Activity.")
            onError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) // Inform the listener logic
            return // Stop execution here
        }

        // Permission should be granted if we reach here
        try {
            Log.d(TAG, "Attempting to start listening (Permission should be granted)...")
            mainHandler.post {
                speechRecognizer.startListening(recognizerIntent)
            }
            Log.d(TAG, "Started listening...")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException starting speech recognition: ${e.message}")
            onError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition: ${e.message}")
            // Consider mapping to a specific SpeechRecognizer error if possible
        }
    }

    fun stopListening() {
        try {
            speechRecognizer.stopListening()
            Log.d(TAG, "Stopped listening.")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech recognition: ${e.message}")
        }
    }

    fun destroy() {
        try {
            speechRecognizer.destroy()
            Log.d(TAG, "SpeechRecognizer destroyed.")
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying speech recognition: ${e.message}")
        }
    }

    // --- RecognitionListener Callbacks ---

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "Ready for speech")
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "Beginning of speech")
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Optional: Log.v(TAG, "RMS changed: $rmsdB")
    }

    override fun onBufferReceived(buffer: ByteArray?) {
        // Optional: Log.v(TAG, "Buffer received")
    }

    override fun onEndOfSpeech() {
        Log.d(TAG, "End of speech")
        didFinishDetection()
    }

    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions (Check Manifest & Runtime Grant)"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No recognition result matched"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "Server sends error status"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown speech error"
        }
        Log.e(TAG, "Speech recognition error: $errorMessage (code: $error)")
        // Consider adding a callback for errors too: callback("Error: $errorMessage")
        didFinishDetection()
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        matches?.let {
            if (it.isNotEmpty()) {
                val spokenText = it[0]
                Log.i(TAG, "Final recognition results: $spokenText")
                callback(spokenText)
            } else {
                Log.i(TAG, "Recognition results are empty.")
                // callback("") // Or handle as appropriate
            }
        } ?: Log.i(TAG, "Recognition results Bundle is null.")
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        matches?.firstOrNull()?.let { partialText ->
            Log.d(TAG, "Partial results: $partialText")
//             callback(partialText)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        Log.d(TAG, "onEvent: $eventType")
    }

    companion object {
        private const val TAG = "SpeechToTextListener"
    }
}


class PermissionRequestActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PermissionRequestAct"
        private const val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 101
        // Custom action to identify the intent
        const val ACTION_REQUEST_PERMISSION = "com.example.accessibilityservice.REQUEST_PERMISSION"
        const val EXTRA_PERMISSION_TO_REQUEST = "permission_to_request"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        // Optional: Set a transparent theme in Manifest or programmatically
        // Optional: No content view needed if fully transparent/dialog based

        handleIntent(intent)
    }

    // Handle case where activity is already running and launched again


    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_REQUEST_PERMISSION) {
            val permission = intent.getStringExtra(EXTRA_PERMISSION_TO_REQUEST)
            if (permission == Manifest.permission.RECORD_AUDIO) {
                requestAudioPermission()
            } else {
                Log.w(TAG, "Unsupported permission requested or missing extra: $permission")
                finish() // Close if invalid request
            }
        } else {
            Log.w(TAG, "Activity launched with unexpected intent action: ${intent?.action}")
            finish() // Close if launched incorrectly
        }
    }

    private fun requestAudioPermission() {
        Log.d(TAG, "Checking RECORD_AUDIO permission")
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.i(TAG, "RECORD_AUDIO permission already granted.")
                Toast.makeText(this, "Audio permission already granted", Toast.LENGTH_SHORT).show()
                finish() // Nothing to do, close
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.RECORD_AUDIO
            ) -> {
                Log.d(TAG, "Showing rationale for RECORD_AUDIO permission.")
                AlertDialog.Builder(this)
                    .setTitle("Permission Needed")
                    .setMessage("This feature requires microphone access to transcribe speech. Please grant the permission.")
                    .setPositiveButton("OK") { _, _ ->
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.RECORD_AUDIO),
                            RECORD_AUDIO_PERMISSION_REQUEST_CODE
                        )
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                        Toast.makeText(this, "Permission needed to use speech feature.", Toast.LENGTH_LONG).show()
                        finish() // Close if user cancels rationale
                    }
                    .setOnCancelListener {
                        Toast.makeText(this, "Permission needed to use speech feature.", Toast.LENGTH_LONG).show()
                        finish() // Close if user dismisses dialog
                    }
                    .create()
                    .show()
            }

            else -> {
                Log.d(TAG, "Requesting RECORD_AUDIO permission directly.")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    RECORD_AUDIO_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionsResult called for code $requestCode")
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "RECORD_AUDIO permission granted by user.")
                Toast.makeText(this, "Audio permission granted", Toast.LENGTH_SHORT).show()
                // Service will re-check permission status
            } else {
                Log.w(TAG, "RECORD_AUDIO permission denied by user.")
                Toast.makeText(this, "Audio permission denied", Toast.LENGTH_LONG).show()
                // Check if permanently denied ("Don't ask again")
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                    Log.w(TAG, "Permission denied permanently. Guiding user to settings.")
                    showSettingsDialog()
                    return // Keep activity open for the settings dialog
                }
            }
            finish() // Close the activity after handling the result (unless going to settings)
        }
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Microphone access was denied. Please enable it manually in App Settings to use the speech-to-text feature.")
            .setPositiveButton("Go to Settings") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open app settings", e)
                    Toast.makeText(this, "Could not open app settings.", Toast.LENGTH_LONG).show()
                } finally {
                    finish() // Close after attempting to send user to settings
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                finish() // Close if user cancels
            }
            .setOnCancelListener {
                finish() // Close if user dismisses dialog
            }
            .create()
            .show()
    }
}