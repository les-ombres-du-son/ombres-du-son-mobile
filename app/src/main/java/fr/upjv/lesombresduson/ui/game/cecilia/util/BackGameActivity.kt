package fr.upjv.lesombresduson.ui.game.cecilia.util

import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import fr.upjv.lesombresduson.data.remote.RealtimeHelper
import fr.upjv.lesombresduson.data.repository.SettingsRepository
import fr.upjv.lesombresduson.manager.audio.AudioTTSManager
import fr.upjv.lesombresduson.manager.network.GameSyncManager
import fr.upjv.lesombresduson.manager.sensor.HapticManager
import fr.upjv.lesombresduson.ui.StartChoiseCharacter
import fr.upjv.lesombresduson.ui.game.cecilia.helper.GameDialogHelper

/**
 * Centralise la gestion du cycle de vie des ressources partagées (Audio, Haptique)
 * et la logique de navigation inter-activités pour éviter la duplication de code.
 */
abstract class BackGameActivity : AppCompatActivity() {

    protected lateinit var hapticManager: HapticManager
    protected lateinit var audioManager: AudioTTSManager
    protected lateinit var settings: SettingsRepository
    protected lateinit var syncManager: GameSyncManager
    protected lateinit var dialogHelper: GameDialogHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        RealtimeHelper.init(this)

        // 2. Initialisation des outils
        hapticManager = HapticManager(this)
        audioManager = AudioTTSManager(this)
        settings = SettingsRepository(this)
        syncManager = GameSyncManager(this)
        dialogHelper = GameDialogHelper(this)

        // On applique les réglages à l'audio
        audioManager.voiceVolume = settings.getVoiceVolume()
    }

    /**
     * Définit le comportement du bouton retour.
     */
    protected fun setupBackButton(button: View) {
        button.setOnClickListener {
            val intent = Intent(this, StartChoiseCharacter::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
    }

    /**
     * Affiche la modale de fin de niveau et navigue vers l'activité suivante.
     */
    protected fun showLevelCompleteDialog(
        score: Int,
        profil: String,
        details: String,
        speechText: String,
        nextActivityClass: Class<*>?
    ) {
        audioManager.stopIntro()

        // 1. On affiche la modale (elle attendra le clic OU la fin de l'audio)
        dialogHelper.showLevelCompleteDialog(score, profil, details) {
            nextActivityClass?.let {
                startActivity(Intent(this, it))
                finish()
            }
        }

        // 2. On lance l'audio et on écoute quand il se termine
        audioManager.speakWithCompletion(speechText, "LEVEL_END_ID") {
            // Ce bloc est appelé depuis un thread en arrière-plan (TTS)
            // Il faut repasser sur le thread principal (UI) pour changer de page
            runOnUiThread {
                dialogHelper.triggerNavigation()
            }
        }
    }

    /**
     * Centralisation de l'écoute des conseils IA.
     * Protégé pour être accessible par les niveaux enfants.
     */
    protected fun setupAIAssistanceListener() {
        RealtimeHelper.listenForAssistance { type, message ->
            if (type == "toast") {
                Toast.makeText(this, "Conseil IA : $message", Toast.LENGTH_LONG).show()
            } else if (type == "vocal") {
                audioManager.speak(message, TextToSpeech.QUEUE_ADD, "AI_HELP")
            }
        }
    }

    /**
     * Libération des ressources TTS et audio.
     */
    override fun onDestroy() {
        super.onDestroy()
        audioManager.release()
        dialogHelper.cancelTimer()
        hapticManager.cancel()
    }
}