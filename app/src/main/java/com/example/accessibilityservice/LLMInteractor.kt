package com.example.accessibilityservice

import com.google.gson.Gson
import ContentPart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LLMInteractor {
    private val debugTag = "ConversationManager"
    private val debouncePeriod = 1000L // 1 second
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var currentJob: Job? = null

    fun canRequestLLM(): Boolean {
        return true
    }

    fun askLLM(prompt: List<ContentPart>, onResult: (ScreenResponse) -> Unit) {
        currentJob?.cancel() // Cancel any existing job
        currentJob = coroutineScope.launch {
            delay(debouncePeriod) // Wait for the debounce period
            withContext(Dispatchers.Main) { // Switch to main thread for onResult
                NetworkService.getResponse(prompt) { response ->
                    response?.let { res ->
                        val data = ScreenResponseDeserializer().parseJson(res)
                        onResult(data)
                    }
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