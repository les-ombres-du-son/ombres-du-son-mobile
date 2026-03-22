package fr.upjv.lesombresduson.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import fr.upjv.lesombresduson.data.model.LevelStat

/**
 * Repository pour interroger Firebase pour avoir les scores de chaque niveau.
 */
class ScoreRepository {
    private val db = FirebaseFirestore.getInstance()

    fun getLevelStats(userId: String, characterName: String,
                      onSuccess: (List<LevelStat>) -> Unit,
                      onFailure: (Exception) -> Unit) {

        db.collection("Users")
            .document(userId)
            .collection("Games")
            .document(characterName)
            .collection("LevelStats")
            .get()
            .addOnSuccessListener { documents ->
                val stats = documents.map { doc ->
                    LevelStat(
                        id = doc.id,
                        score = doc.getLong("score_global")?.toInt() ?: 0,
                        profil = doc.getString("profil") ?: "Inconnu"
                    )
                }
                onSuccess(stats)
            }
            .addOnFailureListener { onFailure(it) }
    }
}