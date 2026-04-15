package fr.upjv.lesombresduson.ui.game.cecilia

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


@RunWith(AndroidJUnit4::class)
class CeciliaAudioManagerlevel2Test {

    private lateinit var context: Context
    private lateinit var audioManager: CeciliaAudioManagerlevel2

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        audioManager = CeciliaAudioManagerlevel2(context)
    }

    /**
     * Vérifie que le cycle d'initialisation et de chargement des sons
     * se termine avec succès.
     */
    @Test
    fun testInitializationAndLoading() {
        // Un verrou pour attendre la fin du chargement asynchrone (SoundPool)
        val latch = CountDownLatch(1)
        var isReady = false

        audioManager.onAudioReady = {
            isReady = true
            latch.countDown()
        }

        // On lance l'initialisation sur le thread principal pour éviter les soucis de Looper
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            audioManager.init()
        }

        // On attend maximum 10 secondes que les 5 sons soient chargés
        val success = latch.await(10, TimeUnit.SECONDS)

        assertTrue("Le chargement des sons a pris trop de temps (Timeout)", success)
        assertTrue("onAudioReady devrait être vrai après le chargement", isReady)
    }
}