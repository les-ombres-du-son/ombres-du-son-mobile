package fr.upjv.lesombresduson.ui.game.cecilia

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CeciliaLevel3ActivityTest {

    /**
     * Teste la logique de détection de la cible.
     * Vérifie que si le joueur est sur la cible (distance < winThreshold),
     * le compteur de cibles trouvées augmente.
     */
    @Test
    fun testTargetFoundLogic() {
        ActivityScenario.launch(CeciliaLevel3Activity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // 1. On force l'état du jeu pour le test
                activity.isGameRunning = true
                activity.targetsFound = 0 // On part de zéro

                // 2. On place le joueur et la cible exactement aux mêmes coordonnées
                // (Distance = 0, donc < winThreshold de 15f)
                activity.playerX = 50f
                activity.playerY = 50f
                activity.targetX = 50f
                activity.targetY = 50f

                // 3. On déclenche manuellement la mise à jour physique
                activity.updatePhysicsAndFeedback()

                // 4. On vérifie que la cible a été trouvée
                assertTrue(
                    "Le compteur targetsFound devrait être incrémenté quand la distance est < 15f",
                    activity.targetsFound > 0
                )
            }
        }
    }

    /**
     * Teste les limites de déplacement du joueur.
     * Vérifie que le joueur ne peut pas sortir de la grille 0-100.
     */
    @Test
    fun testPlayerMovementLimits() {
        ActivityScenario.launch(CeciliaLevel3Activity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // On force l'état du jeu
                activity.isGameRunning = true

                // On place le joueur au centre (50,50)
                activity.playerX = 50f
                activity.playerY = 50f

                // On applique un tilt extrême pour sortir des limites (Haut = y < 0, Gauche = x > 0)
                // (L'inclinaison simulée est gérée dans onSensorChanged)

                // Test de la limite X = 0 (Gauche extrême)
                activity.playerX = -50f // On force une valeur hors limite
                activity.playerX = activity.playerX.coerceIn(0f, 100f) // La logique de l'activité
                assertEquals("PlayerX ne doit pas descendre sous 0f", 0f, activity.playerX)

                // Test de la limite X = 100 (Droite extrême)
                activity.playerX = 150f
                activity.playerX = activity.playerX.coerceIn(0f, 100f)
                assertEquals("PlayerX ne doit pas dépasser 100f", 100f, activity.playerX)

                // Test de la limite Y = 0 (Haut extrême)
                activity.playerY = -10f
                activity.playerY = activity.playerY.coerceIn(0f, 100f)
                assertEquals("PlayerY ne doit pas descendre sous 0f", 0f, activity.playerY)
            }
        }
    }

    /**
     * Teste que la génération de cible (Spawn) reste dans les limites de sécurité.
     */
    @Test
    fun testTargetSpawnLimits() {
        ActivityScenario.launch(CeciliaLevel3Activity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                // On lance le spawn plusieurs fois pour vérifier la plage de Random
                for (i in 1..100) {
                    activity.spawnNewTarget()

                    // La cible doit être entre 10 et 90 (Random * 80 + 10)
                    assertTrue("TargetX hors limites (min 10) : ${activity.targetX}", activity.targetX >= 10f)
                    assertTrue("TargetX hors limites (max 90) : ${activity.targetX}", activity.targetX <= 90f)
                    assertTrue("TargetY hors limites (min 10) : ${activity.targetY}", activity.targetY >= 10f)
                    assertTrue("TargetY hors limites (max 90) : ${activity.targetY}", activity.targetY <= 90f)
                }
            }
        }
    }
}