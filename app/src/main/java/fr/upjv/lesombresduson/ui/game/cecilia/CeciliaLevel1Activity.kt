package fr.upjv.lesombresduson.ui.game.cecilia

import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import fr.upjv.lesombresduson.R
import fr.upjv.lesombresduson.data.remote.RealtimeHelper
import fr.upjv.lesombresduson.manager.sensor.Level1SensorListener
import fr.upjv.lesombresduson.manager.sensor.Level1SensorManager
import fr.upjv.lesombresduson.ui.game.cecilia.logic.Level1SoundEngine
import fr.upjv.lesombresduson.ui.game.cecilia.util.BackGameActivity

/**
 * Point d'entrée du Niveau 1 (Orientation Urbaine).
 * Orchestre la boucle de gameplay basée sur l'écholocation (Tap-to-hear)
 * et la validation de mouvements via les capteurs.
 */
class CeciliaLevel1Activity : BackGameActivity(), Level1SensorListener {

    override val sessionName = "Niveau1"

    // --- MOTEURS & LOGIQUE ---
    private lateinit var sensorManager: Level1SensorManager // Abstraction des capteurs mvt
    private lateinit var soundEngine: Level1SoundEngine     // Gestion audio faible latence

    // --- STATE MACHINE ---
    private var isIntroFinished = false
    private var isWon = false
    private var startTime: Long = 0

    // --- MÉTRIQUES (SCORING) ---
    private var interruptionCount = 0   // Spam pendant la narration
    private var sonarTapCount = 0       // Efficacité de l'écholocation
    private var movementErrorCount = 0  // Précision des gestes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Instanciation des sous-systèmes
        soundEngine = Level1SoundEngine(this, settings.getSfxVolume(), settings.getMusicVolume())
        sensorManager = Level1SensorManager(this, this)

        // Séquence de démarrage : Intro -> Callback -> Gameplay actif
        audioManager.playIntro(R.raw.voix_off_niveau1) {
            isIntroFinished = true
            startTime = System.currentTimeMillis()

            RealtimeHelper.updateGameStatus("playing")
            RealtimeHelper.updateStep("Phase_Echolocation")

            // On active les capteurs uniquement après l'intro pour éviter le bruit
            sensorManager.startListening()
            hapticManager.vibrateSuccess()
        }
    }

    /**
     * Boolean indiquant si le jeu est en cours.
     */
    override fun isPlaying(): Boolean {
        return isIntroFinished
    }

    /**
     * Gestionnaire d'interaction tactile unique.
     * Sert de détecteur d'impatience (Intro) ou de déclencheur Sonar (Jeu).
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            // Cas 1 : Le joueur tape pendant le dialogue (Pénalité Patience)
            if (!isIntroFinished) {
                interruptionCount++
                RealtimeHelper.updateSonarStats(interruptionCount, 0, true)
                return super.onTouchEvent(event)
            }

            // Cas 2 : Gameplay actif
            if (isWon) {
                soundEngine.playCarAmbience()
            } else {
                // Feedback sonore de progression (Mécanique "Sonar")
                sonarTapCount++
                soundEngine.playStepSound(sensorManager.gestureCount)

                // On envoie l'info du Sonar à Firebase pour l'IA
                val optimalTaps = soundEngine.getSize() * 2
                val isSpamming = sonarTapCount > optimalTaps
                RealtimeHelper.updateSonarStats(sonarTapCount, optimalTaps, isSpamming)
            }
        }
        return super.onTouchEvent(event)
    }

    // --- IMPLEMENTATION CAPTEURS (Level1SensorListener) ---

    /**
     * Réagit aux événements de mouvement détectés par le Level1SensorManager.
     */
    override fun onFeedbackNeeded(message: String) {
        // On ignore les feedbacks capteurs si le jeu n'a pas officiellement commencé
        if (!isIntroFinished) return

        if (message == "VALIDATE") {
            // Envoi de la réussite à l'IA
            RealtimeHelper.updateGyroStats(
                movementErrorCount,
                0,
                sensorManager.currentExpectedDirection,
                sensorManager.currentActualDirection
            )
            hapticManager.vibrateSuccess()
            Toast.makeText(this, "Son identifié", Toast.LENGTH_SHORT).show()

        } else if (message == "WRONG_DIRECTION") {
            // Le joueur penche du mauvais côté
            movementErrorCount++
            RealtimeHelper.updateGyroStats(
                movementErrorCount,
                1, // Considéré comme une instabilité/erreur
                sensorManager.currentExpectedDirection,
                sensorManager.currentActualDirection
            )
            // Pas de Toast pour ne pas spammer

        } else if (message == "Position perdue.") {
            // Le joueur a relâché trop tôt
            movementErrorCount++
            RealtimeHelper.updateGyroStats(
                movementErrorCount,
                1,
                sensorManager.currentExpectedDirection,
                sensorManager.currentActualDirection
            )
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        } else {
            // Mouvement en cours ("Mouvement vers le haut détecté...")
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Réagit aux événements de validation de geste par le SensorGameManager.
     */
    override fun onGestureValidated(isGameComplete: Boolean, nextInstruction: String) {
        // Sécurité : on ne valide rien tant que l'intro tourne
        if (isGameComplete && isIntroFinished) {
            finishLevel()
        }
    }

    // --- LOGIQUE DE FIN & SCORING ---

    /**
     * Termine le niveau en cours.
     */
    private fun finishLevel() {
        isWon = true
        // Arrêt immédiat des capteurs pour économiser la batterie
        sensorManager.stopListening()
        hapticManager.vibrateVictory()

        calculateAndSaveScore()
    }

    /**
     * Calcule le score final selon 3 axes pondérés :
     * 1. Patience (20%) : Respect de la narration.
     * 2. Calme (30%) : Utilisation parcimonieuse du sonar.
     * 3. Précision (50%) : Qualité des mouvements réalisés.
     */
    private fun calculateAndSaveScore() {
        val totalTimeSeconds = (System.currentTimeMillis() - startTime) / 1000

        // 1. Patience
        val scorePatience = (100 - (interruptionCount * 20)).coerceIn(0, 100)

        // 2. Calme (Pénalité si spam excessif du sonar)
        val optimalTaps = soundEngine.getSize() * 2 // Marge de tolérance x2
        val sonarPenalty = if (sonarTapCount > optimalTaps) (sonarTapCount - optimalTaps) * 2 else 0
        val scoreCalme = (100 - sonarPenalty).coerceIn(0, 100)

        // 3. Précision
        val scorePrecision = (100 - (movementErrorCount * 10)).coerceIn(0, 100)

        // Aggregation
        val globalScore = ((scorePrecision * 0.5) + (scoreCalme * 0.3) + (scorePatience * 0.2)).toInt()

        val profilJoueur = when {
            globalScore > 85 -> "Le Navigateur Serein"
            globalScore > 60 -> "L'Explorateur Urbain"
            else -> "Le Passant Confus"
        }

        // Persistance
        saveToFirebase(globalScore, profilJoueur, scorePatience, scoreCalme, scorePrecision, totalTimeSeconds)

        // Feedback UI via BackGameActivity
        val details = "🧭 Orientation : $scorePrecision%\n🧠 Calme : $scoreCalme%\n⏳ Patience : $scorePatience%"

        // Texte vocal simplifié
        val speechText = """
            Niveau terminé. Score global : $globalScore sur cent. 
            Votre profil est : $profilJoueur. 
            Précision : $scorePrecision pour cent. 
            Calme : $scoreCalme pour cent. 
            Patience : $scorePatience pour cent.
        """.trimIndent()

        showLevelCompleteDialog(
            score = globalScore,
            profil = profilJoueur,
            details = details,
            speechText = speechText,
            nextActivityClass = CeciliaLevel2Activity::class.java
        )
    }

    /**
     * Sauvergarde dans la base de données
     */
    private fun saveToFirebase(score: Int, profil: String, patience: Int, calme: Int, precision: Int, time: Long) {
        val metrics = mapOf(
            "patience" to patience,
            "calme_sonar" to calme,
            "precision_mouvement" to precision,
            "temps_total_sec" to time
        )

        syncManager.checkNetworkAndSave( "Niveau1", score, profil, metrics)
    }

    // --- LIFECYCLE MANAGEMENT ---

    /**
     * Mise en route des capteurs lorsque l'activité devient visible.
     */
    override fun onResume() {
        super.onResume()

        // Reprise des capteurs uniquement si le jeu est en cours
        if (isIntroFinished && !isWon) sensorManager.startListening()
    }

    /**
     * Arrêt des capteurs lorsque l'activité est mise en arrière-plan.
     */
    override fun onPause() {
        super.onPause()
        // Pause impérative des capteurs (battery drain)
        sensorManager.stopListening()
    }

    /**
     * Nettoyage des ressources avant destruction de l'activité.
     */
    override fun onDestroy() {
        super.onDestroy()
        soundEngine.release()
    }
}