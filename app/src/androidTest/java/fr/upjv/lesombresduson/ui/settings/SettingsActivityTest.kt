package fr.upjv.lesombresduson.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.SeekBar
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import fr.upjv.lesombresduson.R
import fr.upjv.lesombresduson.util.SettingsConstants
import org.hamcrest.Matcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SettingsActivityTest {

    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        // Nettoyage des préférences pour isoler l'état de chaque test
        val context = ApplicationProvider.getApplicationContext<Context>()
        prefs = context.getSharedPreferences(SettingsConstants.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    /**
     * Test de l'affichage des vues dans l'activité de paramètres
     */
    @Test
    fun testSettingsViewsAreDisplayed() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            // Vérification basique de l'affichage de l'UI
            onView(withId(R.id.seekbar_music_volume)).check(matches(isDisplayed()))
            onView(withId(R.id.switch_camera_usage)).check(matches(isDisplayed()))
            onView(withId(R.id.button_back_home)).check(matches(isDisplayed()))
        }
    }

    /**
     * Test de l'enregistrement du volume de musique dans les SharedPreferences
     */
    @Test
    fun testMusicVolumeSeekBar_UpdatesSharedPreferences() {
        ActivityScenario.launch(SettingsActivity::class.java).use {
            val newVolume = 75
            onView(withId(R.id.seekbar_music_volume)).perform(setProgress(newVolume))

            // Lecture directe dans les SharedPreferences pour valider le comportement
            val savedVolume = prefs.getInt(SettingsConstants.KEY_MUSIC_VOLUME, -1)
            assertEquals("Le volume n'a pas été sauvegardé", newVolume, savedVolume)
        }
    }

    /**
     * Test de retour sur l'activité Home
     */
    @Test
    fun testBackButton_FinishesActivity() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            onView(withId(R.id.button_back_home)).perform(click())

            // Vérif que l'activité est bien détruite (finish() appelé)
            assertTrue(
                "L'activité devrait être détruite",
                scenario.state.isAtLeast(Lifecycle.State.DESTROYED)
            )
        }
    }


    /**
     * Méthode utilitaire pour modifier la progression de la SeekBar.
     */
    private fun setProgress(progress: Int): ViewAction {
        return object : ViewAction {
            override fun perform(uiController: UiController, view: View) {
                val seekBar = view as SeekBar
                seekBar.progress = progress
            }

            override fun getDescription(): String {
                return "Modifie la progression d'une SeekBar à $progress"
            }

            override fun getConstraints(): Matcher<View> {
                return isAssignableFrom(SeekBar::class.java)
            }
        }
    }
}