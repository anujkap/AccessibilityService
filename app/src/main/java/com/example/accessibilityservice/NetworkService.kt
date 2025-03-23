object NetworkService {

    private var conversationContentsCache: MutableList<Content> = mutableListOf()

    fun getResponse(prompt: String, onResult: (String?) -> Unit) {
        addContent(prompt)
        val client = GeminiApiClient()
        client.generateContent(conversationContentsCache) { response ->
            response?.let { conversationContentsCache.add(it.last()) }
            onResult(response?.lastOrNull()?.parts?.firstOrNull()?.text)
        }
    }
    private fun addContent(prompt: String) {
        conversationContentsCache.add(Content(listOf(ContentPart(prompt)), "user"))
    }
}