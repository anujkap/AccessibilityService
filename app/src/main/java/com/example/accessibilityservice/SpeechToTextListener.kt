package com.example.accessibilityservice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class SpeechToTextListener(private val context: Context, private val callback: (String) -> Unit) :
    RecognitionListener {

    private val speechRecognizer: SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(context)
    private val recognizerIntent: Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            Locale.getDefault()
        ) // Use the device's default language
        putExtra(
            RecognizerIntent.EXTRA_PARTIAL_RESULTS,
            true
        )  // Get partial results as the user speaks
    }

    init {
        speechRecognizer.setRecognitionListener(this)
    }


    fun startListening() {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Missing RECORD_AUDIO permission. Please request it.")
            // You should ideally handle this by requesting the permission from the user.
            // This example just logs an error, but in a real app, you'd need to handle the permission request properly.
            return
        }

        try {
            speechRecognizer.startListening(recognizerIntent)
            Log.d(TAG, "Started listening...")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition: ${e.message}")
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
        speechRecognizer.destroy()
        Log.d(TAG, "SpeechRecognizer destroyed.")
    }


    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "Ready for speech")
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "Beginning of speech")
    }

    override fun onRmsChanged(rmsdB: Float) {
        // You could visualize the sound level here if needed.
    }

    override fun onBufferReceived(buffer: ByteArray?) {
        Log.d(TAG, "Buffer received")
    }

    override fun onEndOfSpeech() {
        Log.d(TAG, "End of speech")
    }

    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No recognition result matched"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "Server sends error status"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error"
        }
        Log.e(TAG, "Speech recognition error: $errorMessage (code: $error)")
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        matches?.let {
            val spokenText = it.joinToString(" ")
            Log.i(TAG, "Final recognition results: $spokenText")
            callback(spokenText)  // Send the final recognized text back to the caller
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        matches?.let {
            val partialText = it.joinToString(" ")
            Log.d(TAG, "Partial results: $partialText")
            // You might optionally want to do something with partial results, e.g.,
            // display them to the user in real-time.  For this example, we'll
            // just log them.
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        //  Reserved for adding custom events.  Not typically used.
    }

    companion object {
        private const val TAG = "SpeechToTextListener"
    }
}