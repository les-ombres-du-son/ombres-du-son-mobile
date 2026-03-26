package fr.upjv.lesombresduson.ui.game.cecilia

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.widget.Toast
import fr.upjv.lesombresduson.R
import fr.upjv.lesombresduson.data.remote.RealtimeHelper
import fr.upjv.lesombresduson.ui.game.cecilia.util.BackGameActivity
import kotlin.math.hypot
import kotlin.random.Random

/**
 * Niveau 3 : Navigation à l'aveugle (Dead Reckoning).
 * Le joueur doit localiser 5 cibles dans un espace 2D en se basant uniquement
 * sur le feedback haptique (texture du sol et intensité de vibration).
 */
class CeciliaLevel3Activity : BackGameActivity(), SensorEventListener {

    override val sessionName = "Niveau3"

    // --- SYSTEM SERVICES ---
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // --- GAME LOOP & STATE ---
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isGameRunning = false
    private var isPausedForSuccess = false // Verrouillage temporaire lors d'une validation

    // --- PHYSICS ENGINE (0-100 grid) ---
    private var playerX = 50f
    private var playerY = 50f
    private var targetX = 0f
    private var targetY = 0f

    // --- PROGRESSION ---
    private var targetsFound = 0
    private val totalTargets = 5
    private val winThreshold = 15f   // Rayon de validation
    private val maxDistance = 100f   // Rayon pour le scaling de l'intensité

    // --- ANALYTICS ---
    private var startTime: Long = 0
    private var totalDistanceTraveled: Float = 0f
    private var optimalDistanceAccumulated: Float = 0f
    private var lastPlayerX = 50f
    private var lastPlayerY = 50f

    // Variables de temporisation pour Firebase
    private var previousDistance = Float.MAX_VALUE
    private var lastFirebaseReportTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initSensors()

        // Séquence d'initialisation : Intro Audio -> Démarrage Engine
        audioManager.playIntro(R.raw.voix_off_niveau3) {
            startGame()
        }
    }

    /**
     * Boolean indiquant si le jeu est en cours.
     */
    override fun isPlaying(): Boolean {
        return isGameRunning && targetsFound < totalTargets
    }

    private fun initSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    /**
     * Initialisation de la session de jeu.
     * Reset des vecteurs de position et des compteurs de performance.
     */
    private fun startGame() {
        isGameRunning = true
        targetsFound = 0
        totalDistanceTraveled = 0f
        optimalDistanceAccumulated = 0f

        RealtimeHelper.updateGameStatus("playing")
        RealtimeHelper.updateStep("Phase_Navigation_Haptique")

        spawnNewTarget()
        startTime = System.currentTimeMillis()

        Toast.makeText(this, "Trouvez les 5 zones de vibration !", Toast.LENGTH_SHORT).show()

        // Enregistrement capteur avec fréquence GAME (20ms approx)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        mainHandler.post(gameLoopRunnable)
    }

    /**
     * Boucle principale (Game Loop).
     * Exécutée à ~20Hz pour mettre à jour la logique sans bloquer le UI Thread.
     */
    private val gameLoopRunnable = object : Runnable {
        override fun run() {
            if (isGameRunning && !isPausedForSuccess) {
                updatePhysicsAndFeedback()
                mainHandler.postDelayed(this, 50)
            }
        }
    }

    /**
     * Calcul de la distance vectorielle et génération du feedback haptique dynamique.
     * Module l'amplitude selon la proximité et le pattern selon le type de terrain virtuel.
     */
    private fun updatePhysicsAndFeedback() {
        val distance = hypot((targetX - playerX).toDouble(), (targetY - playerY).toDouble()).toFloat()

        // Hitbox detection
        if (distance < winThreshold) {
            handleTargetFound()
            return
        }

        // Télémétrie IA (Chaud/Froid)
        val isApproaching = distance < previousDistance
        previousDistance = distance

        val currentTime = System.currentTimeMillis()
        // On envoie à Firebase toutes les 500ms pour ne pas spammer le réseau
        if (currentTime - lastFirebaseReportTime > 500) {
            RealtimeHelper.updateHapticNavigationStats(
                playerX, playerY, targetX, targetY, distance, isApproaching, targetsFound
            )
            lastFirebaseReportTime = currentTime
        }

        // Mapping distance -> intensité (Courbe exponentielle pour finesse en approche finale)
        val normalizedDist = (1f - (distance / maxDistance)).coerceIn(0f, 1f)
        val amplitude = (normalizedDist * normalizedDist * 255).toInt().coerceIn(10, 255)

        // Mapping position Y -> Texture sol
        // < 33: Béton (Constant), < 66: Herbe (Amorti), > 66: Gravier (Impulsions)
        val terrainType = when {
            playerY < 33 -> "CONCRETE"
            playerY < 66 -> "GRASS"
            else -> "PATH"
        }

        renderHapticTexture(terrainType, amplitude)
    }

    /**
     * Génère une texture haptique.
     * on garde la logique de texture ici pour la précision.
     */
    private fun renderHapticTexture(terrain: String, amplitude: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            hapticManager.vibrate(50) // Fallback legacy
            return
        }

        val effect = when (terrain) {
            "GRASS" -> VibrationEffect.createOneShot(60, (amplitude * 0.6).toInt().coerceAtLeast(1))
            "PATH" -> if (System.currentTimeMillis() % 200 < 100) VibrationEffect.createOneShot(60, amplitude) else null
            else -> VibrationEffect.createOneShot(60, amplitude) // CONCRETE
        }

        effect?.let {
            // On accède au vibrator système via le context car HapticManager encapsule trop pour ce cas précis
            (getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator).vibrate(it)
        }
    }

    /**
     * Gestion de la validation d'étape.
     * Pause temporaire de la boucle physique pour jouer le feedback de succès.
     */
    private fun handleTargetFound() {
        isPausedForSuccess = true
        targetsFound++
        hapticManager.vibrateVictory() // Feedback validation étape

        // Réinitialisation de la distance précédente pour la prochaine cible
        previousDistance = Float.MAX_VALUE

        if (targetsFound >= totalTargets) {
            stopGame()
            calculateAndSaveScore()
        } else {
            Toast.makeText(this, "Trouvé ! $targetsFound/$totalTargets", Toast.LENGTH_SHORT).show()

            // Délai de transition
            mainHandler.postDelayed({
                spawnNewTarget()
                isPausedForSuccess = false
                mainHandler.post(gameLoopRunnable)
            }, 1500)
        }
    }

    /**
     * Génération procédurale de la prochaine cible.
     * Met à jour l'accumulateur de distance optimale pour le score.
     */
    private fun spawnNewTarget() {
        val startX = playerX
        val startY = playerY

        // Génération procédurale simple avec marge de sécurité (padding 10)
        targetX = Random.nextFloat() * 80f + 10f
        targetY = Random.nextFloat() * 80f + 10f

        // Accumulation de la distance optimale (Ligne droite)
        optimalDistanceAccumulated += hypot((targetX - startX).toDouble(), (targetY - startY).toDouble()).toFloat()
    }

    // --- SENSOR INPUT HANDLING ---

    /**
     * Réception des données brutes accéléromètre.
     * Intègre l'accélération pour mettre à jour la position virtuelle (X,Y).
     */
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER || !isGameRunning || isPausedForSuccess) return

        // Integration de l'accélération (Tilt control)
        val xTilt = event.values[0]
        val yTilt = event.values[1]
        val speed = 2.0f

        // Mise à jour coordonnées
        playerX = (playerX - xTilt * speed).coerceIn(0f, 100f)
        playerY = (playerY + yTilt * speed).coerceIn(0f, 100f)

        // Odometry (Mesure distance réelle)
        val delta = hypot((playerX - lastPlayerX).toDouble(), (playerY - lastPlayerY).toDouble()).toFloat()
        if (delta > 0.1f) {
            totalDistanceTraveled += delta
        }

        lastPlayerX = playerX
        lastPlayerY = playerY
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // --- SCORING & TEARDOWN ---

    /**
     * Arrêt contrôlé de la session.
     * Désenregistre les listeners pour éviter le battery drain.
     */
    private fun stopGame() {
        isGameRunning = false
        sensorManager.unregisterListener(this)
        mainHandler.removeCallbacks(gameLoopRunnable)
        hapticManager.cancel()
    }

    /**
     * Algorithme de score :
     * - Temps (50%) : Objectif < 90s.
     * - Efficacité (50%) : Ratio distance optimale / distance réelle.
     */
    private fun calculateAndSaveScore() {
        val totalTimeSec = (System.currentTimeMillis() - startTime) / 1000

        // Score Temps (Objectif < 90s)
        val scoreTime = ((90 - totalTimeSec) * (100.0 / 60.0)).toInt().coerceIn(0, 100)

        // Score Efficacité (Ratio Distance Idéale / Réelle)
        val ratio = if (totalDistanceTraveled > 0) optimalDistanceAccumulated / totalDistanceTraveled else 0f
        val scoreEfficiency = (ratio * 130).toInt().coerceIn(0, 100)

        val globalScore = ((scoreTime * 0.5) + (scoreEfficiency * 0.5)).toInt()

        val profilJoueur = when {
            scoreEfficiency > 85 -> "La Chauve-Souris"
            scoreTime > 90 -> "Le TGV Haptique"
            scoreEfficiency < 40 -> "Le Tâtonneur"
            else -> "L'Explorateur Sonore"
        }

        saveToFirebase(globalScore, profilJoueur, scoreEfficiency, totalTimeSec)

        val details = "⏱️ Temps : ${totalTimeSec}s\n🧭 Précision : $scoreEfficiency% (Ratio: ${(ratio*100).toInt()}%)"

        // Texte vocal simplifié
        val speechText = """
            Niveau trois terminé. Score global : $globalScore sur cent. 
            Votre profil est : $profilJoueur. 
            Vous avez terminé le parcours en $totalTimeSec secondes. 
            Votre précision de navigation est de $scoreEfficiency pour cent. 
            Votre trajectoire était ${(ratio * 100).toInt()} pour cent efficace par rapport au chemin idéal.
        """.trimIndent()

        showLevelCompleteDialog(
            score = globalScore,
            profil = profilJoueur,
            details = details,
            speechText = speechText,
            nextActivityClass = CeciliaLevel4Activity::class.java
        )
    }

    /**
     * Enregistre le score global et les métriques détaillées pour analyse.
     */
    private fun saveToFirebase(score: Int, profil: String, efficiency: Int, time: Long) {
        // Préparation des métriques spécifiques au Niveau 3
        val metrics = mapOf(
            "temps_total_sec" to time,
            "distance_reelle" to totalDistanceTraveled,
            "distance_optimale" to optimalDistanceAccumulated,
            "ratio_efficacite" to efficiency
        )

        // APPEL CENTRALISÉ : Gère la progression, Firebase et le Toast réseau
        syncManager.checkNetworkAndSave("Niveau3", score, profil, metrics)
    }

    // --- LIFECYCLE ---

    /**
     * Désenregistre les listeners avant la pause de l'activité.
     */
    override fun onPause() {
        super.onPause()
        if (isGameRunning) {
            sensorManager.unregisterListener(this)
            mainHandler.removeCallbacks(gameLoopRunnable)
            hapticManager.cancel()
        }
    }

    /**
     * Reenregistre les listeners après la reprise de l'activité.
     */
    override fun onResume() {
        super.onResume()
        if (isGameRunning && !isPausedForSuccess) {
            accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
            mainHandler.post(gameLoopRunnable)
        }
    }

    /**
     * Libère les ressources avant la fin de l'activité.
     */
    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)
    }
}