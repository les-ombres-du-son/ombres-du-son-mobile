package fr.upjv.lesombresduson.ui.game.cecilia

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Button
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import fr.upjv.lesombresduson.R
import fr.upjv.lesombresduson.data.remote.FirebaseHelper
import fr.upjv.lesombresduson.ui.game.cecilia.util.BackGameActivity
import kotlin.math.hypot
import kotlin.random.Random

class CeciliaLevel3Activity : BackGameActivity(), SensorEventListener {

    private lateinit var btnBack: Button

    // Audio
    private var mediaPlayer: MediaPlayer? = null

    // Capteurs & Vibration
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var vibrator: Vibrator

    // Variables de jeu
    private var isGameRunning = false
    private val handler = Handler(Looper.getMainLooper())

    private var playerX = 50f
    private var playerY = 50f

    // Gestion de la progression (5 étapes)
    private var targetsFound = 0
    private val totalTargets = 5

    private var targetX = 0f
    private var targetY = 0f

    // Difficulté et validation
    private val winThreshold = 15f
    private val maxDistance = 100f

    // Flag pour bloquer le jeu pendant l'animation de réussite
    private var isPausedForSuccess = false

    // --- VARIABLES SCORE & STATISTIQUES ---
    private var startTime: Long = 0
    private var totalDistanceTraveled: Float = 0f // Distance réelle parcourue par le joueur
    private var optimalDistanceAccumulated: Float = 0f // Distance minimale théorique (ligne droite)
    private var lastPlayerX = 50f
    private var lastPlayerY = 50f
    // --------------------------------------

    /**
     * Ma boucle de jeu principale.
     * Elle tourne en continu (toutes les 50ms) pour vérifier la position du joueur
     * et mettre à jour les vibrations tant que le jeu n'est pas en pause.
     */
    private val gameRunnable = object : Runnable {
        override fun run() {
            if (isGameRunning && !isPausedForSuccess) {
                updateGameLogic()
                handler.postDelayed(this, 50)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gameplay_cecilia)

        btnBack = findViewById(R.id.button_back)
        setupBackButton(btnBack)

        initSensors()

        // Je lance l'audio d'intro dès le début de l'activité
        playIntroAudio()
    }

    /**
     * J'initialise ici le gestionnaire de capteurs (accéléromètre)
     * et je récupère le service de vibration en gérant la compatibilité des versions Android.
     */
    private fun initSensors() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibrator = vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /**
     * Je configure et lance le fichier audio d'introduction.
     * Une fois la lecture terminée (OnCompletionListener), je lance automatiquement le jeu.
     */
    private fun playIntroAudio() {
        mediaPlayer = MediaPlayer.create(this, R.raw.voix_off_niveau3)

        mediaPlayer?.setOnCompletionListener {
            startGame()
        }

        // Sécurité : si le fichier audio ne charge pas, je lance quand même le jeu
        if (mediaPlayer == null) {
            startGame()
        } else {
            mediaPlayer?.start()
        }
    }

    /**
     * Cette méthode démarre officiellement la partie.
     * Je réinitialise le compteur, je crée la première cible et j'active l'écoute de l'accéléromètre.
     */
    private fun startGame() {
        isGameRunning = true
        targetsFound = 0

        // Reset Stats
        totalDistanceTraveled = 0f
        optimalDistanceAccumulated = 0f
        startTime = System.currentTimeMillis()

        spawnNewTarget()

        Toast.makeText(this, "Trouvez les 5 zones de vibration !", Toast.LENGTH_SHORT).show()

        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        handler.post(gameRunnable)
    }

    /**
     * Je génère des coordonnées aléatoires (X et Y) pour placer la prochaine zone à trouver.
     * Je garde une marge (10 à 90) pour ne pas être collé aux bords.
     */
    private fun spawnNewTarget() {
        // Sauvegarde de la position actuelle avant de changer la cible
        val startStepX = playerX
        val startStepY = playerY

        targetX = Random.nextFloat() * 80f + 10f
        targetY = Random.nextFloat() * 80f + 10f

        // Calcul de la distance "parfaite" (ligne droite) pour ce segment
        val distanceToNext = hypot((targetX - startStepX).toDouble(), (targetY - startStepY).toDouble()).toFloat()
        optimalDistanceAccumulated += distanceToNext
    }

    /**
     * Arrête proprement le jeu, désactive les capteurs et le vibreur.
     * Si le joueur a gagné, j'affiche un message et je ferme l'écran.
     */
    private fun stopGame(success: Boolean) {
        isGameRunning = false
        sensorManager.unregisterListener(this)
        handler.removeCallbacks(gameRunnable)
        vibrator.cancel()

        if (success) {
            calculateAndSaveScore()
        }
    }

    /**
    Passer au niveau 4
     */
    private fun goToLevel4() {
        // Vérifier si l'activité n'est pas déjà fermée
        if (!isFinishing) {
            val intent = Intent(this, CeciliaLevel4Activity::class.java)
            startActivity(intent)
            finish() // Ferme le niveau pour libérer la mémoire
        }
    }

    /**
     * Cœur logique du jeu appelé en boucle.
     * 1. Je calcule la distance joueur-cible.
     * 2. Si proche -> Victoire de l'étape.
     * 3. Sinon -> Je calcule l'intensité de vibration selon la distance et le type de sol.
     */
    private fun updateGameLogic() {
        val distance = hypot((targetX - playerX).toDouble(), (targetY - playerY).toDouble()).toFloat()

        if (distance < winThreshold) {
            handleTargetFound()
            return
        }

        // Calcul de l'intensité (exponentiel pour un meilleur ressenti en fin de course)
        val rawProgress = (1f - (distance / maxDistance)).coerceIn(0f, 1f)
        val progress = rawProgress * rawProgress
        val amplitude = (progress * 255).toInt().coerceIn(10, 255)

        // Détermination du type de sol selon la position Y du joueur
        val terrainType = when {
            playerY < 33 -> "CONCRETE"
            playerY < 66 -> "GRASS"
            else -> "PATH"
        }

        triggerVibration(terrainType, amplitude)
    }

    /**
     * Gère la validation d'un point trouvé.
     * Je mets le jeu en pause, je lance la vibration de succès, et je programme l'apparition du point suivant.
     */
    private fun handleTargetFound() {
        isPausedForSuccess = true
        targetsFound++

        vibrateStepSuccess()

        if (targetsFound >= totalTargets) {
            stopGame(true)
        } else {
            Toast.makeText(this, "Trouvé ! ${targetsFound}/$totalTargets", Toast.LENGTH_SHORT).show()

            // Délai de 1.5s pour laisser finir la vibration avant de reprendre
            handler.postDelayed({
                spawnNewTarget()
                isPausedForSuccess = false
                handler.post(gameRunnable)
            }, 1500)
        }
    }

    /**
     * Calcule le score final basé sur le Temps et la Précision de navigation.
     */
    private fun calculateAndSaveScore() {
        val totalTimeMs = System.currentTimeMillis() - startTime
        val totalTimeSeconds = totalTimeMs / 1000

        // 1. SCORE DE TEMPS (Objectif : 45 secondes pour 5 cibles)
        val scoreTime = ((90 - totalTimeSeconds) * (100.0 / 60.0)).toInt().coerceIn(0, 100)

        // 2. SCORE D'EFFICACITÉ (Ratio Distance Idéale / Distance Réelle)
        // Si ratio = 1.0 (ligne parfaite) -> 100%. Si ratio < 0.3 (beaucoup de détours) -> faible.
        // On évite la division par zéro
        val ratio = if (totalDistanceTraveled > 0) optimalDistanceAccumulated / totalDistanceTraveled else 0f
        // On booste un peu le ratio car être parfaitement droit à l'aveugle est impossible
        val scoreEfficiency = (ratio * 130).toInt().coerceIn(0, 100)

        // SCORE GLOBAL (50% Temps, 50% Précision)
        val globalScore = ((scoreTime * 0.5) + (scoreEfficiency * 0.5)).toInt()

        // Détermination du profil
        val profilJoueur = when {
            scoreEfficiency > 85 -> "La Chauve-Souris" // Très précis
            scoreTime > 90 -> "Le TGV Haptique" // Très rapide mais peut-être brouillon
            scoreEfficiency < 40 -> "Le Tâtonneur" // Beaucoup de détours
            else -> "L'Explorateur Sonore"
        }

        // Sauvegarde Firebase
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            // Débloque niveau 4
            FirebaseHelper.getInstance().saveLevelProgression(user.uid, "Cécilia", 4)

            val stats = hashMapOf<String, Any>(
                "score_global" to globalScore,
                "profil" to profilJoueur,
                "metriques" to hashMapOf<String, Any>(
                    "temps_total_sec" to totalTimeSeconds,
                    "distance_reelle" to totalDistanceTraveled,
                    "distance_optimale" to optimalDistanceAccumulated,
                    "ratio_efficacite" to ratio
                )
            )
            FirebaseHelper.getInstance().saveLevelStats(user.uid, "Cécilia", "Niveau3", stats)
        }

        showEndLevelDialog(globalScore, profilJoueur, scoreEfficiency, totalTimeSeconds)
    }

    private fun showEndLevelDialog(score: Int, profil: String, scorePrecision: Int, timeSec: Long) {
        val handler = Handler(Looper.getMainLooper())

        // Runnable pour passer automatiquement après délai si pas de clic
        val autoStartRunnable = Runnable {
            if (!isFinishing) goToLevel4()
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Niveau 3 Terminé : $score/100")
            .setMessage("Profil : $profil\n\n" +
                    "⏱️ Temps : ${timeSec}s\n" +
                    "🧭 Précision : $scorePrecision% ${if(scorePrecision > 70) "🔥" else "〰️"}\n" +
                    "(Ratio chemin : ${(optimalDistanceAccumulated/totalDistanceTraveled * 100).toInt()}%)\n\n" +
                    "⏳ Niveau 4 dans 10s...")
            .setPositiveButton("Continuer") { _, _ ->
                handler.removeCallbacks(autoStartRunnable)
                goToLevel4()
            }
            .setCancelable(false)
            .create()

        dialog.show()
        handler.postDelayed(autoStartRunnable, 10000) // Auto-skip après 10s
    }

    /**
     * Déclenche une vibration spécifique selon le terrain (Béton, Herbe, Chemin).
     * L'amplitude varie selon la distance calculée précédemment.
     */
    private fun triggerVibration(terrain: String, amplitude: Int) {
        if (isPausedForSuccess) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val duration = 60L
            val effect = when (terrain) {
                "CONCRETE" -> VibrationEffect.createOneShot(duration, amplitude)
                "GRASS" -> {
                    // Pour l'herbe, je réduis un peu l'intensité pour faire "mou"
                    val softAmp = (amplitude * 0.6).toInt().coerceAtLeast(1)
                    VibrationEffect.createOneShot(duration, softAmp)
                }
                "PATH" -> {
                    // Pour le chemin, je vibre une fois sur deux pour simuler des cailloux
                    if (System.currentTimeMillis() % 200 < 100) {
                        VibrationEffect.createOneShot(duration, amplitude)
                    } else null
                }
                else -> VibrationEffect.createOneShot(duration, amplitude)
            }
            effect?.let { vibrator.vibrate(it) }
        } else {
            vibrator.vibrate(60)
        }
    }

    /**
     * Joue un motif de vibration distinct pour confirmer que le joueur a trouvé la zone.
     */
    private fun vibrateStepSuccess() {
        vibrator.cancel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 100, 100, 100, 100, 400)
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)

            try {
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            } catch (e: Exception) {
                val simpleTimings = longArrayOf(0, 100, 100, 100, 100, 400)
                vibrator.vibrate(VibrationEffect.createWaveform(simpleTimings, -1))
            }
        } else {
            vibrator.vibrate(500)
        }
    }

    /**
     * Récupère les données de l'accéléromètre pour déplacer le joueur (X et Y).
     * Je convertis l'inclinaison physique en coordonnées sur la carte virtuelle.
     */
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER && isGameRunning && !isPausedForSuccess) {
            val xTilt = event.values[0]
            val yTilt = event.values[1]
            val speed = 2.0f

            // Mise à jour position
            playerX -= xTilt * speed
            playerY += yTilt * speed

            // Bornes (0-100)
            playerX = playerX.coerceIn(0f, 100f)
            playerY = playerY.coerceIn(0f, 100f)

            // --- TRACKING DISTANCE POUR LE SCORE ---
            // On calcule la distance parcourue depuis la dernière frame
            val deltaDistance = hypot((playerX - lastPlayerX).toDouble(), (playerY - lastPlayerY).toDouble()).toFloat()

            // On filtre les micro-tremblements (bruit du capteur)
            if (deltaDistance > 0.1f) {
                totalDistanceTraveled += deltaDistance
            }

            // Mise à jour de la dernière position connue
            lastPlayerX = playerX
            lastPlayerY = playerY
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /**
     * Gestion du cycle de vie : je mets en pause l'audio et les capteurs si l'appli passe en arrière-plan.
     */
    override fun onPause() {
        super.onPause()
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
        if (isGameRunning) {
            sensorManager.unregisterListener(this)
            handler.removeCallbacks(gameRunnable)
            vibrator.cancel()
        }
    }

    /**
     * Si l'utilisateur revient sur l'appli, je relance les capteurs et la boucle de jeu.
     */
    override fun onResume() {
        super.onResume()
        if (isGameRunning) {
            accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
            handler.post(gameRunnable)
        }
    }

    /**
     * Nettoyage final : je libère le lecteur audio et le handler pour éviter les fuites de mémoire.
     */
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        handler.removeCallbacksAndMessages(null)
    }
}