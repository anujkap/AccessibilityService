package com.example.accessibilityservice

import com.google.gson.Gson
import ContentPart

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
