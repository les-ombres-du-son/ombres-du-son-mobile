package fr.upjv.lesombresduson.ui.game.cecilia.logic

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Level1SoundEngineTest {

    private lateinit var context: Context
    private lateinit var soundEngine: Level1SoundEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // On initialise avec des volumes à 1.0f pour le test
        soundEngine = Level1SoundEngine(context, 1.0f, 1.0f)
    }

    /**
     * Teste que la taille de la liste des sons correspond bien
     * au nombre de ressources préchargées (5 sons dans ton code).
     */
    @Test
    fun testGetSize() {
        assertEquals("Le moteur devrait avoir chargé 5 sons de progression", 5, soundEngine.getSize())
    }

    /**
     * Teste que les méthodes de lecture ne provoquent pas d'exception.
     */
    @Test
    fun testPlayMethodsDoNotCrash() {
        // Test de lecture normale
        soundEngine.playStepSound(0)
        // Test avec index hors limites (doit plafonner au dernier son selon ta logique)
        soundEngine.playStepSound(10)
        // Test ambiance
        soundEngine.playCarAmbience()
    }

    /**
     * Libération des ressources.
     */
    @Test
    fun testRelease() {
        soundEngine.release()
    }
}