package fr.upjv.lesombresduson.ui.game.cecilia

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.*
import android.view.MotionEvent
import android.widget.Button
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import fr.upjv.lesombresduson.R
import fr.upjv.lesombresduson.data.remote.FirebaseHelper
import fr.upjv.lesombresduson.manager.sensor.Level1SensorListener
import fr.upjv.lesombresduson.manager.sensor.Level1SensorManager
import fr.upjv.lesombresduson.ui.game.cecilia.util.BackGameActivity

/**
 * Activité gérant le premier niveau de jeu pour le personnage de Cécilia.
 * Implémente une progression sonore basée sur la détection de mouvements
 * et une interaction tactile pour l'exploration de l'environnement.
 */
class CeciliaLevel1Activity : BackGameActivity(), Level1SensorListener {

    private lateinit var btnBack: Button
    private lateinit var vibrator: Vibrator
    private lateinit var level1SensorManager: Level1SensorManager

    private var soundIntroVoice: MediaPlayer? = null
    private var isIntroFinished = false
    private var isWon = false

    private lateinit var soundPool: SoundPool
    private val progressionSoundIds = mutableListOf<Int>()
    private var carSoundId: Int = -1

    // --- VARIABLES DE SCORE & SENSIBILISATION ---
    private var interruptionCount = 0   // Impatience pendant la voix off
    private var sonarTapCount = 0       // Efficacité du "sonar" (clics écran)
    private var movementErrorCount = 0  // Erreurs de mouvement
    private var startTime: Long = 0     // Pour calculer le temps total
    // --------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gameplay_cecilia)

        btnBack = findViewById(R.id.button_back)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        level1SensorManager = Level1SensorManager(this, this)

        initAudioEngine()
        lancerVoixOff()

        setupBackButton(btnBack)
    }

    /**
     * Configure le moteur audio SoundPool pour les effets sonores à faible latence.
     */
    private fun initAudioEngine() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        val resIds = listOf(R.raw.son1, R.raw.son2, R.raw.son3, R.raw.son4, R.raw.son5)
        resIds.forEach { id -> progressionSoundIds.add(soundPool.load(this, id, 1)) }

        carSoundId = soundPool.load(this, R.raw.ambiance_carrefour, 1)
    }

    /**
     * Gère la lecture de la narration initiale et débloque le gameplay à la fin de celle-ci.
     */
    private fun lancerVoixOff() {
        soundIntroVoice = MediaPlayer.create(this, R.raw.voix_off_niveau1)
        soundIntroVoice?.setOnCompletionListener {
            isIntroFinished = true
            startTime = System.currentTimeMillis()
            level1SensorManager.startListening()
            vibrer(200)
            it.release()
            soundIntroVoice = null
        }
        soundIntroVoice?.start()
    }

    /**
     * Intercepte les interactions tactiles pour déclencher les retours sonores.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {

            // 1. Détection de l'impatience (Clic pendant l'intro)
            if (!isIntroFinished) {
                interruptionCount++
                return super.onTouchEvent(event)
            }

            // 2. Gameplay normal
            if (isWon) {
                soundPool.play(carSoundId, 1f, 1f, 1, 0, 1f)
            } else {
                // Tracking de l'utilisation du sonar
                sonarTapCount++
                jouerSonProgression()
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * Joue le son correspondant à l'étape actuelle de validation des mouvements.
     */
    private fun jouerSonProgression() {
        val currentStep = level1SensorManager.gestureCount

        if (currentStep < progressionSoundIds.size) {
            val soundId = progressionSoundIds[currentStep]
            soundPool.play(soundId, 0.7f, 0.7f, 1, 0, 1f)
        } else if (progressionSoundIds.isNotEmpty()) {
            soundPool.play(progressionSoundIds.last(), 0.7f, 0.7f, 1, 0, 1f)
        }
    }

    /**
     * Reçoit les événements de détection du sensor manager pour fournir un feedback haptique ou visuel.
     */
    override fun onFeedbackNeeded(message: String) {
        if (!isIntroFinished) return
        if (message == "VALIDATE") {
            vibrer(100)
            Toast.makeText(this, "Son identifié", Toast.LENGTH_SHORT).show()
        } else {
            // Si ce n'est pas une validation, c'est une erreur
            movementErrorCount++
        }
    }

    /**
     * Appelé lorsque la séquence complète de mouvements est validée.
     */
    override fun onGestureValidated(isGameComplete: Boolean, nextInstruction: String) {
        if (isGameComplete && isIntroFinished) {
            reussiteCarrefour()
        }
    }

    /**
     * Active l'état de victoire et modifie l'environnement sonore.
     */
    private fun reussiteCarrefour() {
        isWon = true
        level1SensorManager.stopListening()
        vibrer(500)

        // Calcul et affichage du score
        calculateAndSaveScore()
    }

    /**
     * Calcul du score de sensibilisation et affichage de la boite de dialogue
     */
    private fun calculateAndSaveScore() {
        val endTime = System.currentTimeMillis()
        val totalTimeSeconds = (endTime - startTime) / 1000

        // 1. PATIENCE (Écoute) : Basé sur les interruptions de voix off
        val scorePatience = (100 - (interruptionCount * 20)).coerceIn(0, 100)

        // 2. CALME (Efficacité Sonar)
        // Il y a 5 sons (progressionSoundIds). Un joueur parfait clique 5 à 10 fois.
        // Si le joueur clique 30 fois, il spamme de panique.
        val optimalTaps = progressionSoundIds.size * 2 // Marge de tolérance
        val sonarPenalty = if (sonarTapCount > optimalTaps) (sonarTapCount - optimalTaps) * 2 else 0
        val scoreCalme = (100 - sonarPenalty).coerceIn(0, 100)

        // 3. PRÉCISION (Orientation)
        // Basé sur les erreurs de mouvement remontées par le Manager
        val scorePrecision = (100 - (movementErrorCount * 10)).coerceIn(0, 100)

        // SCORE GLOBAL
        // Dans la rue, l'orientation (Précision) est vitale (50%), le calme ensuite (30%), la patience (20%)
        val globalScore = ((scorePrecision * 0.5) + (scoreCalme * 0.3) + (scorePatience * 0.2)).toInt()

        val profilJoueur = when {
            globalScore > 85 -> "Le Navigateur Serein"
            globalScore > 60 -> "L'Explorateur Urbain"
            else -> "Le Passant Confus"
        }

        // Sauvegarde Firebase
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            // Sauvegarde de la progression pure
            FirebaseHelper.getInstance().saveLevelProgression(user.uid, "Cécilia", 2)

            // Sauvegarde des stats détaillées
            val stats = hashMapOf<String, Any>(
                "score_global" to globalScore,
                "profil" to profilJoueur,
                "metriques" to hashMapOf<String, Any>(
                    "patience" to scorePatience,
                    "calme_sonar" to scoreCalme,
                    "precision_mouvement" to scorePrecision,
                    "temps_total_sec" to totalTimeSeconds
                ),
                "debug_info" to hashMapOf<String, Any>(
                    "clics_sonar" to sonarTapCount,
                    "erreurs_mvt" to movementErrorCount,
                    "interruptions" to interruptionCount
                )
            )
            FirebaseHelper.getInstance().saveLevelStats(user.uid, "Cécilia", "Niveau1", stats)
        }

        showEndLevelDialog(globalScore, profilJoueur, scorePrecision, scoreCalme)
    }

    private fun showEndLevelDialog(score: Int, profil: String, precision: Int, calme: Int) {
        val handler = Handler(Looper.getMainLooper())

        val goToNextLevel = {
            val intent = Intent(this, CeciliaLevel2Activity::class.java)
            startActivity(intent)
            finish()
        }

        val autoStartRunnable = Runnable {
            if (!isFinishing) goToNextLevel()
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Niveau 1 Terminé : $score/100")
            .setMessage("Profil : $profil\n\n" +
                    "🧭 Orientation : $precision% ${(if(precision<50) "⚠️ Trop d'hésitations." else "✅")}\n" +
                    "🧠 Calme : $calme% ${(if(calme<50) "⚠️ Ne spammez pas le son." else "✅")}\n\n" +
                    "⏳ Niveau 2 dans 10s...")
            .setPositiveButton("Continuer") { _, _ ->
                handler.removeCallbacks(autoStartRunnable)
                goToNextLevel()
            }
            .setCancelable(false)
            .create()

        dialog.show()
        handler.postDelayed(autoStartRunnable, 10000)
    }
    /**
     * Déclenche une vibration unique sur l'appareil.
     */
    private fun vibrer(duree: Long) {
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duree, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(duree)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isIntroFinished && !isWon) level1SensorManager.startListening()
    }

    override fun onPause() {
        super.onPause()
        level1SensorManager.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool.release()
        soundIntroVoice?.release()
    }
}