package fr.upjv.lesombresduson.ui.game.cecilia.util

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID

/**
 * Classe qui gère la synthèse vocale et la reconnaissance vocale.
 */
class VoiceManagerlevel4(
    private val activity: Activity,
    private val onSpeechResult: (String) -> Unit,
    private val onTtsDone: () -> Unit,
    private val onSpeechError: () -> Unit
) {
    private val TAG = "VoiceManager"
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    var isListeningEnabled = false // Interrupteur global pour le micro

    init {
        initTTS()
        initSTT()
    }

    /**
     * Initialisation du TTS.
     */
    private fun initTTS() {
        tts = TextToSpeech(activity) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.FRANCE
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d(TAG, "L'IA commence à parler...")
                    }
                    override fun onDone(utteranceId: String?) {
                        Log.d(TAG, "L'IA a fini de parler.")
                        activity.runOnUiThread { onTtsDone() }
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        activity.runOnUiThread { onSpeechError() }
                    }
                })
            }
        }
    }

    /**
     * Initialisation du STT.
     */
    private fun initSTT() {
        if (SpeechRecognizer.isRecognitionAvailable(activity)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { Log.d(TAG, "Micro prêt...") }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    if (!isListeningEnabled) return
                    if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        startListening()
                    } else {
                        Log.e(TAG, "Erreur micro : $error. Pause avant relance...")
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (isListeningEnabled) startListening()
                        }, 500)
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        isListeningEnabled = false // Coupe le micro après réception
                        onSpeechResult(matches[0])
                    } else {
                        if (isListeningEnabled) startListening()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    /**
     * Démarre la synthèse vocale.
     */
    fun speak(text: String) {
        val utteranceId = UUID.randomUUID().toString()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    /**
     * Démarre la lecture vocale.
     */
    fun startListening() {
        if (!isListeningEnabled) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    /**
     * Arrête la lecture vocale.
     */
    fun stopListening() {
        isListeningEnabled = false
        speechRecognizer?.stopListening()
    }

    /**
     * Libère les ressources TTS et STT.
     */
    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }
}