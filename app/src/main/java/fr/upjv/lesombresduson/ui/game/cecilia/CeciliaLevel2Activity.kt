package fr.upjv.lesombresduson.ui.game.cecilia

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.Button
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import fr.upjv.lesombresduson.R
import fr.upjv.lesombresduson.data.remote.RealtimeHelper
import fr.upjv.lesombresduson.ui.game.cecilia.util.BackGameActivity
import kotlin.random.Random

/**
 * Implémentation du Niveau 2 : "Un, deux, trois, soleil" sonore.
 * Le joueur doit maintenir le doigt appuyé au feu vert (son A) et relâcher au feu rouge (son B).
 * La difficulté réside dans l'aléatoire des délais et les distractions sonores.
 */
class CeciliaLevel2Activity : BackGameActivity() {

    override val sessionName = "Niveau2"
    // --- DEPENDANCES ---
    private lateinit var level2AudioManager: CeciliaAudioManager

    // --- GAME LOOP HANDLERS ---
    private val mainHandler = Handler(Looper.getMainLooper())
    private val distractionHandler = Handler(Looper.getMainLooper())

    // --- STATE MACHINE ---
    private var isGameReady = false
    private var isLevelComplete = false
    private var isGameLost = false

    private var isGreenLight = false      // État du feu (Vert=Appui, Rouge=Relâche)
    private var isFingerPressed = false   // État de l'input joueur

    // --- TIMING & METRICS ---
    private var timeRedLightStarted: Long = 0
    private var stepsSuccess = 0
    private val GOAL_STEPS = 3
    private var nextLightChangeTimestamp: Long = 0L // Mémorise l'heure du prochain changement

    // Stats pour le scoring
    private var cumulativeReactionTime: Long = 0
    private var falseStartCount = 0     // Relâchement prématuré (sur vert)
    private var distractionErrors = 0   // Fautes directes ou distractions

    // Constantes de gameplay
    private val REACTION_TOLERANCE_MS = 500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialisation Audio
        level2AudioManager = CeciliaAudioManager(this)
        level2AudioManager.setVolumes(settings.getSfxVolume(), settings.getVoiceVolume())
        level2AudioManager.init()

        // Séquence de démarrage
        level2AudioManager.onAudioReady = {
            mainHandler.post {
                audioManager.playIntro(R.raw.voix_off_niveau2) {
                    startGameLoop()
                }
            }
        }
    }

    /**
     * Lance la machine à états du jeu.
     * Active les boucles de distractions et de changement de feux.
     */
    private fun startGameLoop() {
        isGameReady = true
        stepsSuccess = 0
        isGameLost = false

        // Reset métriques
        cumulativeReactionTime = 0
        falseStartCount = 0
        distractionErrors = 0

        level2AudioManager.playAmbiance()

        // Démarrage des boucles asynchrones
        mainHandler.post(checkRulesRunnable)
        distractionHandler.post(distractionRunnable)
        cycleTrafficLights()
    }

    // --- LOGIQUE DE JEU (CORE LOOP) ---

    /**
     * Changement aléatoire des feux rouges/verts.
     */
    private fun cycleTrafficLights() {
        if (isLevelComplete || !isGameReady || isGameLost) return

        isGreenLight = !isGreenLight
        level2AudioManager.playSignal(isGreenLight)

        if (isGreenLight) {
            val duration = Random.nextLong(2000, 8000)
            nextLightChangeTimestamp = System.currentTimeMillis() + duration // On calcule le futur
            mainHandler.postDelayed({ cycleTrafficLights() }, duration)
        } else {
            timeRedLightStarted = System.currentTimeMillis()
            val duration = Random.nextLong(3000, 6000)
            nextLightChangeTimestamp = System.currentTimeMillis() + duration // On calcule le futur
            mainHandler.postDelayed({ cycleTrafficLights() }, duration)
        }

        // On envoie les nouvelles données à Firebase
        RealtimeHelper.updateTimingStats(
            isGreenLight, isFingerPressed, falseStartCount, distractionErrors,
            nextLightChangeTimestamp, stepsSuccess
        )
    }

    /**
     * Vérifie en temps réel si le joueur échoue à relâcher au feu rouge.
     */
    private val checkRulesRunnable = object : Runnable {
        override fun run() {
            if (!isGameReady || isLevelComplete || isGameLost) return

            if (!isGreenLight && isFingerPressed) {
                val timeSinceRed = System.currentTimeMillis() - timeRedLightStarted

                // Timeout dépassé -> Échec
                if (timeSinceRed > REACTION_TOLERANCE_MS) {
                    triggerGameOver("Trop lent ! Relâchez plus vite.")
                    return
                }
            }
            mainHandler.postDelayed(this, 50) // Polling 20Hz
        }
    }

    /**
     * Générateur de chaos : Sons parasites aléatoires.
     */
    private val distractionRunnable = object : Runnable {
        override fun run() {
            if (!isGameReady || isLevelComplete || isGameLost) return

            // 30% de probabilité de distraction
            if (Random.nextDouble() > 0.7) level2AudioManager.playDistraction()

            distractionHandler.postDelayed(this, Random.nextLong(500, 3000))
        }
    }

    // --- INPUT HANDLING ---

    /**
     * Gestionnaire central des interactions tactiles.
     * Implémente la mécanique "Maintenir pour attendre / Relâcher pour avancer".
     * Gère la détection des fautes directes (appuis au rouge) et des relâchements prématurés.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isLevelComplete || !isGameReady) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isFingerPressed = true

                RealtimeHelper.updateTimingStats(
                    isGreenLight,
                    isFingerPressed,
                    falseStartCount,
                    distractionErrors,
                    nextLightChangeTimestamp,
                    stepsSuccess
                )

                // Faute directe : Appui pendant le rouge (hors tolérance réflexe)
                if (!isGreenLight && !isGameLost) {
                    falseStartCount++
                    triggerGameOver("Attendez le signal sonore !")
                }
            }
            MotionEvent.ACTION_UP -> {
                isFingerPressed = false

                RealtimeHelper.updateTimingStats(
                    isGreenLight,
                    isFingerPressed,
                    falseStartCount,
                    distractionErrors,
                    nextLightChangeTimestamp,
                    stepsSuccess
                )

                if (isGameLost) {
                    resetLevelState()
                    return true
                }

                if (isGreenLight) {
                    // Erreur mineure : Relâchement prématuré
                    falseStartCount++
                    Toast.makeText(this, "Maintenez appuyé !", Toast.LENGTH_SHORT).show()
                } else {
                    // Succès : Relâchement correct au rouge
                    val reactionTime = System.currentTimeMillis() - timeRedLightStarted
                    cumulativeReactionTime += reactionTime
                    validateStep()
                }
            }
        }
        return true
    }

    // --- GAME STATE MANAGEMENT ---

    /**
     * Valide une séquence de réaction réussie.
     * Incrémente la progression et vérifie la condition de victoire (GOAL_STEPS).
     */
    private fun validateStep() {
        stepsSuccess++
        if (stepsSuccess >= GOAL_STEPS) {
            handleVictory()
        } else {
            level2AudioManager.playSuccess()
            Toast.makeText(this, "Voie $stepsSuccess franchie !", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Déclenche l'état d'échec du niveau.
     * Interrompt les boucles de jeu, fournit un feedback négatif immédiat (Audio/Haptique)
     * et enregistre l'erreur pour les statistiques de concentration.
     */
    private fun triggerGameOver(reason: String) {
        isGameLost = true
        stopLoops()

        level2AudioManager.stopGameSounds()
        level2AudioManager.playEchec()
        hapticManager.vibrate(500) // Feedback haptique erreur

        Toast.makeText(this, reason, Toast.LENGTH_SHORT).show()
        distractionErrors++ // Comptabilisé comme erreur d'attention

        RealtimeHelper.updateTimingStats(
            isGreenLight,
            isFingerPressed,
            falseStartCount,
            distractionErrors,
            nextLightChangeTimestamp,
            stepsSuccess
        )
    }

    /**
     * Réinitialise la machine à états pour une nouvelle tentative.
     * Inclut un délai de sécurité (Cool-down) pour éviter les inputs accidentels lors de la transition.
     */
    private fun resetLevelState() {
        stopLoops()
        stepsSuccess = 0
        isGameLost = false
        isFingerPressed = false

        // Cool-down avant restart
        mainHandler.postDelayed({
            if (!isLevelComplete) {
                isGreenLight = true // Force le reset d'état
                startGameLoop()
            }
        }, 2000)
    }

    /**
     * Finalise le niveau en cas de succès.
     * Gèle l'état du jeu et initie le calcul des scores.
     */
    private fun handleVictory() {
        isLevelComplete = true
        stopLoops()
        level2AudioManager.stopGameSounds()
        hapticManager.vibrateVictory() // Pattern haptique complexe

        calculateAndSaveScore()
    }

    // --- SCORING & PERSISTENCE ---

    /**
     * Calcule les métriques de performance et détermine le profil du joueur.
     * - Score Réflexe (70%) : Basé sur la moyenne des temps de réaction.
     * - Score Concentration (30%) : Basé sur le nombre d'erreurs et de faux départs.
     */
    private fun calculateAndSaveScore() {
        // 1. Réflexe (Moyenne ms)
        val avgReactionTime = if (stepsSuccess > 0) cumulativeReactionTime / stepsSuccess else REACTION_TOLERANCE_MS
        // Scoring : 300ms = 100pts, 600ms = 0pts
        val scoreReflexe = ((600 - avgReactionTime).toDouble() / 3.0).toInt().coerceIn(0, 100)

        // 2. Concentration (Basé sur les erreurs)
        val totalErrors = falseStartCount + distractionErrors
        val scoreConcentration = (100 - (totalErrors * 25)).coerceIn(0, 100)

        // Score pondéré : 70% Réflexe, 30% Concentration
        val globalScore = ((scoreReflexe * 0.7) + (scoreConcentration * 0.3)).toInt()

        val profilJoueur = when {
            avgReactionTime < 350 -> "Le Lynx Sonore"
            scoreConcentration == 100 -> "Le Sage Imperturbable"
            else -> "Le Piéton Prudent"
        }

        saveToFirebase(globalScore, profilJoueur, scoreReflexe, scoreConcentration, avgReactionTime)

        // Affichage UI via BackGameActivity
        val details = "⚡ Réflexe : ${avgReactionTime}ms ($scoreReflexe%)\n🧠 Concentration : $scoreConcentration%"

        // Texte vocal simplifié
        val speechText = """
            Niveau deux terminé. Score global : $globalScore sur cent. 
            Votre profil est : $profilJoueur. 
            Votre temps de réaction moyen est de $avgReactionTime millisecondes. 
            Score de réflexe : $scoreReflexe pour cent. 
            Score de concentration : $scoreConcentration pour cent.
        """.trimIndent()

        showLevelCompleteDialog(
            score = globalScore,
            profil = profilJoueur,
            details = details,
            speechText = speechText,
            nextActivityClass = CeciliaLevel3Activity::class.java
        )
    }

    /***
     * Met à jour la progression globale et stocke les métriques détaillées pour l'analytics.
     */
    private fun saveToFirebase(score: Int, profil: String, reflexe: Int, conc: Int, ms: Long) {
        val metrics = mapOf(
            "reflexe_ms" to ms,
            "score_reflexe" to reflexe,
            "score_concentration" to conc
        )

        syncManager.checkNetworkAndSave("Niveau2", score, profil, metrics)
    }

    // --- CLEANUP ---

    /**
     * Interrompt tous les Runnables en attente pour éviter les fuites de mémoire.
     */
    private fun stopLoops() {
        mainHandler.removeCallbacksAndMessages(null)
        distractionHandler.removeCallbacksAndMessages(null)
    }

    /**
     * Nettoyage du cycle de vie.
     * Assure la libération des ressources audio et l'arrêt des threads UI.
     */
    override fun onDestroy() {
        super.onDestroy()
        stopLoops()
    }
}