package com.example.accessibilityservice

import ContentPart
import com.google.gson.Gson

// Add Debounce
// Add request creator
// Add difference between question and answer

class ConversationManager {
    private val llmInteractor = LLMInteractor()

    fun UIUpdated(withJson: String) {
        if (llmInteractor.canRequestLLM()) {
            llmInteractor.askLLM(PromptCreator().createPromptForUIScreenUpdated(withJson), onResult = { response ->
                MyAccessibilityService.getService()?.processActions(response.actions)
            })
        }

    }

    fun userVoiceInput() {
        // to be called when user says something
    }


}

class LLMInteractor {
    private val debugTag = "ConversationManager"
    private var lastRequestTime = 0L

    fun canRequestLLM(): Boolean {
        return (System.currentTimeMillis() - lastRequestTime > 3000)
    }
    fun askLLM(prompt: List<ContentPart>, onResult: (ScreenResponse) -> Unit) {
        lastRequestTime = System.currentTimeMillis()
        NetworkService.getResponse(prompt) { response ->

            response?.let { res ->
                val data = ScreenResponseDeserializer().parseJson(res)
                data.let { d ->
                    onResult(d)
                    println("Response XXXXXX: $data")
                }

            }

        }
    }
}

@kotlinx.serialization.Serializable
data class ScreenResponse(
    val responseType: String,
    val text: String,
    val actions: List<DeviceAction> = emptyList()
)

class ScreenResponseDeserializer {

    private val gson = Gson()

    // Function to deserialize JSON string into a ResponseData object.
    fun parseJson(json: String): ScreenResponse {
        return gson.fromJson(json, ScreenResponse::class.java)
    }
}
