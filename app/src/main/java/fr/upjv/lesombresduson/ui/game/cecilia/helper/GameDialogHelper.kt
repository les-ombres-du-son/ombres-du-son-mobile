package fr.upjv.lesombresduson.ui.game.cecilia.helper

import android.app.Activity
import android.app.AlertDialog
import android.os.Handler
import android.os.Looper

/**
 * Gestionnaire de modales de fin de niveau.
 */
class GameDialogHelper(private val activity: Activity) {

    private val handler = Handler(Looper.getMainLooper())
    private var autoStartRunnable: Runnable? = null

    /**
     * Affiche la modale de fin et gère le compte à rebours de 10 secondes.
     * @param onNavigate Callback exécuté quand l'utilisateur clique ou quand le temps est écoulé.
     */
    fun showLevelCompleteDialog(
        score: Int,
        profil: String,
        details: String,
        onNavigate: () -> Unit
    ) {
        val messageWithTimer = "Profil : $profil\n\n$details\n\n⏳ Niveau suivant dans 10s..."

        // Définition de l'action de navigation
        val navigateAction = {
            if (!activity.isFinishing) {
                onNavigate()
            }
        }

        autoStartRunnable = Runnable { navigateAction() }

        val dialog = AlertDialog.Builder(activity)
            .setTitle("Niveau Terminé : $score/100")
            .setMessage(messageWithTimer)
            .setPositiveButton("Continuer") { _, _ ->
                // Annule le timer si l'utilisateur clique manuellement
                cancelTimer()
                navigateAction()
            }
            .setCancelable(false)
            .create()

        dialog.show()

        // Lancement du compte à rebours (10 000 ms = 10s)
        autoStartRunnable?.let { handler.postDelayed(it, 10000) }
    }

    /**
     * À appeler dans le onDestroy() de l'activité pour éviter les fuites de mémoire (Memory Leaks).
     */
    fun cancelTimer() {
        autoStartRunnable?.let { handler.removeCallbacks(it) }
        handler.removeCallbacksAndMessages(null)
    }
}