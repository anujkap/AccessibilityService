package com.example.accessibilityservice

import ContentPart

class PromptCreator {
    fun createPromptForUIScreenUpdated(screenData: String): List<ContentPart> {
        return listOf(ContentPart(screenData))
    }
    fun createPromptForUserVoiceInput(text: String): List<ContentPart> {
        return listOf(ContentPart(text))
    }
}