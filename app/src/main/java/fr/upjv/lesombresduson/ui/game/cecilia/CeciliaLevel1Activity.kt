package fr.upjv.lesombresduson.ui.game.cecilia

import android.net.ConnectivityManager
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.Toast
import com.google.api.Context
import com.google.firebase.auth.FirebaseAuth
import fr.upjv.lesombresduson.R
import fr.upjv.lesombresduson.data.remote.FirebaseHelper
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

    // --- MOTEURS & LOGIQUE ---
    private lateinit var sensorManager: Level1SensorManager // Abstraction des capteurs mvt
    private lateinit var soundEngine: Level1SoundEngine     // Gestion audio faible latence

    // --- UI ---
    private lateinit var btnBack: Button

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
        setContentView(R.layout.activity_gameplay_cecilia)

        btnBack = findViewById(R.id.button_back)
        setupBackButton(btnBack) // Heritage BackGameActivity

        // Instanciation des sous-systèmes
        soundEngine = Level1SoundEngine(this, sfxVolume, musicVolume)
        sensorManager = Level1SensorManager(this, this)

        // Séquence de démarrage : Intro -> Callback -> Gameplay actif
        playIntro(R.raw.voix_off_niveau1) {
            isIntroFinished = true
            startTime = System.currentTimeMillis()

            // On active les capteurs uniquement après l'intro pour éviter le bruit
            sensorManager.startListening()
            hapticManager.vibrateSuccess()
        }
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
                return super.onTouchEvent(event)
            }

            // Cas 2 : Gameplay actif
            if (isWon) {
                soundEngine.playCarAmbience()
            } else {
                // Feedback sonore de progression (Mécanique "Sonar")
                sonarTapCount++
                soundEngine.playStepSound(sensorManager.gestureCount)
            }
        }
        return super.onTouchEvent(event)
    }

    // --- IMPLEMENTATION CAPTEURS (Level1SensorListener) ---

    override fun onFeedbackNeeded(message: String) {
        // On ignore les feedbacks capteurs si le jeu n'a pas officiellement commencé
        if (!isIntroFinished) return

        if (message == "VALIDATE") {
            hapticManager.vibrateSuccess()
            Toast.makeText(this, "Son identifié", Toast.LENGTH_SHORT).show()
        } else {
            // Mouvement incorrect détecté
            movementErrorCount++
        }
    }

    override fun onGestureValidated(isGameComplete: Boolean, nextInstruction: String) {
        // Sécurité : on ne valide rien tant que l'intro tourne
        if (isGameComplete && isIntroFinished) {
            finishLevel()
        }
    }

    // --- LOGIQUE DE FIN & SCORING ---

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

        showLevelCompleteDialog(
            score = globalScore,
            profil = profilJoueur,
            details = details,
            nextActivityClass = CeciliaLevel2Activity::class.java
        )
    }

    /**
     * Sauvergarde dans la base de données
     */
    private fun saveToFirebase(score: Int, profil: String, patience: Int, calme: Int, precision: Int, time: Long) {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        // Mise à jour progression globale
        FirebaseHelper.getInstance().saveLevelProgression(user.uid, "Cécilia (cécité totale)", 2)

        // Analytics détaillées du niveau
        val stats = hashMapOf<String, Any>(
            "score_global" to score,
            "profil" to profil,
            "metriques" to hashMapOf(
                "patience" to patience,
                "calme_sonar" to calme,
                "precision_mouvement" to precision,
                "temps_total_sec" to time
            )
        )
        FirebaseHelper.getInstance().saveLevelStats(user.uid, "Cécilia (cécité totale)", "Niveau1", stats)

        val connectivityManager = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork

        // Si activeNetwork est null, cela signifie qu'il n'y a aucune connexion (Wi-Fi ou Data)
        if (activeNetwork == null) {
            Toast.makeText(
                this,
                "Connexion perdue. Votre score est sauvegardé localement et sera synchronisé dès le retour du réseau.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // --- LIFECYCLE MANAGEMENT ---

    override fun onResume() {
        super.onResume()
        // Reprise des capteurs uniquement si le jeu est en cours
        if (isIntroFinished && !isWon) sensorManager.startListening()
    }

    override fun onPause() {
        super.onPause()
        // Pause impérative des capteurs (battery drain)
        sensorManager.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Nettoyage ressources audio natives
        soundEngine.release()
    }
}