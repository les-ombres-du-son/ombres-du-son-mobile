package fr.upjv.lesombresduson.ui

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import fr.upjv.lesombresduson.R
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class StartChoiseCharacterTest {

    /**
     * Crée un Intent spécial pour le test qui contourne la sécurité Firebase.
     */
    private fun createTestIntent(): Intent {
        val intent = Intent(ApplicationProvider.getApplicationContext(), StartChoiseCharacter::class.java)
        // On passe le flag pour prévenir l'activité qu'il s'agit d'un test automatisé
        intent.putExtra("IS_TEST_MODE", true)
        return intent
    }

    /**
     * Test de l'état initial de l'activité.
     */
    @Test
    fun testInitialState_ShowsSelectionGroupOnly() {
        ActivityScenario.launch<StartChoiseCharacter>(createTestIntent()).use {
            onView(withId(R.id.character_selection_group)).check(matches(isDisplayed()))
            onView(withId(R.id.character_confirmed_group)).check(matches(not(isDisplayed())))
        }
    }

    /**
     * Test du clic sur la carte de Cécilia.
     */
    @Test
    fun testClickOnCecilia_ShowsConfirmationGroup() {
        ActivityScenario.launch<StartChoiseCharacter>(createTestIntent()).use {
            onView(withId(R.id.layout_cecilia)).perform(click())

            onView(withId(R.id.character_confirmed_group)).check(matches(isDisplayed()))
            onView(withId(R.id.text_center_name)).check(matches(withText("Cécilia (cécité totale)")))
            onView(withId(R.id.character_selection_group)).check(matches(not(isDisplayed())))
        }
    }

    /**
     * Test du clic sur la carte de Lum.
     */
    @Test
    fun testBackButton_FromConfirmation_CancelsSelection() {
        ActivityScenario.launch<StartChoiseCharacter>(createTestIntent()).use {
            onView(withId(R.id.layout_lum)).perform(click())
            onView(withId(R.id.character_confirmed_group)).check(matches(isDisplayed()))

            onView(withId(R.id.button_back)).perform(click())

            onView(withId(R.id.character_selection_group)).check(matches(isDisplayed()))
            onView(withId(R.id.character_confirmed_group)).check(matches(not(isDisplayed())))
        }
    }
}