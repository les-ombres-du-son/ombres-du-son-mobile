package fr.upjv.lesombresduson.ui

import androidx.test.core.app.ActivityScenario
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
     * Test de l'état initial de l'activité.
     */
    @Test
    fun testInitialState_ShowsSelectionGroupOnly() {
        // Lance l'activité
        ActivityScenario.launch(StartChoiseCharacter::class.java).use {
            // L'écran de sélection doit être visible
            onView(withId(R.id.character_selection_group)).check(matches(isDisplayed()))

            // L'écran de confirmation doit être caché
            onView(withId(R.id.character_confirmed_group)).check(matches(not(isDisplayed())))
        }
    }

    /**
     * Test du clic sur la carte de Cécilia.
     */
    @Test
    fun testClickOnCecilia_ShowsConfirmationGroup() {
        ActivityScenario.launch(StartChoiseCharacter::class.java).use {
            // On simule un clic sur la carte de Cécilia
            onView(withId(R.id.layout_cecilia)).perform(click())

            // Le groupe de confirmation doit apparaître
            onView(withId(R.id.character_confirmed_group)).check(matches(isDisplayed()))

            // Le texte doit avoir changé pour afficher le nom de Cécilia
            onView(withId(R.id.text_center_name)).check(matches(withText("Cécilia (cécité totale)")))

            // Le groupe de sélection initiale doit avoir disparu
            onView(withId(R.id.character_selection_group)).check(matches(not(isDisplayed())))
        }
    }

    /**
     * Test du clic sur la carte de Lum.
     */
    @Test
    fun testBackButton_FromConfirmation_CancelsSelection() {
        ActivityScenario.launch(StartChoiseCharacter::class.java).use {
            // 1. On va sur l'écran de confirmation
            onView(withId(R.id.layout_lum)).perform(click())
            onView(withId(R.id.character_confirmed_group)).check(matches(isDisplayed()))

            // 2. On clique sur le bouton retour
            onView(withId(R.id.button_back)).perform(click())

            // 3. On vérifie qu'on est bien revenu en arrière (sélection visible, confirmation cachée)
            onView(withId(R.id.character_selection_group)).check(matches(isDisplayed()))
            onView(withId(R.id.character_confirmed_group)).check(matches(not(isDisplayed())))
        }
    }
}