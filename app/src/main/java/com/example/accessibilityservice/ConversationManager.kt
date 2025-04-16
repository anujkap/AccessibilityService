package com.example.accessibilityservice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class ConversationManager(private val context: Context) { // Accept Context here

    private val llmInteractor = LLMInteractor()

    private val managerJob = Job()
    private val managerScope = CoroutineScope(Dispatchers.Main + managerJob)

    private var speechListener: SpeechToTextListener? = null
    private var announcer: AccessibilityAnnouncer? = null
    private var canStartConversation = false
    private var isAnnouncing = false
    private var isListening = false


    companion object {
        const val TAG = "ConvManager"
    }

    fun UIUpdated(withJson: String) {
        if (!canStartConversation) { return }
        if (llmInteractor.canRequestLLM()) {
            Log.d(TAG, "UI Updated with json")
            llmInteractor.askLLM(PromptCreator().createPromptForUIScreenUpdated(withJson), onResult = { response ->
                val service = MyAccessibilityService.getService()
                service?.processActions(response.actions)
                Log.d(TAG, "Response text: ${response.text}")
                announce(response.text)
            })
        }
    }

    fun serviceConnected() {
        PermissionHelper.serviceConnected(
            callback = { permissionGranted ->
                initialiseServices()

            },
            context = context
        )

    }

    fun initialiseServices() {

        speechListener = SpeechToTextListener(
            context = context, // Use the context passed to ConversationManager
            callback = { text ->
                Log.d("XXXX", "Speech to text: $text")

                isListening = false
                updatedUserConversation(text)

            },
            didFinishDetection = {
                isListening = false
            }
        )
        if (announcer == null) {
            announcer = AccessibilityAnnouncer(context, {
                isAnnouncing = true
            }, {
                isAnnouncing = false
                startSpeechRecognitionInternal()
            }, {
                isAnnouncing = false
                startSpeechRecognitionInternal()
            })
        }
        startSpeechRecognitionInternal()
        canStartConversation = true
    }

    private fun updatedUserConversation(text: String) {
        if (llmInteractor.canRequestLLM()) {
            Log.d(TAG, "UI Updated with json")
            llmInteractor.askLLM(PromptCreator().createPromptForUserVoiceInput(text), onResult = { response ->
                val service = MyAccessibilityService.getService()
                service?.processActions(response.actions)
                Log.d(TAG, "Response text: ${response.text}")
                announce(response.text)
            })
        }
    }

    private fun startSpeechRecognitionInternal() {
        if (isAnnouncing) return
        isListening = true
        Log.d(TAG, "Internal check: Verifying permission before starting listener...")
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Microphone permission denied.", Toast.LENGTH_SHORT).show()
            return
        }
            Log.i(TAG, "Permission granted, calling startListening().")
            speechListener?.startListening() // Call startListening on the instance
    }
    private fun announce(text: String) {
        if (isListening) return
        announcer?.announce(text)
    }
}