package com.example.ui.screens

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.room.AppDatabase
import com.example.data.room.SavedPhrase
import com.example.data.room.TravelNote
import com.example.data.translation.Language
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.InlineData
import com.example.network.Part
import com.example.network.RetrofitClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

data class ConversationMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val senderName: String,
    val isPrimaryUser: Boolean,
    val originalText: String,
    val translatedText: String,
    val sourceLangCode: String,
    val targetLangCode: String,
    val phonetic: String = "",
    val culturalTip: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class LensAnalysisResult(
    val detectedLanguage: String = "",
    val translatedText: String = "",
    val foodDescription: String = "",
    val culturalTip: String = "",
    val rawExtracted: String = ""
)

class TranslateViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {
    private val TAG = "TranslateViewModel"
    private val database = AppDatabase.getDatabase(application)
    private val phraseDao = database.savedPhraseDao()
    private val travelNoteDao = database.travelNoteDao()

    val savedPhrases: StateFlow<List<SavedPhrase>> = phraseDao.getAllPhrases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active languages
    private val _primaryLang = MutableStateFlow(Language.findByCode("ro"))
    val primaryLang: StateFlow<Language> = _primaryLang.asStateFlow()

    private val _targetLang = MutableStateFlow(Language.findByCode("ja"))
    val targetLang: StateFlow<Language> = _targetLang.asStateFlow()

    // Mode: 0 = Dual Conversation, 1 = Camera Lens, 2 = Offline Phrasebook
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Conversation History
    private val _conversation = MutableStateFlow<List<ConversationMessage>>(emptyList())
    val conversation: StateFlow<List<ConversationMessage>> = _conversation.asStateFlow()

    // Current Translation State
    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    // Lens Analysis State
    private val _lensResult = MutableStateFlow<LensAnalysisResult?>(null)
    val lensResult: StateFlow<LensAnalysisResult?> = _lensResult.asStateFlow()

    private val _isLensAnalyzing = MutableStateFlow(false)
    val isLensAnalyzing: StateFlow<Boolean> = _isLensAnalyzing.asStateFlow()

    // Snackbar notifications
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent = _toastEvent.asSharedFlow()

    // Fullscreen show card text
    private val _fullscreenPhrase = MutableStateFlow<String?>(null)
    val fullscreenPhrase: StateFlow<String?> = _fullscreenPhrase.asStateFlow()

    // TTS Engine
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        tts = TextToSpeech(application, this)
        preloadSurvivalPhrases()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            tts?.language = Locale.ENGLISH
        } else {
            Log.e(TAG, "TTS initialization failed")
        }
    }

    fun setTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setPrimaryLanguage(language: Language) {
        _primaryLang.value = language
    }

    fun setTargetLanguage(language: Language) {
        _targetLang.value = language
    }

    // Swap animation trigger
    private val _swapAnimationCounter = MutableStateFlow(0)
    val swapAnimationCounter: StateFlow<Int> = _swapAnimationCounter.asStateFlow()

    // Auto-detect mode toggle
    private val _autoDetectEnabled = MutableStateFlow(true)
    val autoDetectEnabled: StateFlow<Boolean> = _autoDetectEnabled.asStateFlow()

    fun toggleAutoDetect() {
        _autoDetectEnabled.value = !_autoDetectEnabled.value
        val status = if (_autoDetectEnabled.value) "activată" else "dezactivată"
        viewModelScope.launch {
            _toastEvent.emit("Comutare automată bidirecțională $status.")
        }
    }

    fun swapLanguages() {
        val currentPrimary = _primaryLang.value
        _primaryLang.value = _targetLang.value
        _targetLang.value = currentPrimary
        _swapAnimationCounter.value += 1
        viewModelScope.launch {
            _toastEvent.emit("Schimbat: ${_primaryLang.value.flag} ${_primaryLang.value.nameRo} ⇄ ${_targetLang.value.flag} ${_targetLang.value.nameRo}")
        }
    }

    fun showFullscreenCard(text: String?) {
        _fullscreenPhrase.value = text
    }

    fun speakText(text: String, lang: Language) {
        if (!isTtsReady || text.isBlank()) return
        try {
            val result = tts?.setLanguage(lang.ttsLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to English if exact locale unavailable
                tts?.setLanguage(Locale.ENGLISH)
            }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "WanderAITTS_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error in TTS speak", e)
        }
    }

    private fun isNetworkConnected(): Boolean {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val cap = cm.getNetworkCapabilities(network) ?: return false
        return cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                cap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    /**
     * Translates input from either the primary user or the secondary (local) speaker.
     */
    fun translateMessage(text: String, fromPrimaryUser: Boolean) {
        if (text.isBlank()) return

        val sourceLang = if (fromPrimaryUser) _primaryLang.value else _targetLang.value
        val destLang = if (fromPrimaryUser) _targetLang.value else _primaryLang.value
        val senderTitle = if (fromPrimaryUser) "Tu (${sourceLang.flag} ${sourceLang.nameRo})" else "Localnic (${sourceLang.flag} ${sourceLang.nameRo})"

        _isTranslating.value = true

        viewModelScope.launch {
            if (!isNetworkConnected()) {
                // Offline fallback from local database or simple message
                _toastEvent.emit("Fără internet în roaming: Salvare în dicționarul local!")
                val offlineMsg = ConversationMessage(
                    senderName = senderTitle,
                    isPrimaryUser = fromPrimaryUser,
                    originalText = text,
                    translatedText = "[Offline - Roaming Inactiv] $text",
                    sourceLangCode = sourceLang.code,
                    targetLangCode = destLang.code,
                    phonetic = "Salvat offline în Room",
                    culturalTip = "Conectează-te la Wi-Fi pentru traducere avansată cu Gemini."
                )
                _conversation.value = _conversation.value + offlineMsg
                _isTranslating.value = false
                return@launch
            }

            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                val systemPrompt = """
                    You are Jules, a high-intelligence agentic travel companion.
                    Translate the following traveler text from ${sourceLang.nameRo} (${sourceLang.code}) into ${destLang.nameRo} (${destLang.code}).
                    Return a strictly valid JSON object with:
                    - "translation": The natural, polite and culturally accurate translation in ${destLang.nameRo}.
                    - "phonetic": A phonetic reading guide (e.g., Romaji, Pinyin, or easy Latin phonetic pronunciation).
                    - "culturalTip": A concise travel or etiquette tip for this phrase (e.g. when to use it, gestures, polite form).
                    Act as an agent, not just a translator. Provide context if necessary.
                    Do not include markdown or extra text. Only JSON.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(role = "user", parts = listOf(Part(text = "Text to translate: \"$text\"")))
                    ),
                    systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
                )

                val response = RetrofitClient.service.generateContent("gemini-3.8-flash", apiKey, request)
                val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                
                var cleanJson = rawJson.trim()
                if (cleanJson.startsWith("```json")) cleanJson = cleanJson.removePrefix("```json").trim()
                if (cleanJson.startsWith("```")) cleanJson = cleanJson.removePrefix("```").trim()
                if (cleanJson.endsWith("```")) cleanJson = cleanJson.removeSuffix("```").trim()

                val jsonObj = JSONObject(cleanJson)
                val translated = jsonObj.optString("translation", text)
                val phonetic = jsonObj.optString("phonetic", "")
                val tip = jsonObj.optString("culturalTip", "")

                val newMsg = ConversationMessage(
                    senderName = senderTitle,
                    isPrimaryUser = fromPrimaryUser,
                    originalText = text,
                    translatedText = translated,
                    sourceLangCode = sourceLang.code,
                    targetLangCode = destLang.code,
                    phonetic = phonetic,
                    culturalTip = tip
                )
                _conversation.value = _conversation.value + newMsg

                // Auto-speak target translation
                speakText(translated, destLang)

                // Save to Room for offline history
                phraseDao.insertPhrase(
                    SavedPhrase(
                        originalText = text,
                        translatedText = translated,
                        sourceLanguage = sourceLang.code,
                        targetLanguage = destLang.code,
                        phonetic = phonetic,
                        culturalNote = tip,
                        category = "Conversație"
                    )
                )

                // Cache as TravelNote in Room for offline travel review
                travelNoteDao.insertNote(
                    TravelNote(
                        title = "Traducere: ${destLang.nameRo}",
                        destination = destLang.nameRo,
                        type = TravelNote.NOTE_TYPE_VOICE,
                        transcriptOrPrompt = text,
                        aiResponse = "$translated\n(Fonetica: $phonetic${if (tip.isNotBlank()) " | Sfat: $tip" else ""})",
                        sourceLanguage = sourceLang.code,
                        targetLanguage = destLang.code,
                        tags = "Traducere, Voce, Conversație, Offline"
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Translation error", e)
                _toastEvent.emit("Eroare la traducere: ${e.localizedMessage ?: "Verifică cheia API"}")
            } finally {
                _isTranslating.value = false
            }
        }
    }

    /**
     * Multimodal Lens: Translates a photo of a restaurant menu, sign, or street direction.
     */
    fun analyzeCameraImage(base64Image: String) {
        _isLensAnalyzing.value = true
        _lensResult.value = null

        viewModelScope.launch {
            if (!isNetworkConnected()) {
                _toastEvent.emit("Scanarea vizuală necesită conexiune la internet.")
                _isLensAnalyzing.value = false
                return@launch
            }

            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                val prompt = """
                    You are Jules, the Agentic Travel Lens. Analyze this travel photo (which may be a restaurant menu, food item, street sign, metro map, or notice).
                    Translate all important text into Romanian (${_primaryLang.value.nameRo}).
                    Return a JSON object with:
                    - "detectedLanguage": Name of the language found on the sign/menu.
                    - "rawExtracted": The main text found in the original language.
                    - "translatedText": Clear Romanian translation.
                    - "foodDescription": If this is a menu or food, explain what the dish is, key ingredients, allergens (gluten, dairy, nuts) and dietary info (vegetarian/halal). If it's a sign, explain what it means.
                    - "culturalTip": A travel tip about ordering, customs, or navigating this location.
                    Act as an agent, providing deep insights beyond just text.
                    Only return raw valid JSON.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(
                            parts = listOf(
                                Part(text = prompt),
                                Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                            )
                        )
                    )
                )

                val response = RetrofitClient.service.generateContent("gemini-3.8-flash", apiKey, request)
                val raw = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                var clean = raw.trim()
                if (clean.startsWith("```json")) clean = clean.removePrefix("```json").trim()
                if (clean.startsWith("```")) clean = clean.removePrefix("```").trim()
                if (clean.endsWith("```")) clean = clean.removeSuffix("```").trim()

                val json = JSONObject(clean)
                _lensResult.value = LensAnalysisResult(
                    detectedLanguage = json.optString("detectedLanguage", "Limbă detectată"),
                    rawExtracted = json.optString("rawExtracted", ""),
                    translatedText = json.optString("translatedText", ""),
                    foodDescription = json.optString("foodDescription", ""),
                    culturalTip = json.optString("culturalTip", "")
                )
            } catch (e: Exception) {
                Log.e(TAG, "Lens error", e)
                _toastEvent.emit("Eroare la analiza foto: ${e.localizedMessage}")
            } finally {
                _isLensAnalyzing.value = false
            }
        }
    }

    fun toggleFavorite(phrase: SavedPhrase) {
        viewModelScope.launch {
            phraseDao.updateFavorite(phrase.id, !phrase.isFavorite)
        }
    }

    fun deletePhrase(id: Int) {
        viewModelScope.launch {
            phraseDao.deletePhrase(id)
            _toastEvent.emit("Fraza a fost ștearsă din istoricul offline.")
        }
    }

    fun clearConversation() {
        _conversation.value = emptyList()
    }

    /**
     * Preloads essential offline survival phrases into Room if the database is newly initialized.
     */
    private fun preloadSurvivalPhrases() {
        viewModelScope.launch {
            val existing = phraseDao.getAllPhrases().stateIn(viewModelScope).value
            if (existing.isEmpty()) {
                val defaults = listOf(
                    SavedPhrase(
                        originalText = "Unde este cel mai apropiat spital sau farmacie?",
                        translatedText = "Where is the nearest hospital or pharmacy?",
                        sourceLanguage = "ro",
                        targetLanguage = "en",
                        phonetic = "Uer iz dă niărest hospital",
                        culturalNote = "Întrebare de urgență. Arată ecranul direct oricărui trecător.",
                        category = "Urgențe",
                        isFavorite = true
                    ),
                    SavedPhrase(
                        originalText = "Ajutor! Vă rog chemați poliția!",
                        translatedText = "たすけて！警察を呼んでください！ (Tasukete! Keisatsu o yonde kudasai!)",
                        sourceLanguage = "ro",
                        targetLanguage = "ja",
                        phonetic = "Ta-su-ke-te! Kei-sa-tsu o yon-de ku-da-sai!",
                        culturalNote = "Urgență extremă în Japonia. Numărul de poliție este 110.",
                        category = "Urgențe",
                        isFavorite = true
                    ),
                    SavedPhrase(
                        originalText = "Cât costă o cursă până la această adresă?",
                        translatedText = "How much does a ride to this address cost?",
                        sourceLanguage = "ro",
                        targetLanguage = "en",
                        phonetic = "Hau maci daz ă raid tu dis adres cost?",
                        culturalNote = "Negociază sau confirmă pornirea aparatului de taxat înainte de plecare.",
                        category = "Transport",
                        isFavorite = true
                    ),
                    SavedPhrase(
                        originalText = "Nu consum carne sau lactate. Aveți opțiuni vegetariene?",
                        translatedText = "肉や乳製品は食べられません。ベジタリアン料理はありますか？",
                        sourceLanguage = "ro",
                        targetLanguage = "ja",
                        phonetic = "Niku ya nyuuseihin wa taberaremasen. Bejitarian ryouri wa arimasuka?",
                        culturalNote = "În Japonia, bulionul Dashi conține adesea pește; arată această notă bucătarului.",
                        category = "Restaurant",
                        isFavorite = true
                    ),
                    SavedPhrase(
                        originalText = "Nota de plată, vă rog!",
                        translatedText = "Il conto, per favore!",
                        sourceLanguage = "ro",
                        targetLanguage = "it",
                        phonetic = "Il kon-to, per fa-vo-re!",
                        culturalNote = "În Italia, coperto (taxa de masă) este normală pe notă.",
                        category = "Restaurant",
                        isFavorite = true
                    ),
                    SavedPhrase(
                        originalText = "Pot plăti cu cardul bancar?",
                        translatedText = "Can I pay by credit card?",
                        sourceLanguage = "ro",
                        targetLanguage = "en",
                        phonetic = "Kæn ai pei bai kreddit kard?",
                        culturalNote = "Unele piețe locale acceptă doar numerar.",
                        category = "Cumpărături",
                        isFavorite = false
                    )
                )
                defaults.forEach { phraseDao.insertPhrase(it) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
    }
}
