package fr.upjv.lesombresduson.ui.game.cecilia.helper

import android.app.Activity
import android.app.AlertDialog

/**
 * Gestionnaire de modales de fin de niveau.
 */
class GameDialogHelper(private val activity: Activity) {

    private var currentDialog: AlertDialog? = null
    private var navigateAction: (() -> Unit)? = null
    private var isNavigated = false // Empêche de naviguer deux fois (clic + fin audio)

    /**
     * Affiche la modale de fin de niveau.
     */
    fun showLevelCompleteDialog(
        score: Int,
        profil: String,
        details: String,
        onNavigate: () -> Unit
    ) {
        this.navigateAction = onNavigate
        this.isNavigated = false

        val message = "Profil : $profil\n\n$details"

        currentDialog = AlertDialog.Builder(activity)
            .setTitle("Niveau Terminé : $score/100")
            .setMessage(message)
            .setPositiveButton("Continuer") { _, _ ->
                // L'utilisateur clique manuellement
                triggerNavigation()
            }
            .setCancelable(false)
            .create()

        currentDialog?.show()
    }

    /**
     * Déclenche la navigation et ferme la modale.
     * Peut être appelé par le clic du bouton ou la fin de l'audio.
     */
    fun triggerNavigation() {
        if (!isNavigated && !activity.isFinishing) {
            isNavigated = true
            currentDialog?.dismiss()
            navigateAction?.invoke()
        }
    }

    /**
     * Sécurité pour le onDestroy()
     */
    fun cancelTimer() {
        currentDialog?.dismiss()
        currentDialog = null
        navigateAction = null
    }
}