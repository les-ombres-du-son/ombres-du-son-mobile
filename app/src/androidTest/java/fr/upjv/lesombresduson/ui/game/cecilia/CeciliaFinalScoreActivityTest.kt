package fr.upjv.lesombresduson.ui.game.cecilia

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.upjv.lesombresduson.R
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CeciliaFinalScoreActivityTest {

    @Before
    fun setUp() {
        // Démarre l'espion des Intents avant chaque test
        Intents.init()
    }

    @After
    fun tearDown() {
        // Nettoie l'espion après chaque test
        Intents.release()
    }

    /**
     * Test du clique sur le lien et ouverture du navigateur
     */
    @Test
    fun testClickOnWebsiteLink_OpensBrowser() {
        // 1. On récupère le contexte du test pour lire l'URL officielle dans tes strings.xml
        val context = ApplicationProvider.getApplicationContext<Context>()
        val expectedUrl = context.getString(R.string.url_site_web)

        // 2. On lance l'activité
        ActivityScenario.launch(CeciliaFinalScoreActivity::class.java)

        // 3. On simule un clic sur le texte du lien web
        onView(withId(R.id.tv_website_link)).perform(click())

        // 4. On vérifie l'Action (Ouvrir le navigateur) ET la Data (l'URL exacte)
        intended(
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(expectedUrl)
            )
        )
    }
}