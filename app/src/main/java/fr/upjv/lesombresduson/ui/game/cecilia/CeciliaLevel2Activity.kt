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

class CeciliaLevel2Activity : BackGameActivity() {

    private lateinit var soundPool: SoundPool
    private lateinit var vibrator: Vibrator
    private lateinit var btnBack: Button

    private var introPlayer: MediaPlayer? = null
    private var isGameReady = false
    private var isLevelComplete = false

    // Audio IDs
    private var soundAmbianceId: Int = -1
    private var soundFeuRougeId: Int = -1
    private var soundFeuVertId: Int = -1
    private var soundEchecId: Int = -1     // Son en cas d'erreur
    private var soundSuccessStepId: Int = -1 // Son quand on valide une étape

    // Audio Streams
    private var streamAmbianceId: Int = -1
    private var streamFeuId: Int = -1

    // Gestion du chargement audio
    private var loadedSoundCount = 0
    private val TOTAL_SOUNDS_TO_LOAD = 5 // Ambiance, Rouge, Vert, Echec, Etape

    // Logique du jeu
    private var isFeuVert = false
    private var isFingerPressed = false // État actuel du doigt
    private var stepsSuccess = 0        // Étapes réussies (Objectif : 3)
    private val GOAL_STEPS = 3

    private val handler = Handler(Looper.getMainLooper())

    // Délai de tolérance (en millisecondes)..
    private val REACTION_TIME_MS = 500L
    private var timeRedLightStarted: Long = 0

    private var isGameLost = false // Pour savoir si le temps est dépassé mais le doigt encore là
    private val MIN_DELAY_VERT = 2000L // 2 secondes min
    private val MAX_DELAY_VERT = 8000L // 8 secondes max (très aléatoire)

    // Runnable pour surveiller si le joueur triche pendant le rouge
    private val checkRedLightRules = object : Runnable {
        override fun run() {
            if (!isGameReady || isLevelComplete || isGameLost) return

            // Si c'est ROUGE et doigt APPUYÉ
            if (!isFeuVert && isFingerPressed) {
                val timeSinceRed = System.currentTimeMillis() - timeRedLightStarted

                // Si temps de réaction dépassé (Trop lent !)
                if (timeSinceRed > REACTION_TIME_MS) {
                    // 1. On marque l'état comme PERDU
                    isGameLost = true

                    // 2. On coupe tous les sons (Ambiance et Feu) pour faire un "blanc"
                    // Cela indique au joueur qu'il a raté, avant même le message
                    soundPool.stop(streamAmbianceId)
                    soundPool.stop(streamFeuId)

                    // On arrête de vérifier
                    return
                }
            }
            handler.postDelayed(this, 50)
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
            .setMaxStreams(5)
            .setAudioAttributes(attrs)
            .build()

        // 1. Écouteur de chargement (Anti-doublon)
        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                loadedSoundCount++
                if (loadedSoundCount == TOTAL_SOUNDS_TO_LOAD) {
                    handler.post { lancerIntroVoix() }
                }
            }
        }

        // 2. Chargement des sons
        soundAmbianceId = soundPool.load(this, R.raw.ambiance_trafic_dense, 1)
        soundFeuRougeId = soundPool.load(this, R.raw.feu_sonore_rouge, 1)
        soundFeuVertId = soundPool.load(this, R.raw.feu_sonore_vert, 1)
        // Ajoutez ces sons dans votre dossier raw ou remplacez par des sons existants
        soundEchecId = soundPool.load(this, R.raw.sound_error, 1) // Son de klaxon ou erreur
        soundSuccessStepId = soundPool.load(this, R.raw.ding, 1) // Petit chime positif
    }

    private fun lancerIntroVoix() {
        // Sécurité doublon
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
        stepsSuccess = 0 // On part de 0

        // Lancer l'ambiance trafic
        streamAmbianceId = soundPool.play(soundAmbianceId, 0.8f, 0.8f, 1, -1, 1f)

        // Démarrer la surveillance des règles (Red Light check)
        handler.post(checkRedLightRules)

        // Démarrer le cycle des feux
        cycleFeuTraffic()
    }

    private fun cycleFeuTraffic() {
        if (isLevelComplete || !isGameReady || isGameLost) return

        soundPool.stop(streamFeuId)
        isFeuVert = !isFeuVert

        if (isFeuVert) {
            // --- C'EST VERT ---
            streamFeuId = soundPool.play(soundFeuVertId, 1f, 1f, 1, -1, 1f)

            // Durée beaucoup plus aléatoire (de 2s à 8s) pour casser le rythme
            val dureeVert = (MIN_DELAY_VERT..MAX_DELAY_VERT).random()

            handler.postDelayed({
                // FIN DU FEU VERT : On passe juste au rouge, on ne valide RIEN ici.
                // C'est le réflexe du joueur qui validera l'étape.
                cycleFeuTraffic()
            }, dureeVert)

        } else {
            // --- C'EST ROUGE ---
            streamFeuId = soundPool.play(soundFeuRougeId, 1f, 1f, 1, -1, 1f)

            // On lance le chrono pour le réflexe
            timeRedLightStarted = System.currentTimeMillis()

            // Durée du rouge variable
            val dureeRouge = (3000..6000).random().toLong()
            handler.postDelayed({ cycleFeuTraffic() }, dureeRouge)
        }
    }

    /**
     * Appelé quand une voie est traversée avec succès (fin du cycle vert + doigt appuyé)
     */
    private fun validerEtape() {
        stepsSuccess++

        if (stepsSuccess >= GOAL_STEPS) {
            victoire()
        } else {
            // Petit feedback sonore pour dire "Voie validée, attention rouge !"
            soundPool.play(soundSuccessStepId, 1f, 1f, 0, 0, 1f)
            Toast.makeText(this, "Voie $stepsSuccess franchie !", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Appelé si le joueur touche l'écran pendant le rouge
     */
    private fun echecCritique() {
        // Reset du jeu
        isFingerPressed = false
        stepsSuccess = 0 // Retour à la case départ

        // Feedback Échec
        vibrer(500)
        soundPool.play(soundEchecId, 1f, 1f, 1, 0, 1f)
        Toast.makeText(this, "AIE ! C'était rouge ! Retour au départ.", Toast.LENGTH_SHORT).show()

        // On relance le cycle après un petit délai de punition
        handler.removeCallbacksAndMessages(null) // Stop tout

        // On relance la surveillance mais on attend un peu pour le cycle
        handler.postDelayed({
            if (!isLevelComplete) {
                // On s'assure que le feu reparte sur du ROUGE pour laisser le temps de se calmer
                isFeuVert = true // L'appel cycleFeuTraffic va inverser ça et mettre Rouge
                handler.post(checkRedLightRules)
                cycleFeuTraffic()
            }
        }, 2000)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isLevelComplete || !isGameReady) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isFingerPressed = true
                // Si on appuie alors que c'est déjà rouge (et pas en échec latent)
                if (!isFeuVert && !isGameLost) {
                    triggerEchecImmediate() // Fonction pour échec immédiat (voir plus bas)
                }
            }
            MotionEvent.ACTION_UP -> {
                isFingerPressed = false

                // CAS 1 : Le joueur a levé le doigt car il a perdu (trop lent)
                if (isGameLost) {
                    finaliserEchec() // On joue le son d'erreur maintenant
                    return true
                }

                // CAS 2 : Le joueur lève le doigt pendant le ROUGE (et n'a pas perdu)
                // -> C'est un SUCCÈS de réflexe !
                if (!isFeuVert) {
                    validerEtape()
                }

                // CAS 3 : Le joueur lève le doigt pendant le VERT
                // -> Rien ne se passe, il arrête juste d'avancer.
            }
        }
        return true
    }

    private fun victoire() {
        isLevelComplete = true
        handler.removeCallbacksAndMessages(null) // Stop les cycles et checks

        soundPool.stop(streamAmbianceId)
        soundPool.stop(streamFeuId)

        vibrer(1000)
        Toast.makeText(this, "Traversée réussie !!!", Toast.LENGTH_LONG).show()

        handler.postDelayed({
            // Code pour passer au niveau suivant ou fermer
            finish()
        }, 4000)
    }

    // Appelé quand le joueur lève le doigt APRES avoir été trop lent
    private fun finaliserEchec() {
        vibrer(500)
        soundPool.play(soundEchecId, 1f, 1f, 1, 0, 1f)
        Toast.makeText(this, "Trop lent ! Relâchez plus vite au rouge.", Toast.LENGTH_SHORT).show()

        resetLevelState()
    }

    // Appelé si le joueur appuie ALORS que c'est déjà rouge (triche ou erreur)
    private fun triggerEchecImmediate() {
        isGameLost = true
        soundPool.stop(streamAmbianceId)
        soundPool.stop(streamFeuId)
        finaliserEchec()
    }

    private fun resetLevelState() {
        // Reset complet pour recommencer
        stepsSuccess = 0
        isGameLost = false
        isFingerPressed = false

        handler.removeCallbacksAndMessages(null)

        // Petite pause avant de relancer
        handler.postDelayed({
            if (!isLevelComplete) {
                isFeuVert = true // Pour forcer le redémarrage propre
                commencerGameplay() // Ou relancer juste le cycle
            }
        }, 2000)
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
        introPlayer?.release()
        introPlayer = null
        soundPool.release()
    }
}