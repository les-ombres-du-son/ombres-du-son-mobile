package fr.upjv.lesombresduson.ui.game.cecilia.util

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import fr.upjv.lesombresduson.data.remote.FirebaseHelper
import fr.upjv.lesombresduson.data.remote.RealtimeHelper
import fr.upjv.lesombresduson.manager.sensor.HapticManager
import fr.upjv.lesombresduson.ui.StartChoiseCharacter
import fr.upjv.lesombresduson.util.SettingsConstants
import java.util.Locale

/**
 * Centralise la gestion du cycle de vie des ressources partagées (Audio, Haptique)
 * et la logique de navigation inter-activités pour éviter la duplication de code.
 */
abstract class BackGameActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    // Handler pour gérer le compte à rebours de fin de niveau
    private val handler = Handler(Looper.getMainLooper())

    // Instance partagée du gestionnaire de vibrations
    protected lateinit var hapticManager: HapticManager

    private var introPlayer: MediaPlayer? = null

    // --- VARIABLES GLOBALES DE RÉGLAGES ---
    // Accessibles par toutes les activités qui héritent de cette classe (protected)
    protected var voiceVolume: Float = 0.7f
    protected var sfxVolume: Float = 1.0f
    protected var musicVolume: Float = 1.0f
    protected var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialisation unique du service haptique lié au contexte de l'activité
        hapticManager = HapticManager(this)
        loadGlobalSettings()

        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.FRENCH
        }
    }

    /**
     * Charge les préférences sauvegardées dans SettingsActivity
     */
    private fun loadGlobalSettings() {
        val sharedPrefs = getSharedPreferences(SettingsConstants.PREFS_NAME, Context.MODE_PRIVATE)

        // On récupère une valeur entre 0 et 100
        val rawVoiceVol = sharedPrefs.getInt(SettingsConstants.KEY_VOICE_RATE, SettingsConstants.DEFAULT_VOICE_RATE)
        val rawMusicVol = sharedPrefs.getInt(SettingsConstants.KEY_MUSIC_VOLUME, SettingsConstants.DEFAULT_MUSIC_VOLUME)
        val rawSfxVol = sharedPrefs.getInt(SettingsConstants.KEY_SFX_VOLUME, SettingsConstants.DEFAULT_SFX_VOLUME)

        // On convertit pour MediaPlayer (qui veut entre 0.0 et 1.0)
        voiceVolume = rawVoiceVol / 100f
        sfxVolume = rawSfxVol / 100f
        musicVolume = rawMusicVol / 100f
    }

    /**
     * Configure la navigation de retour vers l'écran de sélection.
     * Gère la pile d'activités (Flags) pour éviter l'empilement inutile des vues.
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
     * Gère la lecture asynchrone de la piste audio d'introduction.
     * Déclenche le callback onComplete une fois la lecture terminée pour synchroniser le début du gameplay.
     */
    protected fun playIntro(resId: Int, onComplete: () -> Unit) {
        introPlayer?.release()

        introPlayer = MediaPlayer.create(this, resId).apply {
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
     * Affiche la modale de fin de niveau avec le récapitulatif des performances.
     * Gère la transition vers l'activité suivante via Intent.
     */
    protected fun showLevelCompleteDialog(
        score: Int,
        profil: String,
        details: String,
        speechText: String,
        nextActivityClass: Class<*>?
    ) {
        // Stopper l'intro si elle joue encore
        introPlayer?.stop()

        // 4. Lancer la synthèse vocale
        tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "LEVEL_END_ID")

        // Définition de l'action de navigation pour éviter de dupliquer le code
        val navigateToNext = {
            nextActivityClass?.let {
                val intent = Intent(this, it)
                startActivity(intent)
                finish()
            }
        }

        // Tâche automatique exécutée après 10 secondes
        val autoStartRunnable = Runnable {
            if (!isFinishing) {
                navigateToNext()
            }
        }

        // Construction du message avec l'info du compte à rebours
        val messageWithTimer = "Profil : $profil\n\n$details\n\n⏳ Niveau suivant dans 10s..."

        val dialog = AlertDialog.Builder(this)
            .setTitle("Niveau Terminé : $score/100")
            .setMessage(messageWithTimer)
            .setPositiveButton("Continuer") { _, _ ->
                // Annule le timer si l'utilisateur clique manuellement
                handler.removeCallbacks(autoStartRunnable)
                navigateToNext()
            }
            .setCancelable(false)
            .create()

        dialog.show()

        // Lancement du compte à rebours (10 000 ms = 10s)
        handler.postDelayed(autoStartRunnable, 10000)
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
                tts?.speak(message, TextToSpeech.QUEUE_ADD, null, "AI_HELP")
            }
        }
    }

    /**
     * Libération systématique des ressources média et haptiques
     * pour prévenir les fuites de mémoire à la destruction de l'activité.
     */
    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()

        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        introPlayer?.release()
        introPlayer = null
        hapticManager.cancel()
    }

    /**
     * Méthode centralisée de sauvegarde de données dans Firebase.
     */
    fun checkNetworkAndSave(userId: String, levelName: String, score: Int, profil: String, metrics: Map<String, Any>) {
        FirebaseHelper.getInstance().saveLevelData(
            userId,
            "Cécilia (cécité totale)",
            levelName,
            score,
            profil,
            metrics
        )

        RealtimeHelper.endSession()

        val connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork

        if (activeNetwork == null) {
            Toast.makeText(
                this,
                "Connexion perdue. Sauvegarde locale effectuée, synchro en attente.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}