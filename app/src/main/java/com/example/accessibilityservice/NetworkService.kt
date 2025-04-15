object NetworkService {

    private var conversationContentsCache: MutableList<Content> = mutableListOf()

    fun getResponse(prompt: List<ContentPart>, onResult: (String?) -> Unit) {
        addContent(prompt)
        val client = GeminiApiClient()
        client.generateContent(conversationContentsCache) { response ->
            response?.let { addContent(it.last().parts) }
            onResult(response?.lastOrNull()?.parts?.firstOrNull()?.text)
        }
    }
    private fun addContent(prompt: List<ContentPart>) {
        if (conversationContentsCache.size < 2) {
            conversationContentsCache.add(Content(prompt, "user"))
        } else {
            conversationContentsCache.removeAt(0)
            conversationContentsCache.add(Content(prompt, "user"))
        }


    }
}