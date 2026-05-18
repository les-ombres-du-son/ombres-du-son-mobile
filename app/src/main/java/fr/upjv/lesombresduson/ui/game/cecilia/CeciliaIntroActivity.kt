package fr.upjv.lesombresduson.ui.game.cecilia

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Vibrator
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import fr.upjv.lesombresduson.R
import fr.upjv.lesombresduson.data.remote.FirebaseHelper
import fr.upjv.lesombresduson.data.remote.RealtimeHelper
import fr.upjv.lesombresduson.manager.input.TouchNavigationManager
import fr.upjv.lesombresduson.manager.sensor.MicrophoneManager
import fr.upjv.lesombresduson.manager.sensor.SensorGameManager
import fr.upjv.lesombresduson.ui.game.cecilia.util.BackGameActivity
import fr.upjv.lesombresduson.manager.input.GestureListener
import fr.upjv.lesombresduson.ui.game.cecilia.logic.IntroLogic
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Implémentation de l'Introduction : Tutoriel et Éveil Sensoriel.
 * Le joueur découvre les trois mécaniques de base du jeu : inclinaison (gyroscope),
 * recherche tactile guidée par le son (chaud/froid), et souffle (microphone).
 * L'objectif est de familiariser le joueur avec les interactions non-visuelles avant les niveaux avancés.
 */
class CeciliaIntroActivity : BackGameActivity(), SensorGameManager.SensorGameListener, GestureListener {

    override val sessionName = "Intro"
    private val logic = IntroLogic()

    private lateinit var textToSpeech: TextToSpeech

    // --- SERVICES ET MANAGERS ---
    private val vibrator: Vibrator by lazy { getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
    private lateinit var gameManager: SensorGameManager
    private var touchManager: TouchNavigationManager? = null
    private var micManager: MicrophoneManager? = null

    // --- ÉTAT DU JEU ---
    private var isGameStarted = false
    private var isIntroSequenceFinished = false
    private var mediaPlayerIntro: MediaPlayer? = null
    private var mediaPlayerAfterIntro: MediaPlayer? = null
    private var currentLocalStep = 0

    // --- MÉTRIQUES ---
    private var interruptionCount = 0
    private var phaseStartTime: Long = 0
    private var totalTimeReaction: Long = 0

    companion object {
        private const val MICROPHONE_PERMISSION_CODE = 102
    }

    // =========================================================================
    //                      CYCLE DE VIE
    // =========================================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.FRENCH
            }
        }

        gameManager = SensorGameManager(this, this)

        loadIntroProgress()
    }

    override fun onResume() {
        super.onResume()
        if (!isGameStarted) return

        RealtimeHelper.updateGyroStats(interruptionCount, gameManager.instabilityCount, gameManager.currentExpectedDirection, gameManager.currentActualDirection)

        if (!isIntroSequenceFinished && mediaPlayerIntro?.isPlaying == false) mediaPlayerIntro?.start()
        if (isIntroSequenceFinished && mediaPlayerAfterIntro?.isPlaying == false && gameManager.gestureCount == 0) mediaPlayerAfterIntro?.start()
        if (isIntroSequenceFinished && gameManager.gestureCount < 4) gameManager.startListening()
    }

    override fun onPause() {
        super.onPause()
        gameManager.stopListening()
        mediaPlayerIntro?.pause()
        mediaPlayerAfterIntro?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        gameManager.stopListening()
        releaseMediaPlayers()
        touchManager?.cleanup()
        micManager?.stopListening()

        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    // =========================================================================
    //                      LOGIQUE AUDIO & INITIALISATION
    // =========================================================================

    /**
     * Vérifie le volume et lance le jeu ou affiche une popup.
     */
    private fun checkVolumeAndStart() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) > 0) {
            startIntroSequence()
        } else {
            showVolumePopup()
        }
    }

    /**
     * Affiche la popup demandant d'activer le son.
     */
    private fun showVolumePopup() {
        AlertDialog.Builder(this)
            .setTitle("Son requis 🔊")
            .setMessage("Ce jeu est basé sur l'audio. Votre volume semble être coupé. Veuillez l'augmenter pour profiter de l'expérience.")
            .setCancelable(false) // Empêche de fermer en cliquant à côté
            .setPositiveButton("J'ai activé le son") { dialog, _ ->
                // On vérifie à nouveau quand l'utilisateur clique sur OK
                checkVolumeAndStart()
            }
            .setNegativeButton("Quitter") { _, _ ->
                // Si l'utilisateur refuse, on retourne à l'écran précédent
                btnBack.performClick()
            }
            .show()
    }

    /**
     * Démarre la séquence d'introduction.
     */
    private fun startIntroSequence() {
        isGameStarted = true
        isIntroSequenceFinished = false

        mediaPlayerIntro = MediaPlayer.create(this, R.raw.cecilia_intro)?.apply {
            setVolume(audioManager.voiceVolume, audioManager.voiceVolume)
            start()
            setOnCompletionListener {
                isIntroSequenceFinished = true
                phaseStartTime = System.currentTimeMillis()
                onInstructionReady("Inclinez votre téléphone à droite !")
            }
        }
    }

    /**
     * Libère les ressources des MediaPlayers.
     */
    private fun releaseMediaPlayers() {
        mediaPlayerIntro?.release()
        mediaPlayerAfterIntro?.release()
        mediaPlayerIntro = null
        mediaPlayerAfterIntro = null
    }

    /**
     * Boolean indiquant si le jeu est en cours.
     */
    override fun isPlaying(): Boolean {
        return isIntroSequenceFinished
    }

    /**
     * Vérifie dans Firebase si le joueur a déjà passé certaines étapes
     */
    private fun loadIntroProgress() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            checkVolumeAndStart()
            return
        }

        FirebaseHelper.getInstance().getGameData(uid, "Cécilia (cécité totale)", object : FirebaseHelper.GameDataCallback {
            override fun onDataLoaded(gameData: Map<String, Any>?) {
                val currentStep = (gameData?.get("introCurrentStep") as? Number)?.toInt() ?: 0
                currentLocalStep = currentStep

                when (currentStep) {
                    1 -> resumeFromTouchPhase() // A fini le gyroscope
                    2 -> resumeFromMicPhase()   // A fini le tactile
                    else -> checkVolumeAndStart() // Rien fini, on commence au début
                }
            }

            override fun onFailure(e: Exception) {
                checkVolumeAndStart() // En cas d'erreur réseau, on lance le début
            }
        })
    }

    /**
     * Reprise directe à la phase Tactile
     */
    private fun resumeFromTouchPhase() {
        isGameStarted = true
        Toast.makeText(this, "Reprise : Phase de recherche tactile", Toast.LENGTH_SHORT).show()

        textToSpeech.speak("Reprise de la partie. Touchez l'écran pour trouver la porte.", TextToSpeech.QUEUE_FLUSH, null, null)

        startTouchNavigationPhase()
    }

    /**
     * Reprise directe à la phase Microphone
     */
    private fun resumeFromMicPhase() {
        isGameStarted = true
        Toast.makeText(this, "Reprise : Phase de souffle (Microphone)", Toast.LENGTH_SHORT).show()

        textToSpeech.speak("Reprise de la partie. Soufflez sur l'écran pour appeler le chien.", TextToSpeech.QUEUE_FLUSH, null, null)

        startMicrophonePhase()
    }

    /**
     * Rejoue la cinématique ou l'instruction de l'étape en cours.
     */
    private fun replayCurrentInstruction() {
        if (!isGameStarted) return

        Toast.makeText(this, "Répétition de la consigne...", Toast.LENGTH_SHORT).show()

        // On coupe le TextToSpeech s'il parlait
        if (::textToSpeech.isInitialized && textToSpeech.isSpeaking) {
            textToSpeech.stop()
        }

        when (currentLocalStep) {
            0 -> {
                // Phase Gyroscope (Début)
                mediaPlayerIntro?.seekTo(0)
                mediaPlayerIntro?.start() ?: startIntroSequence()
            }
            1 -> {
                // Phase Tactile
                if (mediaPlayerAfterIntro != null) {
                    mediaPlayerAfterIntro?.seekTo(0)
                    mediaPlayerAfterIntro?.start()
                } else {
                    textToSpeech.speak("Rappel : Touchez l'écran pour trouver la porte.", TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
            2 -> {
                // Phase Microphone
                textToSpeech.speak("Rappel : Soufflez sur l'écran pour appeler le chien.", TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    /**
     * Surcharge de la méthode du parent : déclenchée quand l'IA envoie "repeter: true"
     */
    override fun onReplayRequested() {
        super.onReplayRequested()
        replayCurrentInstruction()
    }

    // =========================================================================
    //                      PHASE 1 : GYROSCOPE (SensorGameListener)
    // =========================================================================

    /**
     * Répond à l'événement indiquant que la voix off d'instruction est prête.
     * Affiche l'instruction et démarre l'écoute de l'accéléromètre.
     * @param instruction Le texte de l'instruction.
     */
    override fun onInstructionReady(instruction: String) {
        Toast.makeText(this, "Voix off terminée. $instruction", Toast.LENGTH_LONG).show()
        RealtimeHelper.updateGameStatus("playing")

        RealtimeHelper.updateStep("Phase_Orientation_Gyroscope")

        if (gameManager.isAccelerometerAvailable) {
            gameManager.startListening()
        } else {
            Toast.makeText(this, "Capteur d'accélération non disponible.", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Répond à l'événement de validation d'un geste par le SensorGameManager.
     * Fournit un retour haptique (vibration) et sonore (ding), puis passe à l'étape suivante.
     * @param isGameComplete Indique si la série de gestes est terminée.
     * @param nextInstruction La prochaine instruction à donner au joueur.
     */
    override fun onGestureValidated(isGameComplete: Boolean, nextInstruction: String) {
        if (phaseStartTime > 0) totalTimeReaction += (System.currentTimeMillis() - phaseStartTime)
        phaseStartTime = System.currentTimeMillis()

        vibrator.vibrate(200)
        playSfx(R.raw.ding)

        RealtimeHelper.updateGyroStats(interruptionCount, gameManager.instabilityCount, gameManager.currentExpectedDirection, gameManager.currentActualDirection)

        if (isGameComplete) {
            FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
                FirebaseHelper.getInstance().updateGameProgress(uid, "Cécilia (cécité totale)", "introCurrentStep", 1)
            }

            currentLocalStep = 1

            gameManager.stopListening()
            mediaPlayerAfterIntro = MediaPlayer.create(this, R.raw.cecilia_after_intro)?.apply {
                setVolume(audioManager.voiceVolume, audioManager.voiceVolume)
                start()
                setOnCompletionListener { startTouchNavigationPhase() }
            }
        }
    }

    /**
     * Répond à l'événement de feedback du SensorGameManager.
     * Affiche un message toast.
     */
    override fun onFeedbackNeeded(message: String) {
        detectImpatience(message)
        if (message != "VALIDATE" && !message.contains("WRONG_DIRECTION")) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Détecte les tentatives d'impatience et les enregistre.
     */
    private fun detectImpatience(message: String) {
        if ((mediaPlayerIntro?.isPlaying == true || mediaPlayerAfterIntro?.isPlaying == true) && message != "VALIDATE") {
            interruptionCount++
            RealtimeHelper.updateGyroStats(interruptionCount, gameManager.instabilityCount, gameManager.currentExpectedDirection, gameManager.currentActualDirection)
        }
    }

    // =========================================================================
    //                      PHASE 2 & 3 : TACTILE & MICROPHONE
    // =========================================================================

    /**
     * Démarre la phase de navigation tactile
     */
    private fun startTouchNavigationPhase() {
        RealtimeHelper.updateStep("Phase_Tactile_Porte")
        phaseStartTime = System.currentTimeMillis()
        val rootView = window.decorView
        touchManager = TouchNavigationManager(this, this, rootView.width, rootView.height)
        rootView.setOnTouchListener(touchManager)
    }

    /**
     * Répond à l'événement de détection de la cible (la porte) par le TouchNavigationManager.
     * Arrête la phase tactile, joue un son de succès et lance la phase microphone.
     */
    override fun onTargetFound() {
        if (phaseStartTime > 0) totalTimeReaction += (System.currentTimeMillis() - phaseStartTime)
        window.decorView.setOnTouchListener(null)

        touchManager?.cleanup()
        touchManager = null

        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            FirebaseHelper.getInstance().updateGameProgress(uid, "Cécilia (cécité totale)", "introCurrentStep", 2)
        }

        currentLocalStep = 2

        playSfx(R.raw.success_chime) { startMicrophonePhase() }
    }

    /**
     * Démarre la phase de détection du soufflement
     */
    private fun startMicrophonePhase() {
        RealtimeHelper.updateStep("Phase_Microphone")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), MICROPHONE_PERMISSION_CODE)
        } else {
            micManager = MicrophoneManager(this).also { it.startListening() }
        }
    }

    /**
     * Gère la réponse à la demande de permissions
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MICROPHONE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Le joueur vient d'accepter, on lance le micro !
                micManager = MicrophoneManager(this).also { it.startListening() }
            } else {
                // Le joueur a refusé
                Toast.makeText(this, "Le micro est indispensable pour trouver le chien !", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Répond à l'événement de détection du chien (soufflement détecté par le micro).
     * Joue un son d'aboiement, arrête le micro
     */
    override fun onDogFound() {
        playSfx(R.raw.dog_bark)
        micManager?.stopListening()
        calculateAndSaveScore()
    }


    // =========================================================================
    //                      BILAN ET SCORE FINAL
    // =========================================================================

    /**
     * Calcul du score et envoie le score et la progression à Firebase.
     */
    private fun calculateAndSaveScore() {
        val totalDistance = touchManager?.totalDistanceTraveled ?: 0f

        // UTILISATION DE LA LOGIQUE DÉPORTÉE
        val (scoreEcoute, scoreCalme, globalScore) = logic.calculateScores(
            interruptionCount,
            gameManager.instabilityCount,
            totalDistance
        )
        val profilJoueur = logic.getPlayerProfile(globalScore)
        val (conseilEcoute, conseilCalme) = logic.getAdvice(scoreEcoute, scoreCalme)

        // SAUVEGARDE FIREBASE
        val metrics = mapOf(
            "patience_ecoute" to scoreEcoute,
            "stabilite_calme" to scoreCalme,
            "temps_total_ms" to totalTimeReaction
        )
        syncManager.checkNetworkAndSave("Cécilia (cécité totale) - Intro", globalScore, profilJoueur, metrics)

        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            FirebaseHelper.getInstance().updateGameProgress(uid, "Cécilia (cécité totale)", "introFinished", true)
            FirebaseHelper.getInstance().updateGameProgress(uid, "Cécilia (cécité totale)", "introCurrentStep", 3)
        }

        // PRÉPARATION DU TEXTE VOCAL (TTS)
        val speechText = """
            Bilan Sensoriel. Score global : $globalScore sur cent. 
            Votre profil est : $profilJoueur. 
            Score d'écoute : $scoreEcoute pour cent. $conseilEcoute
            Score de calme : $scoreCalme pour cent. $conseilCalme
        """.trimIndent()

        // DIALOGUE ET NAVIGATION
        showLevelCompleteDialog(
            score = globalScore,
            profil = profilJoueur,
            details = "👂 Écoute : $scoreEcoute%\n🧘 Calme : $scoreCalme%",
            speechText = speechText,
            nextActivityClass = CeciliaLevel1Activity::class.java
        )
    }

    // =========================================================================
    //                      SUIVI TEMPS RÉEL
    // =========================================================================

    /**
     * Reçoit la direction en temps réel depuis le capteur et l'envoie à Firebase.
     */
    override fun onDirectionChanged(actualDirection: String) {
        RealtimeHelper.updateGyroStats(
            interruptionCount,
            gameManager.instabilityCount,
            gameManager.currentExpectedDirection,
            actualDirection
        )
    }

    /**
     * Méthode utilitaire pour jouer un son.
     */
    private fun playSfx(resId: Int, onComplete: (() -> Unit)? = null) {
        MediaPlayer.create(this, resId)?.apply {
            val vol = settings.getSfxVolume()
            setVolume(vol, vol)
            setOnCompletionListener {
                it.release()
                onComplete?.invoke()
            }
            start()
        }
    }
}