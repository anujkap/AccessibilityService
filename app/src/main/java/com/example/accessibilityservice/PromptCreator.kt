package com.example.accessibilityservice

import ContentPart

class PromptCreator {
    fun createPromptForUIScreenUpdated(screenData: String): List<ContentPart> {
         val initialPrompt = " take a screenshot of the page im on"
        return listOf(ContentPart(screenData))
    }
}