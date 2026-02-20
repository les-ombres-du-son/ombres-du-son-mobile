package fr.upjv.lesombresduson.data.remote

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import fr.upjv.lesombresduson.R

object RealtimeHelper {

    private const val TAG = "RealtimeHelper"
    private var database: FirebaseDatabase? = null

    // Pour calculer le temps passé sur l'étape actuelle localement
    private var stepStartTimeLocal: Long = 0

    private val sessionRef: DatabaseReference?
        get() = database?.getReference("active_sessions")

    private val userId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid


    /**
     * Initialisation de la base de données Firebase Realtime.
     * @param context Contexte de l'application.
     */
    fun init(context: Context) {
        if (database == null) {
            try {
                val url = context.getString(R.string.database_url)
                if (url.isNotEmpty()) {
                    database = FirebaseDatabase.getInstance(url)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Init failed: ${e.message}")
            }
        }
    }


    /**
     * Démarre une nouvelle session de jeu.
     * @param levelName Nom du niveau.
     */
    fun startSession(levelName: String) {
        val ref = sessionRef ?: return
        userId?.let { uid ->
            stepStartTimeLocal = System.currentTimeMillis()
            val sessionData = mapOf(
                "level" to levelName,
                "currentStep" to "start",
                "startTime" to ServerValue.TIMESTAMP,
                "status" to "playing"
            )
            ref.child(uid).setValue(sessionData)
        }
    }

    /**
     * Met à jour le temps passé sur l'étape actuelle.
     * @param stepName Nom de l'étape.
     */
    fun updateStep(stepName: String) {
        val ref = sessionRef ?: return
        userId?.let { uid ->
            stepStartTimeLocal = System.currentTimeMillis() // Reset du chrono local
            val updates = mapOf(
                "currentStep" to stepName,
                "stepStartTime" to ServerValue.TIMESTAMP
            )
            ref.child(uid).updateChildren(updates)
        }
    }

    /**
     * Termine la session de jeu.
     * Supprime les données de la session de la base de données.
     */
    fun endSession() {
        val ref = sessionRef ?: return
        userId?.let { uid -> ref.child(uid).removeValue() }
    }


    // =========================================================================
    //                      METRIQUES DES CAPTEURS
    // =========================================================================

    /**
     * Met à jour les statistiques de capteurs tactiles.
     * @param distanceToTarget Distance à la cible.
     */
    fun updateTactileStats(distanceToTarget: Double, isTouching: Boolean) {
        val ref = sessionRef ?: return
        userId?.let { uid ->
            val elapsedTime = System.currentTimeMillis() - stepStartTimeLocal

            val updates = mapOf(
                "metrics/type" to "tactile",
                "metrics/distanceToTarget" to distanceToTarget.toInt(), // Distance en pixels
                "metrics/isTouching" to isTouching,
                "metrics/timeSpentOnStepMs" to elapsedTime, // Temps passé en millisecondes
                "lastActionTime" to ServerValue.TIMESTAMP
            )
            ref.child(uid).updateChildren(updates)
        }
    }

    /**
     * Met à jour les statistiques de capteurs de micro.
     * @param amplitude Amplitude du son.
     */
    fun updateMicStats(amplitude: Int, threshold: Int, isDetecting: Boolean) {
        val ref = sessionRef ?: return
        userId?.let { uid ->
            val elapsedTime = System.currentTimeMillis() - stepStartTimeLocal

            val updates = mapOf(
                "metrics/type" to "microphone",
                "metrics/currentAmplitude" to amplitude,
                "metrics/thresholdNeeded" to threshold,
                "metrics/isBlowing" to isDetecting, // Est-ce qu'il souffle assez fort ?
                "metrics/timeSpentOnStepMs" to elapsedTime,
                "lastActionTime" to ServerValue.TIMESTAMP
            )
            ref.child(uid).updateChildren(updates)
        }
    }

    /**
     * Met à jour les statistiques de capteurs d'accéléromètre.
     * @param errorCount Nombre d'erreurs.
     * @param instability Nombre de tremblements/relâchements.
     * @param expectedDirection La direction attendue (ex: "à droite").
     * @param actualDirection La direction réelle du mouvement (ex: "vers le haut").
     */
    fun updateGyroStats(errorCount: Int, instability: Int, expectedDirection: String, actualDirection: String) {
        val ref = sessionRef ?: return
        userId?.let { uid ->
            val elapsedTime = System.currentTimeMillis() - stepStartTimeLocal

            val updates = mapOf(
                "metrics/type" to "gyroscope",
                "metrics/errorCount" to errorCount,
                "metrics/movementIntensity" to instability,
                "metrics/expectedDirection" to expectedDirection,
                "metrics/actualDirection" to actualDirection,
                "metrics/timeSpentOnStepMs" to elapsedTime,
                "lastActionTime" to ServerValue.TIMESTAMP
            )
            ref.child(uid).updateChildren(updates)
        }
    }

    /**
     * Écoute les réponses de l'IA.
     * @param onAssistanceReceived Fonction à appeler avec le type et le message de l'IA.
     */
    fun listenForAssistance(onAssistanceReceived: (String, String) -> Unit) {
        val ref = sessionRef ?: return
        userId?.let { uid ->
            ref.child(uid).child("ai_assistance").addValueEventListener(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val type = snapshot.child("type").getValue(String::class.java)
                    val message = snapshot.child("message").getValue(String::class.java)
                    if (type != null && message != null) {
                        onAssistanceReceived(type, message)
                        snapshot.ref.removeValue()
                    }
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            })
        }
    }
}