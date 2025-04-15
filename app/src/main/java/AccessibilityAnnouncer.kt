import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log

class AccessibilityAnnouncer(context: Context) : TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private val pendingAnnouncements = mutableListOf<String>()

    init {
        // Initialize TTS immediately when the Announcer is created
        textToSpeech = TextToSpeech(context.applicationContext, this)
        Log.d("AccessibilityAnnouncer", "TTS Initialization requested.")
    }

    /**
     * Called when TextToSpeech initialization is complete.
     */
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            Log.i("AccessibilityAnnouncer", "TextToSpeech initialized successfully.")
            // Optional: Set language, pitch, speed etc.
            // val result = textToSpeech?.setLanguage(Locale.US)
            // if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            //     Log.e("AccessibilityAnnouncer", "TTS language is not supported.")
            // } else {
            //     Log.d("AccessibilityAnnouncer", "TTS language set.")
            // }

            // Speak any pending announcements
            synchronized(pendingAnnouncements) {
                pendingAnnouncements.forEach { speakInternal(it) }
                pendingAnnouncements.clear()
            }
        } else {
            isTtsInitialized = false
            Log.e("AccessibilityAnnouncer", "TextToSpeech initialization failed: $status")
            textToSpeech = null // Release the object if init failed
        }
    }

    /**
     * Announces the given text using TextToSpeech.
     * If TTS is not yet initialized, the text will be queued.
     */
    fun announce(text: String?) {
        if (text.isNullOrBlank()) {
            Log.w("AccessibilityAnnouncer", "Attempted to announce null or empty text.")
            return
        }

        if (isTtsInitialized && textToSpeech != null) {
            speakInternal(text)
        } else {
            Log.w("AccessibilityAnnouncer", "TTS not ready, queuing announcement: '$text'")
            synchronized(pendingAnnouncements) {
                pendingAnnouncements.add(text)
            }
            // If TTS object exists but wasn't initialized, maybe retry init? Or rely on initial attempt.
            // For simplicity, we assume the initial init call is sufficient.
            if (textToSpeech == null && isTtsInitialized) {
                // This case might mean TTS was shutdown or failed init previously
                Log.e("AccessibilityAnnouncer", "Cannot announce, TTS instance is null after potential initialization.")
            }
        }
    }

    private fun speakInternal(text: String) {
        Log.d("AccessibilityAnnouncer", "Speaking: '$text'")
        // Use a unique utterance ID if you need callbacks (onStart, onDone, onError)
        val utteranceId = this.hashCode().toString() + "_" + System.currentTimeMillis()
        textToSpeech?.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)
        // QUEUE_FLUSH cancels previous announcements, QUEUE_ADD adds to the end. Choose based on need.
    }

    /**
     * Releases the TextToSpeech engine resources.
     * Call this when the announcer is no longer needed (e.g., in onDestroy of your service).
     */
    fun shutdown() {
        Log.d("AccessibilityAnnouncer", "Shutting down TTS.")
        isTtsInitialized = false
        // Stop any current speech before shutting down
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        synchronized(pendingAnnouncements) {
            pendingAnnouncements.clear() // Clear queue on shutdown
        }
    }
}