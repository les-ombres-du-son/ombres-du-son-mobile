package fr.upjv.lesombresduson

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import fr.upjv.lesombresduson.data.remote.FirebaseHelper
import fr.upjv.lesombresduson.ui.game.cecilia.CeciliaLevel2Activity
import fr.upjv.lesombresduson.ui.game.cecilia.CeciliaLevel3Activity
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CeciliaLevel2ActivityTest {

    private val mockFirebaseHelper = mockk<FirebaseHelper>(relaxed = true)
    private val mockUser = mockk<FirebaseUser>(relaxed = true) {
        every { uid } returns "TEST_USER_ID"
    }

    @Before
    fun setUp() {
        Intents.init()

        // Mock des singletons Firebase pour éviter les appels réseaux et simuler une session active
        mockkStatic(FirebaseHelper::class, FirebaseAuth::class)

        // Configuration du comportement : on force le retour de nos instances mockées
        every { FirebaseHelper.getInstance() } returns mockFirebaseHelper
        every { FirebaseAuth.getInstance().currentUser } returns mockUser
    }

    @After
    fun tearDown() {
        Intents.release()
        unmockkAll()
    }

    @Test
    fun testVictoirePasseAuNiveau3() {
        val scenario = ActivityScenario.launch(CeciliaLevel2Activity::class.java)

        scenario.onActivity { activity ->
            // Injection de l'état interne pour simuler une fin de niveau imminente (2 étapes sur 3)
            activity.setGameState(ready = true, feuVert = true, scoreActuel = 2)

            // Déclenchement manuel de la logique de validation pour contourner les délais du gameplay
            activity.triggerValidation()
        }

        // Vérifie que l'Activity lance bien l'intent vers le Niveau 3 après le succès (avec polling)
        waitFor(5000) {
            intended(hasComponent(CeciliaLevel3Activity::class.java.name))
        }

        // Confirmation que la progression est bien sauvegardée dans le backend mocké
        verify {
            mockFirebaseHelper.saveLevelProgression("TEST_USER_ID", "Cécilia (cécité totale)", 3)
        }

        scenario.close()
    }

    // --- Helpers de réflexion pour le White-box testing ---

    private fun CeciliaLevel2Activity.setGameState(ready: Boolean, feuVert: Boolean, scoreActuel: Int) {
        setField("isGameReady", ready)
        setField("isFeuVert", feuVert)
        setField("stepsSuccess", scoreActuel)
    }

    private fun CeciliaLevel2Activity.triggerValidation() {
        val method = this.javaClass.getDeclaredMethod("validerEtape")
        method.isAccessible = true
        method.invoke(this)
    }

    private fun Any.setField(name: String, value: Any) {
        this.javaClass.getDeclaredField(name).apply {
            isAccessible = true
            set(this@setField, value)
        }
    }

    /**
     * Mécanisme de polling pour attendre une assertion asynchrone.
     * Remplace Thread.sleep() pour une exécution plus rapide des tests.
     */
    private fun waitFor(timeoutMs: Long, block: () -> Unit) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            try { block(); return } catch (_: Throwable) { Thread.sleep(100) }
        }
        block()
    }
}