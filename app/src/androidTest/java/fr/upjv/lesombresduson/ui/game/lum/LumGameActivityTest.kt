package fr.upjv.lesombresduson.ui.game.lum

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.upjv.lesombresduson.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LumGameActivityTest {

    // Lance l'activité avant chaque test
    @get:Rule
    val activityRule = ActivityScenarioRule(LumGameActivity::class.java)

    @Test
    fun `Le bouton Retour doit etre affiche au lancement`() {
        onView(withId(R.id.button_back)).check(matches(isDisplayed()))
    }

    @Test
    fun `Cliquer sur Changer de vue doit mettre a jour le texte de la maladie`() {
        // On vérifie que le texte de base est "Vision Normale"
        onView(withId(R.id.text_disease_name)).check(matches(withText("Vision Normale")))

        // On clique sur le bouton pour changer de vue
        onView(withId(R.id.button_change_filter)).perform(click())

        // On vérifie que le texte a bien changé pour le 2ème de la liste
        onView(withId(R.id.text_disease_name)).check(matches(withText("DMLA (Tache centrale)")))
    }

    @Test
    fun `Cliquer sur Debuter la partie doit masquer les boutons de menu et afficher le bouton Analyser`() {
        // On clique sur "Débuter la partie"
        onView(withId(R.id.button_start)).perform(click())

        // On vérifie que le bouton "Analyser" et l'instruction apparaissent
        onView(withId(R.id.button_analyze)).check(matches(isDisplayed()))
        onView(withId(R.id.text_instruction)).check(matches(isDisplayed()))
    }
}