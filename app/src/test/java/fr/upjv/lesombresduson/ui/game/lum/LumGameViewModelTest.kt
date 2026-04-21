package fr.upjv.lesombresduson.ui.game.lum

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LumGameViewModelTest {

    private lateinit var viewModel: LumGameViewModel

    @Before
    fun setUp() {
        // Avant chaque test, on crée un nouveau ViewModel tout neuf
        viewModel = LumGameViewModel()
    }

    @Test
    fun `generateNewColor doit attribuer une couleur valide de la liste`() {
        // Au début, c'est vide
        assertTrue(viewModel.targetColorToFind.isEmpty())

        // On génère une couleur
        viewModel.generateNewColor()

        // On vérifie que la couleur fait bien partie de la liste autorisée
        val validColors = listOf("Rouge", "Bleu", "Vert", "Jaune")
        assertTrue(validColors.contains(viewModel.targetColorToFind))
    }

    @Test
    fun `nextFilter doit incrementer l index et revenir a zero a la fin`() {
        viewModel.currentFilterIndex = 0

        viewModel.nextFilter()
        assertEquals(1, viewModel.currentFilterIndex)

        // On simule qu'on est sur le tout dernier filtre
        viewModel.currentFilterIndex = VisionData.filters.size - 1
        viewModel.nextFilter()

        // Ça doit boucler et revenir à 0
        assertEquals(0, viewModel.currentFilterIndex)
    }

    @Test
    fun `addPointsForWin doit calculer les points en fonction du filtre actuel`() {
        // On simule un score de départ
        viewModel.currentScore = 10

        // On simule qu'on a choisi le filtre 1 (DMLA qui vaut 12 points selon VisionData)
        viewModel.currentFilterIndex = 1

        // On déclenche la victoire
        val pointsGagnes = viewModel.addPointsForWin()

        // On vérifie qu'on a bien gagné 12 points
        assertEquals(12, pointsGagnes)

        // On vérifie que le score total est bien 10 + 12 = 22
        assertEquals(22, viewModel.currentScore)
    }
}