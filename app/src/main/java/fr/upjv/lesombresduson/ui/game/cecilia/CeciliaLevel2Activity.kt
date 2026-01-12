package fr.upjv.lesombresduson.ui.game.cecilia

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.*
import android.view.MotionEvent
import android.widget.Button
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import fr.upjv.lesombresduson.R
import fr.upjv.lesombresduson.data.remote.FirebaseHelper
import fr.upjv.lesombresduson.ui.game.cecilia.util.BackGameActivity

/**
 * Activité principale du Niveau 2 (Cecilia).
 * Gère la logique de gameplay : cycles de feux, détection de triche,
 * temps de réaction et transitions d'états.
 */
class CeciliaLevel2Activity : BackGameActivity() {

    /* -------------------------------------------------------------------------- */
    /* COMPOSANTS                                                                 */
    /* -------------------------------------------------------------------------- */
    private lateinit var audioManager: CeciliaAudioManager
    private lateinit var vibrator: Vibrator
    private lateinit var btnBack: Button

    private var introPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val handlerDistraction = Handler(Looper.getMainLooper())

    /* -------------------------------------------------------------------------- */
    /* ÉTAT DU JEU                                                                */
    /* -------------------------------------------------------------------------- */
    private var isGameReady = false
    private var isLevelComplete = false
    private var isGameLost = false
    private var isFeuVert = false
    private var isFingerPressed = false
    private var stepsSuccess = 0
    private var timeRedLightStarted: Long = 0

    // Constantes de difficulté
    private val GOAL_STEPS = 3
    private val REACTION_TIME_MS = 500L
    private val MIN_DELAY_VERT = 2000L
    private val MAX_DELAY_VERT = 8000L

    /* -------------------------------------------------------------------------- */
    /* RUNNABLES                                                                  */
    /* -------------------------------------------------------------------------- */

    /**
     * Runnable de surveillance : Vérifie si le joueur maintient le doigt
     * appuyé trop longtemps après le passage au feu rouge.
     */
    private val checkRedLightRules = object : Runnable {
        override fun run() {
            if (!isGameReady || isLevelComplete || isGameLost) return

            if (!isFeuVert && isFingerPressed) {
                val timeSinceRed = System.currentTimeMillis() - timeRedLightStarted

                // Si le temps de tolérance est dépassé -> Échec silencieux
                if (timeSinceRed > REACTION_TIME_MS) {
                    isGameLost = true
                    audioManager.stopGameSounds()
                    return
                }
            }
            handler.postDelayed(this, 50)
        }
    }

    /**
     * Runnable de distraction : Déclenche aléatoirement des sons parasites
     * via l'AudioManager pour perturber l'attention du joueur.
     */
    private val distractionLoop = object : Runnable {
        override fun run() {
            if (!isGameReady || isLevelComplete || isGameLost) return

            // 30% de chance de lancer un leurre
            if (Math.random() > 0.7) {
                audioManager.playDistraction()
            }
            // Prochaine tentative aléatoire (entre 0.5s et 3s)
            handlerDistraction.postDelayed(this, (500..3000).random().toLong())
        }
    }

    /* -------------------------------------------------------------------------- */
    /* CYCLE DE VIE                                                               */
    /* -------------------------------------------------------------------------- */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gameplay_cecilia)

        btnBack = findViewById(R.id.button_back)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        setupBackButton(btnBack)

        // Initialisation déléguée au Manager Audio
        audioManager = CeciliaAudioManager(this)
        audioManager.onAudioReady = {
            handler.post { lancerIntroVoix() }
        }
        audioManager.init()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopGameLoops()
        introPlayer?.release()
        audioManager.release()
    }

    /**
     * Joue la voix off d'introduction et lance le jeu à la fin de la lecture.
     */
    private fun lancerIntroVoix() {
        if (introPlayer != null) return
        introPlayer = MediaPlayer.create(this, R.raw.voix_off_niveau2)
        introPlayer?.setOnCompletionListener {
            it.release()
            introPlayer = null
            commencerGameplay()
        }
        introPlayer?.start()
    }

    /* -------------------------------------------------------------------------- */
    /* LOGIQUE DE JEU                                                             */
    /* -------------------------------------------------------------------------- */

    /**
     * Initialise les états et lance les boucles de jeu (Règles, Distractions, Feux).
     */
    private fun commencerGameplay() {
        isGameReady = true
        stepsSuccess = 0
        isGameLost = false

        audioManager.playAmbiance()

        handler.post(checkRedLightRules)
        handlerDistraction.post(distractionLoop)
        cycleFeuTraffic()
    }

    /**
     * Gère l'alternance cyclique et récursive entre le Feu Vert et le Feu Rouge.
     * Les délais sont aléatoires pour éviter l'anticipation.
     */
    private fun cycleFeuTraffic() {
        if (isLevelComplete || !isGameReady || isGameLost) return

        isFeuVert = !isFeuVert

        // Délègue le changement de son au manager
        audioManager.playSignal(isFeuVert)

        if (isFeuVert) {
            val dureeVert = (MIN_DELAY_VERT..MAX_DELAY_VERT).random()
            handler.postDelayed({ cycleFeuTraffic() }, dureeVert)
        } else {
            timeRedLightStarted = System.currentTimeMillis()
            val dureeRouge = (3000..6000).random().toLong()
            handler.postDelayed({ cycleFeuTraffic() }, dureeRouge)
        }
    }

    /* -------------------------------------------------------------------------- */
    /* GESTION DES ENTRÉES                                                        */
    /* -------------------------------------------------------------------------- */

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isLevelComplete || !isGameReady) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isFingerPressed = true
                // Si appui immédiat sur rouge (sans délai de grâce), c'est perdu
                if (!isFeuVert && !isGameLost) {
                    triggerEchecImmediate()
                }
            }
            MotionEvent.ACTION_UP -> {
                isFingerPressed = false

                // Cas 1 : Le joueur a déjà perdu (temps dépassé), on finalise
                if (isGameLost) {
                    finaliserEchec()
                    return true
                }
                // Cas 2 : Relâchement correct sur le rouge
                if (!isFeuVert) {
                    validerEtape()
                }
            }
        }
        return true
    }

    /* -------------------------------------------------------------------------- */
    /* FEEDBACK ET FIN DE JEU                                                     */
    /* -------------------------------------------------------------------------- */

    /**
     * Valide une étape réussie et vérifie si le niveau est terminé.
     */
    private fun validerEtape() {
        stepsSuccess++
        if (stepsSuccess >= GOAL_STEPS) {
            victoire()
        } else {
            audioManager.playSuccess()
            Toast.makeText(this, "Voie $stepsSuccess franchie !", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Affiche le feedback d'échec (Vibration, Son, Toast) et reset le niveau.
     */
    private fun finaliserEchec() {
        vibrer(500)
        audioManager.playEchec()
        Toast.makeText(this, "Trop lent ! Relâchez dès que le son change.", Toast.LENGTH_SHORT).show()
        resetLevelState()
    }

    /**
     * Provoque un échec immédiat (faute directe) et coupe le son.
     */
    private fun triggerEchecImmediate() {
        isGameLost = true
        audioManager.stopGameSounds()
        finaliserEchec()
    }

    /**
     * Réinitialise les variables locales et redémarre le gameplay après un délai.
     */
    private fun resetLevelState() {
        stepsSuccess = 0
        isGameLost = false
        isFingerPressed = false
        stopGameLoops()

        handler.postDelayed({
            if (!isLevelComplete) {
                isFeuVert = true // Force le reset pour repartir proprement
                commencerGameplay()
            }
        }, 2000)
    }

    /**
     * Gère la séquence de victoire du niveau.
     */
    private fun victoire() {
        isLevelComplete = true
        stopGameLoops()
        audioManager.stopGameSounds()

        vibrer(1000)
        Toast.makeText(this, "Niveau 2 terminé ! En route pour le niveau 3...", Toast.LENGTH_LONG).show()

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            FirebaseHelper.getInstance()
                .saveLevelProgression(user.uid, "Cécilia (cécité totale)", 3)
        }

        // On attend 4 secondes avant de changer d'écran
        handler.postDelayed({
            goToLevel3()
        }, 4000)
    }

    /**
    Passer au niveau 3
     */
    private fun goToLevel3() {
        // Vérifier si l'activité n'est pas déjà fermée
        if (!isFinishing) {
            val intent = Intent(this, CeciliaLevel3Activity::class.java)
            startActivity(intent)
            finish() // Ferme le niveau 1 pour libérer la mémoire
        }
    }

    /**
     * Arrête proprement tous les Runnables actifs.
     */
    private fun stopGameLoops() {
        handler.removeCallbacksAndMessages(null)
        handlerDistraction.removeCallbacksAndMessages(null)
    }

    /**
     * Helper pour déclencher une vibration haptique compatible selon l'API.
     */
    private fun vibrer(duree: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duree, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(duree)
        }
    }
}