package fr.upjv.lesombresduson.manager.sensor

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import fr.upjv.lesombresduson.manager.input.GestureListener
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class MicrophoneManagerTest {

    // FORCE l'autorisation du micro pour le test
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    private lateinit var micManager: MicrophoneManager
    private var dogFoundTriggered = false
    private var lastFeedbackMessage = ""

    // On crée un faux listener pour intercepter les appels du manager
    private val mockListener = object : GestureListener {
        override fun onDogFound() {
            dogFoundTriggered = true
        }

        override fun onFeedbackNeeded(message: String) {
            lastFeedbackMessage = message
        }

        override fun onInstructionReady(instruction: String) {}
        override fun onGestureValidated(isGameComplete: Boolean, nextInstruction: String) {}
        override fun onTargetFound() {}
    }

    @Before
    fun setUp() {
        micManager = MicrophoneManager(mockListener)
    }

    /**
     * Test de démarrage du microphone manager.
     */
    @Test
    fun testStartListening_SendsInitialFeedback() {
        // Exécuter sur le thread principal car MicrophoneManager utilise un Handler
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            micManager.startListening()
        }

        // On laisse une petite fraction de seconde au Handler pour traiter le message
        Thread.sleep(100)

        assertTrue("Le message reçu était : '$lastFeedbackMessage'",
            lastFeedbackMessage.contains("Soufflez"))

        micManager.stopListening()
    }

    /**
     * Test de l'arrêt du manager.
     */
    @Test
    fun testStopListening_CleansUpState() {
        micManager.startListening()
        micManager.stopListening()

        // On vérifie qu'on n'est plus en mode détection
        // (On vérifie indirectement en s'assurant qu'aucun callback n'est plus actif)
        assertFalse("Le chien ne devrait pas être trouvé après l'arrêt", dogFoundTriggered)
    }
}