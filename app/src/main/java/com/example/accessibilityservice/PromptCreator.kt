package com.example.accessibilityservice

import ContentPart

class PromptCreator {
    fun createPromptForUIScreenUpdated(screenData: String): List<ContentPart> {
        return listOf(ContentPart(screenData))
    }

    fun createPromptForUserVoiceInput(query: String, screenData: String?): List<ContentPart> {
        if (screenData == null) {
            return listOf(
                ContentPart("Answer the following user query."),
                ContentPart(query)
            )
        } else {
            return listOf(
                ContentPart(screenData),
                ContentPart("Answer the following user query based on the previous screen context."),
                ContentPart(query)
            )
        }
    }
}