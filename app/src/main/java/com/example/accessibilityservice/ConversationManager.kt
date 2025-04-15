package com.example.accessibilityservice
import AccessibilityAnnouncer


class ConversationManager {
    private val llmInteractor = LLMInteractor()

    fun UIUpdated(withJson: String) {
        if (llmInteractor.canRequestLLM()) {
            llmInteractor.askLLM(PromptCreator().createPromptForUIScreenUpdated(withJson), onResult = { response ->
                MyAccessibilityService.getService()?.processActions(response.actions)
                AccessibilityAnnouncer(MyAccessibilityService.getService()!!).announce(response.text)
            })
        }

    }

    fun userVoiceInput() {
        // to be called when user says something
    }

}

