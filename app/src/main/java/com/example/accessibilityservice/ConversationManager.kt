package com.example.accessibilityservice

import android.util.Log

class ConversationManager {
    private val debugTag = "ConversationManager"

    fun UIUpdated(withJson: String) {
        askLLM(PromptCreator().createPrompt(withJson))
    }

    fun userVoiceInput() {
        // to be called when user says something
    }

    private fun askLLM(prompt: String) {
        NetworkService.getResponse(prompt, { response ->
            run {
                Log.d(debugTag, "Response: $response")
                // TO DO: something with response
            }
        })
    }
}