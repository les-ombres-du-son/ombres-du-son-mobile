package fr.upjv.lesombresduson.ui.game.cecilia

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import fr.upjv.lesombresduson.R
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Gestionnaire audio dédié au Niveau 2 (Cecilia).
 * Responsable du chargement des assets, du masquage auditif (volume/pitch)
 * et de la gestion des leurres sonores.
 */
class CeciliaAudioManager(private val context: Context) {

    /* -------------------------------------------------------------------------- */
    /* PROPRIÉTÉS                                                                 */
    /* -------------------------------------------------------------------------- */
    private var soundPool: SoundPool? = null

    // Identifiants des ressources sonores
    private var soundAmbianceId: Int = -1
    private var soundFeuRougeId: Int = -1
    private var soundFeuVertId: Int = -1
    private var soundEchecId: Int = -1
    private var soundSuccessStepId: Int = -1

    // Liste des identifiants pour les leurres (bruits parasites)
    private val distractorSounds = CopyOnWriteArrayList<Int>()

    // Flux audio actifs
    private var streamAmbianceId: Int = -1
    private var streamFeuId: Int = -1

    // Suivi du chargement
    private var loadedSoundCount = 0
    private val TOTAL_SOUNDS_TO_LOAD = 5
    var onAudioReady: (() -> Unit)? = null

    /* -------------------------------------------------------------------------- */
    /* INITIALISATION                                                             */
    /* -------------------------------------------------------------------------- */

    /**
     * Initialise le moteur SoundPool avec les attributs audio optimisés pour le jeu.
     * Configure le listener de chargement.
     */
    fun init() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(attrs)
            .build()

        soundPool?.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                loadedSoundCount++
                if (loadedSoundCount == TOTAL_SOUNDS_TO_LOAD) {
                    onAudioReady?.invoke()
                }
            }
        }

        loadAssets()
    }

    /**
     * Charge les fichiers bruts (Raw resources) dans la mémoire du SoundPool.
     */
    private fun loadAssets() {
        soundPool?.let { pool ->
            soundAmbianceId = pool.load(context, R.raw.ambiance_trafic_dense, 1)
            soundFeuRougeId = pool.load(context, R.raw.feu_sonore_rouge, 1)
            soundFeuVertId = pool.load(context, R.raw.feu_sonore_vert, 1)
            soundEchecId = pool.load(context, R.raw.sound_error, 1)
            soundSuccessStepId = pool.load(context, R.raw.ding, 1)
        }
    }

    /* -------------------------------------------------------------------------- */
    /* CONTRÔLES DE JEU                                                           */
    /* -------------------------------------------------------------------------- */

    /**
     * Lance la boucle d'ambiance sonore (Trafic).
     * Le volume est réglé au maximum (1.0) pour masquer les signaux.
     */
    fun playAmbiance() {
        streamAmbianceId = soundPool?.play(soundAmbianceId, 1f, 1f, 1, -1, 1f) ?: -1
    }

    /**
     * Joue le signal du feu (Vert ou Rouge).
     * Applique un volume réduit (0.6) et une variation de pitch aléatoire
     * pour augmenter la difficulté d'écoute.
     *
     * @param isVert True pour le feu vert, False pour le feu rouge.
     */
    fun playSignal(isVert: Boolean) {
        // Arrêt du son précédent pour éviter la superposition
        soundPool?.stop(streamFeuId)

        val soundId = if (isVert) soundFeuVertId else soundFeuRougeId

        // Calcul du pitch aléatoire (0.9x à 1.1x)
        val randomRate = (90..110).random() / 100f
        val volumeSignal = 0.6f

        streamFeuId = soundPool?.play(soundId, volumeSignal, volumeSignal, 1, -1, randomRate) ?: -1
    }

    /**
     * Déclenche un son parasite aléatoire (si disponible).
     * Utilise un volume faible et un pitch très variable pour la confusion.
     */
    fun playDistraction() {
        if (distractorSounds.isNotEmpty()) {
            val soundId = distractorSounds.random()
            val vol = (20..60).random() / 100f
            val rate = (80..140).random() / 100f
            soundPool?.play(soundId, vol, vol, 0, 0, rate)
        }
    }

    /**
     * Joue le son de validation d'étape (Feedback positif).
     */
    fun playSuccess() {
        soundPool?.play(soundSuccessStepId, 1f, 1f, 0, 0, 1f)
    }

    /**
     * Joue le son d'échec critique (Feedback négatif).
     */
    fun playEchec() {
        soundPool?.play(soundEchecId, 1f, 1f, 1, 0, 1f)
    }

    /**
     * Coupe immédiatement les sons d'ambiance et de signal.
     * Utilisé pour créer l'effet de "silence inquiétant" lors d'une erreur.
     */
    fun stopGameSounds() {
        soundPool?.stop(streamAmbianceId)
        soundPool?.stop(streamFeuId)
    }

    /**
     * Libère les ressources mémoire du SoundPool à la destruction de l'activité.
     */
    fun release() {
        soundPool?.release()
        soundPool = null
    }
}