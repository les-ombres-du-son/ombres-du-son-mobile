package fr.upjv.lesombresduson.ui.game.cecilia.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import fr.upjv.lesombresduson.data.model.LevelStat
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.lang.reflect.Method


class CeciliaScoreViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()


    /**
     * Teste le calcul de la moyenne globale et le formatage du texte destiné au TTS.
     * Simule un retour de Firebase avec deux niveaux pour s'assurer de la justesse
     * des calculs et de la concaténation des phrases.
     */
    @Test
    fun testCalculateResults_ComputesAverageAndFormatsSpeechCorrectly() {
        val viewModel = CeciliaScoreViewModel()

        // Mocks des statistiques de fin de niveau
        val dummyStats = listOf(
            LevelStat("Niveau1", 100, "Expert"),
            LevelStat("Niveau2", 50, "Novice")
        )

        // Contournement du scope private via la réflexion pour tester la méthode isolément
        val method: Method = CeciliaScoreViewModel::class.java.getDeclaredMethod("calculateResults", List::class.java)
        method.isAccessible = true

        // Exécution avec nos données de test
        method.invoke(viewModel, dummyStats)

        // Vérif de la moyenne calculée : (100 + 50) / 2 = 75
        Assert.assertEquals("La moyenne calculée devrait être 75", 75, viewModel.averageScore.value)

        // Vérif du texte généré pour la synthèse vocale
        val speech = viewModel.speechText.value
        Assert.assertTrue("Le texte vocal ne doit pas être nul", speech != null)

        // Check de la présence des informations clés dans le script vocal
        Assert.assertTrue(
            "Doit mentionner le score du Niveau 1",
            speech!!.contains("Pour Niveau 1, vous avez obtenu 100")
        )
        Assert.assertTrue(
            "Doit mentionner le score du Niveau 2",
            speech.contains("Pour Niveau 2, vous avez obtenu 50")
        )
        Assert.assertTrue(
            "Doit annoncer la moyenne exacte",
            speech.contains("score moyen global est de 75 sur cent")
        )
    }

    /**
     * Test de robustesse.
     * S'assure qu'une liste vide ne provoque pas d'erreur (ex: division par zéro)
     * et laisse l'état des LiveData intact.
     */
    @Test
    fun testCalculateResults_WithEmptyList_DoesNothing() {
        val viewModel = CeciliaScoreViewModel()

        val method: Method = CeciliaScoreViewModel::class.java.getDeclaredMethod("calculateResults", List::class.java)
        method.isAccessible = true

        // Tentative d'exécution avec une liste vide
        method.invoke(viewModel, emptyList<LevelStat>())

        // Les LiveData ne doivent pas avoir bougé (null par défaut lors de l'init du ViewModel)
        Assert.assertEquals(null, viewModel.averageScore.value)
        Assert.assertEquals(null, viewModel.speechText.value)
    }
}