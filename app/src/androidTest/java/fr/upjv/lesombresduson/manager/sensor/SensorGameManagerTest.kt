package fr.upjv.lesombresduson.manager.sensor

import android.content.Context
import android.hardware.SensorEvent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SensorGameManagerTest {

    private lateinit var manager: SensorGameManager
    private var lastFeedback: String = ""

    // On crée un faux listener pour capturer les retours du manager
    private val mockListener = object : SensorGameManager.SensorGameListener {
        override fun onInstructionReady(instruction: String) {}
        override fun onGestureValidated(isGameComplete: Boolean, nextInstruction: String) {}
        override fun onDirectionChanged(actualDirection: String) {}
        override fun onFeedbackNeeded(message: String) {
            lastFeedback = message
        }
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        manager = SensorGameManager(context, mockListener)
    }

    /**
     * Test de la détection de mouvement à droite.
     */
    @Test
    fun testRightTiltDetection() {
        // 1. On crée un faux événement accéléromètre penché à DROITE
        val event = createSensorEvent(x = -7.0f, y = 0f, z = 0f)

        // 2. On injecte l'événement dans le manager
        manager.onSensorChanged(event)

        // 3. On vérifie que le manager a détecté le mouvement et demande de maintenir
        Assert.assertTrue(
            "Le feedback devrait contenir 'détectée'",
            lastFeedback.contains("détectée")
        )
        Assert.assertTrue(
            "Le feedback devrait mentionner la droite",
            lastFeedback.contains("droite")
        )
    }

    /**
     * Test de la détection de mouvement annuler.
     */
    @Test
    fun testReleaseCancelsValidation() {
        // 1. On penche d'abord (déclenche le Handler)
        manager.onSensorChanged(createSensorEvent(-7.0f, 0f, 0f))

        // 2. On remet le téléphone à plat (X = 0)
        manager.onSensorChanged(createSensorEvent(0f, 0f, 0f))

        // 3. On vérifie si le message d'annulation est envoyé
        Assert.assertEquals("Relâchement détecté. Annulation de la validation.", lastFeedback)
    }

    /**
     * Méthode utilitaire pour créer un faux SensorEvent.
     */
    private fun createSensorEvent(x: Float, y: Float, z: Float): SensorEvent {
        // 1. On récupère la classe SensorEvent
        val sensorEventClass = SensorEvent::class.java

        // 2. On récupère le constructeur interne (il prend la taille du tableau de valeurs, ici 3 pour X,Y,Z)
        val constructor = sensorEventClass.getDeclaredConstructor(Int::class.javaPrimitiveType)
        constructor.isAccessible = true

        // 3. On crée l'instance
        val event = constructor.newInstance(3) as SensorEvent

        // 4. On injecte les valeurs de simulation (X, Y, Z) dans le champ "values"
        val valuesField = sensorEventClass.getField("values")
        valuesField.isAccessible = true
        valuesField.set(event, floatArrayOf(x, y, z))

        return event
    }
}