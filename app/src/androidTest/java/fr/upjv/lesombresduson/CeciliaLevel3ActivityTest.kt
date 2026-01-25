package fr.upjv.lesombresduson

import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Vibrator
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.upjv.lesombresduson.ui.game.cecilia.CeciliaLevel3Activity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.*
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class CeciliaLevel3ActivityTest {

    @Mock lateinit var sensorManager: SensorManager
    @Mock lateinit var vibrator: Vibrator
    // ❌ ON SUPPRIME le Handler mocké (c'est lui qui faisait planter)

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun test_onResume_active_le_capteur() {
        val scenario = ActivityScenario.launch(CeciliaLevel3Activity::class.java)

        scenario.onActivity { activity ->
            // On injecte seulement les composants externes (Capteurs, Vibreur)
            setVar(activity, "sensorManager", sensorManager)
            setVar(activity, "vibrator", vibrator)
            // ❌ On ne touche PAS au Handler, on laisse le vrai faire son travail

            setVar(activity, "isGameRunning", true)
        }

        // Cycle Pause -> Resume
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.moveToState(Lifecycle.State.RESUMED)

        // Vérification
        verify(sensorManager).registerListener(
            any(SensorEventListener::class.java),
            any(Sensor::class.java),
            anyInt()
        )

        scenario.close()
    }

    @Test
    fun test_onPause_coupe_tout() {
        val scenario = ActivityScenario.launch(CeciliaLevel3Activity::class.java)

        scenario.onActivity { activity ->
            setVar(activity, "sensorManager", sensorManager)
            setVar(activity, "vibrator", vibrator)
            // ❌ Pas d'injection de Handler ici non plus

            setVar(activity, "isGameRunning", true)
        }

        // Action : On passe l'état à STARTED (ce qui déclenche onPause)
        scenario.moveToState(Lifecycle.State.STARTED)

        // Vérification
        verify(sensorManager).unregisterListener(any(SensorEventListener::class.java)) // Correction du type ici aussi par sécurité
        verify(vibrator).cancel()

        scenario.close()
    }

    private fun setVar(target: Any, name: String, value: Any) {
        try {
            // On cherche le champ dans la classe ou ses parents
            var clazz: Class<*>? = target.javaClass
            while (clazz != null) {
                try {
                    val field = clazz.getDeclaredField(name)
                    field.isAccessible = true
                    field.set(target, value)
                    return
                } catch (e: NoSuchFieldException) {
                    clazz = clazz.superclass
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("Impossible d'injecter '$name': ${e.message}")
        }
    }
}