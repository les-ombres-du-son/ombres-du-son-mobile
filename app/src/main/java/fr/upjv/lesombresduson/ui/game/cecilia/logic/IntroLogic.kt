package fr.upjv.lesombresduson.ui.game.cecilia.logic

/**
 * Logique métier pour l'introduction de Cécilia.
 * Calcule les scores et génère les conseils textuels.
 * Détermine le profil du joueur.
 */
class IntroLogic {

    /**
     * Calcule les scores détaillés et globaux.
     * @param interruptionCount Nombre de tentatives d'impatience.
     * @param instabilityCount Nombre d'instabilités.
     * @param totalDistance Distance totale parcourue.
     * @return Triple(Score Écoute, Score Calme, Score Global)
     */
    fun calculateScores(interruptionCount: Int, instabilityCount: Int, totalDistance: Float): Triple<Int, Int, Int> {
        // Score Écoute (Impatience)
        val scoreEcoute = (100 - (interruptionCount * 15)).coerceIn(0, 100)

        // Score Calme (Stabilité Gyro + Tactile)
        val stabilityGyro = (100 - (instabilityCount * 10)).coerceIn(0, 100)
        val penaltyTactile = ((totalDistance - 1500) / 100).toInt().coerceAtLeast(0)
        val stabilityTouch = (100 - penaltyTactile).coerceIn(0, 100)
        val scoreCalme = (stabilityGyro + stabilityTouch) / 2

        // Score Global (Pondéré)
        val globalScore = ((scoreEcoute * 0.6) + (scoreCalme * 0.4)).toInt()

        return Triple(scoreEcoute, scoreCalme, globalScore)
    }

    /**
     * Détermine le profil du joueur selon son score global.
     */
    fun getPlayerProfile(globalScore: Int): String {
        return when {
            globalScore > 85 -> "L'Oreille Absolue"
            globalScore > 60 -> "L'Apprenti Attentif"
            else -> "Le Visuel Pressé"
        }
    }

    /**
     * Génère les conseils textuels pour le bilan.
     */
    fun getAdvice(scoreEcoute: Int, scoreCalme: Int): Pair<String, String> {
        val conseilEcoute = if (scoreEcoute < 50) "Prenez le temps d'écouter les instructions." else ""
        val conseilCalme = if (scoreCalme < 50) "Essayez de limiter les mouvements brusques." else ""
        return Pair(conseilEcoute, conseilCalme)
    }
}