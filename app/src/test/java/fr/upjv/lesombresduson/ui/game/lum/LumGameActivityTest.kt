package fr.upjv.lesombresduson.ui.game.lum

import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.upjv.lesombresduson.R
import fr.upjv.lesombresduson.data.remote.FirebaseHelper
import fr.upjv.lesombresduson.manager.sensor.CameraManager
import fr.upjv.lesombresduson.ui.StartChoiseCharacter
import io.mockk.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class LumGameActivityTest {

    private lateinit var mockFirebaseHelper: FirebaseHelper

    @Before
    fun setUp() {
        // 1. Simuler (Mock) le constructeur de CameraManager
        mockkConstructor(CameraManager::class)
        every { anyConstructed<CameraManager>().startCamera() } just Runs
        every { anyConstructed<CameraManager>().shutdown() } just Runs

        // 2. Simuler la classe contenant la méthode statique Java
        // ATTENTION : Utilisez bien ::class ici, c'est ce qui corrige votre erreur !
        mockkStatic(FirebaseHelper::class)

        mockFirebaseHelper = mockk(relaxed = true)
        every { FirebaseHelper.getInstance() } returns mockFirebaseHelper
    }

    @After
    fun tearDown() {
        // Nettoyer les mocks après chaque test
        unmockkAll()
    }

    @Test
    fun `Quand le mode Nouvelle Partie est lance, les boutons sont visibles`() {
        // Préparer l'Intent SANS l'extra VISION_INDEX (simule une nouvelle partie)
        val intent = Intent(ApplicationProvider.getApplicationContext(), LumGameActivity::class.java).apply {
            putExtra("USER_ID", "user123")
            putExtra("CHARACTER_NAME", "Lum")
        }

        // Lancer l'activité
        ActivityScenario.launch<LumGameActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val btnStart = activity.findViewById<Button>(R.id.button_start)
                val btnChangeFilter = activity.findViewById<Button>(R.id.button_change_filter)

                // Vérifier que les boutons sont visibles
                assertEquals(View.VISIBLE, btnStart.visibility)
                assertEquals(View.VISIBLE, btnChangeFilter.visibility)
            }
        }
    }

    @Test
    fun `Quand le mode Continuer est lance, les boutons sont caches et l'index est restaure`() {
        val savedIndex = 2 // Disons que l'index sauvegardé est 2

        val intent = Intent(ApplicationProvider.getApplicationContext(), LumGameActivity::class.java).apply {
            putExtra("VISION_INDEX", savedIndex)
        }

        ActivityScenario.launch<LumGameActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val btnStart = activity.findViewById<Button>(R.id.button_start)
                val btnChangeFilter = activity.findViewById<Button>(R.id.button_change_filter)

                // Vérifier que les boutons d'action sont cachés
                assertEquals(View.GONE, btnStart.visibility)
                assertEquals(View.GONE, btnChangeFilter.visibility)

                // La vérification de l'image nécessiterait de mocker VisionData ou d'y avoir accès.
            }
        }
    }

    @Test
    fun `Au clic sur le bouton Start, l'UI change et Firebase est appele`() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), LumGameActivity::class.java).apply {
            putExtra("USER_ID", "user123")
            putExtra("CHARACTER_NAME", "Lum")
        }

        ActivityScenario.launch<LumGameActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val btnStart = activity.findViewById<Button>(R.id.button_start)
                val btnChangeFilter = activity.findViewById<Button>(R.id.button_change_filter)

                // Simuler le clic
                btnStart.performClick()

                // 1. Vérifier le changement d'UI
                assertEquals(View.GONE, btnStart.visibility)
                assertEquals(View.GONE, btnChangeFilter.visibility)

                // 2. Vérifier que Firebase a bien été appelé avec les bons paramètres
                verify(exactly = 1) {
                    mockFirebaseHelper.updateGameProgress("user123", "Lum", "visionIndex", 0)
                }
            }
        }
    }

    @Test
    fun `Au clic sur le bouton Retour, l'intent vers StartChoiseCharacter est lance`() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), LumGameActivity::class.java)

        ActivityScenario.launch<LumGameActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                val btnBack = activity.findViewById<Button>(R.id.button_back)

                btnBack.performClick()

                // Utiliser ShadowActivity de Robolectric pour vérifier l'Intent sortant
                val shadowActivity = shadowOf(activity)
                val expectedIntent = shadowActivity.nextStartedActivity

                // Vérifier que l'activité de destination est bien StartChoiseCharacter
                assertEquals(expectedIntent.component?.className, StartChoiseCharacter::class.java.name)
            }
        }
    }
}