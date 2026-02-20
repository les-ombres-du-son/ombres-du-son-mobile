package fr.upjv.lesombresduson.manager.audio

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Gestionnaire de synthèse vocale et de lecture d'audio.
 */
class AudioTTSManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var introPlayer: MediaPlayer? = null
    var voiceVolume: Float = 1.0f

    /**
     * Initialisation de la synthèse vocale.
     */
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.FRENCH
        }
    }

    /**
     * Lecture asynchrone d'un fichier audio d'introduction.
     */
    fun playIntro(resId: Int, onComplete: () -> Unit) {
        introPlayer?.release()
        introPlayer = MediaPlayer.create(context, resId).apply {
            setVolume(voiceVolume, voiceVolume)
            setOnCompletionListener {
                it.release()
                introPlayer = null
                onComplete()
            }
            start()
        }
    }

    /**
     * Fermeture de la synthèse vocale.
     */
    fun speak(text: String, queueMode: Int, utteranceId: String) {
        tts?.speak(text, queueMode, null, utteranceId)
    }

    /**
     * Arrêt de la lecture d'audio d'introduction.
     */
    fun stopIntro() {
        introPlayer?.stop()
    }

    /**
     * Libération des ressources TTS et audio.
     */
    fun release() {
        tts?.stop()
        tts?.shutdown()
        introPlayer?.release()
        introPlayer = null
    }
}