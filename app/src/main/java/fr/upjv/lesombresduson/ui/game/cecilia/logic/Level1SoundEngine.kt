package fr.upjv.lesombresduson.ui.game.cecilia.logic

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import fr.upjv.lesombresduson.R

/**
 * Moteur audio dédié à la logique du Niveau 1.
 * Encapsule la configuration du SoundPool pour garantir une faible latence
 * lors des retours sonores interactifs.
 */
class Level1SoundEngine(context: Context, private val sfxVolume: Float, private val ambianceVolume: Float) {

    private val soundPool: SoundPool
    private val sounds = mutableListOf<Int>()
    private var carSoundId: Int = -1

    init {
        // Optimisation pour le temps réel
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5) // Suffisant pour overlap steps + ambiance
            .setAudioAttributes(audioAttributes)
            .build()

        // Préchargement des assets pour éviter la latence à la première lecture
        val rawIds = listOf(R.raw.son1, R.raw.son2, R.raw.son3, R.raw.son4, R.raw.son5)
        rawIds.forEach { resId ->
            sounds.add(soundPool.load(context, resId, 1))
        }

        carSoundId = soundPool.load(context, R.raw.ambiance_carrefour, 1)
    }

    /**
     * Joue le feedback sonore associé à l'étape de progression.
     * Si l'index dépasse le nombre de sons, on plafonne au dernier son disponible.
     */
    fun playStepSound(stepIndex: Int) {
        val soundId = if (stepIndex < sounds.size) {
            sounds[stepIndex]
        } else if (sounds.isNotEmpty()) {
            sounds.last()
        } else {
            return
        }

        // Volume G/D à 0.7, priorité 1, pas de boucle, vitesse normale
        soundPool.play(soundId, sfxVolume, sfxVolume, 1, 0, 1f)
    }

    fun playCarAmbience() {
        if (carSoundId != -1) {
            soundPool.play(carSoundId, ambianceVolume, ambianceVolume, 1, 0, 1f)
        }
    }

    fun getSize() = sounds.size

    /**
     * Libère les ressources natives du SoundPool.
     * Impératif pour éviter les fuites de mémoire à la destruction de l'Activity.
     */
    fun release() {
        soundPool.release()
    }
}