package fr.upjv.lesombresduson.data.repository

import android.content.Context
import android.content.SharedPreferences
import fr.upjv.lesombresduson.util.SettingsConstants

/**
 * Gestionnaire de stockage des préférences d'application.
 */
class SettingsRepository(context: Context) {

    /**
     * Accès aux préférences partagées de l'application.
     */
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(
        SettingsConstants.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * Retourne le volume de la voix converti pour le MediaPlayer (de 0.0f à 1.0f)
     */
    fun getVoiceVolume(): Float {
        val rawVol = sharedPrefs.getInt(SettingsConstants.KEY_VOICE_RATE, SettingsConstants.DEFAULT_VOICE_RATE)
        return rawVol / 100f
    }

    /**
     * Retourne le volume de la musique converti (de 0.0f à 1.0f)
     */
    fun getMusicVolume(): Float {
        val rawVol = sharedPrefs.getInt(SettingsConstants.KEY_MUSIC_VOLUME, SettingsConstants.DEFAULT_MUSIC_VOLUME)
        return rawVol / 100f
    }

    /**
     * Retourne le volume des effets sonores converti (de 0.0f à 1.0f)
     */
    fun getSfxVolume(): Float {
        val rawVol = sharedPrefs.getInt(SettingsConstants.KEY_SFX_VOLUME, SettingsConstants.DEFAULT_SFX_VOLUME)
        return rawVol / 100f
    }
}