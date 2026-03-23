package fr.upjv.lesombresduson.ui.navigation

import fr.upjv.lesombresduson.ui.game.cecilia.CeciliaFinalScoreActivity
import fr.upjv.lesombresduson.ui.game.cecilia.CeciliaIntroActivity
import fr.upjv.lesombresduson.ui.game.cecilia.CeciliaLevel1Activity
import fr.upjv.lesombresduson.ui.game.cecilia.CeciliaLevel3Activity
import fr.upjv.lesombresduson.ui.game.lum.LumGameActivity
import org.junit.Assert.assertEquals
import org.junit.Test

class GameRouterTest {

    /**
     * Test de la route du level intro pour le personnage Cécilia.
     */
    @Test
    fun testCeciliaRouting_IntroNotFinished_GoesToIntro() {
        // Niveau 1, mais le joueur n'a pas fini l'intro
        val targetClass = GameRouter.getCeciliaActivityClass(1, false)
        assertEquals("Devrait rediriger vers l'intro", CeciliaIntroActivity::class.java, targetClass)
    }

    /**
     * Test de la route du level 1 pour le personnage Cécilia.
     */
    @Test
    fun testCeciliaRouting_IntroFinished_GoesToLevel1() {
        // Niveau 1, et l'intro est déjà validée
        val targetClass = GameRouter.getCeciliaActivityClass(1, true)
        assertEquals("Devrait rediriger vers le niveau 1", CeciliaLevel1Activity::class.java, targetClass)
    }

    /**
     * Test de la route du level 3 pour le personnage Cécilia.
     */
    @Test
    fun testCeciliaRouting_Level3_GoesToLevel3() {
        // Pour les autres niveaux, le flag introFinished n'a pas d'impact
        val targetClass = GameRouter.getCeciliaActivityClass(3, true)
        assertEquals("Devrait rediriger vers le niveau 3", CeciliaLevel3Activity::class.java, targetClass)
    }

    /**
     * Test de la route du level final pour le personnage Cécilia.
     */
    @Test
    fun testCeciliaRouting_Level6_GoesToFinalScore() {
        val targetClass = GameRouter.getCeciliaActivityClass(6, true)
        assertEquals("Devrait rediriger vers le score final", CeciliaFinalScoreActivity::class.java, targetClass)
    }

    /**
     * Test de la route du jeu pour le personnage Lum.
     */
    @Test
    fun testLumRouting_GoesToLumGame() {
        val targetClass = GameRouter.getLumActivityClass()
        assertEquals("Devrait rediriger vers le jeu de Lum", LumGameActivity::class.java, targetClass)
    }
}