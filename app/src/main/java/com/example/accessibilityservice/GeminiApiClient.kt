import com.example.accessibilityservice.BuildConfig
import com.example.accessibilityservice.Constants
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException

// Data classes for the payload and response

data class ContentPart(val text: String)
data class Content(val parts: List<ContentPart>, val role: String)
data class SystemInstructionContent(val parts: List<ContentPart>)
data class GenerateContentPayload(
    val contents: List<Content>,
    val systemInstruction: SystemInstructionContent = SystemInstructionContent(listOf(ContentPart(
        Constants.UI_UPDATED_SYSTEM_INSTRUCTIONS))),
    val generationConfig: GenerationConfig = GenerationConfig()
)
data class GenerationConfig (
    val responseMimeType: String = "application/json"
)

// The response structure has a "candidates" array containing objects with a "content" field.
data class Candidate(
    val content: Content
)
data class GenerateContentResponse(
    val candidates: List<Candidate>
)

class GeminiApiClient {

    private val apiKey = BuildConfig.apikey
    private val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro-001:generateContent?key=$apiKey"
    private val client = OkHttpClient()
    private val gson = Gson()

    fun generateContent(
        contents: List<Content>,
        onResult: (List<Content>?) -> Unit
    ) {
        val payload = GenerateContentPayload(contents)
        val jsonPayload = gson.toJson(payload)
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = RequestBody.create(mediaType, jsonPayload)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onResult(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!it.isSuccessful) {
                        onResult(null)
                        return
                    }
                    val responseBody = it.body?.string()
                    if (responseBody == null) {
                        onResult(null)
                        return
                    }
                    try {
                        val genResponse = gson.fromJson(responseBody, GenerateContentResponse::class.java)
                        val generatedContents = genResponse.candidates.map { candidate -> candidate.content }
                        onResult(generatedContents)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        onResult(null)
                    }
                }
            }
        })
    }
}