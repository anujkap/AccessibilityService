object NetworkService {


    fun getResponse(prompt: List<ContentPart>, onResult: (String?) -> Unit) {
        val client = GeminiApiClient()
        val conversationContentsCache = makeConversationContents(prompt)
        client.generateContent(conversationContentsCache) { response ->
            onResult(response?.lastOrNull()?.parts?.firstOrNull()?.text)
        }
    }

    private fun makeConversationContents(prompt: List<ContentPart>): List<Content> {
        val conversationContents: MutableList<Content> = mutableListOf()
        for (p in prompt) {
            conversationContents.add(Content(prompt, "user"))
        }
        return conversationContents
    }
}