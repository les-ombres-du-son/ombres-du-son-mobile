package fr.upjv.lesombresduson.ui.game.cecilia

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CeciliaIntroActivityTest {

    /**
     * Test de l'affichage de la popup de volume lorsque le volume est à 0.
     */
    @Test
    fun testVolumePopupAppearsWhenVolumeIsZero() {
        ActivityScenario.launch(CeciliaIntroActivity::class.java).use {
            // Si le volume est à 0, la popup doit être visible
            // On vérifie par le texte du titre de la popup
            onView(withText("Son requis 🔊")).check(matches(isDisplayed()))
        }
    }
}