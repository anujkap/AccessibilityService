package com.example.accessibilityservice

import ContentPart

class PromptCreator {
    fun createPromptForUIScreenUpdated(screenData: String): List<ContentPart> {
         val initialPrompt = "This is the JSON for Android Accessibility UI hierarchy.\n" +
                "You are a screen reading assistant, summarize the UI of the screen for the user. Give me a response in the following JSON format.\n" +
                "{ \"text\": \" What you have to say\"} \n"
        return listOf(ContentPart(initialPrompt), ContentPart(screenData))
    }
}