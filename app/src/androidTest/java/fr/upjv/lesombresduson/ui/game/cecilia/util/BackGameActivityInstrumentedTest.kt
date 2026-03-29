package fr.upjv.lesombresduson.ui.game.cecilia.util

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.upjv.lesombresduson.R
import fr.upjv.lesombresduson.ui.StartChoiseCharacter
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackGameActivityInstrumentedTest {

    @Before
    fun setUp() {
        // Initialisation de Espresso Intents pour monitorer les changements d'activités
        Intents.init()
    }

    @After
    fun tearDown() {
        // Libération systématique pour éviter les conflits entre les sessions de test
        Intents.release()
    }

    /**
     * Test de navigation : vérifie que le bouton de retour est fonctionnel.
     * On s'assure qu'un clic sur R.id.button_back renvoie bien l'utilisateur
     * vers l'écran de sélection des personnages.
     */
    @Test
    fun buttonBack_isDisplayed_and_navigatesToStartChoiseCharacter() {
        // Utilise launch au lieu de simplement déclarer le scénario
        ActivityScenario.launch(TestBackGameActivity::class.java).use { scenario ->

            // On attend explicitement que l'état soit RESUMED avant de continuer
            scenario.moveToState(Lifecycle.State.RESUMED)

            // On laisse un tout petit temps de rendu pour l'UI si nécessaire
            Thread.sleep(500)

            // Vérifie que le bouton est bien visible
            onView(withId(R.id.button_back)).check(matches(isDisplayed()))

            // Simulation du clic
            onView(withId(R.id.button_back)).perform(click())

            // Validation de l'intent
            intended(hasComponent(StartChoiseCharacter::class.java.name))
        }
    }

    /**
     * Test de robustesse du cycle de vie.
     * On vérifie que le passage en arrière-plan (onPause/onStop) et le retour
     * au premier plan (onResume) ne provoquent aucun crash, notamment avec
     * les gestionnaires audio et haptiques.
     */
    @Test
    fun activityLifecycle_resumesProperly() {
        val scenario = ActivityScenario.launch(TestBackGameActivity::class.java)

        // Simulation d'une interruption (ex: appel entrant ou appui sur Home)
        scenario.moveToState(Lifecycle.State.CREATED)

        // Retour immédiat sur le jeu
        scenario.moveToState(Lifecycle.State.RESUMED)

        // Le test passe si aucun crash n'est détecté durant la transition
        scenario.close()
    }
}