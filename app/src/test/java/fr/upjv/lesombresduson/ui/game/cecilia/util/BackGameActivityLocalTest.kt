package fr.upjv.lesombresduson.ui.game.cecilia.util

import android.content.Intent
import android.widget.Button
import fr.upjv.lesombresduson.R
import fr.upjv.lesombresduson.data.remote.RealtimeHelper
import fr.upjv.lesombresduson.ui.StartChoiseCharacter
import io.mockk.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class BackGameActivityLocalTest {

    private lateinit var controller: ActivityController<TestBackGameActivity>
    private lateinit var activity: TestBackGameActivity

    @Before
    fun setUp() {
        // Mock de RealtimeHelper pour éviter les véritables appels réseau vers Firebase pendant les tests
        mockkObject(RealtimeHelper)
        every { RealtimeHelper.init(any()) } just Runs
        every { RealtimeHelper.startSession(any()) } just Runs
        every { RealtimeHelper.updateGameStatus(any()) } just Runs
        every { RealtimeHelper.updateStep(any()) } just Runs

        // Initialisation de l'ActivityController pour avoir un contrôle précis sur le cycle de vie
        controller = Robolectric.buildActivity(TestBackGameActivity::class.java)
        activity = controller.get()

        // Lancement du cycle de vie (déclenche onCreate, onStart, onResume)
        controller.create().start().resume()
    }

    @After
    fun tearDown() {
        // Nettoyage de l'environnement MockK pour ne pas polluer les autres classes de test
        unmockkAll()
    }

    /**
     * Vérifie que le statut du jeu est correctement mis à jour via Firebase
     * lorsque l'activité passe en arrière-plan (onPause).
     */
    @Test
    fun `Quand l activite passe en onPause, RealtimeHelper est mis a jour`() {
        // Act
        controller.pause()

        // Assert
        verify { RealtimeHelper.updateGameStatus("paused") }
    }

    /**
     * Teste le comportement de la navigation UI.
     * Simule un clic sur le bouton "retour" et s'assure que l'Intent généré
     * pointe bien vers l'écran de sélection des personnages.
     */
    @Test
    fun `Clic sur le bouton retour lance StartChoiseCharacter`() {
        // Arrange
        val btnBack = activity.findViewById<Button>(R.id.button_back)

        // Act
        btnBack.performClick()

        // Assert
        val expectedIntent = Intent(activity, StartChoiseCharacter::class.java)
        val actualIntent = shadowOf(activity).nextStartedActivity

        assertEquals(expectedIntent.component, actualIntent.component)
    }
}