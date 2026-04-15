package fr.upjv.lesombresduson.ui.game.cecilia.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Classe qui permet de détecter les secousses d'un téléphone.
 */
class ShakeManagerlevel4(context: Context, private val onShake: () -> Unit) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var lastAccel: Float = SensorManager.GRAVITY_EARTH
    private var accel: Float = SensorManager.GRAVITY_EARTH
    private var accelCurrent: Float = SensorManager.GRAVITY_EARTH

    private var lastShakeTime: Long = 0
    private val SHAKE_COOLDOWN_MS = 10000L // 10 secondes anti-spam
    private val SHAKE_THRESHOLD = 12f

    /**
     * Démarre l'écoute des secousses.
     */
    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    /**
     * Arrête l'écoute des secousses.
     */
    fun stop() {
        sensorManager.unregisterListener(this)
    }

    /**
     * Gère les changements d'accéléromètres.
     */
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            lastAccel = accelCurrent
            accelCurrent = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta = accelCurrent - lastAccel
            accel = accel * 0.9f + delta

            if (accel > SHAKE_THRESHOLD) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastShakeTime > SHAKE_COOLDOWN_MS) {
                    lastShakeTime = currentTime
                    onShake() // Déclenche l'action dans l'activité
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}