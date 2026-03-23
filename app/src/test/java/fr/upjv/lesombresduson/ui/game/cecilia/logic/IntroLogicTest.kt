package fr.upjv.lesombresduson.ui.game.cecilia.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IntroLogicTest {

    private val logic = IntroLogic()

    /**
     * Test de la logique métier pour le calcul des scores.
     */
    @Test
    fun `test score parfait`() {
        // 0 impatience, 0 instabilité, distance courte
        val (ecoute, calme, global) = logic.calculateScores(0, 0, 1000f)

        assertEquals(100, ecoute)
        assertEquals(100, calme)
        assertEquals(100, global)
    }

    /**
     * Test de la logique métier pour le calcul du profil du joueur.
     */
    @Test
    fun `test profil Oreille Absolue pour score haut`() {
        val profil = logic.getPlayerProfile(95)
        assertEquals("L'Oreille Absolue", profil)
    }

    /**
     * Test de la logique métier pour le calcul des conseils textuels.
     */
    @Test
    fun `test penalite impatience`() {
        // 4 interruptions * 15 = 60 de pénalité -> score 40
        val (ecoute, _, _) = logic.calculateScores(4, 0, 1000f)
        assertEquals(40, ecoute)
    }

    /**
     * Test de la logique métier pour le calcul des scores avec limite inférieure.
     */
    @Test
    fun `test score minimum ne descend pas sous zero`() {
        // Énormément d'erreurs
        val (ecoute, calme, global) = logic.calculateScores(100, 100, 5000f)

        assertTrue(ecoute >= 0)
        assertTrue(calme >= 0)
        assertTrue(global >= 0)
    }
}