package fr.upjv.lesombresduson.ui.game.cecilia

import android.Manifest
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.Method

@RunWith(AndroidJUnit4::class)
class CeciliaLevel5ActivityTest {

    // On accorde la permission micro automatiquement pour éviter que le test ne bloque sur la popup
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    /**
     * Teste le moteur de compréhension des choix du joueur (1, 2 ou 3).
     * Vérifie que le système gère les synonymes, les fautes de syntaxe vocale
     * et ignore le texte inutile autour du mot clé.
     */
    @Test
    fun testExtractChoiceNumber_ValidAndInvalidInputs() {
        val scenario = ActivityScenario.launch(CeciliaLevel5Activity::class.java)

        scenario.onActivity { activity ->
            // On récupère la méthode privée via la réflexion
            val method: Method = CeciliaLevel5Activity::class.java.getDeclaredMethod("extractChoiceNumber", String::class.java)
            method.isAccessible = true // On force l'accès pour le test

            // Tests pour le choix 1
            assertEquals("1", method.invoke(activity, "Je choisis la une"))
            assertEquals("1", method.invoke(activity, "le premier choix s'il te plaît"))
            assertEquals("1", method.invoke(activity, "1"))

            // Tests pour le choix 2
            assertEquals("2", method.invoke(activity, "je dirais deux"))
            assertEquals("2", method.invoke(activity, "la seconde réponse"))

            // Tests pour le choix 3
            assertEquals("3", method.invoke(activity, "c'est la trois"))
            assertEquals("3", method.invoke(activity, "troisième"))

            // Tests d'échec (le joueur dit autre chose)
            assertEquals(null, method.invoke(activity, "je ne sais pas du tout"))
            assertEquals(null, method.invoke(activity, "répète la question"))
        }
    }

    /**
     * Teste le moteur de détection des demandes d'aide ou de répétition.
     */
    @Test
    fun testIsRepeatRequest_DetectsIntentCorrectly() {
        val scenario = ActivityScenario.launch(CeciliaLevel5Activity::class.java)

        scenario.onActivity { activity ->
            // On récupère la méthode privée via la réflexion
            val method: Method = CeciliaLevel5Activity::class.java.getDeclaredMethod("isRepeatRequest", String::class.java)
            method.isAccessible = true

            // Le joueur demande à répéter
            assertTrue("Devrait détecter la demande de répétition",
                method.invoke(activity, "pouvez-vous répéter ?") as Boolean)

            assertTrue("Devrait détecter l'incompréhension",
                method.invoke(activity, "j'ai pas compris la question") as Boolean)

            // Le joueur donne une réponse normale
            assertFalse("Ne devrait pas détecter de répétition ici",
                method.invoke(activity, "je choisis le un") as Boolean)
        }
    }

    /**
     * Teste simplement le cycle de vie de l'activité pour s'assurer qu'elle
     * ne crash pas au démarrage (notamment lors de l'initialisation du TTS et du Micro).
     */
    @Test
    fun testActivityLaunch_NoCrash() {
        // Lance l'activité et la ferme proprement.
        // Si aucune exception n'est levée, le test passe.
        ActivityScenario.launch(CeciliaLevel5Activity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // On s'assure juste que l'activité n'est pas nulle
                assertTrue(activity != null)
            }
        }
    }
}