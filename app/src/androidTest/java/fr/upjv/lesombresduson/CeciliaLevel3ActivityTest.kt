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

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun test_onResume_active_le_capteur() {
        val scenario = ActivityScenario.launch(CeciliaLevel3Activity::class.java)

        scenario.onActivity { activity ->
            setVar(activity, "sensorManager", sensorManager)
            setVar(activity, "vibrator", vibrator)
            setVar(activity, "isGameRunning", true)
        }

        // Simule le cycle de vie pour déclencher onResume
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.moveToState(Lifecycle.State.RESUMED)

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
            setVar(activity, "isGameRunning", true)
        }

        // Passe à STARTED pour déclencher onPause
        scenario.moveToState(Lifecycle.State.STARTED)

        verify(sensorManager).unregisterListener(any(SensorEventListener::class.java))
        verify(vibrator).cancel()

        scenario.close()
    }

    private fun setVar(target: Any, name: String, value: Any) {
        try {
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