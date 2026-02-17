package fr.upjv.lesombresduson.ui.game.cecilia

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import fr.upjv.lesombresduson.R
import fr.upjv.lesombresduson.data.remote.FirebaseHelper
import fr.upjv.lesombresduson.manager.input.GestureListener
import fr.upjv.lesombresduson.manager.input.TouchNavigationManager
import fr.upjv.lesombresduson.manager.sensor.MicrophoneManager
import fr.upjv.lesombresduson.manager.sensor.SensorGameManager
import fr.upjv.lesombresduson.ui.game.cecilia.util.BackGameActivity
import java.util.Locale

/**
 * Contrôleur principal pour l'activité du jeu Cecilia.
 * Implémente GestureListener pour les retours du SensorGameManager.
 */
class CeciliaIntroActivity : BackGameActivity(), GestureListener, TextToSpeech.OnInitListener {

    // Utilisation de lateinit pour les variables initialisées dans onCreate
    private lateinit var btnBack: Button
    private var tts: TextToSpeech? = null

    // Utilisation de 'by lazy' pour initialiser le Vibrator une seule fois.
    private val vibrator: Vibrator by lazy {
        getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    // Managers de jeu
    private lateinit var gameManager: SensorGameManager
    private var touchManager: TouchNavigationManager? = null
    private var micManager: MicrophoneManager? = null

    // MediaPlayers (peut être null avant l'initialisation ou après la libération)
    private var mediaPlayerIntro: MediaPlayer? = null
    private var mediaPlayerAfterIntro: MediaPlayer? = null
    private var isGameStarted = false
    private var isIntroSequenceFinished = false

    // --- VARIABLES SCORE & SENSIBILISATION ---
    private var interruptionCount = 0 // Compte l'impatience (action pendant voix off)
    private var phaseStartTime: Long = 0 // Pour le temps de réaction
    private var totalTimeReaction: Long = 0 // Cumul des temps de réaction
    // -----------------------------------------
    
    companion object {
        // Code de permission pour le microphone (pour la phase du chien)
        private const val MICROPHONE_PERMISSION_CODE = 102
    }

    // --- Cycle de vie Android ---

    /**
     * Méthode de création de l'Activity, appelée au démarrage.
     * Initialise l'interface utilisateur, le SensorGameManager et l'audio d'introduction.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gameplay_cecilia)

        tts = TextToSpeech(this, this)

        btnBack = findViewById(R.id.button_back)

        // Initialisation du manager de jeu
        gameManager = SensorGameManager(this, this)

        // Listener simplifié en Kotlin (lambda)
        setupBackButton(btnBack)

        checkVolumeAndStart()
    }

    /**
     * Méthode de destruction de l'Activity, appelée lorsque l'activité est terminée
     */
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.FRENCH
        }
    }

    /**
     * Vérifie le volume et lance le jeu ou affiche une popup.
     */
    private fun checkVolumeAndStart() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        // Si le volume est supérieur à 0, on lance. Sinon, Popup.
        if (currentVolume > 0) {
            startIntroSequence()
        } else {
            showVolumePopup()
        }
    }

    /**
     * AJOUT : Affiche la popup demandant d'activer le son.
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
     * Logique déplacée depuis onCreate. Ne se lance que si le son est OK.
     */
    private fun startIntroSequence() {
        isGameStarted = true
        isIntroSequenceFinished = false // On s'assure qu'elle est fausse au début

        if (mediaPlayerIntro == null) {
            mediaPlayerIntro = MediaPlayer.create(this, R.raw.cecilia_intro)?.apply {
                setVolume(voiceVolume, voiceVolume)
                isLooping = false
                start()
                setOnCompletionListener { mp: MediaPlayer ->
                    // C'est ICI que l'intro est officiellement finie
                    isIntroSequenceFinished = true
                    phaseStartTime = System.currentTimeMillis()
                    // On lance la suite
                    onInstructionReady("Inclinez votre téléphone à droite ! L'intro est finie.")
                }
            }
        }
    }

    /**
     * Appelée lorsque l'Activity redevient visible (après onCreate ou onPause).
     * Reprend l'audio en pause et redémarre l'écoute des capteurs si l'intro est terminée.
     */
    override fun onResume() {
        super.onResume()

        if (!isGameStarted) return

        if (!isIntroSequenceFinished && mediaPlayerIntro?.isPlaying == false) {
            mediaPlayerIntro?.start()
        }

        if (isIntroSequenceFinished && mediaPlayerAfterIntro?.isPlaying == false && gameManager.gestureCount == 0) {
            mediaPlayerAfterIntro?.start()
        }

        if (isIntroSequenceFinished && gameManager.gestureCount < 4) {
            gameManager.startListening()
        }
    }

    /**
     * Appelée lorsque l'Activity passe en arrière-plan (mais est toujours en mémoire).
     * Met en pause l'écoute des capteurs et l'audio pour économiser la batterie.
     */
    override fun onPause() {
        super.onPause()
        gameManager.stopListening()

        mediaPlayerIntro?.pause()
        mediaPlayerAfterIntro?.pause()
    }

    /**
     * Appelée lorsque l'Activity est définitivement détruite.
     * Nettoie toutes les ressources : capteurs, managers et MediaPlayers.
     */
    override fun onDestroy() {
        super.onDestroy()
        gameManager.stopListening()

        tts?.stop()
        tts?.shutdown()

        mediaPlayerIntro?.let {
            it.stop()
            it.release()
            mediaPlayerIntro = null
        }

        mediaPlayerAfterIntro?.let {
            it.stop()
            it.release()
            mediaPlayerAfterIntro = null
        }

        touchManager?.cleanup()
        micManager?.stopListening()
    }

    // --- Implémentation de GestureListener (Réactions du jeu) ---

    /**
     * Répond à l'événement indiquant que la voix off d'instruction est prête.
     * Affiche l'instruction et démarre l'écoute de l'accéléromètre.
     * @param instruction Le texte de l'instruction.
     */
    override fun onInstructionReady(instruction: String) {
        Toast.makeText(this, "Voix off terminée. $instruction", Toast.LENGTH_LONG).show()
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
        // Calcul du temps de réaction pour ce geste
        if (phaseStartTime > 0) {
            totalTimeReaction += (System.currentTimeMillis() - phaseStartTime)
        }
        phaseStartTime = System.currentTimeMillis() // Reset pour le prochain geste

        @Suppress("DEPRECATION") // Utilisation standard du Vibrator
        vibrator.vibrate(200)

        // Utilisation de 'use' pour garantir que le MediaPlayer est bien relâché (release)
        MediaPlayer.create(this, R.raw.ding)?.apply {
            setVolume(sfxVolume, sfxVolume)
            setOnCompletionListener { mp ->
                mp.release() // Libère la ressource après la lecture
            }
            start()
        }

        if (isGameComplete) {
            Toast.makeText(this, "🎉 Tous les gestes sont complétés. Le jeu peut continuer !", Toast.LENGTH_LONG).show()
            gameManager.stopListening()

            mediaPlayerAfterIntro = MediaPlayer.create(this, R.raw.cecilia_after_intro)?.apply {
                setVolume(voiceVolume, voiceVolume)
                isLooping = false
                start()
                setOnCompletionListener { mp: MediaPlayer -> // Correction pour éviter l'ambiguïté du type
                    startTouchNavigationPhase()
                }
            }
        } else {
            Toast.makeText(this, "✅ Geste Validé ! Relâchez et inclinez $nextInstruction.", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Répond aux retours (feedback) du SensorGameManager (ex: "Maintenez 3s..." ou "Annulation").
     * Affiche un Toast informatif pour le joueur.
     * @param message Le message à afficher.
     */
    override fun onFeedbackNeeded(message: String) {
        // --- DETECTION IMPATIENCE ---
        // Si le joueur agit alors qu'un son de voix off important joue encore
        val isIntroPlaying = mediaPlayerIntro?.isPlaying == true
        val isTransitionPlaying = mediaPlayerAfterIntro?.isPlaying == true

        if ((isIntroPlaying || isTransitionPlaying) && message != "VALIDATE") {
            interruptionCount++ // PENALITÉ : Le joueur n'écoute pas !
        }
        // ----------------------------
        if (message == "VALIDATE") {
            // L'action de validation est déjà gérée dans onGestureValidated
            return
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Démarre la phase de navigation tactile
     */
    private fun startTouchNavigationPhase() {
        Toast.makeText(this, "Glissez votre doigt sur l'écran pour chercher la porte. Le son vous guidera.", Toast.LENGTH_LONG).show()

        // Reset chrono pour la phase tactile
        phaseStartTime = System.currentTimeMillis()

        // 1. Obtenir les dimensions de l'écran (pour la cible)
        val rootView = window.decorView
        val width = rootView.width
        val height = rootView.height

        // 2. Initialiser le manager
        touchManager = TouchNavigationManager(this, this, width, height)

        // 3. Attacher le manager à l'écoute des événements tactiles sur la vue racine
        rootView.setOnTouchListener(touchManager)
    }

    /**
     * Répond à l'événement de détection de la cible (la porte) par le TouchNavigationManager.
     * Arrête la phase tactile, joue un son de succès et lance la phase microphone.
     */
    override fun onTargetFound() {
        // Ajout du temps tactile au temps de réaction global
        if (phaseStartTime > 0) {
            totalTimeReaction += (System.currentTimeMillis() - phaseStartTime)
        }

        // 1. Nettoyage du manager tactile
        touchManager?.let {
            val rootView = window.decorView
            rootView.setOnTouchListener(null)
            it.cleanup()
            touchManager = null
        }

        Toast.makeText(this, "VICTOIRE ! La porte est trouvée. Bravo !", Toast.LENGTH_LONG).show()

        // Utilisation de 'use' pour garantir la libération de la ressource audio
        MediaPlayer.create(this, R.raw.success_chime)?.apply {
            setVolume(sfxVolume, sfxVolume)
            setOnCompletionListener { mp: MediaPlayer ->
                mp.release() // Libérer la ressource après la lecture
                startMicrophonePhase() // Ensuite, démarrer la phase suivante
            }
            start()
        } ?: startMicrophonePhase() // Si la création du player échoue, démarrer la phase suivante immédiatement
    }

    /**
     * Démarre la phase de détection du soufflement
     */
    private fun startMicrophonePhase() {
        // Vérification et demande de permission RECORD_AUDIO si nécessaire
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), MICROPHONE_PERMISSION_CODE)
        } else {
            initAndStartMicManager()
        }
    }

    // Initialisation et démarrage du MicrophoneManager
    private fun initAndStartMicManager() {
        micManager = MicrophoneManager(this)
        micManager?.startListening()
    }

    /**
     * Gère le résultat de la demande de permission (après la demande de RECORD_AUDIO).
     * Si la permission est accordée, initialise et démarre le MicrophoneManager.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == MICROPHONE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initAndStartMicManager() // Permission accordée
            } else {
                Toast.makeText(this, "Le microphone est nécessaire pour cette phase du jeu.", Toast.LENGTH_LONG).show()
                // Le jeu peut continuer d'une autre manière ou s'arrêter.
            }
        }
    }

    /**
     * Répond à l'événement de détection du chien (soufflement détecté par le micro).
     * Joue un son d'aboiement, arrête le micro
     */
    override fun onDogFound() {
        // Fin du jeu
        MediaPlayer.create(this, R.raw.dog_bark)?.apply {
            setVolume(sfxVolume, sfxVolume)
            setOnCompletionListener { it.release() }
            start()
        }

        micManager?.stopListening()

        // --- CALCUL DU SCORE DE SENSIBILISATION ---
        calculateAndSaveScore()
    }

    /**
     * Calcul du score et envoie le score et la progression à Firebase.
     *
     * */
    private fun calculateAndSaveScore() {
        // 1. ÉCOUTE (Patience) : Basé sur le nombre d'interruptions
        // Chaque interruption enlève 15% de patience
        val scoreEcoute = (100 - (interruptionCount * 15)).coerceIn(0, 100)

        // 2. CALME (Surcharge Sensorielle)
        // Gyro : InstabilityCount (combien de resets)
        // Tactile : Distance parcourue.
        // Si distance < 3000px = Calme. Si > 10000px = Panique.
        val distance = touchManager?.totalDistanceTraveled ?: 0f
        val stabilityGyro = (100 - (gameManager.instabilityCount * 10)).coerceIn(0, 100)

        // Formule arbitraire pour le tactile : 100 pts - 1 pt tous les 100 pixels au dessus de 1500
        val penaltyTactile = ((distance - 1500) / 100).toInt().coerceAtLeast(0)
        val stabilityTouch = (100 - penaltyTactile).coerceIn(0, 100)

        val scoreCalme = (stabilityGyro + stabilityTouch) / 2

        // 3. SCORE GLOBAL (Moyenne pondérée)
        // L'écoute est le plus important (60%), le calme ensuite (40%)
        val globalScore = ((scoreEcoute * 0.6) + (scoreCalme * 0.4)).toInt()

        // Attribution d'un "Profil"
        val profilJoueur = when {
            globalScore > 85 -> "L'Oreille Absolue"
            globalScore > 60 -> "L'Apprenti Attentif"
            else -> "Le Visuel Pressé"
        }

        // --- ENVOI FIREBASE ---
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val stats = hashMapOf<String, Any>(
                "score_global" to globalScore,
                "profil" to profilJoueur,
                "metriques" to hashMapOf<String, Any>(
                    "patience_ecoute" to scoreEcoute,
                    "stabilite_calme" to scoreCalme,
                    "temps_total_ms" to totalTimeReaction
                ),
                "debug_info" to hashMapOf<String, Any>(
                    "interruptions" to interruptionCount,
                    "distance_doigt" to distance
                )
            )

            FirebaseHelper.getInstance().saveLevelStats(userId,  "Cécilia (cécité totale)", "Intro", stats)

            FirebaseHelper.getInstance().updateGameProgress(userId,  "Cécilia (cécité totale)", "introFinished", true)
        }

        // --- PRÉPARATION DU TEXTE VOCAL ---
        val conseilEcoute = if (scoreEcoute < 50) "Prenez le temps d'écouter les instructions." else ""
        val conseilCalme = if (scoreCalme < 50) "Essayez de limiter les mouvements brusques." else ""

        val speechText = """
            Bilan Sensoriel. Score global : $globalScore sur cent. 
            Votre profil est : $profilJoueur. 
            Score d'écoute : $scoreEcoute pour cent. $conseilEcoute
            Score de calme : $scoreCalme pour cent. $conseilCalme
        """.trimIndent()

        // 1. On prépare l'action de navigation
        val goToNextLevel = {
            tts?.stop() // On coupe la parole si on change de niveau
            val intent = android.content.Intent(this, CeciliaLevel1Activity::class.java)
            startActivity(intent)
            finish()
        }

        // 2. On prépare le Timer (Handler)
        val handler = Handler(Looper.getMainLooper())
        val autoStartRunnable = Runnable {
            // Ce code s'exécutera après 10 secondes
            if (!isFinishing) {
                goToNextLevel()
            }
        }

        // 3. On construit la boite de dialogue
        val dialog = AlertDialog.Builder(this)
            .setTitle("Bilan Sensoriel : $globalScore/100")
            .setMessage("Profil : $profilJoueur\n\n" +
                    "👂 Écoute : $scoreEcoute% ${(if(scoreEcoute<50) "⚠️ Prenez le temps d'écouter." else "✅")}\n" +
                    "🧘 Calme : $scoreCalme% ${(if(scoreCalme<50) "⚠️ Trop de mouvements parasites." else "✅")}\n\n" +
                    "⏳ Démarrage automatique dans 10s...")
            .setPositiveButton("Commencer l'histoire") { dialogInterface, _ ->
                // Si l'utilisateur clique, on ANNULE le timer automatique
                handler.removeCallbacks(autoStartRunnable)
                goToNextLevel()
            }
            .setCancelable(false)
            .create() // On crée l'objet mais on ne l'affiche pas tout de suite

        // 4. On affiche et on lance le chrono
        dialog.show()

        tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "BILAN_ID")

        handler.postDelayed(autoStartRunnable, 10000) // 10 000 ms = 10 secondes
    }
}