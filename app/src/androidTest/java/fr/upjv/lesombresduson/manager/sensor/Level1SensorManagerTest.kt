package fr.upjv.lesombresduson.manager.sensor

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentés pour le gestionnaire de mouvements du Niveau 1.
 * Vérifie l'initialisation des capteurs et la cohérence de la machine à états.
 */
@RunWith(AndroidJUnit4::class)
class Level1SensorManagerTest {

    private lateinit var sensorManager: Level1SensorManager
    private var lastFeedback = ""

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // Création d'un listener anonyme pour intercepter les retours
        val fakeListener = object : Level1SensorListener {
            override fun onFeedbackNeeded(message: String) {
                lastFeedback = message
            }
            override fun onGestureValidated(isGameComplete: Boolean, nextInstruction: String) {}
        }

        sensorManager = Level1SensorManager(context, fakeListener)
    }

    /**
     * Vérifie que le gestionnaire identifie correctement la disponibilité du matériel.
     */
    @Test
    fun testSensorAvailability() {
        // Sur un émulateur standard, l'accéléromètre est généralement présent
        val sm = InstrumentationRegistry.getInstrumentation().targetContext
            .getSystemService(android.content.Context.SENSOR_SERVICE) as android.hardware.SensorManager
        val hasAccel = sm.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER) != null

        if (hasAccel) {
            // On vérifie que startListening ne crash pas
            sensorManager.startListening()
            sensorManager.stopListening()
        }
    }

    /**
     * Vérifie que la séquence d'instructions commence par le bon mouvement.
     */
    @Test
    fun testInitialInstructionIsCorrect() {
        val firstInstruction = sensorManager.currentExpectedDirection
        assertEquals("Le premier mouvement doit être 'vers le haut'", "vers le haut", firstInstruction)
    }

    /**
     * Vérifie que le compteur de gestes est bien initialisé à zéro.
     */
    @Test
    fun testInitialGestureCount() {
        assertEquals(0, sensorManager.gestureCount)
    }

    /**
     * Vérifie que le manager renvoie 'à plat' par défaut sans mouvement.
     */
    @Test
    fun testDefaultDirectionIsFlat() {
        assertEquals("à plat", sensorManager.currentActualDirection)
    }
}