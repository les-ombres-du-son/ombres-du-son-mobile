package fr.upjv.lesombresduson.ui.game.cecilia.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import fr.upjv.lesombresduson.data.model.LevelStat
import fr.upjv.lesombresduson.data.repository.ScoreRepository

class CeciliaScoreViewModel : ViewModel() {
    private val repository = ScoreRepository()

    // LiveData pour que l'Activity puisse observer les changements
    val levelStats = MutableLiveData<List<LevelStat>>()
    val averageScore = MutableLiveData<Int>()
    val speechText = MutableLiveData<String>()
    val error = MutableLiveData<String>()
    val isLoading = MutableLiveData<Boolean>()

    /**
     * Charge les statistiques de chaque niveau depuis Firebase
     */
    fun loadScores(userId: String, characterName: String) {
        isLoading.value = true
        repository.getLevelStats(userId, characterName,
            onSuccess = { stats ->
                levelStats.value = stats
                calculateResults(stats)
                isLoading.value = false
            },
            onFailure = {
                error.value = "Erreur lors de la récupération des résultats."
                isLoading.value = false
            }
        )
    }

    /**
     * Calcul des résultats et mise à jour des LiveData
     */
    private fun calculateResults(stats: List<LevelStat>) {
        if (stats.isEmpty()) return

        var total = 0
        val builder = StringBuilder("Bilan final de l'expérience de Cécilia. ")

        stats.forEach { stat ->
            total += stat.score
            val levelTitle = stat.id.replace("Niveau", "Niveau ")
            builder.append("Pour $levelTitle, vous avez obtenu ${stat.score} sur cent. ")
        }

        val avg = total / stats.size
        averageScore.value = avg
        builder.append("Votre score moyen global est de $avg sur cent. Félicitations !")
        speechText.value = builder.toString()
    }
}