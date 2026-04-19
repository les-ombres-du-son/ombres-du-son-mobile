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

    private var currentLevel: String = "default"

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
     * @param initialStatus Statut de départ (ex: "cinematic", "playing", "menu"). Par défaut sur cinematic.
     */
    fun startSession(levelName: String, initialStatus: String = "cinematic") {
        val ref = sessionRef ?: return
        userId?.let { uid ->
            stepStartTimeLocal = System.currentTimeMillis()
            currentLevel = levelName

            val updates = mapOf(
                "$currentLevel/level" to levelName,
                "$currentLevel/currentStep" to "start",
                "$currentLevel/startTime" to ServerValue.TIMESTAMP,
                "$currentLevel/status" to initialStatus,
                "status" to initialStatus
            )

            // On écrit à la racine de l'utilisateur
            ref.child(uid).updateChildren(updates)
        }
    }

    /**
     * Permet de mettre à jour le statut en cours de partie (ex: fin de cinématique -> "playing")
     * @param status Le nouveau statut ("playing", "cinematic", "paused", etc.)
     */
    fun updateGameStatus(status: String) {
        val ref = sessionRef ?: return
        userId?.let { uid ->
            val updates = mutableMapOf<String, Any>(
                "status" to status // Statut global
            )
            // On met aussi à jour le statut dans le dossier du niveau en cours
            if (currentLevel != "default") {
                updates["$currentLevel/status"] = status
            }
            ref.child(uid).updateChildren(updates)
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
                "$currentLevel/currentStep" to stepName,
                "$currentLevel/stepStartTime" to ServerValue.TIMESTAMP
            )
            // On écrit dans le sous-dossier du niveau
            ref.child(uid).child(currentLevel).updateChildren(updates)
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

            val metricsData = mapOf(
                "type" to "tactile",
                "distanceToTarget" to distanceToTarget.toInt(),
                "isTouching" to isTouching,
                "currentX" to currentX.toInt(),
                "currentY" to currentY.toInt(),
                "targetX" to targetX,
                "targetY" to targetY,
                "isApproaching" to isApproaching,
                "timeSpentOnStepMs" to elapsedTime
            )

            val updates = mapOf(
                "metrics" to metricsData,
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

            val metricsData = mapOf(
                "type" to "microphone",
                "currentAmplitude" to amplitude,
                "thresholdNeeded" to threshold,
                "isBlowing" to isDetecting,
                "timeSpentOnStepMs" to elapsedTime
            )

            val updates = mapOf(
                "metrics" to metricsData,
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

            val metricsData = mapOf(
                "type" to "gyroscope",
                "errorCount" to errorCount,
                "movementIntensity" to instability,
                "expectedDirection" to expectedDirection,
                "actualDirection" to actualDirection,
                "timeSpentOnStepMs" to elapsedTime
            )

            val updates = mapOf(
                "metrics" to metricsData,
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

            val metricsData = mapOf(
                "type" to "sonar_tap",
                "tapCount" to tapCount,
                "optimalTaps" to optimalTaps,
                "isSpamming" to isSpamming,
                "timeSpentOnStepMs" to elapsedTime
            )

            val updates = mapOf(
                "metrics" to metricsData,
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

            val metricsData = mapOf(
                "type" to "timing_reaction",
                "isGreenLight" to isGreenLight,
                "isFingerPressed" to isFingerPressed,
                "falseStartCount" to falseStartCount,
                "distractionErrors" to distractionErrors,
                "timeUntilNextChangeMs" to timeUntilNextChange,
                "currentStep" to currentStep,
                "timeSpentOnStepMs" to elapsedTime
            )

            val updates = mapOf(
                "metrics" to metricsData,
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

            val metricsData = mapOf(
                "type" to "haptic_navigation",
                "playerX" to playerX.toInt(),
                "playerY" to playerY.toInt(),
                "targetX" to targetX.toInt(),
                "targetY" to targetY.toInt(),
                "distance" to distance.toInt(),
                "isApproaching" to isApproaching,
                "targetsFound" to targetsFound,
                "timeSpentOnStepMs" to elapsedTime
            )

            val updates = mapOf(
                "metrics" to metricsData,
                "lastActionTime" to ServerValue.TIMESTAMP
            )
            ref.child(uid).updateChildren(updates)
        }
    }

    /**
     * Met à jour le personnage sélectionné pour le Niveau 4.
     * @param characterId L'ID du personnage (1, 2, 3...)
     */
    fun updateSelectedCharacter(characterId: Int) {
        val ref = sessionRef ?: return
        userId?.let { uid ->
            val updates = mapOf(
                "Niveau4/personnage" to "personnage_$characterId",
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
            val interactionData = mapOf(
                "type" to "speech_to_ai",
                "playerText" to spokenText,
                "timestamp" to ServerValue.TIMESTAMP
            )
            val updates = mapOf(
                "interaction" to interactionData,
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
            ref.child(uid).child("reponse_ai").addValueEventListener(object : com.google.firebase.database.ValueEventListener {
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

    /**
     * Écoute la réponse de l'IA. Gère les messages textuels et les ordres de répétition.
     * @param onResponseReceived Callback qui reçoit (message, isWin, repeter).
     */
    fun listenForAIResponse(onResponseReceived: (String, Boolean, Boolean) -> Unit): com.google.firebase.database.ValueEventListener? {
        val ref = sessionRef ?: return null
        val uid = userId ?: return null

        val responseRef = ref.child(uid).child("reponse_ai")

        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                // On récupère les valeurs depuis Firebase
                val message = snapshot.child("message").getValue(String::class.java) ?: ""
                val isWin = snapshot.child("boolean").getValue(Boolean::class.java) ?: false
                val repeter = snapshot.child("repeter").getValue(Boolean::class.java) ?: false

                // Si l'IA envoie un message OU demande une répétition explicite
                if (message.isNotEmpty() || repeter) {
                    Log.d(TAG, "IA a répondu : $message | Victoire : $isWin | Répéter : $repeter")
                    onResponseReceived(message, isWin, repeter)
                }
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.e(TAG, "Erreur écoute IA: ${error.message}")
            }
        }

        responseRef.addValueEventListener(listener)
        return listener
    }

    /**
     * Remet le flag 'repeter' à false dans Firebase pour éviter que le jeu
     * ne rejoue la cinématique en boucle.
     */
    fun resetRepeterFlag() {
        val ref = sessionRef ?: return
        userId?.let { uid ->
            ref.child(uid).child("reponse_ai").child("repeter").setValue(false)
        }
    }

    /**
     * Permet d'arrêter l'écoute sur un chemin précis.
     */
    fun stopListening(listener: com.google.firebase.database.ValueEventListener) {
        val ref = sessionRef ?: return
        val uid = userId ?: return
        ref.child(uid).child("reponse_ai").removeEventListener(listener)
    }
}