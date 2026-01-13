package fr.upjv.lesombresduson.ui.game.cecilia

import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import fr.upjv.lesombresduson.R
import fr.upjv.lesombresduson.ui.game.cecilia.util.BackGameActivity

/**
 * Activité basique du Niveau 3 (Cecilia).
 * Joue un son au démarrage et se ferme automatiquement à la fin.
 */
class CeciliaLevel3Activity : BackGameActivity() {

    private lateinit var btnBack: Button

    // Variable pour gérer le son
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gameplay_cecilia)

        // Initialisation des vues
        btnBack = findViewById(R.id.button_back)
        setupBackButton(btnBack)

        // --- Lancement de l'audio ---
        playIntroAudio()
    }

    private fun playIntroAudio() {
        // Initialisation avec le fichier dans res/raw/voix_off_niveau_3
        mediaPlayer = MediaPlayer.create(this, R.raw.voix_off_niveau3)

        // Écouteur pour détecter la fin du fichier audio
        mediaPlayer?.setOnCompletionListener {
        }

        // Démarrage de la lecture
        mediaPlayer?.start()
    }

    /**
     * Important : Si l'utilisateur quitte l'activité manuellement (bouton retour)
     * avant la fin du son, il faut arrêter le lecteur pour ne pas qu'il continue en fond.
     */
    override fun onDestroy() {
        super.onDestroy()
        if (mediaPlayer != null) {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }
}