package fr.upjv.lesombresduson.manager.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import fr.upjv.lesombresduson.ui.game.cecilia.CeciliaIntroActivity

/**
 * Gère la logique des capteurs, la détection des gestes, le chronométrage
 * et l'état du jeu, et communique les événements à l'Activity via GestureListener.
 */
class SensorGameManager(private val context: Context, private val listener: SensorGameListener
) : SensorEventListener {
    /**
     * Interface pour communiquer avec l'Activity.
     */
    interface SensorGameListener {
        fun onInstructionReady(instruction: String)
        fun onGestureValidated(isGameComplete: Boolean, nextInstruction: String)
        fun onFeedbackNeeded(message: String)
        fun onDirectionChanged(actualDirection: String)
    }

    // Initialisation des capteurs
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Utilisation de var pour les variables d'état
    var gestureCount = 0 // Compteur de gestes (0:droite, 1:gauche, 2:haut, 3:bas)
        private set // Rendre le setter privé pour contrôler les modifications

    // --- METRIQUES DE SENSIBILISATION ---
    var instabilityCount = 0 // Compte les resets (tremblements ou erreurs)
        private set
    // ------------------------------------

    // --- TEMPS RÉEL & LIMITATEUR (THROTTLE) ---
    private var lastReportedDirection: String = "à plat"
    private var lastReportedTime: Long = 0L
    private val THROTTLE_MS = 250L // Évite de spammer Firebase : envoie max 4 fois par seconde
    // ----------------------------------------------------

    // Constantes de jeu
    private val DELAY_MS = 3000L // 3 secondes
    private val TILT_THRESHOLD = 5.0f

    // Pour tracker le mouvement réel et éviter le spam
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var lastWrongTiltTime = 0L

    // Gestion du chronométrage et de l'état
    private val validationHandler = Handler(Looper.getMainLooper())
    private var isValidationPending = false

    // Instructions pour le jeu
    private val gestureInstructions = arrayOf(
        "à droite", // Geste 0
        "à gauche", // Geste 1
        "vers le haut", // Geste 2
        "vers le bas"   // Geste 3
    )

    // Initialisation du Runnable de validation (Lambda)
    private val validationRunnable = Runnable {
        // Le geste a été maintenu pendant 3 secondes, on valide

        // 1. Demande de feedback de validation à l'Activity (vibration et ding)
        listener.onFeedbackNeeded("VALIDATE")

        // 2. Passage à l'état suivant
        gestureCount++
        isValidationPending = false

        // 3. Communiquer l'état final à l'Activity
        val isGameComplete = (gestureCount >= 4)
        val nextInstruction = if (isGameComplete) "" else gestureInstructions[gestureCount]

        listener.onGestureValidated(isGameComplete, nextInstruction)
    }

    /**
     * Enregistre l'écouteur du capteur.
     */
    fun startListening() {
        if (accelerometer != null && gestureCount < 4) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
    }

    /**
     * Désenregistre l'écouteur du capteur.
     */
    fun stopListening() {
        sensorManager.unregisterListener(this)
        cancelPendingValidation()
    }

    /**
     * Annule toute validation en attente dans le Handler.
     */
    private fun cancelPendingValidation() {
        if (isValidationPending) {
            // Si on annule une validation en cours, c'est une instabilité !
            instabilityCount++
        }
        validationHandler.removeCallbacks(validationRunnable)
        isValidationPending = false
    }

    // --- Implémentation de SensorEventListener ---

    /**
     * Détecte les mouvements de l'accéléromètre pour valider les gestes du jeu.
     * @param event Événement du capteur contenant les valeurs d'accélération.
     */
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor == null || event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]

            lastX = x
            lastY = y

            // --- DÉTECTION EN TEMPS RÉEL ---
            val newDirection = currentActualDirection
            val currentTime = System.currentTimeMillis()

            // Si la direction a changé ET qu'on n'a pas envoyé d'info depuis au moins 250ms
            if (newDirection != lastReportedDirection && (currentTime - lastReportedTime > THROTTLE_MS)) {
                lastReportedDirection = newDirection
                lastReportedTime = currentTime

                // On informe l'Activité IMMÉDIATEMENT
                listener.onDirectionChanged(newDirection)
            }
            // ------------------------------------------

            var tiltDetected = false

            // Logique de détection des gestes (switch remplacé par when)
            when (gestureCount) {
                0 -> { // Droite
                    if (x < -TILT_THRESHOLD) tiltDetected = true
                }
                1 -> { // Gauche
                    if (x > TILT_THRESHOLD) tiltDetected = true
                }
                2 -> { // Haut (Correction: y > 0 quand l'écran est incliné vers le haut)
                    if (y < -TILT_THRESHOLD) tiltDetected = true // y devient négatif quand l'appareil est tourné vers le haut
                }
                3 -> { // Bas (Correction: y < 0 quand l'écran est incliné vers le bas)
                    if (y > TILT_THRESHOLD) tiltDetected = true // y devient positif quand l'appareil est tourné vers le bas
                }
            }

            // Détection si le joueur fait un mouvement, mais pas le bon !
            val isAnyTilt = kotlin.math.abs(x) > TILT_THRESHOLD || kotlin.math.abs(y) > TILT_THRESHOLD
            if (isAnyTilt && !tiltDetected && !isValidationPending) {
                val currentTime = System.currentTimeMillis()
                // On informe l'Activity seulement toutes les 2.5 secondes pour ne pas spammer Firebase
                if (currentTime - lastWrongTiltTime > 2500) {
                    lastWrongTiltTime = currentTime
                    listener.onFeedbackNeeded("WRONG_DIRECTION")
                }
            }

            if (tiltDetected && !isValidationPending) {
                // 1. Geste détecté : Démarrer la minuterie de 3 secondes
                isValidationPending = true
                validationHandler.postDelayed(validationRunnable, DELAY_MS)

                // Demander à l'Activity d'afficher le Toast de maintien
                val direction = gestureInstructions[gestureCount]
                listener.onFeedbackNeeded("Inclinaison $direction détectée. Maintenez pendant 3s...")

            } else if (!tiltDetected && isValidationPending) {
                // 2. Le téléphone a été relâché : Annuler la validation
                cancelPendingValidation()
                // Demander à l'Activity d'afficher le Toast d'annulation
                listener.onFeedbackNeeded("Relâchement détecté. Annulation de la validation.")
            }
        }
    }

    /**
     * Appelée par le système lorsque la précision du capteur change.
     * Nous ignorons les changements de précision pour ne pas complexifier le code inutilement.
     */
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Ignoré
    }

    // ---------Méthodes utilitaires pour la gestion du jeu---------/

    /**
     * Vérifie si le capteur d'accéléromètre est disponible.
     * @return true si disponible, false sinon.
     */
    val isAccelerometerAvailable: Boolean
        get() = accelerometer != null

    /**
     * Retourne la direction attendue pour le geste actuel.
     */
    val currentExpectedDirection: String
        get() = if (gestureCount < gestureInstructions.size) {
            gestureInstructions[gestureCount]
        } else {
            "terminé"
        }

    /**
     * Retourne la direction réelle du mouvement de l'accéléromètre.
     */
    val currentActualDirection: String
        get() {
            val directions = mutableListOf<String>()
            if (lastX < -TILT_THRESHOLD) directions.add("à droite")
            if (lastX > TILT_THRESHOLD) directions.add("à gauche")
            if (lastY < -TILT_THRESHOLD) directions.add("vers le haut")
            if (lastY > TILT_THRESHOLD) directions.add("vers le bas")

            return if (directions.isEmpty()) "à plat" else directions.joinToString(" et ")
        }
}