package fr.upjv.lesombresduson.manager.core

import android.content.Context
import com.google.gson.Gson
import fr.upjv.lesombresduson.R
import fr.upjv.lesombresduson.data.model.TutorialConfig
import fr.upjv.lesombresduson.data.model.TutorialStep
import java.io.InputStreamReader

class TutorialManager(private val context: Context) {

    private var tutorials: List<TutorialStep> = emptyList()

    init {
        loadTutorials()
    }

    /**
     * Charge les tutoriels depuis le fichier JSON.
     */
    private fun loadTutorials() {
        // Lecture du fichier JSON depuis le dossier 'res/raw'
        val inputStream = context.resources.openRawResource(R.raw.gameplay_tutorials)
        val reader = InputStreamReader(inputStream)

        // Conversion du JSON en objets Kotlin
        val config = Gson().fromJson(reader, TutorialConfig::class.java)
        tutorials = config.tutorials
    }

    /**
     * Récupère la liste des tutoriels pour un personnage spécifique.
     */
    fun getTutorialsForCharacter(characterName: String): List<TutorialStep> {
        // Filtre la liste pour ne garder que ceux du bon personnage
        return tutorials.filter { it.character == characterName }
    }
}