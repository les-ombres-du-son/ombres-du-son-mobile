package fr.upjv.lesombresduson.ui.game.lum

import androidx.lifecycle.ViewModel
import kotlin.random.Random

class LumGameViewModel : ViewModel() {

    var currentScore: Int = 0
    var currentFilterIndex: Int = 0
    var targetColorToFind: String = ""

    private val colorsList = listOf("Rouge", "Bleu", "Vert", "Jaune")

    // Génère une nouvelle couleur aléatoire
    fun generateNewColor() {
        targetColorToFind = colorsList[Random.nextInt(colorsList.size)]
    }

    // Calcule les points après une victoire et met à jour le score
    fun addPointsForWin(): Int {
        val pointsGagnes = VisionData.diseasePoints[currentFilterIndex]
        currentScore += pointsGagnes
        return pointsGagnes
    }

    // Passe au filtre visuel suivant
    fun nextFilter() {
        currentFilterIndex = (currentFilterIndex + 1) % VisionData.filters.size
    }
}