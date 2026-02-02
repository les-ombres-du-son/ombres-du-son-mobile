package fr.upjv.lesombresduson.ui.game.cecilia.util

import android.app.AlertDialog
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import fr.upjv.lesombresduson.manager.sensor.HapticManager
import fr.upjv.lesombresduson.ui.StartChoiseCharacter

/**
 * Centralise la gestion du cycle de vie des ressources partagées (Audio, Haptique)
 * et la logique de navigation inter-activités pour éviter la duplication de code.
 */
abstract class BackGameActivity : AppCompatActivity() {

    // Handler pour gérer le compte à rebours de fin de niveau
    private val handler = Handler(Looper.getMainLooper())

    // Instance partagée du gestionnaire de vibrations
    protected lateinit var hapticManager: HapticManager

    private var introPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialisation unique du service haptique lié au contexte de l'activité
        hapticManager = HapticManager(this)
    }

    /**
     * Configure la navigation de retour vers l'écran de sélection.
     * Gère la pile d'activités (Flags) pour éviter l'empilement inutile des vues.
     */
    protected fun setupBackButton(button: View) {
        button.setOnClickListener {
            val intent = Intent(this, StartChoiseCharacter::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
    }

    /**
     * Gère la lecture asynchrone de la piste audio d'introduction.
     * Déclenche le callback onComplete une fois la lecture terminée pour synchroniser le début du gameplay.
     */
    protected fun playIntro(resId: Int, onComplete: () -> Unit) {
        introPlayer?.release()

        introPlayer = MediaPlayer.create(this, resId).apply {
            setOnCompletionListener {
                it.release()
                introPlayer = null
                onComplete()
            }
            start()
        }
    }

    /**
     * Affiche la modale de fin de niveau avec le récapitulatif des performances.
     * Gère la transition vers l'activité suivante via Intent.
     */
    protected fun showLevelCompleteDialog(
        score: Int,
        profil: String,
        details: String,
        nextActivityClass: Class<*>?
    ) {
        // Définition de l'action de navigation pour éviter de dupliquer le code
        val navigateToNext = {
            nextActivityClass?.let {
                val intent = Intent(this, it)
                startActivity(intent)
                finish()
            }
        }

        // Tâche automatique exécutée après 10 secondes
        val autoStartRunnable = Runnable {
            if (!isFinishing) {
                navigateToNext()
            }
        }

        // Construction du message avec l'info du compte à rebours
        val messageWithTimer = "Profil : $profil\n\n$details\n\n⏳ Niveau suivant dans 10s..."

        val dialog = AlertDialog.Builder(this)
            .setTitle("Niveau Terminé : $score/100")
            .setMessage(messageWithTimer)
            .setPositiveButton("Continuer") { _, _ ->
                // Annule le timer si l'utilisateur clique manuellement
                handler.removeCallbacks(autoStartRunnable)
                navigateToNext()
            }
            .setCancelable(false)
            .create()

        dialog.show()

        // Lancement du compte à rebours (10 000 ms = 10s)
        handler.postDelayed(autoStartRunnable, 10000)
    }

    /**
     * Libération systématique des ressources média et haptiques
     * pour prévenir les fuites de mémoire à la destruction de l'activité.
     */
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        introPlayer?.release()
        introPlayer = null
        hapticManager.cancel()
    }
}