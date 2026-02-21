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
     * @param isTouching Si le joueur est en train de toucher.
     * @param currentX Position actuelle sur l'axe X.
     * @param currentY Position actuelle sur l'axe Y.
     * @param targetX Position de la cible sur l'axe X.
     * @param targetY Position de la cible sur l'axe Y.
     * @param isApproaching Si le joueur est approchant la cible.
     */
    fun updateTactileStats(
        distanceToTarget: Double,
        isTouching: Boolean,
        currentX: Float,
        currentY: Float,
        targetX: Int,
        targetY: Int,
        isApproaching: Boolean
    ) {
        val ref = sessionRef ?: return
        userId?.let { uid ->
            val elapsedTime = System.currentTimeMillis() - stepStartTimeLocal

            val updates = mapOf(
                "metrics/type" to "tactile",
                "metrics/distanceToTarget" to distanceToTarget.toInt(),
                "metrics/isTouching" to isTouching,
                "metrics/currentX" to currentX.toInt(),
                "metrics/currentY" to currentY.toInt(),
                "metrics/targetX" to targetX,
                "metrics/targetY" to targetY,
                "metrics/isApproaching" to isApproaching,
                "metrics/timeSpentOnStepMs" to elapsedTime,
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
     * Met à jour les statistiques du Sonar (Niveau 1).
     * @param tapCount Nombre de fois que le joueur a tapé.
     * @param optimalTaps Nombre de taps considérés comme optimaux.
     * @param isSpamming Si le joueur tape frénétiquement.
     */
    fun updateSonarStats(tapCount: Int, optimalTaps: Int, isSpamming: Boolean) {
        val ref = sessionRef ?: return
        userId?.let { uid ->
            val elapsedTime = System.currentTimeMillis() - stepStartTimeLocal

            val updates = mapOf(
                "metrics/type" to "sonar_tap",
                "metrics/tapCount" to tapCount,
                "metrics/optimalTaps" to optimalTaps,
                "metrics/isSpamming" to isSpamming,
                "metrics/timeSpentOnStepMs" to elapsedTime,
                "lastActionTime" to ServerValue.TIMESTAMP
            )
            ref.child(uid).updateChildren(updates)
        }
    }

    /**
     * Met à jour les statistiques temporelles (Feu rouge / Feu vert - Niveau 2).
     * @param isGreenLight Indique si le joueur doit maintenir (vert) ou relâcher (rouge).
     * @param isFingerPressed L'état actuel du doigt du joueur sur l'écran.
     * @param falseStartCount Nombre de fois où le joueur a relâché trop tôt ou appuyé sur rouge.
     * @param distractionErrors Nombre de fois où le joueur a été trop lent.
     * @param nextChangeTimestamp Timestamp du prochain changement de couleur.
     * @param currentStep L'étape actuelle de la voie.
     */
    fun updateTimingStats(isGreenLight: Boolean, isFingerPressed: Boolean, falseStartCount: Int, distractionErrors: Int, nextChangeTimestamp: Long, currentStep: Int) {
        val ref = sessionRef ?: return
        userId?.let { uid ->
            val elapsedTime = System.currentTimeMillis() - stepStartTimeLocal

            // Calcul du temps restant en millisecondes (jamais négatif)
            val timeUntilNextChange = if (nextChangeTimestamp > 0) {
                (nextChangeTimestamp - System.currentTimeMillis()).coerceAtLeast(0)
            } else 0

            val updates = mapOf(
                "metrics/type" to "timing_reaction",
                "metrics/isGreenLight" to isGreenLight,
                "metrics/isFingerPressed" to isFingerPressed,
                "metrics/falseStartCount" to falseStartCount,
                "metrics/distractionErrors" to distractionErrors,
                "metrics/timeUntilNextChangeMs" to timeUntilNextChange,
                "metrics/currentStep" to currentStep,
                "metrics/timeSpentOnStepMs" to elapsedTime,
                "lastActionTime" to ServerValue.TIMESTAMP
            )
            ref.child(uid).updateChildren(updates)
        }
    }

    /**
     * Met à jour les statistiques de navigation haptique (Niveau 3).
     * @param playerX Position du joueur sur l'axe X.
     * @param playerY Position du joueur sur l'axe Y.
     * @param targetX Position de la cible sur l'axe X.
     * @param targetY Position de la cible sur l'axe Y.
     * @param distance Distance entre le joueur et la cible.
     * @param isApproaching Si le joueur est approchant la cible.
     */
    fun updateHapticNavigationStats(
        playerX: Float, playerY: Float,
        targetX: Float, targetY: Float,
        distance: Float, isApproaching: Boolean,
        targetsFound: Int
    ) {
        val ref = sessionRef ?: return
        userId?.let { uid ->
            val elapsedTime = System.currentTimeMillis() - stepStartTimeLocal

            val updates = mapOf(
                "metrics/type" to "haptic_navigation",
                "metrics/playerX" to playerX.toInt(),
                "metrics/playerY" to playerY.toInt(),
                "metrics/targetX" to targetX.toInt(),
                "metrics/targetY" to targetY.toInt(),
                "metrics/distance" to distance.toInt(),
                "metrics/isApproaching" to isApproaching,
                "metrics/targetsFound" to targetsFound,
                "metrics/timeSpentOnStepMs" to elapsedTime,
                "lastActionTime" to ServerValue.TIMESTAMP
            )
            ref.child(uid).updateChildren(updates)
        }
    }

    /**
     * Envoie le texte prononcé par le joueur vers la base de données.
     * C'est ce texte que l'IA va lire et analyser.
     * @param spokenText Le texte transcrit depuis le microphone.
     */
    fun sendPlayerSpeech(spokenText: String) {
        val ref = sessionRef ?: return
        userId?.let { uid ->
            val updates = mapOf(
                "interaction/type" to "speech_to_ai",
                "interaction/playerText" to spokenText,
                "interaction/timestamp" to ServerValue.TIMESTAMP,
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