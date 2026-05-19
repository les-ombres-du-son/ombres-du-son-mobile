package fr.upjv.lesombresduson.ui.game.lum

import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import kotlin.random.Random

class LumGameViewModel : ViewModel() {

    var currentScore: Int = 0
    var currentFilterIndex: Int = 0
    var targetColorToFind: String = ""
    var timeLeft: Int = 0
    private var timer: CountDownTimer? = null

    var onTickCallback: ((Int) -> Unit)? = null
    var onTimeUpCallback: (() -> Unit)? = null

    private val colorsList = listOf("Rouge", "Bleu", "Vert", "Jaune", "Orange", "Violet", "Gris", "Noir", "Blanc")

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

    // Début du timer pour la recherche de la couleur
    fun startTimerForFilter() {
        // Sécurité : on arrête un éventuel timer déjà en cours
        timer?.cancel()

        // CALCUL DU TEMPS : 15 secondes de base + un bonus selon la difficulté du handicap
        // Exemple : Vision normale (1pt) -> 17s | Cataracte (8pts) -> 31s
        val baseTime = 15
        val bonusTime = VisionData.diseasePoints[currentFilterIndex] * 2
        timeLeft = baseTime + bonusTime

        timer = object : CountDownTimer((timeLeft * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeft = (millisUntilFinished / 1000).toInt()
                // On prévient l'activité pour mettre à jour l'UI
                onTickCallback?.invoke(timeLeft)
            }

            override fun onFinish() {
                // Le temps est écoulé
                onTimeUpCallback?.invoke()
            }
        }.start()
    }

    // Arrête le timer
    fun stopTimer() {
        timer?.cancel()
    }

    // Sécurité si le ViewModel est détruit par Android
    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}