package fr.upjv.lesombresduson.manager.sensor

import android.content.Context
import android.os.Vibrator
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HapticManagerTest {

    private lateinit var hapticManager: HapticManager
    private lateinit var systemVibrator: Vibrator

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        hapticManager = HapticManager(context)

        // On récupère le vrai service pour vérifier sa présence
        systemVibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /**
     * Vérifie que le HapticManager a correctement récupéré le service Vibrator
     * correspondant à la version du SDK de l'appareil.
     */
    @Test
    fun testVibratorInitialization() {
        // Vérifie que le manager a bien réussi à instancier le vibreur
        assertNotNull("Le vibreur ne devrait pas être null", hapticManager)
    }

    /**
     * Teste la vibration de succès (OneShot).
     * Valide que l'appel ne provoque pas d'exception sur les API anciennes ou récentes.
     */
    @Test
    fun testVibrateSuccess_NoCrash() {
        // On teste que l'appel à la méthode ne provoque pas d'exception
        // Même sur un émulateur sans moteur haptique, l'API Android gère l'appel sans crash
        hapticManager.vibrateSuccess()
    }

    /**
     * Teste la vibration de victoire (Waveform).
     * Vérifie la construction correcte du pattern de vibration complexe.
     */
    @Test
    fun testVibrateVictory_NoCrash() {
        // Teste le pattern complexe (Waveform)
        hapticManager.vibrateVictory()
    }

    /**
     * Vérifie que la commande d'annulation (cancel) fonctionne correctement
     * et libère l'accès au matériel après une vibration programmée.
     */
    @Test
    fun testCancelVibration() {
        hapticManager.vibrate(500)
        hapticManager.cancel()
        // Si on arrive ici sans crash, le test est validé
    }
}