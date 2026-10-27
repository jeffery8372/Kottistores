package com.kottistore.pricefinder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.kottistore.pricefinder.data.ItemRepository
import com.kottistore.pricefinder.match.FuzzyMatcher
import com.kottistore.pricefinder.quantity.QuantityCalc
import com.kottistore.pricefinder.quantity.Quote
import com.kottistore.pricefinder.ui.KottistoreTheme
import com.kottistore.pricefinder.ui.PriceFinderScreen
import java.util.Locale

enum class AppLanguage(val label: String, val localeTag: String) {
    ENGLISH("English", "en-IN"),
    TAMIL("தமிழ்", "ta-IN")
}

class MainActivity : ComponentActivity() {

    private lateinit var matcher: FuzzyMatcher

    private var itemCount by mutableStateOf(0)
    private var listening by mutableStateOf(false)
    private var transcript by mutableStateOf("")
    private var results by mutableStateOf<List<FuzzyMatcher.SearchResult>>(emptyList())
    private var status by mutableStateOf<String?>(null)
    private var language by mutableStateOf(AppLanguage.ENGLISH)
    private var ttsReady by mutableStateOf(false)

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startListening()
            } else {
                status = "Microphone permission is required."
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        matcher = FuzzyMatcher(ItemRepository(applicationContext).load())
        itemCount = matcher.count
        setupTts()
        setupRecognizer()

        setContent {
            KottistoreTheme {
                PriceFinderScreen(
                    itemCount = itemCount,
                    language = language,
                    listening = listening,
                    transcript = transcript,
                    results = results,
                    status = status,
                    onLanguageChange = { language = it },
                    onMicClick = { if (listening) stopListening() else requestAndListen() },
                    onSpeak = { speakQuote(it) },
                    onClear = {
                        transcript = ""
                        results = emptyList()
                        status = null
                    },
                    onTextSearch = { handleQuery(it) }
                )
            }
        }
    }

    private fun setupTts() {
        tts = TextToSpeech(this) { result ->
            ttsReady = result == TextToSpeech.SUCCESS
        }
    }

    private fun setupRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status = "Speech recognition is not available on this device."
            return
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    status = "Listening..."
                }

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    listening = false
                }

                override fun onError(error: Int) {
                    listening = false
                    status = speechErrorMessage(error)
                }

                override fun onResults(results: Bundle?) {
                    listening = false
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (!text.isNullOrBlank()) {
                        handleQuery(text)
                    } else {
                        status = "I did not catch that. Tap the mic and try again."
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val text = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                    if (!text.isNullOrBlank()) {
                        transcript = text
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun requestAndListen() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startListening() {
        val speech = recognizer
        if (speech == null) {
            status = "Speech recognition is not available."
            return
        }
        transcript = ""
        results = emptyList()
        status = null

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.localeTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language.localeTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say an item name")
        }
        listening = true
        speech.startListening(intent)
    }

    private fun stopListening() {
        recognizer?.stopListening()
        listening = false
    }

    private fun handleQuery(text: String) {
        transcript = text
        val found = matcher.search(text)
        results = found

        if (found.isEmpty() || found.first().score < 0.50) {
            status = "No confident match for \"$text\". Try the item name again."
            speak("No confident match found. Please try again.", false)
            return
        }

        val top = found.first()
        if (found.size > 1 && top.score - found[1].score < 0.06 && top.score < 0.88) {
            status = "Did you mean ${top.item.name} or ${found[1].item.name}?"
            speak("Did you mean ${top.item.name} or ${found[1].item.name}?", false)
        } else {
            status = null
            val quote = QuantityCalc.defaultQuote(top.item)
            if (quote != null) {
                speakQuote(quote)
            } else {
                speak("${top.item.name}. Price unavailable.", false)
            }
        }
    }

    private fun speakQuote(quote: Quote) {
        val useTamil = language == AppLanguage.TAMIL &&
            hasTamil(quote.item.alias) &&
            ttsSupportsTamil()
        val name = if (useTamil) quote.item.alias else quote.item.name
        val quantity = QuantityCalc.formatQuantity(quote.quantity)
        val unit = QuantityCalc.spokenUnit(quote.unit, useTamil)
        val total = formatPrice(quote.total)
        val text = if (useTamil) {
            "$name. $quantity $unit. மொத்தம் $total ரூபாய்."
        } else {
            "$name. $quantity $unit. Total $total rupees."
        }
        speak(text, useTamil)
    }

    private fun speak(text: String, tamil: Boolean) {
        val engine = tts ?: return
        if (!ttsReady) return
        val locale = if (tamil) Locale("ta", "IN") else Locale("en", "IN")
        val available = if (tamil) {
            engine.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE
        } else {
            engine.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE ||
                engine.isLanguageAvailable(Locale.ENGLISH) >= TextToSpeech.LANG_AVAILABLE
        }
        engine.setLanguage(if (available) locale else Locale.ENGLISH)
        engine.setSpeechRate(0.95f)
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "kottistore-price")
    }

    private fun ttsSupportsTamil(): Boolean {
        val engine = tts ?: return false
        return engine.isLanguageAvailable(Locale("ta", "IN")) >= TextToSpeech.LANG_AVAILABLE
    }

    private fun hasTamil(text: String): Boolean {
        return text.any { it in '\u0B80'..'\u0BFF' }
    }

    private fun speechErrorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                "I did not catch that. Tap the mic and try again."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                "Microphone permission is required."
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                "Network error during recognition. Check your internet connection."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                "Recognizer is busy. Try again in a moment."
            SpeechRecognizer.ERROR_CLIENT ->
                "Recognition was cancelled."
            else -> "Recognition error ($error). Please try again."
        }
    }

    override fun onDestroy() {
        recognizer?.destroy()
        recognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}
