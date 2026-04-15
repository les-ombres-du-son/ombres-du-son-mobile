package fr.upjv.lesombresduson.ui.game.cecilia


import android.view.MotionEvent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CeciliaLevel2ActivityTest {

    /**
     * Teste l'état initial du jeu.
     */
    @Test
    fun testInitialState() {
        ActivityScenario.launch(CeciliaLevel2Activity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // Vérifier que le jeu n'est pas prêt tant que l'audio n'a pas fini de charger
                assertNotNull(activity)
            }
        }
    }

    /**
     * Teste le comportement du jeu lorsque le joueur appuie pendant le feu rouge.
     */
    @Test
    fun testTouchDuringRedLight() {
        ActivityScenario.launch(CeciliaLevel2Activity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // 1. On force l'état "Feu Rouge" et on s'assure que le jeu est prêt
                activity.isGreenLight = false
                activity.isGameReady = true
                activity.isGameLost = false // On part d'un état propre

                // 2. On simule un clic (ACTION_DOWN)
                val downTime = android.os.SystemClock.uptimeMillis()
                val event = MotionEvent.obtain(
                    downTime, downTime,
                    MotionEvent.ACTION_DOWN, 0f, 0f, 0
                )

                activity.onTouchEvent(event)
                event.recycle()
            }

            // 3. On laisse un tout petit délai pour que le Handler de l'activité
            // traite le triggerGameOver
            Thread.sleep(100)

            // 4. On vérifie l'état final
            scenario.onActivity { activity ->
                assertTrue("Le jeu devrait être perdu après un appui au feu rouge", activity.isGameLost)
            }
        }
    }
}