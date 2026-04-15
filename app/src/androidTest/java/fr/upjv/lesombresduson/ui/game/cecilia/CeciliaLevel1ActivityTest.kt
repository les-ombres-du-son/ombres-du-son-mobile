package fr.upjv.lesombresduson.ui.game.cecilia

import android.view.MotionEvent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CeciliaLevel1ActivityTest {

    /**
     * Teste la résistance aux clics répétés (Sonar) pendant la phase d'introduction.
     */
    @Test
    fun testSonarTapsDuringIntro() {
        ActivityScenario.launch(CeciliaLevel1Activity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val downTime = android.os.SystemClock.uptimeMillis()
                val event = MotionEvent.obtain(
                    downTime, downTime,
                    MotionEvent.ACTION_DOWN, 500f, 500f, 0
                )

                // On vérifie que le spam ne provoque pas de crash
                for (i in 1..20) {
                    activity.onTouchEvent(event)
                }

                event.recycle()
            }
        }
    }

    /**
     * Teste le déclenchement de la fin du niveau.
     * Note : Correction du crash "This method can not be called from the main application thread".
     */
    @Test
    fun testFinishLevelFlow() {
        ActivityScenario.launch(CeciliaLevel1Activity::class.java).use { scenario ->
            // onActivity s'exécute déjà sur le Thread UI (Main Thread)
            scenario.onActivity { activity ->
                // On appelle directement la méthode de validation pour simuler la fin
                activity.onGestureValidated(true, "")
            }
        }
    }
}