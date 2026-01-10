package fr.upjv.lesombresduson.ui.game.cecilia

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.*
import android.view.MotionEvent
import android.widget.Button
import android.widget.Toast
import fr.upjv.lesombresduson.R
import fr.upjv.lesombresduson.ui.game.cecilia.util.BackGameActivity
import java.util.concurrent.CopyOnWriteArrayList

class CeciliaLevel2Activity : BackGameActivity() {

    // UI & Hardware
    private lateinit var soundPool: SoundPool
    private lateinit var vibrator: Vibrator
    private lateinit var btnBack: Button

    // MediaPlayer pour la voix off (intro)
    private var introPlayer: MediaPlayer? = null

    // États du jeu
    private var isGameReady = false
    private var isLevelComplete = false
    private var isGameLost = false // Le joueur a perdu mais n'a pas encore relâché
    private var isFeuVert = false
    private var isFingerPressed = false

    // Progression
    private var stepsSuccess = 0
    private val GOAL_STEPS = 3

    // --- CONFIGURATION AUDIO ---
    // IDs chargés dans le SoundPool
    private var soundAmbianceId: Int = -1
    private var soundFeuRougeId: Int = -1
    private var soundFeuVertId: Int = -1
    private var soundEchecId: Int = -1
    private var soundSuccessStepId: Int = -1

    // Liste des IDs pour les bruits parasites (Leurres)
    private val distractorSounds = CopyOnWriteArrayList<Int>()

    // IDs des flux audio en cours de lecture
    private var streamAmbianceId: Int = -1
    private var streamFeuId: Int = -1

    // Chargement
    private var loadedSoundCount = 0
    private val TOTAL_SOUNDS_TO_LOAD = 5 // Ajuster si tu ajoutes des leurres

    // --- CONFIGURATION TEMPS & DIFFICULTÉ ---
    private val handler = Handler(Looper.getMainLooper())
    private val handlerDistraction = Handler(Looper.getMainLooper())

    // Tolérance réflexe humain (500ms)
    private val REACTION_TIME_MS = 500L
    private var timeRedLightStarted: Long = 0

    // Délais aléatoires pour le feu vert (Imprévisible)
    private val MIN_DELAY_VERT = 2000L
    private val MAX_DELAY_VERT = 8000L

    // --- RUNNABLES (Boucles de jeu) ---

    // 1. Surveillance du temps de réaction (Règle du Rouge)
    private val checkRedLightRules = object : Runnable {
        override fun run() {
            if (!isGameReady || isLevelComplete || isGameLost) return

            // Si c'est ROUGE et que le doigt est encore APPUYÉ
            if (!isFeuVert && isFingerPressed) {
                val timeSinceRed = System.currentTimeMillis() - timeRedLightStarted

                // Si le temps dépasse la tolérance -> PERDU (en silence)
                if (timeSinceRed > REACTION_TIME_MS) {
                    isGameLost = true

                    // On coupe les sons pour créer un "malaise" (Silence inquiétant)
                    soundPool.stop(streamAmbianceId)
                    soundPool.stop(streamFeuId)

                    // On arrête de vérifier, on attend que le joueur lève le doigt
                    return
                }
            }
            // Vérification rapide (toutes les 50ms)
            handler.postDelayed(this, 50)
        }
    }

    // 2. Générateur de Distractions (Bruits parasites)
    private val distractionLoop = object : Runnable {
        override fun run() {
            if (!isGameReady || isLevelComplete || isGameLost) return

            // 30% de chance de lancer un bruit parasite maintenant
            if (Math.random() > 0.7 && distractorSounds.isNotEmpty()) {
                val soundId = distractorSounds.random()

                // Volume variable (faible pour rester en fond, mais gênant)
                val vol = (20..60).random() / 100f
                // Pitch variable (0.8 à 1.4) pour déformer le son
                val rate = (80..140).random() / 100f

                soundPool.play(soundId, vol, vol, 0, 0, rate)
            }

            // Prochaine tentative de distraction dans 0.5s à 3s
            val nextDelay = (500..3000).random().toLong()
            handlerDistraction.postDelayed(this, nextDelay)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gameplay_cecilia)

        btnBack = findViewById(R.id.button_back)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        setupBackButton(btnBack)
        initAudio()
    }

    private fun initAudio() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10) // Augmenté pour gérer ambiance + feux + leurres
            .setAudioAttributes(attrs)
            .build()

        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                loadedSoundCount++
                // On lance le jeu quand les sons principaux sont chargés
                if (loadedSoundCount == TOTAL_SOUNDS_TO_LOAD) {
                    handler.post { lancerIntroVoix() }
                }
            }
        }

        // Chargement des sons principaux
        soundAmbianceId = soundPool.load(this, R.raw.ambiance_trafic_dense, 1)
        soundFeuRougeId = soundPool.load(this, R.raw.feu_sonore_rouge, 1)
        soundFeuVertId = soundPool.load(this, R.raw.feu_sonore_vert, 1)
        soundEchecId = soundPool.load(this, R.raw.sound_error, 1)
        soundSuccessStepId = soundPool.load(this, R.raw.ding, 1)
    }

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

    private fun commencerGameplay() {
        isGameReady = true
        stepsSuccess = 0
        isGameLost = false

        // Lancer l'ambiance trafic (Volume 100% - Très fort pour masquer le reste)
        streamAmbianceId = soundPool.play(soundAmbianceId, 1f, 1f, 1, -1, 1f)

        // Démarrer les boucles de surveillance
        handler.post(checkRedLightRules)
        handlerDistraction.post(distractionLoop)

        // Démarrer le cycle des feux
        cycleFeuTraffic()
    }

    private fun cycleFeuTraffic() {
        if (isLevelComplete || !isGameReady || isGameLost) return

        // On arrête le son du feu précédent
        soundPool.stop(streamFeuId)

        // Changement d'état
        isFeuVert = !isFeuVert

        // CONFIGURATION DIFFICULTÉ AUDITIVE
        // 1. Variation de Vitesse (Pitch) : Entre 0.9 et 1.1
        // Cela empêche le joueur de mémoriser exactement la fréquence
        val randomRate = (90..110).random() / 100f

        // 2. Volume réduit : Le signal est à 60% du volume, l'ambiance à 100%
        // Le joueur doit tendre l'oreille.
        val volumeSignal = 0.6f

        if (isFeuVert) {
            // --- C'EST VERT ---
            streamFeuId = soundPool.play(soundFeuVertId, volumeSignal, volumeSignal, 1, -1, randomRate)

            // Durée aléatoire (Longue plage pour casser le rythme)
            val dureeVert = (MIN_DELAY_VERT..MAX_DELAY_VERT).random()

            handler.postDelayed({
                // Fin du vert : on relance le cycle (passage au rouge)
                cycleFeuTraffic()
            }, dureeVert)

        } else {
            // --- C'EST ROUGE ---
            streamFeuId = soundPool.play(soundFeuRougeId, volumeSignal, volumeSignal, 1, -1, randomRate)

            // Top départ pour le chrono réflexe
            timeRedLightStarted = System.currentTimeMillis()

            // Durée du rouge
            val dureeRouge = (3000..6000).random().toLong()
            handler.postDelayed({ cycleFeuTraffic() }, dureeRouge)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isLevelComplete || !isGameReady) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isFingerPressed = true

                // Si le joueur appuie alors que c'est DÉJÀ rouge (et qu'il n'a pas déjà perdu)
                // C'est une faute directe (Triche ou inattention)
                if (!isFeuVert && !isGameLost) {
                    triggerEchecImmediate()
                }
            }
            MotionEvent.ACTION_UP -> {
                isFingerPressed = false

                // CAS 1 : C'était déjà perdu (trop lent), le joueur lève enfin le doigt
                if (isGameLost) {
                    finaliserEchec()
                    return true
                }

                // CAS 2 : Le joueur relâche pendant le ROUGE (Bon réflexe !)
                if (!isFeuVert) {
                    validerEtape()
                }

                // CAS 3 : Relâche pendant le VERT -> Rien (Pause)
            }
        }
        return true
    }

    private fun validerEtape() {
        stepsSuccess++
        if (stepsSuccess >= GOAL_STEPS) {
            victoire()
        } else {
            // Petit son de validation
            soundPool.play(soundSuccessStepId, 1f, 1f, 0, 0, 1f)
            Toast.makeText(this, "Voie $stepsSuccess franchie !", Toast.LENGTH_SHORT).show()
        }
    }

    private fun finaliserEchec() {
        // C'est ici qu'on joue le son d'erreur, au moment où le joueur réalise
        vibrer(500)
        soundPool.play(soundEchecId, 1f, 1f, 1, 0, 1f)
        Toast.makeText(this, "Trop lent ! Relâchez dès que le son change.", Toast.LENGTH_SHORT).show()
        resetLevelState()
    }

    private fun triggerEchecImmediate() {
        isGameLost = true
        // Arrêt immédiat des sons pour marquer l'erreur
        soundPool.stop(streamAmbianceId)
        soundPool.stop(streamFeuId)
        finaliserEchec()
    }

    private fun resetLevelState() {
        stepsSuccess = 0
        isGameLost = false
        isFingerPressed = false

        // Stop tout
        handler.removeCallbacksAndMessages(null)
        handlerDistraction.removeCallbacksAndMessages(null)

        // Pause de 2 secondes avant de reprendre
        handler.postDelayed({
            if (!isLevelComplete) {
                isFeuVert = true // Force le reset logique
                commencerGameplay()
            }
        }, 2000)
    }

    private fun victoire() {
        isLevelComplete = true
        handler.removeCallbacksAndMessages(null)
        handlerDistraction.removeCallbacksAndMessages(null)

        soundPool.stop(streamAmbianceId)
        soundPool.stop(streamFeuId)
        soundPool.autoPause()

        vibrer(1000)
        Toast.makeText(this, "Niveau Terminé !!!", Toast.LENGTH_LONG).show()

        handler.postDelayed({ finish() }, 4000)
    }

    private fun vibrer(duree: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duree, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(duree)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        handlerDistraction.removeCallbacksAndMessages(null)
        introPlayer?.release()
        introPlayer = null
        soundPool.release()
    }
}