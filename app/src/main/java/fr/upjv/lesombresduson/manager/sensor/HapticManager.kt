package fr.upjv.lesombresduson.manager.sensor

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Gestionnaire centralisé pour les retours haptiques.
 */
class HapticManager(context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /**
     * Déclenche une vibration courte pour confirmer une action.
     */
    fun vibrateSuccess() {
        vibrate(100)
    }

    /**
     * Joue un pattern plus complexe pour signaler une victoire ou une étape majeure.
     * Utilise une waveform sur Android O+, sinon un fallback simple.
     */
    fun vibrateVictory() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Timing : Pause, Vib, Pause, Vib
            val timings = longArrayOf(0, 100, 50, 200)
            val amplitudes = intArrayOf(0, 255, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            vibrator.vibrate(350)
        }
    }

    /**
     * Vibration générique avec durée personnalisée.
     * Gère la création de l'effet selon l'API level (OneShot vs Legacy).
     */
    fun vibrate(ms: Long) {
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(ms)
            }
        }
    }

    fun cancel() {
        vibrator.cancel()
    }
}