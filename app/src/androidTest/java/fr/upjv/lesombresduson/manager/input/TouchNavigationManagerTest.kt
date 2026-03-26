package fr.upjv.lesombresduson.manager.input

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TouchNavigationManagerTest {

    private lateinit var context: Context
    private lateinit var touchManager: TouchNavigationManager
    private lateinit var dummyView: View
    private var isTargetFoundCalled = false
    private var feedbackReceived = ""

    /**
     * Configuration intial avant chaque test
     */
    @Before
    fun setup() {
        // 1. Récupération d'un vrai contexte Android grâce à AndroidX Test
        context = ApplicationProvider.getApplicationContext()
        dummyView = View(context)

        // 2. Création d'un faux listener (Mock) pour espionner le manager
        val fakeListener = object : GestureListener {
            override fun onInstructionReady(instruction: String) {}
            override fun onGestureValidated(isGameComplete: Boolean, nextInstruction: String) {}
            override fun onTargetFound() {
                isTargetFoundCalled = true
            }
            override fun onDogFound() {}
            override fun onFeedbackNeeded(message: String) {
                feedbackReceived = message // On capture le message envoyé par le manager
            }
        }

        // 3. Initialisation du manager avec un faux écran de 1000x1000 pixels
        touchManager = TouchNavigationManager(context, fakeListener, 1000, 1000)
    }

    /**
     * Test la prise en compte du début d'une interaction tactile
     */
    @Test
    fun testActionDownIsConsumedSuccessfully() {
        // ÉTAPE 1 : Création d'un faux événement tactile (Doigt qui se pose)
        val downTime = SystemClock.uptimeMillis()
        val eventDown = MotionEvent.obtain(
            downTime, downTime,
            MotionEvent.ACTION_DOWN,
            500f, 500f, 0 // Position (500, 500) au centre
        )

        // ÉTAPE 2 : On simule le toucher
        val result = touchManager.onTouch(dummyView, eventDown)

        // ÉTAPE 3 : Vérification (la méthode doit retourner true)
        assertTrue("L'événement ACTION_DOWN devrait être consommé et retourner true", result)

        // Nettoyage de l'événement
        eventDown.recycle()
    }

    /**
     * Test la logique de validation spatiale lors du relâchement du doigts
     */
    @Test
    fun testActionUpFarFromTargetTriggersFeedback() {
        // Comme targetX et targetY ont une marge de 10% (donc entre 100 et 900),
        // si on clique à la coordonnée (0, 0), on est 100% sûr d'être loin de la cible !

        val downTime = SystemClock.uptimeMillis()
        val eventUp = MotionEvent.obtain(
            downTime, downTime,
            MotionEvent.ACTION_UP,
            0f, 0f, 0 // Position (0,0) dans le coin supérieur gauche
        )

        touchManager.onTouch(dummyView, eventUp)

        // Vérifications :
        // 1. Le joueur n'a pas gagné
        assertFalse("Le joueur ne devrait pas avoir trouvé la cible", isTargetFoundCalled)
        // 2. Le manager a bien envoyé le message d'erreur au listener
        assertTrue(
            "Le feedback doit indiquer que le joueur s'éloigne",
            feedbackReceived.contains("éloignez")
        )

        eventUp.recycle()
    }
}